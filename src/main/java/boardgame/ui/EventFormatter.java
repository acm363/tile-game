package boardgame.ui;

import boardgame.agriculture.ResourceSold;
import boardgame.agriculture.WagesPaid;
import boardgame.agriculture.WorkerDeployed;
import boardgame.agriculture.WorkerDismissed;
import boardgame.engine.GameEnded;
import boardgame.engine.GameEvent;
import boardgame.engine.GameResult;
import boardgame.engine.Harvested;
import boardgame.engine.Passed;
import boardgame.engine.RoundStarted;
import boardgame.engine.TurnStarted;
import boardgame.player.Player;
import boardgame.war.ArmyDeployed;
import boardgame.war.ArmyRallied;
import boardgame.war.ArmyReinforced;
import boardgame.war.ArmyStarved;
import boardgame.war.ArmyWeakened;
import boardgame.war.FoodProduced;

import java.util.stream.Collectors;

public final class EventFormatter {

    private static final String INDENT = "  ";

    private final Labels labels;

    public EventFormatter(Labels labels) {
        this.labels = labels;
    }

    public String format(GameEvent event) {
        return switch (event) {
            case RoundStarted e -> labels.text("event.roundStarted", e.round());
            case TurnStarted e -> labels.text("event.turnStarted", e.player());
            case Passed e -> e.goldEarned() > 0
                    ? indented("event.passedWithIncome", e.player(), e.goldEarned())
                    : indented("event.passed", e.player());
            case Harvested e -> indented("event.harvested", e.player(), labels.resources(e.resources()));
            case ArmyDeployed e -> indented("event.armyDeployed", e.player(), e.size(), labels.terrain(e.terrain()),
                    labels.position(e.position()));
            case ArmyWeakened e -> indented("event.armyWeakened", e.owner(), labels.position(e.position()), e.size());
            case ArmyRallied e -> indented("event.armyRallied", e.previousOwner(), labels.position(e.position()),
                    e.newOwner());
            case ArmyReinforced e -> indented("event.armyReinforced", labels.position(e.position()), e.size());
            case FoodProduced e -> indented("event.foodProduced", e.player(), e.food());
            case ArmyStarved e -> indented("event.armyStarved", e.size(), e.owner(), labels.position(e.position()));
            case WorkerDeployed e -> indented("event.workerDeployed", e.player(), labels.terrain(e.terrain()),
                    labels.position(e.position()));
            case ResourceSold e -> indented("event.resourceSold", e.player(), e.quantity(),
                    labels.resource(e.resource()), e.gold());
            case WagesPaid e -> indented("event.wagesPaid", e.player(), e.gold());
            case WorkerDismissed e -> indented("event.workerDismissed", e.owner(), e.wage(),
                    labels.position(e.position()));
            case GameEnded e -> result(e.result());
            default -> event.toString();
        };
    }

    private String indented(String key, Object... args) {
        return INDENT + labels.text(key, args);
    }

    private String result(GameResult result) {
        String reason = labels.text("result.reason." + result.reason().name());
        String scores = result.scores().entrySet().stream()
                .map(entry -> indented("result.score", entry.getKey(), entry.getValue()))
                .collect(Collectors.joining("\n"));
        String names = result.winners().stream().map(Player::name).collect(Collectors.joining(", "));
        String outcome = labels.text(result.winners().size() == 1 ? "result.winner" : "result.tie", names);
        return labels.text("result.header", result.roundsPlayed(), reason) + "\n" + scores + "\n" + outcome;
    }
}
