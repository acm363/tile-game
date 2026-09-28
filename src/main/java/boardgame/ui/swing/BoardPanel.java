package boardgame.ui.swing;

import boardgame.board.Board;
import boardgame.board.Position;
import boardgame.board.Terrain;
import boardgame.ui.Labels;
import boardgame.unit.Army;
import boardgame.unit.Unit;

import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

final class BoardPanel extends JPanel {

    private static final int PREFERRED_CELL = 56;
    private static final Color GRID = new Color(0x14125C);
    private static final Map<Terrain, Color> TERRAIN_COLORS = new EnumMap<>(Map.of(
            Terrain.OCEAN, new Color(0x23209A),
            Terrain.PLAIN, new Color(0x33D445),
            Terrain.FOREST, new Color(0x19772A),
            Terrain.DESERT, new Color(0xE8E62F),
            Terrain.MOUNTAIN, new Color(0x976614)));

    private final Board board;
    private final PlayerColors colors;

    BoardPanel(Board board, PlayerColors colors) {
        this.board = board;
        this.colors = colors;
        setPreferredSize(new Dimension(board.cols() * PREFERRED_CELL, board.rows() * PREFERRED_CELL));
        setBackground(GRID);
        setToolTipText("");
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        int cell = cellSize();
        g.setFont(getFont().deriveFont(Font.BOLD, cell * 0.36f));
        for (Position position : board.positions()) {
            int x = originX(cell) + position.col() * cell;
            int y = originY(cell) + position.row() * cell;
            g.setColor(TERRAIN_COLORS.get(board.terrain(position)));
            g.fillRect(x, y, cell, cell);
            g.setColor(GRID);
            g.drawRect(x, y, cell, cell);
            board.occupant(position).ifPresent(unit -> paintUnit(g, unit, x, y, cell));
        }
        g.dispose();
    }

    private void paintUnit(Graphics2D g, Unit unit, int x, int y, int cell) {
        int inset = cell / 6;
        int diameter = cell - 2 * inset;
        Color fill = colors.of(unit.owner());
        g.setColor(fill);
        g.fillOval(x + inset, y + inset, diameter, diameter);
        g.setColor(Color.BLACK);
        g.setStroke(new BasicStroke(Math.max(1f, cell / 28f)));
        g.drawOval(x + inset, y + inset, diameter, diameter);
        if (unit instanceof Army army) {
            String label = String.valueOf(army.size());
            FontMetrics metrics = g.getFontMetrics();
            g.setColor(PlayerColors.readableOn(fill));
            g.drawString(label, x + (cell - metrics.stringWidth(label)) / 2,
                    y + (cell - metrics.getHeight()) / 2 + metrics.getAscent());
        }
    }

    @Override
    public String getToolTipText(MouseEvent event) {
        return positionAt(event.getX(), event.getY()).map(this::describe).orElse(null);
    }

    private String describe(Position position) {
        String tile = Labels.terrain(board.terrain(position)) + " " + Labels.position(position);
        return board.occupant(position)
                .map(unit -> tile + " — " + unit.owner() + (unit instanceof Army army ? ", " + army.size() + " guerrier(s)" : "")
                        + ", " + unit.gold() + " or")
                .orElse(tile);
    }

    private Optional<Position> positionAt(int px, int py) {
        int cell = cellSize();
        int col = Math.floorDiv(px - originX(cell), cell);
        int row = Math.floorDiv(py - originY(cell), cell);
        Position position = new Position(row, col);
        return board.contains(position) ? Optional.of(position) : Optional.empty();
    }

    private int cellSize() {
        return Math.max(1, Math.min(getWidth() / board.cols(), getHeight() / board.rows()));
    }

    private int originX(int cell) {
        return (getWidth() - cell * board.cols()) / 2;
    }

    private int originY(int cell) {
        return (getHeight() - cell * board.rows()) / 2;
    }
}
