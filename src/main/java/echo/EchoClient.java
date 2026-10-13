package echo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * Simple client for the echo server: sends each keyboard line, prints the reply.
 *
 * Run: java -cp target/classes echo.EchoClient [host] [port]
 */
public class EchoClient {

    public static void main(String[] args) throws IOException {
        String host = args.length > 0 ? args[0] : "localhost";
        int port = args.length > 1 ? Integer.parseInt(args[1]) : 5000;

        try (Socket socket = new Socket(host, port);
             BufferedReader in = new BufferedReader(
                     new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             PrintWriter out = new PrintWriter(
                     new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
             BufferedReader keyboard = new BufferedReader(new InputStreamReader(System.in))) {

            System.out.println("Connected to " + host + ":" + port + " - waiting for the server...");
            System.out.println(in.readLine());            // welcome line (blocks if the server is busy)

            String line;
            while ((line = keyboard.readLine()) != null) {
                out.println(line);
                String reply = in.readLine();
                if (reply == null) {
                    System.out.println("Server closed the connection.");
                    break;
                }
                System.out.println(reply);
                if (line.equalsIgnoreCase("bye")) {
                    break;
                }
            }
        }
    }
}
