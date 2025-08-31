/* Основные функции движка */

// Глобальное хранилище переменных на уровне окна/вкладки
window.vars = {};

// Доступ к глобальным переменным уровня окна/вкладки
function w(){ return window.vars; }

// Получить элемент документа по id
function d(elId=null, parentElement=null){

    if(parentElement === null){
        parentElement = document;
    }else if(!(parentElement instanceof HTMLElement)){
        log.error("Parent element must be instance of null or HTMLElement or HTMLDocument");
        return null;
    }

    if(typeof elId === "string"){
        return parentElement.querySelector('#'+elId);
    }

    log.error("Elemnt id must be string. "+(typeof elId)+" setted");
    return null;
}

// Получить HTML-форму по id либо сразу HTMLElement формы по номеру
function f(formId, elementNumber=null){
    if(w().forms[formId] !== undefined){
        if(typeof elementNumber == "number"){
            return w().forms[formId].element[elementNumber];
        }else{
            return w().forms[formId];
        }
    }

    return null;
}

// Получить массив объектов класса HTMLElement HTML-кода
function getElementsFromHTML(srcHTML){

    let elements = [];
    let tDiv = document.createElement("div");
    tDiv.innerHTML = srcHTML;

    for (let i = 0; i < tDiv.children.length; i++) {
        elements.push(tDiv.children[i]);
    }
    tDiv.remove();

    return elements;
}

// Получить объект класса HTMLElement по id из HTML-кода
function getElementFromHTML(elId, srcHTML){
    let tDiv = document.createElement("div");
    tDiv.innerHTML = srcHTML;
    let el = d(elId, tDiv);
    tDiv.remove();

    return el;
}

// Вырезать HTMLElement из родительского элемента
function cutElementFromElement(elId, parentElement){

    let el = parentElement.querySelector('#'+elId)

    if(el !== null){
        let clone = el.cloneNode(true)
        el.remove()

        return clone
    }

    return null;
}

// Получить размеры рабочей области в пикселях
function getWorkSize() {
    var e = window, a = 'inner';
    if (!('innerWidth' in window )) {
        a = 'client';
        e = document.documentElement || document.body;
    }
    return { width : e[ a+'Width' ] , height : e[ a+'Height' ] };
}




// Сохранение/отображение/отправка отладочных сообщений
function Logger(printStack=false) {
    this.printStack = printStack;
    this.tagList = [];

    this.fatal   = function (message, obj=null) { this.log(message, obj, 1); },
    this.error   = function (message, obj=null) { this.log(message, obj, 2); },
    this.warn    = function (message, obj=null) { this.log(message, obj, 3); },
    this.info    = function (message, obj=null) { this.log(message, obj, 4); },
    this.debug   = function (message, obj=null) { this.log(message, obj, 5); },
    this.debug6  = function (message, obj=null) { this.log(message, obj, 6); },
    this.debug7  = function (message, obj=null) { this.log(message, obj, 7); },
    this.debug8  = function (message, obj=null) { this.log(message, obj, 8); },

    // Добавление тэга к в логер
    this.addTag = function (tag){
        if(typeof tag === "string"){
            let uTag = tag.toUpperCase();
            let lTag = tag.toLowerCase();
            if(!this.tagList.includes(uTag)){
                this.tagList.push(uTag);
                this[lTag] = {
                    "fatal"  : function (message, obj=null) { this.logger.log(message, obj, 1, uTag); },
                    "error"  : function (message, obj=null) { this.logger.log(message, obj, 2, uTag); },
                    "warn"   : function (message, obj=null) { this.logger.log(message, obj, 3, uTag); },
                    "info"   : function (message, obj=null) { this.logger.log(message, obj, 4, uTag); },
                    "debug"  : function (message, obj=null) { this.logger.log(message, obj, 5, uTag); },
                    "debug6" : function (message, obj=null) { this.logger.log(message, obj, 6, uTag); },
                    "debug7" : function (message, obj=null) { this.logger.log(message, obj, 7, uTag); },
                    "debug8" : function (message, obj=null) { this.logger.log(message, obj, 8, uTag); },
                };
                this[lTag].logger = this;
            }
        }
    }

    // Разбор стэка вызова
    function getCaller(stack){
        let startPos = stack.indexOf("\n", 0)+1;
        let stopPos = stack.indexOf('@', startPos);
        if(stopPos == startPos){
            return ""
        }else{
            return stack.substring(startPos, stopPos);
        }
    }

    // Приём и обработка логов
    this.log = function (message, obj=null, importance, tag=null){
        importance = Number(importance)

        if(this.tagList.includes(tag)){
            tag += " ";
        }else{ tag = ""; }

        if((importance > 0) && (importance <= w().logLevel)){
            let importanceText;

            switch(importance) {
              case 1:
                importanceText = tag+"FATAL";
                if(this.printStack){ console.log(new Error().stack); }
                break;
              case 2:
                importanceText = tag+"ERROR";
                if(this.printStack){ console.log(new Error().stack); }
                break;
              case 3:
                importanceText = tag+"WARN";
                break;
              case 4:
                importanceText = tag+"INFO";
                break;
              default:
                importanceText = tag+"DEBUG "+importance;
                break;
            }

            if(obj !== null){
                console.log([importanceText+": "+message+": "+JSON.stringify(obj), obj]);
            }else if(typeof message === "object"){
                console.log([importanceText+": "+JSON.stringify(message), message]);
            }else{
                console.log(importanceText+": "+message);
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
function refreshForms(formId=null){

    if(formId !== null){
        if(window[w().forms[formId].onResize] instanceof Function){
            let params = w().forms[formId].onResizeParams;
            params.unshift(w().forms[formId].element[0]);
            window[w().forms[formId].onResize](...w().forms[formId].onResizeParams);
        }

        return;
    }

    for(formId in w().forms){
        log.debug7("form id to resize: "+formId);
        if(window[w().forms[formId].onResize] instanceof Function){
            let params = w().forms[formId].onResizeParams;
            params.unshift(w().forms[formId].element[0]);
            window[w().forms[formId].onResize](...w().forms[formId].onResizeParams);
        }
    }
}

// Генерация случайной строки
function getRandomString(len) {
    let result = '';
    let characters = 'abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789+-*';
    let charactersLength = characters.length;
    for (let i = 0; i < len; i++) {
        result += characters.charAt(Math.floor(Math.random() * charactersLength));
    }

    return result;
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

// Получение контента по id контента
function getContent(contentId){
    if((w().resources[contentId] !== null) && (w().resources[contentId] !== undefined)){
        return w().resources[contentId];
    }else {
        let resp = loadContent(contentId, false);
        if (resp instanceof XMLHttpRequest) {
            w().resources[contentId] = resp.responseText;
            return w().resources[contentId];
        }
    }

    log.error("Can't load content for id "+contentId);
    return null;
}

// Получение контента от сервера через POST запрос
// async - true|false (синхронный|асинхронный), on-ready -error, -abort - ссылки на функции обратного вызова
// postParams - объект, который будет сериализован в JSONString и отправлен как данные POST-запроса
// requestHeader - объект с заголовками HTTP-запроса
// transitStr - объект, который будет передаваться в callBack-функции (лучше всего использовать строку)
function loadContent(contentId, async, transitStr=null, onready=null, onerror=null, onabort=null, postParams=null, requestHeaders=null){
    proto = "POST";
    if (async !== false) {async = true;}
    let client  = new XMLHttpRequest();
    let isError = false;
    let url     = w().baseUrl+"content/"+contentId+"/?timestamp=" + new Date().getTime();
    log.debug("Content requested by url "+url);

    client.onreadystatechange = function() {
        if( this.readyState === 2 ) {
            // do something
        }
        if( this.readyState === 3 ) {
            // do something
        }
        if( this.readyState === 4 ) {
            if (this.status !== 200) {
                isError = true;
            }
            if (typeof onready === "function"){
                onready(this, isError, transitStr)
            }
        }
    }

    client.onabort = function(){
        if (typeof onabort === "function"){
            this.onabort = onabort(this, isError, transitStr)
        }
    }

    client.onerror = function(){
        isError = true
        if (typeof onerror === "function"){
            this.onerror = onerror(this, isError, transitStr)
        }
    }

    client.open(proto, url, async);

    if ((requestHeaders !== undefined) && (requestHeaders !== null)){
        for (let headerName of Object.keys(requestHeaders)) {
            client.setRequestHeader(headerName, requestHeaders[headerName]);
        }
    }else{
        for (let headerName of Object.keys(w().baseRequestHeaders)) {
            client.setRequestHeader(headerName, w().baseRequestHeaders[headerName]);
        }
    }

    if ((postParams === undefined) || (postParams === null)) {
        client.send();
    }else{
        client.send(JSON.stringify(postParams));
    }

    if (async === false) {
        if (isError) {return false;}
        return client;
    }else{
        return true;
    }
}

// Загрузить скрипт
function loadScript(contentId, async=false){

    let scriptId = contentId.replaceAll("/", "_");
    log.debug("Loading script "+scriptId);
    let el = d(scriptId);

    if(el === null){
        if(async){              // Асинхронная загрузка скрипта
            return loadContent(contentId, async, scriptId, addScript);
        }else{                  // Синхронная загрузка скрипта
            let resp = loadContent(contentId, async, scriptId);

            if (resp instanceof XMLHttpRequest) {
                return addScript(resp, false, scriptId);
            }else{
                return false
            }
        }
    }else{
        log.debug("Script "+scriptId+" already loaded");
        return true;
    }
}

// Добавить скрипт
function addScript(resp, error, scriptId){

    let el = d(scriptId);

    if(el !== null){
        log.debug("Script "+scriptId+" already exists");
        return true;
    }

    try{
        el          = document.createElement("script");
        el.type     = "text/javascript";
        el.charset  = "UTF-8";
        el.id       = scriptId;
        el.text     = resp.responseText;
        document.getElementsByTagName('head')[0].appendChild(el);
    }catch{
        log.error("Error creating script "+scriptId);
        return false;
    }

    log.debug("Added script "+scriptId);
    return true;
}

// Добавить форму в документ
// Добавляет в документ форму на основании контента по id ресурса (если нужно, загружает с сервера).
// resizeParams - параметры для обработчика onResize, cook - колбэк-функция-обработчик сырого контента, cookParams - параметры для функции cook
function addHTMLForm(contentId, formId, resizeParams=[], cook=null, cookParams=[]){

    if((w().forms[formId] === null) || (w().forms[formId] === undefined)){                      // Проверяем наличие формы в хранилище форм
        log.debug("Requested requested content for form with contentId="+formId);

        let srcHtml = getContent("core/html/requestName");

        if(srcHtml === null){
            log.error("Can't get source HTML for contentId="+contentId+" for form with formId="+formId);
            return null;
        }

        let elements;

        if(cook !== null){                                                                      // Если передан обработчик сырого контента, то он должен вернуть элементы формы
            cookParams.unshift(srcHtml);                                                        // Добавляем первым параметром в списке исходный HTML
            elements = cook(...cookParams);
        }else{                                                                                  // Если спец.обработчик не задан, создаём элементы стандартным образом
            let html = srcHtml.replaceAll("${id}", formId).replaceAll("${z}", 1);
            let elements = getElementsFromHTML(html);
        }

        // Добавляем форму в хранилище
        w().forms[formId] = {
            "id"             : formId,
            "element"        : elements,
            "onResize"       : null,
            "onResizeParams" : [],
            "getFormElement" : function(num) { return this.element[num]; },
            "getHTMLElement" : function(id=null)  { return getInFormHTMLElement(this, id); }
        };

        // Подключаем обработчик формы на событие изменения размера рабочей области (именование стандартизировано)
        let resizeFuncName = formId+"_onResize";
        if(window[resizeFuncName] instanceof Function){
            w().forms[formId].onResize       = resizeFuncName;
            w().forms[formId].onResizeParams = resizeParams;
            refreshForms(formId);
        }
    }else{
        log.debug("Form "+formId+" already exists");
    }

    return w().forms[formId];
}

// Получить элемент HTML-формы по номеру (по умолчанию нулевой (стандартно там находится собранная форма))
function getFormElement(formId, number=0){

    if(w().forms[formId] !== null){
        return w().forms[formId].element[number];
    }

    return null;
}

// Получить HTML-элемент HTML-формы по id (сначала идёт поиск в нулевом элементе (обычно добавлен в body), затем по остальным элементам формы (обычно в body не добавлены))
// Идентификатор передаётся без идентификатора формы (добавляется автоматически)
function getInFormHTMLElement(form, elementId){

    let el;
    if(elementId === null){
        elementId = form.id;
    }else{
        elementId = form.id+"_"+elementId;
    }

    for(num in form.element){
        if(form.element[num].id === elementId){
            return form.element[num];
        }
        el = d(elementId, form.element[num]);
        if(el !== null){
            return el;
        }
    }

    return null;
}

// Удалить форму из документа и хранилища (или где есть)
function delHTMLForm(formId){

    let el = d(formId);

    if(el !== null){
        el.remove();
    }

    if(w().forms[formId] !== null){
        w().forms[formId] = null;
    }
}

// Удалить форму ТОЛЬКО из документа
function removeHTMLForm(formId){

    let el = d(formId);

    if(el !== null){
        el.remove();
    }
}

// Задать дефолтовую функцию автосайзинга HTML-форме
function setHTMLFormAutoSize(formId, width=null, height=null){
    // Здесь будем создавать лямбду для onResize формы, которая будет размещать форму по центру экрана и задавать ей размеры в соответствии с указанными процентами...
}

// Обработка события изменения размера рабочей области для элемента body
function bodyResize(){
//    log.debug("On body resize");
}





// Штатная отправка сообщений серверу. to - адрес получателя, action - действие (обязательное поле), msg - объект с прикладными полями сообщения
function sendMsg(to, action, msg={}){
    if(w().user.connected){
        msg["from"]     = w().user.clientAddress            // Адрес отправителя
        msg["action"]   = action;                           // Действие
        msg["usid"]     = w().user.usid;                    // Идентификатор сессии
        w().ueb.send(to, JSON.stringify(msg));

        return true;
    }else{
        log.error("No active connection for send message to address "+w().user.clientAddress);
        log.error(msg);
    }

    return false;
}


// Обработка входящих сообщений
function onMessage(err, msg){

    log.func.debug6("Callbak on message");
//    log.debug8("В переменной event здесь есть полное событие (log.debug(event))");

    if(err !== null){
        log.error(err)
    }

    log.msg.debug7("Received message", msg);

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
        if((w().user.status === "preRegistration") && (body.action === "newClientSession")){       // Если это подтверждение регистрации на сервере
            log.debug("Successfully registered on general channel");

            w().user.usid = body.usid;
            w().user.clientAddress = body.address;
            w().user.status = "registration";

            setCookie("user.usid", w().user.usid, 1);                                               // Сохраняем в куках идентификатор сессии
            setCookie("user.clientAddress", w().user.clientAddress, 1);                             // Сохраняем в куках адрес сессии
            setCookie("user.clid", w().user.clid, 1);                                               // Сохраняем в куках идентификатор клиента

            w().ueb.unregisterHandler("general", onMessage);                                        // Снимаем регистрацию для публичного адреса
                                                                                                    // И регистрируемся по адресу клиентской сессии
            w().ueb.registerHandler(w().user.clientAddress, {"action":"registration","usid":w().user.usid,"clid":w().user.clid,"address":w().user.clientAddress}, onMessage);
        }else{
            log.error("Unexpected message in channel general");
            log.error(msg);
            return;
        }
    }else if(msg.address === w().user.clientAddress){                                               // Обработка сообщений на персональный адрес клиента
        if(body.action === "error"){
            log.error("from "+body.from+": "+body.text);                                            // Обработка ошибки со стороны сервера
            return;
        }else if((w().user.status === "registration") && (body.action === "serverConfirm")){        // Если это подтверждение регистрации по клиентскому адресу
            log.debug("Session confirmed from server");

            w().user.status = "ready";                                                               // Выставляем статус готовности клиентской сессии

            if(loadScript("core/js/contentManager")){                                                // Синхронно подгружаем скрипт базового контентного модуля
                log.debug("Main content module loaded");
            }else{
                log.error("Error loading main content module");
            }
        }
    }else{
        log.error("Unexpected message in channel "+msg.address);
        log.error(msg);
        return;
    }
}









