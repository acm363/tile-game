package boardgame.agriculture;

import boardgame.board.Board;
import boardgame.board.Boards;
import boardgame.board.Position;
import boardgame.board.Resource;
import boardgame.engine.Action;
import boardgame.engine.Deploy;
import boardgame.engine.Exchange;
import boardgame.engine.GameContext;
import boardgame.engine.GameEvent;
import boardgame.engine.Pass;
import boardgame.engine.Passed;
import boardgame.player.Player;
import boardgame.unit.Worker;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgricultureRulesTest {

    private final Player alice = new Player("Alice");
    private final Player bob = new Player("Bob");
    private final AgricultureRules rules = new AgricultureRules();
    private final List<GameEvent> events = new ArrayList<>();

    private GameContext on(Board board) {
        rules.setUp(alice);
        rules.setUp(bob);
        GameContext context = new GameContext(board, List.of(alice, bob));
        context.addListener(events::add);
        return context;
    }

    private Worker put(GameContext context, Player owner, Position position) {
        Worker worker = new Worker(owner);
        context.board().place(position, worker);
        return worker;
    }

    @Test
    void playersStartWithFifteenGold() {
        // When.
        on(Boards.parse("PP~~~~"));

        // Then.
        assertEquals(15, alice.gold());
    }

    @Test
    void deployingPlacesAWorkerOnTheChosenTile() {
        // Given.
        GameContext context = on(Boards.parse("PP~~~~"));

        // When.
        rules.apply(new Deploy(new Position(0, 1), 1), alice, context);

        // Then.
        assertEquals(List.of(new Position(0, 1)), context.board().territoriesOf(alice));
    }

    @ParameterizedTest
    @CsvSource({"ROCK,8", "SAND,5", "WOOD,2", "WHEAT,2"})
    void eachResourceSellsAtItsPrice(Resource resource, int price) {
        // Given.
        GameContext context = on(Boards.parse("PP~~~~"));
        alice.addResource(resource, 3);

        // When.
        rules.apply(new Exchange(resource, 2), alice, context);

        // Then.
        assertEquals(15 + 2 * price, alice.gold());
        assertEquals(1, alice.resource(resource));
    }

    @Test
    void onlyHeldResourcesCanBeSold() {
        // Given.
        GameContext context = on(Boards.parse("PP~~~~"));
        alice.addResource(Resource.WOOD, 2);

        // When.
        List<Exchange> exchanges = rules.legalActions(alice, context.board()).stream()
                .filter(Exchange.class::isInstance).map(Exchange.class::cast).toList();

        // Then.
        assertEquals(List.of(new Exchange(Resource.WOOD, 1), new Exchange(Resource.WOOD, 2)), exchanges);
    }

    @Test
    void passingEarnsGoldPerForestPlainAndDesertTerritory() {
        // Given.
        GameContext context = on(Boards.parse("FPDM~~~~~~~~"));
        for (int col = 0; col < 4; col++) {
            put(context, alice, new Position(0, col));
        }

        // When.
        rules.apply(new Pass(), alice, context);

        // Then.
        assertEquals(15 + 1 + 1 + 2, alice.gold());
    }

    @Test
    void harvestingYieldsOneUnitPerTerritory() {
        // Given.
        GameContext context = on(Boards.parse("PPF~~~~~~"));
        put(context, alice, new Position(0, 0));
        put(context, alice, new Position(0, 1));
        put(context, alice, new Position(0, 2));

        // When.
        rules.harvest(alice, context);

        // Then.
        assertEquals(2, alice.resource(Resource.WHEAT));
        assertEquals(1, alice.resource(Resource.WOOD));
    }

    @Test
    void workersArePaidAccordingToTheirTerrain() {
        // Given.
        GameContext context = on(Boards.parse("MDFP~~~~~~~~"));
        List<Worker> workers = new ArrayList<>();
        for (int col = 0; col < 4; col++) {
            workers.add(put(context, alice, new Position(0, col)));
        }

        // When.
        rules.upkeep(alice, context);

        // Then.
        assertEquals(15 - 5 - 3 - 1 - 1, alice.gold());
        assertEquals(List.of(5, 3, 1, 1), workers.stream().map(Worker::gold).toList());
    }

    @Test
    void anUnpaidWorkerLeavesTheGameAndFreesItsTile() {
        // Given.
        GameContext context = on(Boards.parse("MP~~~~"));
        alice.spendGold(12);
        put(context, alice, new Position(0, 0));
        Worker paid = put(context, alice, new Position(0, 1));

        // When.
        rules.upkeep(alice, context);

        // Then.
        assertTrue(context.board().isFree(new Position(0, 0)));
        assertEquals(1, paid.gold());
        assertEquals(2, alice.gold());
        assertTrue(events.contains(new WorkerDismissed(alice, new Position(0, 0), 5)));
    }

    @Test
    void sellingMoreThanTheHeldQuantityIsRejected() {
        // Given.
        GameContext context = on(Boards.parse("PP~~~~"));
        alice.addResource(Resource.ROCK, 1);

        // Then.
        assertThrows(IllegalStateException.class, () -> rules.apply(new Exchange(Resource.ROCK, 2), alice, context));
        assertEquals(15, alice.gold());
    }

    @Test
    void occupiedTilesCannotBeDeployedOn() {
        // Given.
        GameContext context = on(Boards.parse("PP~~~~"));
        put(context, bob, new Position(0, 0));

        // When.
        List<Deploy> deploys = rules.legalActions(alice, context.board()).stream()
                .filter(Deploy.class::isInstance).map(Deploy.class::cast).toList();

        // Then.
        assertEquals(List.of(new Deploy(new Position(0, 1), 1)), deploys);
    }

    @Test
    void passingWithoutTerritoryEarnsNothing() {
        // Given.
        GameContext context = on(Boards.parse("PP~~~~"));

        // When.
        rules.apply(new Pass(), alice, context);

        // Then.
        assertEquals(15, alice.gold());
        assertTrue(events.contains(new Passed(alice, 0)));
    }

    @Test
    void harvestedResourcesCanOnlyBeSoldOnALaterTurn() {
        // Given.
        GameContext context = on(Boards.parse("MP~~~~"));
        put(context, alice, new Position(0, 0));

        // When.
        List<Action> beforeHarvest = rules.legalActions(alice, context.board());
        rules.harvest(alice, context);
        List<Action> afterHarvest = rules.legalActions(alice, context.board());

        // Then.
        assertTrue(beforeHarvest.stream().noneMatch(Exchange.class::isInstance));
        assertTrue(afterHarvest.contains(new Exchange(Resource.ROCK, 1)));
    }

    @Test
    void aDismissedWorkerTakesItsEarningsOutOfTheScore() {
        // Given.
        GameContext context = on(Boards.parse("MP~~~~"));
        put(context, alice, new Position(0, 0)).addGold(5);
        put(context, alice, new Position(0, 1)).addGold(2);
        alice.spendGold(15);

        // When.
        rules.upkeep(alice, context);

        // Then.
        assertEquals(0, rules.score(alice, context.board()));
    }

    @Test
    void scoreIsTheGoldEarnedByTheWorkers() {
        // Given.
        GameContext context = on(Boards.parse("PP~~~~"));
        put(context, alice, new Position(0, 0)).addGold(4);
        put(context, alice, new Position(0, 1)).addGold(3);

        // When.
        int score = rules.score(alice, context.board());

        // Then.
        assertEquals(7, score);
    }
}
