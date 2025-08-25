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
    private SessionsManager          gameSessions           = null;                                     // Менеджер сессий игр



    @Override
    public void start(Promise<Void> startPromise) throws Exception {
        log.info("Core verticle started");

        clientSessions      = new ClientSessionsManager();                                              // Менеджер сессий клиентов
        gameSessions        = new SessionsManager();                                                    // Менеджер сессий игр

        eb                  = vertx.eventBus();                                                         // Шина данных для обмена сообщениями вертиклами между собой и вертиклов с веб-клиентами
        verticlesAddresses  = vertx.sharedData().getLocalMap("verticlesAddresses");                  // Подключаем общий массив адресов вертиклов

        verticlesAddresses.put(localAddress, "core");                                                // Регистрируем адрес в общем списке
        eb.localConsumer(localAddress, this::onLocalMessage);                                           // Подписываемся на сообщения для сервера

        startPromise.complete();
    }


    // Обработка внутренних сообщений
    public void onLocalMessage(Message<Object> ebMsg){

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
        System.out.println("Core received local message: "+ebMsg.body());
    }

    public void onServerMessage(HashMap<String, Object> msg){

        String uid;
        HashMap<String, Object> ses;

        switch ((String) msg.get("action")){
            case "clientSessionRequest":
                log.debug("Creating empty session");

                uid = clientSessions.create();

                if (uid != null){
                    eb.send("server", new JSONObject()
                            .put("from", localAddress)
                            .put("action", "newClientSession")
                            .put("usid", uid)
                            .put("address", clientSessions.getAddress(uid))
                            .toString());
                }
                break;
            case "clientSessionConfirm":
                log.debug("Confirming empty session");

                String address = (String) msg.get("address");

                ses = clientSessions.getSessionByAddress(address);

                if (ses == null) {
                    log.error("Core::onServerMessage: no session for address: "+address);
                    eb.send("server", new JSONObject()
                            .put("from", localAddress)
                            .put("action", "removeClientAddress")
                            .put("address", address)
                            .toString());
                    return;
                }

                uid = (String) ses.get("uid");

                if (clientSessions.getStatus(uid).equals("confirmed")) {
                    // Здесь проверка на наличие активного подключения (хост:порт)
                }else {                                                                                                         // Если сессия ранее не подтверждалась - подтверждаем безусловно
                    clientSessions.setHostPort((String) msg.get("host"), Integer.valueOf((String) msg.get("port")), uid);
                    clientSessions.setStatus("confirmed", uid);
                    clientSessions.setActivity(uid);
                    eb.consumer(address, this::onClientMessage);                                                            // Подписываемся на сообщения от клиента

                    eb.send(address, new JSONObject()
                            .put("from", localAddress)
                            .put("action", "confirmClientSession")
                            .put("usid", uid)
                            .put("address", address)
                            .toString());
                }
                break;
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

        System.out.println("Core received client message...");
        System.out.println(msg.toString());
    }
}


