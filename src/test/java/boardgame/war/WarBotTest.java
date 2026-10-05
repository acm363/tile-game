package boardgame.war;

import boardgame.board.Board;
import boardgame.board.Boards;
import boardgame.board.Position;
import boardgame.engine.Action;
import boardgame.engine.Attack;
import boardgame.engine.Deploy;
import boardgame.engine.Pass;
import boardgame.player.Player;
import boardgame.unit.Army;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WarBotTest {

    private final Player alice = new Player("Alice");
    private final Player bob = new Player("Bob");
    private final WarRules rules = new WarRules();

    private Action choice(Board board, int rounds) {
        rules.setUp(alice);
        rules.setUp(bob);
        WarBot bot = new WarBot(rules, board, rounds, new Random(1));
        return bot.choose(alice, rules.legalActions(alice, board));
    }

    @Test
    void attacksTheStrongestEnemyItCanBeat() {
        // Given.
        Board board = Boards.parse("PPP");
        board.place(new Position(0, 1), new Army(alice, 5));
        board.place(new Position(0, 0), new Army(bob, 2));
        board.place(new Position(0, 2), new Army(bob, 4));

        // When.
        Action action = choice(board, 10);

        // Then.
        assertEquals(new Attack(new Position(0, 1), new Position(0, 2)), action);
    }

    @Test
    void deploysWhereNoEnemyCanBeatTheNewArmy() {
        // Given.
        Board board = Boards.parse("MPPPP");
        board.place(new Position(0, 0), new Army(bob, 5));

        // When.
        Action action = choice(board, 10);

        // Then.
        assertEquals(new Position(0, 4), ((Deploy) action).position());
    }

    @Test
    void prefersATileFromWhichItCanBeatAnEnemy() {
        // Given.
        Board board = Boards.parse("PPP~P");
        board.place(new Position(0, 0), new Army(bob, 1));

        // When.
        Action action = choice(board, 10);

        // Then.
        assertEquals(new Position(0, 1), ((Deploy) action).position());
    }

    @Test
    void prefersTheLongestRangeWhenNothingElseDiffers() {
        // When.
        Action action = choice(Boards.parse("PDMF"), 10);

        // Then.
        assertEquals(new Position(0, 2), ((Deploy) action).position());
    }

    @Test
    void spreadsItsReserveOverTheTurnsLeft() {
        // When.
        Action action = choice(Boards.parse("PPPP"), 10);

        // Then.
        assertEquals(4, ((Deploy) action).size());
    }

    @Test
    void deploysJustEnoughSoldiersToBeSafe() {
        // Given.
        Board board = Boards.parse("PP");
        board.place(new Position(0, 0), new Army(bob, 3));

        // When.
        Action action = choice(board, 35);

        // Then.
        assertEquals(new Deploy(new Position(0, 1), 3), action);
    }

    @Test
    void passesWhenNothingElseIsLegal() {
        // Given.
        Board board = Boards.parse("PP");
        board.place(new Position(0, 0), new Army(alice, 1));
        board.place(new Position(0, 1), new Army(bob, 5));

        // When.
        Action action = choice(board, 10);

        // Then.
        assertEquals(new Pass(), action);
    }

    @Test
    void theSameSeedMakesTheSameChoices() {
        // Given.
        Board board = Boards.parse("PPPP", "PPPP");
        rules.setUp(alice);
        List<Action> legal = rules.legalActions(alice, board);

        // When.
        Action first = new WarBot(rules, board, 10, new Random(7)).choose(alice, legal);
        Action second = new WarBot(rules, board, 10, new Random(7)).choose(alice, legal);

        // Then.
        assertEquals(first, second);
    }
}
