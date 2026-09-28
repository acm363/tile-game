package boardgame.ui.console;

import boardgame.engine.GameEvent;
import boardgame.engine.GameListener;
import boardgame.ui.EventFormatter;
import boardgame.ui.Labels;
import boardgame.ui.Language;

import java.io.PrintStream;

public final class ConsoleLog implements GameListener {

    private final PrintStream out;
    private final EventFormatter formatter = new EventFormatter(new Labels(Language.FR));

    public ConsoleLog(PrintStream out) {
        this.out = out;
    }

    @Override
    public void onEvent(GameEvent event) {
        out.println(formatter.format(event));
    }
}
