package boardgame.ui;

import boardgame.agriculture.ResourceSold;
import boardgame.agriculture.WorkerDismissed;
import boardgame.board.Position;
import boardgame.board.Resource;
import boardgame.board.Terrain;
import boardgame.engine.EndReason;
import boardgame.engine.GameEnded;
import boardgame.engine.GameEvent;
import boardgame.engine.GameResult;
import boardgame.engine.Harvested;
import boardgame.engine.Passed;
import boardgame.player.Player;
import boardgame.war.ArmyDeployed;
import boardgame.war.ArmyRallied;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EventFormatterTest {

    private final EventFormatter formatter = new EventFormatter();
    private final Player alice = new Player("Alice");
    private final Player bob = new Player("Bob");

    @Test
    void deploymentsNameTheTerrainAndPosition() {
        assertEquals("  Alice déploie une armée de 3 sur une tuile désert (2, 4)",
                formatter.format(new ArmyDeployed(alice, new Position(2, 4), Terrain.DESERT, 3)));
    }

    @Test
    void rallyingNamesBothPlayers() {
        assertEquals("  l'armée de Bob en (0, 1) se rallie à Alice",
                formatter.format(new ArmyRallied(bob, alice, new Position(0, 1))));
    }

    @Test
    void passingMentionsTheIncomeOnlyWhenThereIsOne() {
        assertEquals("  Alice ne fait rien", formatter.format(new Passed(alice, 0)));
        assertEquals("  Alice ne fait rien et reçoit 3 or", formatter.format(new Passed(alice, 3)));
    }

    @Test
    void harvestsListResourcesInAStableOrder() {
        Map<Resource, Integer> harvested = Map.of(Resource.WOOD, 1, Resource.ROCK, 2);

        assertEquals("  Alice récolte 2 roche, 1 bois", formatter.format(new Harvested(alice, harvested)));
    }

    @Test
    void salesAndDismissalsUseFrenchResourceNames() {
        assertEquals("  Alice vend 2 blé pour 4 or", formatter.format(new ResourceSold(alice, Resource.WHEAT, 2, 4)));
        assertEquals("  Alice ne peut pas payer 5 or : l'ouvrier en (1, 1) quitte le jeu",
                formatter.format(new WorkerDismissed(alice, new Position(1, 1), 5)));
    }

    @Test
    void aSingleWinnerIsAnnounced() {
        String text = formatter.format(ended(Map.of(alice, 7), List.of(alice), EndReason.ROUNDS_COMPLETED));

        assertTrue(text.contains("Alice : 7 point(s)"));
        assertTrue(text.endsWith("Vainqueur : Alice"));
    }

    @Test
    void aTieNamesEveryWinnerAndTheEndReason() {
        Map<Player, Integer> scores = new LinkedHashMap<>();
        scores.put(alice, 4);
        scores.put(bob, 4);

        String text = formatter.format(ended(scores, List.of(alice, bob), EndReason.NO_TERRITORY_LEFT));

        assertTrue(text.contains("plus de territoire à conquérir"));
        assertTrue(text.endsWith("Égalité entre Alice, Bob"));
    }

    @Test
    void unknownEventsFallBackToTheirDescription() {
        GameEvent custom = new GameEvent() {
            @Override
            public String toString() {
                return "custom";
            }
        };

        assertEquals("custom", formatter.format(custom));
    }

    private static GameEnded ended(Map<Player, Integer> scores, List<Player> winners, EndReason reason) {
        return new GameEnded(new GameResult(scores, winners, reason, 3));
    }
}
