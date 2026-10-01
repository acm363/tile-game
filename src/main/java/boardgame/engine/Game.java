package boardgame.engine;

import boardgame.board.Board;
import boardgame.player.Player;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class Game {

    private final GameRules rules;
    private final GameContext context;
    private final Decider decider;
    private final int rounds;
    private int round = 1;
    private int turnIndex;
    private GameResult result;

    public Game(GameRules rules, Board board, List<Player> players, int rounds, Decider decider) {
        if (players.isEmpty()) {
            throw new IllegalArgumentException("A game needs at least one player");
        }
        if (rounds < 1) {
            throw new IllegalArgumentException("A game needs at least one round: " + rounds);
        }
        this.rules = rules;
        this.context = new GameContext(board, players);
        this.decider = decider;
        this.rounds = rounds;
        players.forEach(rules::setUp);
    }

    public Board board() {
        return context.board();
    }

    public List<Player> players() {
        return context.players();
    }

    public GameRules rules() {
        return rules;
    }

    public void addListener(GameListener listener) {
        context.addListener(listener);
    }

    public boolean isOver() {
        return result != null;
    }

    public Optional<GameResult> result() {
        return Optional.ofNullable(result);
    }

    public Player currentPlayer() {
        requireNotOver();
        return context.players().get((round - 1 + turnIndex) % context.players().size());
    }

    public List<Action> legalActions() {
        return rules.legalActions(currentPlayer(), board());
    }

    public GameResult play() {
        while (!isOver()) {
            playTurn();
        }
        return result;
    }

    public void playTurn() {
        Player player = currentPlayer();
        if (turnIndex == 0) {
            context.emit(new RoundStarted(round));
        }
        context.emit(new TurnStarted(player));

        List<Action> legalActions = legalActions();
        Action action = decider.choose(player, legalActions);
        if (!legalActions.contains(action)) {
            throw new IllegalStateException(player + " chose an illegal action: " + action);
        }
        rules.apply(action, player, context);
        if (board().freePositions().isEmpty()) {
            end(EndReason.NO_TERRITORY_LEFT);
            return;
        }
        rules.harvest(player, context);
        rules.upkeep(player, context);

        turnIndex++;
        if (turnIndex == context.players().size()) {
            turnIndex = 0;
            if (round == rounds) {
                end(EndReason.ROUNDS_COMPLETED);
            } else {
                round++;
            }
        }
    }

    private void requireNotOver() {
        if (isOver()) {
            throw new IllegalStateException("The game is over");
        }
    }

    private void end(EndReason reason) {
        Map<Player, Integer> scores = new LinkedHashMap<>();
        context.players().forEach(player -> scores.put(player, rules.score(player, board())));
        int best = Collections.max(scores.values());
        List<Player> winners = scores.entrySet().stream()
                .filter(entry -> entry.getValue() == best)
                .map(Map.Entry::getKey)
                .toList();
        result = new GameResult(Collections.unmodifiableMap(scores), winners, reason, round);
        context.emit(new GameEnded(result));
    }
}
