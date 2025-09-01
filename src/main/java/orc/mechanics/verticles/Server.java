package orc.mechanics.verticles;

import com.google.gson.Gson;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.shareddata.LocalMap;
import io.vertx.ext.bridge.PermittedOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.ext.web.handler.sockjs.BridgeEvent;
import io.vertx.ext.web.handler.sockjs.SockJSBridgeOptions;
import io.vertx.ext.web.handler.sockjs.SockJSHandler;
import io.vertx.rxjava.core.http.HttpHeaders;
import org.json.JSONObject;

import java.io.File;
import java.util.HashMap;

import static java.lang.System.in;


// Главный вертикл: держатель веб-сервера
// Адреса на шине данных: registration, server
public class Server extends AbstractVerticle {

    private Logger                  log                     = LoggerFactory.getLogger(Server.class);    // Логер
    private Integer                 ServerHttpPort          = 80;                                       // Номер порта, на котром веб-сервер будет слушать HTTP
    private String                  localAddress            = "server";                                 // Локальный адрес вертикла
    private LocalMap<String, String> verticlesAddresses     = null;                                     // Массив персональных адресов вертиклов на шине данных
    private EventBus                eb                      = null;                                     // Шина данных для обмена сообщениями вертиклами между собой и вертиклов с веб-клиентами
    private SockJSHandler           wsHandler               = null;                                     // Обработчик для вебсокета
    private SockJSBridgeOptions     wsHandlerBridgeOptions  = null;                                     // Опции обработчика для вебсокета
    private Router                  router                  = null;                                     // Основной маршрутизатор (для вебсервера)
    private Router                  contentRouter           = null;                                     // Дополнительный маршрутизатор для выдачи непубличного контента
    private Router                  wsRouter                = null;                                     // Дополнительный маршрутизатор (для вебсокета)
    private String                  contentFilesPath        = null;                                     // Путь к корню директории с контентом
    private HashMap<String, PermittedOptions> permittedAddresses = null;                                // Список разрешённых адресов (имён каналов)


    @Override
    public void start(Promise<Void> startPromise) throws Exception {
        log.info("Server verticle started");

        eb                      = vertx.eventBus();                                                     // Шина данных для обмена сообщениями вертиклами между собой и вертиклов с веб-клиентами
        verticlesAddresses      = vertx.sharedData().getLocalMap("verticlesAddresses");              // Подключаем общий массив адресов вертиклов
        verticlesAddresses.put(localAddress, "server");                                              // Регистрируем адрес в общем списке
        eb.localConsumer(localAddress, this::onLocalMessage);                                           // Подписываемся на сообщения для сервера

        contentFilesPath        = "./content/";                                                         // Путь к папке с закрытым веб-контентом
        permittedAddresses      = new HashMap<>();                                                      // Создаём пустое хранилище опций с разрешениями адресов
//        clientsAdresses         = new TreeMap<>();                                                      // Создаём пустое хранилище адресов клиентов
        router                  = Router.router(vertx);                                                 // Создаём основной роутер веб-сервера
        contentRouter           = Router.router(vertx);                                                 // Создаём подроутер для закрытого контента
        wsHandler               = SockJSHandler.create(vertx);                                          // Создаём хэндлер для веб-сокета
        wsHandlerBridgeOptions  = new SockJSBridgeOptions();                                            // Создаём объект с опциями для моста хэндлер -> обработчик
        wsRouter                = wsHandler.bridge(wsHandlerBridgeOptions, this::onClientMessage);      // Создаём мост хэндлер -> обработчик

        router.mountSubRouter("/eventbus", wsRouter);                                                 // Монтируем подроутер для веб-сокета
        router.mountSubRouter("/content", contentRouter);                                             // Монтируем подроутер для запросов закытого контента
        router.route().handler(StaticHandler.create().setCachingEnabled(false).setDefaultContentEncoding("utf-8"));     // Создаём статический обработчик к запросам открытого контента
        contentRouter.post().handler(this::onRequestContent);                                            // Цепляем обработчик к хэндлеру роутера для закрытого контента

        addEbPermit("general");                                                                   // Добавляем разрешение для приёма сообщений через веб-сокет на общий адрес веб-сервера

        for (String verAddress : verticlesAddresses.keySet()){                                            // Добавляем разрешения для адресов вертиклов на шине
            addEbPermit(verAddress);
            System.out.println("Added permit to address: "+verAddress);
        }

        // Запускаем веб-сервер
        vertx.createHttpServer().requestHandler(router).listen(ServerHttpPort).onComplete(http -> {
            if (http.succeeded()) {
                startPromise.complete();
                log.info("HTTP server started on port "+ServerHttpPort);
            } else {
                log.error("Error starting HTTP server");
                startPromise.fail(http.cause());
            }
        });
    }

    // Обработка внутренних сообщений
    public void onLocalMessage(Message<Object> ebMsg){

        HashMap<String, Object> msg;

        try {
            msg = new Gson().fromJson(ebMsg.body().toString(), HashMap.class);
        }catch (Exception e){
            log.error("Server::onLocalMessage: received wrong JSON-string ("+ebMsg.body().toString()+")");
            return;
        }
        if (msg.get("from") == null){
            log.error("Server::onLocalMessage: no from field in body ("+ebMsg.body().toString()+")");
            return;
        }
        if ((msg.get("from") == null) || !verticlesAddresses.containsKey(msg.get("from"))) {
            log.error("Server::onLocalMessage: not registered address: "+msg.get("from"));
            return;
        }

        switch ((String) msg.get("from")){
            case "core":
                onCoreMessage(msg);
                break;
            default:
                log.error("Unexpected sender: "+msg.get("from"));
        }
//        System.out.println("Server received local message: "+ebMsg.body());
    }

    public void onCoreMessage(HashMap<String, Object> msg){

        switch ((String) msg.get("action")){
            case "newClientSession":
//                log.debug("Server::onCoreMessage: Sending session, adding address permission");
//                System.out.println("Sending session, adding address permission for "+msg.get("address"));
                addEbPermit((String) msg.get("address"));                                                   // Добавляем разрешение для адреса новой сессии
                msg.put("from", "server");
                eb.send("general", (new JSONObject(msg)).toString());
                break;
            case "removeAddress":
//                log.debug("Server::onCoreMessage: removing client session address"+msg.get("address"));
//                System.out.println("removing client session address: "+msg.get("address"));
                delEbPermit((String) msg.get("address"));                                                   // Удаляем разрешение для адреса новой сессии
                break;
            default:
                log.error("Server::onCoreMessage: Unexpected action: "+(String) msg.get("action"));
        }
    }

    // Обработка клиентских сообщений
    public void onClientMessage(BridgeEvent event){

        switch (event.type()){
            case SOCKET_CREATED:
//                System.out.println("Received SOCKET_CREATED from "+event.socket().remoteAddress().host()+":"+event.socket().remoteAddress().port());
                break;
            case SOCKET_CLOSED:
//                System.out.println("Received SOCKET_CLOSED from "+event.socket().remoteAddress().host()+":"+event.socket().remoteAddress().port());
                onClientSocketClosed(event);
                break;
            case SOCKET_PING:
//                System.out.println("Received PING from "+event.socket().remoteAddress().host()+":"+event.socket().remoteAddress().port());
                break;
            case REGISTER:
//                System.out.println("Received REGISTER from address "+event.getRawMessage().getString("address"));
//                System.out.println(event.getRawMessage().toString());
                break;
            case REGISTERED:
//                System.out.println("Received REGISTERED from address "+event.getRawMessage().getString("address"));
                onClientRegistered(event);
                break;
            case UNREGISTER:
//                System.out.println("Received UNREGISTER from address "+event.getRawMessage().getString("address")+" and "+event.socket().remoteAddress().host()+":"+event.socket().remoteAddress().port());
                onClientUnRegister(event);
                break;
            case SEND:
//                System.out.println("Received SEND for address "+event.getRawMessage().getString("address"));
                break;
            case RECEIVE:
//                System.out.println("Received RECEIVE from address "+event.getRawMessage().getString("address"));
//                System.out.println(event.getRawMessage().toString());
                break;
            default:
                System.out.println("Received unexpected "+event.type()+" from "+event.socket().remoteAddress().host()+":"+event.socket().remoteAddress().port());
        }

        event.complete(true);
    }

    // Обработка запросов клиентов на регистрацию
    public void onClientRegistered(BridgeEvent event){

        String address = event.getRawMessage().getString("address");
        HashMap<String, Object> msg;
        try {
            msg = new Gson().fromJson(event.getRawMessage().getString("headers"), HashMap.class);
        }catch (Exception e){
            log.error("Server::onClientRegistered: received wrong JSON-string as headers: "+event.getRawMessage().getString("headers"));
            return;
        }
        if (msg == null) {
            log.error("Server::onClientRegistered: received message with empty headers: "+event.getRawMessage().toString());
            return;
        }

        // Подтверждаем регистрацию клиента на общем канале и отправляем сообщение на Core для поиска/создания клиентской сессии
        if (address.equals("general")) {
//            System.out.println("Client general registered with headers: "+event.getRawMessage().getString("headers"));

            if ((msg.get("action") != null) && (msg.get("action").equals("registration"))){
                msg.put("from", localAddress);
                msg.put("address", address);
                msg.put("action", "clientRegistration");
                msg.put("host", event.socket().remoteAddress().host());
                msg.put("port", String.valueOf(event.socket().remoteAddress().port()));
                eb.send("core", new JSONObject(msg).toString());
            }else {
                log.error("Server::onClientRegistered: Unexpected action: "+msg.get("action"));
            }
        } else if ((address.length() == 14) && (address.substring(0,2).equals("cl"))) {
//            System.out.println("Client personal registered with headers: "+event.getRawMessage().getString("headers"));

            if ((msg.get("action") != null) && (msg.get("action").equals("registration"))){
                msg.put("from", localAddress);
                msg.put("address", address);
                msg.put("action", "clientConfirm");
                msg.put("host", event.socket().remoteAddress().host());
                msg.put("port", String.valueOf(event.socket().remoteAddress().port()));
                eb.send("core", new JSONObject(msg).toString());
            }else {
                log.error("Server::onClientRegistered: Unexpected action: "+msg.get("action"));
            }
        }else {
            log.error("Server::onClientRegistered: received wrong client address: "+address);
        }
    }

    // Обработка запросов клиентов на снятие регистрации
    public void onClientUnRegister(BridgeEvent event) {

        String address = event.getRawMessage().getString("address");
        HashMap<String, Object> msg;
        try {
            msg = new Gson().fromJson(event.getRawMessage().getString("headers"), HashMap.class);
        } catch (Exception e) {
            log.error("Server::onClientRegistered: received wrong JSON-string as headers: " + event.getRawMessage().getString("headers"));
            return;
        }
        if (msg == null) {
            msg = new HashMap<>();
        }

        // Обработка запросов клиентов на снятие регистрации клиентской сессии
        if ((address.length() == 14) && (address.substring(0, 2).equals("cl"))) {
//            System.out.println("Server::onClientUnRegistered: Unregister request for client session with address: "+address);
            delEbPermit(address);
            msg.put("from", localAddress);
            msg.put("address", address);
            msg.put("action", "clientUnregister");
            msg.put("host", event.socket().remoteAddress().host());
            msg.put("port", String.valueOf(event.socket().remoteAddress().port()));
            eb.send("core", new JSONObject(msg).toString());
        }
    }

    // Обработка сообщений о закрытии клиентского сокета
    public void onClientSocketClosed(BridgeEvent event) {

        HashMap<String, Object> msg = new HashMap<>();

        // Обработка сообщений о закрытии клиентского сокета
//        System.out.println("Server::onClientSocketClosed: Closed client socket with host:port: "+event.socket().remoteAddress().host()+":"+String.valueOf(event.socket().remoteAddress().port()));
        msg.put("from", localAddress);
        msg.put("action", "clientSocketClosed");
        msg.put("host", event.socket().remoteAddress().host());
        msg.put("port", String.valueOf(event.socket().remoteAddress().port()));
        eb.send("core", new JSONObject(msg).toString());
    }

    // Обработка запросов контента GET/POST запросами
    private void onRequestContent(RoutingContext rc){

        String[] path = rc.pathParams().get("*").split("/");

        // Вообще здесь запланирована передача идентификатора клиентской сессии (в пути или в параметрах POST или в куках... и проверка сессии на валидность...)
        if (path.length > 2) {                              // Если путь состоит больше чем из одного параметра
            if (!path[2].equals("java")) {                  // Если имя второго параметра в пути не "java", значит контент находится в файле
                String contentFilePath = contentFilesPath;
                int i;
                for (i = 1; i < path.length-1; i++) {
                    contentFilePath += path[i]+"/";
                }
                contentFilePath += path[i]+"."+path[i-1];

                File contentFile = new File(contentFilePath);
                if(!contentFile.exists() || contentFile.isDirectory()) {
                    rc.response().setStatusCode(404).end("");
                }else {
                    rc.response().sendFile(contentFilePath);
                }
            }else {
                rc.response().putHeader(HttpHeaders.CONTENT_TYPE, "text/plain")
                        .putHeader(HttpHeaders.CONTENT_ENCODING, "utf-8")
                        .putHeader(HttpHeaders.CACHE_CONTROL, "no-store, no-cache, must-revalidate, max-age=0")
                        .putHeader("pragma", "no-cache")
                        .end("Пока не используется...");
            }

            return;
        }

        rc.response().setStatusCode(404).end("");
    }

    // Добавление разрешённого адреса в шине для входящих и исходящих сообщений
    public void addEbPermit(String address){
        if (permittedAddresses.containsKey(address)) { return; }
        PermittedOptions opt = new PermittedOptions().setAddress(address);
        permittedAddresses.put(address, opt);

        wsHandlerBridgeOptions
                .addInboundPermitted(opt)
                .addOutboundPermitted(opt);
    }

    // Удаление разрешённого адреса в шине для входящих и исходящих сообщений
    public void delEbPermit(String address){
        if (!permittedAddresses.containsKey(address)) { return; }
        PermittedOptions opt = permittedAddresses.get(address);
        permittedAddresses.remove(address);
        wsHandlerBridgeOptions.getInboundPermitteds().remove(opt);
        wsHandlerBridgeOptions.getOutboundPermitteds().remove(opt);
    }
}
