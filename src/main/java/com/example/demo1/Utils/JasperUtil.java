package com.example.demo1.Utils;

import com.example.demo1.Database.Conexion;
import net.sf.jasperreports.engine.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import javax.swing.JOptionPane;
import java.awt.Desktop;
import java.io.File;
import java.io.InputStream;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

/**
 * Utilería central para generar reportes PDF con JasperReports.
 *
 * Uso desde cualquier controller:
 *   JasperUtil.exportarPDF(
 *       "/com/example/demo1/reportes/Reporte_Clientes.jrxml",
 *       "Reporte_Clientes.pdf",
 *       new HashMap<>()   // o agrega parámetros $P{} aquí
 *   );
 */
public class JasperUtil {

    /**
     * Abre un FileChooser, compila el .jrxml, llena con SQL Server y exporta PDF.
     *
     * @param rutaJrxml       Ruta dentro del classpath (empieza con /).
     * @param nombreArchivoDefault  Nombre sugerido para guardar.
     * @param parametros      Mapa de parámetros $P{} del reporte. Puede ser vacío.
     */
    public static void exportarPDF(String rutaJrxml,
                                   String nombreArchivoDefault,
                                   Map<String, Object> parametros) {

        // 1. El usuario elige dónde guardar el PDF
        FileChooser fc = new FileChooser();
        fc.setTitle("Guardar Reporte PDF");
        fc.setInitialFileName(nombreArchivoDefault);
        fc.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("PDF (*.pdf)", "*.pdf"));
        File destino = fc.showSaveDialog(new Stage());
        if (destino == null) return;

        Connection con = null;
        try {
            // 2. Cargar el .jrxml desde el classpath (resources)
            InputStream stream = JasperUtil.class.getResourceAsStream(rutaJrxml);
            if (stream == null) {
                JOptionPane.showMessageDialog(null,
                        "No se encontró el reporte:\n" + rutaJrxml);
                return;
            }

            // 3. Compilar en memoria (.jrxml → JasperReport)
            JasperReport jasperReport = JasperCompileManager.compileReport(stream);

            // 4. Agregar locale español por defecto
            if (!parametros.containsKey("REPORT_LOCALE")) {
                parametros.put("REPORT_LOCALE", new java.util.Locale("es", "DO"));
            }

            // 5. Obtener conexión a SQL Server y llenar el reporte
            con = Conexion.establecerConexion();
            JasperPrint jasperPrint = JasperFillManager.fillReport(
                    jasperReport, parametros, con);

            // 6. Exportar a PDF
            JasperExportManager.exportReportToPdfFile(
                    jasperPrint, destino.getAbsolutePath());

            // 7. Abrir el PDF automáticamente con el visor del sistema
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(destino);
            }

            JOptionPane.showMessageDialog(null,
                    "✅ PDF generado correctamente:\n" + destino.getAbsolutePath());

        } catch (Exception ex) { //error al generar reporte PDF - pantalla Reportes
            ex.printStackTrace();
            JOptionPane.showMessageDialog(null,
                    "Error al generar el reporte PDF:\n" + ex.getMessage());
        } finally {
            try { if (con != null) con.close(); } catch (Exception ignored) {}
        }
    }

    /** Sobrecarga sin parámetros (cuando el reporte no necesita $P{}). */
    public static void exportarPDF(String rutaJrxml, String nombreArchivoDefault) {
        exportarPDF(rutaJrxml, nombreArchivoDefault, new HashMap<>());
    }
}
