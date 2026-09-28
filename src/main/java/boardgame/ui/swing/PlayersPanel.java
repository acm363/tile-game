package boardgame.ui.swing;

import boardgame.engine.Game;
import boardgame.player.Player;
import boardgame.ui.Labels;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

final class PlayersPanel extends JPanel {

    private final Game game;
    private final List<JLabel> summaries = new ArrayList<>();
    private Labels labels;

    PlayersPanel(Game game, PlayerColors colors, Labels labels) {
        super(new GridLayout(0, 1, 0, 6));
        this.game = game;
        this.labels = labels;
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        for (Player player : game.players()) {
            JLabel label = new JLabel();
            label.setIcon(new Swatch(colors.of(player)));
            label.setIconTextGap(8);
            summaries.add(label);
            add(label);
        }
        refresh();
    }

    void setLabels(Labels labels) {
        this.labels = labels;
        refresh();
    }

    void refresh() {
        for (int index = 0; index < summaries.size(); index++) {
            summaries.get(index).setText(summary(game.players().get(index)));
        }
    }

    private String summary(Player player) {
        String reserves = game.rules().reserves(player).entrySet().stream()
                .map(entry -> labels.text("reserve." + entry.getKey()) + " " + entry.getValue())
                .collect(Collectors.joining(" · "));
        String resources = player.resources().isEmpty()
                ? labels.text("player.noResource")
                : labels.resources(player.resources());
        return "<html><b>" + escape(player.name()) + "</b> — "
                + labels.text("player.score", game.rules().score(player, game.board())) + "<br>"
                + labels.text("player.gold", player.gold()) + (reserves.isEmpty() ? "" : " · " + reserves)
                + " · " + labels.text("player.territories", game.board().territoriesOf(player).size()) + "<br>"
                + resources + "</html>";
    }

    private static String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private record Swatch(Color color) implements Icon {

        private static final int SIZE = 16;

        @Override
        public void paintIcon(Component component, Graphics g, int x, int y) {
            g.setColor(color);
            g.fillOval(x, y, SIZE, SIZE);
            g.setColor(Color.BLACK);
            g.drawOval(x, y, SIZE, SIZE);
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
