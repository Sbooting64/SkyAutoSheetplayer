/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package SkyMusicPlayer;

import java.awt.Robot;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import java.awt.FlowLayout;
/**
 *
 * @author botel
 */
public class AutoPlayer {
    // Definición de velocidades según las fuentes [1]
    private static final int SPEED_SLOW = 200; // ms (0.2s)
    private static final int SPEED_MEDIUM = 500; // ms (0.5s)
    private static final int SPEED_FAST = 600; // ms (0.6s)
    private static String filename = "";

    // Lógica de reemplazo de notas/teclas [1]
    public static String replaceSent(String sentence) {
        return sentence.replace("A1", "y").replace("A2", "u").replace("A3", "i")
                .replace("A4", "o").replace("A5", "p").replace("B1", "h")
                .replace("B2", "j").replace("B3", "k").replace("B4", "l")
                .replace("B5", ";").replace("C1", "n").replace("C2", "m")
                .replace("C3", ",").replace(".", "1").replace("C4", ".")
                .replace("C5", "/");
    }

    // Simula la presión de teclas simultáneas [1]
    public static void pressKeys(String keys, Robot robot) {
        if (keys.isEmpty()) return;
        
        char[] charArray = keys.toCharArray();
        List<Integer> pressedCodes = new ArrayList<>();

        // Presionar todas las teclas del grupo
        for (char c : charArray) {
            int keyCode = KeyEvent.getExtendedKeyCodeForChar(c);
            if (keyCode != KeyEvent.VK_UNDEFINED) {
                robot.keyPress(keyCode);
                pressedCodes.add(keyCode);
            }
        }
        
        // Pequeña espera para asegurar el registro
        robot.delay(10); 

        // Soltar todas las teclas
        for (int keyCode : pressedCodes) {
            robot.keyRelease(keyCode);
        }
    }

    // Proceso principal de ejecución [2]
    public static void startProcess(int speed) {
        try {
            // Lectura del archivo original [2]
            StringBuilder content = new StringBuilder();
            BufferedReader reader = new BufferedReader(new FileReader(filename));
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append(" ");
            }
            reader.close();

            // Modificar la sentencia según el mapeo [2]
            String modifiedSentence = replaceSent(content.toString().trim());
            Robot robot = new Robot();
            
            // Espera de 1 segundo antes de empezar [2]
            Thread.sleep(1000); 

            String groupChars = "";
            for (char c : modifiedSentence.toCharArray()) {
                if (c == ' ') {
                    if (!groupChars.isEmpty()) {
                        // Presionar grupo de teclas simultáneamente [2]
                        pressKeys(groupChars, robot);
                        Thread.sleep(speed); 
                        groupChars = "";
                    } else {
                        // Espera por espacio vacío [2]
                        Thread.sleep(300);
                    }
                } else {
                    groupChars += c;
                }
            }
            // Presionar teclas restantes [2]
            if (!groupChars.isEmpty()) {
                pressKeys(groupChars, robot);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        // Configuración de la interfaz gráfica (Swing) [2, 3]
        JFrame frame = new JFrame("Music AutoPlayer PC");
        frame.setSize(300, 150);
        frame.setLayout(new FlowLayout());
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JButton chooseBtn = new JButton("Choose File");
        JButton slowBtn = new JButton("Slow");
        JButton mediumBtn = new JButton("Medium");
        JButton fastBtn = new JButton("Fast");

        // Ocultar botones de velocidad hasta elegir archivo [2]
        slowBtn.setVisible(false);
        mediumBtn.setVisible(false);
        fastBtn.setVisible(false);

        chooseBtn.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            // Intenta abrir en la carpeta "Songs" [2]
            fileChooser.setCurrentDirectory(new File(System.getProperty("user.dir"), "Songs"));
            int result = fileChooser.showOpenDialog(null);
            if (result == JFileChooser.APPROVE_OPTION) {
                filename = fileChooser.getSelectedFile().getAbsolutePath();
                slowBtn.setVisible(true);
                mediumBtn.setVisible(true);
                fastBtn.setVisible(true);
                frame.revalidate();
            }
        });

        // Acciones de los botones de velocidad [3]
        slowBtn.addActionListener(e -> { frame.dispose(); startProcess(SPEED_SLOW); });
        mediumBtn.addActionListener(e -> { frame.dispose(); startProcess(SPEED_MEDIUM); });
        fastBtn.addActionListener(e -> { frame.dispose(); startProcess(SPEED_FAST); });

        frame.add(chooseBtn);
        frame.add(slowBtn);
        frame.add(mediumBtn);
        frame.add(fastBtn);

        frame.setVisible(true);
    }
}
