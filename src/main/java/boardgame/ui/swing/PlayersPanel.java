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
    private final List<JLabel> labels = new ArrayList<>();

    PlayersPanel(Game game, PlayerColors colors) {
        super(new GridLayout(0, 1, 0, 6));
        this.game = game;
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        for (Player player : game.players()) {
            JLabel label = new JLabel();
            label.setIcon(new Swatch(colors.of(player)));
            label.setIconTextGap(8);
            labels.add(label);
            add(label);
        }
        refresh();
    }

    void refresh() {
        for (int index = 0; index < labels.size(); index++) {
            labels.get(index).setText(summary(game.players().get(index)));
        }
    }

    private String summary(Player player) {
        String reserves = game.rules().reserves(player).entrySet().stream()
                .map(entry -> entry.getKey() + " " + entry.getValue())
                .collect(Collectors.joining(" · "));
        String resources = player.resources().isEmpty() ? "aucune ressource" : Labels.resources(player.resources());
        return "<html><b>" + escape(player.name()) + "</b> — " + game.rules().score(player, game.board()) + " pts<br>"
                + "or " + player.gold() + (reserves.isEmpty() ? "" : " · " + reserves)
                + " · " + game.board().territoriesOf(player).size() + " territoire(s)<br>"
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
