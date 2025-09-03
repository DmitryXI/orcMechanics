/* Модуль входа в игру Крестики-нолики */
{
let mainFormId = "TicTacToe_entrance";                     // Устанавливаем имя формы глобально в рамках модуля



function TicTacToe_entrance_main(){
    let selGameFrm = null;

    if(f(mainFormId) === null){             // Навешиваем обработчики и пр. только если форма ещё не загружена и не обработана
        selGameFrm = addHTMLForm("TicTacToe/html/entrance", mainFormId, [80, 80], TicTacToe_entrance_cooker, []);
        selGameFrm.getHTMLElement("back").addEventListener('click', () => {             // Устанавливаем обработчик на конпку "Назад"
            delHTMLForm(mainFormId);                                                    // Полностью удаляем форму выбора игры
            w().user.stage = "selectGame";                                              // Выставляем текущий этап
            log.data.debug("Set user.stage: "+w().user.stage);
            core_selectGame_main();                                                     // Вызываем входную функцию модуля выбора игры
        });
        selGameFrm.getHTMLElement("create").addEventListener('click', () => {           // Устанавливаем обработчик на конпку "Создать"
            w().user.stage = "createGame";                                              // Выставляем текущий этап
            log.data.debug("Set user.stage: "+w().user.stage);
            selGameFrm.getHTMLElement("create").disabled = true;                        // Блокируем кнопку создания игры до получения ответа
            sendMsg("core", "createNewGame", {"game":"TicTacToe","params":{}});         // Отправляем запрос на создание игровой сессии с параметрами
        });
    }else{
        selGameFrm = f(mainFormId);
    }

    TicTacToe_entrance_showEntrance();                                                     // Показываем форму входа
}



// Отображение формы входа в игру
function TicTacToe_entrance_showEntrance(parentId=null){
    log.func.debug6("TicTacToe_entrance_showEntrance: parentId: "+parentId);

    if(parentId === null){
        parentId = "body";
    }

    let parent = d(parentId);

    if(!(parent instanceof HTMLElement)){
        log.error("TicTacToe_entrance_showEntrance: Can't get parent element with id "+parentId);
        return;
    }

    let formElement = f(mainFormId,0);

    if(formElement !== null){
        if(d(mainFormId, parent) === null){
            parent.appendChild(getFormElement(mainFormId));
        }
    }else{
        log.error("TicTacToe_entrance_showEntrance: entrance form not found");
    }
}

// Создатель формы входа в игру
function TicTacToe_entrance_cooker(srcHtml, formId, gamesList){
    log.func.debug6("TicTacToe_entrance_cooker: srcHtml: "+srcHtml+", formId: "+formId+", gamesList: ", gamesList);

    let html = srcHtml.replaceAll("${id}", formId).replaceAll("${z}", 1);
    let elements = getElementsFromHTML(html);

    return elements;
}

}
