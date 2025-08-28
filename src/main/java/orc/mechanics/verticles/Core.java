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
    private EventBus                 eb                     = null;                                     // Шина данных для обмена сообщениями вертиклами между собой и вертиклов с веб-клиентами
    private ClientSessionsManager    clientSessions         = null;                                     // Менеджер сессий клиентов
    private HashMap<String, Object>  disabledClientSessions = null;                                     // Хранилище отключенных сессий клиентов
    private SessionsManager          gameSessions           = null;                                     // Менеджер сессий игр



    @Override
    public void start(Promise<Void> startPromise) throws Exception {
        log.info("Core verticle started");

        clientSessions      = new ClientSessionsManager();                                              // Менеджер сессий клиентов
        gameSessions        = new SessionsManager(null);                                        // Менеджер сессий игр

        eb                  = vertx.eventBus();                                                         // Шина данных для обмена сообщениями вертиклами между собой и вертиклов с веб-клиентами
        verticlesAddresses  = vertx.sharedData().getLocalMap("verticlesAddresses");                  // Подключаем общий массив адресов вертиклов

        verticlesAddresses.put(localAddress, "core");                                                // Регистрируем адрес в общем списке
        eb.localConsumer(localAddress, this::onLocalMessage);                                           // Подписываемся на сообщения от Server

        startPromise.complete();
    }


    // Обработка внутренних сообщений
    public void onLocalMessage(Message<Object> ebMsg){

        System.out.println("Core received local message: "+ebMsg.body());

        HashMap<String, Object> msg;

        try {
            msg = new Gson().fromJson(ebMsg.body().toString(), HashMap.class);
        }catch (Exception e){
            log.error("Core::onLocalMessage: received wrong JSON-string ("+ebMsg.body().toString()+")");
            return;
        }
        if (msg.get("from") == null){
            log.error("Core::onLocalMessage: no from field in body ("+ebMsg.body().toString()+")");
            return;
        }
        if ((msg.get("from") == null) || !verticlesAddresses.containsKey(msg.get("from"))) {
            log.error("Core::onLocalMessage: not registered address: "+msg.get("from"));
            return;
        }

        switch ((String) msg.get("from")){
            case "server":
                onServerMessage(msg);
                break;
            default:
                log.error("Unexpected sender: "+msg.get("from"));
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

        switch ((String) msg.get("action")){
            case "clientRegistration":                                          // Запрос клиентской сессии

                if (uid != null) {                                              // Если передан идентификатор сессии, пробуем её найти
                    System.out.println("Try restoring session");
                    String clientId = clientSessions.getClientId(uid);

                    if (clientId != null) {
                        String clid = (String) msg.get("clid");
                        if (clientId.equals(clid)) {
                            if (clientSessions.getSessionUidByHostport(host+":"+port) == null) {
                                clientSessions.setHostPort(host, port, uid);
                                clientSessions.setStatus("registration", uid);
                                clientSessions.setActivity(uid);
                                ses = clientSessions.getSession(uid);
                            }else {
                                log.error("Core::onServerMessage: session for "+host+":"+port+" already exists: "+clientSessions.getSessionUidByHostport(host+":"+port));
                                return;
                            }
                        }
                    }
                }

                if (ses == null) {                                                                          // Если идентификатора сессии нет, создаём новую
                    System.out.println("Creating new session");

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
                System.out.println("Confirm client session");
                String clientId = clientSessions.getClientId(uid);
                String reason = null;

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

//            case "clientSessionRequest":                                        // Запрос на создание новой сессии
//                log.debug("Creating empty session");
//
//                uid = clientSessions.create();
//
//                if (uid != null){
//                    eb.send("server", new JSONObject()
//                            .put("from", localAddress)
//                            .put("action", "newClientSession")
//                            .put("usid", uid)
//                            .put("address", clientSessions.getAddress(uid))
//                            .toString());
//                }
//                break;
//            case "confirmingClientSession":                                     // Подтверждение сессии клиентом
//                log.debug("Confirming empty session");
//
//                String address = (String) msg.get("address");
//
//                ses = clientSessions.getSessionByAddress(address);
//
//                if (ses == null) {                                              // Если подтверждённая клиентом сессия не найдена - дать команду на удаление её адреса
//                    log.error("Core::onServerMessage: no session for address: "+address);
//                    eb.send("server", new JSONObject()
//                            .put("from", localAddress)
//                            .put("action", "removeClientAddress")
//                            .put("address", address)
//                            .toString());
//                    return;
//                }
//
//                uid = (String) ses.get("uid");
//
//                if (clientSessions.getStatus(uid).equals("confirmed")) {        // Если это запрос на подтверждение уже подтверждённой и активной сессии - сверить хост:порт клиента
//                    // Здесь проверка на наличие активного подключения (хост:порт)
//                }else {                                                                                                         // Если сессия ранее не подтверждалась - подтверждаем безусловно
//                    clientSessions.setHostPort((String) msg.get("host"), Integer.valueOf((String) msg.get("port")), uid);
//                    clientSessions.setStatus("confirmed", uid);
//                    clientSessions.setActivity(uid);
//                    eb.consumer(address, this::onClientMessage);                                                                // Подписываемся на сообщения от клиента
//
//                    eb.send(address, new JSONObject()
//                            .put("from", localAddress)
//                            .put("action", "confirmingClientSession")
//                            .put("usid", uid)
//                            .put("address", address)
//                            .toString());
//                }
//                break;
            default:
                log.error("Core::onServerMessage: Unexpected action: "+(String) msg.get("action"));
        }
    }

    // Обработка сообщений от веб-клиентов
    public void onClientMessage(Message<Object> ebMsg){

        HashMap<String, Object> msg;

        try {
            msg = new Gson().fromJson(ebMsg.body().toString(), HashMap.class);
        }catch (Exception e){
            log.error("Core::onClientMessage: received wrong JSON-string as body ("+ebMsg.body().toString()+")");
            return;
        }

        System.out.println("Core received client message: "+msg.toString());

        HashMap<String, Object> ses = null;
        String uid     = (String) msg.get("usid");
        String action  = (String) msg.get("action");
        String address = ebMsg.address();

        if (action == null) {
            log.error("Core::onClientMessage: received wrong body without action: "+msg.toString());
            eb.send(address, new JSONObject().put("from", localAddress).put("action", "error").put("text", "Received wrong body without action").toString());
            return;
        }
        if (uid == null) {
            log.error("Core::onClientMessage: received wrong body without usid: "+msg.toString());
            eb.send(address, new JSONObject().put("from", localAddress).put("action", "error").put("text", "Received wrong body without usid").toString());
            return;
        }



        switch (action){
//            case "confirmClientSession":
//                ses = clientSessions.getSessionByAddress(address);
//
//                if (ses == null) {
//                    log.error("Core::onClientMessage: no session for address: "+address);
//                    eb.send(address, new JSONObject().put("from", localAddress).put("action", "error").put("text", "No session for address: "+address).toString());
//                    return;
//                }
//                if (msg.get("clid") == null) {
//                    log.error("Core::onClientMessage: received wrong body without clid: "+msg.toString());
//                    eb.send(address, new JSONObject().put("from", localAddress).put("action", "error").put("text", "Received wrong body without clid").toString());
//                    return;
//                }
//                uid = (String) ses.get("uid");
//                if (!uid.equals(msg.get("usid").toString())) {
//                    log.error("Core::onClientMessage: uid in session "+(String) msg.get("uid")+" not equals usid in msg "+(String) msg.get("usid"));
//                    eb.send(address, new JSONObject().put("from", localAddress).put("action", "error").put("text", "usid not equals").toString());
//                    return;
//                }
//
//                clientSessions.setActivity(uid);
//                clientSessions.setStatus("ready", uid);
//                clientSessions.setClientId(msg.get("clid").toString(), uid);
//
//                eb.send(address, new JSONObject()
//                        .put("from", localAddress)
//                        .put("action", "confirmClientSession")
//                        .put("usid", uid)
//                        .put("address", address)
//                        .toString());
//
//                break;
//            case "restoreClientSession":
//                    // Проверить наличие сессии, сверить clid, если всё ок, то скопировать
//                    // все поля кроме адреса, активити ещё может чего-то и удалить старую сессию
//                break;
            default:
                log.error("Core::onClientMessage: Unexpected action: "+action);
        }
    }
}


