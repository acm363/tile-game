package boardgame.unit;

import boardgame.player.Player;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ArmyTest {

    private final Player alice = new Player("Alice");

    @ParameterizedTest
    @ValueSource(ints = {0, 6, -1})
    void anArmyHoldsOneToFiveWarriors(int size) {
        // Then.
        assertThrows(IllegalArgumentException.class, () -> new Army(alice, size));
    }

    @Test
    void anArmyStartsAtLevelZeroAndRisesToThreeAtMost() {
        // Given.
        Army army = new Army(alice, 3);
        int initial = army.level();

        // When.
        for (int promotion = 0; promotion < Army.MAX_LEVEL; promotion++) {
            army.promote();
        }

        // Then.
        assertEquals(0, initial);
        assertEquals(3, army.level());
        assertThrows(IllegalStateException.class, army::promote);
        assertEquals(3, army.level());
    }

    @Test
    void anArmyStartsWithoutGold() {
        // When.
        Army army = new Army(alice, 1);

        // Then.
        assertEquals(0, army.gold());
    }

    @Test
    void aUnitCannotLoseGoldThroughANegativeReward() {
        // Given.
        Worker worker = new Worker(alice);

        // Then.
        assertThrows(IllegalArgumentException.class, () -> worker.addGold(-1));
    }

    @Test
    void aUnitAlwaysHasAnOwner() {
        // Then.
        assertThrows(NullPointerException.class, () -> new Worker(null));
    }
}
