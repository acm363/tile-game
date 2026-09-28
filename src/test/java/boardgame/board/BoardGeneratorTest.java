package boardgame.board;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BoardGeneratorTest {

    private static final int SEEDS = 500;

    @ParameterizedTest
    @CsvSource({"10,10", "25,20", "3,3", "1,6", "2,3", "7,13"})
    void generatedBoardsAreAtLeastTwoThirdsOcean(int rows, int cols) {
        for (long seed = 0; seed < SEEDS; seed++) {
            // When.
            Board board = new BoardGenerator(new Random(seed)).generate(rows, cols);

            // Then.
            long ocean = board.positions().stream().filter(p -> board.terrain(p) == Terrain.OCEAN).count();
            assertTrue(3 * ocean >= 2L * rows * cols, "seed " + seed + " has " + ocean + " ocean tiles");
        }
    }

    @ParameterizedTest
    @CsvSource({"10,10", "25,20", "3,3", "1,6", "2,3", "7,13"})
    void everyLandTileHasALandNeighbour(int rows, int cols) {
        for (long seed = 0; seed < SEEDS; seed++) {
            // When.
            Board board = new BoardGenerator(new Random(seed)).generate(rows, cols);

            // Then.
            for (Position position : board.freePositions()) {
                assertTrue(board.neighbours(position).stream().anyMatch(n -> board.terrain(n).isLand()),
                        "seed " + seed + " isolates " + position);
            }
        }
    }

    @Test
    void generatedBoardsHaveTheRequestedSizeAndAtLeastTwoLandTiles() {
        // When.
        Board board = new BoardGenerator(new Random(1)).generate(4, 7);

        // Then.
        assertEquals(4, board.rows());
        assertEquals(7, board.cols());
        assertTrue(board.freePositions().size() >= 2);
    }

    @Test
    void theSameSeedGeneratesTheSameBoard() {
        // When.
        Board first = new BoardGenerator(new Random(42)).generate(10, 10);
        Board second = new BoardGenerator(new Random(42)).generate(10, 10);

        // Then.
        for (Position position : first.positions()) {
            assertEquals(first.terrain(position), second.terrain(position));
        }
    }

    @Test
    void boardsTooSmallForTwoLandTilesAreRejected() {
        // Then.
        assertThrows(IllegalArgumentException.class, () -> new BoardGenerator(new Random()).generate(2, 2));
    }
}
