package com.memorygame.ui;

import com.memorygame.model.GameConfig;
import com.memorygame.network.GameHost;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Two-step menu panel:
 *   Step 1 — Choose mode: Local / Host / Join
 *   Step 2 — Configure game (n + deck size) or enter connection details
 */
public class MenuPanel extends JPanel {

    private static final Color BG_COLOR = new Color(15, 15, 30);
    private static final Color PANEL_BG = new Color(25, 28, 50);
    private static final Color CARD_BG = new Color(30, 35, 65);
    private static final Color CARD_HOVER = new Color(40, 45, 80);
    private static final Color ACCENT_PURPLE = new Color(168, 130, 255);
    private static final Color ACCENT_CYAN = new Color(0, 210, 255);
    private static final Color ACCENT_PINK = new Color(255, 107, 157);
    private static final Color TEXT_WHITE = new Color(240, 240, 250);
    private static final Color TEXT_DIM = new Color(140, 140, 170);
    private static final Color SUCCESS_GREEN = new Color(46, 204, 113);
    private static final Color ERROR_RED = new Color(231, 76, 60);

    private final BiConsumer<Integer, Integer> onLocalGame;
    private final BiConsumer<Integer, Integer> onHost;
    // (playerName, hostAddress, port)
    private final TriConsumer<String, String, Integer> onJoin;

    @FunctionalInterface
    public interface TriConsumer<A, B, C> {
        void accept(A a, B b, C c);
    }

    private final CardLayout cardLayout;
    private final JPanel contentPanel;

    // Mode selection
    private enum Mode { LOCAL, HOST, JOIN }
    private Mode selectedMode;

    // Config state
    private int matchSize = 2;
    private int selectedDeckSize = 20;

    // Config screen widgets
    private JLabel matchSizeLabel;
    private JButton[] suggestionButtons;
    private JTextField customDeckField;
    private JLabel deckErrorLabel;
    private JLabel statusLabel;
    private JTextField portField;
    private JTextField player1Field;
    private JTextField player2Field;
    private JPanel player2Col; // hidden in HOST mode
    private JTextField joinNameField; // name for the joining player

    public String getPlayer1Name() { return player1Field != null ? player1Field.getText().trim() : ""; }
    public String getPlayer2Name() { return player2Field != null ? player2Field.getText().trim() : ""; }
    public String getJoinPlayerName() { return joinNameField != null ? joinNameField.getText().trim() : ""; }

    public MenuPanel(BiConsumer<Integer, Integer> onLocalGame,
                     BiConsumer<Integer, Integer> onHost,
                     TriConsumer<String, String, Integer> onJoin) {
        this.onLocalGame = onLocalGame;
        this.onHost = onHost;
        this.onJoin = onJoin;

        setBackground(BG_COLOR);
        setLayout(new BorderLayout());

        cardLayout = new CardLayout();
        contentPanel = new JPanel(cardLayout);
        contentPanel.setOpaque(false);

        contentPanel.add(buildModeScreen(), "MODE");
        contentPanel.add(buildConfigScreen(), "CONFIG");
        contentPanel.add(buildJoinScreen(), "JOIN");

        add(contentPanel, BorderLayout.CENTER);
        cardLayout.show(contentPanel, "MODE");
    }

    // ======================== SCREEN 1: MODE SELECTION ========================

    private JPanel buildModeScreen() {
        JPanel screen = new JPanel(new GridBagLayout());
        screen.setBackground(BG_COLOR);

        JPanel center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.setOpaque(false);

        // Title
        JLabel title = makeLabel("MEMORY GAME", 52, Font.BOLD, ACCENT_CYAN);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        center.add(title);
        center.add(Box.createVerticalStrut(6));

        JLabel subtitle = makeLabel("Choose how you want to play", 18, Font.PLAIN, TEXT_DIM);
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        center.add(subtitle);
        center.add(Box.createVerticalStrut(40));

        // Mode cards
        JPanel cardsRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 24, 0));
        cardsRow.setOpaque(false);

        cardsRow.add(buildModeCard(
            "\u265A", "LOCAL GAME",
            "Two players, one screen",
            ACCENT_PURPLE, () -> { selectedMode = Mode.LOCAL; showConfigScreen(); }
        ));
        cardsRow.add(buildModeCard(
            "\u2302", "HOST GAME",
            "Create a LAN session",
            ACCENT_CYAN, () -> { selectedMode = Mode.HOST; showConfigScreen(); }
        ));
        cardsRow.add(buildModeCard(
            "\u2192", "JOIN GAME",
            "Connect to a host",
            ACCENT_PINK, () -> { selectedMode = Mode.JOIN; cardLayout.show(contentPanel, "JOIN"); }
        ));

        center.add(cardsRow);
        screen.add(center);
        return screen;
    }

    private JPanel buildModeCard(String icon, String title, String desc, Color accent, Runnable onClick) {
        JPanel card = new JPanel() {
            boolean hovered = false;
            {
                setPreferredSize(new Dimension(220, 220));
                setOpaque(false);
                setCursor(new Cursor(Cursor.HAND_CURSOR));
                addMouseListener(new MouseAdapter() {
                    @Override public void mouseClicked(MouseEvent e) { onClick.run(); }
                    @Override public void mouseEntered(MouseEvent e) { hovered = true; repaint(); }
                    @Override public void mouseExited(MouseEvent e) { hovered = false; repaint(); }
                });
            }

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int w = getWidth() - 4, h = getHeight() - 4;
                RoundRectangle2D rect = new RoundRectangle2D.Float(2, 2, w, h, 20, 20);

                // Background
                g2.setColor(hovered ? CARD_HOVER : CARD_BG);
                g2.fill(rect);

                // Border
                g2.setColor(hovered ? accent : accent.darker().darker());
                g2.setStroke(new BasicStroke(hovered ? 2.5f : 1.5f));
                g2.draw(rect);

                // Glow on hover
                if (hovered) {
                    g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 25));
                    g2.fill(rect);
                }

                // Icon
                g2.setColor(accent);
                g2.setFont(new Font("SansSerif", Font.PLAIN, 48));
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(icon, (getWidth() - fm.stringWidth(icon)) / 2, 80);

                // Title
                g2.setFont(new Font("SansSerif", Font.BOLD, 17));
                fm = g2.getFontMetrics();
                g2.drawString(title, (getWidth() - fm.stringWidth(title)) / 2, 120);

                // Description
                g2.setColor(TEXT_DIM);
                g2.setFont(new Font("SansSerif", Font.PLAIN, 13));
                fm = g2.getFontMetrics();
                g2.drawString(desc, (getWidth() - fm.stringWidth(desc)) / 2, 150);

                g2.dispose();
            }
        };
        return card;
    }

    // ======================== SCREEN 2: GAME CONFIG ========================

    private JPanel buildConfigScreen() {
        JPanel screen = new JPanel(new GridBagLayout());
        screen.setBackground(BG_COLOR);

        JPanel center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.setOpaque(false);
        center.setMaximumSize(new Dimension(600, 700));

        // Back button
        JButton backBtn = makeBackButton();
        backBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        center.add(backBtn);
        center.add(Box.createVerticalStrut(10));

        // Title
        JLabel configTitle = makeLabel("Configure Your Game", 32, Font.BOLD, TEXT_WHITE);
        configTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        center.add(configTitle);
        center.add(Box.createVerticalStrut(20));

        // ---- Player Names ----
        JPanel namesRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 0));
        namesRow.setOpaque(false);

        JPanel p1Col = new JPanel();
        p1Col.setLayout(new BoxLayout(p1Col, BoxLayout.Y_AXIS));
        p1Col.setOpaque(false);
        JLabel p1Label = makeLabel("Player 1 Name", 14, Font.BOLD, ACCENT_CYAN);
        p1Label.setAlignmentX(Component.CENTER_ALIGNMENT);
        player1Field = new JTextField("Player 1", 10);
        player1Field.setFont(new Font("SansSerif", Font.PLAIN, 16));
        player1Field.setHorizontalAlignment(JTextField.CENTER);
        p1Col.add(p1Label);
        p1Col.add(Box.createVerticalStrut(4));
        p1Col.add(player1Field);

        player2Col = new JPanel();
        player2Col.setLayout(new BoxLayout(player2Col, BoxLayout.Y_AXIS));
        player2Col.setOpaque(false);
        JLabel p2Label = makeLabel("Player 2 Name", 14, Font.BOLD, ACCENT_PINK);
        p2Label.setAlignmentX(Component.CENTER_ALIGNMENT);
        player2Field = new JTextField("Player 2", 10);
        player2Field.setFont(new Font("SansSerif", Font.PLAIN, 16));
        player2Field.setHorizontalAlignment(JTextField.CENTER);
        player2Col.add(p2Label);
        player2Col.add(Box.createVerticalStrut(4));
        player2Col.add(player2Field);

        namesRow.add(p1Col);
        namesRow.add(player2Col);
        center.add(namesRow);
        center.add(Box.createVerticalStrut(20));

        // ---- Match Size (n) ----
        JLabel nTitle = makeLabel("Match Size (n)", 20, Font.BOLD, ACCENT_CYAN);
        nTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        center.add(nTitle);
        center.add(Box.createVerticalStrut(4));

        JLabel nDesc = makeLabel("How many identical cards form a match", 14, Font.PLAIN, TEXT_DIM);
        nDesc.setAlignmentX(Component.CENTER_ALIGNMENT);
        center.add(nDesc);
        center.add(Box.createVerticalStrut(12));

        // n selector: [ - ]  n  [ + ]
        JPanel nRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 16, 0));
        nRow.setOpaque(false);

        JButton minusBtn = makeRoundButton("\u2212", 50, ACCENT_CYAN);
        matchSizeLabel = makeLabel(String.valueOf(matchSize), 36, Font.BOLD, TEXT_WHITE);
        JButton plusBtn = makeRoundButton("+", 50, ACCENT_CYAN);

        minusBtn.addActionListener(e -> { if (matchSize > 2) { matchSize--; updateConfigUI(); } });
        plusBtn.addActionListener(e -> { if (matchSize < 6) { matchSize++; updateConfigUI(); } });

        nRow.add(minusBtn);
        nRow.add(Box.createHorizontalStrut(10));
        nRow.add(matchSizeLabel);
        nRow.add(Box.createHorizontalStrut(10));
        nRow.add(plusBtn);
        center.add(nRow);
        center.add(Box.createVerticalStrut(28));

        // ---- Deck Size ----
        JLabel deckTitle = makeLabel("Number of Cards", 20, Font.BOLD, ACCENT_PURPLE);
        deckTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        center.add(deckTitle);
        center.add(Box.createVerticalStrut(12));

        // 3 suggestion buttons
        JPanel sugRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 16, 0));
        sugRow.setOpaque(false);
        suggestionButtons = new JButton[3];
        String[] sizeLabels = {"Small", "Medium", "Large"};

        for (int i = 0; i < 3; i++) {
            final int idx = i;
            suggestionButtons[i] = new JButton();
            suggestionButtons[i].setPreferredSize(new Dimension(140, 60));
            suggestionButtons[i].setFont(new Font("SansSerif", Font.BOLD, 18));
            suggestionButtons[i].setFocusPainted(false);
            suggestionButtons[i].setCursor(new Cursor(Cursor.HAND_CURSOR));
            suggestionButtons[i].addActionListener(e -> selectSuggestion(idx));
            sugRow.add(suggestionButtons[i]);
        }
        center.add(sugRow);
        center.add(Box.createVerticalStrut(12));

        // Custom entry
        JPanel customRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        customRow.setOpaque(false);

        JLabel orLabel = makeLabel("or enter custom:", 14, Font.ITALIC, TEXT_DIM);
        customRow.add(orLabel);

        customDeckField = new JTextField(5);
        customDeckField.setFont(new Font("SansSerif", Font.PLAIN, 18));
        customDeckField.setPreferredSize(new Dimension(80, 36));
        customDeckField.setHorizontalAlignment(JTextField.CENTER);
        customDeckField.addActionListener(e -> applyCustomDeck());
        customRow.add(customDeckField);

        JButton applyBtn = new JButton("Apply");
        applyBtn.setFont(new Font("SansSerif", Font.BOLD, 14));
        applyBtn.setPreferredSize(new Dimension(80, 36));
        applyBtn.setFocusPainted(false);
        applyBtn.addActionListener(e -> applyCustomDeck());
        customRow.add(applyBtn);

        center.add(customRow);
        center.add(Box.createVerticalStrut(4));

        deckErrorLabel = makeLabel(" ", 13, Font.PLAIN, ERROR_RED);
        deckErrorLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        center.add(deckErrorLabel);
        center.add(Box.createVerticalStrut(10));

        // ---- Port (host only, shown/hidden dynamically) ----
        portField = new JTextField(String.valueOf(GameHost.DEFAULT_PORT), 6);
        portField.setFont(new Font("SansSerif", Font.PLAIN, 18));
        portField.setPreferredSize(new Dimension(100, 36));
        portField.setHorizontalAlignment(JTextField.CENTER);
        // Port row will be added dynamically

        // ---- Start button ----
        center.add(Box.createVerticalStrut(6));
        JButton startBtn = makeBigButton("START GAME", SUCCESS_GREEN);
        startBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        startBtn.addActionListener(e -> handleStart());
        center.add(startBtn);
        center.add(Box.createVerticalStrut(8));

        statusLabel = makeLabel(" ", 15, Font.PLAIN, TEXT_DIM);
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        center.add(statusLabel);

        screen.add(center);

        // Initial state
        updateConfigUI();
        return screen;
    }

    private void showConfigScreen() {
        updateConfigUI();
        // In HOST mode, player 2 joins remotely — only show host's own name
        if (player2Col != null) {
            player2Col.setVisible(selectedMode == Mode.LOCAL);
        }
        cardLayout.show(contentPanel, "CONFIG");
    }

    private void updateConfigUI() {
        matchSizeLabel.setText(String.valueOf(matchSize));

        int[] sizes = getSuggestedDeckSizes(matchSize);
        String[] labels = {"Small", "Medium", "Large"};

        for (int i = 0; i < 3; i++) {
            suggestionButtons[i].setText("<html><center>" + sizes[i] + " cards<br><font size=2>" + labels[i] + "</font></center></html>");
            styleUnselected(suggestionButtons[i]);
        }

        // Auto-select medium
        selectedDeckSize = sizes[1];
        styleSelected(suggestionButtons[1]);
        deckErrorLabel.setText(" ");
        customDeckField.setText("");
    }

    private int[] getSuggestedDeckSizes(int n) {
        int small = (int) (Math.ceil(12.0 / n) * n);
        if (small < n * 2) small = n * 2;
        int medium = (int) (Math.ceil(20.0 / n) * n);
        int large = (int) Math.min(45, Math.ceil(30.0 / n) * n);

        if (medium == small) medium = small + n;
        if (large == medium) large = medium + n;
        if (large > 45) large = (45 / n) * n;

        return new int[]{small, medium, large};
    }

    private void selectSuggestion(int idx) {
        int[] sizes = getSuggestedDeckSizes(matchSize);
        selectedDeckSize = sizes[idx];
        for (int i = 0; i < 3; i++) {
            if (i == idx) styleSelected(suggestionButtons[i]);
            else styleUnselected(suggestionButtons[i]);
        }
        deckErrorLabel.setText(" ");
        customDeckField.setText("");
    }

    private void applyCustomDeck() {
        String text = customDeckField.getText().trim();
        if (text.isEmpty()) return;
        try {
            int size = Integer.parseInt(text);
            if (size % matchSize != 0) {
                int lower = (size / matchSize) * matchSize;
                int upper = lower + matchSize;
                deckErrorLabel.setText("Must be divisible by " + matchSize + ". Try " + lower + " or " + upper);
                selectedDeckSize = -1;
                return;
            }
            GameConfig.validate(matchSize, size);
            selectedDeckSize = size;
            deckErrorLabel.setText(" ");
            for (JButton b : suggestionButtons) styleUnselected(b);
        } catch (NumberFormatException ex) {
            deckErrorLabel.setText("Enter a valid number");
            selectedDeckSize = -1;
        } catch (IllegalArgumentException ex) {
            deckErrorLabel.setText(ex.getMessage());
            selectedDeckSize = -1;
        }
    }

    private void handleStart() {
        if (selectedDeckSize <= 0) {
            deckErrorLabel.setText("Please select or enter a valid deck size");
            return;
        }
        try {
            GameConfig.validate(matchSize, selectedDeckSize);
        } catch (IllegalArgumentException ex) {
            deckErrorLabel.setText(ex.getMessage());
            return;
        }

        if (selectedMode == Mode.LOCAL) {
            onLocalGame.accept(matchSize, selectedDeckSize);
        } else if (selectedMode == Mode.HOST) {
            statusLabel.setText("Waiting for Player 2 to connect...");
            statusLabel.setForeground(ACCENT_CYAN);
            onHost.accept(matchSize, selectedDeckSize);
        }
    }

    private void styleSelected(JButton btn) {
        btn.setBackground(ACCENT_PURPLE);
        btn.setForeground(Color.BLACK);
        btn.setBorder(BorderFactory.createLineBorder(ACCENT_PURPLE.brighter(), 2));
        btn.setBorderPainted(true);
    }

    private void styleUnselected(JButton btn) {
        btn.setBackground(Color.WHITE); // Make background light so black text is readable
        btn.setForeground(Color.BLACK);
        btn.setBorder(BorderFactory.createLineBorder(new Color(60, 60, 90), 1));
        btn.setBorderPainted(true);
    }

    // ======================== SCREEN 3: JOIN ========================

    private JPanel buildJoinScreen() {
        JPanel screen = new JPanel(new GridBagLayout());
        screen.setBackground(BG_COLOR);

        JPanel center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.setOpaque(false);

        JButton backBtn = makeBackButton();
        backBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        center.add(backBtn);
        center.add(Box.createVerticalStrut(20));

        JLabel joinTitle = makeLabel("Join a Game", 32, Font.BOLD, ACCENT_PINK);
        joinTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        center.add(joinTitle);
        center.add(Box.createVerticalStrut(10));

        JLabel joinDesc = makeLabel("Enter your name and the host's connection details", 16, Font.PLAIN, TEXT_DIM);
        joinDesc.setAlignmentX(Component.CENTER_ALIGNMENT);
        center.add(joinDesc);
        center.add(Box.createVerticalStrut(24));

        // Your name
        JLabel yourNameLabel = makeLabel("Your Name", 18, Font.BOLD, ACCENT_PINK);
        yourNameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        center.add(yourNameLabel);
        center.add(Box.createVerticalStrut(8));

        joinNameField = new JTextField("Player 2", 14);
        joinNameField.setFont(new Font("SansSerif", Font.PLAIN, 20));
        joinNameField.setMaximumSize(new Dimension(280, 44));
        joinNameField.setHorizontalAlignment(JTextField.CENTER);
        joinNameField.setAlignmentX(Component.CENTER_ALIGNMENT);
        center.add(joinNameField);
        center.add(Box.createVerticalStrut(20));

        // IP field
        JLabel ipLabel = makeLabel("Host IP Address", 18, Font.BOLD, TEXT_WHITE);
        ipLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        center.add(ipLabel);
        center.add(Box.createVerticalStrut(8));

        JTextField ipField = new JTextField(16);
        ipField.setFont(new Font("Monospaced", Font.PLAIN, 22));
        ipField.setMaximumSize(new Dimension(320, 44));
        ipField.setHorizontalAlignment(JTextField.CENTER);
        ipField.setAlignmentX(Component.CENTER_ALIGNMENT);
        center.add(ipField);
        center.add(Box.createVerticalStrut(20));

        // Port field
        JLabel portLabel = makeLabel("Port", 18, Font.BOLD, TEXT_WHITE);
        portLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        center.add(portLabel);
        center.add(Box.createVerticalStrut(8));

        JTextField joinPort = new JTextField(String.valueOf(GameHost.DEFAULT_PORT), 8);
        joinPort.setFont(new Font("Monospaced", Font.PLAIN, 22));
        joinPort.setMaximumSize(new Dimension(200, 44));
        joinPort.setHorizontalAlignment(JTextField.CENTER);
        joinPort.setAlignmentX(Component.CENTER_ALIGNMENT);
        center.add(joinPort);
        center.add(Box.createVerticalStrut(30));

        // Connect button
        JButton connectBtn = makeBigButton("CONNECT", ACCENT_PINK);
        connectBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        center.add(connectBtn);
        center.add(Box.createVerticalStrut(10));

        JLabel joinStatus = makeLabel(" ", 15, Font.PLAIN, TEXT_DIM);
        joinStatus.setAlignmentX(Component.CENTER_ALIGNMENT);
        center.add(joinStatus);

        connectBtn.addActionListener(e -> {
            String addr = ipField.getText().trim();
            if (addr.isEmpty()) {
                joinStatus.setText("Please enter the host IP address");
                joinStatus.setForeground(ERROR_RED);
                return;
            }
            String joinName = joinNameField.getText().trim();
            if (joinName.isEmpty()) joinName = "Player 2";
            try {
                int port = Integer.parseInt(joinPort.getText().trim());
                joinStatus.setText("Connecting to " + addr + ":" + port + "...");
                joinStatus.setForeground(ACCENT_PINK);
                onJoin.accept(joinName, addr, port);
            } catch (NumberFormatException ex) {
                joinStatus.setText("Invalid port number");
                joinStatus.setForeground(ERROR_RED);
            }
        });

        screen.add(center);
        return screen;
    }

    // ======================== SHARED WIDGET BUILDERS ========================

    private JLabel makeLabel(String text, int size, int style, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("SansSerif", style, size));
        label.setForeground(color);
        return label;
    }

    private JButton makeBackButton() {
        JButton btn = new JButton("\u2190  Back");
        btn.setFont(new Font("SansSerif", Font.BOLD, 16));
        btn.setForeground(TEXT_DIM);
        btn.setBackground(CARD_BG);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(110, 38));
        btn.addActionListener(e -> cardLayout.show(contentPanel, "MODE"));
        return btn;
    }

    private JButton makeBigButton(String text, Color color) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("SansSerif", Font.BOLD, 20));
        btn.setForeground(Color.WHITE);
        btn.setBackground(color);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(260, 52));
        btn.setMaximumSize(new Dimension(260, 52));
        return btn;
    }

    private JButton makeRoundButton(String text, int size, Color color) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("SansSerif", Font.BOLD, 22));
        btn.setForeground(color);
        btn.setBackground(CARD_BG);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createLineBorder(color.darker(), 1));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(size, size));
        return btn;
    }

    // ======================== PUBLIC API ========================

    public void setStatus(String text, Color color) {
        SwingUtilities.invokeLater(() -> {
            if (statusLabel != null) {
                statusLabel.setText(text);
                statusLabel.setForeground(color);
            }
        });
    }

    public int getPort() {
        try {
            return Integer.parseInt(portField.getText().trim());
        } catch (Exception e) {
            return GameHost.DEFAULT_PORT;
        }
    }
}
