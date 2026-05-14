package seda_project.control_alt_defeat.tetris;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.function.Consumer;

public class TetrisHost {
    private ServerSocket serverSocket;
    private Socket clientSocket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private Consumer<Object> onMessage;
    private Runnable onDisconnect;

    public TetrisHost(Consumer<Object> onMessage, Runnable onDisconnect) {
        this.onMessage = onMessage;
        this.onDisconnect = onDisconnect;
    }

    private Thread udpThread;

    public void start(int port, String hostName) throws Exception {
        // Start UDP Discovery responder
        udpThread = new Thread(() -> {
            try (DatagramSocket udpSocket = new DatagramSocket(8081)) {
                byte[] buf = new byte[256];
                while (!Thread.currentThread().isInterrupted()) {
                    DatagramPacket packet = new DatagramPacket(buf, buf.length);
                    udpSocket.receive(packet);
                    String req = new String(packet.getData(), 0, packet.getLength()).trim();
                    if (req.equals("TETRIS_DISCOVER")) {
                        String resp = "TETRIS_HOST:" + hostName + ":" + port;
                        byte[] respData = resp.getBytes();
                        DatagramPacket respPacket = new DatagramPacket(respData, respData.length, packet.getAddress(), packet.getPort());
                        udpSocket.send(respPacket);
                    }
                }
            } catch (Exception e) {}
        });
        udpThread.setDaemon(true);
        udpThread.start();

        serverSocket = new ServerSocket(port);
        clientSocket = serverSocket.accept();
        clientSocket.setTcpNoDelay(true);
        out = new ObjectOutputStream(clientSocket.getOutputStream());
        in = new ObjectInputStream(clientSocket.getInputStream());

        new Thread(() -> {
            try {
                while (true) {
                    Object msg = in.readObject();
                    onMessage.accept(msg);
                }
            } catch (Exception e) {
                onDisconnect.run();
            }
        }).start();
    }

    public void send(Object msg) {
        try {
            if (out != null) {
                out.reset();
                out.writeObject(msg);
                out.flush();
            }
        } catch (Exception e) {
            onDisconnect.run();
        }
    }

    public void close() {
        if (udpThread != null) udpThread.interrupt();
        try {
            if (serverSocket != null) serverSocket.close();
            if (clientSocket != null) clientSocket.close();
        } catch (Exception e) {}
    }
}
