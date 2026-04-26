package com.memorygame.ui;

import com.memorygame.logic.GameLogic;
import com.memorygame.model.GameConfig;
import com.memorygame.model.GamePhase;
import com.memorygame.model.GameState;
import com.memorygame.network.GameClient;
import com.memorygame.network.GameHost;
import com.memorygame.network.GameMessage;
import com.memorygame.network.MessageType;

import javax.swing.*;
import java.awt.*;

/**
 * Main application window and controller.
 * Manages transitions between menu and game screens, and
 * coordinates between game logic and network layers.
 */
public class GameWindow extends JFrame {

    private static final Color BG_COLOR = new Color(26, 26, 46);

    private MenuPanel menuPanel;
    private GamePanel gamePanel;

    // Game state (host only)
    private GameLogic gameLogic;
    private GameConfig currentConfig;

    // Network
    private GameHost gameHost;
    private GameClient gameClient;
    private boolean isHost;
    private boolean isLocalMode;
    private int localPlayerNumber;
    private volatile boolean connected;
    private volatile GamePhase clientPhase = GamePhase.PLAYING; // tracks phase on client side
    private volatile int clientActivePlayer = 1; // tracks whose turn it is on client side
    private String player1Name = "Player 1";
    private String player2Name = "Player 2";

    // Mismatch timer
    private Timer mismatchTimer;

    public GameWindow() {
        super("Memory Game - LAN Multiplayer");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1100, 750);
        setMinimumSize(new Dimension(900, 650));
        setLocationRelativeTo(null);
        getContentPane().setBackground(BG_COLOR);

        showMenu();

        // Clean up on close
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                cleanup();
            }
        });
    }

    private void showMenu() {
        menuPanel = new MenuPanel(
            this::startLocalGame,  // onLocalGame(matchSize, deckSize)
            this::startHosting,    // onHost(matchSize, deckSize)
            this::startJoining     // onJoin(playerName, hostAddress, port)
        );

        getContentPane().removeAll();
        getContentPane().add(menuPanel);
        revalidate();
        repaint();
    }

    private void showGame() {
        gamePanel = new GamePanel(
            localPlayerNumber,
            isLocalMode,
            player1Name,
            player2Name,
            this::onCardClicked,
            this::onRestartRequested,
            this::onBackToMenu
        );

        getContentPane().removeAll();
        getContentPane().add(gamePanel);
        revalidate();
        repaint();
    }

    // ======================== LOCAL MODE ========================

    private void startLocalGame(int matchSize, int deckSize) {
        isHost = true;
        isLocalMode = true;
        localPlayerNumber = 0; // both players share screen
        connected = true;
        clientPhase = GamePhase.PLAYING;
        clientActivePlayer = 1;
        player1Name = menuPanel.getPlayer1Name();
        player2Name = menuPanel.getPlayer2Name();
        currentConfig = new GameConfig(matchSize, deckSize);
        gameLogic = new GameLogic();
        gameLogic.initializeGame(currentConfig);
        GameState state = gameLogic.getState("Player 1's turn!");

        showGame();
        gamePanel.updateState(state);
    }

    // ======================== HOST MODE ========================

    private void startHosting(int matchSize, int deckSize) {
        isHost = true;
        isLocalMode = false;
        localPlayerNumber = 1;
        clientPhase = GamePhase.PLAYING;
        clientActivePlayer = 1;
        // Only host's own name is known at this point; player 2 name set to default
        player1Name = menuPanel.getPlayer1Name();
        player2Name = "Player 2";
        currentConfig = new GameConfig(matchSize, deckSize);
        gameLogic = new GameLogic();

        int port = menuPanel.getPort();

        // Start hosting in background
        new Thread(() -> {
            try {
                gameHost = new GameHost(
                    this::onHostReceivedMessage,
                    this::onConnectionLost
                );

                SwingUtilities.invokeLater(() ->
                    menuPanel.setStatus("Waiting for player 2 on port " + port +
                        " (IP: " + gameHost.getHostAddress() + ")...",
                        new Color(0, 210, 255)));

                gameHost.startAndWaitForClient(port);
                connected = true;

                // Read the joining player's chosen name
                player2Name = gameHost.getClientPlayerName();

                // Player connected! Start the game
                gameLogic.initializeGame(currentConfig);
                GameState state = gameLogic.getState("Game started! Player 1's turn.");

                // Send game start to client (includes both player names)
                gameHost.sendMessage(GameMessage.gameStart(state, currentConfig, player1Name, player2Name));

                // Show game UI
                SwingUtilities.invokeLater(() -> {
                    showGame();
                    gamePanel.updateState(state);
                });

            } catch (Exception e) {
                SwingUtilities.invokeLater(() ->
                    menuPanel.setStatus("Error: " + e.getMessage(), new Color(231, 76, 60)));
            }
        }, "host-setup").start();
    }

    private void onHostReceivedMessage(GameMessage msg) {
        switch (msg.getType()) {
            case CARD_CLICK -> handleRemoteCardClick(msg.getCardIndex());
            case RESTART_REQUEST -> handleRestartRequest();
            default -> { /* ignore heartbeats and unknown messages */ }
        }
    }

    private void handleRemoteCardClick(int cardIndex) {
        // Client (player 2) clicked a card
        if (!isHost) return;

        boolean valid = gameLogic.handleCardClick(cardIndex, 2);
        if (!valid) return;

        GameState state = gameLogic.getState();
        broadcastState(state);

        // Check if we need to resolve a mismatch
        if (state.getPhase() == GamePhase.RESOLVING) {
            scheduleMismatchResolution();
        }
    }

    // ======================== CLIENT MODE ========================

    private void startJoining(String joinPlayerName, String hostAddress, int port) {
        isHost = false;
        isLocalMode = false;
        clientPhase = GamePhase.PLAYING;
        clientActivePlayer = 1;
        // Store the joining player's own name until the host confirms both names
        player2Name = (joinPlayerName == null || joinPlayerName.isBlank()) ? "Player 2" : joinPlayerName;

        new Thread(() -> {
            try {
                gameClient = new GameClient(
                    this::onClientReceivedMessage,
                    this::onConnectionLost
                );

                localPlayerNumber = gameClient.connect(hostAddress, port, player2Name);
                connected = true;

                SwingUtilities.invokeLater(() ->
                    menuPanel.setStatus("Connected as Player " + localPlayerNumber +
                        ". Waiting for game to start...", new Color(255, 107, 157)));

            } catch (Exception e) {
                SwingUtilities.invokeLater(() ->
                    menuPanel.setStatus("Connection failed: " + e.getMessage(),
                        new Color(231, 76, 60)));
            }
        }, "client-connect").start();
    }

    private void onClientReceivedMessage(GameMessage msg) {
        switch (msg.getType()) {
            case GAME_START -> {
                currentConfig = msg.getConfig();
                clientPhase = GamePhase.PLAYING;
                clientActivePlayer = 1;
                // Read authoritative names from host
                String p1 = msg.getP1Name();
                String p2 = msg.getP2Name();
                if (p1 != null && !p1.isBlank()) player1Name = p1;
                if (p2 != null && !p2.isBlank()) player2Name = p2;
                SwingUtilities.invokeLater(() -> {
                    showGame(); // uses updated player1Name / player2Name
                    GameState gs = msg.getGameState();
                    if (gs != null) {
                        clientPhase = gs.getPhase();
                        clientActivePlayer = gs.getActivePlayer();
                        gamePanel.updateState(gs);
                    }
                });
            }
            case STATE_UPDATE -> {
                GameState gs = msg.getGameState();
                if (gs != null) {
                    clientPhase = gs.getPhase();
                    clientActivePlayer = gs.getActivePlayer();
                }
                SwingUtilities.invokeLater(() -> {
                    if (gamePanel != null && gs != null) {
                        gamePanel.updateState(gs);
                    }
                });
            }
            case GAME_END -> {
                GameState gs = msg.getGameState();
                if (gs != null) {
                    clientPhase = gs.getPhase();
                    clientActivePlayer = gs.getActivePlayer();
                }
                SwingUtilities.invokeLater(() -> {
                    if (gamePanel != null && gs != null) {
                        gamePanel.updateState(gs);
                    }
                });
            }
            case RESTART_CONFIRMED -> {
                currentConfig = msg.getConfig();
                clientPhase = GamePhase.PLAYING;
                clientActivePlayer = 1;
                // Refresh names in case they changed (defensive)
                String p1 = msg.getP1Name();
                String p2 = msg.getP2Name();
                if (p1 != null && !p1.isBlank()) player1Name = p1;
                if (p2 != null && !p2.isBlank()) player2Name = p2;
                GameState gs = msg.getGameState();
                SwingUtilities.invokeLater(() -> {
                    if (gamePanel != null && gs != null) {
                        gamePanel.updateState(gs);
                    }
                });
            }
            case ERROR -> {
                SwingUtilities.invokeLater(() -> {
                    if (gamePanel != null) {
                        gamePanel.showError(msg.getMessage());
                    }
                });
            }
            default -> { /* ignore */ }
        }
    }

    // ======================== CARD CLICK HANDLING ========================

    private void onCardClicked(int cardIndex) {
        if (!connected) return;

        if (isLocalMode) {
            // Local mode: attribute click to whoever's turn it is
            int activePlayer = gameLogic.getActivePlayer();
            boolean valid = gameLogic.handleCardClick(cardIndex, activePlayer);
            if (!valid) return;

            GameState state = gameLogic.getState();
            broadcastState(state);

            if (state.getPhase() == GamePhase.RESOLVING) {
                scheduleMismatchResolution();
            }
        } else if (isHost) {
            // Host (player 1) clicked - validate it's their turn
            if (clientPhase == GamePhase.RESOLVING) return;
            if (clientActivePlayer != localPlayerNumber) return;
            boolean valid = gameLogic.handleCardClick(cardIndex, localPlayerNumber);
            if (!valid) return;

            GameState state = gameLogic.getState();
            clientPhase = state.getPhase();
            clientActivePlayer = state.getActivePlayer();
            broadcastState(state);

            if (state.getPhase() == GamePhase.RESOLVING) {
                scheduleMismatchResolution();
            }
        } else {
            // Client (player 2) - block clicks when not their turn or during resolving
            if (clientPhase == GamePhase.RESOLVING) return;
            if (clientActivePlayer != localPlayerNumber) return;
            // Send click to host for authoritative processing
            gameClient.sendMessage(GameMessage.cardClick(cardIndex));
        }
    }

    // ======================== STATE BROADCASTING ========================

    private void broadcastState(GameState state) {
        // Update host's own UI
        SwingUtilities.invokeLater(() -> {
            if (gamePanel != null) {
                gamePanel.updateState(state);
            }
        });

        // Send to client (skip in local mode)
        if (isHost && !isLocalMode && gameHost != null) {
            if (state.getPhase() == GamePhase.GAME_OVER) {
                gameHost.sendMessage(GameMessage.gameEnd(state));
            } else {
                gameHost.sendMessage(GameMessage.stateUpdate(state));
            }
        }
    }

    // ======================== MISMATCH RESOLUTION ========================

    private void scheduleMismatchResolution() {
        if (mismatchTimer != null) {
            mismatchTimer.stop();
        }

        mismatchTimer = new Timer(1000, e -> {
            gameLogic.resolveMismatch();
            GameState newState = gameLogic.getState();
            // Keep host-side turn tracking in sync
            clientPhase = newState.getPhase();
            clientActivePlayer = newState.getActivePlayer();
            broadcastState(newState);
        });
        mismatchTimer.setRepeats(false);
        mismatchTimer.start();
    }

    // ======================== RESTART ========================

    private void onRestartRequested() {
        if (isHost) {
            performRestart();
        } else {
            gameClient.sendMessage(GameMessage.restartRequest());
        }
    }

    private void handleRestartRequest() {
        performRestart();
    }

    private void performRestart() {
        if (!isHost || gameLogic == null) return;

        gameLogic.initializeGame(currentConfig);
        GameState state = gameLogic.getState("New round! Player 1's turn.");
        clientPhase = state.getPhase();
        clientActivePlayer = state.getActivePlayer();

        // Send restart confirmation to client (skip in local mode)
        if (!isLocalMode && gameHost != null) {
            gameHost.sendMessage(GameMessage.restartConfirmed(state, currentConfig, player1Name, player2Name));
        }

        // Update host UI
        SwingUtilities.invokeLater(() -> {
            if (gamePanel != null) {
                gamePanel.updateState(state);
            }
        });
    }

    // ======================== CONNECTION LOSS ========================

    private void onConnectionLost() {
        connected = false;
        SwingUtilities.invokeLater(() -> {
            if (gamePanel != null) {
                gamePanel.disableInput();
                gamePanel.showError("Connection lost! Please restart the application.");
            } else if (menuPanel != null) {
                menuPanel.setStatus("Connection lost.", new Color(231, 76, 60));
            }
        });
    }

    // ======================== CLEANUP & NAVIGATION ========================

    private void onBackToMenu() {
        cleanup();
        showMenu();
    }

    private void cleanup() {
        connected = false;
        if (mismatchTimer != null) mismatchTimer.stop();
        if (gameHost != null) gameHost.close();
        if (gameClient != null) gameClient.close();
        gameHost = null;
        gameClient = null;
        gameLogic = null;
    }
}
