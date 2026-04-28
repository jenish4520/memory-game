package com.memorygame.ui;

import com.memorygame.model.Card;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.HashMap;
import java.util.Map;
import java.util.function.IntConsumer;

/**
 * Custom Swing component rendering a single memory card.
 * Supports face-down, face-up, matched, and hover states with smooth visuals.
 */
public class CardComponent extends JPanel {

    private static final Map<String, Image> imageCache = new HashMap<>();

    private static Image getImage(String name) {
        if (name == null) return null;
        return imageCache.computeIfAbsent(name, k -> {
            try {
                java.net.URL url = CardComponent.class.getResource("/images/" + k + ".png");
                if (url != null) {
                    return ImageIO.read(url);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        });
    }

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

    private boolean targetFaceUp = false;
    private boolean isFaceUpVisual = false;
    private double flipProgress = 1.0;
    private Timer flipTimer;

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
        boolean newFaceUp = (card != null) ? (card.isFaceUp() || card.isMatched()) : false;
        
        this.card = card;
        this.inCurrentAttempt = inCurrentAttempt;
        setCursor(card != null && card.isClickable()
            ? new Cursor(Cursor.HAND_CURSOR)
            : new Cursor(Cursor.DEFAULT_CURSOR));

        if (newFaceUp != targetFaceUp) {
            targetFaceUp = newFaceUp;
            if (flipTimer != null && flipTimer.isRunning()) {
                flipTimer.stop();
            }
            flipProgress = 0.0;
            flipTimer = new Timer(15, e -> {
                flipProgress += 0.08;
                if (flipProgress >= 1.0) {
                    flipProgress = 1.0;
                    isFaceUpVisual = targetFaceUp;
                    flipTimer.stop();
                } else if (flipProgress >= 0.5) {
                    isFaceUpVisual = targetFaceUp;
                }
                repaint();
            });
            flipTimer.start();
        } else if (flipProgress >= 1.0) {
            isFaceUpVisual = targetFaceUp;
            repaint();
        }
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

        double scaleX = 1.0;
        if (flipProgress < 1.0) {
            scaleX = Math.abs(Math.cos(flipProgress * Math.PI));
        }

        double centerX = getWidth() / 2.0;
        g2.translate(centerX, 0);
        g2.scale(scaleX, 1.0);
        g2.translate(-centerX, 0);

        RoundRectangle2D.Float cardShape = new RoundRectangle2D.Float(x, y, w, h, arc, arc);

        if (card.isMatched() && isFaceUpVisual) {
            drawMatchedCard(g2, cardShape, x, y, w, h);
        } else if (isFaceUpVisual) {
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

        // Center logo
        Image logo = getImage("logo");
        if (logo != null) {
            int logoW = Math.min(w - 30, logo.getWidth(null));
            int logoH = logo.getHeight(null) * logoW / Math.max(1, logo.getWidth(null));
            int tx = x + (w - logoW) / 2;
            int ty = y + (h - logoH) / 2;
            g2.drawImage(logo, tx, ty, logoW, logoH, null);
        } else {
            // Fallback question mark
            g2.setColor(new Color(255, 255, 255, 120));
            g2.setFont(new Font("SansSerif", Font.BOLD, 32));
            FontMetrics fm = g2.getFontMetrics();
            String text = "?";
            int tx = x + (w - fm.stringWidth(text)) / 2;
            int ty = y + (h + fm.getAscent() - fm.getDescent()) / 2;
            g2.drawString(text, tx, ty);
        }

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
        if (player == 2) {
            g2.setColor(new Color(231, 76, 60)); // Red
        } else {
            g2.setColor(MATCHED_BORDER); // Green
        }
        g2.setStroke(new BasicStroke(3f));
        g2.draw(shape);
    }

    private void drawSymbol(Graphics2D g2, int x, int y, int w, int h) {
        if (card.getSymbol() == null) return;

        Image img = getImage(card.getSymbol());
        if (img != null) {
            int imgW = w - 10;
            int imgH = img.getHeight(null) * imgW / Math.max(1, img.getWidth(null));
            if (imgH > h - 10) {
                imgH = h - 10;
                imgW = img.getWidth(null) * imgH / Math.max(1, img.getHeight(null));
            }
            int tx = x + (w - imgW) / 2;
            int ty = y + (h - imgH) / 2;
            g2.drawImage(img, tx, ty, imgW, imgH, null);
        } else {
            Color symColor = SYMBOL_COLORS[Math.abs(card.getSymbol().hashCode()) % SYMBOL_COLORS.length];
            g2.setColor(symColor);
            String text = card.getSymbol();
            
            int fontSize = 20;
            g2.setFont(new Font("SansSerif", Font.BOLD, fontSize));
            FontMetrics fm = g2.getFontMetrics();
            while (fm.stringWidth(text) > w - 10 && fontSize > 8) {
                fontSize--;
                g2.setFont(new Font("SansSerif", Font.BOLD, fontSize));
                fm = g2.getFontMetrics();
            }

            int tx = x + (w - fm.stringWidth(text)) / 2;
            int ty = y + (h + fm.getAscent() - fm.getDescent()) / 2;
            g2.drawString(text, tx, ty);
        }
    }
}
