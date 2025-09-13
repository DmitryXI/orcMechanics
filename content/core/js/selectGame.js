/* Модуль формы выбора игры */
{
let mainFormId = "core_selectGame";                     // Устанавливаем имя формы глобально в рамках модуля

// Вызываем стартовую функцию
//core_selectGame_main();


function core_selectGame_main(){
    let selGameFrm = null;

    if(f(mainFormId) === null){             // Навешиваем обработчики и пр. только если форма ещё не загружена и не обработана
        selGameFrm = addHTMLForm("core/html/selectGame", mainFormId, [80, 80], core_selectGame_cooker, [w().user.gameList]);
        selGameFrm.getHTMLElement("back").addEventListener('click', () => {             // Устанавливаем обработчик на конпку "Назад"
            removeHTMLForm(mainFormId);                                                 // Удаляем форму выбора игры
            w().user.stage = "entrance";                                                // Выставляем текущий этап в entrance, т.к. это ожидаемо значение для входной функции модуля
            log.data.debug("Set user.stage: "+w().user.stage);
            core_requestName_main();                                                    // Вызываем входную функцию модуля запроса имени
        });

        if(w().user.gamesList === null){                                                // Если форма не загружена и список игр пуст, скорее всего было обновление страницы и его стоит загрузить
            sendMsg("core", "getGamesList");
        }
    }else{
        selGameFrm = f(mainFormId);
    }

    selGameFrm.getHTMLElement("user_name").innerHTML = w().user.name;                   // Подставляем имя
    selGameFrm.getHTMLElement("gamesList").innerHTML = "";                              // Чистим список элементов с играми

    // Заполняем список игр в форме
    let item;

    for(gameName in w().user.gamesList){
        item = selGameFrm.getHTMLElement("item_").cloneNode(true);
        item.id += gameName;
        item.innerHTML = w().user.gamesList[gameName];
        item.addEventListener('click', () => {
                     sendMsg("core", "getGameEntrance", {"game":gameName});
        });
        selGameFrm.getHTMLElement("gamesList").appendChild(item);
    }

    core_selectGame_showSelectGame();
}


// Отображение формы выбора игры
function core_selectGame_showSelectGame(parentId=null){
    log.func.debug6("core.core_showSelectGame: parentId: "+parentId);

    if(parentId === null){
        parentId = "body";
    }

    let parent = d(parentId);

    if(!(parent instanceof HTMLElement)){
        log.error("Can't get parent element with id "+parentId);
        return;
    }

    let formElement = f(mainFormId,0);

    if(formElement !== null){
        // Для сложных форм здесь не лишним будет вызвать onResize, но в данном случае нет необходимости
        if(d(mainFormId, parent) === null){
            parent.appendChild(getFormElement(mainFormId));
        }
    }else{
        log.error("Select game form not found");
    }
}

// Создатель формы входа на базе сырого HTML
function core_selectGame_cooker(srcHtml, formId, gameList){
    log.func.debug6("core.core_selectGame_cooker: srcHtml: "+srcHtml+", formId: "+formId+", gameList: ", gameList);

    let html = srcHtml.replaceAll("${id}", formId).replaceAll("${z}", 1);
    let elements = getElementsFromHTML(html);

    return elements;
}
}