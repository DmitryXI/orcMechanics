package orc.mechanics;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import java.util.HashMap;
import java.util.Random;

public class SessionsManager {

    protected Logger                  log             = null;                       // Логер
    protected Integer                 uidLen          = null;                       // Длина идентификатора сессии в символах
    protected String                  uidChars        = null;                       // Набор символов для генерации UID'ов
    protected String                  addressPrefix   = null;                       // Префикс для UID'ов сессий
    protected Integer                 ttl             = null;                       // Время жизни неактивной сессии
    protected Random                  rnd             = null;                       // Рандомайзер
    private HashMap<String, Object> sessions          = null;                       // Карта объектов с полными данными сессий
    private HashMap<String, Long>   lastActivity      = null;                       // Карта UID сессий с временем последней активности
    private HashMap<String, Object> addresses         = null;                       // Индекс по адресу



    public SessionsManager(String addressPrefix) {

        log                 = LoggerFactory.getLogger(SessionsManager.class);
        uidLen              = 12;
        uidChars            = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        this.addressPrefix  = addressPrefix;
        ttl                 = 30;                                   // Допустимое время простоя сессии в секндах
        rnd                 = new Random();
        sessions = new HashMap<>();
        lastActivity        = new HashMap<>();
        addresses           = new HashMap<>();
    }

    // Установить ttl
    public void setTtl(Integer ttl) {
        this.ttl = ttl;
    }

    // Получить ttl
    public Integer getTtl() {
        return this.ttl;
    }

    // Создать сессию
    public String create(){

        String uid = generateUID(uidChars, uidLen);
        Integer genCount = 0;

        while (sessions.containsKey(uid)){
            uid = generateUID(uidChars, uidLen);
            genCount++;
            if (genCount > 10){ return null; }
        }

        String  address = addressPrefix+uid;
        Long    ts = getCurrentTimeStamp();

        HashMap<String, Object> session = new HashMap<>();
        session.put("uid", uid);
        session.put("createTime", ts);
        session.put("status", "created");

        sessions.put(uid, session);                                 // Добавляем сессию в карту
        setAddress(address, uid);                                   // Добавляем и индексируем адрес шины для сессии
        setActivity(ts, uid);                                       // Устанавливаем время последнего обновления

        return uid;
    }

    // Удалить сессию
    public boolean remove(String uid){

        if (sessions.containsKey(uid)){
            String address = (String) getSession(uid).get("address");
            addresses.remove(address);
            lastActivity.remove(uid);
            sessions.remove(uid);
        }

        return false;
    }

    // Получить объект сессии по UID
    public HashMap<String, Object> getSession(String uid){

        if (sessions.containsKey(uid)) {
            return (HashMap) sessions.get(uid);
        }else {
            log.error("get: UID "+uid+" not found in sessions map");
        }

        return null;
    }

    // Получить объект сессии по адресу
    public HashMap<String, Object> getSessionByAddress(String address){

        if (addresses.containsKey(address)) {
            return (HashMap) addresses.get(address);
        }else {
            log.error("getSessionByAddress: address "+address+" not found in adresses index");
        }

        return null;
    }

    // Получить адрес шины сессии по UID
    public String getAddress(String uid){

        HashMap<String, Object> ses = getSession(uid);

        if (ses != null) {
            return (String) ses.get("address");
        }else {
            log.error("getAddress: can't get address for session "+uid);
        }

        return null;
    }

    // Получить время последней активности сессии по UID
    public Long getActivity(String uid){

        HashMap<String, Object> ses = getSession(uid);

        if (ses != null) {
            return lastActivity.get(uid);
        }else {
            log.error("getActivity: can't get activity for session "+uid);
        }

        return null;
    }

    // Сопоставить адрес шины сессии
    public boolean setAddress(String address, String uid){

        HashMap<String, Object> ses = getSession(uid);

        if (ses != null) {
            ses.put("address", address);
            addresses.put(address, ses);
            return true;
        }else {
            log.error("setAddress: can't set address "+address+" to session "+uid);
        }

        return false;
    }

    // Обновить время последней активности
    public boolean setActivity(String uid){
        return setActivity(getCurrentTimeStamp(), uid);
    }
    public boolean setActivity(Long ts, String uid){

        HashMap<String, Object> ses = getSession(uid);

        if (ses != null) {
            lastActivity.put(uid, ts);
            return true;
        }else {
            log.error("setActivity: can't set activity time "+ts+" to session "+uid);
        }

        return false;
    }

    // Установить статус сессии
    public boolean setStatus(String status, String uid){

        HashMap<String, Object> ses = getSession(uid);

        if (ses != null) {
            ses.put("status", status);
            return true;
        }else {
            log.error("setStatus: can't set status "+status+" to session "+uid);
        }

        return false;
    }

    // Получить статус сессии по UID
    public String getStatus(String uid){

        HashMap<String, Object> ses = getSession(uid);

        if (ses != null) {
            return (String) ses.get("status");
        }else {
            log.error("getStatus: can't get status for session "+uid);
        }

        return null;
    }

    // Получить текущий timestamp (Unix-формат)
    public long getCurrentTimeStamp(){
        return System.currentTimeMillis() / 1000;
    }

    // Генерация UID
    public String generateUID(String characters, int length) {
        char[] text = new char[length];
        for (int i = 0; i < length; i++) {
            text[i] = characters.charAt(rnd.nextInt(characters.length()));
        }

        return new String(text);
    }

    // Удаление просроченных сессий
    public HashMap<String, Object> removeTheDead(){

        HashMap<String, Object> doomeds = new HashMap<>();
        Long                    ts  = getCurrentTimeStamp();

        sessions.forEach((k, v) -> {
            if ((ts - getActivity(k)) > ttl) {
                doomeds.put(k, v);
            }
        });

        if (doomeds.size() > 0) {
            doomeds.forEach((k, v) -> {
                this.remove(k);
                log.debug("Session "+k+" removed by timeout");
            });
        }

        return doomeds;
    }

    // Получить количество сессий в менеджере
    public Integer size(){
        return sessions.size();
    }

    // Получить ссылка на хранилище сессий
    public HashMap<String, Object> getSessions() {

        return this.sessions;
    }
}