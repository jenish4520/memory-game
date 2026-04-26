package com.memorygame.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Immutable snapshot of the entire game state.
 * Sent over the network to synchronize host and client.
 */
public class GameState implements Serializable {
    private static final long serialVersionUID = 1L;

    private final List<Card> cards;
    private final int player1Score;
    private final int player2Score;
    private final int activePlayer;
    private final GamePhase phase;
    private final List<Integer> currentAttempt;
    private final int matchSize;
    private final int deckSize;
    private final String statusMessage;

    public GameState(List<Card> cards, int player1Score, int player2Score,
                     int activePlayer, GamePhase phase, List<Integer> currentAttempt,
                     int matchSize, int deckSize, String statusMessage) {
        // Deep copy cards
        this.cards = new ArrayList<>();
        for (Card c : cards) {
            this.cards.add(c.copy());
        }
        this.player1Score = player1Score;
        this.player2Score = player2Score;
        this.activePlayer = activePlayer;
        this.phase = phase;
        this.currentAttempt = new ArrayList<>(currentAttempt);
        this.matchSize = matchSize;
        this.deckSize = deckSize;
        this.statusMessage = statusMessage;
    }

    public List<Card> getCards() { return Collections.unmodifiableList(cards); }
    public int getPlayer1Score() { return player1Score; }
    public int getPlayer2Score() { return player2Score; }
    public int getActivePlayer() { return activePlayer; }
    public GamePhase getPhase() { return phase; }
    public List<Integer> getCurrentAttempt() { return Collections.unmodifiableList(currentAttempt); }
    public int getMatchSize() { return matchSize; }
    public int getDeckSize() { return deckSize; }
    public String getStatusMessage() { return statusMessage; }

    public boolean isGameOver() { return phase == GamePhase.GAME_OVER; }

    /** Determines the winner: 1, 2, or 0 for a draw. */
    public int getWinner() {
        if (player1Score > player2Score) return 1;
        if (player2Score > player1Score) return 2;
        return 0;
    }

    public int getTotalMatched() {
        return (int) cards.stream().filter(Card::isMatched).count();
    }

    @Override
    public String toString() {
        return "GameState{phase=" + phase + ", active=" + activePlayer +
               ", score=" + player1Score + ":" + player2Score +
               ", cards=" + cards.size() + ", matched=" + getTotalMatched() + "}";
    }
}
