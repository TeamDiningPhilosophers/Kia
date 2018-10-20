package dining.philosophers.com.kia.GameModels;

public class GameHit {
    public static final int HIT_HEAD = 0;
    public static final int HIT_BODY = 1;

    public String hitBy;


    public GameHit() {

    }

    public GameHit(String hitBy) {
        this.hitBy = hitBy;
    }
}
