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
import boardgame.war.ArmyDestroyed;
import boardgame.war.ArmyPromoted;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EventFormatterTest {

    private final EventFormatter formatter = new EventFormatter(new Labels(Language.FR));
    private final EventFormatter english = new EventFormatter(new Labels(Language.EN));
    private final Player alice = new Player("Alice");
    private final Player bob = new Player("Bob");

    @Test
    void deploymentsNameTheTerrainAndPosition() {
        // When.
        String text = formatter.format(new ArmyDeployed(alice, new Position(2, 4), Terrain.DESERT, 3));

        // Then.
        assertEquals("  Alice déploie une armée de 3 sur une tuile désert (2, 4)", text);
    }

    @Test
    void aPromotionNamesTheArmyAndItsNewLevel() {
        // When.
        String text = formatter.format(new ArmyPromoted(alice, new Position(0, 1), 2));

        // Then.
        assertEquals("  l'armée de Alice en (0, 1) passe au niveau 2", text);
    }

    @Test
    void passingMentionsTheIncomeOnlyWhenThereIsOne() {
        // When.
        String withoutIncome = formatter.format(new Passed(alice, 0));
        String withIncome = formatter.format(new Passed(alice, 3));

        // Then.
        assertEquals("  Alice ne fait rien", withoutIncome);
        assertEquals("  Alice ne fait rien et reçoit 3 or", withIncome);
    }

    @Test
    void harvestsListResourcesInAStableOrder() {
        // Given.
        Map<Resource, Integer> harvested = Map.of(Resource.WOOD, 1, Resource.ROCK, 2);

        // When.
        String text = formatter.format(new Harvested(alice, harvested));

        // Then.
        assertEquals("  Alice récolte 2 roche, 1 bois", text);
    }

    @Test
    void salesAndDismissalsUseFrenchResourceNames() {
        // When.
        String sale = formatter.format(new ResourceSold(alice, Resource.WHEAT, 2, 4));
        String dismissal = formatter.format(new WorkerDismissed(alice, new Position(1, 1), 5));

        // Then.
        assertEquals("  Alice vend 2 blé pour 4 or", sale);
        assertEquals("  Alice ne peut pas payer 5 or : l'ouvrier en (1, 1) quitte le jeu", dismissal);
    }

    @Test
    void aSingleWinnerIsAnnounced() {
        // When.
        String text = formatter.format(ended(Map.of(alice, 7), List.of(alice), EndReason.ROUNDS_COMPLETED));

        // Then.
        assertTrue(text.contains("Alice : 7 point(s)"));
        assertTrue(text.endsWith("Vainqueur : Alice"));
    }

    @Test
    void aTieNamesEveryWinnerAndTheEndReason() {
        // Given.
        Map<Player, Integer> scores = new LinkedHashMap<>();
        scores.put(alice, 4);
        scores.put(bob, 4);

        // When.
        String text = formatter.format(ended(scores, List.of(alice, bob), EndReason.NO_TERRITORY_LEFT));

        // Then.
        assertTrue(text.contains("plus de territoire à conquérir"));
        assertTrue(text.endsWith("Égalité entre Alice, Bob"));
    }

    @Test
    void unknownEventsFallBackToTheirDescription() {
        // Given.
        GameEvent custom = new GameEvent() {
            @Override
            public String toString() {
                return "custom";
            }
        };

        // When.
        String text = formatter.format(custom);

        // Then.
        assertEquals("custom", text);
    }

    @Test
    void eventsAreTranslatedIntoEnglish() {
        // When.
        String deployment = english.format(new ArmyDeployed(alice, new Position(2, 4), Terrain.DESERT, 3));
        String harvest = english.format(new Harvested(alice, Map.of(Resource.WOOD, 1, Resource.ROCK, 2)));

        // Then.
        assertEquals("  Alice deploys an army of 3 on a desert tile (2, 4)", deployment);
        assertEquals("  Alice harvests 2 rock, 1 wood", harvest);
    }

    @Test
    void aDestructionNamesBothArmiesInTheOrderEachLanguageNeeds() {
        // Given.
        ArmyDestroyed destroyed = new ArmyDestroyed(alice, new Position(0, 0), bob, new Position(0, 3), 4);

        // When.
        String french = formatter.format(destroyed);
        String text = english.format(destroyed);

        // Then.
        assertEquals("  l'armée de Alice en (0, 0) détruit l'armée de 4 de Bob en (0, 3)", french);
        assertEquals("  Alice's army at (0, 0) destroys Bob's army of 4 at (0, 3)", text);
    }

    @Test
    void theEnglishResultNamesTheReasonAndTheWinner() {
        // When.
        String text = english.format(ended(Map.of(alice, 7), List.of(alice), EndReason.NO_TERRITORY_LEFT));

        // Then.
        assertEquals("══ Game over at round 3 (no territory left to conquer) ══\n  Alice: 7 point(s)\nWinner: Alice", text);
    }

    private static GameEnded ended(Map<Player, Integer> scores, List<Player> winners, EndReason reason) {
        return new GameEnded(new GameResult(scores, winners, reason, 3));
    }
}
