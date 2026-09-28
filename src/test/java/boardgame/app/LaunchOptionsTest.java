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
        // Given.
        String[] args = {"guerre", "Alice", "Bob"};

        // When.
        LaunchOptions options = LaunchOptions.parse(args);

        // Then.
        assertEquals(GameKind.WAR, options.kind());
        assertEquals(List.of("Alice", "Bob"), options.playerNames());
        assertEquals(10, options.rows());
        assertEquals(10, options.cols());
        assertEquals(OptionalInt.empty(), options.rounds());
        assertEquals(OptionalLong.empty(), options.seed());
        assertFalse(options.gui());
        assertTrue(options.humanNames().isEmpty());
    }

    @Test
    void optionsCanBeMixedWithPlayerNames() {
        // Given.
        String[] args = {"agricole", "Alice", "--rows", "4", "Bob", "--cols", "7", "--rounds", "3", "--seed", "-9", "--gui"};

        // When.
        LaunchOptions options = LaunchOptions.parse(args);

        // Then.
        assertEquals(GameKind.AGRICULTURE, options.kind());
        assertEquals(List.of("Alice", "Bob"), options.playerNames());
        assertEquals(4, options.rows());
        assertEquals(7, options.cols());
        assertEquals(OptionalInt.of(3), options.rounds());
        assertEquals(OptionalLong.of(-9), options.seed());
        assertTrue(options.gui());
    }

    @Test
    void humanPlayersAreNamedWithTheHumanOption() {
        // Given.
        String[] args = {"guerre", "Alice", "Bob", "--gui", "--human", "Bob"};

        // When.
        LaunchOptions options = LaunchOptions.parse(args);

        // Then.
        assertEquals(List.of("Bob"), options.humanNames());
    }

    @Test
    void aHumanMustBeOneOfThePlayers() {
        // Then.
        assertThrows(IllegalArgumentException.class,
                () -> LaunchOptions.parse(new String[]{"guerre", "Alice", "--gui", "--human", "Carol"}));
    }

    @Test
    void humansCanOnlyPlayInTheWindow() {
        // Then.
        assertThrows(IllegalArgumentException.class,
                () -> LaunchOptions.parse(new String[]{"guerre", "Alice", "--human", "Alice"}));
    }

    @Test
    void aGameMustBeNamed() {
        // Then.
        assertThrows(IllegalArgumentException.class, () -> LaunchOptions.parse(new String[]{}));
        assertThrows(IllegalArgumentException.class, () -> LaunchOptions.parse(new String[]{"chess", "Alice"}));
    }

    @Test
    void atLeastOnePlayerIsRequired() {
        // Then.
        assertThrows(IllegalArgumentException.class, () -> LaunchOptions.parse(new String[]{"guerre", "--gui"}));
    }

    @Test
    void unknownOptionsAreRejected() {
        // Then.
        assertThrows(IllegalArgumentException.class,
                () -> LaunchOptions.parse(new String[]{"guerre", "Alice", "--speed", "2"}));
    }

    @Test
    void anOptionWithoutItsValueIsRejected() {
        // Then.
        assertThrows(IllegalArgumentException.class, () -> LaunchOptions.parse(new String[]{"guerre", "Alice", "--rows"}));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-3", "ten"})
    void sizesAndRoundsMustBePositiveIntegers(String value) {
        // Then.
        assertThrows(IllegalArgumentException.class,
                () -> LaunchOptions.parse(new String[]{"guerre", "Alice", "--rows", value}));
        assertThrows(IllegalArgumentException.class,
                () -> LaunchOptions.parse(new String[]{"guerre", "Alice", "--rounds", value}));
    }
}
