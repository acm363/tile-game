package boardgame.engine;

import boardgame.board.Position;

public record Deploy(Position position, int size) implements Action {
}
