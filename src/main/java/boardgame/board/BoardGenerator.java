package boardgame.board;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

public final class BoardGenerator {

    private static final int MIN_LAND = 2;
    private static final List<Terrain> LANDS = Terrain.lands();

    private final Random random;

    public BoardGenerator(Random random) {
        this.random = random;
    }

    public Board generate(int rows, int cols) {
        if (rows < 1 || cols < 1 || rows * cols / 3 < MIN_LAND) {
            throw new IllegalArgumentException("A board needs at least 6 tiles, got " + rows + "x" + cols);
        }
        int maxLand = rows * cols / 3;
        int landCount = MIN_LAND + random.nextInt(maxLand - MIN_LAND + 1);

        Terrain[][] grid = new Terrain[rows][cols];
        for (Terrain[] line : grid) {
            Arrays.fill(line, Terrain.OCEAN);
        }
        Board shape = new Board(grid);

        int placed = 0;
        while (placed < landCount) {
            if (landCount - placed >= 2) {
                Position first = pick(shape.positions().stream()
                        .filter(p -> isOcean(grid, p) && !oceanNeighbours(shape, grid, p).isEmpty())
                        .toList());
                Position second = pick(oceanNeighbours(shape, grid, first));
                setLand(grid, first);
                setLand(grid, second);
                placed += 2;
            } else {
                setLand(grid, pick(shape.positions().stream()
                        .filter(p -> isOcean(grid, p)
                                && shape.neighbours(p).stream().anyMatch(n -> !isOcean(grid, n)))
                        .toList()));
                placed++;
            }
        }
        return new Board(grid);
    }

    private List<Position> oceanNeighbours(Board shape, Terrain[][] grid, Position position) {
        return shape.neighbours(position).stream().filter(n -> isOcean(grid, n)).toList();
    }

    private static boolean isOcean(Terrain[][] grid, Position position) {
        return grid[position.row()][position.col()] == Terrain.OCEAN;
    }

    private void setLand(Terrain[][] grid, Position position) {
        grid[position.row()][position.col()] = LANDS.get(random.nextInt(LANDS.size()));
    }

    private Position pick(List<Position> candidates) {
        return candidates.get(random.nextInt(candidates.size()));
    }
}
