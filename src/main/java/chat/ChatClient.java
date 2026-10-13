package chat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * Interactive chat client.
 * One thread prints messages from the server; the main thread sends what you type.
 *
 * Run: java -cp target/classes chat.ChatClient [host] [port] [nickname]
 */
public class ChatClient {

    public static void main(String[] args) throws IOException {
        String host = args.length > 0 ? args[0] : "localhost";
        int port = args.length > 1 ? Integer.parseInt(args[1]) : ChatServer.DEFAULT_PORT;
        String nickname = args.length > 2 ? args[2] : "user" + (int) (Math.random() * 1000);

        Socket socket = new Socket(host, port);
        BufferedReader in = new BufferedReader(
                new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        PrintWriter out = new PrintWriter(
                new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);

        // Background thread: print everything the server sends.
        Thread receiver = new Thread(() -> {
            try {
                String line;
                while ((line = in.readLine()) != null) {
                    System.out.println(line);
                }
            } catch (IOException e) {
                // socket closed
            }
            System.out.println("-- disconnected from server --");
            System.exit(0);
        }, "receiver");
        receiver.setDaemon(true);
        receiver.start();

        out.println(nickname);                            // first line = nickname
        System.out.println("Connected as " + nickname + ". Commands: /list  /stats  /quit");

        // Main thread: send keyboard input.
        BufferedReader keyboard = new BufferedReader(new InputStreamReader(System.in));
        String line;
        while ((line = keyboard.readLine()) != null) {
            out.println(line);
            if (line.equals("/quit")) {
                break;
            }
        }
        socket.close();
    }
}
