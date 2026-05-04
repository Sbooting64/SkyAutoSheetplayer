package SkyMusicPlayer;

import java.awt.Robot;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import javax.swing.SwingUtilities;

/**
 * Clase AutoPlayer - Encargada de simular pulsaciones de teclas automáticamente.
 * Lee un archivo de texto con notas musicales y las convierte en pulsaciones.
 */
public class AutoPlayer {
    private volatile boolean isRunning = false;
    private Thread playbackThread;
    private boolean useWebKeyboard = false;
    
    // Mapeo manual de caracteres especiales a códigos de tecla (INGLÉS)
    private static final Map<Character, Integer> CHAR_TO_KEYCODE_EN = new HashMap<>();
    
    // Mapeo manual de caracteres especiales a códigos de tecla (WEB)
    private static final Map<Character, Integer> CHAR_TO_KEYCODE_WEB = new HashMap<>();
    
    static {
        // Inicializar caracteres especiales - INGLÉS (QWERTY)
        CHAR_TO_KEYCODE_EN.put(';', KeyEvent.VK_SEMICOLON);
        CHAR_TO_KEYCODE_EN.put(',', KeyEvent.VK_COMMA);
        CHAR_TO_KEYCODE_EN.put('.', KeyEvent.VK_PERIOD);
        CHAR_TO_KEYCODE_EN.put('/', KeyEvent.VK_SLASH);
        
        // Inicializar caracteres especiales - WEB (equivalente a español en este contexto)
        // Mapeamos a teclas que existen de forma segura en todos los teclados
        CHAR_TO_KEYCODE_WEB.put(';', KeyEvent.VK_OPEN_BRACKET);  
        CHAR_TO_KEYCODE_WEB.put(',', KeyEvent.VK_QUOTE);        
        CHAR_TO_KEYCODE_WEB.put('.', KeyEvent.VK_PERIOD);        
        CHAR_TO_KEYCODE_WEB.put('/', KeyEvent.VK_BACK_SLASH);     
    }
    
    // Obtiene el código de tecla para un carácter según el teclado seleccionado
    private int getKeyCodeForChar(char c) {
        try {
            Map<Character, Integer> charMap = useWebKeyboard ? CHAR_TO_KEYCODE_WEB : CHAR_TO_KEYCODE_EN;
            
            // Primero, intentar buscar en el mapa del idioma seleccionado
            if (charMap.containsKey(c)) {
                return charMap.get(c);
            }
            
            // Si no está en el mapa, usar el método estándar
            int keyCode = KeyEvent.getExtendedKeyCodeForChar(c);
            if (keyCode != KeyEvent.VK_UNDEFINED) {
                return keyCode;
            }
            
            // Como último recurso, intentar convertir a mayúscula
            int upperKeyCode = KeyEvent.getExtendedKeyCodeForChar(Character.toUpperCase(c));
            if (upperKeyCode != KeyEvent.VK_UNDEFINED) {
                return upperKeyCode;
            }
            
            // Si nada funciona, buscar por código ASCII
            return c;
        } catch (Exception e) {
            System.out.println("⚠ Error obteniendo código de tecla para '" + c + "': " + e.getMessage());
            return KeyEvent.VK_UNDEFINED;
        }
    }

    // Mapeo de notas a teclas usando HashMap para evitar conflictos
    private static final Map<String, String> NOTE_TO_KEY = new HashMap<>();
    private static final Map<String, String> NOTE_TO_KEY_WEB = new HashMap<>();
    
    static {
        // Inicializar el mapa de notas a teclas para teclado normal
        NOTE_TO_KEY.put("A1", "y");
        NOTE_TO_KEY.put("A2", "u");
        NOTE_TO_KEY.put("A3", "i");
        NOTE_TO_KEY.put("A4", "o");
        NOTE_TO_KEY.put("A5", "p");
        NOTE_TO_KEY.put("B1", "h");
        NOTE_TO_KEY.put("B2", "j");
        NOTE_TO_KEY.put("B3", "k");
        NOTE_TO_KEY.put("B4", "l");
        NOTE_TO_KEY.put("B5", ";");
        NOTE_TO_KEY.put("C1", "n");
        NOTE_TO_KEY.put("C2", "m");
        NOTE_TO_KEY.put("C3", ",");
        NOTE_TO_KEY.put(".", "");       // Silencio - no presiona nada
        NOTE_TO_KEY.put("C4", ".");     // C4 - presiona la tecla .
        NOTE_TO_KEY.put("C5", "/");

        // Inicializar el mapa de notas a teclas para teclado Web (misma asignación de notas)
        NOTE_TO_KEY_WEB.put("A1", "q");
        NOTE_TO_KEY_WEB.put("A2", "w");
        NOTE_TO_KEY_WEB.put("A3", "e");
        NOTE_TO_KEY_WEB.put("A4", "r");
        NOTE_TO_KEY_WEB.put("A5", "t");
        NOTE_TO_KEY_WEB.put("B1", "a");
        NOTE_TO_KEY_WEB.put("B2", "s");
        NOTE_TO_KEY_WEB.put("B3", "d");
        NOTE_TO_KEY_WEB.put("B4", "f");
        NOTE_TO_KEY_WEB.put("B5", "g");
        NOTE_TO_KEY_WEB.put("C1", "z");
        NOTE_TO_KEY_WEB.put("C2", "x");
        NOTE_TO_KEY_WEB.put("C3", "c");
        NOTE_TO_KEY_WEB.put(".", "");       // Silencio - no presiona nada
        NOTE_TO_KEY_WEB.put("C4", "v");     // C4 - presiona la tecla .
        NOTE_TO_KEY_WEB.put("C5", "b");
    }

    // Mapeo de notas a teclas - método robusto que evita conflictos
    private String replaceSent(String sentence) {
        Map<String, String> noteMap = useWebKeyboard ? NOTE_TO_KEY_WEB : NOTE_TO_KEY;
        StringBuilder result = new StringBuilder();
        int i = 0;
        
        while (i < sentence.length()) {
            boolean matched = false;
            
            // Primero intentar coincidir con notas de 2 caracteres (A1, A2, B3, etc.)
            if (i + 1 < sentence.length()) {
                String twoCharNote = sentence.substring(i, i + 2);
                if (noteMap.containsKey(twoCharNote)) {
                    result.append(noteMap.get(twoCharNote));
                    i += 2;
                    matched = true;
                }
            }
            
            // Si no encontró nota de 2 caracteres, intentar carácter individual
            if (!matched) {
                String oneCharNote = String.valueOf(sentence.charAt(i));
                if (noteMap.containsKey(oneCharNote)) {
                    result.append(noteMap.get(oneCharNote));
                    i++;
                    matched = true;
                }
            }
            
            // Si no es una nota mapeada, ignorar (excepto espacios que actúan como separadores)
            if (!matched) {
                if (sentence.charAt(i) == ' ') {
                    result.append(' ');
                }
                i++;
            }
        }
        
        return result.toString();
    }

    // Simula la presión de teclas simultáneas
    private void pressKeys(String keys, Robot robot) {
        if (keys.isEmpty()) return;

        char[] charArray = keys.toCharArray();
        List<Integer> pressedCodes = new ArrayList<>();

        try {
            // Presionar todas las teclas del grupo
            for (char c : charArray) {
                try {
                    int keyCode = getKeyCodeForChar(c);
                    if (keyCode != KeyEvent.VK_UNDEFINED) {
                        robot.keyPress(keyCode);
                        pressedCodes.add(keyCode);
                        robot.delay(3);  // Pausa rápida entre presiones
                    } else {
                        System.out.println("⚠ Advertencia: No se puede mapear la tecla '" + c + "'");
                    }
                } catch (Exception e) {
                    System.out.println("⚠ Error al presionar '" + c + "': " + e.getMessage());
                }
            }

        // Espera para asegurar el registro de la combinación
        robot.delay(15);

            // Soltar todas las teclas en orden inverso (mejor práctica)
            for (int i = pressedCodes.size() - 1; i >= 0; i--) {
                try {
                    robot.keyRelease(pressedCodes.get(i));
                    robot.delay(3);  // Pausa rápida entre liberaciones
                } catch (Exception e) {
                    System.out.println("⚠ Error al liberar tecla: " + e.getMessage());
                }
            }
            
            // Espera después de soltar todas las teclas
            robot.delay(5);
        } catch (Exception e) {
            System.out.println("✗ Error crítico en pressKeys: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Reproduce un archivo de música automáticamente.
     * 
     * @param filePath Ruta al archivo .txt con las notas
     * @param delayMs Retraso en milisegundos entre pulsaciones
     * @param useWebKeyboard True si se usa teclado Web, false si es Inglés
     * @param progressCallback Callback para actualizar el progreso (0-100)
     * @throws java.io.IOException Si hay error al leer el archivo
     */
    public void playFromFile(String filePath, int delayMs, boolean useWebKeyboard, Consumer<Integer> progressCallback) throws java.io.IOException {
        isRunning = true;
        this.useWebKeyboard = useWebKeyboard;
        playbackThread = Thread.currentThread();
        
        String keyboardType = useWebKeyboard ? "WEB" : "INGLÉS";
        System.out.println("Iniciando reproducción con teclado: " + keyboardType);

        try {
            // Leer archivo
            StringBuilder content = new StringBuilder();
            BufferedReader reader = new BufferedReader(new FileReader(filePath));
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append(" ");
            }
            reader.close();

            // Convertir notas a teclas
            String modifiedSentence = replaceSent(content.toString().trim());
            Robot robot = new Robot();

            // Espera de 1 segundo antes de empezar
            Thread.sleep(1000);

            String groupChars = "";
            int totalChars = modifiedSentence.length();
            int processedChars = 0;
            int noteCount = 0;

            for (char c : modifiedSentence.toCharArray()) {
                // Verificar si se solicitó detener
                if (!isRunning || Thread.currentThread().isInterrupted()) {
                    System.out.println("Reproducción detenida.");
                    return;
                }

                if (c == ' ') {
                    if (!groupChars.isEmpty()) {
                        // Presionar grupo de teclas simultáneamente
                        System.out.println("[Nota " + (++noteCount) + "] Presionando: " + groupChars);
                        pressKeys(groupChars, robot);
                        
                        // Aumentar delay mínimamente si hay caracteres especiales problemáticos
                        int adjustedDelay = delayMs;
                        if (groupChars.contains(";") || groupChars.contains(",") || groupChars.contains("/")) {
                            adjustedDelay = Math.max(delayMs, 300);  // Mínimo 300ms para caracteres especiales
                        }
                        
                        Thread.sleep(adjustedDelay);
                        groupChars = "";
                    } else {
                        // Espera por espacio vacío
                        Thread.sleep(300);
                    }
                } else {
                    groupChars += c;
                }

                // Actualizar progreso
                processedChars++;
                int progress = (int) ((processedChars * 100) / totalChars);
                if (progressCallback != null) {
                    progressCallback.accept(progress);
                }
            }

            // Presionar teclas restantes
            if (!groupChars.isEmpty() && isRunning) {
                System.out.println("[Nota " + (++noteCount) + "] Presionando: " + groupChars);
                pressKeys(groupChars, robot);
            }

            System.out.println("✓ Reproducción completada. Total de notas: " + noteCount);

        } catch (InterruptedException e) {
            System.out.println("Reproducción interrumpida.");
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            throw new java.io.IOException("Error durante la reproducción: " + e.getMessage(), e);
        } finally {
            isRunning = false;
        }
    }

    /**
     * Detiene la reproducción actual.
     */
    public void stop() {
        isRunning = false;
        if (playbackThread != null && playbackThread.isAlive()) {
            playbackThread.interrupt();
        }
    }

    /**
     * Comprueba si hay reproducción en curso.
     */
    public boolean isPlaying() {
        return isRunning;
    }

    public static void main(String[] args) {
        // Para mantener compatibilidad, se llama a SkyMusicGUI
        SwingUtilities.invokeLater(() -> {
            new SkyMusicGUI().setVisible(true);
        });
    }
}
