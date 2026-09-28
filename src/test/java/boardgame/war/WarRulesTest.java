package boardgame.war;

import boardgame.board.Board;
import boardgame.board.Boards;
import boardgame.board.Position;
import boardgame.board.Resource;
import boardgame.engine.Action;
import boardgame.engine.Deploy;
import boardgame.engine.GameContext;
import boardgame.engine.GameEvent;
import boardgame.engine.Pass;
import boardgame.player.Player;
import boardgame.unit.Army;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WarRulesTest {

    private static final Position LEFT = new Position(0, 0);
    private static final Position RIGHT = new Position(0, 1);

    private final Player alice = new Player("Alice");
    private final Player bob = new Player("Bob");
    private final WarRules rules = new WarRules();
    private final List<GameEvent> events = new ArrayList<>();

    private GameContext on(Board board) {
        rules.setUp(alice);
        rules.setUp(bob);
        GameContext context = new GameContext(board, List.of(alice, bob));
        context.addListener(events::add);
        return context;
    }

    private Army put(GameContext context, Player owner, int size, Position position) {
        Army army = new Army(owner, size);
        context.board().place(position, army);
        return army;
    }

    private Army deploy(GameContext context, Player player, int size, Position position) {
        rules.apply(new Deploy(position, size), player, context);
        return (Army) context.board().occupant(position).orElseThrow();
    }

    @Test
    void playersStartWith35WarriorsTenFoodAndNoGold() {
        on(Boards.parse("PP~~~~"));

        assertEquals(35, rules.warriors(alice));
        assertEquals(10, rules.food(alice));
        assertEquals(0, alice.gold());
    }

    @Test
    void deployingTakesWarriorsFromTheReserve() {
        GameContext context = on(Boards.parse("PP~~~~"));

        deploy(context, alice, 5, LEFT);

        assertEquals(30, rules.warriors(alice));
    }

    @Test
    void mountainsAndDesertsHoldAtMostThreeWarriors() {
        GameContext context = on(Boards.parse("MDPF~~~~~~~~"));

        List<Action> actions = rules.legalActions(alice, context.board());

        assertEquals(3, maxDeployableSize(actions, new Position(0, 0)));
        assertEquals(3, maxDeployableSize(actions, new Position(0, 1)));
        assertEquals(5, maxDeployableSize(actions, new Position(0, 2)));
        assertEquals(5, maxDeployableSize(actions, new Position(0, 3)));
        assertThrows(IllegalArgumentException.class, () -> deploy(context, alice, 4, new Position(0, 0)));
    }

    @Test
    void aPlayerWithoutWarriorsCanOnlyPass() {
        GameContext context = on(Boards.parse("PPPPPPPP", "~~~~~~~~", "~~~~~~~~"));
        for (int col = 0; col < 7; col++) {
            deploy(context, alice, 5, new Position(0, col));
        }

        assertEquals(List.of(new Pass()), rules.legalActions(alice, context.board()));
    }

    @Test
    void aWeakerEnemyIsHalved() {
        GameContext context = on(Boards.parse("PP~~~~"));
        Army enemy = put(context, bob, 3, LEFT);

        deploy(context, alice, 5, RIGHT);

        assertEquals(1, enemy.size());
        assertSame(bob, enemy.owner());
        assertTrue(events.contains(new ArmyWeakened(bob, LEFT, 1)));
    }

    @Test
    void anEnemyHalvedBelowOneWarriorRalliesAndEarnsTheDeployedArmyTwoGold() {
        GameContext context = on(Boards.parse("PP~~~~"));
        Army enemy = put(context, bob, 1, LEFT);

        Army deployed = deploy(context, alice, 2, RIGHT);

        assertSame(alice, enemy.owner());
        assertEquals(1, enemy.size());
        assertEquals(2, deployed.gold());
        assertEquals(List.of(LEFT, RIGHT), context.board().territoriesOf(alice));
    }

    @Test
    void anEnemyOnAMountainCountsTwoMoreWarriors() {
        GameContext context = on(Boards.parse("MP~~~~"));
        Army enemy = put(context, bob, 2, LEFT);

        deploy(context, alice, 4, RIGHT);

        assertEquals(2, enemy.size());
    }

    @Test
    void anArmyDeployedOnAMountainCountsTwoMoreAgainstEnemies() {
        GameContext context = on(Boards.parse("PM~~~~"));
        Army enemy = put(context, bob, 4, LEFT);

        deploy(context, alice, 3, RIGHT);

        assertEquals(2, enemy.size());
    }

    @Test
    void anEnemyAtLeastAsStrongIsUnaffected() {
        GameContext context = on(Boards.parse("PP~~~~"));
        Army enemy = put(context, bob, 3, LEFT);

        Army deployed = deploy(context, alice, 3, RIGHT);

        assertEquals(3, enemy.size());
        assertEquals(0, deployed.gold());
    }

    @Test
    void aWeakerAllyGainsOneWarriorAndEarnsTheDeployedArmyOneGold() {
        GameContext context = on(Boards.parse("PP~~~~"));
        Army ally = put(context, alice, 2, LEFT);

        Army deployed = deploy(context, alice, 4, RIGHT);

        assertEquals(3, ally.size());
        assertEquals(1, deployed.gold());
    }

    @Test
    void anAllyInTheDesertStaysCappedAtThreeWarriors() {
        GameContext context = on(Boards.parse("DP~~~~"));
        Army ally = put(context, alice, 3, LEFT);

        deploy(context, alice, 5, RIGHT);

        assertEquals(3, ally.size());
    }

    @Test
    void theMountainBonusDoesNotApplyBetweenAllies() {
        GameContext context = on(Boards.parse("MP~~~~"));
        Army ally = put(context, alice, 2, LEFT);

        deploy(context, alice, 3, RIGHT);

        assertEquals(3, ally.size());
    }

    @Test
    void anAllyAtLeastAsStrongIsUnaffected() {
        GameContext context = on(Boards.parse("PP~~~~"));
        Army ally = put(context, alice, 4, LEFT);

        Army deployed = deploy(context, alice, 4, RIGHT);

        assertEquals(4, ally.size());
        assertEquals(0, deployed.gold());
    }

    @Test
    void armiesEatTheirSizeAndTwiceItInTheDesert() {
        GameContext context = on(Boards.parse("PD~~~~"));
        put(context, alice, 3, LEFT);
        put(context, alice, 2, RIGHT);

        rules.upkeep(alice, context);

        assertEquals(10 - 3 - 4, rules.food(alice));
        assertEquals(2, context.board().territoriesOf(alice).size());
    }

    @Test
    void anArmyWithExactlyEnoughFoodSurvives() {
        GameContext context = on(Boards.parse("PP~~~~"));
        put(context, alice, 5, LEFT);
        put(context, alice, 5, RIGHT);

        rules.upkeep(alice, context);

        assertEquals(0, rules.food(alice));
        assertEquals(2, context.board().territoriesOf(alice).size());
    }

    @Test
    void aStarvingArmyIsDestroyedFreesItsTileAndPaysOneGold() {
        GameContext context = on(Boards.parse("DP~~~~"));
        put(context, alice, 3, LEFT);
        put(context, alice, 5, RIGHT);

        rules.upkeep(alice, context);

        assertTrue(context.board().isFree(RIGHT));
        assertEquals(List.of(LEFT), context.board().territoriesOf(alice));
        assertEquals(1, alice.gold());
        assertEquals(4, rules.food(alice));
        assertTrue(events.contains(new ArmyStarved(alice, RIGHT, 5)));
    }

    @Test
    void wheatAndWoodAreConvertedIntoFoodWhileRockAndSandAreWorthNothing() {
        GameContext context = on(Boards.parse("PFMD~~~~~~~~"));
        put(context, alice, 1, new Position(0, 0));
        put(context, alice, 1, new Position(0, 1));
        put(context, alice, 1, new Position(0, 2));
        put(context, alice, 1, new Position(0, 3));

        rules.harvest(alice, context);
        rules.upkeep(alice, context);

        assertEquals(10 + 5 + 1 - 1 - 1 - 1 - 2, rules.food(alice));
        assertEquals(0, alice.resource(Resource.WHEAT));
        assertEquals(0, alice.resource(Resource.WOOD));
        assertEquals(1, alice.resource(Resource.ROCK));
    }

    @Test
    void scoreAddsPlayerGoldArmyGoldAndTerrainBonuses() {
        GameContext context = on(Boards.parse("PFMD~~~~~~~~"));
        alice.addGold(3);
        put(context, alice, 1, new Position(0, 0)).addGold(2);
        put(context, alice, 1, new Position(0, 1));
        put(context, alice, 1, new Position(0, 2));
        put(context, alice, 1, new Position(0, 3));

        assertEquals(3 + 2 + 1 + 2 + 4 + 4, rules.score(alice, context.board()));
    }

    @Test
    void tenTerritoriesEarnFiveBonusPoints() {
        GameContext context = on(Boards.parse("PPPPPPPPPP", "~~~~~~~~~~", "~~~~~~~~~~", "~~~~~~~~~~"));
        for (int col = 0; col < 10; col++) {
            put(context, alice, 1, new Position(0, col));
        }

        assertEquals(10 + 5, rules.score(alice, context.board()));
    }

    private static int maxDeployableSize(List<Action> actions, Position position) {
        return actions.stream()
                .filter(action -> action instanceof Deploy deploy && deploy.position().equals(position))
                .mapToInt(action -> ((Deploy) action).size())
                .max()
                .orElse(0);
    }
}
