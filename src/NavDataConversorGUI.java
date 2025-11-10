import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class NavDataConversorGUI extends JFrame {

    private List<File> archivosXml = new ArrayList<>();
    private JTextField txtRuta;
    private JTextArea areaLog;
    private JProgressBar barra;
    private JButton btnProcesar, btnBorrarOriginales;
    private JPanel abajo;

    public NavDataConversorGUI() {
        setTitle("NavData Conversor");
        setSize(650, 450);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // --- Panel principal con padding ---
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setBorder(new EmptyBorder(12, 12, 12, 12));
        panel.setBackground(new Color(245, 245, 245));

        // --- Parte superior: selector de carpeta ---
        JPanel arriba = new JPanel(new BorderLayout(6, 6));
        arriba.setBackground(new Color(245, 245, 245));

        txtRuta = new JTextField();
        txtRuta.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200), 1));
        JButton btnSeleccionar = new JButton("Seleccionar carpeta");
        btnSeleccionar.setBackground(new Color(70, 130, 180));
        btnSeleccionar.setForeground(Color.WHITE);
        btnSeleccionar.setFocusPainted(false);
        btnSeleccionar.addActionListener(e -> seleccionarCarpeta());

        arriba.add(txtRuta, BorderLayout.CENTER);
        arriba.add(btnSeleccionar, BorderLayout.EAST);

        // --- Centro: log con scroll y fuente monoespaciada ---
        areaLog = new JTextArea();
        areaLog.setEditable(false);
        areaLog.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        areaLog.setBackground(new Color(250, 250, 250));
        areaLog.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200), 1));
        JScrollPane scroll = new JScrollPane(areaLog);

        // --- Abajo: barra de progreso + botones ---
        abajo = new JPanel(new BorderLayout(8, 8));
        abajo.setBackground(new Color(245, 245, 245));
        barra = new JProgressBar();
        barra.setStringPainted(true);
        barra.setForeground(new Color(70, 130, 180));
        barra.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200), 1));

        // Botón procesar
        btnProcesar = new JButton("Procesar");
        btnProcesar.setBackground(new Color(60, 179, 113));
        btnProcesar.setForeground(Color.WHITE);
        btnProcesar.setFocusPainted(false);
        btnProcesar.addActionListener(e -> iniciarProceso());

        // Botón borrar originales
        btnBorrarOriginales = new JButton("Borrar originales");
        btnBorrarOriginales.setBackground(new Color(220, 20, 60));
        btnBorrarOriginales.setForeground(Color.WHITE);
        btnBorrarOriginales.setFocusPainted(false);
        btnBorrarOriginales.setEnabled(false);
        btnBorrarOriginales.addActionListener(e -> borrarOriginales());

        abajo.add(barra, BorderLayout.CENTER);
        abajo.add(btnProcesar, BorderLayout.EAST);
        abajo.add(btnBorrarOriginales, BorderLayout.WEST);

        // --- Montaje final ---
        panel.add(arriba, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);
        panel.add(abajo, BorderLayout.SOUTH);

        add(panel);
    }

    private void seleccionarCarpeta() {
        JFileChooser fc = new JFileChooser();
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            txtRuta.setText(fc.getSelectedFile().getAbsolutePath());
        }
    }

    private void iniciarProceso() {

        String ruta = txtRuta.getText().trim();

        if (ruta.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Selecciona una carpeta primero.");
            return;
        }

        File carpeta = new File(ruta);

        if (!carpeta.exists() || !carpeta.isDirectory()) {
            JOptionPane.showMessageDialog(this, "La ruta no es válida.");
            return;
        }

        btnProcesar.setEnabled(false);
        btnBorrarOriginales.setEnabled(false);
        archivosXml.clear();

        SwingWorker<Void, String> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                publish("Buscando archivos .xml...");
                buscarXML(carpeta);

                int total = archivosXml.size();
                barra.setMaximum(total);

                if (total == 0) {
                    publish("No se encontraron archivos XML.");
                    return null;
                }

                publish("Total encontrados: " + total);

                int contador = 0;
                for (File archivo : archivosXml) {
                    procesarArchivo(archivo);
                    contador++;
                    barra.setValue(contador);
                }

                publish("Proceso completado.");
                return null;
            }

            @Override
            protected void process(List<String> chunks) {
                for (String msg : chunks) {
                    areaLog.append(msg + "\n");
                    areaLog.setCaretPosition(areaLog.getDocument().getLength());
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

    private void buscarXML(File carpeta) {
        File[] archivos = carpeta.listFiles();
        if (archivos == null) return;

        for (File f : archivos) {
            if (f.isDirectory()) {
                buscarXML(f);
            } else {
                String nombre = f.getName();
                if (nombre.toLowerCase().endsWith(".xml") && nombre.matches("^[A-Za-z0-9]{4}\\.xml$")) {
                    archivosXml.add(f);
                }
            }
        }
    }

    private void borrarOriginales() {
        if (archivosXml.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No hay archivos para borrar.");
            return;
        }

        int resp = JOptionPane.showConfirmDialog(
                this,
                "¿Seguro que quieres borrar los archivos originales?\nEsta acción no se puede deshacer.",
                "Confirmar borrado",
                JOptionPane.YES_NO_OPTION
        );

        if (resp != JOptionPane.YES_OPTION) return;

        int contador = 0;
        for (File f : archivosXml) {
            if (f.exists() && f.delete()) {
                contador++;
            } else {
                areaLog.append("No se pudo borrar: " + f.getName() + "\n");
            }
        }

        areaLog.append("Archivos originales eliminados: " + contador + "\n");
        JOptionPane.showMessageDialog(this, "Borrado completado.");
    }

    private void procesarArchivo(File archivo) {
        try {
            String nombre = archivo.getName();
            String prefijo = nombre.substring(0, 4);

            if (!prefijo.matches("[A-Za-z0-9]{4}")) {
                areaLog.append("Saltado (prefijo inválido): " + nombre + "\n");
                return;
            }

            char A = prefijo.charAt(0);
            char B = prefijo.charAt(1);
            char C = prefijo.charAt(2);

            File carpetaProcedures = new File(archivo.getParentFile().getParentFile(), "procedures");
            File carpetaA = new File(carpetaProcedures, String.valueOf(A));
            File carpetaB = new File(carpetaA, String.valueOf(B));
            File carpetaC = new File(carpetaB, String.valueOf(C));

            carpetaC.mkdirs();

            String nuevoNombre = prefijo + ".procedures.xml";
            File destino = new File(carpetaC, nuevoNombre);

            if (destino.exists()) {
                areaLog.append("Ya existe: " + nuevoNombre + "\n");
                return;
            }

            copiarArchivo(archivo, destino);
            areaLog.append("Copiado: " + nombre + " -> " + destino.getPath() + "\n");

        } catch (Exception e) {
            areaLog.append("ERROR procesando: " + archivo.getName() + " -> " + e.getMessage() + "\n");
        }
    }

    private void copiarArchivo(File origen, File destino) throws IOException {
        try (FileInputStream fis = new FileInputStream(origen);
             FileOutputStream fos = new FileOutputStream(destino)) {
            byte[] buffer = new byte[4096];
            int length;
            while ((length = fis.read(buffer)) > 0) {
                fos.write(buffer, 0, length);
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new NavDataConversorGUI().setVisible(true));
    }
}