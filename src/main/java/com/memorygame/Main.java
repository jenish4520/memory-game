package com.memorygame;

import com.memorygame.ui.GameWindow;

import javax.swing.*;

/**
 * Entry point for the Memory Game application.
 * Launches the game window on the Swing EDT.
 */
public class Main {

    public static void main(String[] args) {
        // Set system look and feel for native appearance
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
            // Fall back to default L&F
        }

        // Launch on EDT
        SwingUtilities.invokeLater(() -> {
            GameWindow window = new GameWindow();
            window.setVisible(true);
        });
    }
}
