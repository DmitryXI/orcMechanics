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
        ArrayList<HashMap<String, String>> players;                 // Список участников игры

        if (uid == null){
            log.error(localAddress+"::onClientMessage: no usid field in body ("+msg.toString()+")");
            sendClientMessage(from, "error", new JSONObject().put("text","no usid field in body"));
            return;
        }


        switch (action){
            case "getGameEntrance":                                                         // Отвечаем ядру на запрос формы входа

                HashMap<String, Object> sessions = gameSessions.getSessions();
                HashMap<String, Object> waitingGames = new HashMap<>();
                HashMap<String, String> waitingGame;

                for (String sid : sessions.keySet()){
                    if (gameSessions.getStatus(sid).equals("waitingForPlayers")) {
                        waitingGame = new HashMap<>();
                        waitingGame.put("gsid", sid);
                        waitingGame.put("address", gameSessions.getAddress(sid));
                        waitingGame.put("name", gameSessions.getString(sid, "name"));
                        waitingGame.put("playersCount", gameSessions.getInteger(sid, "playersCount").toString());
                        waitingGame.put("availableSeats", gameSessions.getInteger(sid, "availableSeats").toString());
                        waitingGames.put(sid, waitingGame);
                    }
                }

//                waitingGame = new HashMap<>();
//                waitingGame.put("gsid", "WR6IZJST78M0");
//                waitingGame.put("address", "gmWR6IZJST78M0");
//                waitingGame.put("name", "Поиграть 0");
//                waitingGame.put("playersCount", "2");
//                waitingGame.put("availableSeats", "1");
//                waitingGames.put("WR6IZJST78M0", waitingGame);
//                waitingGame = new HashMap<>();
//                waitingGame.put("gsid", "WR6IZJST78M1");
//                waitingGame.put("address", "gmWR6IZJST78M1");
//                waitingGame.put("name", "Поиграть 1");
//                waitingGame.put("playersCount", "3");
//                waitingGame.put("availableSeats", "3");
//                waitingGames.put("WR6IZJST78M1", waitingGame);
//                waitingGame = new HashMap<>();
//                waitingGame.put("gsid", "WR6IZJST78M2");
//                waitingGame.put("address", "gmWR6IZJST78M2");
//                waitingGame.put("name", "Поиграть 2");
//                waitingGame.put("playersCount", "4");
//                waitingGame.put("availableSeats", "2");
//                waitingGames.put("WR6IZJST78M2", waitingGame);

                sendClientMessage(from, "setGameEntrance", new JSONObject()
                        .put("appName","TicTacToe")
                        .put("moduleName","entrance")
                        .put("resourceId","TicTacToe/js/entrance")
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
                LinkedTreeMap<String, Object> params = (LinkedTreeMap<String, Object>)msg.get("params");
                players = new ArrayList<>();

                for (Object playerType : ((LinkedTreeMap<String, Object>)((LinkedTreeMap<String, Object>)msg.get("params")).get("players")).values()){
                    HashMap<String, String> player = new HashMap<>();
                    player.put("type", (String) playerType);                            // Тип игрока
                    player.put("csid", null);                                           // Идентификатор клиентской сессии
                    if (((String) playerType).equals("human")) {
                        humans++;
                        player.put("name", "Human "+humans);
                    }else {
                        ais++;
                        player.put("name", "ИИ "+ais);
                    }
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

                String gameAddress = gameSessions.getAddress(gsid);
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
                gameSessions.setValue(gsid, "availableSeats", humans);                                 // Свободно для мест для участников-людей
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
                        break;
                    }
                }

                ArrayList<String> playersNames = new ArrayList<>();
                for (HashMap<String, String> player : players){
                    playersNames.add((String) player.get("name"));
                }

                sendClientMessage(from, "setGameSession", new JSONObject()                        // Отправляем сессию игроку (комплектом данные для подключения модуля)
                        .put("gameAddress", gameSessions.getAddress(gsid))
                        .put("clientAddress", (String) msg.get("clientAddress"))
                        .put("usid", (String) msg.get("usid"))
                        .put("gsid", gsid)
                        .put("appName","TicTacToe")
                        .put("moduleName","awaiting")
                        .put("resourceId","TicTacToe/js/awaiting")
                );

                Integer nn = 0;
                Boolean youTurn = false;
                if (gameSessions.getStatus(gsid).equals("ready")) {                                      // Если сбор участников завершён, рассылаем команду на старт людям
//System.out.println("Game ready");
                    for (HashMap<String, String> player : players){
                        if (player.get("type").equals("human")){
//System.out.println("Player is human");
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
                            );
                        }
                        nn++;
                    }
                }

                gameSessions.setActivity(gsid);

                break;
            default:
                log.error(localAddress+"::onClientMessage: unknown action "+action+" from client="+uid+", address="+from);
                sendClientMessage(from,"error", new JSONObject().put("text","unknown action "+action));
        }
    }

    // Обработка сообщений от клиентов в рамках игровых сессий
    public void onClientMessage(Message<Object> ebMsg){

        System.out.println(localAddress+"::onClientMessage: Received message");

        String address = ebMsg.address();
        String gsid = gameSessions.getUidByAddress(address);

        if (gsid == null){
            sendClientMessage(address, "error", new JSONObject().put("text", "Session not found"));
        }

        HashMap<String, Object> msg;

        try {
            msg = new Gson().fromJson(ebMsg.body().toString(), HashMap.class);
        }catch (Exception e){
            log.error(localAddress+"::onClientMessage: received wrong JSON-string ("+ebMsg.body().toString()+")");
            return;
        }

        String from   = (String) msg.get("from");
        String action = (String) msg.get("action");

        if (from == null){
            log.error(localAddress+"::onClientMessage: no from field in body ("+msg.toString()+")");
            return;
        }
        if (action == null) {
            log.error(localAddress + "::onClientMessage: no action field in body (" + msg.toString() + ")");
            sendClientMessage(from, "error", new JSONObject().put("text", "no action field in body"));
            return;
        }

        sendClientMessage(from, "error", new JSONObject().put("text", "Not ready yet"));
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

