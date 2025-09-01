/* Модуль формы выбора игры */


// Вызываем стартовую функцию
selectGame_main();


function selectGame_main(){
    let selGameFrm = addHTMLForm("core/html/selectGame", "core_selectGame", [80, 80], core_selectGame_cooker, [w().user.gameList]);
    selGameFrm.getHTMLElement("user_name").innerHTML = w().user.name;

    // Заполняем список игр в форме
    let item;

    for(gameName in w().user.gamesList){
        item = selGameFrm.getHTMLElement("item_").cloneNode(true);
        item.id += gameName;
        item.innerHTML = w().user.gamesList[gameName];
        item.addEventListener('click', () => {
                     sendMsg(gameName, "getEntranceForm");
        });
        selGameFrm.getHTMLElement().appendChild(item);
    }

    core_showSelectGame();
}


// Отображение формы выбора игры
function core_showSelectGame(parentId=null){
    log.func.debug6("core.core_showSelectGame: parentId: "+parentId);

    if(parentId === null){
        parentId = "body";
    }

    let parent = d(parentId);

    if(!(parent instanceof HTMLElement)){
        log.error("Can't get parent element with id "+parentId);
        return;
    }

    let formElement = f("core_selectGame",0);

    if(formElement !== null){
        // Для сложных форм здесь не лишним будет вызвать onResize, но в данном случае нет необходимости

        if(d("core_selectGame", parent) === null){
            parent.appendChild(getFormElement("core_selectGame"));
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