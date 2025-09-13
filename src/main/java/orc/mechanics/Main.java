package orc.mechanics;

import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import orc.mechanics.verticles.Core;
import orc.mechanics.verticles.Server;
import orc.mechanics.games.TicTacToe.TicTacToe;


public class Main {
    private static Vertx vertx;                                 // Непосредственно vertx

    public static Vertx getVertx() {
        return vertx;
    }            // Геттер для вертекса

    public static void main(String[] args) {

        VertxOptions options = new VertxOptions();

        try {
            vertx = Vertx.vertx(options);
            vertx.deployVerticle(Core.class.getName());
        }catch (Exception e){
            System.err.println("Не могу запустить ядро\n");
            e.printStackTrace();
        }

        try {
            vertx.deployVerticle(TicTacToe.class.getName());
        }catch (Exception e){
            System.err.println("Не могу запустить Крестики-нолики\n");
            e.printStackTrace();
        }

        try {
            vertx.deployVerticle(Server.class.getName());
        }catch (Exception e){
            System.err.println("Не могу запустить веб-сервер\n");
            e.printStackTrace();
        }
    }
}