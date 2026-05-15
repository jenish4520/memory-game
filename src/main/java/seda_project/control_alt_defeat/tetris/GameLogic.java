package seda_project.control_alt_defeat.tetris;

public class GameLogic {

    public PlayerState p1;
    public PlayerState p2;
    
    private long nextSwapSpawnTime;
    
    public GameLogic(String name1, String name2) {
        p1 = new PlayerState(1, name1);
        p1.spawnNext();
        
        p2 = new PlayerState(2, name2);
        p2.spawnNext();
        
        nextSwapSpawnTime = System.currentTimeMillis() + (15000 + (long)(Math.random() * 15000));
    }
    
    public void update(long currentTime) {
        updatePlayer(p1, currentTime);
        updatePlayer(p2, currentTime);
        
        if (currentTime > nextSwapSpawnTime && !p1.board.hasSwapPowerup && !p2.board.hasSwapPowerup) {
            spawnSwapPowerup();
            nextSwapSpawnTime = currentTime + (15000 + (long)(Math.random() * 15000));
        }
    }
    
    private void spawnSwapPowerup() {
        PlayerState target = Math.random() < 0.5 ? p1 : p2;
        int emptyCount = 0;
        for (int y=0; y<Board.HEIGHT; y++) {
            for (int x=0; x<Board.WIDTH; x++) {
                if (target.board.grid[y][x] == null) emptyCount++;
            }
        }
        if (emptyCount == 0) return;
        int chosen = (int)(Math.random() * emptyCount);
        int c = 0;
        for (int y=0; y<Board.HEIGHT; y++) {
            for (int x=0; x<Board.WIDTH; x++) {
                if (target.board.grid[y][x] == null) {
                    if (c == chosen) {
                        target.board.hasSwapPowerup = true;
                        target.board.swapX = x;
                        target.board.swapY = y;
                        return;
                    }
                    c++;
                }
            }
        }
    }
    
    private void updatePlayer(PlayerState p, long currentTime) {
        if (p.isGameOver) return;
        
        if (p.activePiece == null) return;
        
        // piece falls over time
        if (currentTime - p.lastFallTime > p.getFallDelay()) {
            Tetromino t = p.activePiece.copy();
            t.y++;
            if (p.board.isValid(t)) {
                p.activePiece = t;
                p.lastFallTime = currentTime;
            } else {
                // hit something
                if (p.lockStartTime == 0) {
                    p.lockStartTime = currentTime;
                }
            }
        }
        
        // wait a bit before locking the piece in
        if (p.lockStartTime > 0 && (currentTime - p.lockStartTime > 500 || p.lockResets >= 15)) {
            // check if it's still stuck
            Tetromino t = p.activePiece.copy();
            t.y++;
            if (!p.board.isValid(t)) {
                lockPiece(p);
            } else {
                p.lockStartTime = 0; // it's free again
            }
        }
    }
    
    public void lockPiece(PlayerState p) {
        p.board.lock(p.activePiece);
        
        // did they hit the swap power-up?
        if (p.board.hasSwapPowerup) {
            int[][] shape = p.activePiece.getShape();
            boolean triggered = false;
            for (int r = 0; r < shape.length; r++) {
                for (int c = 0; c < shape[r].length; c++) {
                    if (shape[r][c] == 1) {
                        int bx = p.activePiece.x + c;
                        int by = p.activePiece.y + r;
                        if (bx == p.board.swapX && by == p.board.swapY) {
                            triggered = true;
                        }
                    }
                }
            }
            if (triggered) {
                p.board.hasSwapPowerup = false;
                p1.board.swapFlash = true;
                p2.board.swapFlash = true;
                // trade boards
                String[][] tempGrid = p1.board.grid;
                p1.board.grid = p2.board.grid;
                p2.board.grid = tempGrid;
            }
        }
        
        int[] fullRows = p.board.getFullRows();
        if (fullRows.length > 0) {
            p.board.clearRows(fullRows, p.id == 2);
            
            int lines = fullRows.length;
            p.linesCleared += lines;
            
            int baseScore = 0;
            boolean isTetris = (lines == 4);
            switch (lines) {
                case 1: baseScore = 100; break;
                case 2: baseScore = 300; break;
                case 3: baseScore = 500; break;
                case 4: baseScore = 800; break;
            }
            
            if (isTetris) {
                if (p.backToBack) {
                    baseScore = 1200;
                }
                p.backToBack = true;
            } else {
                p.backToBack = false;
            }
            
            p.score += baseScore * (1 + p.linesCleared / 10);
        }
        
        p.activePiece = null;
        p.spawnNext();
    }
    
    public void moveLeft(PlayerState p) {
        if (p.isGameOver || p.activePiece == null) return;
        Tetromino t = p.activePiece.copy();
        
        // handling horizontal flip for P2
        t.x += (p.id == 1) ? -1 : 1; 
        
        if (p.board.isValid(t)) {
            p.activePiece = t;
            resetLock(p);
        }
    }
    
    public void moveRight(PlayerState p) {
        if (p.isGameOver || p.activePiece == null) return;
        Tetromino t = p.activePiece.copy();
        t.x += (p.id == 1) ? 1 : -1;
        if (p.board.isValid(t)) {
            p.activePiece = t;
            resetLock(p);
        }
    }
    
    public void softDrop(PlayerState p) {
        if (p.isGameOver || p.activePiece == null) return;
        Tetromino t = p.activePiece.copy();
        t.y++;
        if (p.board.isValid(t)) {
            p.activePiece = t;
            p.score += 1;
            p.lastFallTime = System.currentTimeMillis(); // reset the drop timer
        }
    }
    
    public void hardDrop(PlayerState p) {
        if (p.isGameOver || p.activePiece == null) return;
        int dropped = 0;
        Tetromino t = p.activePiece.copy();
        while (true) {
            t.y++;
            if (p.board.isValid(t)) {
                p.activePiece = t;
                dropped++;
            } else {
                break;
            }
        }
        p.score += 2 * dropped;
        lockPiece(p);
    }
    
    public void rotateCW(PlayerState p) {
        if (p.isGameOver || p.activePiece == null) return;
        Tetromino t = p.activePiece.copy();
        int fromState = t.state;
        
        // P2 visually inverted vertically and horizontally (180 deg)
        // If P2 presses CW, it should visually rotate CW.
        // A logical CW rotation, viewed upside-down, looks like a CW rotation!
        // Wait: if you rotate an object CW, and look at it upside down, it rotates CW.
        // So logical CW = visual CW.
        
        t.rotateCW();
        int toState = t.state;
        
        tryKicks(p, t, fromState, toState, true);
    }
    
    public void rotateCCW(PlayerState p) {
        if (p.isGameOver || p.activePiece == null) return;
        Tetromino t = p.activePiece.copy();
        int fromState = t.state;
        t.rotateCCW();
        int toState = t.state;
        
        tryKicks(p, t, fromState, toState, false);
    }
    
    private void tryKicks(PlayerState p, Tetromino t, int fromState, int toState, boolean isCW) {
        int[][] kicks = WallKicks.getKicks(t.type, fromState, toState, isCW);
        for (int[] kick : kicks) {
            Tetromino test = t.copy();
            test.x += (p.id == 1) ? kick[0] : -kick[0]; 
            test.y += (p.id == 1) ? -kick[1] : kick[1]; // kick[1] is UP (+), so logically subtract for P1
            
            if (p.board.isValid(test)) {
                p.activePiece = test;
                resetLock(p);
                return;
            }
        }
    }
    
    private void resetLock(PlayerState p) {
        if (p.lockStartTime > 0 && p.lockResets < 15) {
            p.lockStartTime = System.currentTimeMillis();
            p.lockResets++;
        }
    }
    
    public Tetromino getGhost(PlayerState p) {
        if (p.activePiece == null) return null;
        Tetromino ghost = p.activePiece.copy();
        while (true) {
            ghost.y++;
            if (!p.board.isValid(ghost)) {
                ghost.y--;
                break;
            }
        }
        return ghost;
    }
}
