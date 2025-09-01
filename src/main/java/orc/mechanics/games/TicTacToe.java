package orc.mechanics.games;

import com.google.gson.Gson;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.shareddata.LocalMap;
import orc.mechanics.GameSessionsManager;
import orc.mechanics.verticles.Core;
import org.json.JSONObject;
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
            log.error(localAddress+"::onLocalMessage: received wrong JSON-string ("+ebMsg.body().toString()+")");
            return;
        }

        String from   = (String) msg.get("from");
        String action = (String) msg.get("action");

        if (from == null){
            log.error(localAddress+"::onClientMessage: no from field in body ("+msg.toString()+")");
            return;
        }
        if (action == null){
            log.error(localAddress+"::onClientMessage: no action field in body ("+msg.toString()+")");
            sendClientMessage(from, "error", new JSONObject().put("text","no action field in body"));
            return;
        }

        if (from.equals("server")) {
            onServerMessage(msg);
        } else if (from.equals("core")) {
            onCoreMessage(msg);
        } else if ((from.length() == 14) && (from.substring(0,2).equals("gm"))) {
            onClientMessage(msg);
        } else {
            log.error(localAddress+"::onLocalMessage: Unexpected sender: "+from);
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
        String uid    = (String) msg.get("usid");

        if (uid == null){
            log.error(localAddress+"::onClientMessage: no usid field in body ("+msg.toString()+")");
            sendClientMessage(from, "error", new JSONObject().put("text","no usid field in body"));
            return;
        }


        switch (action){
            case "getGameEntrance":
//                ses.put("zzzz", (String) (msg.get("name")));
//                sendClientMessage(from, "setPlayerName", new JSONObject().put("name", (String) ses.get("playerName")));
                // Подготовить форму со список игровых сессий и созданием новой игры (наверно, лучше разделить на левый и правы блок...)
                // или сделать кнопку переключения на уровне клиента
                // И написать обработку всего этого....
                break;
            default:
                log.error(localAddress+"::onClientMessage: unknown action "+action+" from client="+uid+", address="+from);
                sendClientMessage(from,"error", new JSONObject().put("text","unknown action "+action));
        }
    }

    // Обработка сообщений от клиентов в рамках игровых сессий
    public void onClientMessage(HashMap<String, Object> msg){

        System.out.println(localAddress+"onClientMessage: Received message from game: "+msg.toString());
    }

    // Отправление шаблонного сообщения клиенту
    public void sendClientMessage(String to, String action,JSONObject msg){
        msg.put("from", localAddress);
        msg.put("action", action);
        eb.send(to, msg.toString());
        log.debug(localAddress+"::sendClientMessage: sended message: "+msg.toString());
    }
}

