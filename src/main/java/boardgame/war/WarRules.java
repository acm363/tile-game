package boardgame.war;

import boardgame.board.Board;
import boardgame.board.Position;
import boardgame.board.Terrain;
import boardgame.engine.Action;
import boardgame.engine.Attack;
import boardgame.engine.Deploy;
import boardgame.engine.GameContext;
import boardgame.engine.GameEvent;
import boardgame.engine.GameRules;
import boardgame.engine.Pass;
import boardgame.engine.Passed;
import boardgame.player.Player;
import boardgame.unit.Army;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class WarRules implements GameRules {

    static final int ROUNDS = 10;
    static final int INITIAL_SOLDIERS = 35;
    static final int DESERT_EXPOSURE = 1;

    private final Map<Player, Integer> soldiers = new HashMap<>();

    @Override
    public int defaultRounds() {
        return ROUNDS;
    }

    @Override
    public void setUp(Player player) {
        soldiers.put(player, INITIAL_SOLDIERS);
    }

    public int soldiers(Player player) {
        Integer reserve = soldiers.get(player);
        if (reserve == null) {
            throw new IllegalStateException(player + " is not part of this war game");
        }
        return reserve;
    }

    @Override
    public List<Action> legalActions(Player player, Board board) {
        List<Action> actions = new ArrayList<>();
        actions.add(new Pass());
        int maxSize = Math.min(Army.MAX_SIZE, soldiers(player));
        for (Position position : board.freePositions()) {
            for (int size = Army.MIN_SIZE; size <= maxSize; size++) {
                actions.add(new Deploy(position, size));
            }
        }
        for (Position from : board.territoriesOf(player)) {
            for (Position target : board.positions()) {
                if (canAttack(player, from, target, board)) {
                    actions.add(new Attack(from, target));
                }
            }
        }
        return actions;
    }

    @Override
    public void apply(Action action, Player player, GameContext context) {
        switch (action) {
            case Deploy deploy -> deploy(player, deploy.position(), deploy.size(), context);
            case Attack attack -> attack(player, attack, context);
            case Pass pass -> context.emit(new Passed(player, 0));
            default -> throw new IllegalArgumentException("Unsupported action in the war game: " + action);
        }
    }

    private void deploy(Player player, Position position, int size, GameContext context) {
        Board board = context.board();
        if (size > soldiers(player)) {
            throw new IllegalArgumentException(player + " cannot deploy " + size + " soldiers out of " + soldiers(player));
        }
        board.place(position, new Army(player, size));
        soldiers.merge(player, -size, Integer::sum);
        context.emit(new ArmyDeployed(player, position, board.terrain(position), size));
    }

    private void attack(Player player, Attack attack, GameContext context) {
        Board board = context.board();
        if (!canAttack(player, attack.from(), attack.target(), board)) {
            throw new IllegalArgumentException(player + " cannot win an attack on " + attack.target()
                    + " from " + attack.from());
        }
        for (GameEvent consequence : consequences(player, attack, board)) {
            switch (consequence) {
                case ArmyDestroyed destroyed -> board.remove(destroyed.target());
                case ArmyPromoted promoted -> armyAt(board, promoted.position()).promote();
                default -> throw new IllegalStateException("Unexpected consequence: " + consequence);
            }
            context.emit(consequence);
        }
    }

    @Override
    public List<GameEvent> preview(Action action, Player player, Board board) {
        return action instanceof Attack attack && canAttack(player, attack.from(), attack.target(), board)
                ? consequences(player, attack, board)
                : List.of();
    }

    private static List<GameEvent> consequences(Player player, Attack attack, Board board) {
        Army attacker = armyAt(board, attack.from());
        Army defender = armyAt(board, attack.target());
        List<GameEvent> consequences = new ArrayList<>();
        consequences.add(new ArmyDestroyed(player, attack.from(), defender.owner(), attack.target(), defender.size()));
        if (attacker.level() < Army.MAX_LEVEL) {
            consequences.add(new ArmyPromoted(player, attack.from(), attacker.level() + 1));
        }
        return consequences;
    }

    private static boolean canAttack(Player player, Position from, Position target, Board board) {
        return board.occupant(from).orElse(null) instanceof Army attacker && attacker.owner() == player
                && board.occupant(target).orElse(null) instanceof Army defender && defender.owner() != player
                && reaches(from, target, board)
                && power(attacker) > defence(defender, board.terrain(target));
    }

    private static boolean reaches(Position from, Position target, Board board) {
        int distance = from.distanceTo(target);
        return distance <= range(board.terrain(from)) && (board.terrain(target) != Terrain.FOREST || distance == 1);
    }

    private static int range(Terrain terrain) {
        return switch (terrain) {
            case PLAIN, FOREST -> 1;
            case DESERT -> 2;
            case MOUNTAIN -> 3;
            case OCEAN -> 0;
        };
    }

    private static int power(Army army) {
        return army.size() + army.level();
    }

    private static int defence(Army army, Terrain terrain) {
        return power(army) - (terrain == Terrain.DESERT ? DESERT_EXPOSURE : 0);
    }

    private static Army armyAt(Board board, Position position) {
        return (Army) board.occupant(position).orElseThrow();
    }

    @Override
    public void harvest(Player player, GameContext context) {
    }

    @Override
    public void upkeep(Player player, GameContext context) {
    }

    @Override
    public int score(Player player, Board board) {
        return board.territoriesOf(player).size();
    }

    @Override
    public Map<String, Integer> reserves(Player player) {
        return Map.of("soldiers", soldiers(player));
    }
}
