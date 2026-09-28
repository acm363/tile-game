package boardgame.war;

import boardgame.board.Position;
import boardgame.engine.GameEvent;
import boardgame.player.Player;

public record ArmyStarved(Player owner, Position position, int size) implements GameEvent {
}
