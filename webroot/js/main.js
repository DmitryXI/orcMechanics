/* Основные параметры, базовые функции работы с документом */

// Глобальное хранилище переменных на уровне окна/вкладки
window.vars = {
    "logLevel" : 8,              // Уровень журналирования в консоль
    "tagList"  : [],             // Список тэгов для отображения по слоям
    "logger"   : new Logger(),   // Глобальный логер
    "user"     : null,           // Информация о текущем пользователе
    "game"     : null,           // Идентификатор сессии пользователя
    "forms"    : {},             // Список активных форм на случай изменения размеров окна
    "resources": {},             // Список загруженных с сервера ресурсов (сырой контент)
    "ueb"      : null,           // Шина данных (EventBus) сессии клиента
    "geb"      : null,           // Шина данных (EventBus) игровой сессии (пока будет таже, что и менеджерская)
    "baseUrl"  : null,           // Путь к корневой точке для запросов к веб-серверу
    "baseRequestHeaders": null   // Заголовки POST-запросов используемые по умолчанию, при запросе контента
};

var log = w().logger;

// Данные клиентской сессии
w().user = {
    "usid"          : null,         // Идентификатор клиентской сессии
    "gsid"          : null,         // Идентификатор игровой сессии
    "clientAddress" : null,         // Адрес для клиентской сессии на сервере
    "gameAddress"   : null,         // Адрес для клиентской сессии на сервере
    "connected"     : false,        // Статус подключения
    "status"        : "loading",    // Статус клиента
    "clid"          : null,         // Идентификатор клиента (создаётся на стороне клиента для привязки сессии)
    "name"          : "noname"      // Имя игрока
};

// Обрабатываем завершение загрузки для окна
window.onload = function() {
    if (document.readyState == 'loading') {
      document.addEventListener('DOMContentLoaded', docReady);
    } else {
      docReady()
    }
}

// Обрабатываем событие изменения размера окна
window.onresize = function(){
    log.debug7("Resizing window");
    refreshForms();
}

// Событие завершение формирования документа и создание основной инфраструктуры
function docReady(){
    log.debug6("Document ready");

    w().baseUrl = getBaseUrl();
    log.debug6("Base URL: "+w().baseUrl);

    w().baseRequestHeaders = {"cache-control":"no-cache, no-store, must-revalidate","pragma":"no-cache","expires":"0"};

    // Добавляем body в список активных форм
    w().forms["body"] = {"element":[d("body")],"onResize":"bodyResize","onResizeParams":[]};      // Для теста. Врядли будет использоваться
    w().user.status = "connecting";

    // Шина данных клиентской сессии
    w().ueb = new EventBus("/eventbus/");

//eraseCookie("user.usid");
//eraseCookie("user.clid");
//eraseCookie("user.clientAddress");

    // Проверяем куки на предмет сохранённого идентификатора клиента, сессии и её адреса
    w().user.usid = getCookie("user.usid");
    w().user.clid = getCookie("user.clid");
    w().user.clientAddress = getCookie("user.clientAddress");
    if(w().user.clid === null){
        w().user.clid = getRandomString(16);
        w().user.usid = null;
        w().user.clientAddress = null;
    }

    // Событие подключения шины данных к серверу через вебсокет
    w().ueb.onopen = function() {
        log.info("Connected to server");
        w().ueb.send = function(address, message, headers, callback) {
            if(w().user.connected){
                log.debug8("Send to "+address+":");
                log.debug8(JSON.parse(message));
                return EventBus.prototype.send.call(this, address, message, headers, callback);
            }else{
                log.error("No active connection for send message to address "+address);
                log.error(JSON.parse(message));
            }
        }
        w().user.connected = true;                          // Обновляем статус подключения

        if(w().user.status === "connecting"){
            log.info("Request general registration");
            w().user.status = "preRegistration";
            w().ueb.registerHandler("general", {"action":"registration","usid":w().user.usid,"clid":w().user.clid}, onMessage);   // Отправляем запрос на регистрацию на общий адрес (для получения персонального адреса)
        }
    };

    // Событие отключения шины данных
    w().ueb.onclose = function() {
        log.info("Connect to server closed");
        w().user.connected = false;                         // Обновляем статус подключения
    }
}