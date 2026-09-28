package boardgame.ui.swing;

import boardgame.board.Terrain;
import boardgame.ui.Labels;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.util.EnumMap;
import java.util.Map;

final class TerrainLegend extends JPanel {

    private final Map<Terrain, JLabel> entries = new EnumMap<>(Terrain.class);

    TerrainLegend(Labels labels) {
        super(new FlowLayout(FlowLayout.RIGHT, 12, 4));
        for (Terrain terrain : Terrain.values()) {
            JLabel entry = new JLabel("", new Swatch(TerrainColors.of(terrain)), JLabel.LEFT);
            entry.setIconTextGap(6);
            entries.put(terrain, entry);
            add(entry);
        }
        setLabels(labels);
    }

    void setLabels(Labels labels) {
        entries.forEach((terrain, entry) -> entry.setText(label(labels, terrain)));
    }

    private static String label(Labels labels, Terrain terrain) {
        String name = labels.terrain(terrain);
        return terrain.isLand() ? name + " (" + labels.resource(terrain.resource()) + ")" : name;
    }

    record Swatch(Color color) implements Icon {

        private static final int SIZE = 14;

        @Override
        public void paintIcon(Component component, Graphics g, int x, int y) {
            g.setColor(color);
            g.fillRect(x, y, SIZE, SIZE);
            g.setColor(Color.DARK_GRAY);
            g.drawRect(x, y, SIZE, SIZE);
        }

        @Override
        public int getIconWidth() {
            return SIZE + 1;
        }

        @Override
        public int getIconHeight() {
            return SIZE + 1;
        }
    }
}
