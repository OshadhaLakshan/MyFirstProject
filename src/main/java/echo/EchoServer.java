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
 * Part 1: a SINGLE-THREADED echo server.
 * It serves one client at a time: while handle() is running, the main thread
 * cannot return to accept(), so a second client has to wait.
 *
 * Run: echo.EchoServer [port]   (default port 5000)
 */
public class EchoServer {

    public static void main(String[] args) throws IOException {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 5000;

        try (ServerSocket server = new ServerSocket(port)) {
            System.out.println("Echo server listening on port " + port);

            while (true) {
                Socket socket = server.accept();          // wait for the next client
                System.out.println("Client connected: " + socket.getRemoteSocketAddress());
                handle(socket);                           // serve this client until it leaves
                System.out.println("Client disconnected");
            }
        }
    }

    /** Talks to one client: reads lines and sends each one back. */
    static void handle(Socket socket) {
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
