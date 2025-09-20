package orc.mechanics.games.TicTacToe;

public class BattleField {
    private Integer[][] field;                                          // Игровое поле
    private Integer width = null;                                       // Ширина игрового поля
    private Integer height = null;                                      // Высота игрового поля
    private Integer cellCount;                                          // Количество ячеек на поле
    private Integer filled;                                             // Количество заполненных ячеек на поле

    public BattleField(Integer width, Integer heigth) {
        this.width  = width;
        this.height = heigth;

        cellCount = width*height;
        filled    = 0;

        field = new Integer[width][heigth];

        // Заполняем ячейки игрового поля (null - ячейка пустая)
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < heigth; y++) {
                field[x][y] = null;
            }
        }
    }

    // Получить значение ячейки
    public Integer getCell(Integer x, Integer y){
        return field[x][y];
    }

    // Установить значение ячейки
    public void setCell(Integer x, Integer y, Integer value){
        if ((value != null) && (getCell(x,y) == null)) {
            filled++;
        } else if ((value == null) && (getCell(x,y) != null)) {
            filled--;
        }

        field[x][y] = value;
    }

    // Попытаться установить значение ячейки с проверкой допустимости
    public boolean checkSetCell(Integer x, Integer y, Integer value){

        if (getCell(x,y) == null) {
            setCell(x, y, value);
            return true;
        }

        return false;
    }

    // Получить игровое поле
    public Integer[][] getField() {
        return field;
    }

    // Записать игровое поле
    public void setField(Integer[][] field) {
        this.field = field;
    }

    // Получить ширину игрового поля
    public Integer getWidth() {
        return width;
    }

    // Получить высоту игрового поля
    public Integer getHeight() {
        return height;
    }

    // Проверить факт полного заполнения поля
    public boolean isFilled(){
        if (filled == cellCount) {
            return true;
        }

        return false;
    }

}




