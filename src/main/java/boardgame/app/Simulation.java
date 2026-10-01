package boardgame.app;

import boardgame.board.Board;
import boardgame.board.BoardGenerator;
import boardgame.board.Terrain;
import boardgame.engine.EndReason;
import boardgame.engine.Game;
import boardgame.engine.GameEvent;
import boardgame.engine.GameResult;
import boardgame.engine.GameRules;
import boardgame.engine.TurnStarted;
import boardgame.player.Player;
import boardgame.ui.Labels;
import boardgame.war.ArmyDestroyed;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

final class Simulation {

    private final GameKind kind;
    private final List<String> names;
    private final int rows;
    private final int cols;
    private final int rounds;

    Simulation(GameKind kind, List<String> names, int rows, int cols, int rounds) {
        this.kind = kind;
        this.names = List.copyOf(names);
        this.rows = rows;
        this.cols = cols;
        this.rounds = rounds;
    }

    Report run(long firstSeed, int seeds) {
        Tally tally = new Tally(names.size());
        for (long seed = firstSeed; seed < firstSeed + seeds; seed++) {
            for (int shift = 0; shift < names.size(); shift++) {
                play(seed, shift, tally);
            }
        }
        return tally.report(seeds);
    }

    private void play(long seed, int shift, Tally tally) {
        List<Player> seats = new ArrayList<>(names.stream().map(Player::new).toList());
        Collections.rotate(seats, shift);
        Board board = new BoardGenerator(new Random(seed)).generate(rows, cols);
        GameRules rules = kind.newRules();
        Game game = new Game(rules, board, seats, rounds, kind.newBot(rules, board, rounds, new Random(seed)));
        game.addListener(event -> tally.record(event, board));
        tally.record(game.play(), seats, board);
    }

    private static final class Tally {

        private final int[] seatWins;
        private final Map<EndReason, Integer> endReasons = new EnumMap<>(EndReason.class);
        private final Map<Terrain, Integer> killsByTerrain = new EnumMap<>(Terrain.class);
        private int ties;
        private int roundsPlayed;
        private int landTiles;
        private int attacks;
        private int doubleTurnAttacks;
        private Player lastTurn;
        private boolean doubleTurn;

        Tally(int seats) {
            seatWins = new int[seats];
        }

        void record(GameEvent event, Board board) {
            switch (event) {
                case TurnStarted started -> {
                    doubleTurn = started.player() == lastTurn;
                    lastTurn = started.player();
                }
                case ArmyDestroyed destroyed -> {
                    attacks++;
                    killsByTerrain.merge(board.terrain(destroyed.from()), 1, Integer::sum);
                    if (doubleTurn) {
                        doubleTurnAttacks++;
                    }
                }
                default -> {
                }
            }
        }

        void record(GameResult result, List<Player> seats, Board board) {
            if (result.winners().size() == 1) {
                seatWins[seats.indexOf(result.winners().getFirst())]++;
            } else {
                ties++;
            }
            roundsPlayed += result.roundsPlayed();
            endReasons.merge(result.reason(), 1, Integer::sum);
            landTiles += (int) board.positions().stream().filter(position -> board.terrain(position).isLand()).count();
            lastTurn = null;
        }

        Report report(int seeds) {
            return new Report(seeds, Arrays.stream(seatWins).boxed().toList(), ties, roundsPlayed, landTiles,
                    Collections.unmodifiableMap(new EnumMap<>(endReasons)),
                    Collections.unmodifiableMap(new EnumMap<>(killsByTerrain)), attacks, doubleTurnAttacks);
        }
    }

    record Report(int seeds, List<Integer> seatWins, int ties, int roundsPlayed, int landTiles,
                  Map<EndReason, Integer> endReasons, Map<Terrain, Integer> killsByTerrain, int attacks,
                  int doubleTurnAttacks) {

        int games() {
            return seeds * seatWins.size();
        }

        List<String> describe(Labels labels) {
            List<String> lines = new ArrayList<>();
            lines.add(labels.text("simulation.header", games(), seeds, seatWins.size()));
            for (int seat = 0; seat < seatWins.size(); seat++) {
                lines.add(labels.text("simulation.seat", seat + 1, seatWins.get(seat), percent(seatWins.get(seat), games())));
            }
            lines.add(labels.text("simulation.ties", ties, percent(ties, games())));
            lines.add(labels.text("simulation.rounds", (double) roundsPlayed / games()));
            lines.add(labels.text("simulation.land", (double) landTiles / games()));
            endReasons.forEach((reason, count) -> lines.add(
                    labels.text("simulation.endReason", labels.text("result.reason." + reason.name()), count)));
            if (attacks > 0) {
                lines.add(labels.text("simulation.kills", killsByTerrain.entrySet().stream()
                        .map(entry -> labels.text("simulation.share", labels.terrain(entry.getKey()),
                                percent(entry.getValue(), attacks)))
                        .collect(Collectors.joining(", "))));
                lines.add(labels.text("simulation.doubleTurn", doubleTurnAttacks, attacks));
            }
            return lines;
        }

        private static double percent(int count, int total) {
            return 100.0 * count / total;
        }
    }
}
