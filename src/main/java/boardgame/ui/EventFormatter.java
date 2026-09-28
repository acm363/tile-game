package boardgame.ui;

import boardgame.agriculture.ResourceSold;
import boardgame.agriculture.WagesPaid;
import boardgame.agriculture.WorkerDeployed;
import boardgame.agriculture.WorkerDismissed;
import boardgame.engine.EndReason;
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

import static boardgame.ui.Labels.position;
import static boardgame.ui.Labels.terrain;

public final class EventFormatter {

    public String format(GameEvent event) {
        return switch (event) {
            case RoundStarted e -> "── Tour " + e.round() + " ──";
            case TurnStarted e -> e.player() + " joue";
            case Passed e -> e.goldEarned() > 0
                    ? "  " + e.player() + " ne fait rien et reçoit " + e.goldEarned() + " or"
                    : "  " + e.player() + " ne fait rien";
            case Harvested e -> "  " + e.player() + " récolte " + Labels.resources(e.resources());
            case ArmyDeployed e -> "  " + e.player() + " déploie une armée de " + e.size() + " sur une tuile "
                    + terrain(e.terrain()) + " " + position(e.position());
            case ArmyWeakened e -> "  l'armée de " + e.owner() + " en " + position(e.position())
                    + " est réduite à " + e.size();
            case ArmyRallied e -> "  l'armée de " + e.previousOwner() + " en " + position(e.position())
                    + " se rallie à " + e.newOwner();
            case ArmyReinforced e -> "  l'armée alliée en " + position(e.position()) + " compte " + e.size()
                    + " guerrier(s)";
            case FoodProduced e -> "  " + e.player() + " convertit ses récoltes en " + e.food() + " nourriture";
            case ArmyStarved e -> "  l'armée de " + e.size() + " de " + e.owner() + " en " + position(e.position())
                    + " meurt de faim (+1 or)";
            case WorkerDeployed e -> "  " + e.player() + " déploie un ouvrier sur une tuile " + terrain(e.terrain())
                    + " " + position(e.position());
            case ResourceSold e -> "  " + e.player() + " vend " + e.quantity() + " " + Labels.resource(e.resource())
                    + " pour " + e.gold() + " or";
            case WagesPaid e -> "  " + e.player() + " paie " + e.gold() + " or à ses ouvriers";
            case WorkerDismissed e -> "  " + e.owner() + " ne peut pas payer " + e.wage() + " or : l'ouvrier en "
                    + position(e.position()) + " quitte le jeu";
            case GameEnded e -> result(e.result());
            default -> event.toString();
        };
    }

    private String result(GameResult result) {
        String reason = result.reason() == EndReason.NO_TERRITORY_LEFT
                ? "plus de territoire à conquérir"
                : "tous les tours ont été joués";
        String scores = result.scores().entrySet().stream()
                .map(entry -> "  " + entry.getKey() + " : " + entry.getValue() + " point(s)")
                .collect(Collectors.joining("\n"));
        String names = result.winners().stream().map(Player::name).collect(Collectors.joining(", "));
        String outcome = result.winners().size() == 1 ? "Vainqueur : " + names : "Égalité entre " + names;
        return "══ Fin de partie au tour " + result.roundsPlayed() + " (" + reason + ") ══\n" + scores + "\n" + outcome;
    }
}
