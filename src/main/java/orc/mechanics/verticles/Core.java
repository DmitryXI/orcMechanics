package orc.mechanics.verticles;

import com.google.gson.Gson;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.shareddata.LocalMap;
import org.json.JSONObject;

import java.util.HashMap;


// Ядро платформы: основной процессор всего кроме непосредственно классов игр
public class Core extends AbstractVerticle {

    private Logger                   log                    = LoggerFactory.getLogger(Core.class);      // Логер
    private String                   localAddress           = "core";                                   // Локальный адрес вертикла
    private LocalMap<String, String> verticlesAddresses     = null;                                     // Массив персональных адресов вертиклов на шине данных



    @Override
    public void start(Promise<Void> startPromise) throws Exception {
        log.info("Core verticle started");

        EventBus eb = vertx.eventBus();                                                                 // Шина данных для обмена сообщениями вертиклами между собой и вертиклов с веб-клиентами
        verticlesAddresses = vertx.sharedData().getLocalMap("verticlesAddresses");                   // Подключаем общий массив адресов вертиклов

        verticlesAddresses.put(localAddress, "core");                                                // Регистрируем адрес в общем списке
        eb.localConsumer(localAddress, this::onLocalMessage);                                           // Подписываемся на сообщения для сервера




//        eb.send("server", new JSONObject().put("from", localAddress).put("flood", "Server strted!").toString());
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

        System.out.println("Core received local message: "+ebMsg.body());
    }
}
