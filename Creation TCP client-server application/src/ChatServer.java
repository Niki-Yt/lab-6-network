import java.io.*;
import java.net.*;
import java.util.concurrent.ConcurrentHashMap;

public class ChatServer {
    private static final int PORT = 12345;
    private static ConcurrentHashMap<String, PrintWriter> users = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        System.out.println("Сервер запущено...");
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                new ClientHandler(serverSocket.accept()).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class ClientHandler extends Thread {
        private Socket socket;
        private String clientName;
        private PrintWriter out;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        public void run() {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                out = new PrintWriter(socket.getOutputStream(), true);
                out.println("Ведіть ваше ім'я:");
                clientName = in.readLine().trim();
                if (users.putIfAbsent(clientName, out) != null) {
                    out.println("Це ім'я вже зайняте. Спробуйте інше.");
                    socket.close();
                    return;
                }
                out.println("Вітаємо " + clientName + "! Напишіть 'exit' щоб від'єднатись.");
                System.out.println(clientName + " приєднався до чату.");
                System.out.println("Активні користувачі: " + users.keySet());

                String input;
                while ((input = in.readLine()) != null) {
                    if (input.equalsIgnoreCase("exit")) break;

                    String[] parts = input.split(":", 2);
                    if (parts.length == 2) {
                        String targetName = parts[0].trim();
                        String message = parts[1].trim();
                        PrintWriter targetOut = users.get(targetName);
                        if (targetOut != null) {
                            targetOut.println(clientName + ": " + message);
                        } else {
                            out.println("Користувач " + targetName + " не знайден.");
                        }
                    } else {
                        out.println("Неправильний формат. Використовуйте 'Ім'я: Повідомлення'.");
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (clientName != null) {
                    users.remove(clientName);
                    System.out.println(clientName + " відключився. Активні користувачі: " + users.keySet());
                }
            }
        }
    }
}