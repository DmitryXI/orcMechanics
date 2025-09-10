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
//                TicTacToe_entrance_main();                                                       // Вызываем входную функцию модуля входа в игру
            });
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




    function TicTacToe_awaiting_onGameMessage(msg){
        log.func.debug6("TicTacToe_awaiting_onGameMessage: msg: ", msg);


    }
}