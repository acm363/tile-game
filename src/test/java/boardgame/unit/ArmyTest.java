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
        assertThrows(IllegalArgumentException.class, () -> new Army(alice, size));
    }

    @Test
    void resizingOutsideTheLimitsIsRejectedAndKeepsTheSize() {
        Army army = new Army(alice, 3);

        assertThrows(IllegalArgumentException.class, () -> army.setSize(0));
        assertEquals(3, army.size());
    }

    @Test
    void anArmyStartsWithoutGold() {
        assertEquals(0, new Army(alice, 1).gold());
    }

    @Test
    void aUnitCannotLoseGoldThroughANegativeReward() {
        assertThrows(IllegalArgumentException.class, () -> new Worker(alice).addGold(-1));
    }

    @Test
    void aUnitCanChangeSides() {
        Player bob = new Player("Bob");
        Army army = new Army(alice, 2);

        army.changeOwner(bob);

        assertSame(bob, army.owner());
    }

    @Test
    void aUnitAlwaysHasAnOwner() {
        assertThrows(NullPointerException.class, () -> new Worker(null));
        assertThrows(NullPointerException.class, () -> new Worker(alice).changeOwner(null));
    }
}
