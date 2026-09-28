package boardgame.ui.swing;

import boardgame.board.Board;
import boardgame.board.Position;
import boardgame.engine.Deploy;
import boardgame.engine.GameEvent;
import boardgame.player.Player;
import boardgame.ui.Labels;
import boardgame.unit.Army;
import boardgame.unit.Unit;
import boardgame.war.ArmyRallied;
import boardgame.war.ArmyReinforced;
import boardgame.war.ArmyWeakened;

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
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

final class BoardPanel extends JPanel {

    private static final int PREFERRED_CELL = 56;
    private static final Color GRID = new Color(0x14125C);
    private static final Color HIGHLIGHT = Color.WHITE;
    static final Color WEAKENED = new Color(0xFF4D4D);
    static final Color RALLIED = new Color(0xFFD700);
    static final Color REINFORCED = new Color(0x7CFC00);
    private static final int GHOST_ALPHA = 90;

    private final Board board;
    private final PlayerColors colors;
    private Labels labels;
    private Set<Position> highlighted = Set.of();
    private Consumer<Position> onTileClicked = position -> {
    };
    private Consumer<Optional<Position>> onTileHovered = position -> {
    };
    private Optional<Position> hovered = Optional.empty();
    private Deploy previewedDeploy;
    private Player previewedOwner;
    private List<GameEvent> previewedEffects = List.of();

    BoardPanel(Board board, PlayerColors colors, Labels labels) {
        this.board = board;
        this.colors = colors;
        this.labels = labels;
        setPreferredSize(new Dimension(board.cols() * PREFERRED_CELL, board.rows() * PREFERRED_CELL));
        setBackground(GRID);
        setToolTipText("");
        MouseAdapter mouse = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                positionAt(event.getX(), event.getY()).ifPresent(onTileClicked);
            }

            @Override
            public void mouseMoved(MouseEvent event) {
                hover(positionAt(event.getX(), event.getY()));
            }

            @Override
            public void mouseExited(MouseEvent event) {
                hover(Optional.empty());
            }
        };
        addMouseListener(mouse);
        addMouseMotionListener(mouse);
    }

    private void hover(Optional<Position> position) {
        if (!Objects.equals(hovered, position)) {
            hovered = position;
            onTileHovered.accept(position);
        }
    }

    void setOnTileHovered(Consumer<Optional<Position>> listener) {
        onTileHovered = listener;
    }

    void setPreview(Deploy deploy, Player owner, List<GameEvent> effects) {
        previewedDeploy = deploy;
        previewedOwner = owner;
        previewedEffects = List.copyOf(effects);
        repaint();
    }

    void clearPreview() {
        if (previewedDeploy != null) {
            previewedDeploy = null;
            previewedOwner = null;
            previewedEffects = List.of();
            repaint();
        }
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
            int x = tileX(position, cell);
            int y = tileY(position, cell);
            g.setColor(TerrainColors.of(board.terrain(position)));
            g.fillRect(x, y, cell, cell);
            g.setColor(GRID);
            g.drawRect(x, y, cell, cell);
            board.occupant(position).ifPresent(unit -> paintUnit(g, unit, x, y, cell));
            if (highlighted.contains(position)) {
                paintHighlight(g, x, y, cell);
            }
        }
        paintPreview(g, cell);
        g.dispose();
    }

    private void paintPreview(Graphics2D g, int cell) {
        if (previewedDeploy == null) {
            return;
        }
        Position target = previewedDeploy.position();
        paintGhost(g, colors.of(previewedOwner), previewedDeploy.size(), tileX(target, cell), tileY(target, cell), cell);
        for (GameEvent effect : previewedEffects) {
            switch (effect) {
                case ArmyWeakened e -> paintEffect(g, e.position(), WEAKENED, "→" + e.size(), cell);
                case ArmyRallied e -> paintEffect(g, e.position(), RALLIED, "★", cell);
                case ArmyReinforced e -> paintEffect(g, e.position(), REINFORCED, "→" + e.size(), cell);
                default -> {
                }
            }
        }
    }

    private void paintGhost(Graphics2D g, Color owner, int size, int x, int y, int cell) {
        int inset = cell / 6;
        int diameter = cell - 2 * inset;
        g.setColor(new Color(owner.getRed(), owner.getGreen(), owner.getBlue(), GHOST_ALPHA));
        g.fillOval(x + inset, y + inset, diameter, diameter);
        float width = Math.max(2f, cell / 16f);
        g.setColor(owner);
        g.setStroke(new BasicStroke(width, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f,
                new float[]{width * 2, width * 1.5f}, 0f));
        g.drawOval(x + inset, y + inset, diameter, diameter);
        String label = String.valueOf(size);
        FontMetrics metrics = g.getFontMetrics();
        g.setColor(Color.WHITE);
        g.drawString(label, x + (cell - metrics.stringWidth(label)) / 2,
                y + (cell - metrics.getHeight()) / 2 + metrics.getAscent());
    }

    private void paintEffect(Graphics2D g, Position position, Color color, String badge, int cell) {
        int x = tileX(position, cell);
        int y = tileY(position, cell);
        int width = Math.max(3, cell / 10);
        g.setColor(color);
        g.setStroke(new BasicStroke(width));
        g.drawRect(x + width / 2, y + width / 2, cell - width, cell - width);
        Font font = g.getFont();
        g.setFont(font.deriveFont(cell * 0.26f));
        FontMetrics metrics = g.getFontMetrics();
        int badgeWidth = metrics.stringWidth(badge) + 6;
        int badgeHeight = metrics.getHeight();
        int badgeX = x + cell - badgeWidth - width;
        int badgeY = y + width;
        g.setColor(Color.BLACK);
        g.fillRoundRect(badgeX, badgeY, badgeWidth, badgeHeight, 6, 6);
        g.setColor(color);
        g.drawString(badge, badgeX + 3, badgeY + metrics.getAscent());
        g.setFont(font);
    }

    private void paintHighlight(Graphics2D g, int x, int y, int cell) {
        int width = Math.max(2, cell / 14);
        g.setColor(HIGHLIGHT);
        g.setStroke(new BasicStroke(width));
        g.drawRect(x + width, y + width, cell - 2 * width, cell - 2 * width);
    }

    private void paintUnit(Graphics2D g, Unit unit, int x, int y, int cell) {
        String label = unit instanceof Army army ? String.valueOf(army.size()) : "";
        paintDisc(g, colors.of(unit.owner()), label, x, y, cell);
    }

    private void paintDisc(Graphics2D g, Color fill, String label, int x, int y, int cell) {
        int inset = cell / 6;
        int diameter = cell - 2 * inset;
        g.setColor(fill);
        g.fillOval(x + inset, y + inset, diameter, diameter);
        g.setColor(Color.BLACK);
        g.setStroke(new BasicStroke(Math.max(1f, cell / 28f)));
        g.drawOval(x + inset, y + inset, diameter, diameter);
        FontMetrics metrics = g.getFontMetrics();
        g.setColor(PlayerColors.readableOn(fill));
        g.drawString(label, x + (cell - metrics.stringWidth(label)) / 2,
                y + (cell - metrics.getHeight()) / 2 + metrics.getAscent());
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

    private int tileX(Position position, int cell) {
        return originX(cell) + position.col() * cell;
    }

    private int tileY(Position position, int cell) {
        return originY(cell) + position.row() * cell;
    }

    private int originX(int cell) {
        return (getWidth() - cell * board.cols()) / 2;
    }

    private int originY(int cell) {
        return (getHeight() - cell * board.rows()) / 2;
    }
}
