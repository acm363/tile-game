package boardgame.ui.swing;

import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FallingSandTest {

    private static final int MAX_STEPS = 2 * FallingSand.GRID;

    @Test
    void grainsAreNeverLostNorStackedOnTheSameCell() {
        // Given.
        FallingSand sand = new FallingSand(Color.RED, new Random(1));
        int count = sand.grains().size();

        for (int step = 0; step < MAX_STEPS; step++) {
            // When.
            sand.step();

            // Then.
            List<FallingSand.Grain> grains = sand.grains();
            assertEquals(count, grains.size());
            assertEquals(count, cells(grains).size());
            assertTrue(grains.stream().allMatch(grain -> grain.x() >= 0 && grain.x() < FallingSand.GRID
                    && grain.y() >= 0 && grain.y() < FallingSand.GRID));
        }
    }

    @Test
    void theSandSettlesIntoAPileWhereEveryGrainIsSupported() {
        // Given.
        FallingSand sand = new FallingSand(Color.RED, new Random(2));

        // When.
        int steps = 0;
        while (sand.step()) {
            steps++;
        }

        // Then.
        assertTrue(steps < MAX_STEPS, "still falling after " + steps + " steps");
        Set<List<Integer>> cells = cells(sand.grains());
        for (FallingSand.Grain grain : sand.grains()) {
            int below = grain.y() + 1;
            assertTrue(below == FallingSand.GRID || isBlocked(cells, grain.x(), below), "grain floats at " + cells);
        }
        assertFalse(sand.step());
    }

    private static boolean isBlocked(Set<List<Integer>> cells, int x, int y) {
        return (x - 1 < 0 || cells.contains(List.of(x - 1, y))) && cells.contains(List.of(x, y))
                && (x + 1 >= FallingSand.GRID || cells.contains(List.of(x + 1, y)));
    }

    private static Set<List<Integer>> cells(List<FallingSand.Grain> grains) {
        Set<List<Integer>> cells = new HashSet<>();
        grains.forEach(grain -> cells.add(List.of(grain.x(), grain.y())));
        return cells;
    }
}
