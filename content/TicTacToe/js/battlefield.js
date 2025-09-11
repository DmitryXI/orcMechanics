/* Модуль непосредственно игры */
{
    let mainFormId = "TicTacToe_battlefield";                       // Устанавливаем имя формы глобально в рамках модуля
    let mainForm   = null;                                          // Основная форма модуля (форма в понимании движка (не HTML-элемент))
    w().user.onGameMessage = TicTacToe_battlefield_onGameMessage;   // Вешаем обработчик входящих сообщений от игры



    function TicTacToe_battlefield_main(){                          // Входная функция модуля
        log.func.debug6("TicTacToe_battlefield_main");

        alert("Игра началась...");
    }

    // Обработка сообщений сервера
    function TicTacToe_battlefield_onGameMessage(msg){
        log.func.debug6("TicTacToe_battlefield_onGameMessage: msg: ", msg);

    }

}