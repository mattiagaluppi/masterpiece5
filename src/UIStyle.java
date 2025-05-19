import java.awt.*;

public class UIStyle {
    public static final Color BACKGROUND_COLOR = new Color(30, 30, 30); // Sfondo della finestra principale
    public static final Color INPUT_BACKGROUND_COLOR = new Color(45, 45, 45); // Sfondo campo input
    public static final Color INPUT_TEXT_COLOR = Color.WHITE; // Colore testo campo input
    public static final Font INPUT_FONT = new Font("Arial", Font.PLAIN, 16); // Font campo input

    public static final Color BUTTON_BACKGROUND_COLOR = new Color(70, 130, 180); // Sfondo pulsanti
    public static final Color BUTTON_TEXT_COLOR = Color.WHITE; // Colore testo pulsanti
    public static final Font BUTTON_FONT = new Font("Arial", Font.BOLD, 16); // Font pulsanti

    public static final Color SENT_MESSAGE_COLOR = new Color(0, 102, 204); // Colore messaggi inviati (blu intenso)
    public static final Color RECEIVED_MESSAGE_COLOR = new Color(34, 139, 34); // Colore messaggi ricevuti (verde scuro)
    public static final Color SYSTEM_MESSAGE_COLOR = new Color(255, 140, 0); // Colore messaggi di sistema (arancione scuro)

    public static final Color MESSAGE_TEXT_COLOR = Color.WHITE; // Colore del testo dei messaggi (bianco per massimo contrasto)
    public static final Font MESSAGE_FONT = new Font("Arial", Font.PLAIN, 16); // Font messaggi con dimensione maggiore
}
