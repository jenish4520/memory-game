package com.memorygame.model;

import java.io.Serializable;

/**
 * Configuration for a game round.
 * Validates that deck size is divisible by match size n.
 */
public class GameConfig implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final int MIN_MATCH_SIZE = 2;
    public static final int MAX_MATCH_SIZE = 6;
    public static final int MIN_DECK_SIZE = 4;
    public static final int MAX_DECK_SIZE = 45;

    private final int matchSize;
    private final int deckSize;

    public GameConfig(int matchSize, int deckSize) {
        validate(matchSize, deckSize);
        this.matchSize = matchSize;
        this.deckSize = deckSize;
    }

    public static void validate(int matchSize, int deckSize) {
        if (matchSize < MIN_MATCH_SIZE || matchSize > MAX_MATCH_SIZE) {
            throw new IllegalArgumentException(
                "Match size n must be between " + MIN_MATCH_SIZE + " and " + MAX_MATCH_SIZE + ", got: " + matchSize);
        }
        if (deckSize < MIN_DECK_SIZE || deckSize > MAX_DECK_SIZE) {
            throw new IllegalArgumentException(
                "Deck size must be between " + MIN_DECK_SIZE + " and " + MAX_DECK_SIZE + ", got: " + deckSize);
        }
        if (deckSize % matchSize != 0) {
            throw new IllegalArgumentException(
                "Deck size (" + deckSize + ") must be divisible by match size n (" + matchSize + ")");
        }
    }

    /** Returns the number of unique symbols needed. */
    public int getUniqueSymbolCount() {
        return deckSize / matchSize;
    }

    public int getMatchSize() { return matchSize; }
    public int getDeckSize() { return deckSize; }

    @Override
    public String toString() {
        return "GameConfig{matchSize=" + matchSize + ", deckSize=" + deckSize + "}";
    }
}
