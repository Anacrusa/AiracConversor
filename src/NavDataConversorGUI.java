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
    private JButton btnProcesar;

    public NavDataConversorGUI() {
        setTitle("Navdata Conversor");
        setSize(600, 400);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // --- Parte superior: selector de carpeta ---
        JPanel arriba = new JPanel(new BorderLayout(5, 5));
        txtRuta = new JTextField();
        JButton btnSeleccionar = new JButton("Seleccionar carpeta");

        btnSeleccionar.addActionListener(e -> seleccionarCarpeta());

        arriba.add(txtRuta, BorderLayout.CENTER);
        arriba.add(btnSeleccionar, BorderLayout.EAST);

        // --- Centro: log ---
        areaLog = new JTextArea();
        areaLog.setEditable(false);
        JScrollPane scroll = new JScrollPane(areaLog);

        // --- Abajo: progreso + botón ---
        JPanel abajo = new JPanel(new BorderLayout(5, 5));
        barra = new JProgressBar();
        barra.setStringPainted(true);

        btnProcesar = new JButton("Procesar");
        btnProcesar.addActionListener(e -> iniciarProceso());

        abajo.add(barra, BorderLayout.CENTER);
        abajo.add(btnProcesar, BorderLayout.EAST);

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
                }
            }

            @Override
            protected void done() {
                btnProcesar.setEnabled(true);
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

            if (nombre.toLowerCase().endsWith(".xml")
                    && nombre.matches("^[A-Za-z0-9]{4}\\.xml$")) {

                archivosXml.add(f);
            }
        }
    }
}

    private void procesarArchivo(File archivo) {

        try {
            String nombre = archivo.getName();
            String prefijo = nombre.substring(0, 4);

            if (!prefijo.matches("[A-Za-z0-9]{4}")) {
                areaLog.append("Saltado (prefijo inválido): " + nombre + "\n");
                return;
            }

            // Rutas ABC → A/B/C
            char A = prefijo.charAt(0);
            char B = prefijo.charAt(1);
            char C = prefijo.charAt(2);

            File carpetaProcedures = new File(archivo.getParentFile().getParentFile(), "procedures");
            File carpetaA = new File(carpetaProcedures, String.valueOf(A));
            File carpetaB = new File(carpetaA, String.valueOf(B));
            File carpetaC = new File(carpetaB, String.valueOf(C));

            carpetaC.mkdirs(); // crea toda la cadena A/B/C

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
