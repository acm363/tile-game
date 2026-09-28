package boardgame.engine;

import boardgame.board.Board;
import boardgame.player.Player;

import java.util.ArrayList;
import java.util.List;

public final class GameContext {

    private final Board board;
    private final List<Player> players;
    private final List<GameListener> listeners = new ArrayList<>();

    public GameContext(Board board, List<Player> players) {
        this.board = board;
        this.players = List.copyOf(players);
    }

    public Board board() {
        return board;
    }

    public List<Player> players() {
        return players;
    }

    public void addListener(GameListener listener) {
        listeners.add(listener);
    }

    public void emit(GameEvent event) {
        listeners.forEach(listener -> listener.onEvent(event));
    }
}
