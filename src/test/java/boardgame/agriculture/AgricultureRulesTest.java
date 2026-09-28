package boardgame.agriculture;

import boardgame.board.Board;
import boardgame.board.Boards;
import boardgame.board.Position;
import boardgame.board.Resource;
import boardgame.engine.Deploy;
import boardgame.engine.Exchange;
import boardgame.engine.GameContext;
import boardgame.engine.GameEvent;
import boardgame.engine.Pass;
import boardgame.player.Player;
import boardgame.unit.Worker;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
        on(Boards.parse("PP~~~~"));

        assertEquals(15, alice.gold());
    }

    @Test
    void deployingPlacesAWorkerOnTheChosenTile() {
        GameContext context = on(Boards.parse("PP~~~~"));

        rules.apply(new Deploy(new Position(0, 1), 1), alice, context);

        assertEquals(List.of(new Position(0, 1)), context.board().territoriesOf(alice));
    }

    @ParameterizedTest
    @CsvSource({"ROCK,8", "SAND,5", "WOOD,2", "WHEAT,2"})
    void eachResourceSellsAtItsPrice(Resource resource, int price) {
        GameContext context = on(Boards.parse("PP~~~~"));
        alice.addResource(resource, 3);

        rules.apply(new Exchange(resource, 2), alice, context);

        assertEquals(15 + 2 * price, alice.gold());
        assertEquals(1, alice.resource(resource));
    }

    @Test
    void onlyHeldResourcesCanBeSold() {
        GameContext context = on(Boards.parse("PP~~~~"));
        alice.addResource(Resource.WOOD, 2);

        List<Exchange> exchanges = rules.legalActions(alice, context.board()).stream()
                .filter(Exchange.class::isInstance).map(Exchange.class::cast).toList();

        assertEquals(List.of(new Exchange(Resource.WOOD, 1), new Exchange(Resource.WOOD, 2)), exchanges);
    }

    @Test
    void passingEarnsGoldPerForestPlainAndDesertTerritory() {
        GameContext context = on(Boards.parse("FPDM~~~~~~~~"));
        for (int col = 0; col < 4; col++) {
            put(context, alice, new Position(0, col));
        }

        rules.apply(new Pass(), alice, context);

        assertEquals(15 + 1 + 1 + 2, alice.gold());
    }

    @Test
    void harvestingYieldsOneUnitPerTerritory() {
        GameContext context = on(Boards.parse("PPF~~~~~~"));
        put(context, alice, new Position(0, 0));
        put(context, alice, new Position(0, 1));
        put(context, alice, new Position(0, 2));

        rules.harvest(alice, context);

        assertEquals(2, alice.resource(Resource.WHEAT));
        assertEquals(1, alice.resource(Resource.WOOD));
    }

    @Test
    void workersArePaidAccordingToTheirTerrain() {
        GameContext context = on(Boards.parse("MDFP~~~~~~~~"));
        List<Worker> workers = new ArrayList<>();
        for (int col = 0; col < 4; col++) {
            workers.add(put(context, alice, new Position(0, col)));
        }

        rules.upkeep(alice, context);

        assertEquals(15 - 5 - 3 - 1 - 1, alice.gold());
        assertEquals(List.of(5, 3, 1, 1), workers.stream().map(Worker::gold).toList());
    }

    @Test
    void anUnpaidWorkerLeavesTheGameAndFreesItsTile() {
        GameContext context = on(Boards.parse("MP~~~~"));
        alice.spendGold(12);
        put(context, alice, new Position(0, 0));
        Worker paid = put(context, alice, new Position(0, 1));

        rules.upkeep(alice, context);

        assertTrue(context.board().isFree(new Position(0, 0)));
        assertEquals(1, paid.gold());
        assertEquals(2, alice.gold());
        assertTrue(events.contains(new WorkerDismissed(alice, new Position(0, 0), 5)));
    }

    @Test
    void scoreIsTheGoldEarnedByTheWorkers() {
        GameContext context = on(Boards.parse("PP~~~~"));
        put(context, alice, new Position(0, 0)).addGold(4);
        put(context, alice, new Position(0, 1)).addGold(3);

        assertEquals(7, rules.score(alice, context.board()));
    }
}
