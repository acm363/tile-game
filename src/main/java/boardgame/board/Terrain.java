package boardgame.board;

import java.util.Arrays;
import java.util.List;

public enum Terrain {
    OCEAN(null),
    MOUNTAIN(Resource.ROCK),
    PLAIN(Resource.WHEAT),
    DESERT(Resource.SAND),
    FOREST(Resource.WOOD);

    private final Resource resource;

    Terrain(Resource resource) {
        this.resource = resource;
    }

    public static List<Terrain> lands() {
        return Arrays.stream(values()).filter(Terrain::isLand).toList();
    }

    public boolean isLand() {
        return resource != null;
    }

    public Resource resource() {
        if (resource == null) {
            throw new IllegalStateException(this + " produces no resource");
        }
        return resource;
    }
}
