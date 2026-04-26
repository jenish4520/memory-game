package com.memorygame.ui;

import com.memorygame.model.Card;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.function.IntConsumer;

/**
 * Custom Swing component rendering a single memory card.
 * Supports face-down, face-up, matched, and hover states with smooth visuals.
 */
public class CardComponent extends JPanel {

    // Card back colors
    private static final Color CARD_BACK_TOP = new Color(108, 52, 131);     // #6c3483
    private static final Color CARD_BACK_BOTTOM = new Color(142, 68, 173);  // #8e44ad
    private static final Color CARD_BACK_PATTERN = new Color(128, 58, 152, 40);

    // Card face colors
    private static final Color CARD_FACE_BG = new Color(253, 246, 227);     // cream
    private static final Color CARD_FACE_BORDER = new Color(200, 190, 170);

    // Matched card
    private static final Color MATCHED_OVERLAY = new Color(46, 204, 113, 60);  // green tint
    private static final Color MATCHED_BORDER = new Color(46, 204, 113);

    // Highlight for current attempt
    private static final Color ATTEMPT_BORDER = new Color(255, 215, 0);     // gold

    // Hover
    private static final Color HOVER_GLOW = new Color(0, 210, 255, 50);    // cyan glow

    // Symbol colors (cycled based on symbol hash)
    private static final Color[] SYMBOL_COLORS = {
        new Color(231, 76, 60),    // red
        new Color(52, 152, 219),   // blue
        new Color(46, 204, 113),   // green
        new Color(155, 89, 182),   // purple
        new Color(241, 196, 15),   // yellow
        new Color(230, 126, 34),   // orange
        new Color(26, 188, 156),   // teal
        new Color(236, 72, 153),   // pink
        new Color(99, 102, 241),   // indigo
        new Color(20, 184, 166),   // cyan-dark
    };

    private Card card;
    private boolean hovered;
    private boolean inCurrentAttempt;
    private final int index;

    public CardComponent(int index, IntConsumer onClick) {
        this.index = index;
        this.hovered = false;
        this.inCurrentAttempt = false;

        setPreferredSize(new Dimension(90, 110));
        setOpaque(false);
        setCursor(new Cursor(Cursor.HAND_CURSOR));

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (card != null && card.isClickable()) {
                    onClick.accept(index);
                }
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                hovered = true;
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hovered = false;
                repaint();
            }
        });
    }

    public void updateCard(Card card, boolean inCurrentAttempt) {
        this.card = card;
        this.inCurrentAttempt = inCurrentAttempt;
        setCursor(card != null && card.isClickable()
            ? new Cursor(Cursor.HAND_CURSOR)
            : new Cursor(Cursor.DEFAULT_CURSOR));
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (card == null) return;

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);

        int w = getWidth() - 4;
        int h = getHeight() - 4;
        int x = 2;
        int y = 2;
        int arc = 14;

        RoundRectangle2D.Float cardShape = new RoundRectangle2D.Float(x, y, w, h, arc, arc);

        if (card.isMatched()) {
            drawMatchedCard(g2, cardShape, x, y, w, h);
        } else if (card.isFaceUp()) {
            drawFaceUpCard(g2, cardShape, x, y, w, h);
        } else {
            drawFaceDownCard(g2, cardShape, x, y, w, h);
        }

        // Hover glow
        if (hovered && card.isClickable()) {
            g2.setColor(HOVER_GLOW);
            g2.setStroke(new BasicStroke(3f));
            g2.draw(cardShape);
        }

        g2.dispose();
    }

    private void drawFaceDownCard(Graphics2D g2, RoundRectangle2D.Float shape, int x, int y, int w, int h) {
        // Gradient background
        GradientPaint gp = new GradientPaint(x, y, CARD_BACK_TOP, x, y + h, CARD_BACK_BOTTOM);
        g2.setPaint(gp);
        g2.fill(shape);

        // Diamond pattern overlay
        g2.setColor(CARD_BACK_PATTERN);
        int spacing = 16;
        for (int px = x; px < x + w; px += spacing) {
            for (int py = y; py < y + h; py += spacing) {
                g2.fillRect(px + spacing / 4, py + spacing / 4, spacing / 2, spacing / 2);
            }
        }

        // Center question mark
        g2.setColor(new Color(255, 255, 255, 120));
        g2.setFont(new Font("SansSerif", Font.BOLD, 32));
        FontMetrics fm = g2.getFontMetrics();
        String text = "?";
        int tx = x + (w - fm.stringWidth(text)) / 2;
        int ty = y + (h + fm.getAscent() - fm.getDescent()) / 2;
        g2.drawString(text, tx, ty);

        // Border
        g2.setColor(new Color(80, 30, 100));
        g2.setStroke(new BasicStroke(1.5f));
        g2.draw(shape);
    }

    private void drawFaceUpCard(Graphics2D g2, RoundRectangle2D.Float shape, int x, int y, int w, int h) {
        // White background
        g2.setColor(CARD_FACE_BG);
        g2.fill(shape);

        // Draw symbol
        drawSymbol(g2, x, y, w, h);

        // Border - gold if in current attempt
        if (inCurrentAttempt) {
            g2.setColor(ATTEMPT_BORDER);
            g2.setStroke(new BasicStroke(3f));
        } else {
            g2.setColor(CARD_FACE_BORDER);
            g2.setStroke(new BasicStroke(1.5f));
        }
        g2.draw(shape);
    }

    private void drawMatchedCard(Graphics2D g2, RoundRectangle2D.Float shape, int x, int y, int w, int h) {
        // White background
        g2.setColor(CARD_FACE_BG);
        g2.fill(shape);

        // Draw symbol (no fading, as emojis disappear with alpha composites)
        drawSymbol(g2, x, y, w, h);

        // Player number in top right corner
        int player = card.getMatchedByPlayer();
        if (player > 0) {
            g2.setColor(player == 1 ? new Color(0, 180, 220) : new Color(220, 80, 130));
            g2.setFont(new Font("SansSerif", Font.BOLD, 20));
            g2.drawString(String.valueOf(player), x + w - 22, y + 24);
        } else {
            // Checkmark as fallback
            g2.setColor(MATCHED_BORDER);
            g2.setFont(new Font("SansSerif", Font.BOLD, 18));
            g2.drawString("\u2713", x + w - 22, y + 22);
        }

        // Border
        g2.setColor(MATCHED_BORDER);
        g2.setStroke(new BasicStroke(3f));
        g2.draw(shape);
    }

    private void drawSymbol(Graphics2D g2, int x, int y, int w, int h) {
        if (card.getSymbol() == null) return;

        Color symColor = SYMBOL_COLORS[Math.abs(card.getSymbol().hashCode()) % SYMBOL_COLORS.length];
        g2.setColor(symColor);
        g2.setFont(new Font("SansSerif", Font.BOLD, 40));
        FontMetrics fm = g2.getFontMetrics();
        String text = card.getSymbol();
        int tx = x + (w - fm.stringWidth(text)) / 2;
        int ty = y + (h + fm.getAscent() - fm.getDescent()) / 2;
        g2.drawString(text, tx, ty);
    }
}
