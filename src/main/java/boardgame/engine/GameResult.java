package boardgame.engine;

import boardgame.player.Player;

import java.util.List;
import java.util.Map;

public record GameResult(Map<Player, Integer> scores, List<Player> winners, EndReason reason, int roundsPlayed) {
}
