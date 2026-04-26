package com.memorygame.network;

import com.memorygame.model.GameConfig;
import com.memorygame.model.GameState;

import java.io.Serializable;

/**
 * Serializable message exchanged between host and client over the network.
 * Uses a tagged-union style: only fields relevant to the message type are populated.
 */
public class GameMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    private final MessageType type;
    private GameState gameState;
    private GameConfig config;
    private int cardIndex;
    private int playerNumber;
    private String message;
    private String playerName;  // used in JOIN_REQUEST (joining player's chosen name)
    private String p1Name;      // used in GAME_START / RESTART_CONFIRMED
    private String p2Name;      // used in GAME_START / RESTART_CONFIRMED

    public GameMessage(MessageType type) {
        this.type = type;
        this.cardIndex = -1;
        this.playerNumber = -1;
    }

    // --- Factory methods for common messages ---

    public static GameMessage joinRequest(String playerName) {
        GameMessage msg = new GameMessage(MessageType.JOIN_REQUEST);
        msg.playerName = (playerName == null || playerName.isBlank()) ? "Player 2" : playerName;
        return msg;
    }

    public static GameMessage joinAccepted(int playerNumber) {
        GameMessage msg = new GameMessage(MessageType.JOIN_ACCEPTED);
        msg.playerNumber = playerNumber;
        return msg;
    }

    public static GameMessage gameStart(GameState state, GameConfig config, String p1Name, String p2Name) {
        GameMessage msg = new GameMessage(MessageType.GAME_START);
        msg.gameState = state;
        msg.config = config;
        msg.p1Name = p1Name;
        msg.p2Name = p2Name;
        return msg;
    }

    public static GameMessage cardClick(int cardIndex) {
        GameMessage msg = new GameMessage(MessageType.CARD_CLICK);
        msg.cardIndex = cardIndex;
        return msg;
    }

    public static GameMessage stateUpdate(GameState state) {
        GameMessage msg = new GameMessage(MessageType.STATE_UPDATE);
        msg.gameState = state;
        return msg;
    }

    public static GameMessage gameEnd(GameState state) {
        GameMessage msg = new GameMessage(MessageType.GAME_END);
        msg.gameState = state;
        return msg;
    }

    public static GameMessage restartRequest() {
        return new GameMessage(MessageType.RESTART_REQUEST);
    }

    public static GameMessage restartConfirmed(GameState state, GameConfig config, String p1Name, String p2Name) {
        GameMessage msg = new GameMessage(MessageType.RESTART_CONFIRMED);
        msg.gameState = state;
        msg.config = config;
        msg.p1Name = p1Name;
        msg.p2Name = p2Name;
        return msg;
    }

    public static GameMessage error(String errorMessage) {
        GameMessage msg = new GameMessage(MessageType.ERROR);
        msg.message = errorMessage;
        return msg;
    }

    public static GameMessage heartbeat() {
        return new GameMessage(MessageType.HEARTBEAT);
    }

    // --- Getters ---

    public MessageType getType() { return type; }
    public GameState getGameState() { return gameState; }
    public GameConfig getConfig() { return config; }
    public int getCardIndex() { return cardIndex; }
    public int getPlayerNumber() { return playerNumber; }
    public String getMessage() { return message; }
    public String getPlayerName() { return playerName; }
    public String getP1Name() { return p1Name; }
    public String getP2Name() { return p2Name; }

    @Override
    public String toString() {
        return "GameMessage{type=" + type +
               (gameState != null ? ", state=" + gameState : "") +
               (cardIndex >= 0 ? ", cardIndex=" + cardIndex : "") +
               (playerNumber >= 0 ? ", player=" + playerNumber : "") +
               (message != null ? ", msg='" + message + "'" : "") +
               "}";
    }
}
