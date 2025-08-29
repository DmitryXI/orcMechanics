/* Менеджер контента ядра */

log.debug("Core content manager loaded");

w().ueb.send("core", JSON.stringify({"address":w().user.address,"from":"hez","action":"dumpForTest","usid":w().user.usid}));
sendMsg("core", "mainSend!", {"obj":"body"});
