package boardgame.ui.swing;

import boardgame.board.Terrain;
import org.junit.jupiter.api.Test;

import javax.swing.JLabel;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TerrainLegendTest {

    @Test
    void everyTerrainIsListedWithItsResourceAndBoardColour() {
        // When.
        List<JLabel> entries = Arrays.stream(new TerrainLegend().getComponents()).map(JLabel.class::cast).toList();

        // Then.
        assertEquals(List.of("océan", "montagne (roche)", "plaine (blé)", "désert (sable)", "forêt (bois)"),
                entries.stream().map(JLabel::getText).toList());
        assertEquals(Arrays.stream(Terrain.values()).map(TerrainColors::of).toList(),
                entries.stream().map(entry -> ((TerrainLegend.Swatch) entry.getIcon()).color()).toList());
    }
}
