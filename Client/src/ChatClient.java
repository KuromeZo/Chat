import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.Socket;

public class ChatClient {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private JFrame frame;
    private JTextArea chatArea;
    private JTextField messageField;
    private JButton sendButton;
    public ChatClient(String host, int port) {
        setupGUI();

        try {
            connectToServer(host, port);
        } catch (IOException e) {
            chatArea.append("Could not connect to the server.\n");
            e.printStackTrace();
        }
    }

    private void setupGUI() {
        frame = new JFrame("Chat Client");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(400, 300);

        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);

        JScrollPane chatScrollPane = new JScrollPane(chatArea);

        messageField = new JTextField();
        sendButton = new JButton("Send");

        // Обработчик кнопки отправки
        sendButton.addActionListener(e -> sendMessage());
        // Отправка по Enter
        messageField.addActionListener(e -> sendMessage());

        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new BorderLayout());
        inputPanel.add(messageField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);

        frame.setLayout(new BorderLayout());
        frame.add(chatScrollPane, BorderLayout.CENTER);
        frame.add(inputPanel, BorderLayout.SOUTH);

        frame.setVisible(true);
    }

    private void connectToServer(String host, int port) throws IOException {
        socket = new Socket(host, port);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);

        // Thread of getting messages from server
        new Thread(() -> {
            try {
                String message;
                while ((message = in.readLine()) != null) {
                    System.out.println("Получено от сервера: " + message); // log
                    String finalMessage = message;
                    SwingUtilities.invokeLater(() -> chatArea.append(finalMessage + "\n")); // Обновление UI в EDT
                }
            } catch (IOException e) {
                SwingUtilities.invokeLater(() -> chatArea.append("Disconnected from server.\n"));
                e.printStackTrace();
            }
        }).start();
    }

    private void sendMessage() {
        String message = messageField.getText().trim();
        if (!message.isEmpty()) {
            System.out.println("Отправляю сообщение: " + message); // log
            out.println(message);
            messageField.setText(""); // Clearing the input field
        }
    }


    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ChatClient("192.168.178.24", 8080));
    }
}
