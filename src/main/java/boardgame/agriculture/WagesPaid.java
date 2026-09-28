package boardgame.agriculture;

import boardgame.engine.GameEvent;
import boardgame.player.Player;

public record WagesPaid(Player player, int gold) implements GameEvent {
}
