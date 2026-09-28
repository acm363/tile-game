package boardgame.board;

public final class Boards {

    private Boards() {
    }

    public static Board parse(String... rows) {
        Terrain[][] grid = new Terrain[rows.length][];
        for (int row = 0; row < rows.length; row++) {
            grid[row] = rows[row].chars().mapToObj(Boards::terrain).toArray(Terrain[]::new);
        }
        return new Board(grid);
    }

    private static Terrain terrain(int symbol) {
        return switch (symbol) {
            case '~' -> Terrain.OCEAN;
            case 'M' -> Terrain.MOUNTAIN;
            case 'P' -> Terrain.PLAIN;
            case 'D' -> Terrain.DESERT;
            case 'F' -> Terrain.FOREST;
            default -> throw new IllegalArgumentException("Unknown terrain symbol: " + (char) symbol);
        };
    }
}
