/* Основные функции движка */

// Глобальное хранилище переменных на уровне окна/вкладки
window.vars = {};

// Доступ к глобальным переменным уровня окна/вкладки
function w(){ return window.vars; }

// Получить элемент документа по id
function d(id=null){

    if((id !== undefined) && (id !== null)){
        return document.getElementById(id);
    }

    return document;
}

// Сохранение/отображение/отправка отладочных сообщений
function Logger() {
    this.fatal  = function (message) { this.log(message, 1); },
    this.error  = function (message) { this.log(message, 2); },
    this.warn   = function (message) { this.log(message, 3); },
    this.info   = function (message) { this.log(message, 4); },
    this.debug  = function (message) { this.log(message, 5); },
    this.debug6 = function (message) { this.log(message, 6); },
    this.debug7 = function (message) { this.log(message, 7); },
    this.debug8 = function (message) { this.log(message, 8); },

    this.log = function (message, importance){
        importance = Number(importance)

        if(((w().logList.length == 0) && (importance > 0) && (importance <= w().logLevel)) || (w().logList.includes(importance))){
            let importanceText;

            switch(importance) {
              case 1:
                importanceText = "FATAL";
                break;
              case 2:
                importanceText = "ERROR";
                break;
              case 3:
                importanceText = "WARN";
                break;
              case 4:
                importanceText = "INFO";
                break;
              default:
                importanceText = "DEBUG "+importance;
                break;
            }

            console.log(importanceText+": "+message);

            if(typeof message === "object"){
                console.log(message);
            }
        }
    }
}

// Получение базового адреса URL на основе текущей локации
function getBaseUrl(){
    let tail = document.location.pathname.split("/");
    let baseUrl = document.location.protocol+"//"+document.location.host
    if(document.location.port != ""){
        baseUrl += ":"+document.location.port;
    }
    for (let i = 1; i < tail.length-1; i++) {
        baseUrl += "/"+tail[i];
    }
    baseUrl += "/"

    return baseUrl;
}

// Изменение размеров активных форм
function refreshForms(){

    for(let formId in w().forms){
        log.debug7("form id to resize: "+formId);
        window[w().forms[formId].function](...w().forms[formId].params);
    }

}

// Запись значения в cookie
function setCookie(name,value,days) {
    var expires = "";
    if (days) {
        var date = new Date();
        date.setTime(date.getTime() + (days*24*60*60*1000));
        expires = "; expires=" + date.toUTCString();
    }
    document.cookie = name + "=" + (value || "")  + expires + "; path=/";
}

// Чтение значения из cookie
function getCookie(name) {
    var nameEQ = name + "=";
    var ca = document.cookie.split(';');
    for(var i=0;i < ca.length;i++) {
        var c = ca[i];
        while (c.charAt(0)==' ') c = c.substring(1,c.length);
        if (c.indexOf(nameEQ) == 0) return c.substring(nameEQ.length,c.length);
    }
    return null;
}

// Очистка cookie
function eraseCookie(name) {
    document.cookie = name +'=; Path=/; Expires=Thu, 01 Jan 1970 00:00:01 GMT;';
}









// Обработка входящих сообщений
function onMessge(err, msg){

    log.debug6("Callbak on message");
    log.debug7("В переменной event здесь есть полное событие (log.debug(event))");

    if(err !== null){
        log.error(err)
    }

    log.debug7(msg);

    // body в сообщении может и не быть
    let body = null;

    if(msg.body !== null){
        try{
            body = JSON.parse(msg.body);
        }catch{
            log.error("Error parsing body JSON");
            return;
        }
    }else{return;}

    // Обрабатываем сообщение, только если есть body

    if(msg.address === "general"){
        if((w().user.status === "registration") && (body.action === "newClientSession")){
            w().user.usid = body.usid;
            w().user.clientAddress = body.address;
            w().user.status = "confirming";
            setCookie("user.usid", w().user.usid, 1);                             // Сохраняем в куках идентификатор сессии и её адрес
            setCookie("user.clientAddress", w().user.clientAddress, 1);
            log.debug("Registered on server success");
            w().ueb.unregisterHandler("general", onMessge);                       // Снимаем регистрацию для публичного адреса
            w().ueb.registerHandler(w().user.clientAddress, onMessge);            // И регистрируемся по адресу клиентской сессии
        }else{
            log.error("Unexpected message in channel general");
            log.error(msg);
            return;
        }
    }else if(msg.address === w().user.clientAddress){
        log.debug("Message in client channel");
        w().ueb.send(w().user.clientAddress, JSON.stringify({"test":"message"}));
    }
}









