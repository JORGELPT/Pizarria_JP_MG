package com.example.demo1.Controllers;

import com.example.demo1.Database.Conexion;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.export.JRPdfExporter;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.awt.Desktop;
import java.io.File;
import java.io.InputStream;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

public class CONTROLLER_Reporte1 {

    Conexion conexion = new Conexion();

    @FXML private TextField TXTbuscar;
    @FXML private TableView<String[]> tablaEmpleados;
    @FXML private TableColumn<String[], String> colCedula;
    @FXML private TableColumn<String[], String> colNombre;
    @FXML private TableColumn<String[], String> colTelefono;
    @FXML private TableColumn<String[], String> colCargo;
    @FXML private TableColumn<String[], String> colHorario;
    @FXML private TableColumn<String[], String> colSalario;
    @FXML private TableColumn<String[], String> colDepartamento;
    @FXML private Label lblTotal;
    @FXML private Label lblFechaReporte;

    @FXML
    public void initialize() {
        lblFechaReporte.setText(
            LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
        );

        colCedula.setCellValueFactory(      c -> new SimpleStringProperty(c.getValue()[0]));
        colNombre.setCellValueFactory(       c -> new SimpleStringProperty(c.getValue()[1]));
        colTelefono.setCellValueFactory(     c -> new SimpleStringProperty(c.getValue()[2]));
        colCargo.setCellValueFactory(        c -> new SimpleStringProperty(c.getValue()[3]));
        colHorario.setCellValueFactory(      c -> new SimpleStringProperty(c.getValue()[4]));
        colSalario.setCellValueFactory(      c -> new SimpleStringProperty(c.getValue()[5]));
        colDepartamento.setCellValueFactory( c -> new SimpleStringProperty(c.getValue()[6]));

        cargarEmpleados("");
    }

    // ------------------------------------------------------------------ //
    //  ACCIONES DE BOTONES                                                //
    // ------------------------------------------------------------------ //

    @FXML public void FnBuscar()      { cargarEmpleados(TXTbuscar.getText().trim()); }
    @FXML public void FnLimpiar()     { TXTbuscar.clear(); cargarEmpleados(""); }
    @FXML public void FnCargarTodos() { TXTbuscar.clear(); cargarEmpleados(""); }

    @FXML
    public void FnVerGrafico() {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        String sql = """
            SELECT d.nombre AS nombre_departamento, COUNT(e.id_empleado) AS total
            FROM tbl_empleado e
            LEFT JOIN tbl_cargo c ON e.id_cargo = c.id_cargo
            LEFT JOIN tbl_departamento d ON c.id_departamento = d.id_departamento
            WHERE d.nombre IS NOT NULL
            GROUP BY d.nombre
            ORDER BY total DESC
            """;
        try (Connection con = conexion.establecerConexion();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                dataset.addValue(rs.getInt("total"), "Empleados",
                        rs.getString("nombre_departamento"));
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error al cargar datos del gráfico: " + e.getMessage());
            return;
        }

        JFreeChart chart = ChartFactory.createBarChart(
                "Empleados por Departamento",
                "Departamento", "Cantidad",
                dataset, PlotOrientation.VERTICAL,
                false, true, false
        );

        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Gráfico — Empleados por Departamento");
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            frame.add(new ChartPanel(chart));
            frame.setSize(700, 480);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }

    /**
     * Genera el PDF con JasperReports usando el .jrxml del classpath
     * y abre el archivo automáticamente al terminar.
     */
    @FXML
    public void FnExportarPDF() {
        // 1. Que el usuario elija dónde guardar
        FileChooser fc = new FileChooser();
        fc.setTitle("Guardar Reporte PDF");
        fc.setInitialFileName("Reporte_Empleados.pdf");
        fc.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("PDF (*.pdf)", "*.pdf")
        );
        File destino = fc.showSaveDialog(new Stage());
        if (destino == null) return;   // canceló

        try {
            // 2. Cargar y compilar el .jrxml
            InputStream jrxmlStream = getClass().getResourceAsStream(
                "/com/example/demo1/reportes/Reporte_Empleados.jrxml"
            );
            if (jrxmlStream == null) {
                JOptionPane.showMessageDialog(null,
                    "No se encontró el archivo Reporte_Empleados.jrxml en el classpath.");
                return;
            }
            JasperReport jasperReport = JasperCompileManager.compileReport(jrxmlStream);

            // 3. Parámetros extras (pueden usarse en el JRXML con $P{...})
            Map<String, Object> params = new HashMap<>();
            params.put("REPORT_LOCALE", new java.util.Locale("es", "DO"));

            // 4. Llenar con datos de la BD
            try (Connection con = conexion.establecerConexion()) {
                JasperPrint jasperPrint = JasperFillManager.fillReport(
                    jasperReport, params, con
                );

                // 5. Exportar a PDF
                JRPdfExporter exporter = new JRPdfExporter();
                exporter.setExporterInput(new SimpleExporterInput(jasperPrint));
                exporter.setExporterOutput(
                    new SimpleOutputStreamExporterOutput(destino)
                );
                exporter.exportReport();
            }

            // 6. Abrir el PDF automáticamente
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(destino);
            }

            JOptionPane.showMessageDialog(null,
                "✅ PDF generado correctamente:\n" + destino.getAbsolutePath());

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(null,
                "Error al generar el PDF:\n" + ex.getMessage());
        }
    }

    // ------------------------------------------------------------------ //
    //  CARGA DE DATOS EN LA TABLA JAVAFX                                  //
    // ------------------------------------------------------------------ //

    private void cargarEmpleados(String filtro) {
        ObservableList<String[]> datos = FXCollections.observableArrayList();

        String sql = """
            SELECT p.cedula, p.nombre, p.tel,
                   c.nombre     AS cargo,
                   e.horario,
                   e.salario,
                   d.nombre     AS departamento
            FROM tbl_empleado e
            JOIN tbl_persona       p ON p.id_persona      = e.id_persona
            LEFT JOIN tbl_cargo      c ON c.id_cargo        = e.id_cargo
            LEFT JOIN tbl_departamento d ON d.id_departamento = c.id_departamento
            WHERE (p.cedula LIKE ? OR p.nombre LIKE ?)
            ORDER BY p.nombre
            """;

        try (Connection con = conexion.establecerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {

            String like = "%" + filtro + "%";
            ps.setString(1, like);
            ps.setString(2, like);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    datos.add(new String[]{
                        nvl(rs.getString("cedula")),
                        nvl(rs.getString("nombre")),
                        nvl(rs.getString("tel")),
                        nvl(rs.getString("cargo")),
                        nvl(rs.getString("horario")),
                        rs.getObject("salario") != null
                            ? String.format("%.2f", rs.getDouble("salario")) : "0.00",
                        nvl(rs.getString("departamento"))
                    });
                }
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(null,
                "Error al cargar empleados: " + e.getMessage());
        }

        tablaEmpleados.setItems(datos);
        lblTotal.setText(String.valueOf(datos.size()));
    }

    private String nvl(String v) { return v != null ? v : ""; }
}
