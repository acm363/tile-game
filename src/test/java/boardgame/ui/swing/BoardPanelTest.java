package boardgame.ui.swing;

import boardgame.board.Board;
import boardgame.board.Boards;
import boardgame.board.Position;
import boardgame.engine.Deploy;
import boardgame.engine.Passed;
import boardgame.player.Player;
import boardgame.ui.Labels;
import boardgame.ui.Language;
import boardgame.unit.Army;
import boardgame.war.ArmyStarved;
import boardgame.war.ArmyWeakened;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BoardPanelTest {

    private static final int CELL = 50;

    private final Player alice = new Player("Alice");
    private final Board board = Boards.parse("~P", "D~");
    private BoardPanel panel;

    @BeforeEach
    void setUp() {
        panel = new BoardPanel(board, new PlayerColors(List.of(alice)), new Labels(Language.FR));
        panel.setSize(2 * CELL, 2 * CELL);
    }

    @Test
    void tilesArePaintedWithTheirTerrainColourAndUnitsWithTheirOwnersColour() {
        // Given.
        board.place(new Position(0, 1), new Army(alice, 2));

        // When.
        BufferedImage image = render();

        // Then.
        assertEquals(new Color(0x23209A).getRGB(), image.getRGB(10, 10));
        assertEquals(new Color(0xE8E62F).getRGB(), image.getRGB(10, CELL + 10));
        assertEquals(new Color(0x33D445).getRGB(), image.getRGB(CELL + 3, 3));
        assertEquals(new Color(0xE63946).getRGB(), image.getRGB(CELL + 15, CELL / 2));
    }

    @Test
    void theTooltipDescribesTheHoveredTileAndItsOccupant() {
        // Given.
        board.place(new Position(0, 1), new Army(alice, 2));

        // When.
        String occupied = tooltipAt(CELL + 20, 20);
        String free = tooltipAt(20, CELL + 20);

        // Then.
        assertEquals("plaine (0, 1) — Alice, 2 guerrier(s), 0 or", occupied);
        assertEquals("désert (1, 0)", free);
    }

    @Test
    void theTooltipFollowsTheSelectedLanguage() {
        // Given.
        board.place(new Position(0, 1), new Army(alice, 2));

        // When.
        panel.setLabels(new Labels(Language.EN));

        // Then.
        assertEquals("plain (0, 1) — Alice, 2 warrior(s), 0 gold", tooltipAt(CELL + 20, 20));
    }

    @Test
    void highlightedTilesAreOutlined() {
        // Given.
        panel.setHighlighted(Set.of(new Position(1, 0)));

        // When.
        BufferedImage image = render();

        // Then.
        assertEquals(Color.WHITE.getRGB(), image.getRGB(CELL / 2, CELL + 4));
        assertEquals(new Color(0xE8E62F).getRGB(), image.getRGB(CELL / 2, CELL + CELL / 2));
    }

    @Test
    void clickingATileReportsItsPosition() {
        // Given.
        List<Position> clicked = new ArrayList<>();
        panel.setOnTileClicked(clicked::add);

        // When.
        panel.dispatchEvent(new MouseEvent(panel, MouseEvent.MOUSE_CLICKED, 0, 0, CELL + 20, CELL + 20, 1, false));

        // Then.
        assertEquals(List.of(new Position(1, 1)), clicked);
    }

    @Test
    void thePreviewShowsTheArmyToDeployAndMarksTheAffectedNeighbours() {
        // Given.
        Player bob = new Player("Bob");
        board.place(new Position(0, 1), new Army(bob, 2));

        // When.
        panel.setPreview(new Deploy(new Position(1, 0), 3), alice,
                List.of(new ArmyWeakened(bob, new Position(0, 1), 1)));
        BufferedImage image = render();

        // Then.
        assertEquals(BoardPanel.WEAKENED.getRGB(), image.getRGB(CELL + 2, CELL / 2));
        assertNotEquals(new Color(0xE8E62F).getRGB(), image.getRGB(15, CELL + CELL / 2));
    }

    @Test
    void clearingThePreviewRestoresTheBoard() {
        // Given.
        panel.setPreview(new Deploy(new Position(1, 0), 3), alice, List.of());

        // When.
        panel.clearPreview();
        BufferedImage image = render();

        // Then.
        assertEquals(new Color(0xE8E62F).getRGB(), image.getRGB(15, CELL + CELL / 2));
    }

    @Test
    void hoveringReportsEachNewTileAndLeavingTheBoard() {
        // Given.
        List<Optional<Position>> hovered = new ArrayList<>();
        panel.setOnTileHovered(hovered::add);

        // When.
        panel.dispatchEvent(new MouseEvent(panel, MouseEvent.MOUSE_MOVED, 0, 0, 10, 10, 0, false));
        panel.dispatchEvent(new MouseEvent(panel, MouseEvent.MOUSE_MOVED, 0, 0, 12, 12, 0, false));
        panel.dispatchEvent(new MouseEvent(panel, MouseEvent.MOUSE_EXITED, 0, 0, -1, -1, 0, false));

        // Then.
        assertEquals(List.of(Optional.of(new Position(0, 0)), Optional.empty()), hovered);
    }

    @Test
    void aWeakenedArmyFlashesThenTheTileReturnsToNormal() {
        // Given.
        int plain = new Color(0x33D445).getRGB();

        // When.
        panel.animate(new ArmyWeakened(alice, new Position(0, 1), 1));
        int flashing = render().getRGB(CELL + 3, 3);
        for (int frame = 0; frame < TileAnimations.FLASH_FRAMES; frame++) {
            panel.tickAnimations();
        }

        // Then.
        assertNotEquals(plain, flashing);
        assertFalse(panel.isAnimating());
        assertEquals(plain, render().getRGB(CELL + 3, 3));
    }

    @Test
    void aStarvedArmyCrumblesToTheBottomOfItsTile() {
        // When.
        panel.animate(new ArmyStarved(alice, new Position(0, 1), 3));
        for (int frame = 0; frame < TileAnimations.CRUMBLE_FRAMES / 2; frame++) {
            panel.tickAnimations();
        }
        BufferedImage midway = render();

        // Then.
        assertTrue(panel.isAnimating());
        assertEquals(new Color(0x33D445).getRGB(), midway.getRGB(CELL + CELL / 2, CELL / 4));
        assertNotEquals(new Color(0x33D445).getRGB(), midway.getRGB(CELL + CELL / 2, CELL - 2));
    }

    @Test
    void eventsWithoutAVisualEffectDoNotAnimate() {
        // When.
        panel.animate(new Passed(alice, 0));

        // Then.
        assertFalse(panel.isAnimating());
    }

    @Test
    void thereIsNoTooltipOutsideTheBoard() {
        // Given.
        panel.setSize(4 * CELL, 2 * CELL);

        // When.
        String tooltip = tooltipAt(5, 5);

        // Then.
        assertNull(tooltip);
    }

    private BufferedImage render() {
        BufferedImage image = new BufferedImage(panel.getWidth(), panel.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        panel.paint(graphics);
        graphics.dispose();
        return image;
    }

    private String tooltipAt(int x, int y) {
        return panel.getToolTipText(new MouseEvent(panel, MouseEvent.MOUSE_MOVED, 0, 0, x, y, 0, false));
    }
}
