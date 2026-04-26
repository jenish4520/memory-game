# Memory Game — LAN Multiplayer (Java 21+)

A two-player Memory Game with configurable **n-of-a-kind** matching, played over a local area network (LAN) using a host-client architecture.

## Features

- **Configurable match size** `n` (2 ≤ n ≤ 6) — match groups of 2, 3, 4, 5, or 6 identical cards
- **Configurable deck size** (4–36 cards, must be divisible by n)
- **Two-player LAN multiplayer** — host-client model over TCP/IP
- **Full synchronization** — game state, scores, turns, and lifecycle events
- **Rich Swing UI** — dark theme, gradient cards, hover effects, match indicators
- **Connection loss detection** via heartbeat mechanism
- **Restart support** — both players can start a new round without reconnecting

## Requirements

- **Java 21** or newer
- **Apache Maven 3.8+**
- Both players on the same **IPv4 LAN**

## Build

```bash
cd memory-game
mvn clean package
```

This compiles, runs all tests, and produces `target/memory-game-1.0.0.jar`.

## Run

### Option 1: Using Maven

```bash
mvn exec:java -Dexec.mainClass="com.memorygame.Main"
```

### Option 2: Using the JAR directly

```bash
java -jar target/memory-game-1.0.0.jar
```

Run this command on **both** computers.

## How to Play

### 1. Host a Game (Player 1)

1. Launch the application.
2. In the **HOST A GAME** section:
   - Set **Match Size (n)** — how many cards must match (default: 2).
   - Set **Deck Size** — total number of cards (must be divisible by n).
   - Set **Port** (default: 5555).
3. Click **Start Hosting**.
4. Note your IP address displayed in the UI.
5. Wait for Player 2 to connect.

### 2. Join a Game (Player 2)

1. Launch the application on a second computer on the same LAN.
2. In the **JOIN A GAME** section:
   - Enter the **Host IP** address.
   - Enter the **Port** (must match the host's port).
3. Click **Connect**.

### 3. Gameplay

- **Player 1** always goes first.
- On your turn, click **n** face-down cards to reveal them.
- If all n cards match:
  - You score **+1 point**.
  - You **keep your turn**.
- If they don't match:
  - Cards flip back after 1 second.
  - Turn passes to the other player.
- The game ends when **all cards are matched**.
- The player with the **higher score wins**.
- Click **New Game** to restart with the same settings.

## Architecture

```
com.memorygame
├── Main.java               # Entry point
├── model/
│   ├── Card.java            # Card with symbol, face-up/matched state
│   ├── GameConfig.java      # Match size + deck size configuration
│   ├── GamePhase.java       # LOBBY, PLAYING, RESOLVING, GAME_OVER
│   └── GameState.java       # Immutable state snapshot (serializable)
├── logic/
│   └── GameLogic.java       # Core game rules engine (host only)
├── network/
│   ├── MessageType.java     # Protocol message types
│   ├── GameMessage.java     # Serializable network message
│   ├── GameHost.java        # TCP server + heartbeat
│   └── GameClient.java      # TCP client + heartbeat
└── ui/
    ├── GameWindow.java      # Main controller (menu ↔ game transitions)
    ├── MenuPanel.java       # Host/Join configuration UI
    ├── GamePanel.java       # Game board, scores, status
    └── CardComponent.java   # Custom-painted card widget
```

### Key Design Decisions

- **Host is authoritative**: All game logic runs on the host. The client only sends input and receives state updates.
- **Immutable state snapshots**: `GameState` is a deep-copied, immutable snapshot sent over the wire. This prevents concurrency issues.
- **ObjectOutputStream/ObjectInputStream**: Java serialization for simplicity and type safety.
- **Heartbeat mechanism**: Both peers send periodic heartbeats (3s interval) to detect connection loss.
- **Mismatch delay on host**: The host uses a Swing `Timer` (1s) to show mismatched cards before flipping them back, then broadcasts the resolved state.

## Tests

```bash
mvn test
```

Tests cover:
- **Card**: creation, state transitions, deep copy, equality
- **GameConfig**: validation (n range, deck size range, divisibility)
- **GameState**: immutability, winner determination, game-over detection
- **GameLogic**: initialization, card clicks, match/mismatch resolution, turn switching, n-of-a-kind, game over, restart
- **GameMessage**: factory methods, serialization of all message types
- **Network Integration**: host-client connection, message exchange, disconnection detection

## Firewall Notes

If the client cannot connect, ensure:
- The host's firewall allows inbound TCP on the configured port (default 5555).
- Both machines are on the same subnet / can ping each other.
