package boardgame.engine;

@FunctionalInterface
public interface GameListener {

    void onEvent(GameEvent event);
}
