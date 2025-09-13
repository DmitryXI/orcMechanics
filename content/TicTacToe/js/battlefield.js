/* Модуль непосредственно игры */
{
    let mainFormId = "TicTacToe_battlefield";                       // Устанавливаем имя формы глобально в рамках модуля
    let mainForm   = null;                                          // Основная форма модуля (форма в понимании движка (не HTML-элемент))
    w().user.onGameMessage = TicTacToe_battlefield_onGameMessage;   // Вешаем обработчик входящих сообщений от игры



    function TicTacToe_battlefield_main(){                          // Входная функция модуля
        log.func.debug6("TicTacToe_battlefield_main()");

        if(f(mainFormId) === null){                                                         // Навешиваем обработчики и пр. только если форма ещё не загружена и не обработана
            mainForm = addHTMLForm("TicTacToe/html/battlefield", mainFormId, [100, 100]);
            d("body").innerHTML = "";                                                       // Обнуляем содержимое body

            sendMsg(w().user.gameAddress, "getPlayersList", {"game":"TicTacToe","gsid":w().user.gsid}); // Отправляем запрос списка имён игроков
            sendMsg("core", "checkGameStatus", {"game":"TicTacToe","gsid":w().user.gsid});              // Отправляем запрос статуса игры (если уже началась, ожидаем команду на старт в ответ)
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

    // Обработка сообщений сервера
    function TicTacToe_battlefield_onGameMessage(msg){
        log.func.debug6("TicTacToe_battlefield_onGameMessage(msg)", msg);

    }

}