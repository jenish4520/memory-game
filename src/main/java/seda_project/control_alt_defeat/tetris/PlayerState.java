package seda_project.control_alt_defeat.tetris;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PlayerState implements Serializable {
    public int id; // 1 or 2
    public String name;
    public Board board;
    public Tetromino activePiece;
    public Tetromino nextPiece;
    public List<Tetromino> bag = new ArrayList<>();
    
    public int linesCleared = 0;
    public int score = 0;
    public boolean backToBack = false;
    
    public boolean isGameOver = false;
    
    public long lastFallTime = 0;
    public int lockResets = 0;
    public long lockStartTime = 0;
    
    public PlayerState(int id, String name) {
        this.id = id;
        this.name = name;
        this.board = new Board();
        refillBag();
        nextPiece = pullFromBag();
    }
    
    public void refillBag() {
        bag.add(Tetromino.create(Tetromino.Type.I));
        bag.add(Tetromino.create(Tetromino.Type.J));
        bag.add(Tetromino.create(Tetromino.Type.L));
        bag.add(Tetromino.create(Tetromino.Type.O));
        bag.add(Tetromino.create(Tetromino.Type.S));
        bag.add(Tetromino.create(Tetromino.Type.T));
        bag.add(Tetromino.create(Tetromino.Type.Z));
        for (Tetromino custom : CustomPieceDesigner.customPieces) {
            bag.add(custom.copy());
        }
        Collections.shuffle(bag);
    }
    
    public Tetromino pullFromBag() {
        if (bag.isEmpty()) {
            refillBag();
        }
        return bag.remove(0);
    }
    
    public void spawnNext() {
        activePiece = nextPiece;
        nextPiece = pullFromBag();
        
        // Spawn top-center
        activePiece.x = Board.WIDTH / 2 - 2;
        activePiece.y = 0;
        
        // P2 inverted? If we handle inversion ONLY in rendering, then logic is same!
        // But wait: "Spawn top-center (P1) / bottom-center (P2)."
        // If P2 spawns bottom-center logically, it must fall UP logically.
        // Or if it spawns top-center logically and is rendered upside down, visually it's bottom-center.
        // Yes, let's keep logic identical: y=0 is spawn for BOTH. P2 is rendered flipped vertically!
        // This fully satisfies "internally mirrored, displayed flipped vertically".
        // If we want it "internally mirrored", maybe x is also inverted?
        // "internally mirrored" = logically same, visually mirrored?
        // Let's assume logically x is 0..9.
        
        if (!board.isValid(activePiece)) {
            isGameOver = true;
        }
        
        lockResets = 0;
        lockStartTime = 0;
    }
    
    public int getFallDelay() {
        int delay = 1000 - (linesCleared * 40);
        if (delay < 60) return 60;
        return delay;
    }
}
