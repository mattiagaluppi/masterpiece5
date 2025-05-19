// Importazioni delle librerie necessarie
import java.awt.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

// Classe principale del client
public class Client {
    private static final int SERVER_PORT = 12345; // Porta del server a cui connettersi
    private static final String SERVER_ADDRESS = "localhost"; // Indirizzo del server

    private DatagramSocket socket; // Socket per la comunicazione
    private SecretKey aesKey; // Chiave AES per la crittografia simmetrica
    private PublicKey serverPublicKey; // Chiave pubblica del server per la crittografia asimmetrica

    private JPanel messagePanel; // Pannello per visualizzare i messaggi
    private JTextField inputField; // Campo di input per digitare i messaggi
    private JScrollPane scrollPane; // Pannello scorrevole per i messaggi

    // Metodo principale che avvia il client
    public static void main(String[] args) {
        SwingUtilities.invokeLater(Client::new); // Esegue il costruttore del client nella thread dell'interfaccia grafica
    }

    // Costruttore della classe Client
    public Client() {
        // Creazione della finestra principale
        JFrame frame = new JFrame("Client - Chat Segreta");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // Comando per chiudere il programma quando si chiude la finestra
        frame.setSize(500, 600); // Imposta le dimensioni della finestra

        // Configurazione del pannello principale
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout()); // Layout del pannello principale
        mainPanel.setBackground(UIStyle.BACKGROUND_COLOR); // Imposta il colore di sfondo

        // Configurazione del pannello dei messaggi
        messagePanel = new JPanel();
        messagePanel.setLayout(new BoxLayout(messagePanel, BoxLayout.Y_AXIS)); // I messaggi sono disposti in verticale
        messagePanel.setBackground(UIStyle.BACKGROUND_COLOR);

        // Configurazione dello scroll per il pannello dei messaggi
        scrollPane = new JScrollPane(messagePanel);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER); // Disabilita lo scrolling orizzontale
        scrollPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); // Imposta un bordo vuoto attorno ai messaggi
        scrollPane.setBackground(UIStyle.BACKGROUND_COLOR);

        // Configurazione del campo di input per i messaggi
        inputField = new JTextField();
        inputField.setFont(UIStyle.INPUT_FONT); // Font del testo
        inputField.setForeground(UIStyle.INPUT_TEXT_COLOR); // Colore del testo
        inputField.setBackground(UIStyle.INPUT_BACKGROUND_COLOR); // Sfondo del campo
        inputField.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); // Bordo interno

        // Azione per l'invio del messaggio premendo "Invio"
        inputField.addActionListener(e -> sendMessage(inputField.getText()));

        // Pulsante per inviare il messaggio
        JButton sendButton = new JButton("Invia");
        sendButton.setFont(UIStyle.BUTTON_FONT);
        sendButton.setBackground(UIStyle.BUTTON_BACKGROUND_COLOR);
        sendButton.setForeground(UIStyle.BUTTON_TEXT_COLOR);
        sendButton.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        sendButton.addActionListener(e -> sendMessage(inputField.getText()));

        // Pannello inferiore per il campo di input e il pulsante
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBackground(UIStyle.BACKGROUND_COLOR);
        bottomPanel.add(inputField, BorderLayout.CENTER); // Campo di input centrato
        bottomPanel.add(sendButton, BorderLayout.EAST); // Pulsante posizionato a destra

        // Aggiunge i componenti al pannello principale
        mainPanel.add(scrollPane, BorderLayout.CENTER); // Scroll dei messaggi al centro
        mainPanel.add(bottomPanel, BorderLayout.SOUTH); // Campo di input e pulsante in basso
        frame.add(mainPanel);
        frame.setVisible(true); // Rende la finestra visibile

        // Avvia il client in un thread separato
        new Thread(this::startClient).start();
    }

    // Metodo per inizializzare il client
    private void startClient() {
        try {
            socket = new DatagramSocket(); // Crea un socket UDP
            requestPublicKey(); // Richiede la chiave pubblica del server
            generateAESKey(); // Genera e invia la chiave AES
            new Thread(this::receiveMessages).start(); // Avvia il thread per ricevere messaggi
        } catch (Exception e) {
            addMessage("Errore: " + e.getMessage(), "system"); // Mostra l'errore nella GUI
        }
    }

    // Richiede la chiave pubblica al server
    private void requestPublicKey() throws Exception {
        String request = "KEY_REQUEST"; // Messaggio di richiesta
        DatagramPacket packet = new DatagramPacket(request.getBytes(), request.length(),
                InetAddress.getByName(SERVER_ADDRESS), SERVER_PORT);
        socket.send(packet); // Invia il pacchetto al server

        byte[] buffer = new byte[1024]; // Buffer per la risposta
        packet = new DatagramPacket(buffer, buffer.length);
        socket.receive(packet); // Riceve la risposta del server

        String publicKeyBase64 = new String(packet.getData(), 0, packet.getLength()); // Decodifica la chiave pubblica
        byte[] publicKeyBytes = Base64.getDecoder().decode(publicKeyBase64);
        serverPublicKey = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(publicKeyBytes));

        addMessage("Chiave pubblica del server ricevuta.", "system");
    }

    // Genera una chiave AES e la invia al server
    private void generateAESKey() throws Exception {
        aesKey = KeyGenerator.getInstance("AES").generateKey(); // Genera una chiave AES casuale

        Cipher cipher = Cipher.getInstance("RSA"); // Configura il cifratore RSA
        cipher.init(Cipher.ENCRYPT_MODE, serverPublicKey); // Inizializza in modalità cifratura
        byte[] encryptedKey = cipher.doFinal(aesKey.getEncoded()); // Cifra la chiave AES
        String encryptedKeyBase64 = Base64.getEncoder().encodeToString(encryptedKey); // Codifica in Base64

        String keyMessage = "AES_KEY:" + encryptedKeyBase64; // Prepara il messaggio
        DatagramPacket packet = new DatagramPacket(keyMessage.getBytes(), keyMessage.length(),
                InetAddress.getByName(SERVER_ADDRESS), SERVER_PORT);
        socket.send(packet); // Invia la chiave cifrata

        addMessage("Chiave AES inviata al server.", "system");
    }

    // Riceve messaggi dal server
    private void receiveMessages() {
        try {
            byte[] buffer = new byte[1024];
            while (true) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet); // Riceve un pacchetto

                Cipher cipher = Cipher.getInstance("AES");
                cipher.init(Cipher.DECRYPT_MODE, aesKey); // Decifra con la chiave AES
                byte[] decryptedMessage = cipher.doFinal(packet.getData(), 0, packet.getLength());
                String message = new String(decryptedMessage); // Converte il messaggio in stringa

                addMessage(message, "received"); // Mostra il messaggio ricevuto
            }
        } catch (Exception e) {
            addMessage("Errore durante la ricezione del messaggio: " + e.getMessage(), "system");
        }
    }

    // Invia un messaggio al server
    private void sendMessage(String message) {
        try {
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, aesKey); // Cifra con la chiave AES
            byte[] encryptedMessage = cipher.doFinal(message.getBytes());

            DatagramPacket packet = new DatagramPacket(encryptedMessage, encryptedMessage.length,
                    InetAddress.getByName(SERVER_ADDRESS), SERVER_PORT);
            socket.send(packet); // Invia il pacchetto cifrato

            addMessage(message, "sent"); // Mostra il messaggio inviato nella GUI
            inputField.setText(""); // Pulisce il campo di input
        } catch (Exception e) {
            addMessage("Errore durante l'invio del messaggio: " + e.getMessage(), "system");
        }
    }

    // Aggiunge un messaggio alla GUI
    private void addMessage(String message, String type) {
        SwingUtilities.invokeLater(() -> {
            JPanel messageBubble = new JPanel() {
                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    Graphics2D g2 = (Graphics2D) g;
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                    if ("sent".equals(type)) {
                        g2.setColor(UIStyle.SENT_MESSAGE_COLOR); // Colore per messaggi inviati
                    } else if ("received".equals(type)) {
                        g2.setColor(UIStyle.RECEIVED_MESSAGE_COLOR); // Colore per messaggi ricevuti
                    } else {
                        g2.setColor(UIStyle.SYSTEM_MESSAGE_COLOR); // Colore per messaggi di sistema
                    }
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20); // Rende i bordi arrotondati
                }
            };

            messageBubble.setLayout(new BorderLayout()); // Layout interno al messaggio
            messageBubble.setOpaque(false);
            messageBubble.setBorder(new EmptyBorder(10, 15, 10, 15)); // Padding interno

            JLabel messageLabel = new JLabel("<html><body style='width: auto;'>" + message + "</body></html>");
            messageLabel.setFont(UIStyle.MESSAGE_FONT);
            messageLabel.setForeground(UIStyle.MESSAGE_TEXT_COLOR); // Colore del testo

            messageBubble.add(messageLabel, BorderLayout.CENTER); // Aggiunge il messaggio al pannello

            JPanel container = new JPanel(new BorderLayout());
            container.setOpaque(false);
            container.setBorder(new EmptyBorder(5, 10, 5, 10)); // Margine attorno ai messaggi

            if ("sent".equals(type)) {
                container.add(messageBubble, BorderLayout.EAST); // Allinea a destra
            } else {
                container.add(messageBubble, BorderLayout.WEST); // Allinea a sinistra
            }

            container.setMaximumSize(new Dimension(Integer.MAX_VALUE, messageLabel.getPreferredSize().height + 30));

            messagePanel.add(container); // Aggiunge il messaggio al pannello principale
            messagePanel.revalidate(); // Aggiorna la GUI
            scrollPane.getVerticalScrollBar().setValue(scrollPane.getVerticalScrollBar().getMaximum()); // Scorre verso il basso
        });
    }
}
