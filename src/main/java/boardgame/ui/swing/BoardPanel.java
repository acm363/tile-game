package boardgame.ui.swing;

import boardgame.agriculture.WorkerDeployed;
import boardgame.agriculture.WorkerDismissed;
import boardgame.board.Board;
import boardgame.board.Position;
import boardgame.engine.Action;
import boardgame.engine.Deploy;
import boardgame.engine.GameEvent;
import boardgame.player.Player;
import boardgame.ui.Labels;
import boardgame.unit.Army;
import boardgame.unit.Unit;
import boardgame.war.ArmyDeployed;
import boardgame.war.ArmyDestroyed;
import boardgame.war.ArmyPromoted;
import boardgame.war.DeploymentsClashed;

import javax.swing.JPanel;
import javax.swing.Timer;
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
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.function.Consumer;

final class BoardPanel extends JPanel {

    private static final int PREFERRED_CELL = 56;
    private static final Color GRID = new Color(0x14125C);
    static final Color HIGHLIGHT = Color.WHITE;
    static final Color RED = new Color(0xFF4D4D);
    static final Color GOLD = new Color(0xFFD700);
    private static final int GHOST_ALPHA = 90;
    private static final int ANIMATION_FRAME_MS = 40;

    private final Board board;
    private final PlayerColors colors;
    private Labels labels;
    private Map<Position, Color> highlights = Map.of();
    private Consumer<Position> onTileClicked = position -> {
    };
    private Consumer<Optional<Position>> onTileHovered = position -> {
    };
    private Optional<Position> hovered = Optional.empty();
    private Action previewedAction;
    private Player previewedOwner;
    private List<GameEvent> previewedEffects = List.of();
    private final TileAnimations animations = new TileAnimations(new Random());
    private final Timer animationTimer = new Timer(ANIMATION_FRAME_MS, event -> tickAnimations());

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

    void animate(GameEvent event) {
        switch (event) {
            case ArmyDeployed e -> animations.flash(e.position(), HIGHLIGHT);
            case WorkerDeployed e -> animations.flash(e.position(), HIGHLIGHT);
            case ArmyDestroyed e -> {
                animations.flash(e.from(), HIGHLIGHT);
                animations.crumble(e.target(), colors.of(e.defender()));
            }
            case ArmyPromoted e -> animations.flash(e.position(), GOLD);
            case DeploymentsClashed e -> animations.flash(e.position(), RED);
            case WorkerDismissed e -> animations.crumble(e.position(), colors.of(e.owner()));
            default -> {
                return;
            }
        }
        if (!animationTimer.isRunning()) {
            animationTimer.start();
        }
        repaint();
    }

    boolean isAnimating() {
        return animations.isRunning();
    }

    void tickAnimations() {
        animations.tick();
        if (!animations.isRunning()) {
            animationTimer.stop();
        }
        repaint();
    }

    void setOnTileHovered(Consumer<Optional<Position>> listener) {
        onTileHovered = listener;
    }

    void setPreview(Action action, Player owner, List<GameEvent> effects) {
        previewedAction = action;
        previewedOwner = owner;
        previewedEffects = List.copyOf(effects);
        repaint();
    }

    void clearPreview() {
        if (previewedAction != null) {
            previewedAction = null;
            previewedOwner = null;
            previewedEffects = List.of();
            repaint();
        }
    }

    void setHighlights(Map<Position, Color> highlights) {
        this.highlights = Map.copyOf(highlights);
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
            Color highlight = highlights.get(position);
            if (highlight != null) {
                paintHighlight(g, highlight, x, y, cell);
            }
        }
        animations.paint(g, originX(cell), originY(cell), cell);
        paintPreview(g, cell);
        g.dispose();
    }

    private void paintPreview(Graphics2D g, int cell) {
        if (previewedAction instanceof Deploy deploy) {
            Position target = deploy.position();
            paintGhost(g, colors.of(previewedOwner), deploy.size(), tileX(target, cell), tileY(target, cell), cell);
        }
        for (GameEvent effect : previewedEffects) {
            switch (effect) {
                case ArmyDestroyed e -> paintEffect(g, e.target(), RED, "×", cell);
                case ArmyPromoted e -> paintEffect(g, e.position(), GOLD, "★" + e.level(), cell);
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

    private void paintHighlight(Graphics2D g, Color color, int x, int y, int cell) {
        int width = Math.max(2, cell / 14);
        g.setColor(color);
        g.setStroke(new BasicStroke(width));
        g.drawRect(x + width, y + width, cell - 2 * width, cell - 2 * width);
    }

    private void paintUnit(Graphics2D g, Unit unit, int x, int y, int cell) {
        String label = unit instanceof Army army ? String.valueOf(army.size()) : "";
        paintDisc(g, colors.of(unit.owner()), label, x, y, cell);
        if (unit instanceof Army army) {
            paintLevel(g, army.level(), x, y, cell);
        }
    }

    private void paintLevel(Graphics2D g, int level, int x, int y, int cell) {
        int pip = Math.max(4, cell / 8);
        int gap = pip / 2;
        int left = x + (cell - level * pip - (level - 1) * gap) / 2;
        int top = y + cell - pip - Math.max(1, cell / 28);
        g.setStroke(new BasicStroke(1f));
        for (int index = 0; index < level; index++) {
            int pipX = left + index * (pip + gap);
            g.setColor(GOLD);
            g.fillOval(pipX, top, pip, pip);
            g.setColor(Color.BLACK);
            g.drawOval(pipX, top, pip, pip);
        }
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
                        ? labels.text("tooltip.army", tile, unit.owner(), army.size(), army.level())
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
