package boardgame.app;

import boardgame.agriculture.AgricultureRules;
import boardgame.engine.GameRules;
import boardgame.war.WarRules;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.Supplier;

enum GameKind {
    WAR("guerre", "Jeu de guerre", WarRules::new),
    AGRICULTURE("agricole", "Jeu agricole", AgricultureRules::new);

    private final String command;
    private final String title;
    private final Supplier<GameRules> rules;

    GameKind(String command, String title, Supplier<GameRules> rules) {
        this.command = command;
        this.title = title;
        this.rules = rules;
    }

    static Optional<GameKind> fromCommand(String command) {
        return Arrays.stream(values()).filter(kind -> kind.command.equals(command)).findFirst();
    }

    String command() {
        return command;
    }

    String title() {
        return title;
    }

    GameRules newRules() {
        return rules.get();
    }
}
