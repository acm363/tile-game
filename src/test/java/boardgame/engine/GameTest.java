package boardgame.engine;

import boardgame.agriculture.AgricultureRules;
import boardgame.board.Board;
import boardgame.board.BoardGenerator;
import boardgame.board.Boards;
import boardgame.board.Position;
import boardgame.player.Player;
import boardgame.unit.Worker;
import boardgame.ui.EventFormatter;
import boardgame.ui.Labels;
import boardgame.ui.Language;
import boardgame.war.WarRules;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameTest {

    private static final Decider ALWAYS_PASS = (player, actions) -> new Pass();
    private static final Decider DEPLOY_FIRST = (player, actions) -> actions.stream()
            .filter(Deploy.class::isInstance).findFirst().orElse(new Pass());

    private final Player alice = new Player("Alice");
    private final Player bob = new Player("Bob");
    private final List<GameEvent> events = new ArrayList<>();

    @Test
    void theGameStopsImmediatelyWhenNoTerritoryIsLeftToConquer() {
        // Given.
        Game game = new Game(new AgricultureRules(), Boards.parse("P~~P~~"), List.of(alice, bob), 6, DEPLOY_FIRST);
        game.addListener(events::add);

        // When.
        GameResult result = game.play();

        // Then.
        assertEquals(EndReason.NO_TERRITORY_LEFT, result.reason());
        assertEquals(1, result.roundsPlayed());
        assertEquals(14, alice.gold());
        assertEquals(15, bob.gold());
        assertTrue(bob.resources().isEmpty());
        assertEquals(new GameEnded(result), events.getLast());
    }

    @Test
    void theGameEndsAfterTheConfiguredNumberOfRounds() {
        // Given.
        Game game = new Game(new WarRules(), Boards.parse("PP~~~~"), List.of(alice, bob), 3, ALWAYS_PASS);
        game.addListener(events::add);

        // When.
        GameResult result = game.play();

        // Then.
        assertEquals(EndReason.ROUNDS_COMPLETED, result.reason());
        assertEquals(3, result.roundsPlayed());
        assertEquals(6, events.stream().filter(TurnStarted.class::isInstance).count());
        assertEquals(List.of(new RoundStarted(1), new RoundStarted(2), new RoundStarted(3)),
                events.stream().filter(RoundStarted.class::isInstance).toList());
    }

    @Test
    void playersAreScoredAndTiedPlayersAllWin() {
        // Given.
        Game game = new Game(new WarRules(), Boards.parse("PP~~~~"), List.of(alice, bob), 1, ALWAYS_PASS);

        // When.
        GameResult result = game.play();

        // Then.
        assertEquals(0, result.scores().get(alice));
        assertEquals(List.of(alice, bob), result.winners());
    }

    @Test
    void anActionOutsideTheLegalOnesIsRejected() {
        // Given.
        Decider cheater = (player, actions) -> new Deploy(new Position(0, 2), 1);
        Game game = new Game(new WarRules(), Boards.parse("PP~~~~"), List.of(alice, bob), 1, cheater);

        // Then.
        assertThrows(IllegalStateException.class, game::playTurn);
    }

    @Test
    void noTurnCanBePlayedOnceTheGameIsOver() {
        // Given.
        Game game = new Game(new WarRules(), Boards.parse("PP~~~~"), List.of(alice), 1, ALWAYS_PASS);

        // When.
        game.playTurn();

        // Then.
        assertTrue(game.isOver());
        assertThrows(IllegalStateException.class, game::playTurn);
    }

    @Test
    void playersTakeTheirTurnsInTheGivenOrderAndTheFirstPlayerRotatesEachRound() {
        // Given.
        Player carol = new Player("Carol");
        Game game = new Game(new WarRules(), Boards.parse("PP~~~~"), List.of(bob, carol, alice), 3, ALWAYS_PASS);
        game.addListener(events::add);

        // When.
        game.play();

        // Then.
        List<Player> turns = events.stream().filter(TurnStarted.class::isInstance)
                .map(event -> ((TurnStarted) event).player()).toList();
        assertEquals(List.of(bob, carol, alice, carol, alice, bob, alice, bob, carol), turns);
    }

    @Test
    void inASimultaneousGameEveryoneChoosesOnTheSameBoardBeforeTheOrdersAreRevealed() {
        // Given.
        List<Integer> freeTilesSeen = new ArrayList<>();
        Decider deployOnOwnSide = (player, actions) -> {
            freeTilesSeen.add((int) actions.stream().filter(Deploy.class::isInstance).count());
            return new Deploy(new Position(0, player == alice ? 0 : 2), 1);
        };
        Game game = new Game(new WarRules(), Boards.parse("PPP~~~"), List.of(alice, bob), 1, deployOnOwnSide);
        game.addListener(events::add);

        // When.
        game.playTurn();
        int armiesAfterTheFirstOrder = game.board().territoriesOf(alice).size();
        game.playTurn();

        // Then.
        assertEquals(List.of(15, 15), freeTilesSeen);
        assertEquals(0, armiesAfterTheFirstOrder);
        assertEquals(List.of(new RoundStarted(1), new TurnStarted(alice), new TurnStarted(bob), new OrdersRevealed(1)),
                events.subList(0, 4));
        assertEquals(1, game.board().territoriesOf(alice).size());
        assertEquals(1, game.board().territoriesOf(bob).size());
    }

    @Test
    void onlyThePlayerWhoseTurnItIsHarvests() {
        // Given.
        Board board = Boards.parse("PPP~~~~~~");
        board.place(new Position(0, 0), new Worker(alice));
        board.place(new Position(0, 1), new Worker(bob));
        Game game = new Game(new AgricultureRules(), board, List.of(alice, bob), 1, ALWAYS_PASS);

        // When.
        game.playTurn();

        // Then.
        assertEquals(1, alice.resources().size());
        assertTrue(bob.resources().isEmpty());
    }

    @Test
    void aGameNeedsPlayersAndRounds() {
        // Given.
        Board board = Boards.parse("PP~~~~");

        // Then.
        assertThrows(IllegalArgumentException.class, () -> new Game(new WarRules(), board, List.of(), 1, ALWAYS_PASS));
        assertThrows(IllegalArgumentException.class,
                () -> new Game(new WarRules(), board, List.of(alice), 0, ALWAYS_PASS));
    }

    @Test
    void theResultIsOnlyAvailableOnceTheGameIsOver() {
        // Given.
        Game game = new Game(new WarRules(), Boards.parse("PP~~~~"), List.of(alice, bob), 1, ALWAYS_PASS);

        // When.
        game.playTurn();
        Optional<GameResult> midGame = game.result();
        game.playTurn();

        // Then.
        assertTrue(midGame.isEmpty());
        assertTrue(game.result().isPresent());
    }

    @Test
    void theUpcomingTurnIsExposedBeforeItIsPlayed() {
        // Given.
        Game game = new Game(new WarRules(), Boards.parse("PP~~~~"), List.of(alice, bob), 1, ALWAYS_PASS);

        // When.
        game.playTurn();

        // Then.
        assertEquals(bob, game.currentPlayer());
        assertEquals(game.rules().legalActions(bob, game.board()), game.legalActions());
    }

    @Test
    void theUpcomingTurnIsUnavailableOnceTheGameIsOver() {
        // Given.
        Game game = new Game(new WarRules(), Boards.parse("PP~~~~"), List.of(alice), 1, ALWAYS_PASS);

        // When.
        game.play();

        // Then.
        assertThrows(IllegalStateException.class, game::currentPlayer);
        assertThrows(IllegalStateException.class, game::legalActions);
    }

    @Test
    void theSameSeedReplaysTheSameGame() {
        // When.
        List<String> first = replay(42);
        List<String> second = replay(42);
        List<String> other = replay(7);

        // Then.
        assertEquals(first, second);
        assertNotEquals(first, other);
    }

    private static List<String> replay(long seed) {
        Random random = new Random(seed);
        Board board = new BoardGenerator(random).generate(10, 10);
        Game game = new Game(new WarRules(), board, List.of(new Player("A"), new Player("B")), 10,
                new RandomDecider(random));
        EventFormatter formatter = new EventFormatter(new Labels(Language.FR));
        List<String> log = new ArrayList<>();
        game.addListener(event -> log.add(formatter.format(event)));
        game.play();
        return log;
    }
}
