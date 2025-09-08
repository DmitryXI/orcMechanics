package orc.mechanics;

import java.util.ArrayList;
import java.util.HashMap;

// Класс менеджера клиентских сессий
public class GameSessionsManager extends SessionsManager {

    public GameSessionsManager() {
        super("gm");
    }

    // Получить список игроков
    public ArrayList<HashMap<String, String>> getPlayers(String uid){

        HashMap<String, Object> ses = getSession(uid);

        if (ses != null) {
            return (ArrayList<HashMap<String, String>>) (ses.get("players"));
        }

        return null;
    }
}


