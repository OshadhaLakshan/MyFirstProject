package chat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * Serves ONE connected client. Each ClientHandler runs in its own thread.
 *
 * Protocol (one line per message):
 *   first line   nickname
 *   any text     broadcast "[nickname] text" to everyone
 *   /list        reply with the nicknames of connected clients
 *   /quit        leave the chat
 *   /stats       reply with the total number of chat messages received
 */
public class ClientHandler implements Runnable {

    private final Socket socket;
    private PrintWriter out;
    private String nickname = "(unnamed)";

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            out = new PrintWriter(
                    new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);

            String name = in.readLine();                  // first line = nickname
            if (name == null || name.isBlank()) {
                return;                                   // finally{} closes the socket
            }
            nickname = name.trim();
            ChatServer.log(nickname + " connected from " + socket.getRemoteSocketAddress()
                    + " on " + Thread.currentThread().getName());

            ChatServer.clients.add(this);
            broadcast("* " + nickname + " joined");


            String line;
            while ((line = in.readLine()) != null) {
                if (line.equals("/quit")) {
                    break;
                } else if (line.equals("/list")) {
                    String names = ChatServer.clients.stream()
                            .map(ClientHandler::getNickname)
                            .collect(java.util.stream.Collectors.joining(", "));
                    send("online: " + names);

                } else if (line.equals("/stats")) {
                    send("stats: " + ChatServer.messageCount.get() + " messages");
                } else {
                    ChatServer.messageCount.incrementAndGet();

                    broadcast("[" + nickname + "] " + line);
                }
            }
        } catch (IOException e) {
            ChatServer.log("Connection error with " + nickname + ": " + e.getMessage());
        } finally {
            disconnect();
        }
    }

    /** Sends one line to THIS client. */
    synchronized void send(String msg) {
        out.println(msg);
    }

    /** Sends one line to EVERY connected client. */
    void broadcast(String msg) {
        for (ClientHandler client : ChatServer.clients) {
            client.send(msg);
        }

    }

    String getNickname() {
        return nickname;
    }

    /** Runs when the client leaves for ANY reason: /quit, disconnect or error. */
    private void disconnect() {
        try {
            if (ChatServer.clients.remove(this)) {
                broadcast("* " + nickname + " left");
            }

        } finally {
            // Always runs, even if the code above throws an exception,
            // so the socket is never left open.
            try {
                socket.close();
            } catch (IOException ignored) {
                // already closed
            }
            ChatServer.log(nickname + " disconnected");
        }
    }
}
