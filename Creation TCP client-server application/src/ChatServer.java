import java.io.*;
import java.net.*;
import java.util.*;

public class ChatServer {
    private static final int PORT = 12345;
    private static Map<String, ClientHandler> clients = new HashMap<>();

    public static void main(String[] args) {
        System.out.println("Сервер запущено...");
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket socket = serverSocket.accept();
                new ClientHandler(socket).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static class ClientHandler extends Thread {
        private Socket socket;
        private String clientName;
        private BufferedReader in;
        private PrintWriter out;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                out.println("Ведіть ваше ім'я:");
                clientName = in.readLine();

                synchronized (clients) {
                    if (clients.containsKey(clientName)) {
                        out.println("Таке ім'я вже є. Відключення...");
                        socket.close();
                        return;
                    }
                    clients.put(clientName, this);
                }

                out.println("Вітаємо " + clientName + "! Напишіть 'exit' щоб від'єднатись.");
                System.out.println(clientName + " підключен.");

                String message;
                while ((message = in.readLine()) != null) {
                    if (message.equalsIgnoreCase("exit")) {
                        break;
                    }
                    handleMessage(message);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                disconnect();
            }
        }

        private void handleMessage(String message) {
            int colonIndex = message.indexOf(":");
            if (colonIndex == -1) {
                out.println("Invalid message format. Use 'recipientName:message'");
                return;
            }

            String recipientName = message.substring(0, colonIndex).trim();
            String msg = message.substring(colonIndex + 1).trim();

            synchronized (clients) {
                ClientHandler recipient = clients.get(recipientName);
                if (recipient != null) {
                    recipient.out.println(clientName + ": " + msg);
                } else {
                    out.println("Користувач " + recipientName + " не знайден.");
                }
            }
        }

        private void disconnect() {
            try {
                synchronized (clients) {
                    clients.remove(clientName);
                }
                socket.close();
                System.out.println(clientName + " disconnected.");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}