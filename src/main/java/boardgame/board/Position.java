package boardgame.board;

import java.util.List;

public record Position(int row, int col) {

    public List<Position> adjacent() {
        return List.of(
                new Position(row - 1, col),
                new Position(row + 1, col),
                new Position(row, col + 1),
                new Position(row, col - 1));
    }
}
