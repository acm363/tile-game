package boardgame.app;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.OptionalInt;
import java.util.OptionalLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LaunchOptionsTest {

    @Test
    void defaultsToATenByTenConsoleGameWithTheRulesOwnRoundCount() {
        LaunchOptions options = LaunchOptions.parse(new String[]{"guerre", "Alice", "Bob"});

        assertEquals(GameKind.WAR, options.kind());
        assertEquals(List.of("Alice", "Bob"), options.playerNames());
        assertEquals(10, options.rows());
        assertEquals(10, options.cols());
        assertEquals(OptionalInt.empty(), options.rounds());
        assertEquals(OptionalLong.empty(), options.seed());
        assertFalse(options.gui());
    }

    @Test
    void optionsCanBeMixedWithPlayerNames() {
        LaunchOptions options = LaunchOptions.parse(new String[]{
                "agricole", "Alice", "--rows", "4", "Bob", "--cols", "7", "--rounds", "3", "--seed", "-9", "--gui"});

        assertEquals(GameKind.AGRICULTURE, options.kind());
        assertEquals(List.of("Alice", "Bob"), options.playerNames());
        assertEquals(4, options.rows());
        assertEquals(7, options.cols());
        assertEquals(OptionalInt.of(3), options.rounds());
        assertEquals(OptionalLong.of(-9), options.seed());
        assertTrue(options.gui());
    }

    @Test
    void aGameMustBeNamed() {
        assertThrows(IllegalArgumentException.class, () -> LaunchOptions.parse(new String[]{}));
        assertThrows(IllegalArgumentException.class, () -> LaunchOptions.parse(new String[]{"chess", "Alice"}));
    }

    @Test
    void atLeastOnePlayerIsRequired() {
        assertThrows(IllegalArgumentException.class, () -> LaunchOptions.parse(new String[]{"guerre", "--gui"}));
    }

    @Test
    void unknownOptionsAreRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> LaunchOptions.parse(new String[]{"guerre", "Alice", "--speed", "2"}));
    }

    @Test
    void anOptionWithoutItsValueIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> LaunchOptions.parse(new String[]{"guerre", "Alice", "--rows"}));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-3", "ten"})
    void sizesAndRoundsMustBePositiveIntegers(String value) {
        assertThrows(IllegalArgumentException.class,
                () -> LaunchOptions.parse(new String[]{"guerre", "Alice", "--rows", value}));
        assertThrows(IllegalArgumentException.class,
                () -> LaunchOptions.parse(new String[]{"guerre", "Alice", "--rounds", value}));
    }
}
