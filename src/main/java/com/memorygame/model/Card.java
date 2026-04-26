package com.memorygame.model;

import java.io.Serializable;
import java.util.Objects;

/**
 * Represents a single card in the Memory Game.
 * Each card has a unique ID, a symbol for matching, and state flags.
 */
public class Card implements Serializable {
    private static final long serialVersionUID = 1L;

    private final int id;
    private final String symbol;
    private boolean faceUp;
    private boolean matched;
    private int matchedByPlayer;

    public Card(int id, String symbol) {
        this.id = id;
        this.symbol = symbol;
        this.faceUp = false;
        this.matched = false;
        this.matchedByPlayer = 0;
    }

    /** Creates a deep copy of this card. */
    public Card copy() {
        Card copy = new Card(this.id, this.symbol);
        copy.faceUp = this.faceUp;
        copy.matched = this.matched;
        copy.matchedByPlayer = this.matchedByPlayer;
        return copy;
    }

    public int getId() { return id; }
    public String getSymbol() { return symbol; }
    public boolean isFaceUp() { return faceUp; }
    public boolean isMatched() { return matched; }
    public int getMatchedByPlayer() { return matchedByPlayer; }

    public void setFaceUp(boolean faceUp) { this.faceUp = faceUp; }
    public void setMatched(boolean matched) { this.matched = matched; }
    public void setMatchedByPlayer(int playerNum) { this.matchedByPlayer = playerNum; }

    /** Returns true if this card can be clicked (face-down and not yet matched). */
    public boolean isClickable() {
        return !faceUp && !matched;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Card card = (Card) o;
        return id == card.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Card{id=" + id + ", symbol='" + symbol + "', faceUp=" + faceUp + ", matched=" + matched + ", matchedBy=" + matchedByPlayer + "}";
    }
}
