package boardgame.war;

import boardgame.board.Board;
import boardgame.board.BoardGenerator;
import boardgame.engine.Decider;
import boardgame.engine.Game;
import boardgame.engine.RandomDecider;
import boardgame.player.Player;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SeatFairnessTest {

    private static final int ROUNDS = 10;

    private Map<String, Integer> scores(long seed, String first, String second) {
        Board board = new BoardGenerator(new Random(seed)).generate(10, 10);
        WarRules rules = new WarRules();
        Map<String, Decider> deciders = Map.of(
                "greedy", new WarBot(rules, board, ROUNDS, new Random(seed)),
                "other greedy", new WarBot(rules, board, ROUNDS, new Random(seed + 1)),
                "random", new RandomDecider(new Random(seed)));
        List<Player> seats = List.of(new Player(first), new Player(second));
        Game game = new Game(rules, board, seats, ROUNDS, (player, actions) -> deciders.get(player.name())
                .choose(player, actions));
        return game.play().scores().entrySet().stream()
                .collect(Collectors.toMap(entry -> entry.getKey().name(), Map.Entry::getValue));
    }

    @Test
    void swappingSeatsBetweenTwoBotsNeverChangesTheOutcome() {
        // Then.
        for (long seed = 1; seed <= 50; seed++) {
            assertEquals(scores(seed, "greedy", "other greedy"), scores(seed, "other greedy", "greedy"), "seed " + seed);
        }
    }

    @Test
    void swappingSeatsBetweenABotAndAnotherKindOfPlayerNeverChangesTheOutcome() {
        // Then.
        for (long seed = 1; seed <= 50; seed++) {
            assertEquals(scores(seed, "greedy", "random"), scores(seed, "random", "greedy"), "seed " + seed);
        }
    }
}
