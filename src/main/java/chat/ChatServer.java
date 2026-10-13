package chat;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Multithreaded chat server: fixed pool of ten workers by default.
 *
 * Run: chat.ChatServer [port] [--thread-per-client] (default port 5000).
 * Use --thread-per-client to repeat Part 4 before the Part 5 pool experiment.
 */
public class ChatServer {

    public static final int DEFAULT_PORT = 5000;

    // ---- Shared state, used by ALL client threads ----------------------------
    // Part 4: safe snapshot iteration and atomic message counting.
    static List<ClientHandler> clients = new CopyOnWriteArrayList<>();
    static final AtomicInteger messageCount = new AtomicInteger(0);
    // ---------------------------------------------------------------------------

    public static void main(String[] args) throws IOException {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : DEFAULT_PORT;

        boolean threadPerClient = args.length > 1 && args[1].equals("--thread-per-client");

        try (ServerSocket server = new ServerSocket(port);
             ExecutorService pool = Executors.newFixedThreadPool(10)) {
            log("Chat server listening on port " + port
                    + (threadPerClient ? " (one thread per client)" : " (10-worker pool)"));

            while (true) {
                Socket socket = server.accept();
                ClientHandler handler = new ClientHandler(socket);

                // Part 5: queue handlers when all ten workers are occupied.
                if (threadPerClient) {
                    new Thread(handler).start();
                } else {
                    pool.submit(handler);
                }
            }
        }
    }

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");

    /** Prints a time-stamped line to the server console. */
    static void log(String msg) {
        System.out.println(LocalTime.now().format(TIME) + "  " + msg);
    }
}
