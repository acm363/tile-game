package boardgame.ui.swing;

import boardgame.board.Board;
import boardgame.board.Position;
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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

final class BoardPanel extends JPanel {

    private static final int PREFERRED_CELL = 56;
    private static final Color GRID = new Color(0x14125C);
    private static final Color HIGHLIGHT = Color.WHITE;

    private final Board board;
    private final PlayerColors colors;
    private Labels labels;
    private Set<Position> highlighted = Set.of();
    private Consumer<Position> onTileClicked = position -> {
    };

    BoardPanel(Board board, PlayerColors colors, Labels labels) {
        this.board = board;
        this.colors = colors;
        this.labels = labels;
        setPreferredSize(new Dimension(board.cols() * PREFERRED_CELL, board.rows() * PREFERRED_CELL));
        setBackground(GRID);
        setToolTipText("");
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                positionAt(event.getX(), event.getY()).ifPresent(onTileClicked);
            }
        });
    }

    void setHighlighted(Set<Position> positions) {
        highlighted = Set.copyOf(positions);
        repaint();
    }

    void setOnTileClicked(Consumer<Position> listener) {
        onTileClicked = listener;
    }

    void setLabels(Labels labels) {
        this.labels = labels;
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
            g.setColor(TerrainColors.of(board.terrain(position)));
            g.fillRect(x, y, cell, cell);
            g.setColor(GRID);
            g.drawRect(x, y, cell, cell);
            board.occupant(position).ifPresent(unit -> paintUnit(g, unit, x, y, cell));
            if (highlighted.contains(position)) {
                paintHighlight(g, x, y, cell);
            }
        }
        g.dispose();
    }

    private void paintHighlight(Graphics2D g, int x, int y, int cell) {
        int width = Math.max(2, cell / 14);
        g.setColor(HIGHLIGHT);
        g.setStroke(new BasicStroke(width));
        g.drawRect(x + width, y + width, cell - 2 * width, cell - 2 * width);
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
        String tile = labels.terrain(board.terrain(position)) + " " + labels.position(position);
        return board.occupant(position)
                .map(unit -> unit instanceof Army army
                        ? labels.text("tooltip.army", tile, unit.owner(), army.size(), unit.gold())
                        : labels.text("tooltip.unit", tile, unit.owner(), unit.gold()))
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
