/* Основные параметры, базовые функции работы с документом */

// Глобальное хранилище переменных на уровне окна/вкладки
window.vars = {
    "logLevel" : 6,              // Уровень журналирования в консоль (общий для всех тэгов (уровень журналирования тега приоритетней))
    "logger"   : null,           // Глобальный логер
    "user"     : null,           // Информация о текущем пользователе
    "game"     : null,           // Идентификатор сессии пользователя (Планировалось по аналоги с user, но по запаре содержимое засунуто в users)
    "forms"    : {},             // Список активных форм на случай изменения размеров окна
    "resources": {},             // Список загруженных с сервера ресурсов (сырой контент)
    "ueb"      : null,           // Шина данных (EventBus) сессии клиента
    "geb"      : null,           // Шина данных (EventBus) игровой сессии (пока будет таже, что и менеджерская)
    "baseUrl"  : null,           // Путь к корневой точке для запросов к веб-серверу
    "baseRequestHeaders": null   // Заголовки POST-запросов используемые по умолчанию, при запросе контента
};

w().logger = new Logger(5, false, true, true);      // Создаём логер
var log = w().logger;
log.addTag("msg", 8);            // Добавляем тэг для фильтрации журналов сообщений        (сообщения)
log.addTag("form", 8);           // Добавляем тэг для фильтрации журналов работы с формами (события HTML-форм)
log.addTag("func", 6);           // Добавляем тэг для фильтрации обращения к функциям      (события связанные с функциями)
log.addTag("data", 8);           // Добавляем тэг для фильтрации обращения к функциям      (события связанные с переменными и данными)



// Данные клиентской сессии
w().user = {
    "usid"          : null,         // Идентификатор клиентской сессии
    "gsid"          : null,         // Идентификатор игровой сессии
    "clientAddress" : null,         // Адрес для клиентской сессии на сервере
    "gameAddress"   : null,         // Адрес для игровой сессии на сервере
    "connected"     : false,        // Статус подключения
    "status"        : "loading",    // Статус клиента
    "stage"         : null,         // Этап сессии
    "clid"          : null,         // Идентификатор клиента (создаётся на стороне клиента для привязки сессии)
    "name"          : null,         // Имя игрока
    "gamesList"     : null,         // Список доступных игр
    "gameSessionsList": null,       // Список доступных игровых сессий
    "onGameMessage": null           // Ссылка на обработчик сообщений от игрового вертикла (приходят на адрес клиента)
};

// Обрабатываем завершение загрузки для окна
window.onload = function() {
    log.func.debug6("main.window.onload");
    if (document.readyState == 'loading') {
      document.addEventListener('DOMContentLoaded', docReady);
    } else {
      docReady()
    }
}

// Обрабатываем событие изменения размера окна
window.onresize = function(){
    log.func.debug6("main.window.onresize");
    refreshForms();
}

// Событие завершение формирования документа и создание основной инфраструктуры
function docReady(){
    log.func.debug6("main.docReady");

    w().baseUrl = getBaseUrl();
    log.data.debug("Set baseURL: "+w().baseUrl);

    w().baseRequestHeaders = {"cache-control":"no-cache, no-store, must-revalidate","pragma":"no-cache","expires":"0"};
    log.data.debug("Set baseRequestHeaders: ", w().baseRequestHeaders);

    // Шина данных клиентской сессии
    w().ueb = new EventBus("/eventbus/");
    w().user.status = "connecting";
    log.data.debug("Set user.status: "+w().user.status);

//eraseCookie("user.usid");
//eraseCookie("user.clid");
//eraseCookie("user.clientAddress");

    // Проверяем куки на предмет сохранённого идентификатора клиента, сессии и её адреса
    w().user.usid = getCookie("user.usid");
    w().user.clid = getCookie("user.clid");
    w().user.clientAddress = getCookie("user.clientAddress");
    log.data.debug("Get from cookie: clid: "+w().user.clid+", usid: "+w().user.usid+", clientAddress: "+w().user.clientAddress);
    if(w().user.clid === null){
        w().user.clid = getRandomString(16);
        w().user.usid = null;
        w().user.clientAddress = null;
    }
    log.data.debug("Set after processing: clid: "+w().user.clid+", usid: "+w().user.usid+", clientAddress: "+w().user.clientAddress);

    // Событие подключения шины данных к серверу через вебсокет
    w().ueb.onopen = function() {
        log.func.debug6("main.ueb.onopen");
        log.info("Connected to server");
        w().ueb.send = function(address, message, headers, callback) {
            if(w().user.connected){
                log.msg.debug7("Send to "+address, JSON.parse(message));
                return EventBus.prototype.send.call(this, address, message, headers, callback);
            }else{
                log.error("No active connection for send message to address "+address);
                log.error(JSON.parse(message));
            }
        }
        w().user.connected = true;                          // Обновляем статус подключения
        log.data.debug("Set user.connected: "+w().user.connected);

        if(w().user.status === "connecting"){
            log.info("Request general registration");
            w().user.status = "preRegistration";
            log.data.debug("Set user.status: "+w().user.status);
            w().ueb.registerHandler("general", {"action":"registration","usid":w().user.usid,"clid":w().user.clid}, onMessage);   // Отправляем запрос на регистрацию на общий адрес (для получения персонального адреса)
        }
    };

    // Событие отключения шины данных и закрытия веб-сокета
    w().ueb.onclose = function() {
        log.func.debug6("main.ueb.onclose");
        log.info("Connect to server closed");
        w().user.connected = false;                         // Обновляем статус подключения
        log.data.debug("Set user.connected: "+w().user.connected);
    }
}