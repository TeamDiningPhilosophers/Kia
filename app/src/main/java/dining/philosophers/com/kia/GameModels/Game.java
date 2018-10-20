package dining.philosophers.com.kia.GameModels;

import java.util.ArrayList;

public class Game {

    public String id;

    public GamePlayer player1;
    public GamePlayer player2;

    //-1 not started, 0 = game running, 1 = game over
    public int state = -1;
    public String winnerId = "";

    public Game() {
        gameWorldObject = new ArrayList<>();
    }

    public Game(String id) {
        this.id = id;
    }

    public ArrayList<GameWorldObject> gameWorldObject;
}
