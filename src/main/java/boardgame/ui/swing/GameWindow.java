package boardgame.ui.swing;

import boardgame.engine.Game;
import boardgame.engine.GameEvent;
import boardgame.engine.GameListener;
import boardgame.ui.EventFormatter;
import boardgame.ui.Labels;
import boardgame.ui.Language;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JToggleButton;
import javax.swing.Timer;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public final class GameWindow extends JFrame implements GameListener {

    private static final int AUTO_PLAY_DELAY_MS = 350;
    private static final Language DEFAULT_LANGUAGE = Language.FR;

    private final Game game;
    private final String titleKey;
    private final List<GameEvent> history = new ArrayList<>();
    private final BoardPanel boardPanel;
    private final PlayersPanel playersPanel;
    private final TerrainLegend legend;
    private final JTextArea log = new JTextArea();
    private final JButton stepButton = new JButton();
    private final JToggleButton autoPlayButton = new JToggleButton();
    private final JComboBox<Language> languageBox = new JComboBox<>(Language.values());
    private final Timer autoPlay = new Timer(AUTO_PLAY_DELAY_MS, event -> step());
    private Labels labels = new Labels(DEFAULT_LANGUAGE);
    private EventFormatter formatter = new EventFormatter(labels);

    public GameWindow(String titleKey, Game game) {
        this.game = game;
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
        languageBox.setSelectedItem(DEFAULT_LANGUAGE);
        languageBox.addActionListener(event -> switchTo((Language) languageBox.getSelectedItem()));
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT));
        controls.add(stepButton);
        controls.add(autoPlayButton);
        controls.add(languageBox);
        JPanel bottom = new JPanel(new BorderLayout());
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
        switchTo(DEFAULT_LANGUAGE);
        pack();
        setLocationRelativeTo(null);

        game.addListener(this);
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
        boardPanel.setLabels(labels);
        playersPanel.setLabels(labels);
        legend.setLabels(labels);
        log.setText(history.stream().map(event -> formatter.format(event) + "\n").collect(Collectors.joining()));
        log.setCaretPosition(log.getDocument().getLength());
        revalidate();
    }

    private void step() {
        if (!game.isOver()) {
            game.playTurn();
        }
        if (game.isOver()) {
            autoPlay.stop();
            autoPlayButton.setSelected(false);
            autoPlayButton.setEnabled(false);
            stepButton.setEnabled(false);
        }
    }
}
