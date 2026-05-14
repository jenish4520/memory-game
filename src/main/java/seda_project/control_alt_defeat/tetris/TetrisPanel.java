package seda_project.control_alt_defeat.tetris;

import javafx.application.Platform;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import java.util.HashSet;
import java.util.Set;

public class TetrisPanel extends StackPane {

    private Canvas canvas;
    private GameLogic logic;
    private boolean running = false;
    private Thread loopThread;
    
    private int CELL = 14;
    private int BOARD_W = Board.WIDTH * CELL;
    private int BOARD_H = Board.HEIGHT * CELL;
    
    private boolean isClient = false;
    private java.util.function.Consumer<TetrisMessage.Type> outMessage;
    
    // Key states
    private Set<KeyCode> activeKeys = new HashSet<>();
    private long lastDAS_P1 = 0;
    private long lastDAS_P2 = 0;
    private long lastRepeat_P1 = 0;
    private long lastRepeat_P2 = 0;
    private boolean initialDAS_P1 = false;
    private boolean initialDAS_P2 = false;
    
    public TetrisPanel(GameLogic logic, Runnable onBack) {
        this.logic = logic;
        canvas = new Canvas(900, 650);
        
        // Bind canvas size to parent size so it resizes
        canvas.widthProperty().bind(widthProperty());
        canvas.heightProperty().bind(heightProperty());
        
        Button backBtn = new Button("← Quit Game");
        backBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px;");
        backBtn.setOnAction(e -> {
            stop();
            onBack.run();
        });
        StackPane.setAlignment(backBtn, javafx.geometry.Pos.TOP_LEFT);
        StackPane.setMargin(backBtn, new javafx.geometry.Insets(10));
        
        getChildren().addAll(canvas, backBtn);
        
        setFocusTraversable(true);
        setOnMouseClicked(e -> requestFocus());
        setOnKeyPressed(e -> activeKeys.add(e.getCode()));
        setOnKeyReleased(e -> {
            activeKeys.remove(e.getCode());
            if (e.getCode() == KeyCode.LEFT || e.getCode() == KeyCode.RIGHT) {
                initialDAS_P1 = false;
            }
            if (e.getCode() == KeyCode.A || e.getCode() == KeyCode.D) {
                initialDAS_P2 = false;
            }
        });
    }
    
    public void setNetworkMode(boolean isClient, java.util.function.Consumer<TetrisMessage.Type> outMessage) {
        this.isClient = isClient;
        this.outMessage = outMessage;
    }
    
    public void updateState(GameLogic state) {
        this.logic = state;
    }
    
    public void start() {
        Platform.runLater(this::requestFocus);
        running = true;
        loopThread = new Thread(() -> {
            long lastTime = System.currentTimeMillis();
            while (running) {
                long now = System.currentTimeMillis();
                
                handleInput(now);
                logic.update(now);
                
                Platform.runLater(this::render);
                
                long elapsed = System.currentTimeMillis() - now;
                long sleepTime = 16 - elapsed;
                if (sleepTime > 0) {
                    try { Thread.sleep(sleepTime); } catch (InterruptedException e) {}
                }
            }
        });
        loopThread.setDaemon(true);
        loopThread.start();
    }
    
    public void stop() {
        running = false;
    }
    
    private void handleInput(long now) {
        if (isClient) {
            // Client DAS
            if (activeKeys.contains(KeyCode.A)) {
                if (!initialDAS_P2) {
                    outMessage.accept(TetrisMessage.Type.INPUT_LEFT);
                    initialDAS_P2 = true;
                    lastDAS_P2 = now;
                } else if (now - lastDAS_P2 > 170 && now - lastRepeat_P2 > 50) {
                    outMessage.accept(TetrisMessage.Type.INPUT_LEFT);
                    lastRepeat_P2 = now;
                }
            } else if (activeKeys.contains(KeyCode.D)) {
                if (!initialDAS_P2) {
                    outMessage.accept(TetrisMessage.Type.INPUT_RIGHT);
                    initialDAS_P2 = true;
                    lastDAS_P2 = now;
                } else if (now - lastDAS_P2 > 170 && now - lastRepeat_P2 > 50) {
                    outMessage.accept(TetrisMessage.Type.INPUT_RIGHT);
                    lastRepeat_P2 = now;
                }
            }
            if (activeKeys.contains(KeyCode.W)) {
                outMessage.accept(TetrisMessage.Type.INPUT_SOFT_DROP);
            }
            return;
        }
        
        // P1 DAS
        if (activeKeys.contains(KeyCode.LEFT)) {
            if (!initialDAS_P1) {
                logic.moveLeft(logic.p1);
                initialDAS_P1 = true;
                lastDAS_P1 = now;
            } else if (now - lastDAS_P1 > 170 && now - lastRepeat_P1 > 50) {
                logic.moveLeft(logic.p1);
                lastRepeat_P1 = now;
            }
        } else if (activeKeys.contains(KeyCode.RIGHT)) {
            if (!initialDAS_P1) {
                logic.moveRight(logic.p1);
                initialDAS_P1 = true;
                lastDAS_P1 = now;
            } else if (now - lastDAS_P1 > 170 && now - lastRepeat_P1 > 50) {
                logic.moveRight(logic.p1);
                lastRepeat_P1 = now;
            }
        }
        
        if (activeKeys.contains(KeyCode.DOWN)) {
            logic.softDrop(logic.p1);
        }
        
        // P2 DAS (A/D)
        if (activeKeys.contains(KeyCode.A)) {
            if (!initialDAS_P2) {
                logic.moveLeft(logic.p2);
                initialDAS_P2 = true;
                lastDAS_P2 = now;
            } else if (now - lastDAS_P2 > 170 && now - lastRepeat_P2 > 50) {
                logic.moveLeft(logic.p2);
                lastRepeat_P2 = now;
            }
        } else if (activeKeys.contains(KeyCode.D)) {
            if (!initialDAS_P2) {
                logic.moveRight(logic.p2);
                initialDAS_P2 = true;
                lastDAS_P2 = now;
            } else if (now - lastDAS_P2 > 170 && now - lastRepeat_P2 > 50) {
                logic.moveRight(logic.p2);
                lastRepeat_P2 = now;
            }
        }
        
        if (activeKeys.contains(KeyCode.W)) {
            logic.softDrop(logic.p2);
        }
    }
    
    // We also need one-time presses for rotation and hard drop.
    // They are better handled via key events so we don't drop inputs.
    public void setupKeyEvents() {
        setOnKeyPressed(e -> {
            activeKeys.add(e.getCode());
            if (isClient) {
                switch(e.getCode()) {
                    case S: outMessage.accept(TetrisMessage.Type.INPUT_ROTATE_CW); break;
                    case Q: outMessage.accept(TetrisMessage.Type.INPUT_ROTATE_CCW); break;
                    case SHIFT: outMessage.accept(TetrisMessage.Type.INPUT_HARD_DROP); break;
                }
                return;
            }
            
            switch(e.getCode()) {
                case UP: logic.rotateCW(logic.p1); break;
                case Z: logic.rotateCCW(logic.p1); break;
                case SPACE: logic.hardDrop(logic.p1); break;
                
                case S: logic.rotateCW(logic.p2); break;
                case Q: logic.rotateCCW(logic.p2); break;
                case SHIFT: logic.hardDrop(logic.p2); break;
            }
        });
    }
    
    private void render() {
        double w = canvas.getWidth();
        double h = canvas.getHeight();
        
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setFill(Color.web("#0f0f1e"));
        gc.fillRect(0, 0, w, h);
        
        double availableHeight = h - 20; // top/bottom padding
        CELL = (int) (availableHeight / (Board.HEIGHT * 2.05));
        if (CELL < 10) CELL = 10;
        BOARD_W = Board.WIDTH * CELL;
        BOARD_H = Board.HEIGHT * CELL;
        
        double offY2 = 10; // P2 (upwards) on top
        double offY1 = offY2 + BOARD_H + 20; // P1 (downwards) below
        double offX = w / 2 - BOARD_W / 2;
        
        drawPlayerState(gc, logic.p2, offX, offY2);
        drawPlayerState(gc, logic.p1, offX, offY1);
        
        if (logic.p1.isGameOver && logic.p2.isGameOver) {
            gc.setFill(Color.color(0, 0, 0, 0.8));
            gc.fillRect(0, 0, w, h);

            String msg = "DRAW!";
            Color c = Color.WHITE;
            if (logic.p1.score > logic.p2.score) {
                msg = logic.p1.name.toUpperCase() + " WINS!";
                c = Color.web("#00d2ff");
            } else if (logic.p2.score > logic.p1.score) {
                msg = logic.p2.name.toUpperCase() + " WINS!";
                c = Color.web("#ff6b9d");
            }
            
            gc.setFont(Font.font("SansSerif", FontWeight.BOLD, 56));
            // Shadow
            gc.setFill(Color.BLACK);
            gc.fillText(msg, w/2 - msg.length() * 15, h/2 - 20 + 4);
            // Main Text
            gc.setFill(c);
            gc.fillText(msg, w/2 - msg.length() * 15 - 4, h/2 - 20);
        }
    }
    
    private void drawPlayerState(GraphicsContext gc, PlayerState p, double offX, double offY) {
        // Draw HUD on Left
        double textX = offX - 120;
        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("SansSerif", FontWeight.BOLD, 20));
        gc.fillText(p.name, textX, offY + 20);
        
        gc.setFont(Font.font("SansSerif", 16));
        gc.setFill(Color.web("#8c8caa"));
        gc.fillText("Score", textX, offY + 60);
        gc.setFill(Color.WHITE);
        gc.fillText(String.valueOf(p.score), textX, offY + 80);
        
        gc.setFill(Color.web("#8c8caa"));
        gc.fillText("Lines", textX, offY + 120);
        gc.setFill(Color.WHITE);
        gc.fillText(String.valueOf(p.linesCleared), textX, offY + 140);
        
        // Draw Next Piece on Right
        double nextX = offX + BOARD_W + 30;
        gc.setFill(Color.web("#8c8caa"));
        gc.fillText("Next", nextX, offY + 30);
        if (p.nextPiece != null) {
            drawShape(gc, p.nextPiece.getShape(), nextX, offY + 50, p.nextPiece.colorHex, false, p.id == 2);
        }
        
        // Draw Board Bg
        gc.setFill(Color.web("#202020"));
        gc.fillRect(offX, offY, BOARD_W, BOARD_H);
        
        // Grid lines
        gc.setStroke(Color.web("#303030"));
        gc.setLineWidth(1);
        for (int i = 0; i <= Board.WIDTH; i++) {
            gc.strokeLine(offX + i * CELL, offY, offX + i * CELL, offY + BOARD_H);
        }
        for (int i = 0; i <= Board.HEIGHT; i++) {
            gc.strokeLine(offX, offY + i * CELL, offX + BOARD_W, offY + i * CELL);
        }
        
        boolean inverted = (p.id == 2);
        
        // Draw Locked Blocks
        for (int y = 0; y < Board.HEIGHT; y++) {
            for (int x = 0; x < Board.WIDTH; x++) {
                if (p.board.grid[y][x] != null) {
                    drawCell(gc, offX, offY, x, y, p.board.grid[y][x], inverted);
                }
            }
        }
        
        // Draw Ghost
        Tetromino ghost = logic.getGhost(p);
        if (ghost != null) {
            drawGhostShape(gc, ghost, offX, offY, inverted);
        }
        
        // Draw Swap Powerup
        if (p.board.hasSwapPowerup) {
            double rx = offX + (inverted ? (Board.WIDTH - 1 - p.board.swapX) : p.board.swapX) * CELL;
            double ry = offY + (inverted ? (Board.HEIGHT - 1 - p.board.swapY) : p.board.swapY) * CELL;
            gc.setFill(Color.MAGENTA);
            gc.fillOval(rx + 4, ry + 4, CELL - 8, CELL - 8);
        }
        
        // Swap Flash
        if (p.board.swapFlash) {
            gc.setFill(Color.color(1, 0, 1, 0.5));
            gc.fillRect(offX, offY, BOARD_W, BOARD_H);
            gc.setFill(Color.WHITE);
            gc.setFont(Font.font("SansSerif", FontWeight.BOLD, 40));
            gc.fillText("SWAP!", offX + 70, offY + BOARD_H / 2);
            // clear flag after one frame to keep it simple, or use a timer
            p.board.swapFlash = false;
        }
        
        // Draw Active Piece
        if (!p.isGameOver) {
            if (p.activePiece != null) {
                drawActiveShape(gc, p.activePiece, offX, offY, inverted);
            }
        } else {
            gc.setFill(Color.color(0, 0, 0, 0.5));
            gc.fillRect(offX, offY, BOARD_W, BOARD_H);
        }
    }
    
    private void drawShape(GraphicsContext gc, int[][] shape, double px, double py, String hex, boolean isGhost, boolean inverted) {
        for (int r = 0; r < shape.length; r++) {
            for (int c = 0; c < shape[r].length; c++) {
                if (shape[r][c] == 1) {
                    double cx = px + c * CELL;
                    double cy = py + r * CELL; // Next piece doesn't need vertical invert visually usually, but let's keep it simple
                    drawBeveledCell(gc, cx, cy, hex, isGhost);
                }
            }
        }
    }
    
    private void drawActiveShape(GraphicsContext gc, Tetromino t, double offX, double offY, boolean inverted) {
        int[][] shape = t.getShape();
        for (int r = 0; r < shape.length; r++) {
            for (int c = 0; c < shape[r].length; c++) {
                if (shape[r][c] == 1) {
                    drawCell(gc, offX, offY, t.x + c, t.y + r, t.colorHex, inverted);
                }
            }
        }
    }
    
    private void drawGhostShape(GraphicsContext gc, Tetromino t, double offX, double offY, boolean inverted) {
        int[][] shape = t.getShape();
        for (int r = 0; r < shape.length; r++) {
            for (int c = 0; c < shape[r].length; c++) {
                if (shape[r][c] == 1) {
                    int bx = t.x + c;
                    int by = t.y + r;
                    
                    double renderX = offX + (inverted ? (Board.WIDTH - 1 - bx) : bx) * CELL;
                    double renderY = offY + (inverted ? (Board.HEIGHT - 1 - by) : by) * CELL;
                    
                    gc.setStroke(Color.web(t.colorHex));
                    gc.setLineWidth(2);
                    gc.strokeRect(renderX + 2, renderY + 2, CELL - 4, CELL - 4);
                }
            }
        }
    }
    
    private void drawCell(GraphicsContext gc, double offX, double offY, int bx, int by, String hex, boolean inverted) {
        double renderX = offX + (inverted ? (Board.WIDTH - 1 - bx) : bx) * CELL;
        double renderY = offY + (inverted ? (Board.HEIGHT - 1 - by) : by) * CELL;
        drawBeveledCell(gc, renderX, renderY, hex, false);
    }
    
    private void drawBeveledCell(GraphicsContext gc, double cx, double cy, String hex, boolean isGhost) {
        Color base = Color.web(hex);
        if (isGhost) {
            gc.setFill(Color.color(base.getRed(), base.getGreen(), base.getBlue(), 0.3));
            gc.fillRect(cx, cy, CELL, CELL);
            return;
        }
        
        gc.setFill(base);
        gc.fillRect(cx, cy, CELL, CELL);
        
        // Highlight top-left
        gc.setFill(Color.color(1, 1, 1, 0.4));
        gc.fillPolygon(new double[]{cx, cx + CELL, cx + CELL - 4, cx + 4, cx + 4}, 
                       new double[]{cy, cy, cy + 4, cy + 4, cy + CELL - 4}, 5);
        
        // Shadow bottom-right
        gc.setFill(Color.color(0, 0, 0, 0.4));
        gc.fillPolygon(new double[]{cx, cx + CELL, cx + CELL, cx + CELL - 4, cx + 4}, 
                       new double[]{cy + CELL, cy + CELL, cy, cy + 4, cy + CELL - 4}, 5);
    }
}
