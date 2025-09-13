package orc.mechanics.games.TicTacToe;

public class BattleField {
    private Integer[][] field;                                          // Игровое поле
    private Integer width = null;                                       // Ширина игрового поля
    private Integer height = null;                                      // Высота игрового поля
    private Integer cellCount;                                          // Число ячеек на поле

    public BattleField(Integer width, Integer heigth) {
        this.width = width;
        this.height = heigth;

        cellCount = width*height;

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
        field[x][y] = value;
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


}




