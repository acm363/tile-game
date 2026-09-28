package boardgame.ui.swing;

import boardgame.board.Position;
import boardgame.engine.Action;
import boardgame.engine.Game;
import boardgame.engine.GameEvent;
import boardgame.engine.GameListener;
import boardgame.engine.Pass;
import boardgame.ui.EventFormatter;
import boardgame.ui.HumanDecider;
import boardgame.ui.HumanTurn;
import boardgame.ui.Labels;
import boardgame.ui.Language;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JToggleButton;
import javax.swing.Timer;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public final class GameWindow extends JFrame implements GameListener {

    private static final int AUTO_PLAY_DELAY_MS = 350;
    private static final Language DEFAULT_LANGUAGE = Language.FR;

    private final Game game;
    private final HumanDecider human;
    private final String titleKey;
    private final List<GameEvent> history = new ArrayList<>();
    private final BoardPanel boardPanel;
    private final PlayersPanel playersPanel;
    private final TerrainLegend legend;
    private final JTextArea log = new JTextArea();
    private final JLabel status = new JLabel(" ");
    private final JButton stepButton = new JButton();
    private final JToggleButton autoPlayButton = new JToggleButton();
    private final JButton passButton = new JButton();
    private final JLabel sizeLabel = new JLabel();
    private final JComboBox<Integer> sizeBox = new JComboBox<>();
    private final JComboBox<Action> otherBox = new JComboBox<>();
    private final JButton otherButton = new JButton();
    private final JComboBox<Language> languageBox = new JComboBox<>(Language.values());
    private final Timer autoPlay = new Timer(AUTO_PLAY_DELAY_MS, event -> step());
    private Labels labels = new Labels(DEFAULT_LANGUAGE);
    private EventFormatter formatter = new EventFormatter(labels);
    private HumanTurn turn;
    private int preferredSize = Integer.MAX_VALUE;
    private boolean refreshingSizes;

    public GameWindow(String titleKey, Game game, HumanDecider human) {
        this.game = game;
        this.human = human;
        this.titleKey = titleKey;
        PlayerColors colors = new PlayerColors(game.players());
        boardPanel = new BoardPanel(game.board(), colors, labels);
        playersPanel = new PlayersPanel(game, colors, labels);
        legend = new TerrainLegend(labels);

        log.setEditable(false);
        log.setLineWrap(true);
        log.setWrapStyleWord(true);
        log.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane logScroll = new JScrollPane(log);
        logScroll.setPreferredSize(new Dimension(420, 360));

        stepButton.addActionListener(event -> step());
        autoPlayButton.addActionListener(event -> {
            if (autoPlayButton.isSelected()) {
                autoPlay.start();
            } else {
                autoPlay.stop();
            }
        });
        passButton.addActionListener(event -> playHuman(new Pass()));
        sizeBox.addActionListener(event -> {
            if (!refreshingSizes && sizeBox.getSelectedItem() instanceof Integer size) {
                preferredSize = size;
                boardPanel.setHighlighted(turn == null ? Set.of() : turn.deployTargets(size));
            }
        });
        otherBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean selected,
                                                          boolean focused) {
                Object text = value instanceof Action action ? labels.action(action) : value;
                return super.getListCellRendererComponent(list, text, index, selected, focused);
            }
        });
        otherButton.addActionListener(event -> {
            if (otherBox.getSelectedItem() instanceof Action action) {
                playHuman(action);
            }
        });
        boardPanel.setOnTileClicked(this::deployAt);
        languageBox.setSelectedItem(DEFAULT_LANGUAGE);
        languageBox.addActionListener(event -> switchTo((Language) languageBox.getSelectedItem()));

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT));
        controls.add(stepButton);
        controls.add(autoPlayButton);
        controls.add(passButton);
        controls.add(sizeLabel);
        controls.add(sizeBox);
        controls.add(otherBox);
        controls.add(otherButton);
        controls.add(languageBox);
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.add(status, BorderLayout.NORTH);
        bottom.add(controls, BorderLayout.WEST);
        bottom.add(legend, BorderLayout.EAST);

        JPanel side = new JPanel(new BorderLayout());
        side.add(playersPanel, BorderLayout.NORTH);
        side.add(logScroll, BorderLayout.CENTER);

        setLayout(new BorderLayout());
        add(boardPanel, BorderLayout.CENTER);
        add(side, BorderLayout.EAST);
        add(bottom, BorderLayout.SOUTH);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent event) {
                autoPlay.stop();
            }
        });
        game.addListener(this);
        switchTo(DEFAULT_LANGUAGE);
        pack();
        setLocationRelativeTo(null);
    }

    @Override
    public void onEvent(GameEvent event) {
        history.add(event);
        log.append(formatter.format(event) + "\n");
        log.setCaretPosition(log.getDocument().getLength());
        boardPanel.repaint();
        playersPanel.refresh();
    }

    private void switchTo(Language language) {
        labels = new Labels(language);
        formatter = new EventFormatter(labels);
        setTitle(labels.text(titleKey));
        stepButton.setText(labels.text("button.step"));
        autoPlayButton.setText(labels.text("button.autoPlay"));
        passButton.setText(labels.text("button.pass"));
        sizeLabel.setText(labels.text("label.size"));
        otherButton.setText(labels.text("button.sell"));
        boardPanel.setLabels(labels);
        playersPanel.setLabels(labels);
        legend.setLabels(labels);
        log.setText(history.stream().map(event -> formatter.format(event) + "\n").collect(Collectors.joining()));
        log.setCaretPosition(log.getDocument().getLength());
        refreshTurn();
    }

    private void step() {
        if (!game.isOver() && turn == null) {
            game.playTurn();
            refreshTurn();
        }
    }

    private void deployAt(Position position) {
        if (turn != null) {
            turn.deployAt(position, selectedSize()).ifPresent(this::playHuman);
        }
    }

    private void playHuman(Action action) {
        if (turn != null) {
            human.submit(action);
            game.playTurn();
            refreshTurn();
        }
    }

    private void refreshTurn() {
        turn = !game.isOver() && human.controls(game.currentPlayer()) ? new HumanTurn(game.legalActions()) : null;
        boolean humanTurn = turn != null;

        refreshingSizes = true;
        sizeBox.removeAllItems();
        List<Integer> sizes = humanTurn ? turn.deploySizes() : List.of();
        sizes.forEach(sizeBox::addItem);
        sizes.stream().filter(size -> size <= preferredSize).reduce((first, second) -> second)
                .or(() -> sizes.stream().findFirst())
                .ifPresent(sizeBox::setSelectedItem);
        refreshingSizes = false;
        sizeLabel.setVisible(sizes.size() > 1);
        sizeBox.setVisible(sizes.size() > 1);

        otherBox.removeAllItems();
        List<Action> others = humanTurn ? turn.otherActions() : List.of();
        others.forEach(otherBox::addItem);
        otherBox.setVisible(!others.isEmpty());
        otherButton.setVisible(!others.isEmpty());

        passButton.setVisible(humanTurn);
        passButton.setEnabled(humanTurn && turn.canPass());
        stepButton.setEnabled(!game.isOver() && !humanTurn);
        boardPanel.setHighlighted(humanTurn ? turn.deployTargets(selectedSize()) : Set.of());
        status.setText(humanTurn ? labels.text("status.humanTurn", game.currentPlayer()) : " ");

        if (game.isOver()) {
            autoPlay.stop();
            autoPlayButton.setSelected(false);
            autoPlayButton.setEnabled(false);
        }
        revalidate();
    }

    private int selectedSize() {
        return sizeBox.getSelectedItem() instanceof Integer size ? size : 1;
    }
}
