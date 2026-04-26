package com.memorygame.network;

import java.io.*;
import java.net.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * Network host that listens for a single client connection.
 * Maintains the server socket and provides send/receive capabilities.
 * Runs a background reader thread and heartbeat thread.
 */
public class GameHost implements AutoCloseable {

    public static final int DEFAULT_PORT = 5555;
    private static final long HEARTBEAT_INTERVAL_MS = 3000;
    private static final long CONNECTION_TIMEOUT_MS = 10000;

    private ServerSocket serverSocket;
    private Socket clientSocket;
    private ObjectOutputStream out;
    private ObjectInputStream in;

    private final Consumer<GameMessage> messageHandler;
    private final Runnable onConnectionLost;

    private volatile boolean running;
    private String clientPlayerName = "Player 2"; // set from JOIN_REQUEST
    private final ExecutorService executor = Executors.newFixedThreadPool(3);
    private ScheduledExecutorService heartbeatScheduler;

    /**
     * @param messageHandler callback invoked on the reader thread for each received message
     * @param onConnectionLost callback invoked when the client connection is lost
     */
    public GameHost(Consumer<GameMessage> messageHandler, Runnable onConnectionLost) {
        this.messageHandler = messageHandler;
        this.onConnectionLost = onConnectionLost;
    }

    /**
     * Starts listening on the given port and waits for a client to connect.
     * This method blocks until a client connects or is interrupted.
     *
     * @return the client's InetAddress once connected
     * @throws IOException if the server socket cannot be created
     */
    public InetAddress startAndWaitForClient(int port) throws IOException {
        serverSocket = new ServerSocket(port);
        serverSocket.setReuseAddress(true);
        serverSocket.setSoTimeout(0); // block indefinitely

        running = true;
        clientSocket = serverSocket.accept();
        clientSocket.setTcpNoDelay(true);
        clientSocket.setKeepAlive(true);

        out = new ObjectOutputStream(clientSocket.getOutputStream());
        out.flush();
        in = new ObjectInputStream(clientSocket.getInputStream());

        // Wait for JOIN_REQUEST and read the joining player's name
        try {
            GameMessage joinMsg = (GameMessage) in.readObject();
            if (joinMsg.getType() != MessageType.JOIN_REQUEST) {
                throw new IOException("Expected JOIN_REQUEST, got: " + joinMsg.getType());
            }
            String name = joinMsg.getPlayerName();
            clientPlayerName = (name != null && !name.isBlank()) ? name : "Player 2";
        } catch (ClassNotFoundException e) {
            throw new IOException("Protocol error: " + e.getMessage(), e);
        }

        // Send JOIN_ACCEPTED
        sendMessage(GameMessage.joinAccepted(2));

        // Start reader and heartbeat threads
        startReaderThread();
        startHeartbeat();

        return clientSocket.getInetAddress();
    }

    /** Sends a message to the connected client. Thread-safe. */
    public synchronized void sendMessage(GameMessage message) {
        if (!running || out == null) return;
        try {
            out.writeObject(message);
            out.flush();
            out.reset(); // prevent caching of previously-sent objects
        } catch (IOException e) {
            handleConnectionLoss();
        }
    }

    private void startReaderThread() {
        executor.submit(() -> {
            while (running && !Thread.currentThread().isInterrupted()) {
                try {
                    GameMessage msg = (GameMessage) in.readObject();
                    if (msg.getType() != MessageType.HEARTBEAT) {
                        messageHandler.accept(msg);
                    }
                } catch (IOException | ClassNotFoundException e) {
                    if (running) {
                        handleConnectionLoss();
                    }
                    break;
                }
            }
        });
    }

    private void startHeartbeat() {
        heartbeatScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "host-heartbeat");
            t.setDaemon(true);
            return t;
        });
        heartbeatScheduler.scheduleAtFixedRate(() -> {
            sendMessage(GameMessage.heartbeat());
        }, HEARTBEAT_INTERVAL_MS, HEARTBEAT_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }

    private void handleConnectionLoss() {
        if (!running) return;
        running = false;
        onConnectionLost.run();
    }

    public boolean isRunning() {
        return running;
    }

    /** Returns the display name the joining player chose. */
    public String getClientPlayerName() {
        return clientPlayerName;
    }

    /** Returns the local IP address the server is bound to. */
    public String getHostAddress() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            return "127.0.0.1";
        }
    }

    @Override
    public void close() {
        running = false;
        if (heartbeatScheduler != null) heartbeatScheduler.shutdownNow();
        executor.shutdownNow();
        try { if (in != null) in.close(); } catch (IOException ignored) {}
        try { if (out != null) out.close(); } catch (IOException ignored) {}
        try { if (clientSocket != null) clientSocket.close(); } catch (IOException ignored) {}
        try { if (serverSocket != null) serverSocket.close(); } catch (IOException ignored) {}
    }
}
