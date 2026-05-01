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
    private boolean useSpanishKeyboard = false;
    
    // Mapeo manual de caracteres especiales a códigos de tecla (INGLÉS)
    private static final Map<Character, Integer> CHAR_TO_KEYCODE_EN = new HashMap<>();
    
    // Mapeo manual de caracteres especiales a códigos de tecla (ESPAÑOL)
    private static final Map<Character, Integer> CHAR_TO_KEYCODE_ES = new HashMap<>();
    
    static {
        // Inicializar caracteres especiales - INGLÉS (QWERTY)
        CHAR_TO_KEYCODE_EN.put(';', KeyEvent.VK_SEMICOLON);
        CHAR_TO_KEYCODE_EN.put(',', KeyEvent.VK_COMMA);
        CHAR_TO_KEYCODE_EN.put('.', KeyEvent.VK_PERIOD);
        CHAR_TO_KEYCODE_EN.put('/', KeyEvent.VK_SLASH);
        CHAR_TO_KEYCODE_EN.put('.', KeyEvent.VK_1);
        
        // Inicializar caracteres especiales - ESPAÑOL (QWERTY)
        // Mapeamos a teclas que existen de forma segura en todos los teclados
        CHAR_TO_KEYCODE_ES.put(';', KeyEvent.VK_OPEN_BRACKET);  // [ (Ñ en español)
        CHAR_TO_KEYCODE_ES.put(',', KeyEvent.VK_QUOTE);          // ' (? en español)
        CHAR_TO_KEYCODE_ES.put('.', KeyEvent.VK_PERIOD);         // . (punto, igual)
        CHAR_TO_KEYCODE_ES.put('/', KeyEvent.VK_BACK_SLASH);     // \ (/ en español con Shift)
        CHAR_TO_KEYCODE_ES.put('.', KeyEvent.VK_1);              // 1 (igual)
    }
    
    // Obtiene el código de tecla para un carácter según el idioma
    private int getKeyCodeForChar(char c) {
        try {
            Map<Character, Integer> charMap = useSpanishKeyboard ? CHAR_TO_KEYCODE_ES : CHAR_TO_KEYCODE_EN;
            
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
    
    static {
        // Inicializar el mapa de notas a teclas
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
        NOTE_TO_KEY.put(".", "1");  // Nota de silencio o espacio en blanco
        NOTE_TO_KEY.put("C4", "."); 
        NOTE_TO_KEY.put("C5", "/");
    }

    // Mapeo de notas a teclas - método robusto que evita conflictos
    private static String replaceSent(String sentence) {
        StringBuilder result = new StringBuilder();
        int i = 0;
        
        while (i < sentence.length()) {
            // Intentar coincidir con notas de 2 caracteres (A1, A2, B3, C4, etc.)
            boolean matched = false;
            
            if (i + 1 < sentence.length()) {
                String twoCharNote = sentence.substring(i, i + 2);
                if (NOTE_TO_KEY.containsKey(twoCharNote)) {
                    result.append(NOTE_TO_KEY.get(twoCharNote));
                    i += 2;
                    matched = true;
                }
            }
            
            // Si no encontró nota de 2 caracteres, procesar el carácter actual
            if (!matched) {
                result.append(sentence.charAt(i));
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
     * @param isSpanish True si se usa teclado español, false si es inglés
     * @param progressCallback Callback para actualizar el progreso (0-100)
     * @throws java.io.IOException Si hay error al leer el archivo
     */
    public void playFromFile(String filePath, int delayMs, boolean isSpanish, Consumer<Integer> progressCallback) throws java.io.IOException {
        isRunning = true;
        useSpanishKeyboard = isSpanish;
        playbackThread = Thread.currentThread();
        
        String keyboardType = isSpanish ? "ESPAÑOL" : "INGLÉS";
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

                // Ignorar puntos (silencios/partituras en blanco)
                if (c == '.') {
                    processedChars++;
                    continue;
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
