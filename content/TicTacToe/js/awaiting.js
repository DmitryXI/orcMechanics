/* Модуль ожидания старта игры */
{
    let mainFormId = "TicTacToe_awaiting";                          // Устанавливаем имя формы глобально в рамках модуля
    let mainForm   = null;                                          // Основная форма модуля (форма в понимании движка (не HTML-элемент))
    w().user.onGameMessage = TicTacToe_awaiting_onGameMessage;      // Вешаем обработчик входящих сообщений от игры


    function TicTacToe_awaiting_main(){                                                     // Входная функция модуля
        log.func.debug6("TicTacToe_awaiting_main");

        if(f(mainFormId) === null){                                                         // Навешиваем обработчики и пр. только если форма ещё не загружена и не обработана
            mainForm = addHTMLForm("TicTacToe/html/awaiting", mainFormId, [80, 80]);
            delHTMLForm("TicTacToe_entrance");                                              // Полностью удаляем форму входа в игру
            d("body").innerHTML = "";                                                       // Для порядка обнуляем содержимое body

            mainForm.getHTMLElement("back").addEventListener('click', () => {                    // Устанавливаем обработчик на конпку "Назад"
                sendMsg("core", "leaveGame", {                                                  // Отправляем сообщение серверу о выходе из игры
                    "game":"TicTacToe",
                    "gsid":w().user.gsid
                });
                delHTMLForm(mainFormId);                                                         // Полностью удаляем форму входа в игру
                w().user.stage = "selectGame";                                                   // Выставляем текущий этап как выбор игры
                log.data.debug("Set user.stage: "+w().user.stage);
                sendMsg("core", "getGameEntrance", {"game":"TicTacToe"});                        // Отправляем запрос на получение формы входа (в текущей реализации подругому мы не получим список сессий)
            });

            sendMsg(w().user.gameAddress, "getPlayersList", {"game":"TicTacToe","gsid":w().user.gsid}); // Отправляем запрос на создание игровой сессии с параметрами
        }

        TicTacToe_awaiting_showForm();                                                        // Показываем форму
    }

    function TicTacToe_awaiting_showForm(parentId=null){                                // Отображение формы входа в игру
        log.func.debug6("TicTacToe_awaiting_showForm: parentId: "+parentId);

        if(parentId === null){
            parentId = "body";
        }

        let parent = d(parentId);

        if(!(parent instanceof HTMLElement)){
            log.error("TicTacToe_awaiting_showForm: Can't get parent element with id "+parentId);
            return;
        }

        if(mainForm !== null){
            if(d(mainFormId, parent) === null){
                parent.appendChild(getFormElement(mainFormId));
            }
        }else{
            log.error("TicTacToe_awaiting_showForm: entrance form not found");
        }
    }

    function TicTacToe_awaiting_fillPlayersList(playersList){                           // Заполняем список игроков в форме
        log.func.debug6("TicTacToe_awaiting_fillPlayersList: playersList: ", playersList);

        if(mainForm !== null){
            let list    = mainForm.getHTMLElement("list");                          // Элемент со списком
            let srcItem = mainForm.getFormElement(1);                               // Шаблон элемента списка
            let item;                                                               // Элемент списка

            list.innerHTML = "";                                                    // Чистим список

            for(playerNum in playersList){
                item = srcItem.cloneNode(true);
                item.children[0].innerHTML = playerNum;                             // Указываем номер по порядку
                item.children[1].innerHTML = playersList[playerNum].name;           // Указываем имя
                item.children[2].innerHTML = playersList[playerNum].type;           // Указываем тип игрока
                item.children[3].innerHTML = playersList[playerNum].status;         // Указываем статус

                list.appendChild(item);                                             // Добавляем HTML-элемент
            }
        }
    }

    function TicTacToe_awaiting_onGameMessage(msg){
        log.func.debug6("TicTacToe_awaiting_onGameMessage: msg: ", msg);

        switch(msg.action) {
            case "alert":                                                                       // Отображаем алерт
                alert(msg.text);
            break;
            case "playerLeave":
                // Можно отобразить уведомление о отключении игрока...
            break;
            case "playersList":                                                                 // Заполняем/обновляем список игроков
                TicTacToe_awaiting_fillPlayersList(msg.playersNames);
            break;
            case "startGame":                                                                   // Запускаем игру
                TicTacToe_awaiting_fillPlayersList(msg.playersNames);                           // Для красоты заполняем список игроков
                delHTMLForm(mainFormId);                                                        // Полностью удаляем форму входа в игру
                log.info("Starting game");
                if(msg.resourceId === undefined){ log.error("TicTacToe_awaiting_onGameMessage: recourceId not set"); return; }
                if(msg.appName === undefined){ log.error("TicTacToe_awaiting_onGameMessage: appName not set"); return; }
                if(msg.moduleName === undefined){ log.error("TicTacToe_awaiting_onGameMessage: moduleName not set"); return; }

                if(loadScript(msg.resourceId)){                            // Синхронно подгружаем скрипт модуля непосредственно игры
                    log.debug(msg.moduleName+" module loaded");
                    let mainFunctionName = msg.appName+"_"+msg.moduleName+"_main";
                    log.debug("Call "+mainFunctionName);
                    if(window[mainFunctionName] instanceof Function){       // Вызываем входную функцию
                        window[mainFunctionName]();
                    }
                }else{
                    log.error("TicTacToe_awaiting_onGameMessage: Error loading starting game module");
                }
            break;
            case "sessionRemoved":
                alert(msg.reason);                                                               // Отображаем алерт с причиной удаления сессии
                delHTMLForm(mainFormId);                                                         // Полностью удаляем форму входа в игру
                w().user.stage = "selectGame";                                                   // Выставляем текущий этап как выбор игры
                log.data.debug("Set user.stage: "+w().user.stage);
                sendMsg("core", "getGameEntrance", {"game":"TicTacToe"});                        // Отправляем запрос на получение формы входа (в текущей реализации подругому мы не получим список сессий)
            break;
            default:
                log.error("TicTacToe_awaiting_onGameMessage: Unknown action: "+msg.action);
                return;
            break;
        }
    }
}