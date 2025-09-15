/* Менеджер контента ядра */
{
log.debug("Core content manager loaded");

core_contentManager_main();

function core_contentManager_main() {
    log.func.debug6("core.contentManager_main");

    // Выставляем текущий этап продвижения к игре
    if(w().user.stage === null){
        w().user.stage = "entrance";
        log.data.debug("Set user.stage: "+w().user.stage);

        sendMsg("core", "getUserEntrance");                                                    // Запрашиваем форму входа/аутентификации пользователя
    }
}

// Обработчик сообщений от клиента
function core_contentManager_onMessage(msg){
    log.func.debug6("core.contentManager_onMessage: msg: ", msg);

//    log.debug8("user.stage = "+w().user.stage+", user.gameAddress = "+w().user.gameAddress);

    if((w().user.stage === "gaming") && (msg.from === w().user.gameAddress)){
        w().user.onGameMessage(msg);
        return;
    }

    switch(msg.action) {
      case "alert":                                                     // Отображаем алерт
        alert(msg.text);
        break;
      case "setUserEntrance":                                           // Принимаем модуль входа пользователя
         if(w().user.stage = "entrance"){                               // Если это на этапе entrance, то удаляем форму входа и подгружаем форму выбора игры
            if(loadScript(msg.resourceId)){                             // Синхронно подгружаем скрипт модуля формы входа пользователя
                log.debug(msg.moduleName+" module loaded");
                let mainFunctionName = msg.appName+"_"+msg.moduleName+"_main";
                log.debug("Call "+mainFunctionName);
                if(window[mainFunctionName] instanceof Function){       // Вызываем входную функцию
                    window[mainFunctionName]();
                }
            }else{
                log.error("core_contentManager_onMessage: Error loading user entrance module");
            }
         }else{
            log.error("core_contentManager_onMessage: Unexpected action "+msg.action+" on current stage "+w().user.stage);
         }
        break;
      case "setPlayerName":
        w().user.name = msg.name;
        log.data.debug("Set user.name: "+w().user.name);
        break;
      case "setGamesList":                                              // Принимаем список доступных игр (если этап entrance, то подключаем модуль)
         w().user.gamesList = msg.list;
         log.data.debug("Set user.gamesList: ", w().user.gamesList);
         log.debug("Received list of games: ", w().user.gamesList);
         if(w().user.stage = "entrance"){                               // Если это на этапе entrance, то удаляем форму входа и подгружаем форму выбора игры
            w().user.stage = "selectGame";
            log.data.debug("Set user.stage: "+w().user.stage);
            removeHTMLForm("core_requestName");                         // Удаляем форму ввода имени
            if(loadScript("core/js/selectGame")){                       // Синхронно подгружаем скрипт модуля формы выбора игры
                log.debug("selectGame module loaded");
                core_selectGame_main();                                 // Вызываем входную функцию модуля
            }else{
                log.error("core_contentManager_onMessage: Error loading selectGame module");
            }
         }
        break;
        case "setGameEntrance":                                         // Отображаем форму входа игры (поставляется самой игрой)
            log.data.debug("Received game entrance params: ", msg);
            if(msg.resourceId === undefined){ log.error("core_contentManager_onMessage: recourceId not set"); return; }
            if(msg.appName === undefined){ log.error("core_contentManager_onMessage: appName not set"); return; }
            if(msg.moduleName === undefined){ log.error("core_contentManager_onMessage: moduleName not set"); return; }
            if(msg.sessionsList === undefined){ log.error("core_contentManager_onMessage: sessionsList not set"); return; }

             w().user.gameSessionsList = msg.sessionsList;
             log.data.debug("Set user.gameSessionsList: ", w().user.gameSessionsList);
             log.debug("Received list of game sessions: ", w().user.gameSessionsList);

            if(w().user.stage = "selectGame"){                             // Если это на этапе entrance, то удаляем форму входа в игру и подгружаем форму выбора игры
                w().user.stage = "gameEntrance";
                log.data.debug("Set user.stage: "+w().user.stage);
                removeHTMLForm("core_selectGame");                         // Удаляем форму ввода имени
                if(loadScript(msg.resourceId)){                            // Синхронно подгружаем скрипт модуля формы входа игры
                    log.debug(msg.moduleName+" module loaded");
                    let mainFunctionName = msg.appName+"_"+msg.moduleName+"_main";
                    log.debug("Call "+mainFunctionName);
                    if(window[mainFunctionName] instanceof Function){       // Вызываем входную функцию
                        window[mainFunctionName]();
                    }
                }else{
                    log.error("core_contentManager_onMessage: Error loading selectGame module");
                }
            }
        break;
        case "setGameSession":                                              // Принимаем игровую сессию и передаём управление игре
            log.data.debug("Received game session: ", msg);
            if(msg.resourceId === undefined){ log.error("core_contentManager_onMessage: recourceId not set"); return; }
            if(msg.appName === undefined){ log.error("core_contentManager_onMessage: appName not set"); return; }
            if(msg.moduleName === undefined){ log.error("core_contentManager_onMessage: moduleName not set"); return; }
            if(msg.gameAddress === undefined){ log.error("core_contentManager_onMessage: gameAddress not set"); return; }
            if(msg.gsid === undefined){ log.error("core_contentManager_onMessage: gsid not set"); return; }
            if(msg.nn === undefined){ log.error("core_contentManager_onMessage: nn not set"); return; }
            w().user.gameAddress  = msg.gameAddress;
            w().user.gsid         = msg.gsid;
            w().user.numberInGame = msg.nn;
            w().user.stage = "gaming";
            log.data.debug("Set user.gsid: "+w().user.gsid);
            log.data.debug("Set user.gameAddress: "+w().user.gameAddress);
            log.data.debug("Set user.numberInGame: "+w().user.numberInGame);
            log.data.debug("Set user.stage: "+w().user.stage);
            if(loadScript(msg.resourceId)){                            // Синхронно подгружаем скрипт модуля формы ожидания игроков (в синглплеере сразу игру)
                log.debug(msg.moduleName+" module loaded");
                let mainFunctionName = msg.appName+"_"+msg.moduleName+"_main";
                log.debug("Call "+mainFunctionName);
                if(window[mainFunctionName] instanceof Function){       // Вызываем входную функцию
                    window[mainFunctionName]();
                }
            }else{
                log.error("core_contentManager_onMessage: Error loading starting game module");
            }
        break;
        case "returnGameSession":                                         // Возвращаем игрока в игровую сессию
            log.data.debug("Received game session for return: ", msg);
            if(msg.resourceId === undefined){ log.error("core_contentManager_onMessage: recourceId not set"); return; }
            if(msg.appName === undefined){ log.error("core_contentManager_onMessage: appName not set"); return; }
            if(msg.moduleName === undefined){ log.error("core_contentManager_onMessage: moduleName not set"); return; }
            if(msg.gameAddress === undefined){ log.error("core_contentManager_onMessage: gameAddress not set"); return; }
            if(msg.gsid === undefined){ log.error("core_contentManager_onMessage: gsid not set"); return; }
            if(msg.nn === undefined){ log.error("core_contentManager_onMessage: nn not set"); return; }
            if(msg.gameStatus === undefined){ log.error("core_contentManager_onMessage: gameStatus not set"); return; }
            if(msg.playersNames === undefined){ log.error("core_contentManager_onMessage: playersNames not set"); return; }
            if(msg.gameStatus === undefined){ log.error("core_contentManager_onMessage: gameStatus not set"); return; }
            w().user.gameAddress  = msg.gameAddress;
            w().user.gsid         = msg.gsid;
            w().user.numberInGame = msg.nn;
            w().user.name         = msg.playersNames[msg.nn].name;
            w().user.stage = "gaming";
            log.data.debug("Set user.gsid: "+w().user.gsid);
            log.data.debug("Set user.gameAddress: "+w().user.gameAddress);
            log.data.debug("Set user.numberInGame: "+w().user.numberInGame);
            log.data.debug("Set user.name: "+w().user.name);
            log.data.debug("Set user.stage: "+w().user.stage);
            log.debug("Trying async load module requestName");
            loadScript("core/js/requestName", true);                         // Асинхронно загружаем модуль входа пользователя (если придётся возвращаться после игры)
            log.debug("Trying async load module selectGame");
            loadScript("core/js/selectGame", true);                          // Асинхронно загружаем модуль выбора игры (если придётся возвращаться после игры)
            if(loadScript(msg.resourceId)){                            // Синхронно подгружаем скрипт модуля формы ожидания игроков (в синглплеере сразу игру)
                log.debug(msg.moduleName+" module loaded");
                let mainFunctionName = msg.appName+"_"+msg.moduleName+"_main";
                log.debug("Call "+mainFunctionName);
                if(window[mainFunctionName] instanceof Function){       // Вызываем входную функцию
                    window[mainFunctionName]();
                }
            }else{
                log.error("core_contentManager_onMessage: Error loading starting game module");
            }
        break;
        case "setGameSessions":                                              // Принимаем список игровых сессий
            log.data.debug("Received game sessions list: ", msg);
            if(msg.sessionsList === undefined){ log.error("core_contentManager_onMessage: sessionsList not set"); return; }

             w().user.gameSessionsList = msg.sessionsList;
             log.data.debug("Set user.gameSessionsList: ", w().user.gameSessionsList);

             if(w().user.onSessionsList instanceof Function){       // Вызываем обработчик модуля для обновления списка сессий
                 w().user.onSessionsList();
             }
        break;
      default:
        log.error("core_contentManager_onMessage: Unknown action: "+msg.action);
        return;
        break;
    }
}
}