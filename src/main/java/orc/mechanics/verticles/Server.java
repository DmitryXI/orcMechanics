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
        router                  = Router.router(vertx);                                                 // Создаём основной роутер веб-сервера
        contentRouter           = Router.router(vertx);                                                 // Создаём подроутер для закрытого контента
        wsHandler               = SockJSHandler.create(vertx);                                          // Создаём хэндлер для веб-сокета
        wsHandlerBridgeOptions  = new SockJSBridgeOptions();                                            // Создаём объект с опциями для моста хэндлер -> обработчик
        wsRouter                = wsHandler.bridge(wsHandlerBridgeOptions, this::onClientMessage);      // Создаём мост хэндлер -> обработчик

        router.mountSubRouter("/eventbus", wsRouter);                                                 // Монтируем подроутер для веб-сокета
        router.mountSubRouter("/content", contentRouter);                                             // Монтируем подроутер для запросов закытого контента
        router.route().handler(StaticHandler.create().setCachingEnabled(false).setDefaultContentEncoding("utf-8"));     // Создаём статический обработчик к запросам открытого контента
        contentRouter.post().handler(this::onRequestContent);                                            // Цепляем обработчик к хэндлеру роутера для закрытого контента

        addEbPermit("general");                                                                   // Добавляем разрешение для приёма сообщений через веб-сокет на общий адрес

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

        System.out.println("Server received local message: "+ebMsg.body());
    }

    // Обработка клиентских сообщений
    public void onClientMessage(BridgeEvent event){

        switch (event.type()){
            case SOCKET_CREATED:
                System.out.println("Received SOCKET_CREATED from "+event.socket().remoteAddress().host()+":"+event.socket().remoteAddress().port());
                break;
            case SOCKET_CLOSED:
                System.out.println("Received SOCKET_CLOSED from "+event.socket().remoteAddress().host()+":"+event.socket().remoteAddress().port());
                break;
            case SOCKET_PING:
                System.out.println("Received PING from "+event.socket().remoteAddress().host()+":"+event.socket().remoteAddress().port());
                break;
            case REGISTER:
                System.out.println("Received REGISTER from address "+event.getRawMessage().getString("address"));
                break;
            case REGISTERED:
                System.out.println("Received REGISTERED from address "+event.getRawMessage().getString("address"));
                eb.send("core", new JSONObject().put("from", localAddress).put("action", "clientAnyMessage").toString());
                break;
            case UNREGISTER:
                System.out.println("Received UNREGISTER from address "+event.getRawMessage().getString("address")+" and "+event.socket().remoteAddress().host()+":"+event.socket().remoteAddress().port());
                break;
            case SEND:
                System.out.println("Received SEND for address "+event.getRawMessage().getString("address"));
                break;
            case RECEIVE:
                System.out.println("Received RECEIVE from address "+event.getRawMessage().getString("address"));
                System.out.println(event.getRawMessage().toString());
                break;
            default:
                System.out.println("Received unexpected "+event.type()+" from "+event.socket().remoteAddress().host()+":"+event.socket().remoteAddress().port());
        }

        event.complete(true);
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
