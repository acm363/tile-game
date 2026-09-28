package boardgame.unit;

import boardgame.player.Player;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
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
    void resizingOutsideTheLimitsIsRejectedAndKeepsTheSize() {
        // Given.
        Army army = new Army(alice, 3);

        // Then.
        assertThrows(IllegalArgumentException.class, () -> army.setSize(0));
        assertEquals(3, army.size());
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
    void aUnitCanChangeSides() {
        // Given.
        Player bob = new Player("Bob");
        Army army = new Army(alice, 2);

        // When.
        army.changeOwner(bob);

        // Then.
        assertSame(bob, army.owner());
    }

    @Test
    void aUnitAlwaysHasAnOwner() {
        // Then.
        assertThrows(NullPointerException.class, () -> new Worker(null));
        assertThrows(NullPointerException.class, () -> new Worker(alice).changeOwner(null));
    }
}
