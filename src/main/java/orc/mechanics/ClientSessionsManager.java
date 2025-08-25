package orc.mechanics;

import java.util.HashMap;

// Класс менеджера клиентских сессий
public class ClientSessionsManager extends SessionsManager {

    private HashMap<String, Object> hostPorts       = null;                         // Индекс по хост:порт


    public ClientSessionsManager() {
        super();
        hostPorts           = new HashMap<>();
    }

    // Получить сессию клиента по хост:порт
    public HashMap<String, Object> getSessionByHostport(String hostPort){

        HashMap<String, Object> ses = (HashMap<String, Object>) hostPorts.get(hostPort);

        if (ses != null) {
            return ses;
        }else {
            log.error("getSessionByHostport: can't get session for host:port: "+hostPort);
        }

        return null;
    }

    // Получить UID сессии клиента по хост:порт
    public String getSessionUidByHostport(String hostPort){

        HashMap<String, Object> ses = (HashMap<String, Object>) hostPorts.get(hostPort);

        if (ses != null) {
            return (String) ses.get("uid");
        }else {
            log.error("getSessionUidByHostport: can't get session UID for host:port: "+hostPort);
        }

        return null;
    }

    // Получить хост:порт клиента по UID
    public String getHostPort(String uid){

        HashMap<String, Object> ses = getSession(uid);

        if (ses != null) {
            return ses.get("host")+":"+ses.get("port");
        }else {
            log.error("getHostPort: can't get host:port for session "+uid);
        }

        return null;
    }

    // Получить хост клиента по UID
    public String getHost(String uid){

        HashMap<String, Object> ses = getSession(uid);

        if (ses != null) {
            return (String) ses.get("host");
        }else {
            log.error("getHost: can't get host for session "+uid);
        }

        return null;
    }

    // Получить порт клиента по UID
    public Integer getPort(String uid){

        HashMap<String, Object> ses = getSession(uid);

        if (ses != null) {
            return (Integer) ses.get("port");
        }else {
            log.error("getPort: can't get port for session "+uid);
        }

        return null;
    }

    // Сопоставить хост:порт клиента сессии
    public boolean setHostPort(String host, Integer port, String uid){

        HashMap<String, Object> ses = getSession(uid);

        if (ses != null) {
            ses.put("host", host);
            ses.put("port", port);
            hostPorts.put(host+":"+port, ses);
            return true;
        }else {
            log.error("setHostPort: can't set host:port "+host+":"+port+" to session "+uid);
        }

        return false;
    }
}



