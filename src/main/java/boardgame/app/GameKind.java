package boardgame.app;

import boardgame.agriculture.AgricultureRules;
import boardgame.engine.GameRules;
import boardgame.war.WarRules;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.Supplier;

enum GameKind {
    WAR("guerre", "title.war", WarRules::new),
    AGRICULTURE("agricole", "title.agriculture", AgricultureRules::new);

    private final String command;
    private final String titleKey;
    private final Supplier<GameRules> rules;

    GameKind(String command, String titleKey, Supplier<GameRules> rules) {
        this.command = command;
        this.titleKey = titleKey;
        this.rules = rules;
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
}
