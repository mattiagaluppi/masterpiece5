// Importazione delle librerie necessarie
import java.awt.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.swing.*;

// Classe principale del server
public class Server {
    private static final int SERVER_PORT = 12345; // Porta su cui il server ascolta

    private DatagramSocket socket; // Socket per la comunicazione UDP
    private JTextArea logArea; // Area di testo per mostrare i log nella GUI

    private KeyPair rsaKeyPair; // Coppia di chiavi RSA per crittografia asimmetrica
    private ConcurrentHashMap<String, SecretKey> clientKeys; // Mappa delle chiavi AES per ogni client

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Server::new); // Avvia l'applicazione GUI in un thread separato
    }

    // Costruttore della classe Server
    public Server() {
        // Configurazione della finestra principale
        JFrame frame = new JFrame("Server - Chat Segreta");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // Chiude l'applicazione alla chiusura della finestra
        frame.setSize(600, 400); // Dimensioni della finestra

        logArea = new JTextArea(); // Area di testo per i log
        logArea.setEditable(false); // Imposta l'area di testo come non modificabile
        JScrollPane scrollPane = new JScrollPane(logArea); // Aggiunge uno scroll all'area di testo

        frame.setLayout(new BorderLayout()); // Layout della finestra
        frame.add(scrollPane, BorderLayout.CENTER); // Posiziona l'area di testo al centro
        frame.setVisible(true); // Rende visibile la finestra

        // Avvia il server in un thread separato
        new Thread(this::startServer).start();
    }

    // Metodo per avviare il server
    private void startServer() {
        try {
            socket = new DatagramSocket(SERVER_PORT); // Inizializza il socket UDP sulla porta specificata
            rsaKeyPair = generateRSAKeyPair(); // Genera una coppia di chiavi RSA
            clientKeys = new ConcurrentHashMap<>(); // Inizializza la mappa delle chiavi dei client

            log("Server avviato sulla porta " + SERVER_PORT); // Log dell'avvio del server

            byte[] buffer = new byte[1024]; // Buffer per ricevere i dati
            while (true) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length); // Pacchetto per ricevere dati
                socket.receive(packet); // Riceve un pacchetto dal client

                handlePacket(packet); // Gestisce il pacchetto ricevuto
            }
        } catch (Exception e) {
            log("Errore: " + e.getMessage()); // Log in caso di errore
        }
    }

    // Metodo per gestire i pacchetti ricevuti
    private void handlePacket(DatagramPacket packet) {
        try {
            String message = new String(packet.getData(), 0, packet.getLength()); // Converte i dati ricevuti in stringa
            InetAddress clientAddress = packet.getAddress(); // Indirizzo del client
            int clientPort = packet.getPort(); // Porta del client
            String clientKey = clientAddress.getHostAddress() + ":" + clientPort; // Chiave univoca per identificare il client

            log("Pacchetto ricevuto da " + clientAddress + ":" + clientPort); // Log dell'arrivo di un pacchetto

            if (message.startsWith("KEY_REQUEST")) {
                sendPublicKey(clientAddress, clientPort); // Invia la chiave pubblica al client

            } else if (message.startsWith("AES_KEY:")) {
                String encryptedKeyBase64 = message.substring("AES_KEY:".length()); // Estrae la chiave cifrata
                byte[] encryptedKey = Base64.getDecoder().decode(encryptedKeyBase64); // Decodifica Base64
                SecretKey aesKey = decryptAESKey(encryptedKey); // Decifra la chiave AES
                clientKeys.put(clientKey, aesKey); // Salva la chiave AES per il client
                log("Chiave AES salvata per il client: " + clientAddress + ":" + clientPort);

            } else {
                SecretKey aesKey = clientKeys.get(clientKey); // Recupera la chiave AES del client
                if (aesKey == null) {
                    log("Errore: chiave AES non trovata per il client: " + clientAddress + ":" + clientPort);
                    return;
                }

                Cipher cipher = Cipher.getInstance("AES");
                cipher.init(Cipher.DECRYPT_MODE, aesKey); // Decifra il messaggio con la chiave AES
                byte[] decryptedMessage = cipher.doFinal(packet.getData(), 0, packet.getLength());
                String decryptedText = new String(decryptedMessage); // Converte il messaggio decifrato in stringa

                log("Messaggio ricevuto (crittografato): " + Base64.getEncoder().encodeToString(packet.getData())); // Log del messaggio cifrato
                broadcastMessage(decryptedText, clientAddress, clientPort); // Invia il messaggio agli altri client
            }
        } catch (Exception e) {
            log("Errore durante la gestione del pacchetto: " + e.getMessage()); // Log di errore
        }
    }

    // Invia la chiave pubblica RSA al client
    private void sendPublicKey(InetAddress clientAddress, int clientPort) throws Exception {
        String publicKeyBase64 = Base64.getEncoder().encodeToString(rsaKeyPair.getPublic().getEncoded()); // Codifica la chiave in Base64
        DatagramPacket packet = new DatagramPacket(publicKeyBase64.getBytes(), publicKeyBase64.length(),
                clientAddress, clientPort);
        socket.send(packet); // Invia il pacchetto contenente la chiave pubblica
        log("Chiave pubblica inviata al client: " + clientAddress + ":" + clientPort);
    }

    // Decifra la chiave AES ricevuta dal client
    private SecretKey decryptAESKey(byte[] encryptedKey) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA"); // Configura il cifratore RSA
        cipher.init(Cipher.DECRYPT_MODE, rsaKeyPair.getPrivate()); // Inizializza in modalità decifratura con la chiave privata
        byte[] decryptedKey = cipher.doFinal(encryptedKey); // Decifra la chiave AES
        return new SecretKeySpec(decryptedKey, "AES"); // Crea un oggetto SecretKey dalla chiave decifrata
    }

    // Invia un messaggio a tutti i client connessi
    private void broadcastMessage(String message, InetAddress senderAddress, int senderPort) {
        try {
            for (String clientKey : clientKeys.keySet()) {
                String[] clientInfo = clientKey.split(":"); // Divide l'indirizzo e la porta del client
                InetAddress clientAddress = InetAddress.getByName(clientInfo[0]);
                int clientPort = Integer.parseInt(clientInfo[1]);

                if (!(clientAddress.equals(senderAddress) && clientPort == senderPort)) { // Evita di rinviare al mittente
                    SecretKey aesKey = clientKeys.get(clientKey); // Recupera la chiave AES del client

                    Cipher cipher = Cipher.getInstance("AES");
                    cipher.init(Cipher.ENCRYPT_MODE, aesKey); // Cifra il messaggio con la chiave AES
                    byte[] encryptedMessage = cipher.doFinal(message.getBytes());

                    DatagramPacket packet = new DatagramPacket(encryptedMessage, encryptedMessage.length,
                            clientAddress, clientPort);
                    socket.send(packet); // Invia il pacchetto cifrato al client
                }
            }
        } catch (Exception e) {
            log("Errore durante l'invio del messaggio: " + e.getMessage()); // Log di errore
        }
    }

    // Genera una coppia di chiavi RSA
    private KeyPair generateRSAKeyPair() throws Exception {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA"); // Configura il generatore di chiavi RSA
        keyGen.initialize(2048); // Lunghezza della chiave in bit
        return keyGen.generateKeyPair(); // Genera la coppia di chiavi
    }

    // Aggiunge un messaggio al log della GUI
    private void log(String message) {
        SwingUtilities.invokeLater(() -> logArea.append(message + "\n")); // Aggiorna l'area di log nella GUI
    }
}
