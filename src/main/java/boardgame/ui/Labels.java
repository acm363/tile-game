package boardgame.ui;

import boardgame.board.Position;
import boardgame.board.Resource;
import boardgame.board.Terrain;
import boardgame.engine.Action;
import boardgame.engine.Exchange;

import java.util.Map;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public final class Labels {

    private static final String BUNDLE = "boardgame.ui.messages";

    private final Language language;
    private final ResourceBundle bundle;

    public Labels(Language language) {
        this.language = language;
        this.bundle = bundle(language);
    }

    static ResourceBundle bundle(Language language) {
        return ResourceBundle.getBundle(BUNDLE, language.locale(),
                ResourceBundle.Control.getNoFallbackControl(ResourceBundle.Control.FORMAT_PROPERTIES));
    }

    public Language language() {
        return language;
    }

    public String text(String key, Object... args) {
        return String.format(language.locale(), bundle.getString(key), args);
    }

    public String terrain(Terrain terrain) {
        return text("terrain." + terrain.name());
    }

    public String resource(Resource resource) {
        return text("resource." + resource.name());
    }

    public String position(Position position) {
        return "(" + position.row() + ", " + position.col() + ")";
    }

    public String action(Action action) {
        return action instanceof Exchange exchange
                ? text("action.exchange", exchange.quantity(), resource(exchange.resource()))
                : action.toString();
    }

    public String resources(Map<Resource, Integer> resources) {
        return resources.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> entry.getValue() + " " + resource(entry.getKey()))
                .collect(Collectors.joining(", "));
    }
}
