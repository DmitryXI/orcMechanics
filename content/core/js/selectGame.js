/* Модуль формы выбора игры */


// Вызываем стартовую функцию
selectGame_main();


function selectGame_main(){
    let selGameFrm = addHTMLForm("core/html/selectGame", "core_selectGame");
//    let selGameFrm = addHTMLForm("core/html/selectGame", "core_selectGame", [], core_selectGame_cooker, ["core_selectGame"]);
//    selGameFrm.getHTMLElement("entrance").addEventListener('click', () => {
//        reqNameFrm.getHTMLElement("entrance").disabled = true;
//        sendMsg("core", "setPlayerName", {"name":reqNameFrm.getHTMLElement("login").value});
//        sendMsg("core", "getGamesList");
//    });
//
//    core_showSelectGameForm();         // Отображаем форму в документе
}