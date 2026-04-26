package com.memorygame.ui;

import com.memorygame.model.Card;
import com.memorygame.model.GamePhase;
import com.memorygame.model.GameState;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

/**
 * Panel displaying the game board (card grid), scores, turn indicator, and status.
 * Receives GameState snapshots and updates the UI accordingly.
 */
public class GamePanel extends JPanel {

    private static final Color BG_COLOR = new Color(26, 26, 46);             // #1a1a2e
    private static final Color PANEL_BG = new Color(22, 33, 62);             // #16213e
    private static final Color PLAYER1_COLOR = new Color(0, 210, 255);       // cyan
    private static final Color PLAYER2_COLOR = new Color(255, 107, 157);     // pink
    private static final Color TEXT_COLOR = new Color(230, 230, 240);
    private static final Color DIM_TEXT = new Color(150, 150, 170);
    private static final Color GOLD = new Color(255, 215, 0);

    private final int localPlayerNumber;
    private final boolean localMode;
    private final String player1Name;
    private final String player2Name;
    private final IntConsumer onCardClick;
    private final Runnable onRestartClick;
    private final Runnable onBackClick;

    private JLabel player1NameLabel;
    private JLabel player1ScoreLabel;
    private JLabel player2NameLabel;
    private JLabel player2ScoreLabel;
    private JLabel turnLabel;
    private JLabel statusLabel;
    private JPanel cardGridPanel;
    private JButton restartButton;

    private List<CardComponent> cardComponents = new ArrayList<>();
    private GameState currentState;

    public GamePanel(int localPlayerNumber, boolean localMode, String p1Name, String p2Name, IntConsumer onCardClick, Runnable onRestartClick, Runnable onBackClick) {
        this.localPlayerNumber = localPlayerNumber;
        this.localMode = localMode;
        this.player1Name = p1Name == null || p1Name.isBlank() ? "Player 1" : p1Name;
        this.player2Name = p2Name == null || p2Name.isBlank() ? "Player 2" : p2Name;
        this.onCardClick = onCardClick;
        this.onRestartClick = onRestartClick;
        this.onBackClick = onBackClick;

        setBackground(BG_COLOR);
        setLayout(new BorderLayout(0, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        add(createTopBar(), BorderLayout.NORTH);
        add(createStatusBar(), BorderLayout.SOUTH);

        // Card grid (center) will be initialized when game starts
        cardGridPanel = new JPanel();
        cardGridPanel.setOpaque(false);
        add(cardGridPanel, BorderLayout.CENTER);
    }

    private JPanel createTopBar() {
        JPanel topBar = new JPanel(new BorderLayout(20, 0));
        topBar.setOpaque(false);
        topBar.setBorder(BorderFactory.createEmptyBorder(5, 10, 15, 10));

        // Player 1 info
        JPanel p1Panel = createPlayerPanel(1);
        player1NameLabel = (JLabel) ((JPanel) p1Panel.getComponent(0)).getComponent(0);
        player1ScoreLabel = (JLabel) p1Panel.getComponent(1);

        // Player 2 info
        JPanel p2Panel = createPlayerPanel(2);
        player2NameLabel = (JLabel) ((JPanel) p2Panel.getComponent(0)).getComponent(0);
        player2ScoreLabel = (JLabel) p2Panel.getComponent(1);

        // Turn indicator (center)
        turnLabel = new JLabel("Waiting...", SwingConstants.CENTER);
        turnLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
        turnLabel.setForeground(GOLD);

        topBar.add(p1Panel, BorderLayout.WEST);
        topBar.add(turnLabel, BorderLayout.CENTER);
        topBar.add(p2Panel, BorderLayout.EAST);

        return topBar;
    }

    private String getPlayerDisplayName(int playerNum) {
        String name = (playerNum == 1) ? player1Name : player2Name;
        if (!localMode && playerNum == localPlayerNumber) name += " (You)";
        return name;
    }

    private JPanel createPlayerPanel(int playerNum) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);
        panel.setPreferredSize(new Dimension(200, 60));

        Color playerColor = (playerNum == 1) ? PLAYER1_COLOR : PLAYER2_COLOR;
        String name = getPlayerDisplayName(playerNum);

        JPanel nameRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));
        nameRow.setOpaque(false);

        JLabel nameLabel = new JLabel(name);
        nameLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        nameLabel.setForeground(playerColor);
        nameRow.add(nameLabel);

        JLabel scoreLabel = new JLabel("0");
        scoreLabel.setFont(new Font("SansSerif", Font.BOLD, 28));
        scoreLabel.setForeground(TEXT_COLOR);
        scoreLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        scoreLabel.setHorizontalAlignment(SwingConstants.CENTER);

        panel.add(nameRow);
        panel.add(scoreLabel);
        return panel;
    }

    private JPanel createStatusBar() {
        JPanel bar = new JPanel(new BorderLayout(10, 0));
        bar.setOpaque(false);
        bar.setBorder(BorderFactory.createEmptyBorder(10, 10, 5, 10));

        statusLabel = new JLabel(" ", SwingConstants.CENTER);
        statusLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));
        statusLabel.setForeground(DIM_TEXT);

        restartButton = new JButton("New Game");
        restartButton.setFont(new Font("SansSerif", Font.BOLD, 14));
        restartButton.setForeground(Color.WHITE);
        restartButton.setBackground(new Color(46, 204, 113));
        restartButton.setFocusPainted(false);
        restartButton.setBorderPainted(false);
        restartButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        restartButton.setPreferredSize(new Dimension(130, 36));
        restartButton.setVisible(false);
        restartButton.addActionListener(e -> onRestartClick.run());

        JButton backButton = new JButton("Main Menu");
        backButton.setFont(new Font("SansSerif", Font.BOLD, 14));
        backButton.setForeground(TEXT_COLOR);
        backButton.setBackground(new Color(231, 76, 60)); // Red-ish
        backButton.setFocusPainted(false);
        backButton.setBorderPainted(false);
        backButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        backButton.setPreferredSize(new Dimension(130, 36));
        backButton.addActionListener(e -> onBackClick.run());

        bar.add(backButton, BorderLayout.WEST);
        bar.add(statusLabel, BorderLayout.CENTER);
        bar.add(restartButton, BorderLayout.EAST);

        return bar;
    }

    /**
     * Updates the entire panel to reflect the given game state.
     * Must be called on the EDT.
     */
    public void updateState(GameState state) {
        this.currentState = state;

        // Scores
        player1ScoreLabel.setText(String.valueOf(state.getPlayer1Score()));
        player2ScoreLabel.setText(String.valueOf(state.getPlayer2Score()));

        // Highlight active player and set crown
        boolean p1Active = state.getActivePlayer() == 1;
        player1NameLabel.setText(p1Active ? "\uD83D\uDC51 " + getPlayerDisplayName(1) : getPlayerDisplayName(1));
        player2NameLabel.setText(!p1Active ? "\uD83D\uDC51 " + getPlayerDisplayName(2) : getPlayerDisplayName(2));
        
        player1NameLabel.setForeground(p1Active ? PLAYER1_COLOR : DIM_TEXT);
        player2NameLabel.setForeground(!p1Active ? PLAYER2_COLOR : DIM_TEXT);
        player1ScoreLabel.setForeground(p1Active ? PLAYER1_COLOR : TEXT_COLOR);
        player2ScoreLabel.setForeground(!p1Active ? PLAYER2_COLOR : TEXT_COLOR);

        // Turn label
        if (state.getPhase() == GamePhase.GAME_OVER) {
            int winner = state.getWinner();
            if (winner == 0) {
                turnLabel.setText("It's a Draw!");
                turnLabel.setForeground(GOLD);
            } else {
                Color wColor = (winner == 1) ? PLAYER1_COLOR : PLAYER2_COLOR;
                String winnerName = (winner == 1) ? player1Name : player2Name;
                turnLabel.setText(winnerName + " Wins!");
                turnLabel.setForeground(wColor);
            }
            restartButton.setVisible(true);
        } else if (state.getPhase() == GamePhase.RESOLVING) {
            turnLabel.setText("Resolving...");
            turnLabel.setForeground(GOLD);
        } else {
            if (localMode) {
                String playerName = (state.getActivePlayer() == 1) ? player1Name : player2Name;
                turnLabel.setText(playerName + "'s Turn");
                Color turnColor = (state.getActivePlayer() == 1) ? PLAYER1_COLOR : PLAYER2_COLOR;
                turnLabel.setForeground(turnColor);
            } else {
                boolean myTurn = state.getActivePlayer() == localPlayerNumber;
                turnLabel.setText(myTurn ? "Your Turn" : "Opponent's Turn");
                turnLabel.setForeground(myTurn ? GOLD : DIM_TEXT);
            }
            restartButton.setVisible(false);
        }

        // Status message
        String msg = state.getStatusMessage();
        if (msg != null && !msg.isEmpty()) {
            statusLabel.setText(msg);
        } else {
            int matched = state.getTotalMatched();
            int total = state.getDeckSize();
            statusLabel.setText("Matched: " + matched + " / " + total +
                " | Match size: " + state.getMatchSize());
        }

        // Initialize card grid if needed
        if (cardComponents.size() != state.getCards().size()) {
            initializeCardGrid(state);
        }

        // Update cards
        List<Card> cards = state.getCards();
        List<Integer> attempt = state.getCurrentAttempt();
        for (int i = 0; i < cards.size(); i++) {
            cardComponents.get(i).updateCard(cards.get(i), attempt.contains(i));
        }
    }

    private void initializeCardGrid(GameState state) {
        cardGridPanel.removeAll();
        cardComponents.clear();

        int total = state.getDeckSize();
        int rows = (int) Math.sqrt(total);
        while (total % rows != 0) {
            rows--;
        }
        int cols = total / rows;

        cardGridPanel.setLayout(new GridLayout(rows, cols, 8, 8));

        for (int i = 0; i < total; i++) {
            CardComponent cc = new CardComponent(i, onCardClick);
            cardComponents.add(cc);
            cardGridPanel.add(cc);
        }

        cardGridPanel.revalidate();
        cardGridPanel.repaint();
    }

    /** Disables all card clicks (e.g., on connection loss). */
    public void disableInput() {
        for (CardComponent cc : cardComponents) {
            cc.setEnabled(false);
        }
    }

    public void showError(String message) {
        statusLabel.setText("ERROR: " + message);
        statusLabel.setForeground(new Color(231, 76, 60));
    }
}
