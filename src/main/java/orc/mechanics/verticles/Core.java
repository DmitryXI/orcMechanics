package orc.mechanics.verticles;

import com.google.gson.Gson;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.shareddata.LocalMap;
import orc.mechanics.SessionsManager;
import orc.mechanics.ClientSessionsManager;
import org.json.JSONObject;

import java.util.HashMap;


// Ядро платформы: основной процессор всего кроме непосредственно классов игр
public class Core extends AbstractVerticle {

    private Logger                   log                    = LoggerFactory.getLogger(Core.class);      // Логер
    private String                   localAddress           = "core";                                   // Локальный адрес вертикла
    private LocalMap<String, String> verticlesAddresses     = null;                                     // Массив персональных адресов вертиклов на шине данных
    private LocalMap<String, String> gamesList              = null;                                     // Список игр
    private LocalMap<String, String> clientSessionsList     = null;                                     // Список активных клиентских сессий (для проверки сессии клиента другими вертиклами)
    private EventBus                 eb                     = null;                                     // Шина данных для обмена сообщениями вертиклами между собой и вертиклов с веб-клиентами
    private ClientSessionsManager    clientSessions         = null;                                     // Менеджер сессий клиентов
//    private SessionsManager          gameSessions           = null;                                     // Менеджер сессий игр



    @Override
    public void start(Promise<Void> startPromise) throws Exception {
        log.info("Core verticle started");

        clientSessions      = new ClientSessionsManager();                                              // Менеджер сессий клиентов
//        gameSessions        = new SessionsManager(null);                                     // Менеджер сессий игр

        eb                  = vertx.eventBus();                                                         // Шина данных для обмена сообщениями вертиклами между собой и вертиклов с веб-клиентами
        verticlesAddresses  = vertx.sharedData().getLocalMap("verticlesAddresses");                  // Подключаем общий массив адресов вертиклов
        gamesList           = vertx.sharedData().getLocalMap("gamesList");                           // Подключаем список игр
        clientSessionsList  = vertx.sharedData().getLocalMap("clientSessionsList");                  // Подключаем общий список клиентских сессий

        verticlesAddresses.put(localAddress, "core");                                                // Регистрируем адрес в общем списке
        eb.localConsumer(localAddress, this::onMessage);                                                // Подписываемся на сообщения от Server
        clientSessions.setTtl(600);                                                                     // Устанавливаем время жизни неактивной клиентской сессии в 10 минут

        if (clientSessions.getTtl() > 0) {                                                              // Проверяем наличие критически неактивных сессий по таймеру и удаляем их
            vertx.setPeriodic(
                    clientSessions.getTtl(),
                    h -> {
                        HashMap<String, Object> doomeds = this.clientSessions.removeTheDead();

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

//        System.out.println("Core received local message: "+ebMsg.body());

        HashMap<String, Object> msg;
        String                  from;

        try {
            msg = new Gson().fromJson(ebMsg.body().toString(), HashMap.class);
        }catch (Exception e){
            log.error("Core::onLocalMessage: received wrong JSON-string ("+ebMsg.body().toString()+")");
            return;
        }

        from = (String) msg.get("from");            // From сразу выносим в string

        if (from == null){
            log.error("Core::onLocalMessage: no from field in body ("+ebMsg.body().toString()+")");
            return;
        }
//        if ((from == null) || !verticlesAddresses.containsKey(from)) {
//            log.error("Core::onLocalMessage: not registered address: "+msg.get("from"));
//            return;
//        }

        if (from.equals("server")) {
            onServerMessage(msg);
        } else if (gamesList.containsKey(from)) {
            onGamesMessage(msg);
        } else if ((from.length() == 14) && (from.substring(0,2).equals("cl"))) {
            onClientMessage(msg);
        } else {
            log.error("Core::onLocalMessage: Unexpected sender: "+from);
            return;
        }
    }

    // Обработка внутренних сообщений от Server
    public void onServerMessage(HashMap<String, Object> msg){

        if (msg.get("action") == null){
            log.error("Core::onServerMessage: no action field in body ("+msg.toString()+")");
            return;
        }

        HashMap<String, Object> ses = null;
        String uid     = (String) msg.get("usid");
        String host    = (String) msg.get("host");
        String port    = (String) msg.get("port");
        String address = (String) msg.get("address");
        String reason  = null;

        switch ((String) msg.get("action")){
            case "clientRegistration":                                          // Запрос клиентской сессии

                if (uid != null) {                                              // Если передан идентификатор сессии, пробуем её найти
//                    System.out.println("Try restoring session");
                    String clientId = clientSessions.getClientId(uid);

                    if (clientId != null) {
                        String clid = (String) msg.get("clid");
                        if (clientId.equals(clid)) {
                            String status = clientSessions.getStatus(uid);
                            String hpUid = clientSessions.getSessionUidByHostport(host+":"+port);
                            if (((hpUid == null) && status.equals("closed")) || (hpUid == uid)) {
                                clientSessions.setHostPort(host, port, uid);
                                clientSessions.setStatus("registration", uid);
                                clientSessions.setActivity(uid);
                                ses = clientSessions.getSession(uid);
                            }else {
                                log.error("Core::onServerMessage: session for "+host+":"+port+" already exists, but sessions uids not equals or not exists, but status requaried sesssion by uid not closed");
                                return;
                            }
                        }
                    }
                }

                if (ses != null) {                                                                          // Отправляем старую сессию
//                    System.out.println("Send old session: "+uid);

                    eb.send("server", new JSONObject()
                            .put("from", localAddress)
                            .put("action", "newClientSession")
                            .put("usid", uid)
                            .put("address", clientSessions.getAddress(uid))
                            .toString());
                } else {                                                                                    // Создаём новую сессию
//                    System.out.println("Creating new session");

                    uid = clientSessions.create();
                    clientSessions.setStatus("registration", uid);
                    clientSessions.setClientId((String) msg.get("clid"), uid);
                    clientSessions.setHostPort(host, port, uid);

                    if (uid != null){
                        eb.send("server", new JSONObject()
                                .put("from", localAddress)
                                .put("action", "newClientSession")
                                .put("usid", uid)
                                .put("address", clientSessions.getAddress(uid))
                                .toString());
                    }else {
                        log.error("Core::onServerMessage: Can't create session for client "+host+":"+port);
                    }
                }
                break;
            case "clientConfirm":                                                 // Подтверждение сессии клиентом
//                System.out.println("Confirm client session");
                String clientId = clientSessions.getClientId(uid);

                if (clientId != null) {
                    String clid = (String) msg.get("clid");
                    if (clientId.equals(clid)) {
                        if ((address != null) && (address.equals(clientSessions.getAddress(uid)))) {
                            ses = clientSessions.getSessionByHostport(host + ":" + port);
                            if (ses == clientSessions.getSession(uid)) {
                                clientSessions.setStatus("ready", uid);
                                clientSessions.setActivity(uid);
                            } else { ses = null; reason = "host:port not equals"; }
                        } else { reason = "address not equals"; }
                    } else { reason = "clientId not equals"; }
                } else { reason = "no clientId in session"; }

                if (ses != null) {

//                    clientSessionsList.put(uid, clientSessions.getStatus(uid));        // Добавляем id и статус клиентской сессии в общедоступный список

                    eb.send(address, new JSONObject()
                            .put("from", localAddress)
                            .put("action", "serverConfirm")
                            .put("usid", uid)
                            .put("address", address)
                            .put("status", clientSessions.getStatus(uid))
                            .toString());
                }else {
                    log.error("Core::onServerMessage: Can't confirm session ("+reason+") for "+msg.toString());
                    return;
                }
                break;
            case "clientUnregister":                                                 // Снятие регистрации с сессии клиента
//                System.out.println("Unregister client session");

                ses = clientSessions.getSessionByAddress(address);
                if (ses != null) {
                    if (ses == clientSessions.getSessionByHostport(host + ":" + port)) {
                        uid = (String) ses.get("uid");
                        clientSessions.setStatus("unregistered", uid);
                        clientSessions.setActivity(uid);
                    } else { ses = null; reason = "sessions host:port not equals"; }
                } else { reason = "session with address "+address+" not exists"; }

                if (ses == null) {
                    log.error("Core::onServerMessage: Can't unregister session ("+reason+") for "+msg.toString());
                    return;
                }
                break;
            case "clientSocketClosed":                                                 // Проверка статуса клиентской сессии при закрытии сокета
                System.out.println("Checking client sessions on closing socket");

                ses = clientSessions.getSessionByHostport(host + ":" + port);
                if (ses != null) {
                    uid = (String) ses.get("uid");
                    clientSessions.setStatus("closed", uid);
                    clientSessions.setHostPort(null, null, uid);
                    System.out.println("Session from host:port "+host+":"+port+" closed");
                }
                break;
            default:
                log.error("Core::onServerMessage: Unexpected action: "+(String) msg.get("action"));
        }
    }

    // Обработка внешних сообщений от клиентов
    public void onClientMessage(HashMap<String, Object>msg){

        System.out.println("Core::onClientMessage: Core received client message: "+msg.toString());

        String from   = (String) msg.get("from");
        String action = (String) msg.get("action");
        String uid    = (String) msg.get("usid");

        if (from == null){
            log.error("Core::onClientMessage: no from field in body ("+msg.toString()+")");
            return;
        }
        if (action == null){
            log.error("Core::onClientMessage: no action field in body ("+msg.toString()+")");
            sendClientMessage(from, "error", new JSONObject().put("text","no action field in body"));
            return;
        }
        if (uid == null){
            log.error("Core::onClientMessage: no usid field in body ("+msg.toString()+")");
            sendClientMessage(from, "error", new JSONObject().put("text","no usid field in body"));
            return;
        }

        HashMap<String, Object> ses = clientSessions.getSession(uid);

        if (ses == null){
            log.error("Core::onClientMessage: session "+uid+" not exists");
            sendClientMessage(from, "error", new JSONObject().put("text","session "+uid+" not exists"));
            return;
        }

        clientSessions.setActivity(uid);                                    // Обновляем время последней активности клиентской сессии

        JSONObject resp = new JSONObject();

        switch (action){
            case "setPlayerName":
                ses.put("playerName", (String) (msg.get("name")));
                sendClientMessage(from, "setPlayerName", new JSONObject().put("name", (String) ses.get("playerName")));
                break;
            case "getGamesList":
//                System.out.println("Список игр: " + new JSONObject(gamesList).toString());
                resp.put("list", new JSONObject());
                gamesList.forEach((String gameAdderss, String gameName) -> {
                    resp.getJSONObject("list").put(gameAdderss, gameName);
                });
                sendClientMessage(from,"setGamesList", resp);
                break;
            case "getGameEntrance":                                         // Запрашиваем форму входа для любой игры транзитом через core для контроля сессий и любого другого контроля
                resp.put("game", (String) msg.get("game"))
                    .put("usid", (String) msg.get("usid"))
                    .put("clientAddress", (String) msg.get("from"));
                sendClientMessage((String) msg.get("game"),"getGameEntrance", resp);
                break;
            default:
                log.error("Core::onClientMessage: unknown action "+action+" from client="+uid+", address="+from);
                sendClientMessage(from,"error", new JSONObject().put("text","unknown action "+action));
        }
    }

    // Обработка сообщений от игровых вертиклов
    public void onGamesMessage(HashMap<String, Object>msg){

        System.out.println(localAddress+"onGamesMessage: Received message from game: "+msg.toString());
    }

    // Отправление шаблонного сообщения клиенту
    public void sendClientMessage(String to, String action,JSONObject msg){
        msg.put("from", localAddress);
        msg.put("action", action);
        eb.send(to, msg.toString());
        log.debug("Core::sendClientMessage: sended message: "+msg.toString());
    }
}


