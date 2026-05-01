/*
 * Clase principal del proyecto SkyAutoSheetplayer
 */
package SkyMusicPlayer;

import javax.swing.SwingUtilities;

/**
 * Punto de entrada de la aplicación
 * @author botel
 */
public class SkyMusicPlayer {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // Iniciar la GUI en el hilo de eventos de Swing
        SwingUtilities.invokeLater(() -> {
            new SkyMusicGUI().setVisible(true);
        });
    }
}
