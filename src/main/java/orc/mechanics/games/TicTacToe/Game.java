package orc.mechanics.games.TicTacToe;

import orc.mechanics.GameSessionsManager;

import java.util.ArrayList;
import java.util.HashMap;

public class Game {

    public Game() {
        //
    }

    public boolean makeTurn(String csid, String gsid, String clientAddress, HashMap<String, String> player, GameSessionsManager gameSessions, Integer x, Integer y){

        BattleField field = ((BattleField)(gameSessions.getSession(gsid).get("field")));

        if (field.getCell(x,y) != null) {
            return false;
        }

        field.setCell(x, y, Integer.valueOf(player.get("number")));                                                     // Занимаем место

        if (Integer.valueOf(player.get("number")) < (gameSessions.getPlayers(gsid).size()-1)){                          // Передаём ход
            gameSessions.setValue(gsid, "turnOf", Integer.valueOf(player.get("number"))+1);
        }else{
            gameSessions.setValue(gsid, "turnOf", 0);
        }

        return true;
    }
}
