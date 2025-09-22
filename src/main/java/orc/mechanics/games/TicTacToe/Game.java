package orc.mechanics.games.TicTacToe;

import orc.mechanics.GameSessionsManager;

import java.util.*;

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

        Integer selfNum = gameSessions.getInteger(gsid, "turnOf");
        HashMap<String, String> player = gameSessions.getPlayers(gsid).get(selfNum);

        if (!player.get("type").equals("ai")) {                 // Если тип текущего игрока не ИИ, возвращаем Null
            return null;
        }

        Integer[] point       = new Integer[3];                       // Массив в три значения: x, y, номер игрока
        Integer playersCount  = gameSessions.getPlayers(gsid).size();
        Integer[] playersNums = new Integer[playersCount-1];
        BattleField field     = ((BattleField)(gameSessions.getSession(gsid).get("field")));
        Integer winLineLen    = gameSessions.getInteger(gsid,"winLineLen");


        // Готовим список символов противников
        int j=0;
        for (Integer i=0; i < playersCount; i++) {
            if (!i.equals(selfNum)){
                playersNums[j] = i;
                j++;
            }
        }

        if ((point = searchUnfinishedLine(winLineLen, 1, 1, new Integer[]{selfNum}, field)) != null) {         // Поиск завершения своей линии в один ход
            System.out.println("Self unfinished line finded");
        }else if ((point = searchUnfinishedLine(winLineLen, 1, winLineLen-1, playersNums, field)) != null) {              // Поиск завершения линии противника в один ход
            point[2] = selfNum;                                                         // Т.к. здесь будет номер оппонента, то меняем на свой
            System.out.println("Opponent unfinished line finded");
        }else {                                                                                                         // Случайная клетка
            System.out.println("Get random cell");
            point = new Integer[3];
            Integer[][] emptyCells = field.getCellsByValue(null);

            if (emptyCells.length > 1) {
                Integer[] coords = emptyCells[rnd.nextInt(emptyCells.length - 1)];
                point[0] = coords[0];
                point[1] = coords[1];
                point[2] = Integer.valueOf(player.get("number"));
            } else {
                Integer[] coords = emptyCells[0];
                point[0] = coords[0];
                point[1] = coords[1];
                point[2] = Integer.valueOf(player.get("number"));
            }
        }

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

    // Поиск незавершённой линии на всём поле
    private Integer[] searchUnfinishedLine(int lineLength, int allowedSpaceMin, int allowedSpaceMax, Integer[] searchNumbers, BattleField field){

        Integer   padding = lineLength-1;
        Integer[] point   = null;

        for (int allowedSpace = allowedSpaceMin; allowedSpace <= allowedSpaceMax; allowedSpace++) {       // Если минимально- и мксимально допустимое кол-во пустых ячеек отличается
            for (int x = 0, w = field.getWidth(); x < w; x++) {
                for (int y = 0, h = field.getHeight(); y < h; y++) {

                    // Горизонтальная линия слева направо
                    if (x < (w - padding)) {
                        if ((point = searchUnfinishedLineFromPoint(x, y, 1, 0, lineLength, allowedSpace, searchNumbers, field)) != null) {
                            return point;
                        }
                    }

                    // Вертикальная линия сверху вниз
                    if (y < (h - padding)) {
                        if ((point = searchUnfinishedLineFromPoint(x, y, 0, 1, lineLength, allowedSpace, searchNumbers, field)) != null) {
                            return point;
                        }
                    }

                    // Диагональная линия слева направо сверху вниз
                    if (x < (w - padding) && y < (h - padding)) {
                        if ((point = searchUnfinishedLineFromPoint(x, y, 1, 1, lineLength, allowedSpace, searchNumbers, field)) != null) {
                            return point;
                        }
                    }

                    // Диагональная линия справа налево сверху вниз
                    if (x >= padding && y < (h - padding)) {
                        if ((point = searchUnfinishedLineFromPoint(x, y, -1, 1, lineLength, allowedSpace, searchNumbers, field)) != null) {
                            return point;
                        }
                    }
                }
            }
        }

        return null;
    }

    // Поиск незавершённой линии от заданной точки
    private Integer[] searchUnfinishedLineFromPoint(int sx, int sy, int moveX, int moveY, int lineLength, int allowedSpace, Integer[] searchNumbers, BattleField field){


        Integer[] emptyCells = new Integer[lineLength];     // Список номеров по порядку пустых ячеек

        int emptyCount   = 0;           // Количество пустых ячеек на линии
        int importantNum = 0;           // Важный номер ячейки на линии
        int lastIsEmpty  = 0;           // Пуста ли предыдущая клетка
        List<Integer> searchNumbersList = Arrays.asList(searchNumbers);     // Делаем лист из массива, чтобы, *лять, проверять наличие значения...
        int x = sx;
        int y = sy;

        int searchedNumber = -1;        // Уже найденный номер на линии

        for (int i = 0; i < lineLength; i++) {
            if (field.getCell(x, y) == null) {
                emptyCells[emptyCount] = i;
                emptyCount++;
                if (lastIsEmpty == 0) { importantNum = i; }
                if (lastIsEmpty == 1) { importantNum = i; lastIsEmpty = 2; }
            } else if (searchedNumber == -1 && searchNumbersList.contains(field.getCell(x, y))) {
                searchedNumber = field.getCell(x, y);
                lastIsEmpty = 1;
            } else if (searchedNumber != field.getCell(x, y)) {
                return null;
            }

            x += moveX;
            y += moveY;
        }

        if (emptyCount > allowedSpace || emptyCount < 1){           // Если пустых клеток на линии больше допустимого, или каким-то образом нет ни одной пустой клетки
            return null;
        }
//        System.out.println("sx="+sx+", sy="+sy+", x="+x+", y="+y+", moveX="+moveX+", moveY="+moveY+", iNum="+importantNum+", searchedNum="+searchedNumber);
        return new Integer[]{sx+(moveX*importantNum), sy+(moveY*importantNum), searchedNumber};         // Вообще здесь можно сделать случайный выбор пустой ячейки из набранного массива, но не факт, что так будет лучше
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
