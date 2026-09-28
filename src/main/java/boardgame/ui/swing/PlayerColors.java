package boardgame.ui.swing;

import boardgame.player.Player;

import java.awt.Color;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class PlayerColors {

    private static final List<Color> PALETTE = List.of(
            new Color(0xE63946), new Color(0xFFFFFF), new Color(0x9B5DE5), new Color(0xF77F00),
            new Color(0xF15BB5), new Color(0x00BBF9), new Color(0x222222), new Color(0x8D99AE));

    private final Map<Player, Color> colors = new HashMap<>();

    PlayerColors(List<Player> players) {
        for (int index = 0; index < players.size(); index++) {
            colors.put(players.get(index), PALETTE.get(index % PALETTE.size()));
        }
    }

    Color of(Player player) {
        return colors.getOrDefault(player, Color.GRAY);
    }

    static Color readableOn(Color background) {
        double luminance = 0.299 * background.getRed() + 0.587 * background.getGreen() + 0.114 * background.getBlue();
        return luminance > 150 ? Color.BLACK : Color.WHITE;
    }
}
