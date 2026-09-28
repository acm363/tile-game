package boardgame.board;

import boardgame.player.Player;
import boardgame.unit.Worker;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BoardTest {

    private final Player alice = new Player("Alice");
    private final Player bob = new Player("Bob");
    private final Board board = Boards.parse(
            "P~F",
            "MD~");

    @Test
    void freePositionsAreTheUnoccupiedLandTiles() {
        // Given.
        board.place(new Position(0, 2), new Worker(alice));

        // When.
        List<Position> free = board.freePositions();

        // Then.
        assertEquals(List.of(new Position(0, 0), new Position(1, 0), new Position(1, 1)), free);
    }

    @Test
    void placingAUnitMakesTheTileATerritoryOfItsOwner() {
        // When.
        board.place(new Position(1, 1), new Worker(alice));

        // Then.
        assertFalse(board.isFree(new Position(1, 1)));
        assertEquals(List.of(new Position(1, 1)), board.territoriesOf(alice));
        assertTrue(board.territoriesOf(bob).isEmpty());
    }

    @Test
    void territoriesFollowTheOwnerWhenAUnitChangesSides() {
        // Given.
        Worker worker = new Worker(alice);
        board.place(new Position(0, 0), worker);

        // When.
        worker.changeOwner(bob);

        // Then.
        assertTrue(board.territoriesOf(alice).isEmpty());
        assertEquals(List.of(new Position(0, 0)), board.territoriesOf(bob));
    }

    @Test
    void removingAUnitFreesItsTile() {
        // Given.
        board.place(new Position(0, 0), new Worker(alice));

        // When.
        board.remove(new Position(0, 0));

        // Then.
        assertTrue(board.isFree(new Position(0, 0)));
        assertTrue(board.territoriesOf(alice).isEmpty());
    }

    @Test
    void unitsCannotStandOnTheOcean() {
        // Then.
        assertThrows(IllegalStateException.class, () -> board.place(new Position(0, 1), new Worker(alice)));
    }

    @Test
    void aTileHoldsAtMostOneUnit() {
        // Given.
        board.place(new Position(0, 0), new Worker(alice));

        // Then.
        assertThrows(IllegalStateException.class, () -> board.place(new Position(0, 0), new Worker(bob)));
    }

    @Test
    void aUnitCannotBePlacedTwice() {
        // Given.
        Worker worker = new Worker(alice);
        board.place(new Position(0, 0), worker);

        // Then.
        assertThrows(IllegalStateException.class, () -> board.place(new Position(1, 0), worker));
    }

    @Test
    void neighboursAreTheOrthogonalTilesInsideTheBoard() {
        // When.
        List<Position> neighbours = board.neighbours(new Position(0, 0));

        // Then.
        assertEquals(List.of(new Position(1, 0), new Position(0, 1)), neighbours);
    }

    @Test
    void positionsOutsideTheBoardAreRejected() {
        // Then.
        assertThrows(IndexOutOfBoundsException.class, () -> board.terrain(new Position(2, 0)));
    }
}
