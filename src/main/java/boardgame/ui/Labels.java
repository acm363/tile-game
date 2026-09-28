package boardgame.ui;

import boardgame.board.Position;
import boardgame.board.Resource;
import boardgame.board.Terrain;

import java.util.Map;
import java.util.stream.Collectors;

public final class Labels {

    private Labels() {
    }

    public static String terrain(Terrain terrain) {
        return switch (terrain) {
            case OCEAN -> "océan";
            case MOUNTAIN -> "montagne";
            case PLAIN -> "plaine";
            case DESERT -> "désert";
            case FOREST -> "forêt";
        };
    }

    public static String resource(Resource resource) {
        return switch (resource) {
            case ROCK -> "roche";
            case WHEAT -> "blé";
            case SAND -> "sable";
            case WOOD -> "bois";
        };
    }

    public static String position(Position position) {
        return "(" + position.row() + ", " + position.col() + ")";
    }

    public static String resources(Map<Resource, Integer> resources) {
        return resources.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> entry.getValue() + " " + resource(entry.getKey()))
                .collect(Collectors.joining(", "));
    }
}
