/* Модуль ожидания старта игры */
{
    let mainFormId = "TicTacToe_awaiting";                          // Устанавливаем имя формы глобально в рамках модуля
    w().user.onGameMessage = TicTacToe_awaiting_onGameMessage;      // Вешаем обработчик входящих сообщений от игры

    function TicTacToe_awaiting_onGameMessage(msg){
        log.func.debug6("TicTacToe_awaiting_onGameMessage: msg: ", msg);


    }
}