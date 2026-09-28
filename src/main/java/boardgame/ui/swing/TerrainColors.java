package boardgame.ui.swing;

import boardgame.board.Terrain;

import java.awt.Color;
import java.util.EnumMap;
import java.util.Map;

final class TerrainColors {

    private static final Map<Terrain, Color> COLORS = new EnumMap<>(Map.of(
            Terrain.OCEAN, new Color(0x23209A),
            Terrain.PLAIN, new Color(0x33D445),
            Terrain.FOREST, new Color(0x19772A),
            Terrain.DESERT, new Color(0xE8E62F),
            Terrain.MOUNTAIN, new Color(0x976614)));

    private TerrainColors() {
    }

    static Color of(Terrain terrain) {
        return COLORS.get(terrain);
    }
}
