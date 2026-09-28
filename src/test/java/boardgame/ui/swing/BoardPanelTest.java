package boardgame.ui.swing;

import boardgame.board.Board;
import boardgame.board.Boards;
import boardgame.board.Position;
import boardgame.player.Player;
import boardgame.unit.Army;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class BoardPanelTest {

    private static final int CELL = 50;

    private final Player alice = new Player("Alice");
    private final Board board = Boards.parse("~P", "D~");
    private BoardPanel panel;

    @BeforeEach
    void setUp() {
        panel = new BoardPanel(board, new PlayerColors(List.of(alice)));
        panel.setSize(2 * CELL, 2 * CELL);
    }

    @Test
    void tilesArePaintedWithTheirTerrainColourAndUnitsWithTheirOwnersColour() {
        board.place(new Position(0, 1), new Army(alice, 2));

        BufferedImage image = render();

        assertEquals(new Color(0x23209A).getRGB(), image.getRGB(10, 10));
        assertEquals(new Color(0xE8E62F).getRGB(), image.getRGB(10, CELL + 10));
        assertEquals(new Color(0x33D445).getRGB(), image.getRGB(CELL + 3, 3));
        assertEquals(new Color(0xE63946).getRGB(), image.getRGB(CELL + 15, CELL / 2));
    }

    @Test
    void theTooltipDescribesTheHoveredTileAndItsOccupant() {
        board.place(new Position(0, 1), new Army(alice, 2));

        assertEquals("plaine (0, 1) — Alice, 2 guerrier(s), 0 or", tooltipAt(CELL + 20, 20));
        assertEquals("désert (1, 0)", tooltipAt(20, CELL + 20));
    }

    @Test
    void thereIsNoTooltipOutsideTheBoard() {
        panel.setSize(4 * CELL, 2 * CELL);

        assertNull(tooltipAt(5, 5));
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
