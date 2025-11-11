import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

public class NavDataConversorGUI extends JFrame {

    private JTextField txtRutaEntrada;
    private JTextField txtRutaSalida;

    private JTextArea areaLog;
    private JProgressBar barra;
    private JButton btnProcesar, btnBorrarOriginales;

    private List<File> archivosXml = new ArrayList<>();

    // Ruta por defecto multiplataforma
    private final Path rutaPorDefecto = Paths.get(
            System.getProperty("user.home"),
            "FlightGear", "Downloads", "fgdata_2024_1", "Scenery", "Airports"
    );

    public NavDataConversorGUI() {

        setTitle("NavData Conversor");
        setSize(750, 500);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));
        panel.setBackground(new Color(245, 245, 245));

        // ============================================================
        // PANEL SUPERIOR: Entrada + Salida
        // ============================================================
        JPanel arriba = new JPanel(new GridLayout(2, 1, 8, 8));
        arriba.setBackground(new Color(245, 245, 245));

        // ---- Entrada ----
        JPanel lineaEntrada = new JPanel(new BorderLayout(5, 5));
        lineaEntrada.setBackground(new Color(245, 245, 245));
        txtRutaEntrada = new JTextField();
        JButton btnSelEntrada = new JButton("Seleccionar carpeta de entrada");
        btnSelEntrada.addActionListener(e -> seleccionarCarpeta(txtRutaEntrada));
        lineaEntrada.add(txtRutaEntrada, BorderLayout.CENTER);
        lineaEntrada.add(btnSelEntrada, BorderLayout.EAST);

        // ---- Salida ----
        JPanel lineaSalida = new JPanel(new BorderLayout(5, 5));
        lineaSalida.setBackground(new Color(245, 245, 245));
        txtRutaSalida = new JTextField(rutaPorDefecto.toString());
        JButton btnSelSalida = new JButton("Seleccionar carpeta de salida");
        btnSelSalida.addActionListener(e -> seleccionarCarpeta(txtRutaSalida));
        lineaSalida.add(txtRutaSalida, BorderLayout.CENTER);
        lineaSalida.add(btnSelSalida, BorderLayout.EAST);

        arriba.add(lineaEntrada);
        arriba.add(lineaSalida);

        // ============================================================
        // PANEL CENTRAL: LOG
        // ============================================================
        areaLog = new JTextArea();
        areaLog.setEditable(false);
        areaLog.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        JScrollPane scroll = new JScrollPane(areaLog);

        // ============================================================
        // PANEL INFERIOR: PROGRESO + BOTONES
        // ============================================================
        JPanel abajo = new JPanel(new BorderLayout(8, 8));
        abajo.setBackground(new Color(245, 245, 245));

        barra = new JProgressBar();
        barra.setStringPainted(true);

        btnProcesar = new JButton("Procesar");
        btnProcesar.setBackground(new Color(60, 179, 113));
        btnProcesar.setForeground(Color.WHITE);
        btnProcesar.addActionListener(e -> iniciarProceso());

        btnBorrarOriginales = new JButton("Borrar originales");
        btnBorrarOriginales.setBackground(new Color(200, 50, 50));
        btnBorrarOriginales.setForeground(Color.WHITE);
        btnBorrarOriginales.setEnabled(false);
        btnBorrarOriginales.addActionListener(e -> borrarOriginales());

        abajo.add(barra, BorderLayout.CENTER);
        abajo.add(btnProcesar, BorderLayout.EAST);
        abajo.add(btnBorrarOriginales, BorderLayout.WEST);

        // ============================================================
        // ENSAMBLADO FINAL
        // ============================================================
        panel.add(arriba, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);
        panel.add(abajo, BorderLayout.SOUTH);

        add(panel);
    }

    // ============================================================
    // Selección de carpeta
    // ============================================================
    private void seleccionarCarpeta(JTextField campo) {
        JFileChooser fc = new JFileChooser();
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            campo.setText(fc.getSelectedFile().getAbsolutePath());
        }
    }

    // ============================================================
    // Inicio proceso
    // ============================================================
    private void iniciarProceso() {

        String rutaEntrada = txtRutaEntrada.getText().trim();
        String rutaSalida = txtRutaSalida.getText().trim();

        if (rutaEntrada.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Selecciona una carpeta de ENTRADA.");
            return;
        }

        if (rutaSalida.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Selecciona una carpeta de SALIDA.");
            return;
        }

        // Aviso si la salida es la predeterminada y si no existe aún crearla
        if (rutaSalida.equals(rutaPorDefecto.toString())) {
            int r = JOptionPane.showConfirmDialog(
                    this,
                    "La carpeta de salida es la predeterminada.\n" +
                            "Puede que no exista en algunos equipos.\n\n" +
                            "¿Deseas continuar igualmente?",
                    "Advertencia",
                    JOptionPane.YES_NO_OPTION
            );

            if (r != JOptionPane.YES_OPTION) return;
        }

        File carpetaEntrada = new File(rutaEntrada);
        File carpetaSalida = new File(rutaSalida);

        carpetaSalida.mkdirs(); // crear si no existe

        archivosXml.clear();
        btnProcesar.setEnabled(false);
        btnBorrarOriginales.setEnabled(false);
        areaLog.setText("");

        SwingWorker<Void, String> worker = new SwingWorker<>() {

            @Override
            protected Void doInBackground() {

                publish("Buscando archivos XML...");
                buscarXML(carpetaEntrada);

                barra.setMaximum(archivosXml.size());

                if (archivosXml.isEmpty()) {
                    publish("No se encontraron XML válidos.");
                    return null;
                }

                publish("Encontrados: " + archivosXml.size());

                int count = 0;

                for (File xml : archivosXml) {
                    procesarArchivo(xml, carpetaSalida);
                    barra.setValue(++count);
                }

                publish("✅ Proceso completado.");

                return null;
            }

            @Override
            protected void process(List<String> chunks) {
                for (String msg : chunks) {
                    areaLog.append(msg + "\n");
                }
            }

            @Override
            protected void done() {
                btnProcesar.setEnabled(true);
                btnBorrarOriginales.setEnabled(true);
            }
        };

        worker.execute();
    }

    // ============================================================
    // Buscar XML de 4 letras
    // ============================================================
    private void buscarXML(File carpeta) {
        File[] archivos = carpeta.listFiles();
        if (archivos == null) return;

        for (File f : archivos) {
            if (f.isDirectory()) {
                buscarXML(f);
            } else if (f.getName().matches("^[A-Za-z0-9]{4}\\.xml$")) {
                archivosXml.add(f);
            }
        }
    }

    // ============================================================
    // Procesar un archivo
    // ============================================================
    private void procesarArchivo(File xml, File carpetaSalidaBase) {

        try {
            String nombre = xml.getName();
            String prefijo = nombre.substring(0, 4);

            // carpetas X / Y / Z
            File sub1 = new File(carpetaSalidaBase, "" + prefijo.charAt(0));
            File sub2 = new File(sub1, "" + prefijo.charAt(1));
            File sub3 = new File(sub2, "" + prefijo.charAt(2));
            sub3.mkdirs();

            File destino = new File(sub3, prefijo + ".procedures.xml");

            Files.copy(xml.toPath(), destino.toPath(), StandardCopyOption.REPLACE_EXISTING);

            areaLog.append("Copiado: " + xml.getName() + " → " + destino.getPath() + "\n");

        } catch (Exception e) {
            areaLog.append("ERROR procesando " + xml.getName() + ": " + e.getMessage() + "\n");
        }
    }

    // ============================================================
    //  Borrar originales
    // ============================================================
    private void borrarOriginales() {
        int r = JOptionPane.showConfirmDialog(
                this,
                "¿Seguro que quieres borrar los originales?\nEsto no se puede deshacer.",
                "Confirmar",
                JOptionPane.YES_NO_OPTION
        );

        if (r != JOptionPane.YES_OPTION) return;

        int count = 0;

        for (File f : archivosXml) {
            if (f.exists() && f.delete()) count++;
        }

        JOptionPane.showMessageDialog(this, "Eliminados: " + count);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new NavDataConversorGUI().setVisible(true));
    }
}
