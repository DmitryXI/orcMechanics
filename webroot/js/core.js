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

        if((importance > 0) && (importance <= w().logLevel)){
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
        }
    }
}









