package boardgame.app;

import boardgame.board.Board;
import boardgame.board.BoardGenerator;
import boardgame.engine.Decider;
import boardgame.engine.Game;
import boardgame.engine.GameRules;
import boardgame.engine.RandomDecider;
import boardgame.player.Player;
import boardgame.ui.HumanDecider;
import boardgame.ui.console.ConsoleLog;
import boardgame.ui.swing.GameWindow;

import javax.swing.SwingUtilities;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        LaunchOptions options;
        Random random;
        Board board;
        try {
            options = LaunchOptions.parse(args);
            random = options.seed().isPresent() ? new Random(options.seed().getAsLong()) : new Random();
            board = new BoardGenerator(random).generate(options.rows(), options.cols());
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
            System.err.println(LaunchOptions.USAGE);
            System.exit(2);
            return;
        }
        GameRules rules = options.kind().newRules();
        List<Player> players = options.playerNames().stream().map(Player::new).toList();
        HumanDecider human = new HumanDecider(players.stream()
                .filter(player -> options.humanNames().contains(player.name()))
                .collect(Collectors.toSet()));
        Decider bot = new RandomDecider(random);
        Decider decider = (player, actions) -> (human.controls(player) ? human : bot).choose(player, actions);
        Game game = new Game(rules, board, players, options.rounds().orElse(rules.defaultRounds()), decider);

        if (options.gui()) {
            SwingUtilities.invokeLater(() -> new GameWindow(options.kind().titleKey(), game, human).setVisible(true));
        } else {
            game.addListener(new ConsoleLog(System.out));
            game.play();
        }
    }
}
