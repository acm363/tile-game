package boardgame.engine;

import boardgame.board.Position;

public record Attack(Position from, Position target) implements Action {
}
