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

        if (!field.checkSetCell(x, y, Integer.valueOf(player.get("number")))){                                          // Пытаемся занять место
            return false;
        }

        if (Integer.valueOf(player.get("number")) < (gameSessions.getPlayers(gsid).size()-1)){                          // Передаём ход
            gameSessions.setValue(gsid, "turnOf", Integer.valueOf(player.get("number"))+1);
        }else{
            gameSessions.setValue(gsid, "turnOf", 0);
        }

        return true;
    }

    // Проверяем поле на наличие победы
    public setGameFinish checkGameWin(Integer playerNum, String gsid, GameSessionsManager gameSessions){

        setGameFinish res   = new setGameFinish();
        BattleField field   = ((BattleField)(gameSessions.getSession(gsid).get("field")));
        Integer winLineLen  = gameSessions.getInteger(gsid,"winLineLen");
        Integer padding     = winLineLen-1;

        res.finished     = false;
        res.winnerNumber = playerNum;

        for (int x = 0, w = field.getWidth(); x < w; x++) {
            for (int y = 0, h = field.getHeight(); y < h; y++) {
                if (field.getCell(x,y) == playerNum) {
                    // Горизонтальная линия слева направо
                    if (x < (w-padding)) {
                        if ((res.winLine = checkFilledLine(playerNum, x, y, 1, 0, winLineLen, field)) != null) {
                            res.finished = true;
                            return res;
                        }
                    }
                    // Вертикальная линия сверху вниз
                    if (y < (h-padding)) {
                        if ((res.winLine = checkFilledLine(playerNum, x, y, 0, 1, winLineLen, field)) != null) {
                            res.finished = true;
                            return res;
                        }
                    }
                    // Диагональная линия слева направо сверху вниз
                    if (x < (w-padding) && y < (h-padding)) {
                        if ((res.winLine = checkFilledLine(playerNum, x, y, 1, 1, winLineLen, field)) != null) {
                            res.finished = true;
                            return res;
                        }
                    }
                    // Диагональная линия справа налево сверху вниз
                    if (x >= padding && y < (h-padding)){
                        if ((res.winLine = checkFilledLine(playerNum, x, y, -1, 1, winLineLen, field)) != null) {
                            res.finished = true;
                            return res;
                        }
                    }
                }
            }
        }

        return res;
    }

    // Проверка на заполнение линии одним из значений
    private Integer[][] checkFilledLine(Integer number, Integer x, Integer y, Integer moveX, Integer moveY, Integer lineLength, BattleField field) {
        return checkFilledLine(new Integer[]{number}, x, y, moveX, moveY, lineLength, field);
    }
    private Integer[][] checkFilledLine(Integer[] numbers, Integer x, Integer y, Integer moveX, Integer moveY, Integer lineLength, BattleField field){

        Integer[][] res = new Integer[lineLength][2];

        for (Integer num : numbers){
            for (int i = 0; i < lineLength; i++) {
                if (field.getCell(x,y) != num) {
                    return null;
                }

                res[i] = new Integer[]{x,y};

                x += moveX;
                y += moveY;
            }
        }

        return res;
    }

    // Проверяем поле на полное заполнение без победы
    public boolean checkGameDraw(String gsid, GameSessionsManager gameSessions){

//        BattleField field = ((BattleField)(gameSessions.getSession(gsid).get("field")));

        return ((BattleField)(gameSessions.getSession(gsid).get("field"))).isFilled();
    }
}
