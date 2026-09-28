package boardgame.war;

import boardgame.board.Board;
import boardgame.board.Position;
import boardgame.board.Resource;
import boardgame.board.Terrain;
import boardgame.engine.Action;
import boardgame.engine.Deploy;
import boardgame.engine.GameContext;
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
        Army army = new Army(player, size);
        board.place(position, army);
        supplies(player).warriors -= size;
        context.emit(new ArmyDeployed(player, position, board.terrain(position), size));

        for (Position neighbour : board.neighbours(position)) {
            if (board.occupant(neighbour).orElse(null) instanceof Army other) {
                confront(army, position, other, neighbour, context);
            }
        }
    }

    private void confront(Army deployed, Position deployedAt, Army other, Position otherAt, GameContext context) {
        Board board = context.board();
        if (other.owner() != deployed.owner()) {
            if (strengthAgainstEnemy(other, board.terrain(otherAt)) < strengthAgainstEnemy(deployed, board.terrain(deployedAt))) {
                int halved = other.size() / 2;
                if (halved < Army.MIN_SIZE) {
                    Player previousOwner = other.owner();
                    other.changeOwner(deployed.owner());
                    deployed.addGold(CAPTURE_REWARD);
                    context.emit(new ArmyRallied(previousOwner, deployed.owner(), otherAt));
                } else {
                    other.setSize(halved);
                    context.emit(new ArmyWeakened(other.owner(), otherAt, halved));
                }
            }
        } else if (other.size() < deployed.size()) {
            if (other.size() < maxSize(board.terrain(otherAt))) {
                other.setSize(other.size() + 1);
            }
            deployed.addGold(REINFORCEMENT_REWARD);
            context.emit(new ArmyReinforced(other.owner(), otherAt, other.size()));
        }
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
        reserves.put("guerriers", warriors(player));
        reserves.put("nourriture", food(player));
        return reserves;
    }

    static int maxSize(Terrain terrain) {
        return switch (terrain) {
            case MOUNTAIN, DESERT -> HIGH_GROUND_MAX_SIZE;
            case PLAIN, FOREST -> Army.MAX_SIZE;
            case OCEAN -> 0;
        };
    }

    private static int strengthAgainstEnemy(Army army, Terrain terrain) {
        return army.size() + (terrain == Terrain.MOUNTAIN ? MOUNTAIN_DEFENCE_BONUS : 0);
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
