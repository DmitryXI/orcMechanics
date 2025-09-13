/* Модуль формы входа */
{
let mainFormId = "core_requestName";                     // Устанавливаем имя формы глобально в рамках модуля

function core_requestName_main(){

    let reqNameFrm = null;

    if(f("core_requestName") === null){             // Навешиваем обработчики и пр. только если форма ещё не загружена и не обработана
        reqNameFrm = addHTMLForm("core/html/requestName", "core_requestName", [], core_requestName_cooker, ["core_requestName", "text...."]);
        reqNameFrm.getHTMLElement("entrance").addEventListener('click', () => {
            reqNameFrm.getHTMLElement("entrance").disabled = true;
            sendMsg("core", "setPlayerName", {"name":reqNameFrm.getHTMLElement("login").value});
            sendMsg("core", "getGamesList");
        });
    }else{                                          // Если форма использовалась раньше, то включаем кнопку и указываем имеющееся имя игрока
        reqNameFrm = f("core_requestName");
        reqNameFrm.getHTMLElement("entrance").disabled = false;
        reqNameFrm.getHTMLElement("login").value = w().user.name;
    }

    core_requestName_showRequestNameForm();         // Отображаем форму в документе
}

// Отображение формы входа
function core_requestName_showRequestNameForm(parentId=null){
    log.func.debug6("core.core_showRequestNameForm: parentId: "+parentId);

    if(parentId === null){
        parentId = "body";
    }

    let parent = d(parentId);

    if(!(parent instanceof HTMLElement)){
        log.error("Can't get parent element with id "+parentId);
        return;
    }

    if(w().user.name !== null){                                         // Если имя игрока уже задано, заполняем форму
        f(mainFormId).getHTMLElement("login").value = w().user.name;
    }

    if(d("core_requestName", parent) === null){
        core_requestName_onResize(f("core_requestName",0));
        parent.appendChild(getFormElement("core_requestName"));
    }else{
        core_requestName_onResize(f("core_requestName",0));
    }
}

// Создатель формы входа на базе сырого HTML
// Вообще он здесь не нужен (хватит и предустановленного в ядре, но просто... для теста)
function core_requestName_cooker(srcHtml, formId, anyText){
//    log.func.debug6("core.core_requestName_cooker: srcHtml: "+srcHtml+", formId: "+formId);
    log.func.debug6("core.core_requestName_cooker(srcHtml, formId, anyText)", srcHtml, formId, anyText);

    let html = srcHtml.replaceAll("${id}", formId).replaceAll("${z}", 1);
    let elements = getElementsFromHTML(html);
    elements[1].innerHTML += " - однозначно"

    return elements;
}

// Обработчик изменения рабочей области для формы
function core_requestName_onResize(form){
    log.func.debug6("core.core_requestName_onResize: form: ", form);

    let formId = form.id;

    let screen = getWorkSize();
    let w = Math.trunc(screen.width/3)
    let h = Math.trunc(screen.height/3)
    form.style.left = w+"px"
    form.style.top = h+"px"
    form.style.width = form.style.left
    form.style.height = form.style.top

    let loginInp = form.querySelector("#"+formId+"_login")
    loginInp.style.left = Math.trunc(w/4)+"px"
    loginInp.style.width = Math.trunc(w/2)+"px"
    loginInp.style.top = Math.trunc(h/4)+"px"
    loginInp.style.height = Math.trunc(h/2)+"px"
    loginInp.style.fontSize = Math.trunc(h/5)+"px"

    let loginBtn = form.querySelector("#"+formId+"_entrance")
    loginBtn.style.left = Math.trunc(w/4)+"px"
    loginBtn.style.width = Math.trunc(w/2)+"px"
    loginBtn.style.top = Math.trunc(h/4)*3+"px"
    loginBtn.style.height = Math.trunc(h/4)+"px"
    loginBtn.style.fontSize = Math.trunc(h/6)+"px"
}



//addHTMLForm("core/html/requestName", "core_requestName");
//log.debug(reqNameFrm.getHTMLElement());
//log.debug(reqNameFrm.getHTMLElement("login"));
//log.debug(reqNameFrm.getHTMLElement("poeben"));
//log.debug(reqNameFrm.getHTMLElement("hueten"));
//log.debug(reqNameFrm.getHTMLElement("undefined"));
//log.debug(reqNameFrm);
//log.debug(f("core_requestName"));
//log.debug(f("core_requestName", 0));
//log.debug(f("core_requestName").getFormElement(1));
//loginBtn.addEventListener('click', () => {getFromUrl("POST", window.CPJ.basePath, false, showSelectGameForm, null, null, {"action":answer.for,"usessid":window.CPJ.usessid,"login":$("#etrFrm_login").value}, {"cache-control":"no-cache, no-store, must-revalidate","pragma":"no-cache","expires":"0"}, "body:"+formId, true)});
//window.CPJ.activeForms[formId] = {"function":"showEntranceForm", "params":["refresh"]}
//delHTMLForm("core_requestName");
//    removeHTMLForm(reqNameFrm.id);
}