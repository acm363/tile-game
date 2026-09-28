package boardgame.player;

import boardgame.board.Resource;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerTest {

    private final Player player = new Player("Alice");

    @Test
    void aNewPlayerHasNoGoldAndNoResources() {
        assertEquals(0, player.gold());
        assertTrue(player.resources().isEmpty());
        assertEquals(0, player.resource(Resource.ROCK));
    }

    @Test
    void goldCanBeEarnedAndSpent() {
        player.addGold(10);

        player.spendGold(4);

        assertEquals(6, player.gold());
    }

    @Test
    void goldCannotGoNegative() {
        player.addGold(3);

        assertThrows(IllegalStateException.class, () -> player.spendGold(4));
        assertEquals(3, player.gold());
    }

    @Test
    void resourcesAccumulatePerKind() {
        player.addResource(Resource.WOOD, 2);
        player.addResource(Resource.WOOD, 3);
        player.addResource(Resource.SAND, 1);

        assertEquals(Map.of(Resource.WOOD, 5, Resource.SAND, 1), player.resources());
    }

    @Test
    void aResourceRemovedEntirelyDisappearsFromTheStock() {
        player.addResource(Resource.WHEAT, 2);

        player.removeResource(Resource.WHEAT, 2);

        assertTrue(player.resources().isEmpty());
    }

    @Test
    void moreThanTheHeldQuantityCannotBeRemoved() {
        player.addResource(Resource.ROCK, 1);

        assertThrows(IllegalStateException.class, () -> player.removeResource(Resource.ROCK, 2));
        assertEquals(1, player.resource(Resource.ROCK));
    }

    @Test
    void negativeAmountsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> player.addGold(-1));
        assertThrows(IllegalArgumentException.class, () -> player.spendGold(-1));
        assertThrows(IllegalArgumentException.class, () -> player.addResource(Resource.WOOD, -1));
        assertThrows(IllegalArgumentException.class, () -> player.removeResource(Resource.WOOD, -1));
    }

    @Test
    void theExposedStockCannotBeModified() {
        player.addResource(Resource.WOOD, 1);

        assertThrows(UnsupportedOperationException.class, () -> player.resources().put(Resource.WOOD, 99));
    }
}
