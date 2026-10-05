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
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
    public boolean simultaneous() {
        return true;
    }

    @Override
    public void apply(Action action, Player player, GameContext context) {
        resolve(Map.of(player, action), context);
    }

    @Override
    public void resolve(Map<Player, Action> orders, GameContext context) {
        Board board = context.board();
        orders.forEach((player, action) -> {
            if (!(action instanceof Deploy || action instanceof Attack || action instanceof Pass)) {
                throw new IllegalArgumentException("Unsupported action in the war game: " + action);
            }
            if (!legalActions(player, board).contains(action)) {
                throw new IllegalArgumentException(player + " cannot " + action);
            }
        });
        Map<Player, Attack> attacks = new LinkedHashMap<>();
        Map<Position, List<Player>> claims = new LinkedHashMap<>();
        Map<Player, Deploy> deploys = new HashMap<>();
        orders.forEach((player, action) -> {
            switch (action) {
                case Attack attack -> attacks.put(player, attack);
                case Deploy deploy -> {
                    claims.computeIfAbsent(deploy.position(), position -> new ArrayList<>()).add(player);
                    deploys.put(player, deploy);
                }
                default -> {
                }
            }
        });
        for (GameEvent consequence : volley(attacks, board)) {
            switch (consequence) {
                case ArmyDestroyed destroyed -> board.remove(destroyed.target());
                case ArmyPromoted promoted -> armyAt(board, promoted.position()).promote();
                default -> throw new IllegalStateException("Unexpected consequence: " + consequence);
            }
            context.emit(consequence);
        }
        claims.forEach((position, players) -> {
            players.forEach(player -> soldiers.merge(player, -deploys.get(player).size(), Integer::sum));
            if (players.size() == 1) {
                deploy(players.getFirst(), position, deploys.get(players.getFirst()).size(), context);
                return;
            }
            context.emit(new DeploymentsClashed(position, List.copyOf(players)));
            List<Player> bySize = players.stream()
                    .sorted(Comparator.comparingInt((Player player) -> deploys.get(player).size()).reversed()).toList();
            int survivors = deploys.get(bySize.get(0)).size() - deploys.get(bySize.get(1)).size();
            if (survivors > 0) {
                deploy(bySize.getFirst(), position, survivors, context);
            }
        });
        orders.forEach((player, action) -> {
            if (action instanceof Pass) {
                context.emit(new Passed(player, 0));
            }
        });
    }

    private static void deploy(Player player, Position position, int size, GameContext context) {
        Board board = context.board();
        board.place(position, new Army(player, size));
        context.emit(new ArmyDeployed(player, position, board.terrain(position), size));
    }

    @Override
    public List<GameEvent> preview(Action action, Player player, Board board) {
        return action instanceof Attack attack && canAttack(player, attack.from(), attack.target(), board)
                ? volley(Map.of(player, attack), board)
                : List.of();
    }

    private static List<GameEvent> volley(Map<Player, Attack> attacks, Board board) {
        Set<Position> targets = attacks.values().stream().map(Attack::target).collect(Collectors.toSet());
        Map<Position, GameEvent> destroyed = new LinkedHashMap<>();
        List<GameEvent> promoted = new ArrayList<>();
        attacks.forEach((player, attack) -> {
            Army attacker = armyAt(board, attack.from());
            Army defender = armyAt(board, attack.target());
            destroyed.putIfAbsent(attack.target(),
                    new ArmyDestroyed(player, attack.from(), defender.owner(), attack.target(), defender.size()));
            if (!targets.contains(attack.from()) && attacker.level() < Army.MAX_LEVEL) {
                promoted.add(new ArmyPromoted(player, attack.from(), attacker.level() + 1));
            }
        });
        List<GameEvent> consequences = new ArrayList<>(destroyed.values());
        consequences.addAll(promoted);
        return consequences;
    }

    private static boolean canAttack(Player player, Position from, Position target, Board board) {
        return board.occupant(from).orElse(null) instanceof Army attacker && attacker.owner() == player
                && board.occupant(target).orElse(null) instanceof Army defender && defender.owner() != player
                && reaches(from, target, board)
                && power(attacker) > defence(defender, board.terrain(target));
    }

    static boolean reaches(Position from, Position target, Board board) {
        int distance = from.distanceTo(target);
        return distance <= range(board.terrain(from)) && (board.terrain(target) != Terrain.FOREST || distance == 1);
    }

    static int range(Terrain terrain) {
        return switch (terrain) {
            case PLAIN, FOREST -> 1;
            case DESERT -> 2;
            case MOUNTAIN -> 3;
            case OCEAN -> 0;
        };
    }

    static int power(Army army) {
        return army.size() + army.level();
    }

    static int defence(Army army, Terrain terrain) {
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
        return board.territoriesOf(player).stream().mapToInt(position -> power(armyAt(board, position))).sum();
    }

    @Override
    public Map<String, Integer> reserves(Player player) {
        return Map.of("soldiers", soldiers(player));
    }
}
