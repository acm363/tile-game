package boardgame.ui.swing;

import boardgame.board.Position;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

final class TileAnimations {

    static final int FLASH_FRAMES = 15;
    static final int CRUMBLE_FRAMES = 22;
    private static final int FADE_FRAMES = 6;
    private static final float FLASH_OPACITY = 0.9f;

    private final Random random;
    private final List<Animation> running = new ArrayList<>();

    TileAnimations(Random random) {
        this.random = random;
    }

    void flash(Position position, Color color) {
        running.add(new Flash(position, color));
    }

    void crumble(Position position, Color color) {
        running.add(new Crumble(position, new FallingSand(color, random)));
    }

    boolean isRunning() {
        return !running.isEmpty();
    }

    void tick() {
        running.removeIf(animation -> !animation.tick());
    }

    void paint(Graphics2D g, int originX, int originY, int cell) {
        for (Animation animation : running) {
            Position position = animation.position();
            animation.paint(g, originX + position.col() * cell, originY + position.row() * cell, cell);
        }
    }

    private static Color withOpacity(Color color, float opacity) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), Math.round(255 * opacity));
    }

    private interface Animation {

        Position position();

        boolean tick();

        void paint(Graphics2D g, int x, int y, int cell);
    }

    private static final class Flash implements Animation {

        private final Position position;
        private final Color color;
        private int remaining = FLASH_FRAMES;

        private Flash(Position position, Color color) {
            this.position = position;
            this.color = color;
        }

        @Override
        public Position position() {
            return position;
        }

        @Override
        public boolean tick() {
            return --remaining > 0;
        }

        @Override
        public void paint(Graphics2D g, int x, int y, int cell) {
            int width = Math.max(3, cell / 8);
            g.setColor(withOpacity(color, FLASH_OPACITY * remaining / FLASH_FRAMES));
            g.setStroke(new BasicStroke(width));
            g.drawRect(x + width / 2, y + width / 2, cell - width, cell - width);
        }
    }

    private static final class Crumble implements Animation {

        private final Position position;
        private final FallingSand sand;
        private int remaining = CRUMBLE_FRAMES;

        private Crumble(Position position, FallingSand sand) {
            this.position = position;
            this.sand = sand;
        }

        @Override
        public Position position() {
            return position;
        }

        @Override
        public boolean tick() {
            sand.step();
            return --remaining > 0;
        }

        @Override
        public void paint(Graphics2D g, int x, int y, int cell) {
            float opacity = Math.min(1f, (float) remaining / FADE_FRAMES);
            for (FallingSand.Grain grain : sand.grains()) {
                int left = x + grain.x() * cell / FallingSand.GRID;
                int top = y + grain.y() * cell / FallingSand.GRID;
                int right = x + (grain.x() + 1) * cell / FallingSand.GRID;
                int bottom = y + (grain.y() + 1) * cell / FallingSand.GRID;
                g.setColor(withOpacity(grain.color(), opacity));
                g.fillRect(left, top, right - left, bottom - top);
            }
        }
    }
}
