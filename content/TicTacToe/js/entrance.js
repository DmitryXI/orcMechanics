/* Модуль входа в игру Крестики-нолики */
{
let mainFormId = "TicTacToe_entrance";                          // Устанавливаем имя формы глобально в рамках модуля
let selectedGame = {"gsid":null,"address":null,"name":null,"el":null}     // Хранилище параметров выбранной игры для подключения



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
            let players = {};

            log.debug("Starting game...");
            log.debug("Game name: "+selGameFrm.getHTMLElement("gameName").value);
            log.debug("Players count: "+selGameFrm.getHTMLElement("playersCount").value);
            log.debug("Field size: "+selGameFrm.getHTMLElement("fieldSizeX").value+"x"+selGameFrm.getHTMLElement("fieldSizeY").value);
            log.debug("Winline len: "+selGameFrm.getHTMLElement("winLineLen").value);
            for(let i=0; i < selGameFrm.getHTMLElement("playersCount").value; i++){
                log.debug(selGameFrm.getHTMLElement("playerName_"+i).innerHTML+"="+selGameFrm.getHTMLElement("playerType_"+i).value);
                players["playerName_"+i] = selGameFrm.getHTMLElement("playerType_"+i).value;
            }

            sendMsg("core", "createNewGame", {"game":"TicTacToe","params":{
                "gameName":selGameFrm.getHTMLElement("gameName").value,
                "playersCount":selGameFrm.getHTMLElement("playersCount").value,
                "fieldSizeX":selGameFrm.getHTMLElement("fieldSizeX").value,
                "fieldSizeY":selGameFrm.getHTMLElement("fieldSizeY").value,
                "winLineLen":selGameFrm.getHTMLElement("winLineLen").value,
                "players":players}});                                                        // Отправляем запрос на создание игровой сессии с параметрами
        });

        selGameFrm.getHTMLElement("playersCount").addEventListener('change', function() {    // Устанавливаем обработчик для события change на селект с количеством игроков
            TicTacToe_entrance_selectMaker(this.value);
        }, false);

        selGameFrm.getHTMLElement("join").addEventListener('click', () => {             // Устанавливаем обработчик на конпку "Подключиться"
            w().user.stage = "joinGame";                                                // Выставляем текущий этап
            log.data.debug("Set user.stage: "+w().user.stage);
            selGameFrm.getHTMLElement("join").disabled = true;                          // Блокируем кнопку подключения
            log.debug("Join to game: ", selectedGame);

            sendMsg("core", "joinToGame", {"game":"TicTacToe","gsid":selectedGame.gsid}); // Отправляем запрос на создание игровой сессии с параметрами
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

    selectedGame = {"gsid":null,"address":null,"name":null,"el":null}           // Каждый раз перед показом/обновлением формы сбрасываем выбранную игровую сессию

    let formElement = f(mainFormId,0);

    if(formElement !== null){

        TicTacToe_entrance_selectMaker(f(mainFormId).getHTMLElement("playersCount").value);         // Собираем блок с типами игроков для выбранного количества
        TicTacToe_entrance_listMaker(w().user.gameSessionsList);

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

// Создатель блока выбора типа игроков
function TicTacToe_entrance_selectMaker(usrCount){
    log.func.debug6("TicTacToe_entrance_selectMaker: usrCount: "+usrCount);

    let playersTypes = f(mainFormId).getHTMLElement("playersTypes");
    let playersTypesLine = f(mainFormId, 1);
    let cPlayersTypesLine;

    if(playersTypes === null) {
        log.func.error("TicTacToe_entrance_selectMaker: Element playersTypes not found in form "+mainFormId);
        return;
    }
    if(playersTypesLine === null) {
        log.func.error("TicTacToe_entrance_selectMaker: Element playersTypesLine not found in form "+mainFormId);
        return;
    }

    playersTypes.innerHTML = "";

    for(let i = 0; i < usrCount; i++){
        cPlayersTypesLine = playersTypesLine.cloneNode(true);
        cPlayersTypesLine.children[0].id += i;
        cPlayersTypesLine.children[1].id += i;

        if(i == 0){
            cPlayersTypesLine.children[0].innerHTML = w().user.name;
        }else{
            cPlayersTypesLine.children[0].innerHTML = "Игрок "+i;
        }

        playersTypes.appendChild(cPlayersTypesLine);
    }
}

// Создатель списка доступных игровых сессий
function TicTacToe_entrance_listMaker(sessionsList){
    log.func.debug6("TicTacToe_entrance_listMaker: sessionsList: ", sessionsList);

    let list = f(mainFormId).getHTMLElement("sessions_list");               // Элемент со списком
    let srcItem = f(mainFormId,2);                                          // Шаблон элемента списка
    let item;                                                               // Элемент списка

    list.innerHTML = "";

    for(session in sessionsList){
        item = srcItem.cloneNode(true);
        item.children[0].innerHTML = sessionsList[session].name;
        item.children[1].innerHTML = sessionsList[session].availableSeats+" / "+sessionsList[session].playersCount;
        let gmSid = sessionsList[session].gsid;
        let gmName = sessionsList[session].name;
        let gmAddress = sessionsList[session].address;

        item.addEventListener('click', function() {                                                      // Устанавливаем обработчик на выбор сессии в списке
                        selectedGame.gsid = gmSid;
                        selectedGame.name = gmName;
                        selectedGame.address = gmAddress;
                        log.data.debug("Set TicTacToe_selectedGame:", selectedGame);
                        if(selectedGame.el instanceof HTMLElement){
                            selectedGame.el.style.borderColor = "#FFF";
                        }
                        selectedGame.el = this;
                        this.style.borderColor = "#999";
                        f(mainFormId).getHTMLElement("join").disabled = false;
                    });

        list.appendChild(item);
    }

}

}
