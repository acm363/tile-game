package boardgame.war;

import boardgame.board.Board;
import boardgame.board.Boards;
import boardgame.board.Position;
import boardgame.board.Resource;
import boardgame.engine.Action;
import boardgame.engine.Attack;
import boardgame.engine.Deploy;
import boardgame.engine.Exchange;
import boardgame.engine.GameContext;
import boardgame.engine.GameEvent;
import boardgame.engine.Pass;
import boardgame.player.Player;
import boardgame.unit.Army;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

    private Army put(GameContext context, Player owner, int size, int level, Position position) {
        Army army = put(context, owner, size, position);
        for (int promotion = 0; promotion < level; promotion++) {
            army.promote();
        }
        return army;
    }

    private Army deploy(GameContext context, Player player, int size, Position position) {
        rules.apply(new Deploy(position, size), player, context);
        return (Army) context.board().occupant(position).orElseThrow();
    }

    private List<Attack> attacks(GameContext context, Player player) {
        return rules.legalActions(player, context.board()).stream()
                .filter(Attack.class::isInstance).map(Attack.class::cast).toList();
    }

    private Set<Position> targetsFrom(GameContext context, Position from) {
        return attacks(context, alice).stream()
                .filter(attack -> attack.from().equals(from)).map(Attack::target).collect(Collectors.toSet());
    }

    @Test
    void anArmyOnAPlainOnlyReachesItsNeighbours() {
        // Given.
        GameContext context = on(Boards.parse("PPP"));
        put(context, alice, 3, LEFT);
        put(context, bob, 1, RIGHT);
        put(context, bob, 1, new Position(0, 2));

        // Then.
        assertEquals(Set.of(RIGHT), targetsFrom(context, LEFT));
    }

    @Test
    void anArmyInTheDesertReachesTwoTiles() {
        // Given.
        GameContext context = on(Boards.parse("DPPP"));
        put(context, alice, 3, LEFT);
        put(context, bob, 1, new Position(0, 2));
        put(context, bob, 1, new Position(0, 3));

        // Then.
        assertEquals(Set.of(new Position(0, 2)), targetsFrom(context, LEFT));
    }

    @Test
    void anArmyOnAMountainReachesThreeTiles() {
        // Given.
        GameContext context = on(Boards.parse("MPPPP"));
        put(context, alice, 3, LEFT);
        put(context, bob, 1, new Position(0, 3));
        put(context, bob, 1, new Position(0, 4));

        // Then.
        assertEquals(Set.of(new Position(0, 3)), targetsFrom(context, LEFT));
    }

    @Test
    void distanceCountsOrthogonalStepsSoADiagonalIsTwoTilesAway() {
        // Given.
        GameContext onPlain = on(Boards.parse("PP", "PP"));
        put(onPlain, alice, 3, LEFT);
        put(onPlain, bob, 1, new Position(1, 1));
        GameContext inDesert = on(Boards.parse("DP", "PP"));
        put(inDesert, alice, 3, LEFT);
        put(inDesert, bob, 1, new Position(1, 1));

        // Then.
        assertEquals(Set.of(), targetsFrom(onPlain, LEFT));
        assertEquals(Set.of(new Position(1, 1)), targetsFrom(inDesert, LEFT));
    }

    @Test
    void anArmyInAForestCanOnlyBeAttackedFromANeighbour() {
        // Given.
        GameContext context = on(Boards.parse("MPPF"));
        put(context, alice, 5, LEFT);
        put(context, alice, 2, new Position(0, 2));
        put(context, bob, 1, new Position(0, 3));

        // Then.
        assertEquals(List.of(new Attack(new Position(0, 2), new Position(0, 3))), attacks(context, alice));
    }

    @Test
    void fiveSoldiersOnAMountainDestroyThreeOnAPlainThreeTilesAwayButNotInAForest() {
        // Given.
        GameContext context = on(Boards.parse("MPPP", "~~F~"));
        put(context, alice, 5, LEFT);
        put(context, bob, 3, new Position(0, 3));
        put(context, bob, 3, new Position(1, 2));

        // Then.
        assertEquals(Set.of(new Position(0, 3)), targetsFrom(context, LEFT));
    }

    @Test
    void anEqualPowerIsNotEnoughToAttack() {
        // Given.
        GameContext context = on(Boards.parse("PP"));
        put(context, alice, 3, LEFT);
        put(context, bob, 3, RIGHT);

        // Then.
        assertEquals(List.of(), attacks(context, alice));
    }

    @Test
    void anArmyInTheDesertDefendsWithOnePowerLess() {
        // Given.
        GameContext context = on(Boards.parse("PD"));
        put(context, alice, 3, LEFT);
        put(context, bob, 3, RIGHT);

        // Then.
        assertEquals(List.of(new Attack(LEFT, RIGHT)), attacks(context, alice));
    }

    @Test
    void levelsAddToPowerWhenAttackingAndWhenDefending() {
        // Given.
        GameContext context = on(Boards.parse("PPP"));
        put(context, alice, 3, 1, new Position(0, 1));
        put(context, bob, 3, LEFT);
        put(context, bob, 3, 1, new Position(0, 2));

        // Then.
        assertEquals(Set.of(LEFT), targetsFrom(context, new Position(0, 1)));
    }

    @Test
    void alliesAreNeverTargets() {
        // Given.
        GameContext context = on(Boards.parse("PP"));
        put(context, alice, 5, LEFT);
        put(context, alice, 1, RIGHT);

        // Then.
        assertEquals(List.of(), attacks(context, alice));
    }

    @Test
    void attacksReachOverOceanAndOtherArmies() {
        // Given.
        GameContext context = on(Boards.parse("M~PP"));
        put(context, alice, 5, LEFT);
        put(context, bob, 1, new Position(0, 2));
        put(context, bob, 1, new Position(0, 3));

        // Then.
        assertEquals(Set.of(new Position(0, 2), new Position(0, 3)), targetsFrom(context, LEFT));
    }

    @Test
    void aWonAttackDestroysTheEnemyFreesItsTileAndPromotesTheAttacker() {
        // Given.
        GameContext context = on(Boards.parse("PP"));
        Army attacker = put(context, alice, 3, LEFT);
        put(context, bob, 2, RIGHT);

        // When.
        rules.apply(new Attack(LEFT, RIGHT), alice, context);

        // Then.
        assertTrue(context.board().isFree(RIGHT));
        assertEquals(1, attacker.level());
        assertEquals(List.of(new ArmyDestroyed(alice, LEFT, bob, RIGHT, 2), new ArmyPromoted(alice, LEFT, 1)), events);
    }

    @Test
    void anArmyStopsLevellingUpAtThree() {
        // Given.
        GameContext context = on(Boards.parse("PP"));
        Army attacker = put(context, alice, 1, Army.MAX_LEVEL, LEFT);
        put(context, bob, 2, RIGHT);

        // When.
        rules.apply(new Attack(LEFT, RIGHT), alice, context);

        // Then.
        assertEquals(Army.MAX_LEVEL, attacker.level());
        assertEquals(List.of(new ArmyDestroyed(alice, LEFT, bob, RIGHT, 2)), events);
    }

    @Test
    void anAttackThatCannotBeWonIsRejectedAndChangesNothing() {
        // Given.
        GameContext context = on(Boards.parse("PP"));
        put(context, alice, 3, LEFT);
        put(context, bob, 3, RIGHT);

        // Then.
        assertThrows(IllegalArgumentException.class, () -> rules.apply(new Attack(LEFT, RIGHT), alice, context));
        assertEquals(List.of(RIGHT), context.board().territoriesOf(bob));
        assertTrue(events.isEmpty());
    }

    @Test
    void playersStartWith35SoldiersAndNothingElse() {
        // When.
        on(Boards.parse("PP"));

        // Then.
        assertEquals(35, rules.soldiers(alice));
        assertEquals(Map.of("soldiers", 35), rules.reserves(alice));
        assertEquals(0, alice.gold());
    }

    @Test
    void deployingTakesSoldiersFromTheReserve() {
        // Given.
        GameContext context = on(Boards.parse("PP"));

        // When.
        Army army = deploy(context, alice, 5, LEFT);

        // Then.
        assertEquals(30, rules.soldiers(alice));
        assertEquals(0, army.level());
        assertEquals(List.of(new ArmyDeployed(alice, LEFT, context.board().terrain(LEFT), 5)), events);
    }

    @Test
    void everyLandTileHoldsUpToFiveSoldiers() {
        // Given.
        GameContext context = on(Boards.parse("MDPF"));

        // When.
        List<Action> actions = rules.legalActions(alice, context.board());

        // Then.
        for (int col = 0; col < 4; col++) {
            assertEquals(5, maxDeployableSize(actions, new Position(0, col)));
        }
    }

    @Test
    void aDeploymentLeavesItsNeighboursUntouched() {
        // Given.
        GameContext context = on(Boards.parse("PPP"));
        Army enemy = put(context, bob, 1, LEFT);
        Army ally = put(context, alice, 1, new Position(0, 2));

        // When.
        deploy(context, alice, 5, RIGHT);

        // Then.
        assertEquals(List.of(LEFT), context.board().territoriesOf(bob));
        assertEquals(1, enemy.size());
        assertEquals(1, ally.size());
        assertEquals(1, events.size());
    }

    @Test
    void aPlayerWithoutSoldiersCanStillAttackOrPass() {
        // Given.
        GameContext context = on(Boards.parse("PPPPPPPP", "~~~~~~~~"));
        for (int col = 0; col < 7; col++) {
            deploy(context, alice, 5, new Position(0, col));
        }
        put(context, bob, 1, new Position(0, 7));

        // Then.
        assertEquals(List.of(new Pass(), new Attack(new Position(0, 6), new Position(0, 7))),
                rules.legalActions(alice, context.board()));
    }

    @Test
    void onlyFreeLandTilesCanBeDeployedOn() {
        // Given.
        GameContext context = on(Boards.parse("P~P"));
        put(context, bob, 1, LEFT);

        // When.
        List<Position> targets = rules.legalActions(alice, context.board()).stream()
                .filter(Deploy.class::isInstance).map(action -> ((Deploy) action).position()).distinct().toList();

        // Then.
        assertEquals(List.of(new Position(0, 2)), targets);
    }

    @Test
    void armiesNeitherHarvestNorEat() {
        // Given.
        GameContext context = on(Boards.parse("PFMD"));
        for (int col = 0; col < 4; col++) {
            put(context, alice, 5, new Position(0, col));
        }

        // When.
        rules.harvest(alice, context);
        rules.upkeep(alice, context);

        // Then.
        assertEquals(4, context.board().territoriesOf(alice).size());
        assertTrue(alice.resources().isEmpty());
        assertTrue(events.isEmpty());
    }

    @Test
    void eachTileHeldScoresOnePoint() {
        // Given.
        GameContext context = on(Boards.parse("PMPF"));
        put(context, alice, 1, LEFT);
        put(context, alice, 5, 3, RIGHT);
        put(context, bob, 5, new Position(0, 2));

        // Then.
        assertEquals(2, rules.score(alice, context.board()));
        assertEquals(1, rules.score(bob, context.board()));
    }

    @Test
    void thePreviewOfAnAttackAnnouncesExactlyWhatItWillDo() {
        // Given.
        GameContext context = on(Boards.parse("PP"));
        put(context, alice, 3, LEFT);
        put(context, bob, 2, RIGHT);
        Attack attack = new Attack(LEFT, RIGHT);

        // When.
        List<GameEvent> preview = rules.preview(attack, alice, context.board());
        rules.apply(attack, alice, context);

        // Then.
        assertEquals(List.of(new ArmyDestroyed(alice, LEFT, bob, RIGHT, 2), new ArmyPromoted(alice, LEFT, 1)), preview);
        assertEquals(preview, events);
    }

    @Test
    void previewingAnAttackLeavesTheBoardUntouched() {
        // Given.
        GameContext context = on(Boards.parse("PP"));
        Army attacker = put(context, alice, 3, LEFT);
        put(context, bob, 2, RIGHT);

        // When.
        rules.preview(new Attack(LEFT, RIGHT), alice, context.board());

        // Then.
        assertEquals(List.of(RIGHT), context.board().territoriesOf(bob));
        assertEquals(0, attacker.level());
    }

    @Test
    void deploymentsAndPassesHaveNoPreview() {
        // Given.
        GameContext context = on(Boards.parse("PP"));
        put(context, bob, 1, LEFT);

        // Then.
        assertTrue(rules.preview(new Deploy(RIGHT, 5), alice, context.board()).isEmpty());
        assertTrue(rules.preview(new Pass(), alice, context.board()).isEmpty());
    }

    @Test
    void rulesRefuseActionsTheyDoNotKnow() {
        // Given.
        GameContext context = on(Boards.parse("PP"));

        // Then.
        assertThrows(IllegalArgumentException.class,
                () -> rules.apply(new Exchange(Resource.WOOD, 1), alice, context));
    }

    private static int maxDeployableSize(List<Action> actions, Position position) {
        return actions.stream()
                .filter(action -> action instanceof Deploy deploy && deploy.position().equals(position))
                .mapToInt(action -> ((Deploy) action).size())
                .max()
                .orElse(0);
    }
}
