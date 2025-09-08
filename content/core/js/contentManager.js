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

        if(loadScript("core/js/requestName")){                                               // Синхронно подгружаем скрипт модуля формы входа
            log.debug("requestName module loaded");
            core_requestName_main();                                                         // Вызываем входную функцию модуля
        }else{
            log.error("core_contentManager_main: Error loading requestName module");
        }
    }
}

// Обработчик сообщений от клиента
function core_contentManager_onMessage(msg){
    log.func.debug6("core.contentManager_onMessage: msg: ", msg);

    log.debug("core_contentManager_onMessage: Received message: ",msg);

            switch(msg.action) {
              case "alert":
                alert(msg.text);
                break;
              case "setPlayerName":
                w().user.name = msg.name;
                log.data.debug("Set user.name: "+w().user.name);
                break;
              case "setGamesList":
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
                case "setGameEntrance":
                    log.data.debug("Received game entrance params: ", msg);
                    if(msg.resourceId === undefined){ log.error("core_contentManager_onMessage: recourceId not set"); return; }
                    if(msg.appName === undefined){ log.error("core_contentManager_onMessage: appName not set"); return; }
                    if(msg.moduleName === undefined){ log.error("core_contentManager_onMessage: moduleName not set"); return; }
                    if(msg.sessionsList === undefined){ log.error("core_contentManager_onMessage: sessionsList not set"); return; }

                     w().user.gameSessionsList = msg.sessionsList;
                     log.data.debug("Set user.gameSessionsList: ", w().user.sessionsList);
                     log.debug("Received list of game sessions: ", w().user.sessionsList);

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
              default:
                log.error("core_contentManager_onMessage: Unknown action: "+msg.action);
                return;
                break;
            }
}
}