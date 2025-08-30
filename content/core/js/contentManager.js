/* Менеджер контента ядра */

log.debug("Core content manager loaded");



//addHTMLForm("core/html/requestName", "core_requestName");
let reqNameFrm = addHTMLForm("core/html/requestName", "core_requestName", [], core_requestName_cooker, ["core_requestName", "text...."]);
reqNameFrm.getHTMLElement("entrance").addEventListener('click', () => {
    log.info("Поехали!!!!");
    reqNameFrm.getHTMLElement("entrance").disabled = true;
    sendMsg("core", "setPlayerName", {"name":reqNameFrm.getHTMLElement("login").value});
    sendMsg("core", "getGamesList");
//    removeHTMLForm(reqNameFrm.id);
});




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

core_showRequestNameForm();
//delHTMLForm("core_requestName");



// Отображение формы входа
function core_showRequestNameForm(parentId=null){

    if(parentId === null){
        parentId = "body";
    }

    let parent = d(parentId);

    if(!(parent instanceof HTMLElement)){
        log.error("Can't get parent element with id "+parentId);
    }

    if(d("core_requestName", parent) === null){
//        core_requestName_onResize();
        parent.appendChild(getFormElement("core_requestName"));
    }else{
//        core_requestName_onResize();
    }
}

function core_requestName_cooker(srcHtml, formId, anyText){

//    log.debug("core_requestName_cooker called");
//    log.debug("source HTML: "+srcHtml);
//    log.debug("formId: "+formId);
//    log.debug("anyText: "+anyText);

    let html = srcHtml.replaceAll("${id}", formId).replaceAll("${z}", 1);
    let elements = getElementsFromHTML(html);
    elements[1].innerHTML += " - однозначно"

    return elements;
}

function core_requestName_onResize(form){

    let formId = form.id;
//    log.debug("core_requestName_onResize called");
//    log.debug(form);
//    log.debug("formId = "+formId);

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


//w().ueb.send("core", JSON.stringify({"address":w().user.address,"from":"hez","action":"dumpForTest","usid":w().user.usid}));
//sendMsg("core", "mainSend!", {"obj":"body"});




