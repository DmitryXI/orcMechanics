/* Модуль непосредственно игры */
{
    let mainFormId = "TicTacToe_battlefield";                       // Устанавливаем имя формы глобально в рамках модуля
    let mainForm   = null;                                          // Основная форма модуля (форма в понимании движка (не HTML-элемент))
    w().user.onGameMessage = TicTacToe_battlefield_onGameMessage;   // Вешаем обработчик входящих сообщений от игры
    let playersList = null;                                         // Список имён участников сессии
    let youTurn = null;                                             // Флаг владения ходом
    let canvas = null;                                              // Холст, на котором рисуем игровое поле
    let field  = {"w":0,"h":0,"cell":null};                         // Данные игрового поля: w - ширина, h - высота, cell - массив с ячейками
    let cellsPos = null;                                            // Массив с координатами клеток сетки на канвасе



    function TicTacToe_battlefield_main(){                          // Входная функция модуля
        log.func.debug6("TicTacToe_battlefield_main()");

        if(f(mainFormId) === null){                                                         // Навешиваем обработчики и пр. только если форма ещё не загружена и не обработана
            mainForm = addHTMLForm("TicTacToe/html/battlefield", mainFormId, [100, 100]);
            d("body").innerHTML = "";                                                       // Обнуляем содержимое body
            canvas = mainForm.getHTMLElement("canvas");                                     // Выносим в переменную модуля ссылку элемент canvas

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

            canvas.addEventListener('click', function(e) {                                  // Цепляем обработчик кликов к канвасу
                TicTacToe_battlefield_canvasClick(e);
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
                TicTacToe_battlefield_onResize(mainForm);                                   // Размеры элементы появляются только у видимых элементов, поэтому после добавления в документ сразу делаем ресайз
            }
        }else{
            log.error("TicTacToe_battlefield_showForm: battlefield main form not found");
        }
    }

    function TicTacToe_battlefield_onResize(form){                                         // Обработка события ресайза клиентской области
        log.func.debug6("TicTacToe_battlefield_onResize(form)", form);

        if(form === null){
            log.error("TicTacToe_battlefield_onResize: Can't get main form");
        }

        let formElement = form.getHTMLElement();
        let menu        = form.getHTMLElement("menu");
        let canvas      = form.getHTMLElement("canvas");


        let screen = getWorkSize();

        let width = Math.trunc(screen.width);
        let height = Math.trunc(screen.height);

        formElement.style.left   = Math.trunc((screen.width-width)/2)+"px";
        formElement.style.top    = Math.trunc((screen.height-height)/2)+"px";
        formElement.style.width  = width+"px";
        formElement.style.height = height+"px";

//        let formRect = formElement.getBoundingClientRect();
//        let menuRect = menu.getBoundingClientRect();
//        width  = formElement.clientWidth;
//        height = formElement.clientHeight - menu.offsetHeight;

        width  = formElement.clientWidth;
        height = formElement.clientHeight - menu.offsetHeight - 4;

        canvas.width  = width;
        canvas.height = height;

        TicTacToe_battlefield_paintField();     // Перерисовываем канвас
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

    // Приём данных игрового поля
    function TicTacToe_battlefield_receiveField(newField){
        log.func.debug6("TicTacToe_battlefield_receiveField(newField)", newField);

        field.w = newField[0].length;
        field.h = newField.length;
        field.cell = newField;
    }

    // Отрисовка игрового поля
    function TicTacToe_battlefield_paintField(){
        log.func.debug6("TicTacToe_battlefield_paintField()");

        if(field.cell === null){ return; }

        let lineThickness = 3;                  // Толщина линий игрового поля в пикселях

        // Размеры
        let width  = Math.trunc(canvas.clientWidth * (w().user.scale/100)) - lineThickness;         // Т.к. линия может быть жирной, то уменьшаем доступную высоту и ширину поля на одну линию
        let height = Math.trunc(canvas.clientHeight * (w().user.scale/100)) - lineThickness;
        if(width > height){ width = height; }else{ height = width; }                    // Квадратизируем поле
        let left   = Math.trunc((canvas.clientWidth - width)/2);
        let top    = Math.trunc((canvas.clientHeight - height)/2);
        let stepX  = Math.trunc(width/field.w);
        let stepY  = Math.trunc(height/field.h);
        width  = stepX*field.w;                                                         // Пересчитываем ширину и высоту, т.к. мы отбрасываем дробные части при делении на квадраты
        height = stepY*field.h;
        cellsPos = [];                                                                  // Формат записи: x1, x2, y1, y2, x, y (x,y - индексы клетки поля в field)

        if (canvas.getContext) {
            let ctx = canvas.getContext("2d");
            ctx.clearRect(0, 0, canvas.clientWidth, canvas.clientHeight);               // Очищаем холст
            ctx.fillStyle = "Black";                                                    // Задаём цвет заливки
            ctx.lineWidth = 1;                                                          // Не используем линии толще единицы

            // Рисуем сетку
            for(x = 0; x <= field.w; x++){                                              // Вертикальные линии
                ctx.fillRect(left+(x*stepX), top, lineThickness, height+lineThickness); // Рисуем линии прямоугольниками (если рисовать линию линией с толщиной больше единицы, то она будет центроваться по линии в один пиксель)
            }

            for(y = 0; y <= field.h; y++){                                              // Горизонтальные линии
                ctx.fillRect(left, top+(y*stepY), width+lineThickness, lineThickness);
            }

            // Выставляем шрифт и размер шрифта и считаем отступы для центровки в ячейке
            let cellWidth  = stepX - lineThickness;
            let cellHeight = stepY - lineThickness;
            let cellFont   = cellWidth;

            ctx.font = cellFont+"px serif";                 // Ставим размер шрифта в размер клетки

            let markSizes = [];

            for(i=0; i < playersList.length; i++){          // Считаем отступы для номера каждого участника
//                markSizes.push({"dw":Math.trunc((stepX-lineThickness-ctx.measureText(i).width)/2), "dh":Math.trunc((stepX-lineThickness-ctx.measureText(i).hangingBaseline)/2)});
                markSizes.push({"dw":Math.trunc((cellWidth-ctx.measureText(i).width)/2), "dh":Math.trunc((cellHeight-ctx.measureText(i).hangingBaseline)/2)});
            }

            for(x=0; x < field.w; x++){                     // Расставляем метки
                for(y=0; y < field.h; y++){
                    if(field.cell[x][y] !== null){
                        ctx.fillText(field.cell[x][y], left+(x*stepX)+markSizes[field.cell[x][y]].dw, top+(y*stepY)+cellHeight-markSizes[field.cell[x][y]].dh);
                    }
                    cellsPos.push([left+(x*stepX)+lineThickness, left+((x+1)*stepX), top+(y*stepY)+lineThickness, top+((y+1)*stepY), x, y]);
                }
            }
        }else{
            log.error("Can't get canvas content");
        }
    }

    // Обработка кликов по канвасу
    function TicTacToe_battlefield_canvasClick(event) {
        log.func.debug6("TicTacToe_battlefield_canvasClick(event)", event);

        if(!youTurn){                                   // Если ход принадлежит другому игроку, возвращаемся
            return;
        }

        let rect = canvas.getBoundingClientRect();
        let xPx  = event.clientX - rect.left;
        let yPx  = event.clientY - rect.top;
        let x    = null;
        let y    = null;
        log.debug("canvasCilck: x: " + xPx + " y: " + yPx);

        for(cellPos of cellsPos){                                                     // Проверяем попадание по клетке
            if((xPx >= cellPos[0]) && (xPx <= cellPos[1]) && (yPx >= cellPos[2]) && (yPx<= cellPos[3])){
                x = cellPos[4];
                y = cellPos[5];
                break;
            }
        }

        if(x === null){                                             // Если x = null, значит ни в одну клетку не попали
            return;
        }
        log.debug("Clicked to cell: x: " + x + " y: " + y);

        if(field.cell[x][y] !== null){
            alert("Сюда тыкать нельзя! Клетка "+x+", "+y+" уже занята");
            return;
        }

        field.cell[x][y] = w().user.numberInGame;
        sendMsg(w().user.gameAddress, "setTurn", {"x":x.toString(),"y":y.toString()});
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
                TicTacToe_battlefield_receiveField(msg.field);
                log.data.debug("Set playersList: ", playersList);
                log.data.debug("Set user.numberInGame: ", w().user.numberInGame);
                log.data.debug("Set youTurn: ", youTurn);
                log.data.debug("Set field: ", field);
                TicTacToe_battlefield_updStatus(msg.turnOf);                                 // Обновляем статусы игроков
                TicTacToe_battlefield_paintField();                                          // Отрисовываем поле
            break;
            case "getField":                                                                 // Заполняем игровое поле
                TicTacToe_battlefield_receiveField(msg.field);
                log.data.debug("Set field: ", field);
                TicTacToe_battlefield_paintField();                                          // Отрисовываем поле
            break;
            case "nextTurn":                                                                 // Заполняем все данные и отрисовываем всё, что нужно
                if(w().user.numberInGame === Number(msg.turnOf)){                            // Вычисляем флаг владения ходом
                    youTurn = true;
                }else{
                    youTurn = false;
                }

                if(msg.newPoints !== null){
                    for(i in msg.newPoints){
                        field.cell[Number(msg.newPoints[i][0])][Number(msg.newPoints[i][1])] = [Number(msg.newPoints[i][2])]
                    }
                }

                log.data.debug("Set youTurn: ", youTurn);
                TicTacToe_battlefield_updStatus(msg.turnOf);
                TicTacToe_battlefield_paintField();                                             // Отрисовываем поле
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