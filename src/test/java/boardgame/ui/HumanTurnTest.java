package boardgame.ui;

import boardgame.board.Position;
import boardgame.board.Resource;
import boardgame.engine.Deploy;
import boardgame.engine.Exchange;
import boardgame.engine.Pass;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HumanTurnTest {

    private static final Position MOUNTAIN = new Position(0, 0);
    private static final Position PLAIN = new Position(0, 1);

    private final HumanTurn turn = new HumanTurn(List.of(
            new Pass(),
            new Deploy(MOUNTAIN, 1), new Deploy(MOUNTAIN, 2), new Deploy(MOUNTAIN, 3),
            new Deploy(PLAIN, 1), new Deploy(PLAIN, 2), new Deploy(PLAIN, 3), new Deploy(PLAIN, 4),
            new Exchange(Resource.WOOD, 1)));

    @Test
    void deploySizesAreTheDistinctLegalSizesInOrder() {
        // When.
        List<Integer> sizes = turn.deploySizes();

        // Then.
        assertEquals(List.of(1, 2, 3, 4), sizes);
    }

    @Test
    void targetsAreTheTilesWhereTheSelectedSizeIsLegal() {
        // When.
        Set<Position> small = turn.deployTargets(3);
        Set<Position> large = turn.deployTargets(4);

        // Then.
        assertEquals(Set.of(MOUNTAIN, PLAIN), small);
        assertEquals(Set.of(PLAIN), large);
    }

    @Test
    void clickingATileOnlyYieldsALegalDeployment() {
        // When.
        Optional<Deploy> legal = turn.deployAt(PLAIN, 4);
        Optional<Deploy> illegal = turn.deployAt(MOUNTAIN, 4);

        // Then.
        assertEquals(Optional.of(new Deploy(PLAIN, 4)), legal);
        assertTrue(illegal.isEmpty());
    }

    @Test
    void actionsOtherThanDeployingAndPassingAreOfferedSeparately() {
        // Then.
        assertTrue(turn.canPass());
        assertEquals(List.of(new Exchange(Resource.WOOD, 1)), turn.otherActions());
    }

    @Test
    void aTurnWithoutPassOffersNoPass() {
        // Given.
        HumanTurn withoutPass = new HumanTurn(List.of(new Deploy(PLAIN, 1)));

        // Then.
        assertFalse(withoutPass.canPass());
    }
}
