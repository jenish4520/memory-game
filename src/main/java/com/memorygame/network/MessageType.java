package com.memorygame.network;

/**
 * Types of messages exchanged between host and client.
 */
public enum MessageType {
    /** Client requests to join the host's session. */
    JOIN_REQUEST,

    /** Host accepts the client's join request, assigns player number. */
    JOIN_ACCEPTED,

    /** Host sends initial game state to start a round. */
    GAME_START,

    /** Player sends a card click to the host. */
    CARD_CLICK,

    /** Host sends updated game state after processing an action. */
    STATE_UPDATE,

    /** Host announces the game has ended. */
    GAME_END,

    /** A player requests to restart the game. */
    RESTART_REQUEST,

    /** Host confirms restart and starts a new round. */
    RESTART_CONFIRMED,

    /** An error occurred. */
    ERROR,

    /** Keep-alive heartbeat to detect connection loss. */
    HEARTBEAT
}
