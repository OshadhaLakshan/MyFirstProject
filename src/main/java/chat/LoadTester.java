package chat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Stress-tests the chat server.
 *
 * Default run: 50 clients each send 1,000 chat messages, while 5 "churn" threads
 * keep connecting and leaving. At the end it asks the server for /stats and
 * compares the server's count with the expected total (clients x messages).
 *
 * Options:
 *   --host localhost   --port 5000
 *   --clients 50       --messages 1000    --churn 5
 *   --hold             connect the clients, keep them open, send nothing (Part 5)
 *   --timeout 120      limit (seconds) on the main test wait; unfinished clients are reported.
 *                      Connecting, /stats requests and cleanup are not included.
 *
 * Run: java -cp target/classes chat.LoadTester [options]
 */
public class LoadTester {

    static String host = "localhost";
    static int port = ChatServer.DEFAULT_PORT;
    static int clients = 50;
    static int messages = 1000;
    static int churn = 5;
    static boolean hold = false;
    static int timeoutSeconds = 120;

    public static void main(String[] args) throws Exception {
        parseArgs(args);
        if (hold) {
            holdClients();
        } else {
            runLoadTest();
        }
    }

    // ---------------------------------------------------------------- load test
    static void runLoadTest() throws Exception {
        long expected = (long) clients * messages;
        System.out.printf("Load test: %d clients x %d messages = %,d expected, %d churn threads%n",
                clients, messages, expected, churn);

        // The server's counter keeps growing across runs, so measure the difference.
        long before = parseCount(askForStats());

        CountDownLatch go = new CountDownLatch(1);
        List<Socket> sockets = new ArrayList<>();
        List<Thread> readers = new ArrayList<>();
        List<Thread> senders = new ArrayList<>();
        AtomicInteger connectFailures = new AtomicInteger();
        AtomicInteger notClosed = new AtomicInteger();
        AtomicInteger writeErrors = new AtomicInteger();

        for (int i = 0; i < clients; i++) {
            final int id = i;
            Socket socket;
            try {
                socket = new Socket(host, port);
            } catch (IOException e) {
                System.out.println("Could not connect client " + id + ": " + e.getMessage());
                connectFailures.incrementAndGet();
                continue;
            }
            sockets.add(socket);
            // Stop reading if no data arrives for 15 s (handler crashed, queued or very slow).
            socket.setSoTimeout(15_000);
            BufferedReader in = reader(socket);
            PrintWriter out = writer(socket);

            // Reader: drain everything the server sends; ends when the server closes the socket.
            Thread r = new Thread(() -> {
                if (!drain(in)) {
                    notClosed.incrementAndGet();
                }
            }, "reader-" + id);
            r.start();
            readers.add(r);

            // Sender: nickname, wait for the start signal, send the messages, then /quit.
            Thread s = new Thread(() -> {
                out.println("load" + id);
                try {
                    go.await();
                } catch (InterruptedException e) {
                    return;
                }
                for (int m = 0; m < messages; m++) {
                    out.println("msg " + m + " from load" + id);
                }
                out.println("/quit");
                // PrintWriter hides write failures; checkError() reveals them.
                if (out.checkError()) {
                    writeErrors.incrementAndGet();
                }
            }, "sender-" + id);
            s.start();
            senders.add(s);
        }

        AtomicBoolean running = new AtomicBoolean(true);
        AtomicInteger churnConnections = new AtomicInteger();
        List<Thread> churners = new ArrayList<>();
        for (int c = 0; c < churn; c++) {
            final int id = c;
            churners.add(new Thread(() -> churnLoop(id, running, churnConnections), "churn-" + id));
        }

        Thread.sleep(500);                       // let everyone connect
        long start = System.nanoTime();
        long deadline = System.currentTimeMillis() + timeoutSeconds * 1000L;
        go.countDown();
        churners.forEach(Thread::start);

        // Wait for senders, then for the server to close each client after its /quit,
        // but never longer than the overall deadline.
        int sendersUnfinished = joinAll(senders, deadline);
        int readersUnfinished = joinAll(readers, deadline);

        running.set(false);
        joinAll(churners, System.currentTimeMillis() + 10_000);

        // Close everything we opened; this also unblocks any thread still waiting.
        for (Socket sk : sockets) {
            try {
                sk.close();
            } catch (IOException ignored) {
                // already closed
            }
        }
        joinAll(senders, System.currentTimeMillis() + 2_000);
        joinAll(readers, System.currentTimeMillis() + 2_000);
        long ms = (System.nanoTime() - start) / 1_000_000;

        String stats = askForStats();
        long after = parseCount(stats);
        System.out.println();
        System.out.printf("Churn connections made: %d%n", churnConnections.get());
        if (connectFailures.get() > 0) {
            System.out.printf("Clients that failed to connect: %d%n", connectFailures.get());
        }
        if (writeErrors.get() > 0) {
            System.out.printf("Clients with write errors: %d%n", writeErrors.get());
        }
        if (notClosed.get() > 0) {
            System.out.printf("Clients with no data before the 15 s read timeout: %d"
                    + " (handler crashed, still queued or very slow - check the server console)%n",
                    notClosed.get());
        }
        if (sendersUnfinished > 0 || readersUnfinished > 0) {
            System.out.printf("Not finished within the %d s test wait: %d senders, %d clients"
                    + " - the test was stopped (stalled, queued or crashed server threads?)%n",
                    timeoutSeconds, sendersUnfinished, readersUnfinished);
        }
        System.out.printf("Expected messages:      %,d%n", expected);
        if (before >= 0 && after >= 0) {
            long counted = after - before;
            System.out.printf("Server counted:         %,d   (lost: %,d)%n", counted, expected - counted);
        } else {
            System.out.println("Server reports:         " + stats);
        }
        System.out.printf("Time:                   %,d ms%n", ms);
    }

    /** Joins each thread until the deadline; returns how many are still running. */
    static int joinAll(List<Thread> threads, long deadlineMillis) throws InterruptedException {
        int unfinished = 0;
        for (Thread t : threads) {
            long remaining = deadlineMillis - System.currentTimeMillis();
            if (remaining > 0) {
                t.join(remaining);
            }
            if (t.isAlive()) {
                unfinished++;
            }
        }
        return unfinished;
    }

    /** Repeatedly joins and leaves the chat, changing the shared client list. */
    static void churnLoop(int id, AtomicBoolean running, AtomicInteger count) {
        int n = 0;
        while (running.get()) {
            try (Socket socket = new Socket(host, port)) {
                socket.setSoTimeout(5000);
                PrintWriter out = writer(socket);
                BufferedReader in = reader(socket);
                out.println("churn" + id + "-" + n++);
                Thread.sleep(20);
                out.println("/quit");
                drain(in);                       // read until the server closes the socket
                count.incrementAndGet();
            } catch (IOException | InterruptedException e) {
                // server busy or connection dropped: try again
            }
        }
    }

    /** Opens a separate connection and asks the server for its message count. */
    static String askForStats() {
        try (Socket socket = new Socket(host, port)) {
            socket.setSoTimeout(10_000);
            PrintWriter out = writer(socket);
            BufferedReader in = reader(socket);
            out.println("stats-checker");
            out.println("/stats");
            String line;
            while ((line = in.readLine()) != null) {
                if (line.startsWith("stats:")) {
                    out.println("/quit");
                    return line;
                }
            }
            return "(no /stats reply - is /stats implemented?)";
        } catch (SocketTimeoutException e) {
            return "(timed out waiting for /stats)";
        } catch (IOException e) {
            return "(could not connect: " + e.getMessage() + ")";
        }
    }

    /** Extracts N from "stats: N messages"; returns -1 if there is no number. */
    static long parseCount(String stats) {
        try {
            return Long.parseLong(stats.replaceAll("\\D+", " ").trim().split(" ")[0]);
        } catch (RuntimeException e) {
            return -1;
        }
    }

    // ---------------------------------------------------------------- hold mode
    /** Part 5: opens N idle clients and keeps them connected until Enter is pressed. */
    static void holdClients() throws IOException {
        List<Socket> sockets = new ArrayList<>();
        for (int i = 0; i < clients; i++) {
            Socket socket = new Socket(host, port);
            sockets.add(socket);
            BufferedReader in = reader(socket);
            writer(socket).println("hold" + i);
            Thread r = new Thread(() -> drain(in), "reader-hold" + i);
            r.setDaemon(true);
            r.start();
        }
        System.out.println(clients + " idle clients connected (hold0 .. hold" + (clients - 1) + ").");
        System.out.println("Press Enter to disconnect them.");
        new BufferedReader(new InputStreamReader(System.in)).readLine();
        for (Socket s : sockets) {
            writer(s).println("/quit");
            s.close();
        }
        System.out.println("Disconnected.");
    }

    // ---------------------------------------------------------------- helpers
    /** Reads and discards lines. Returns true if the server closed the connection cleanly. */
    static boolean drain(BufferedReader in) {
        try {
            while (in.readLine() != null) {
                // discard
            }
            return true;
        } catch (SocketTimeoutException e) {
            return false;                        // no data before the read timeout
        } catch (IOException e) {
            return true;                         // connection reset or closed by us
        }
    }

    static BufferedReader reader(Socket s) throws IOException {
        return new BufferedReader(new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8));
    }

    static PrintWriter writer(Socket s) throws IOException {
        return new PrintWriter(new OutputStreamWriter(s.getOutputStream(), StandardCharsets.UTF_8), true);
    }

    static void parseArgs(String[] args) {
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--host" -> host = args[++i];
                case "--port" -> port = Integer.parseInt(args[++i]);
                case "--clients" -> clients = Integer.parseInt(args[++i]);
                case "--messages" -> messages = Integer.parseInt(args[++i]);
                case "--churn" -> churn = Integer.parseInt(args[++i]);
                case "--hold" -> hold = true;
                case "--timeout" -> timeoutSeconds = Integer.parseInt(args[++i]);
                default -> {
                    System.out.println("Unknown option: " + args[i]);
                    System.exit(1);
                }
            }
        }
    }
}
