package orc.mechanics.games;

import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.shareddata.LocalMap;
import orc.mechanics.GameSessionsManager;
import orc.mechanics.verticles.Core;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


// Игра крестики-нолики
public class TicTacToe extends AbstractVerticle {

    private Logger                   log                    = LoggerFactory.getLogger(Core.class);      // Логер
    private String                   localAddress           = "TicTacToe";                              // Локальный адрес вертикла
    private LocalMap<String, String> verticlesAddresses     = null;                                     // Массив персональных адресов вертиклов на шине данных
    private LocalMap<String, String> gamesList              = null;                                     // Список игр
    private EventBus                 eb                     = null;                                     // Шина данных для обмена сообщениями вертиклами между собой и вертиклов с веб-клиентами
    private GameSessionsManager      gameSessions           = null;                                     // Менеджер сессий игр


    @Override
    public void start(Promise<Void> startPromise) throws Exception {
        log.info("TicTacToe verticle started");

        gameSessions        = new GameSessionsManager();                                                // Менеджер сессий клиентов
        eb                  = vertx.eventBus();                                                         // Шина данных для обмена сообщениями вертиклами между собой и вертиклов с веб-клиентами
        verticlesAddresses  = vertx.sharedData().getLocalMap("verticlesAddresses");                  // Подключаем общий массив адресов вертиклов
        gamesList           = vertx.sharedData().getLocalMap("gamesList");                           // Подключаем список игр

        verticlesAddresses.put(localAddress, "TicTacToe");                                           // Регистрируем адрес в общем списке
        gamesList.put(localAddress, "Крестики-нолики");                                              // Регистрируем игру в общем списке

//        MessageConsumer<Object> mc;                                                                   // Тестируем сохранение консумера для возможности снятия регистрации с адреса
//        mc = eb.localConsumer(localAddress, this::onMessage);                                         // Подписываемся на сообщения на свой адрес
//        mc.unregister();

        eb.localConsumer(localAddress, this::onMessage);                                                // Подписываемся на сообщения на свой адрес
        gameSessions.setTtl(600);                                                                       // Устанавливаем время жизни неактивной клиентской сессии в 10 минут

        if (gameSessions.getTtl() > 0) {                                                                // Проверяем наличие критически неактивных сессий по таймеру и удаляем их
            vertx.setPeriodic(
                    gameSessions.getTtl(),
                    h -> {
                        HashMap<String, Object> doomeds = this.gameSessions.removeTheDead();

                        // Здесь реализовать удаление сессий аналогично removeGameSession....
                        if (doomeds.size() > 0) {
                            doomeds.forEach((k, v) -> {
                                eb.send("server", new JSONObject()
                                        .put("from", localAddress)
                                        .put("action", "removeAddress")
                                        .put("address", (String) ((HashMap<String, Object>) v).get("address"))
                                        .toString());
                            });
                        }
                    }
            );
        }

        startPromise.complete();
    }

    // Обработка входящих сообщений
    public void onMessage(Message<Object> ebMsg){

        System.out.println(localAddress+" received local message: "+ebMsg.body());

        HashMap<String, Object> msg;

        try {
            msg = new Gson().fromJson(ebMsg.body().toString(), HashMap.class);
        }catch (Exception e){
            log.error(localAddress+"::onMessage: received wrong JSON-string ("+ebMsg.body().toString()+")");
            return;
        }

        String from   = (String) msg.get("from");
        String action = (String) msg.get("action");

        if (from == null){
            log.error(localAddress+"::onMessage: no from field in body ("+msg.toString()+")");
            return;
        }
        if (action == null){
            log.error(localAddress+"::onMessage: no action field in body ("+msg.toString()+")");
            sendClientMessage(from, "error", new JSONObject().put("text","no action field in body"));
            return;
        }

        if (from.equals("server")) {
            onServerMessage(msg);
        } else if (from.equals("core")) {
            onCoreMessage(msg);
        }else{
            log.error(localAddress+"::onMessage: Unexpected sender: "+from);
            return;
        }
    }

    // Обработка внутренних сообщений от Server
    public void onServerMessage(HashMap<String, Object> msg){

        System.out.println(localAddress+"::onServerMessage: Received message from game: "+msg.toString());
    }

    // Обработка внутренних сообщений от Server
    public void onCoreMessage(HashMap<String, Object> msg){

        System.out.println(localAddress+"::onCoreMessage: Received message from game: "+msg.toString());

        String from   = (String) msg.get("from");
        String action = (String) msg.get("action");
        String uid    = (String) msg.get("usid");                   // Идентификатор клиентской сессии
        String gsid    = (String) msg.get("gsid");                  // Идентификатор игровой сессии
        String gameAddress = null;                                  // Адрес игровой сессии
        ArrayList<HashMap<String, String>> players;                 // Список участников игры
        ArrayList<HashMap<String, String>> playersNames;            // Список имён участников для рассылки
        HashMap<String, Object> waitingGames;                       // Список ...

        if (uid == null){
            log.error(localAddress+"::onClientMessage: no usid field in body ("+msg.toString()+")");
            sendClientMessage(from, "error", new JSONObject().put("text","no usid field in body"));
            return;
        }


        switch (action){
            case "getGameEntrance":                                                         // Отвечаем ядру на запрос формы входа
                waitingGames = getWaitingGamesSessions(uid, "waitingForPlayers");

                sendClientMessage(from, "setGameEntrance", new JSONObject()
                        .put("appName","TicTacToe")
                        .put("moduleName","entrance")
                        .put("resourceId","TicTacToe/js/entrance")
                        .put("clientAddress", (String) msg.get("clientAddress"))
                        .put("usid", (String) msg.get("usid"))
                        .put("sessionsList", waitingGames)
                );
                break;
            case "getGameSessions":                                                         // Отвечаем ядру на запрос списка ожидающих сессий
                waitingGames = getWaitingGamesSessions(uid, "waitingForPlayers");

                sendClientMessage(from, "setGameSessions", new JSONObject()
                        .put("clientAddress", (String) msg.get("clientAddress"))
                        .put("usid", (String) msg.get("usid"))
                        .put("sessionsList", waitingGames)
                );
                break;
            case "createNewGame":                                                         // Создаём ноаую игру и идём дальше на подключение к ней
                if (((LinkedTreeMap<String, Object>)((LinkedTreeMap<String, Object>)msg.get("params")).get("players")).size() < 2) {        // В игре должно быть не меньше двух игроков
                    sendClientMessage(from, "error", new JSONObject()
                            .put("action","error")
                            .put("text","Players count must be more 1")
                            .put("clientAddress", (String) msg.get("clientAddress"))
                            .put("usid", (String) msg.get("usid"))
                    );

                    return;
                }

                Integer humans = 0;
                Integer ais    = 0;
                Integer playerNN = 0;
                LinkedTreeMap<String, Object> params = (LinkedTreeMap<String, Object>)msg.get("params");
                players = new ArrayList<>();

                for (Object playerType : ((LinkedTreeMap<String, Object>)((LinkedTreeMap<String, Object>)msg.get("params")).get("players")).values()){
                    HashMap<String, String> player = new HashMap<>();
                    player.put("number", String.valueOf(playerNN));                     // Номер по порядку
                    player.put("type", (String) playerType);                            // Тип игрока
                    player.put("csid", null);                                           // Идентификатор клиентской сессии
                    if (((String) playerType).equals("human")) {
                        humans++;
                        player.put("name", "Human "+humans);
                    }else {
                        ais++;
                        player.put("name", "ИИ "+ais);
                    }
                    playerNN++;
                    players.add(player);
                }

                if (humans < 1) {
                    sendClientMessage(from, "error", new JSONObject()
                            .put("action","error")
                            .put("text","Humans count must be more 0")
                            .put("clientAddress", (String) msg.get("clientAddress"))
                            .put("usid", (String) msg.get("usid"))
                    );

                    return;
                }

                gsid = gameSessions.create();                                            // Создаём игровую сессию

                if (gsid == null) {                                                      // Если не удалось создать игровую сессию отправляем на core ошибку
                    sendClientMessage(from, "error", new JSONObject()
                            .put("action","error")
                            .put("text","Can't create new game session")
                            .put("clientAddress", (String) msg.get("clientAddress"))
                            .put("usid", (String) msg.get("usid"))
                    );

                    return;
                }

                gameAddress = gameSessions.getAddress(gsid);
                MessageConsumer<Object> mc = eb.localConsumer(gameAddress, this::onClientMessage);         // Адрес игры на шине (на адрес игры подписан только вертикл игры)

                gameSessions.setValue(gsid, "consumer", mc);                                           // Адрес игры на шине (на адрес игры подписан только вертикл игры)
                gameSessions.setValue(gsid, "owner", msg.get("usid"));                                 // Идентификатор сессии создателя игровой сессии
                gameSessions.setValue(gsid, "name", params.get("gameName"));                           // Имя игровой сессии
                gameSessions.setValue(gsid, "winLineLen", Integer.valueOf((String) params.get("winLineLen")));         // Длина линии победы
                gameSessions.setValue(gsid, "field", null);                                      // Игровое поле
                gameSessions.setValue(gsid, "fieldX", Integer.valueOf((String) params.get("fieldSizeX")));             // Ширина игрового поля
                gameSessions.setValue(gsid, "fieldY", Integer.valueOf((String) params.get("fieldSizeY")));             // Высота игрового поля
                gameSessions.setValue(gsid, "players", players);                                       // Участники игры с текущми статусами и параметрами
                gameSessions.setValue(gsid, "playersCount", Integer.valueOf((String) params.get("playersCount")));     // Общее количество игроков
                gameSessions.setValue(gsid, "humansCount", humans);                                    // Общее количество людей
                gameSessions.setValue(gsid, "availableSeats", humans);                                 // Свободно мест для людей
                gameSessions.setValue(gsid, "turnOf", 0);                                        // Участник, ход которого ожидаем
                gameSessions.setStatus("waitingForPlayers", gsid);                                   // Устанавливаем статус "ожидание игроков"
                action = "joinToGame";                                                                      // Выставляем action в joinToGame просто для порядка и единообразия
                msg.put("action", action);                                                                  // Выставляем action в joinToGame просто для порядка и единообразия
                msg.put("gsid", gsid);                                                                      // Добавляем в сообщение идентификатор игровой сессии

                sendClientMessage(from, "newGameSession", new JSONObject()                            // Отправляем кору запрос на регистрацию адреса игровой сессии
                        .put("address",gameAddress)
                        .put("gsid", gsid)
                );
//                break;                                                                                    // Не прерываем swith после создания игры и сразу переходим на подключение к ней игрока
            case "joinToGame":                                                                                  // Подключение к существующей игре

                System.out.println("Joing to game: "+msg.toString());

                if (gameSessions.getSession(gsid) == null) {                                                    // Если нет такой игровой сессии
                    log.debug(localAddress+"::onClientMessage: no session with gsid: "+gsid);

                    sendClientMessage(from, "error", new JSONObject()
                            .put("text","Session "+gsid+" not exists")
                            .put("clientAddress", (String) msg.get("clientAddress"))
                            .put("usid", (String) msg.get("usid"))
                    );
                    return;
                }

                if (msg.get("return") != null) {                                        // Переподключаем отвалившегося клиента
                    HashMap<String, String> player = getPlayerByUid(uid, gsid);

                    if (player == null) {
                        sendClientMessage(from, "notInGame", new JSONObject()
                                .put("usid", uid)
                                .put("gsid", gsid)
                                .put("game", "TicTacToe")
                                .put("clientAddress", (String) msg.get("clientAddress"))
                        );
                        return;
                    }

                    // Если игра в статусе awaiting, то сделать всю математику и отправить сессию с модулем awaiting
                    // Если игра в статусе ready, то сделать всю математику и отправить сессию с модулем battlefield

                    // Для первого варианта nn взять из player.get("number")
                    // Для второго варианта youTurn вычислить из player.get("number") и gameSessions.getInteger(gsid, "turnOf")
                    // Так же в обоих случаях вернуть имя игрока (на клиенте в обработке restoreSession сделать восстановление имени из playerName)

                    return;
                }

                if (!gameSessions.getStatus(gsid).equals("waitingForPlayers")) {                                // Если статус сессии отличается от "ожидание игроков"
                    log.debug(localAddress+"::onClientMessage: no waiting players in session gsid: "+gsid);

                    sendClientMessage(from, "error", new JSONObject()
                            .put("text","Session "+gsid+" no waiting players")
                            .put("clientAddress", (String) msg.get("clientAddress"))
                            .put("usid", (String) msg.get("usid"))
                    );
                    return;
                }

                players = gameSessions.getPlayers(gsid);
                HashMap<String, String> cPlayer = null;

                for (HashMap<String, String> player : players){
                    if ((player.get("type").equals("human")) && (player.get("csid") == null)){
                        player.put("csid", uid);
                        player.put("clientAddress", (String) msg.get("clientAddress"));
                        player.put("name", (String) msg.get("playerName"));
                        Integer seats = gameSessions.getInteger(gsid, "availableSeats") - 1;
                        gameSessions.setValue(gsid, "availableSeats", seats);
//System.out.println("Seats = "+seats);
                        if (seats < 1) {
                            gameSessions.setStatus("ready", gsid);                                // Если сидалищные места закончились, меняем статус сессии
                        }
                        cPlayer = player;                                                                // Запоминаем объект игрока для формирования сообщения клиенту
                        break;
                    }
                }

                sendClientMessage(from, "setGameSession", new JSONObject()                        // Отправляем сессию игроку (комплектом данные для подключения модуля)
                        .put("gameAddress", gameSessions.getAddress(gsid))
                        .put("clientAddress", (String) msg.get("clientAddress"))
                        .put("usid", (String) msg.get("usid"))
                        .put("gsid", gsid)
                        .put("nn", (String) cPlayer.get("nn"))
                        .put("appName","TicTacToe")
                        .put("moduleName","awaiting")
                        .put("resourceId","TicTacToe/js/awaiting")
                );

                Integer nn = 0;
                Boolean youTurn = false;
                playersNames = getPlayersList(gsid);

                for (HashMap<String, String> player : players){                                     // Рассылаем обновление списка участников
                    if (gameSessions.getStatus(gsid).equals("ready")) {                                      // Если сбор участников завершён, рассылаем команду на старт
                        if (player.get("type").equals("human") && (player.get("csid") != null)){
                            if (nn == gameSessions.getInteger(gsid, "turnOf")) {
                                youTurn = true;
                            }else {
                                youTurn = false;
                            }
                            sendGameMessage((String) player.get("clientAddress"), gameSessions.getAddress(gsid),"startGame", new JSONObject()
                                    .put("gsid", gsid)
                                    .put("gameAddress", gameSessions.getAddress(gsid))
                                    .put("turnOf", gameSessions.getInteger(gsid, "turnOf"))
                                    .put("youTurn", youTurn)
                                    .put("youNum", nn)
                                    .put("playersNames", playersNames)
                                    .put("appName","TicTacToe")
                                    .put("moduleName","battlefield")
                                    .put("resourceId","TicTacToe/js/battlefield")
                            );
                        }
                        nn++;
                    }else {                             // Если собраны не все, только обновляем список участников
                        if (player.get("type").equals("human") && (player.get("csid") != null)){
//                            System.out.println("Send players to: "+(String) player.get("clientAddress"));
                            sendGameMessage((String) player.get("clientAddress"), gameSessions.getAddress(gsid), "playersList", new JSONObject()
                                    .put("gsid", gsid)
                                    .put("gameAddress", gameSessions.getAddress(gsid))
                                    .put("playersNames", playersNames)
                            );
                        }
                    }
                }

                gameSessions.setActivity(gsid);

                break;
            case "leaveGame":                                                                                   // Обработка запроса на выход из игры
                gameAddress = gameSessions.getAddress(gsid);
                HashMap<String, String> player = getPlayerByUid(uid, gsid);
                players = gameSessions.getPlayers(gsid);

                if (player == null) {
                    log.debug(localAddress+"::onClientMessage: player "+uid+" not in game "+gsid);
                    sendClientMessage(from, "error", new JSONObject()
                            .put("text", "Player "+uid+" not in game "+gsid)
                            .put("clientAddress", (String) msg.get("clientAddress"))
                            .put("usid", (String) msg.get("usid"))
                    );
                    return;
                }

                if (((String) player.get("type")).equals("human")) {                                            // Рассылаем сообщение другим игрокам
                    for (Map.Entry<String, String> playerInfo : getPlayersAdresses(gsid).entrySet()){
                        String clAddr = playerInfo.getKey();
                        if (!clAddr.equals((String) player.get("clientAddress"))) {
                            sendGameMessage(clAddr, gameAddress, "playerLeave", new JSONObject().put("nn", (String) player.get("number")).put("name", (String) player.get("name")));
                        }
                    }

                    gameSessions.setValue(gsid, "availableSeats", gameSessions.getInteger(gsid, "availableSeats")+1);

                    player.put("csid", null);               // Очищаем участника от данных отключенного игрока
                    player.put("clientAddress", null);
                    player.put("name", null);
                }

                if (gameSessions.getInteger(gsid, "availableSeats") == gameSessions.getInteger(gsid, "humansCount")) {  // Если все игроки вышли, удаляем сессию
                    log.debug(localAddress+"::onClientMessage: All players leave game "+gsid+". Session removed");
                    removeGameSession(gsid, "All players leave the game");
                }else {
                    gameSessions.setActivity(gsid);                                                                             // Обновляем время последней активности игры
                    playersNames = getPlayersList(gsid);
                    for (HashMap<String, String> cPlayer2 : players){                                     // Рассылаем обновление списка участников
                        if (cPlayer2.get("type").equals("human") && (cPlayer2.get("csid") != null)){
                            sendGameMessage((String) cPlayer2.get("clientAddress"), gameSessions.getAddress(gsid), "playersList", new JSONObject()
                                    .put("gsid", gsid)
                                    .put("gameAddress", gameSessions.getAddress(gsid))
                                    .put("playersNames", playersNames)
                            );
                        }
                    }
                }
                break;
            case "removeGameSession":                                                                                           // Обработка запроса на удаление игровой сессии
                if (gameSessions.getString(gsid, "owner").equals(uid)) {
                    log.debug(localAddress+"::onClientMessage: owner "+uid+" removed game session "+gsid);
                    removeGameSession(gsid, "Owner removed game session");
                }
                break;
            default:
                log.error(localAddress+"::onClientMessage: unknown action "+action+" from client="+uid+", address="+from);
                sendClientMessage(from,"error", new JSONObject().put("text","unknown action "+action));
        }
    }

    // Обработка сообщений от клиентов в рамках игровых сессий
    public void onClientMessage(Message<Object> ebMsg){

        System.out.println(localAddress+"::onClientMessage: Received message on address "+ebMsg.address()+": "+ebMsg.body());

        String gameAddress = ebMsg.address();
        String gsid = gameSessions.getUidByAddress(gameAddress);

        if (gsid == null){
            log.info("Session not found by address: "+gameAddress);
            return;
        }

        HashMap<String, Object> msg;

        try {
            msg = new Gson().fromJson(ebMsg.body().toString(), HashMap.class);
        }catch (Exception e){
            log.error(localAddress+"::onClientMessage: received wrong JSON-string ("+ebMsg.body().toString()+")");
            return;
        }

        String from   = (String) msg.get("from");
        String csid   = (String) msg.get("usid");
        String action = (String) msg.get("action");

        if (from == null){
            log.error(localAddress+"::onClientMessage: no from field in body ("+msg.toString()+")");
            return;
        }
        if (action == null) {
            log.error(localAddress + "::onClientMessage: no action field in body (" + msg.toString() + ")");
            sendGameMessage(from, gameAddress, "error", new JSONObject().put("text", "no action field in body"));
            return;
        }

        switch (action){
            case "getPlayersList":                                                              // Запрос писка участников игровой сессии
                ArrayList<HashMap<String, String>> playersNames = getPlayersList(gsid);
                sendGameMessage(from, gameAddress, "playersList", new JSONObject()
                        .put("gsid", gsid)
                        .put("gameAddress", gameAddress)
                        .put("playersNames", playersNames)
                );
                break;
            default:
                log.debug(localAddress+"::onClientMessage: unknown action: "+action);
                sendGameMessage(from, gameAddress,"error", new JSONObject().put("text", "unknown action: "+action));
        }
    }

    // Удаление игровой сессии
    public boolean removeGameSession(String gsid, String reason){

        HashMap<String, Object> ses = gameSessions.getSession(gsid);

        if (ses == null) { return false; }

        String gameAddress = gameSessions.getAddress(gsid);
        ArrayList<HashMap<String, String>> players = gameSessions.getPlayers(gsid);

        for (HashMap<String, String> player : players){                                             // Рассылаем всем подключенным игрокам сообщение о удалении игровой сессии
            if ((player.get("type").equals("human")) && (player.get("csid") != null)){
                sendGameMessage((String) player.get("clientAddress"), gameAddress, "sessionRemoved", new JSONObject().put("gsid", gsid).put("reason", reason));
            }
        }

        MessageConsumer<Object> mc = (MessageConsumer<Object>) ses.get("consumer");             // Снимаем подписку на адрес сессии
        mc.unregister();

        sendClientMessage("core", "removeGameSession", new JSONObject().put("gsid", gsid).put("address", (String) ses.get("address")).put("reason", reason));  // Отправляем уведомление ядру (что бы проверил клиентские сессии, и если надо, затёр подключение)

        gameSessions.remove(gsid);                                                              // Удаляем сессию из хранилища

        return true;
    }

    // Получение списка всех участников сессии (для рассылки на события подключения/отключения игроков)
    public ArrayList<HashMap<String, String>> getPlayersList(String gsid){

        ArrayList<HashMap<String, String>> res = new ArrayList<>();

        for (HashMap<String, String> player : gameSessions.getPlayers(gsid)){
            HashMap<String, String> item = new HashMap<>();
            if (((player.get("csid") != null) && (player.get("clientAddress") != null)) || (((String) player.get("type")).equals("ai"))){
                item.put("number", (String) player.get("number"));
                item.put("name", (String) player.get("name"));
                item.put("type", (String) player.get("type"));
                item.put("status", "ready");
            }else {
                item.put("number", (String) player.get("number"));
                item.put("name", (String) player.get("name"));
                item.put("type", (String) player.get("type"));
                item.put("status", "waiting");
            }

            res.add(item);
        }

        return res;
    }

    // Получение списка адресов всех активных игроков сессии (для массовой рассылки)
    public HashMap<String, String> getPlayersAdresses(String gsid){

        HashMap<String, String> res = new HashMap<>();

        for (HashMap<String, String> player : gameSessions.getPlayers(gsid)){
            if ((player.get("csid") != null) && (player.get("clientAddress") != null)){
                res.put((String) player.get("clientAddress"), (String) player.get("name"));
            }
        }

        return res;
    }

    // Получение игрока из списка игроков в игровой сессии
    public HashMap<String, String> getPlayerByUid(String uid, String gsid){
        for (HashMap<String, String> player : gameSessions.getPlayers(gsid)){
            if ((player.get("csid") != null) && (player.get("csid").equals(uid))){
                return player;
            }
        }

        return null;
    }

    public HashMap<String, Object> getWaitingGamesSessions(String uid, String status){     // Взять список игровых сессий в заданном статусе (если передан валидный uid клиента, то проставить, где он владелец)

        HashMap<String, Object> waitingGames = new HashMap<>();
        HashMap<String, String> waitingGame;

        for (String sid : gameSessions.getSessions().keySet()){
            if (gameSessions.getStatus(sid).equals(status)) {
                waitingGame = new HashMap<>();
                waitingGame.put("gsid", sid);
                waitingGame.put("address", gameSessions.getAddress(sid));
                waitingGame.put("name", gameSessions.getString(sid, "name"));
                waitingGame.put("playersCount", gameSessions.getInteger(sid, "playersCount").toString());
                waitingGame.put("availableSeats", gameSessions.getInteger(sid, "availableSeats").toString());
                if (gameSessions.getString(sid, "owner").equals(uid)) {
                    waitingGame.put("owner", "true");
                }else {
                    waitingGame.put("owner", "false");
                }

                waitingGames.put(sid, waitingGame);
            }
        }

        return waitingGames;
    }

    // Отправление шаблонного сообщения клиенту
    public void sendClientMessage(String to, String action,JSONObject msg){
        msg.put("from", localAddress);
        msg.put("action", action);
        eb.send(to, msg.toString());
        log.debug(localAddress+"::sendClientMessage: sended message: "+msg.toString());
    }

    // Отправление шаблонного сообщения клиенту
    public void sendGameMessage(String to, String from, String action,JSONObject msg){

        System.out.println(localAddress+"::sendGameMessage: to="+to+", from="+from+", msg="+msg.toString());

        msg.put("from", from);
        msg.put("action", action);
        eb.send(to, msg.toString());
        log.debug(localAddress+"::sendGameMessage: sended message: "+msg.toString());
    }

    // Подготовка списка игровых сессий ожидающих подключение игрока
    private HashMap<String, Object> getAwaitingSessions(){

        HashMap<String, Object> list = new HashMap<>();

        if (gameSessions.size() > 0) {
            gameSessions.getSessions().forEach((uid, ses) -> {
                if ((((HashMap<String, Object>) ses).get("status") != null) && (((String)((HashMap<String, Object>) ses).get("status")).equals("awaiting"))){
                    list.put(uid, ses);
                }
            });
        }

        return list;
    }
}

