package seda_project.control_alt_defeat.tetris;

import java.io.Serializable;
import java.util.Arrays;

public class Board implements Serializable {
    public static final int WIDTH = 10;
    public static final int HEIGHT = 20;
    
    // grid[y][x], stores color hex, null if empty
    public String[][] grid = new String[HEIGHT][WIDTH];
    
    public int linesClearedTotal = 0;
    public int level = 1;
    public int score = 0;
    
    // For visual flashing
    public boolean[] flashedRows = new boolean[HEIGHT];
    public boolean needsFlash = false;
    
    // Swap Power-up
    public boolean hasSwapPowerup = false;
    public int swapX = -1, swapY = -1;
    public boolean swapFlash = false;
    
    public Board() {
        for (int y = 0; y < HEIGHT; y++) {
            Arrays.fill(grid[y], null);
        }
    }
    
    public boolean isValid(Tetromino t) {
        int[][] shape = t.getShape();
        for (int r = 0; r < shape.length; r++) {
            for (int c = 0; c < shape[r].length; c++) {
                if (shape[r][c] == 1) {
                    int boardX = t.x + c;
                    int boardY = t.y + r;
                    if (boardX < 0 || boardX >= WIDTH || boardY < 0 || boardY >= HEIGHT) {
                        return false;
                    }
                    if (grid[boardY][boardX] != null) {
                        return false;
                    }
                }
            }
        }
        return true;
    }
    
    public void lock(Tetromino t) {
        int[][] shape = t.getShape();
        for (int r = 0; r < shape.length; r++) {
            for (int c = 0; c < shape[r].length; c++) {
                if (shape[r][c] == 1) {
                    int boardX = t.x + c;
                    int boardY = t.y + r;
                    if (boardY >= 0 && boardY < HEIGHT && boardX >= 0 && boardX < WIDTH) {
                        grid[boardY][boardX] = t.colorHex;
                    }
                }
            }
        }
    }
    
    public int[] getFullRows() {
        int[] fullRows = new int[HEIGHT];
        int count = 0;
        for (int y = 0; y < HEIGHT; y++) {
            boolean full = true;
            for (int x = 0; x < WIDTH; x++) {
                if (grid[y][x] == null) {
                    full = false;
                    break;
                }
            }
            if (full) {
                fullRows[count++] = y;
            }
        }
        return Arrays.copyOf(fullRows, count);
    }
    
    public void clearRows(int[] rows, boolean isP2) {
        if (rows.length == 0) return;
        
        if (!isP2) {
            // Normal: shift rows above down
            for (int r : rows) {
                for (int y = r; y > 0; y--) {
                    System.arraycopy(grid[y-1], 0, grid[y], 0, WIDTH);
                }
                Arrays.fill(grid[0], null);
            }
        } else {
            // P2: shift toward internal bottom (highest index).
            // Visually, bottom is top.
            // If row y is cleared, shift everything BELOW y UP.
            // Rows are 0 to 19. cleared row is e.g. 15.
            // Shift rows 16..19 to 15..18, and clear 19.
            // But wait, if multiple rows are cleared, better to reconstruct.
            String[][] newGrid = new String[HEIGHT][WIDTH];
            int writeY = 0; // write starting from 0
            int rowIdx = 0; // check from 0
            
            // Wait, for P2, "shift toward internal bottom".
            // Internal bottom is y=19.
            // If we clear, rows with y > cleared shift down (y--).
            // Let's iterate from 0 to 19, keep non-cleared rows.
            int newGridWriteIdx = 0;
            for (int y = 0; y < HEIGHT; y++) {
                boolean isCleared = false;
                for (int cr : rows) {
                    if (cr == y) isCleared = true;
                }
                if (!isCleared) {
                    System.arraycopy(grid[y], 0, newGrid[newGridWriteIdx++], 0, WIDTH);
                }
            }
            // newGridWriteIdx now has the count of remaining rows.
            // But wait, if P2 shifts toward internal bottom, the empty rows should appear at internal top (y=0)?
            // If empty rows appear at y=0, then it's exactly the same as P1!
            // Let's re-read: "P2: shift toward internal bottom."
            // This means empty rows appear at y=0. Wait, if empty rows appear at y=0, then existing blocks move towards y=19.
            // That's exactly what P1 does (shift rows above down).
            // Let's think: P2 spawn is bottom-center (visually).
            // Logically, if P2 renders flipped vertically, y=0 is rendered at the BOTTOM. y=19 is rendered at the TOP.
            // P2 blocks logically fall downward (y++).
            // So visually they spawn at bottom (y=0), and move UP (y++ goes UP visually).
            // When they lock, they pile up near y=19 (visually top).
            // When a line clears, blocks should fall "visually DOWN".
            // Visually DOWN is logically y--.
            // So blocks at higher y should shift to lower y.
            // That means empty rows should be added at y=19 (visually top).
            // Ah! "Shift toward internal bottom" means shift towards y=19.
            // Okay, let's reconstruct:
            int writeIdx = HEIGHT - 1;
            for (int y = HEIGHT - 1; y >= 0; y--) {
                boolean isCleared = false;
                for (int cr : rows) {
                    if (cr == y) isCleared = true;
                }
                if (!isCleared) {
                    System.arraycopy(grid[y], 0, newGrid[writeIdx--], 0, WIDTH);
                }
            }
            grid = newGrid;
        }
    }
}
