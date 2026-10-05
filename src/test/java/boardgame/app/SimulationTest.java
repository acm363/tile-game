package boardgame.app;

import boardgame.ui.Labels;
import boardgame.ui.Language;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SimulationTest {

    private final Simulation simulation = new Simulation(GameKind.WAR, List.of("A", "B"), 10, 10, 10);

    @Test
    void everySeedIsPlayedFromEverySeat() {
        // When.
        Simulation.Report report = simulation.run(1, 3);

        // Then.
        assertEquals(6, report.games());
        assertEquals(2, report.seatWins().size());
        assertEquals(6, report.seatWins().stream().mapToInt(Integer::intValue).sum() + report.ties());
    }

    @Test
    void theSameSeedsGiveTheSameReport() {
        // Then.
        assertEquals(simulation.run(5, 4), simulation.run(5, 4));
    }

    @Test
    void everyKillIsCountedOnceByTheAttackersTerrain() {
        // When.
        Simulation.Report report = simulation.run(1, 5);

        // Then.
        assertTrue(report.attacks() > 0);
        assertEquals(report.attacks(), report.killsByTerrain().values().stream().mapToInt(Integer::intValue).sum());
    }

    @Test
    void eachSeatScoresBetween45And55PercentCountingADrawAsHalfAWin() {
        // When.
        Simulation.Report report = simulation.run(1, 1000);

        // Then.
        for (int seat = 0; seat < report.seatWins().size(); seat++) {
            double score = report.seatScore(seat);
            assertTrue(score >= 45 && score <= 55, "seat " + (seat + 1) + " scores " + score + "%");
        }
    }

    @Test
    void swappedSeatsGiveBothSeatsTheSameNumberOfWins() {
        // When.
        Simulation.Report report = simulation.run(1, 200);

        // Then.
        assertEquals(report.seatWins().get(0), report.seatWins().get(1));
    }

    @Test
    void theReportGivesEachSeatsWinRate() {
        // When.
        List<String> lines = simulation.run(1, 2).describe(new Labels(Language.EN));

        // Then.
        assertTrue(lines.getFirst().startsWith("4 games"));
        assertTrue(lines.stream().anyMatch(line -> line.startsWith("seat 1: ")));
        assertTrue(lines.stream().anyMatch(line -> line.startsWith("seat 2: ")));
    }
}
