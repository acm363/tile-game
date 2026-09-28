package boardgame.app;

import boardgame.board.Board;
import boardgame.board.BoardGenerator;
import boardgame.engine.Game;
import boardgame.engine.GameRules;
import boardgame.engine.RandomDecider;
import boardgame.player.Player;
import boardgame.ui.console.ConsoleLog;
import boardgame.ui.swing.GameWindow;

import javax.swing.SwingUtilities;
import java.util.List;
import java.util.Random;

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
        Game game = new Game(rules, board, players, options.rounds().orElse(rules.defaultRounds()),
                new RandomDecider(random));

        if (options.gui()) {
            SwingUtilities.invokeLater(() -> new GameWindow(options.kind().title(), game).setVisible(true));
        } else {
            game.addListener(new ConsoleLog(System.out));
            game.play();
        }
    }
}
