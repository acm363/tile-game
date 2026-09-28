package boardgame.engine;

import boardgame.board.Position;
import boardgame.player.Player;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RandomDeciderTest {

    private static final int DRAWS = 4000;

    private final Player alice = new Player("Alice");

    @Test
    void aLoneOptionIsAlwaysChosen() {
        // Given.
        RandomDecider decider = new RandomDecider(new Random(1));

        // When.
        Action chosen = decider.choose(alice, List.of(new Pass()));

        // Then.
        assertEquals(new Pass(), chosen);
    }

    @Test
    void onlyLegalActionsAreChosen() {
        // Given.
        List<Action> legal = List.of(new Pass(), new Deploy(new Position(0, 0), 1), new Deploy(new Position(0, 1), 2));
        RandomDecider decider = new RandomDecider(new Random(2));

        for (int draw = 0; draw < DRAWS; draw++) {
            // When.
            Action chosen = decider.choose(alice, legal);

            // Then.
            assertTrue(legal.contains(chosen));
        }
    }

    @Test
    void eachKindOfActionIsEquallyLikelyWhateverTheNumberOfVariants() {
        // Given.
        List<Action> legal = new ArrayList<>();
        legal.add(new Pass());
        for (int col = 0; col < 99; col++) {
            legal.add(new Deploy(new Position(0, col), 1));
        }
        RandomDecider decider = new RandomDecider(new Random(3));

        // When.
        long passes = 0;
        for (int draw = 0; draw < DRAWS; draw++) {
            if (decider.choose(alice, legal) instanceof Pass) {
                passes++;
            }
        }

        // Then.
        assertTrue(passes > DRAWS * 0.45 && passes < DRAWS * 0.55, passes + " passes out of " + DRAWS);
    }
}
