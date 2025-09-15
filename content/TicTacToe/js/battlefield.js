/* Модуль непосредственно игры */
{
    let mainFormId = "TicTacToe_battlefield";                       // Устанавливаем имя формы глобально в рамках модуля
    let mainForm   = null;                                          // Основная форма модуля (форма в понимании движка (не HTML-элемент))
    w().user.onGameMessage = TicTacToe_battlefield_onGameMessage;   // Вешаем обработчик входящих сообщений от игры
    let playersList = null;                                         // Список имён участников сессии
    let youTurn = null;                                             // Флаг владения ходом



    function TicTacToe_battlefield_main(){                          // Входная функция модуля
        log.func.debug6("TicTacToe_battlefield_main()");

        if(f(mainFormId) === null){                                                         // Навешиваем обработчики и пр. только если форма ещё не загружена и не обработана
            mainForm = addHTMLForm("TicTacToe/html/battlefield", mainFormId, [100, 100]);
            d("body").innerHTML = "";                                                       // Обнуляем содержимое body

            mainForm.getHTMLElement("menu_quit").addEventListener('click', () => {          // Устанавливаем обработчик на конпку "Назад"
                sendMsg("core", "leaveGame", {                                              // Отправляем сообщение серверу о выходе из игры
                    "game":"TicTacToe",
                    "gsid":w().user.gsid
                });
                delHTMLForm(mainFormId);                                                    // Полностью удаляем форму входа в игру
                w().user.stage = "selectGame";                                              // Выставляем текущий этап как выбор игры
                log.data.debug("Set user.stage: "+w().user.stage);
                sendMsg("core", "getGameEntrance", {"game":"TicTacToe"});                   // Отправляем запрос на получение формы входа (в текущей реализации подругому мы не получим список сессий)
            });

            sendMsg(w().user.gameAddress, "getClientPart", {"game":"TicTacToe","gsid":w().user.gsid}); // Отправляем запрос необходимой информации об игре
        }

        TicTacToe_battlefield_showForm();
    }

    function TicTacToe_battlefield_showForm(parentId=null){                                // Отображение основной формы
        log.func.debug6("TicTacToe_battlefield_showForm(parentId=null)", parentId);

        if(parentId === null){
            parentId = "body";
        }

        let parent = d(parentId);

        if(!(parent instanceof HTMLElement)){
            log.error("TicTacToe_battlefield_showForm: Can't get parent element with id "+parentId);
            return;
        }

        if(mainForm !== null){
            if(d(mainFormId, parent) === null){
                parent.appendChild(getFormElement(mainFormId));
            }
        }else{
            log.error("TicTacToe_battlefield_showForm: battlefield main form not found");
        }
    }

    // Обновление статуса на форме
    function TicTacToe_battlefield_updStatus(turnOf){
        log.func.debug6("TicTacToe_battlefield_updStatus(turnOf)", turnOf);
        mainForm.getHTMLElement("menu_status").innerHTML = "Ходит "+playersList[turnOf].name;
        if(youTurn){
            mainForm.getHTMLElement("menu_status").style.color = "yellow";
        }else{
            mainForm.getHTMLElement("menu_status").style.color = "orange";
        }
    }

    // Обработка сообщений сервера
    function TicTacToe_battlefield_onGameMessage(msg){
        log.func.debug6("TicTacToe_battlefield_onGameMessage(msg)", msg);

        switch(msg.action) {
            case "alert":                                                                       // Отображаем алерт
                alert(msg.text);
            break;
            case "playerLeave":
                // Можно отобразить уведомление о отключении игрока...
            break;
            case "setClientPart":                                                               // Заполняем все данные и отрисовываем всё, что нужно
                playersList = msg.playersNames;
                w().user.numberInGame = Number(msg.nn);
                if(Number(msg.nn) === Number(msg.turnOf)){                                     // Вычисляем флаг владения ходом
                    youTurn = true;
                }else{
                    youTurn = false;
                }
                log.data.debug("Set playersList: ", playersList);
                log.data.debug("Set user.numberInGame: ", w().user.numberInGame);
                log.data.debug("Set youTurn: ", youTurn);
                TicTacToe_battlefield_updStatus(msg.turnOf)
            break;
            case "nextTurn":                                                                 // Заполняем все данные и отрисовываем всё, что нужно
                if(w().user.numberInGame === Number(msg.turnOf)){                            // Вычисляем флаг владения ходом
                    youTurn = true;
                }else{
                    youTurn = false;
                }
                log.data.debug("Set youTurn: ", youTurn);
                TicTacToe_battlefield_updStatus(msg.turnOf)
            break;
            case "playersList":                                                                 // Заполняем/обновляем список игроков
                playersList = msg.playersNames;
                log.data.debug("Set playersList: ", playersList);
            break;
            case "sessionRemoved":
                alert(msg.reason);                                                               // Отображаем алерт с причиной удаления сессии
                delHTMLForm(mainFormId);                                                         // Полностью удаляем форму игры
                w().user.stage = "selectGame";                                                   // Выставляем текущий этап как выбор игры
                log.data.debug("Set user.stage: "+w().user.stage);
                sendMsg("core", "getGameEntrance", {"game":"TicTacToe"});                        // Отправляем запрос на получение формы входа (в текущей реализации подругому мы не получим список сессий)
            break;
            default:
                log.error("TicTacToe_battlefield_onGameMessage: Unknown action: "+msg.action);
                return;
            break;
        }
    }

}