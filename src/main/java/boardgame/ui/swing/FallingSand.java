package boardgame.ui.swing;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

final class FallingSand {

    static final int GRID = 24;
    private static final int MAX_SPEED = 3;
    private static final int SHADE_RANGE = 40;

    private final Random random;
    private final List<Grain> grains = new ArrayList<>();
    private final boolean[][] occupied = new boolean[GRID][GRID];

    FallingSand(Color color, Random random) {
        this.random = random;
        double centre = (GRID - 1) / 2.0;
        double radius = GRID / 3.0;
        for (int y = 0; y < GRID; y++) {
            for (int x = 0; x < GRID; x++) {
                if (Math.hypot(x - centre, y - centre) <= radius) {
                    grains.add(new Grain(x, y, 1 + random.nextInt(MAX_SPEED), shade(color)));
                    occupied[y][x] = true;
                }
            }
        }
    }

    boolean step() {
        boolean moved = false;
        grains.sort(Comparator.comparingInt((Grain grain) -> grain.y).reversed());
        for (Grain grain : grains) {
            for (int fall = 0; fall < grain.speed; fall++) {
                if (!fall(grain)) {
                    break;
                }
                moved = true;
            }
        }
        return moved;
    }

    List<Grain> grains() {
        return List.copyOf(grains);
    }

    private boolean fall(Grain grain) {
        int below = grain.y + 1;
        if (below >= GRID) {
            return false;
        }
        int side = random.nextBoolean() ? 1 : -1;
        for (int dx : new int[]{0, side, -side}) {
            int x = grain.x + dx;
            if (x >= 0 && x < GRID && !occupied[below][x]) {
                occupied[grain.y][grain.x] = false;
                grain.x = x;
                grain.y = below;
                occupied[below][x] = true;
                return true;
            }
        }
        return false;
    }

    private Color shade(Color color) {
        int delta = random.nextInt(2 * SHADE_RANGE + 1) - SHADE_RANGE;
        return new Color(clamp(color.getRed() + delta), clamp(color.getGreen() + delta), clamp(color.getBlue() + delta));
    }

    private static int clamp(int channel) {
        return Math.max(0, Math.min(255, channel));
    }

    static final class Grain {

        private int x;
        private int y;
        private final int speed;
        private final Color color;

        private Grain(int x, int y, int speed, Color color) {
            this.x = x;
            this.y = y;
            this.speed = speed;
            this.color = color;
        }

        int x() {
            return x;
        }

        int y() {
            return y;
        }

        Color color() {
            return color;
        }
    }
}
