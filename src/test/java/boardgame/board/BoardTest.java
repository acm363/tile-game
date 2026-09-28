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
        board.place(new Position(0, 2), new Worker(alice));

        assertEquals(List.of(new Position(0, 0), new Position(1, 0), new Position(1, 1)), board.freePositions());
    }

    @Test
    void placingAUnitMakesTheTileATerritoryOfItsOwner() {
        board.place(new Position(1, 1), new Worker(alice));

        assertFalse(board.isFree(new Position(1, 1)));
        assertEquals(List.of(new Position(1, 1)), board.territoriesOf(alice));
        assertTrue(board.territoriesOf(bob).isEmpty());
    }

    @Test
    void territoriesFollowTheOwnerWhenAUnitChangesSides() {
        Worker worker = new Worker(alice);
        board.place(new Position(0, 0), worker);

        worker.changeOwner(bob);

        assertTrue(board.territoriesOf(alice).isEmpty());
        assertEquals(List.of(new Position(0, 0)), board.territoriesOf(bob));
    }

    @Test
    void removingAUnitFreesItsTile() {
        board.place(new Position(0, 0), new Worker(alice));

        board.remove(new Position(0, 0));

        assertTrue(board.isFree(new Position(0, 0)));
        assertTrue(board.territoriesOf(alice).isEmpty());
    }

    @Test
    void unitsCannotStandOnTheOcean() {
        assertThrows(IllegalStateException.class, () -> board.place(new Position(0, 1), new Worker(alice)));
    }

    @Test
    void aTileHoldsAtMostOneUnit() {
        board.place(new Position(0, 0), new Worker(alice));

        assertThrows(IllegalStateException.class, () -> board.place(new Position(0, 0), new Worker(bob)));
    }

    @Test
    void aUnitCannotBePlacedTwice() {
        Worker worker = new Worker(alice);
        board.place(new Position(0, 0), worker);

        assertThrows(IllegalStateException.class, () -> board.place(new Position(1, 0), worker));
    }

    @Test
    void neighboursAreTheOrthogonalTilesInsideTheBoard() {
        assertEquals(List.of(new Position(1, 0), new Position(0, 1)), board.neighbours(new Position(0, 0)));
    }

    @Test
    void positionsOutsideTheBoardAreRejected() {
        assertThrows(IndexOutOfBoundsException.class, () -> board.terrain(new Position(2, 0)));
    }
}
