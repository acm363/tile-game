package boardgame.app;

import boardgame.agriculture.AgricultureRules;
import boardgame.board.Board;
import boardgame.engine.Decider;
import boardgame.engine.GameRules;
import boardgame.engine.RandomDecider;
import boardgame.war.WarBot;
import boardgame.war.WarRules;

import java.util.Arrays;
import java.util.Optional;
import java.util.Random;
import java.util.function.Supplier;

enum GameKind {
    WAR("guerre", "title.war", WarRules::new,
            (rules, board, rounds, random) -> new WarBot((WarRules) rules, board, rounds, random)),
    AGRICULTURE("agricole", "title.agriculture", AgricultureRules::new,
            (rules, board, rounds, random) -> new RandomDecider(random));

    private final String command;
    private final String titleKey;
    private final Supplier<GameRules> rules;
    private final BotFactory bot;

    GameKind(String command, String titleKey, Supplier<GameRules> rules, BotFactory bot) {
        this.command = command;
        this.titleKey = titleKey;
        this.rules = rules;
        this.bot = bot;
    }

    static Optional<GameKind> fromCommand(String command) {
        return Arrays.stream(values()).filter(kind -> kind.command.equals(command)).findFirst();
    }

    String command() {
        return command;
    }

    String titleKey() {
        return titleKey;
    }

    GameRules newRules() {
        return rules.get();
    }

    Decider newBot(GameRules rules, Board board, int rounds, Random random) {
        return bot.create(rules, board, rounds, random);
    }

    @FunctionalInterface
    private interface BotFactory {

        Decider create(GameRules rules, Board board, int rounds, Random random);
    }
}
