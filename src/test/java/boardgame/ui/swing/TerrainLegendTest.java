package boardgame.ui.swing;

import boardgame.board.Terrain;
import boardgame.ui.Labels;
import boardgame.ui.Language;
import org.junit.jupiter.api.Test;

import javax.swing.JLabel;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TerrainLegendTest {

    @Test
    void everyTerrainIsListedWithItsResourceAndBoardColour() {
        // When.
        List<JLabel> entries = entries(new TerrainLegend(new Labels(Language.FR)));

        // Then.
        assertEquals(List.of("océan", "montagne (roche)", "plaine (blé)", "désert (sable)", "forêt (bois)"),
                entries.stream().map(JLabel::getText).toList());
        assertEquals(Arrays.stream(Terrain.values()).map(TerrainColors::of).toList(),
                entries.stream().map(entry -> ((TerrainLegend.Swatch) entry.getIcon()).color()).toList());
    }

    @Test
    void switchingLanguageRelabelsEveryEntry() {
        // Given.
        TerrainLegend legend = new TerrainLegend(new Labels(Language.FR));

        // When.
        legend.setLabels(new Labels(Language.EN));

        // Then.
        assertEquals(List.of("ocean", "mountain (rock)", "plain (wheat)", "desert (sand)", "forest (wood)"),
                entries(legend).stream().map(JLabel::getText).toList());
    }

    private static List<JLabel> entries(TerrainLegend legend) {
        return Arrays.stream(legend.getComponents()).map(JLabel.class::cast).toList();
    }
}
