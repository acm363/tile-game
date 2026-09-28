package boardgame.ui;

import boardgame.board.Resource;
import boardgame.board.Terrain;
import boardgame.engine.EndReason;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.Arrays;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LabelsTest {

    @Test
    void everyLanguageDefinesTheSameMessages() {
        // When.
        Set<String> french = Labels.bundle(Language.FR).keySet();
        Set<String> english = Labels.bundle(Language.EN).keySet();

        // Then.
        assertEquals(french, english);
    }

    @ParameterizedTest
    @EnumSource(Language.class)
    void everyEnumValueShownToPlayersIsTranslated(Language language) {
        // Given.
        Set<String> keys = Labels.bundle(language).keySet();

        // Then.
        Arrays.stream(Terrain.values()).forEach(terrain -> assertTrue(keys.contains("terrain." + terrain.name())));
        Arrays.stream(Resource.values()).forEach(resource -> assertTrue(keys.contains("resource." + resource.name())));
        Arrays.stream(EndReason.values()).forEach(reason -> assertTrue(keys.contains("result.reason." + reason.name())));
    }
}
