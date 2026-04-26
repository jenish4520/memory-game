package com.memorygame.model;

/**
 * Represents the current phase of the game lifecycle.
 */
public enum GamePhase {
    /** Waiting in the menu / lobby for players to connect. */
    LOBBY,

    /** Game is active, waiting for the active player to select cards. */
    PLAYING,

    /** All n cards have been revealed; resolving match or mismatch. */
    RESOLVING,

    /** All cards have been matched; the game is over. */
    GAME_OVER
}
