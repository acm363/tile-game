package boardgame.agriculture;

import boardgame.board.Position;
import boardgame.engine.GameEvent;
import boardgame.player.Player;

public record WorkerDismissed(Player owner, Position position, int wage) implements GameEvent {
}
