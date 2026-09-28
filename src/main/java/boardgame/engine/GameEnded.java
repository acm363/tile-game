package boardgame.engine;

public record GameEnded(GameResult result) implements GameEvent {
}
