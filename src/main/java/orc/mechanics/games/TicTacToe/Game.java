package orc.mechanics.games.TicTacToe;

import orc.mechanics.GameSessionsManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

public class Game {

    private Random rnd = null;

    public Game() {
        rnd = new Random();
    }

    // Выполняем ход игрока
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

    // Выполняем ход ИИ
    public Integer[] makeAutoTurn(String gsid, GameSessionsManager gameSessions){

        Integer[] point = new Integer[3];                       // Массив в три значения: x, y, номер игрока
        BattleField field = ((BattleField)(gameSessions.getSession(gsid).get("field")));
        HashMap<String, String> player = gameSessions.getPlayers(gsid).get(gameSessions.getInteger(gsid, "turnOf"));

        System.out.println(player);

        if (!player.get("type").equals("ai")) {                 // Если тип текущего игрока не ИИ, возвращаем Null
            return null;
        }


//        // Готовим список символов противников
//        for (Player player: players) {
//            if (player != activePlayer){
//                otherSymbols.append(player.getSymbol());
//                i++;
//            }
//        }

//        // Поиск завершения своей линии в один ход
//        for (int y = 0, h = field[0].length; y < h; y++) {
//            for (int x = 0, w = field.length; x < w; x++) {
//                // Горизонтальная линия слева направо
//                if (x < (w-padding)) {
//                    i = searchUnfinishedLine(x, y, 1, 0, winLineLength, 1, new String(String.valueOf(selfChar)));
//                    if (i > -1){ return new int[]{x+i,y}; }
//                }
//                // Вертикальная линия сверху вниз
//                if (y < (h-padding)) {
//                    i = searchUnfinishedLine(x, y, 0, 1, winLineLength, 1, new String(String.valueOf(selfChar)));
//                    if (i > -1){ return new int[]{x,y+i}; }
//                }
//                // Диагональная линия слева направо сверху вниз
//                if (x < (w-padding) && y < (h-padding)) {
//                    i = searchUnfinishedLine(x, y, 1, 1, winLineLength, 1, new String(String.valueOf(selfChar)));
//                    if (i > -1){ return new int[]{x+i,y+i}; }
//                }
//                // Диагональная линия справа налево сверху вниз
//                if (x >= padding && y < (h-padding)){
//                    i = searchUnfinishedLine(x, y, -1, 1, winLineLength, 1, new String(String.valueOf(selfChar)));
//                    if (i > -1){ return new int[]{x-i,y+i}; }
//                }
//            }
//        }

        // Случайная клетка
        Integer[][] emptyCells = field.getCellsByValue(null);
        Integer[] coords = emptyCells[rnd.nextInt(emptyCells.length-1)];

        point[0] = coords[0];
        point[1] = coords[1];
        point[2] = Integer.valueOf(player.get("number"));

        if (!field.checkSetCell(point[0], point[1], point[2])){                                          // Пытаемся занять место
            return null;
        }

        if (Integer.valueOf(player.get("number")) < (gameSessions.getPlayers(gsid).size()-1)){                          // Передаём ход
            gameSessions.setValue(gsid, "turnOf", Integer.valueOf(player.get("number"))+1);
        }else{
            gameSessions.setValue(gsid, "turnOf", 0);
        }

        return point;
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

        return ((BattleField)(gameSessions.getSession(gsid).get("field"))).isFilled();
    }
}
