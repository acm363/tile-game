package boardgame.war;

import boardgame.board.Board;
import boardgame.board.Position;
import boardgame.board.Resource;
import boardgame.board.Terrain;
import boardgame.engine.Action;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class WarRules implements GameRules {

    static final int ROUNDS = 10;
    static final int INITIAL_WARRIORS = 35;
    static final int INITIAL_FOOD = 10;
    static final int CAPTURE_REWARD = 2;
    static final int REINFORCEMENT_REWARD = 1;
    static final int STARVATION_COMPENSATION = 1;
    static final int MOUNTAIN_DEFENCE_BONUS = 2;
    static final int HIGH_GROUND_MAX_SIZE = 3;
    static final int TERRITORY_BONUS_THRESHOLD = 10;
    static final int TERRITORY_BONUS = 5;

    private final Map<Player, Supplies> supplies = new HashMap<>();

    @Override
    public int defaultRounds() {
        return ROUNDS;
    }

    @Override
    public void setUp(Player player) {
        supplies.put(player, new Supplies(INITIAL_WARRIORS, INITIAL_FOOD));
    }

    public int warriors(Player player) {
        return supplies(player).warriors;
    }

    public int food(Player player) {
        return supplies(player).food;
    }

    @Override
    public List<Action> legalActions(Player player, Board board) {
        List<Action> actions = new ArrayList<>();
        actions.add(new Pass());
        int warriors = warriors(player);
        for (Position position : board.freePositions()) {
            int maxSize = Math.min(maxSize(board.terrain(position)), warriors);
            for (int size = Army.MIN_SIZE; size <= maxSize; size++) {
                actions.add(new Deploy(position, size));
            }
        }
        return actions;
    }

    @Override
    public void apply(Action action, Player player, GameContext context) {
        switch (action) {
            case Deploy deploy -> deploy(player, deploy.position(), deploy.size(), context);
            case Pass pass -> context.emit(new Passed(player, 0));
            default -> throw new IllegalArgumentException("Unsupported action in the war game: " + action);
        }
    }

    private void deploy(Player player, Position position, int size, GameContext context) {
        Board board = context.board();
        if (size > maxSize(board.terrain(position)) || size > warriors(player)) {
            throw new IllegalArgumentException("Cannot deploy " + size + " warriors on " + position);
        }
        List<GameEvent> confrontations = confrontations(player, position, size, board);
        Army army = new Army(player, size);
        board.place(position, army);
        supplies(player).warriors -= size;
        context.emit(new ArmyDeployed(player, position, board.terrain(position), size));

        for (GameEvent confrontation : confrontations) {
            switch (confrontation) {
                case ArmyRallied rallied -> {
                    armyAt(board, rallied.position()).changeOwner(player);
                    army.addGold(CAPTURE_REWARD);
                }
                case ArmyWeakened weakened -> armyAt(board, weakened.position()).setSize(weakened.size());
                case ArmyReinforced reinforced -> {
                    armyAt(board, reinforced.position()).setSize(reinforced.size());
                    army.addGold(REINFORCEMENT_REWARD);
                }
                default -> throw new IllegalStateException("Unexpected confrontation: " + confrontation);
            }
            context.emit(confrontation);
        }
    }

    @Override
    public List<GameEvent> preview(Action action, Player player, Board board) {
        return action instanceof Deploy deploy
                ? confrontations(player, deploy.position(), deploy.size(), board)
                : List.of();
    }

    private List<GameEvent> confrontations(Player player, Position position, int size, Board board) {
        int strength = strength(size, board.terrain(position));
        List<GameEvent> confrontations = new ArrayList<>();
        for (Position neighbour : board.neighbours(position)) {
            if (board.occupant(neighbour).orElse(null) instanceof Army other) {
                confront(player, size, strength, other, neighbour, board).ifPresent(confrontations::add);
            }
        }
        return confrontations;
    }

    private Optional<GameEvent> confront(Player player, int size, int strength, Army other, Position otherAt,
                                         Board board) {
        Terrain otherTerrain = board.terrain(otherAt);
        if (other.owner() != player) {
            if (strength(other.size(), otherTerrain) >= strength) {
                return Optional.empty();
            }
            int halved = other.size() / 2;
            return Optional.of(halved < Army.MIN_SIZE
                    ? new ArmyRallied(other.owner(), player, otherAt)
                    : new ArmyWeakened(other.owner(), otherAt, halved));
        }
        if (other.size() >= size) {
            return Optional.empty();
        }
        return Optional.of(new ArmyReinforced(player, otherAt, Math.min(other.size() + 1, maxSize(otherTerrain))));
    }

    private static Army armyAt(Board board, Position position) {
        return (Army) board.occupant(position).orElseThrow();
    }

    @Override
    public void upkeep(Player player, GameContext context) {
        Supplies playerSupplies = supplies(player);
        int produced = player.resource(Resource.WHEAT) * foodValue(Resource.WHEAT)
                + player.resource(Resource.WOOD) * foodValue(Resource.WOOD);
        player.removeResource(Resource.WHEAT, player.resource(Resource.WHEAT));
        player.removeResource(Resource.WOOD, player.resource(Resource.WOOD));
        if (produced > 0) {
            playerSupplies.food += produced;
            context.emit(new FoodProduced(player, produced));
        }

        Board board = context.board();
        for (Position territory : board.territoriesOf(player)) {
            Army army = (Army) board.occupant(territory).orElseThrow();
            int ration = army.size() * (board.terrain(territory) == Terrain.DESERT ? 2 : 1);
            if (playerSupplies.food >= ration) {
                playerSupplies.food -= ration;
            } else {
                board.remove(territory);
                player.addGold(STARVATION_COMPENSATION);
                context.emit(new ArmyStarved(player, territory, army.size()));
            }
        }
    }

    @Override
    public int score(Player player, Board board) {
        List<Position> territories = board.territoriesOf(player);
        int score = player.gold();
        for (Position territory : territories) {
            score += board.occupant(territory).orElseThrow().gold() + territoryBonus(board.terrain(territory));
        }
        if (territories.size() >= TERRITORY_BONUS_THRESHOLD) {
            score += TERRITORY_BONUS;
        }
        return score;
    }

    @Override
    public Map<String, Integer> reserves(Player player) {
        Map<String, Integer> reserves = new LinkedHashMap<>();
        reserves.put("warriors", warriors(player));
        reserves.put("food", food(player));
        return reserves;
    }

    static int maxSize(Terrain terrain) {
        return switch (terrain) {
            case MOUNTAIN, DESERT -> HIGH_GROUND_MAX_SIZE;
            case PLAIN, FOREST -> Army.MAX_SIZE;
            case OCEAN -> 0;
        };
    }

    private static int strength(int size, Terrain terrain) {
        return size + (terrain == Terrain.MOUNTAIN ? MOUNTAIN_DEFENCE_BONUS : 0);
    }

    private static int foodValue(Resource resource) {
        return switch (resource) {
            case WHEAT -> 5;
            case WOOD -> 1;
            case ROCK, SAND -> 0;
        };
    }

    private static int territoryBonus(Terrain terrain) {
        return switch (terrain) {
            case PLAIN -> 1;
            case FOREST -> 2;
            case MOUNTAIN, DESERT -> 4;
            case OCEAN -> 0;
        };
    }

    private Supplies supplies(Player player) {
        Supplies playerSupplies = supplies.get(player);
        if (playerSupplies == null) {
            throw new IllegalStateException(player + " is not part of this war game");
        }
        return playerSupplies;
    }

    private static final class Supplies {

        private int warriors;
        private int food;

        private Supplies(int warriors, int food) {
            this.warriors = warriors;
            this.food = food;
        }
    }
}
