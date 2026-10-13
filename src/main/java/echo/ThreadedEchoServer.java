package echo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * Part 2: a MULTITHREADED echo server.
 * The main thread accepts connections; a worker thread serves each client.
 *
 * Run: echo.ThreadedEchoServer [port]   (default port 5000)
 */
public class ThreadedEchoServer {

    public static void main(String[] args) throws IOException {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 5000;

        try (ServerSocket server = new ServerSocket(port)) {
            System.out.println("Threaded echo server listening on port " + port);

            while (true) {
                Socket socket = server.accept();          // wait for the next client
                System.out.println("Client connected: " + socket.getRemoteSocketAddress());
                new Thread(() -> handle(socket)).start();
            }
        }
    }

    /** Talks to one client: reads lines and sends each one back. */
    static void handle(Socket socket) {
        System.out.println("Serving " + socket.getRemoteSocketAddress()
                + " on " + Thread.currentThread().getName());
        try (socket;
             BufferedReader in = new BufferedReader(
                     new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             PrintWriter out = new PrintWriter(
                     new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true)) {

            out.println("Welcome! Type 'bye' to quit.");
            String line;
            while ((line = in.readLine()) != null) {      // blocks until the client sends a line
                if (line.equalsIgnoreCase("bye")) {
                    out.println("Goodbye");
                    break;
                }
                out.println("echo: " + line);
            }
        } catch (IOException e) {
            System.out.println("Connection error: " + e.getMessage());
        }
    }
}
