package boardgame.war;

import boardgame.board.Position;
import boardgame.engine.GameEvent;
import boardgame.player.Player;

public record ArmyRallied(Player previousOwner, Player newOwner, Position position) implements GameEvent {
}
