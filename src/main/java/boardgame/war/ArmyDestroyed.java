package boardgame.war;

import boardgame.board.Position;
import boardgame.engine.GameEvent;
import boardgame.player.Player;

public record ArmyDestroyed(Player attacker, Position from, Player defender, Position target, int size)
        implements GameEvent {
}
