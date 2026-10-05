package boardgame.war;

import boardgame.board.Position;
import boardgame.engine.GameEvent;
import boardgame.player.Player;

public record ArmyPromoted(Player owner, Position position, int level) implements GameEvent {
}
