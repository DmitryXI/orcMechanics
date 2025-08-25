/* Основные параметры, базовые функции работы с документом */

// Глобальное хранилище переменных на уровне окна/вкладки
window.vars = {
    "logLevel" : 8,              // Уровень журналирования в консоль
    "logList"  : [],             // Список номеров важности сообщений для вывода в консоль (альтернатива уровню (logLevel))
    "logger"   : new Logger(),   // Глобальный логер
    "user"     : null,           // Информация о текущем пользователе
    "game"     : null,           // Идентификатор сессии пользователя
    "forms"    : {},             // Список активных форм на случай изменения размеров окна
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
    w().forms["body"] = {"function":"bodyResize","params":[]};      // Для теста. Врядли будет использоваться
    w().user.status = "connecting";

    // Шина данных клиентской сессии
    w().ueb = new EventBus("/eventbus/");

    // Событие подключения шины данных к серверу через вебсокет
    w().ueb.onopen = function() {
        log.info("Connected to server");
        w().ueb.send = function(address, message, headers, callback) {
//            log.debug8("Send to "+address+": "+message);
            log.debug8("Send to "+address+":");
            log.debug8(JSON.parse(message));
            return EventBus.prototype.send.call(this, address, message, headers, callback);
        }
        w().user.connected = true;                  // Обновляем статус подключения
        if(w().user.usid === null){
            log.info("Request registration");
            w().user.status = "registration";
            w().ueb.registerHandler("general", onMessge);   // Отправляем запрос на регистрацию на публичный адрес (для получения персонального адреса)
        }
    };

    // Событие отключения шины данных
    w().ueb.onclose = function() {
        log.info("Connect to server closed");
        w().user.connected = false;                  // Обновляем статус подключения
    }
}