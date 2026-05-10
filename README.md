# Task 1 : Memory Game Application (Retro Game Compilation SuSe2026 SEDA Project)
# Team: Loop Never Ends

## Features Requirements

- **Configurable match size** that is `n` (1 ≤ n ≤ 45) with match groups of n cards of same kind.
- **Configurable deck size** (deck size must be divisible by n).
- **2-player LAN multiplayer** —> using the host-client model over TCP/IP on same network.
- **Full synchronization** —> the game state, scores, turns, and lifecycle events.
- **JavaFX UI** —> Pokemon theme with custom pokemon cards, hover effects
- **Restart support** —> both players can start a new round without reconnecting.

## Requirements

- **Java 21** or newer
- **Apache Maven 3.8+, JavaFX**
- Both players on the same **IPv4 Local Area Network**

## Build

In bash
cd memory-game
mvn clean javafx:jlink
mvn javafx:run


## How to Play Instructions

### 1. Host a Game (Player 1)

1. Launch the application.
2. In the **HOST A GAME** section:-
   - A Player can choose **Match Size (n)**, that is how many cards must match (by default: 2).
   -A Player can set **Deck Size**, that is total number of cards (deck size be divisible by n).
3. Click **Start Hosting**
4. The IP address of the host is displayed in the User Interface.
5. Player 2 will join the game by entering the *IP address* and *port number* and the game starts.

### 2. Join a Game (Player 2)

1. Another player will launch the application on a second computer over the **same network**.
2. In the **JOIN A GAME** section:
   - Player will enter the **Host IP** address.
   - The the player will enter the **Port** (must match the host's port!).
3. Click **Connect** and the game panel will appear where you can start the game!.

### 3. Gameplay Flow

- Player 1 goes first.
- On the player's turn, player clicks **n** face-down cards to reveal them (for example 2 cards, then click 2 consecutive cards).
- If all n cards match:
  - Player 1 scores **+1 point**.
  - Player 1 **keeps his turn**.
- If the cards don't match:
  - Cards flip back after the turn.
  - The turn passes to the other player 2.
- The game session will end when **all cards are matched**.
- The player with the higher score **wins**, or **draw** if the scores are equal

- Click **New Game** to restart with the same settings.