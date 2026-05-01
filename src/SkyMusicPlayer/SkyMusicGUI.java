package SkyMusicPlayer;

import java.awt.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * Interfaz gráfica para el proyecto SkyAutoSheetplayer.
 * Diseñado para facilitar la reproducción de música en Sky:cotl.
 */
public class SkyMusicGUI extends JFrame {
    private JButton btnLoad, btnStart, btnStop, btnClear;
    private JComboBox<String> comboSpeed;
    private JComboBox<String> comboLanguage;
    private JLabel lblStatus, lblFileName;
    private JTextArea textAreaNotes;
    private JProgressBar progressBar;
    private String selectedFilePath = "";
    private boolean isPlaying = false;
    private AutoPlayer autoPlayer;
    private Thread playbackThread;

    public SkyMusicGUI() {
        // Configuración de la ventana principal
        setTitle("Sky Auto Sheetplayer - Sky:cotl");
        setSize(600, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(true);
        setLayout(new BorderLayout(10, 10));

        // Panel superior - Estado y archivo
        JPanel topPanel = new JPanel(new BorderLayout(5, 5));
        topPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        lblStatus = new JLabel("Estado: Listo", SwingConstants.LEFT);
        lblStatus.setFont(new Font("Arial", Font.BOLD, 12));
        lblFileName = new JLabel("Archivo: Ninguno cargado");
        lblFileName.setFont(new Font("Arial", Font.PLAIN, 11));
        topPanel.add(lblStatus, BorderLayout.NORTH);
        topPanel.add(lblFileName, BorderLayout.SOUTH);
        add(topPanel, BorderLayout.NORTH);

        // Panel central - Área de notas
        JPanel centerPanel = new JPanel(new BorderLayout(5, 5));
        centerPanel.setBorder(BorderFactory.createTitledBorder("Vista previa de notas"));
        textAreaNotes = new JTextArea(10, 40);
        textAreaNotes.setEditable(false);
        textAreaNotes.setFont(new Font("Courier New", Font.PLAIN, 11));
        textAreaNotes.setText("Carga un archivo .txt para ver las notas aquí...");
        JScrollPane scrollPane = new JScrollPane(textAreaNotes);
        centerPanel.add(scrollPane, BorderLayout.CENTER);
        add(centerPanel, BorderLayout.CENTER);

        // Panel de progreso
        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setVisible(false);
        add(progressBar, BorderLayout.SOUTH);

        // Panel inferior - Controles
        JPanel bottomPanel = new JPanel(new GridLayout(4, 1, 5, 5));
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Fila 1: Cargar archivo
        JPanel filePanel = new JPanel(new BorderLayout(5, 5));
        btnLoad = new JButton("📂 Cargar Canción (.txt)");
        btnLoad.setFont(new Font("Arial", Font.PLAIN, 12));
        filePanel.add(btnLoad, BorderLayout.CENTER);
        bottomPanel.add(filePanel);

        // Fila 2: Velocidad e Idioma
        JPanel speedLanguagePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 0));
        
        // Velocidad
        speedLanguagePanel.add(new JLabel("Velocidad:"));
        String[] speeds = {"Lento", "Medio", "Rápido", "Muy Rápido"};
        comboSpeed = new JComboBox<>(speeds);
        comboSpeed.setSelectedIndex(1);
        speedLanguagePanel.add(comboSpeed);
        
        // Idioma
        speedLanguagePanel.add(new JLabel("Idioma del teclado:"));
        String[] languages = {"Inglés (QWERTY)", "Español (QWERTY)"};
        comboLanguage = new JComboBox<>(languages);
        comboLanguage.setSelectedIndex(0);
        speedLanguagePanel.add(comboLanguage);
        
        bottomPanel.add(speedLanguagePanel);

        // Fila 3: Botones de control
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        btnStart = new JButton("▶ Reproducir");
        btnStop = new JButton("⏹ Detener");
        btnClear = new JButton("🗑 Limpiar");
        btnStart.setEnabled(false);
        btnStop.setEnabled(false);
        btnStart.setFont(new Font("Arial", Font.PLAIN, 11));
        btnStop.setFont(new Font("Arial", Font.PLAIN, 11));
        btnClear.setFont(new Font("Arial", Font.PLAIN, 11));
        controlPanel.add(btnStart);
        controlPanel.add(btnStop);
        controlPanel.add(btnClear);
        bottomPanel.add(controlPanel);

        add(bottomPanel, BorderLayout.SOUTH);

        // Lógica de los botones
        setupActions();
        
        // Inicializar AutoPlayer
        autoPlayer = new AutoPlayer();
    }

    private void setupActions() {
        // Botón Cargar
        btnLoad.addActionListener(e -> loadFile());

        // Botón Reproducir
        btnStart.addActionListener(e -> startPlayback());

        // Botón Detener
        btnStop.addActionListener(e -> stopPlayback());

        // Botón Limpiar
        btnClear.addActionListener(e -> clearNotes());
    }

    private void loadFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setCurrentDirectory(new File("."));
        fileChooser.setFileFilter(new FileNameExtensionFilter("Archivos de texto (.txt)", "txt"));
        
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            selectedFilePath = selectedFile.getAbsolutePath();
            lblFileName.setText("Archivo: " + selectedFile.getName());
            lblStatus.setText("Estado: Archivo cargado correctamente");
            btnStart.setEnabled(true);
            loadNotesPreview(selectedFile);
        }
    }

    private void loadNotesPreview(File file) {
        StringBuilder notes = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            int lineCount = 0;
            while ((line = reader.readLine()) != null && lineCount < 50) {
                notes.append(line).append("\n");
                lineCount++;
            }
            if (lineCount == 50) {
                notes.append("\n[... más contenido ...]");
            }
            textAreaNotes.setText(notes.toString());
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, 
                "Error al leer el archivo: " + ex.getMessage(), 
                "Error", 
                JOptionPane.ERROR_MESSAGE);
            textAreaNotes.setText("Error al cargar el archivo.");
        }
    }

    private void startPlayback() {
        if (selectedFilePath.isEmpty()) {
            JOptionPane.showMessageDialog(this, 
                "Por favor, carga un archivo primero.", 
                "Advertencia", 
                JOptionPane.WARNING_MESSAGE);
            return;
        }

        isPlaying = true;
        btnStart.setEnabled(false);
        btnStop.setEnabled(true);
        btnLoad.setEnabled(false);
        comboSpeed.setEnabled(false);
        comboLanguage.setEnabled(false);
        
        lblStatus.setText("Estado: Reproduciendo...");
        progressBar.setVisible(true);
        progressBar.setValue(0);

        // Obtener velocidad y idioma seleccionados
        int delayMs = getSelectedSpeed();
        boolean isSpanish = comboLanguage.getSelectedIndex() == 1;

        // Iniciar reproducción en un hilo separado
        playbackThread = new Thread(() -> {
            try {
                autoPlayer.playFromFile(selectedFilePath, delayMs, isSpanish, (progress) -> {
                    SwingUtilities.invokeLater(() -> {
                        progressBar.setValue(progress);
                    });
                });
            } catch (IOException ex) {
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(SkyMusicGUI.this,
                        "Error durante la reproducción: " + ex.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
                    resetPlayback();
                });
            }
            SwingUtilities.invokeLater(this::resetPlayback);
        });
        playbackThread.start();
    }

    private void stopPlayback() {
        isPlaying = false;
        autoPlayer.stop();
        if (playbackThread != null && playbackThread.isAlive()) {
            playbackThread.interrupt();
        }
        resetPlayback();
        lblStatus.setText("Estado: Detenido por el usuario");
    }

    private void resetPlayback() {
        isPlaying = false;
        btnStart.setEnabled(true);
        btnStop.setEnabled(false);
        btnLoad.setEnabled(true);
        comboSpeed.setEnabled(true);
        comboLanguage.setEnabled(true);
        progressBar.setVisible(false);
        if (!lblStatus.getText().contains("Detenido")) {
            lblStatus.setText("Estado: Reproducción completada");
        }
    }

    private void clearNotes() {
        textAreaNotes.setText("");
        lblFileName.setText("Archivo: Ninguno cargado");
        lblStatus.setText("Estado: Listo");
        selectedFilePath = "";
        btnStart.setEnabled(false);
        progressBar.setValue(0);
        progressBar.setVisible(false);
    }

    private int getSelectedSpeed() {
        int selectedIndex = comboSpeed.getSelectedIndex();
        return switch (selectedIndex) {
            case 0 -> 1500;  // Lento - 1.5s
            case 1 -> 1000;  // Medio - 1.0s
            case 2 -> 500;   // Rápido - 0.6s
            case 3 -> 200;   // Muy Rápido - 0.2s
            default -> 500;
        };
    }

    public static void main(String[] args) {
        // Ejecutar la interfaz en el hilo de eventos de Swing
        SwingUtilities.invokeLater(() -> {
            new SkyMusicGUI().setVisible(true);
        });
    }
}