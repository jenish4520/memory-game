package com.memorygame.logic;

import com.memorygame.model.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Core game logic engine. Maintains mutable authoritative state on the host.
 * All public methods are synchronized for thread safety.
 */
public class GameLogic {

    public static final String[] SYMBOL_POOL = {
        "\uD83C\uDF4E", "\uD83C\uDF4A", "\uD83C\uDF47", "\uD83C\uDF49", // 🍎 🍊 🍇 🍉
        "\uD83D\uDC36", "\uD83D\uDC31", "\uD83D\uDC2D", "\uD83D\uDC39", // 🐶 🐱 🐭 🐹
        "\uD83C\uDF4C", "\uD83C\uDF4D", "\uD83C\uDF53", "\uD83C\uDF52", // 🍌 🍍 🍓 🍒
        "\uD83D\uDC30", "\uD83D\uDC3A", "\uD83D\uDC3B", "\uD83D\uDC3C", // 🐰 🐺 🐻 🐼
        "\uD83C\uDF51", "\uD83C\uDF50", "\uD83C\uDF4F", "\uD83E\uDD6D", // 🍑 🍐 🍏 🥭
        "\uD83E\uDD81", "\uD83D\uDC2F", "\uD83D\uDC2E", "\uD83D\uDC37", // 🦁 🐯 🐮 🐷
        "\uD83E\uDD5D", "\uD83C\uDF45", "\uD83E\uDD65", "\uD83E\uDD51", // 🥝 🍅 🥥 🥑
        "\uD83D\uDC38", "\uD83D\uDC35", "\uD83D\uDC14", "\uD83D\uDC27", // 🐸 🐵 🐔 🐧
        "\uD83D\uDC26", "\uD83D\uDC24", "\uD83D\uDC28", "\uD83E\uDD84"  // 🐦 🐤 🐨 🦄
    };

    private List<Card> cards;
    private int player1Score;
    private int player2Score;
    private int activePlayer;
    private GamePhase phase;
    private List<Integer> currentAttempt;
    private int matchSize;
    private int deckSize;

    /**
     * Initializes a new game round with the given configuration.
     * Generates a shuffled deck where each symbol appears exactly n times.
     */
    public synchronized void initializeGame(GameConfig config) {
        this.matchSize = config.getMatchSize();
        this.deckSize = config.getDeckSize();
        this.player1Score = 0;
        this.player2Score = 0;
        this.activePlayer = 1;
        this.phase = GamePhase.PLAYING;
        this.currentAttempt = new ArrayList<>();
        this.cards = generateDeck(config);
    }

    /**
     * Generates a shuffled deck. Each symbol appears exactly matchSize times.
     */
    private List<Card> generateDeck(GameConfig config) {
        int uniqueSymbols = config.getUniqueSymbolCount();
        if (uniqueSymbols > SYMBOL_POOL.length) {
            throw new IllegalArgumentException(
                "Not enough symbols. Need " + uniqueSymbols + " but have " + SYMBOL_POOL.length);
        }

        List<Card> deck = new ArrayList<>();
        int cardId = 0;
        for (int s = 0; s < uniqueSymbols; s++) {
            for (int k = 0; k < config.getMatchSize(); k++) {
                deck.add(new Card(cardId++, SYMBOL_POOL[s]));
            }
        }
        Collections.shuffle(deck);
        return deck;
    }

    /**
     * Handles a card click from a player.
     * @return true if the click was valid and processed, false otherwise.
     */
    public synchronized boolean handleCardClick(int cardIndex, int playerNumber) {
        if (phase != GamePhase.PLAYING) return false;
        if (playerNumber != activePlayer) return false;
        if (cardIndex < 0 || cardIndex >= cards.size()) return false;

        Card card = cards.get(cardIndex);
        if (!card.isClickable()) return false;

        card.setFaceUp(true);
        currentAttempt.add(cardIndex);

        if (currentAttempt.size() == matchSize) {
            resolveAttempt();
        }

        return true;
    }

    /**
     * Resolves the current attempt after n cards have been revealed.
     * Sets phase to RESOLVING on mismatch (cards stay face-up for viewing).
     */
    private void resolveAttempt() {
        String firstSymbol = cards.get(currentAttempt.get(0)).getSymbol();
        boolean allMatch = currentAttempt.stream()
            .allMatch(i -> cards.get(i).getSymbol().equals(firstSymbol));

        if (allMatch) {
            // Match found
            for (int idx : currentAttempt) {
                cards.get(idx).setMatched(true);
                cards.get(idx).setMatchedByPlayer(activePlayer);
            }
            if (activePlayer == 1) player1Score++;
            else player2Score++;
            currentAttempt.clear();

            // Check game over
            if (cards.stream().allMatch(Card::isMatched)) {
                phase = GamePhase.GAME_OVER;
            }
            // Active player keeps their turn on a match
        } else {
            // Mismatch — enter resolving phase (cards shown temporarily)
            phase = GamePhase.RESOLVING;
        }
    }

    /**
     * Called after the mismatch display delay. Flips mismatched cards back
     * and switches the active player.
     */
    public synchronized void resolveMismatch() {
        if (phase != GamePhase.RESOLVING) return;

        for (int idx : currentAttempt) {
            cards.get(idx).setFaceUp(false);
        }
        currentAttempt.clear();
        activePlayer = (activePlayer == 1) ? 2 : 1;
        phase = GamePhase.PLAYING;
    }

    /** Creates an immutable snapshot of the current state. */
    public synchronized GameState getState() {
        return getState("");
    }

    /** Creates an immutable snapshot with a custom status message. */
    public synchronized GameState getState(String statusMessage) {
        return new GameState(
            cards, player1Score, player2Score,
            activePlayer, phase, currentAttempt,
            matchSize, deckSize, statusMessage
        );
    }

    public synchronized GamePhase getPhase() { return phase; }
    public synchronized int getActivePlayer() { return activePlayer; }
    public synchronized int getPlayer1Score() { return player1Score; }
    public synchronized int getPlayer2Score() { return player2Score; }
    public synchronized int getMatchSize() { return matchSize; }
    public synchronized int getDeckSize() { return deckSize; }
    public synchronized List<Integer> getCurrentAttempt() { return new ArrayList<>(currentAttempt); }

    /**
     * Returns the number of cards that have been matched so far.
     */
    public synchronized int getMatchedCount() {
        return (int) cards.stream().filter(Card::isMatched).count();
    }
}
