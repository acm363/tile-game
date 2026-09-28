package boardgame.ui.swing;

import boardgame.engine.Game;
import boardgame.engine.GameEvent;
import boardgame.engine.GameListener;
import boardgame.ui.EventFormatter;

import javax.swing.JButton;
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

public final class GameWindow extends JFrame implements GameListener {

    private static final int AUTO_PLAY_DELAY_MS = 350;

    private final Game game;
    private final EventFormatter formatter = new EventFormatter();
    private final BoardPanel boardPanel;
    private final PlayersPanel playersPanel;
    private final JTextArea log = new JTextArea();
    private final JButton stepButton = new JButton("Tour suivant");
    private final JToggleButton autoPlayButton = new JToggleButton("Lecture auto");
    private final Timer autoPlay = new Timer(AUTO_PLAY_DELAY_MS, event -> step());

    public GameWindow(String title, Game game) {
        super(title);
        this.game = game;
        PlayerColors colors = new PlayerColors(game.players());
        boardPanel = new BoardPanel(game.board(), colors);
        playersPanel = new PlayersPanel(game, colors);

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
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT));
        controls.add(stepButton);
        controls.add(autoPlayButton);
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.add(controls, BorderLayout.WEST);
        bottom.add(new TerrainLegend(), BorderLayout.EAST);

        JPanel side = new JPanel(new BorderLayout());
        side.add(playersPanel, BorderLayout.NORTH);
        side.add(logScroll, BorderLayout.CENTER);

        setLayout(new BorderLayout());
        add(boardPanel, BorderLayout.CENTER);
        add(side, BorderLayout.EAST);
        add(bottom, BorderLayout.SOUTH);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        pack();
        setLocationRelativeTo(null);

        game.addListener(this);
    }

    @Override
    public void onEvent(GameEvent event) {
        log.append(formatter.format(event) + "\n");
        log.setCaretPosition(log.getDocument().getLength());
        boardPanel.repaint();
        playersPanel.refresh();
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
