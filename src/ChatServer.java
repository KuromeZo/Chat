import java.io.*;
import java.net.*;
import java.util.*;

public class ChatServer {
    private int port;
    private String serverName;
    private final Set<String> bannedPhrases = new HashSet<>();
    private Map<String, ClientHandler> clients = new HashMap<>();

    public ChatServer(String configFilePath) throws IOException {
        loadConfig(configFilePath);
    }

    private void loadConfig(String filePath) throws IOException {
        Properties properties = new Properties();
        try (FileInputStream fis = new FileInputStream(filePath)) {
            properties.load(fis);
            port = Integer.parseInt(properties.getProperty("port"));
            serverName = properties.getProperty("server_name");
            String[] phrases = properties.getProperty("banned_phrases",
                    "").split(",");
            bannedPhrases.addAll(Arrays.asList(phrases));
        }
    }

    public void start() throws IOException {
        System.out.println(serverName + " is starting on port " + port);
        try (ServerSocket serverSocket = new ServerSocket(8080, 50,
                InetAddress.getByName("0.0.0.0"))) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                new Thread(new ClientHandler(clientSocket)).start();
            }
        }
    }

    private class ClientHandler implements Runnable {
        private Socket socket;
        private String username;
        private PrintWriter out;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
                this.out = out;

                // Client registration
                while (true) {
                    out.println("Enter your username (or type \\exit to quit):");
                    username = in.readLine();

                    if (username == null || username.trim().isEmpty()) {
                        out.println("Invalid username. Please try again.");
                    } else if (username.equals("\\exit")) {
                        out.println("Goodbye!");
                        return; // Terminate the connection
                    } else {
                        synchronized (clients) {
                            if (clients.containsKey(username)) {
                                out.println("Username already taken. Please choose another.");
                            } else {
                                clients.put(username, this);
                                broadcast(username + " has joined the chat.", null);
                                sendCommandList(out);
                                break;
                            }
                        }
                    }
                }

                // Message processing
                String message;
                while ((message = in.readLine()) != null) {
                    if (message.equals("\\exit")) {
                        out.println("Goodbye!");
                        break; // Disconnect the user
                    } else if (containsBannedPhrase(message)) {
                        out.println("Message contains a banned phrase and will not be sent.");
                    } else if (message.startsWith("\\blocked")) {
                        sendBlockedWords(out);
                    } else if (message.startsWith("\\allUsers")) {
                        sendAllUsers(out);
                    } else {
                        broadcast(message, username);
                        System.out.println("Message sent: " + message); // log
                    }
                }

            } catch (IOException e) {
                System.err.println("Error with client " + username + ": " + e.getMessage());
            } finally {
                disconnect();
            }
        }

        private void sendCommandList(PrintWriter out) {
            out.println("Welcome to the chat! Here are the available commands:");
            out.println("\\blocked - Show the list of blocked phrases.");
            out.println("\\allUsers - Show the list of all connected users.");
            out.println("\\exit - Disconnect from the chat.");
            out.println("To send a private message, use the format \\username: message");
            out.println("To send a message to everyone except specific users, use -\\username: message");
            out.println("Enjoy your time!");
        }

        private void sendAllUsers(PrintWriter out) {
            synchronized (clients) {
                if (clients.isEmpty()) {
                    out.println("No users are currently connected.");
                } else {
                    out.println("Connected users: " + String.join(", ", clients.keySet()));
                }
            }
        }

        private void sendBlockedWords(PrintWriter out) {
            out.println("Blocked phrases: " + String.join(", ", bannedPhrases));
        }
        private boolean containsBannedPhrase(String message) {
            for (String phrase : bannedPhrases) {
                if (message.contains(phrase)) {
                    return true;
                }
            }
            return false;
        }

        private void broadcast(String message, String sender) {
            synchronized (clients) {
                if (message.contains(":")) {
                    String[] parts = message.split(":", 2);
                    String targetPart = parts[0].trim();
                    String content = parts[1].trim();

                    if (targetPart.startsWith("\\")) {
                        // Format: \name1, name2
                        String recipients = targetPart.substring(1).trim();
                        String[] recipientNames = recipients.split(",\\s*");
                        StringBuilder notifiedUsers = new StringBuilder();

                        for (String name : recipientNames) {
                            ClientHandler client = clients.get(name.trim());
                            if (client != null) {
                                client.out.println(sender != null ? sender + ": " + content : content);
                                notifiedUsers.append(name).append(", ");
                            }
                        }

                        // Notify sender
                        if (sender != null) {
                            ClientHandler senderClient = clients.get(sender);
                            if (senderClient != null) {
                                String notification = "You send message to names: " +
                                        (notifiedUsers.length() > 0 ? notifiedUsers.substring(0,
                                                notifiedUsers.length() - 2) : "none");
                                senderClient.out.println(notification);
                                senderClient.out.println("Your message: " + content);
                            }
                        }

                    } else if (targetPart.startsWith("-\\")) {
                        // Format: -\name1, name2
                        String excluded = targetPart.substring(2).trim();
                        String[] excludedNames = excluded.split(",\\s*");
                        StringBuilder excludedUsers = new StringBuilder();

                        // Send to everyone except excluded
                        for (Map.Entry<String, ClientHandler> entry : clients.entrySet()) {
                            String clientName = entry.getKey();
                            if (!Arrays.asList(excludedNames).contains(clientName)) {
                                ClientHandler client = entry.getValue();
                                client.out.println(sender != null ? sender + ": " + content : content);
                            }
                        }

                        // Forming a list of excluded for sending notifications
                        for (String excludedName : excludedNames) {
                            excludedUsers.append(excludedName).append(", ");
                        }

                        // Notify sender
                        if (sender != null) {
                            ClientHandler senderClient = clients.get(sender);
                            if (senderClient != null) {
                                String notification = "You send message to everyone except names: " +
                                        (excludedUsers.length() > 0 ? excludedUsers.substring(0,
                                                excludedUsers.length() - 2) : "none");
                                senderClient.out.println(notification);
                                senderClient.out.println("Your message: " + content);
                            }
                        }

                    } else {
                        // Unknown format
                        if (sender != null) {
                            ClientHandler senderClient = clients.get(sender);
                            if (senderClient != null) {
                                senderClient.out.println("Invalid format. Use \\name1, name2: " +
                                        "message or -\\name1, name2: message.");
                            }
                        }
                    }
                } else {
                    // Regular sending to all
                    for (Map.Entry<String, ClientHandler> entry : clients.entrySet()) {
                        String clientName = entry.getKey();
                        ClientHandler clientHandler = entry.getValue();

                        if (sender == null || !clientName.equals(sender)) {
                            clientHandler.out.println(sender != null ? sender + ": " + message : message);
                        }
                    }

                    // Notify sender
                    if (sender != null) {
                        ClientHandler senderClient = clients.get(sender);
                        if (senderClient != null) {
                            senderClient.out.println("You send message to everyone:");
                            senderClient.out.println("Your message: " + message);
                        }
                    }
                }
            }
        }

        private void disconnect() {
            try {
                synchronized (clients) {
                    if (username != null && clients.containsKey(username)) {
                        clients.remove(username);
                        broadcast(username + " has left the chat.", null);
                    }
                }
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
            } catch (IOException e) {
                System.err.println("Error while disconnecting client " + username + ": " + e.getMessage());
            }
        }

    }

    public static void main(String[] args) {
        try {
            ChatServer server = new ChatServer("server_config.properties");
            server.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}