package boardgame.ui;

import boardgame.engine.Action;
import boardgame.engine.Pass;
import boardgame.player.Player;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HumanDeciderTest {

    private final Player alice = new Player("Alice");
    private final Player bob = new Player("Bob");
    private final HumanDecider decider = new HumanDecider(Set.of(alice));

    @Test
    void onlyTheDesignatedPlayersAreControlled() {
        // Then.
        assertTrue(decider.controls(alice));
        assertFalse(decider.controls(bob));
    }

    @Test
    void theSubmittedActionIsChosenOnce() {
        // Given.
        decider.submit(new Pass());

        // When.
        Action chosen = decider.choose(alice, List.of(new Pass()));

        // Then.
        assertEquals(new Pass(), chosen);
        assertThrows(IllegalStateException.class, () -> decider.choose(alice, List.of(new Pass())));
    }

    @Test
    void choosingWithoutASubmittedActionIsRejected() {
        // Then.
        assertThrows(IllegalStateException.class, () -> decider.choose(alice, List.of(new Pass())));
    }
}
