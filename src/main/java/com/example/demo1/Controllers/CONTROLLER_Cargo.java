package com.example.demo1.Controllers;

import com.example.demo1.Database.Conexion;
import com.example.demo1.Utils.JasperUtil;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javax.swing.JOptionPane;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;

public class CONTROLLER_Cargo {

    Conexion conexion = new Conexion();

    @FXML private TextField TXTnombre;
    @FXML private TextField TXTDescripcion;
    @FXML private ComboBox<String> cmbDepartamento;

    @FXML private TableView<CargoRow> tablaCargos;
    @FXML private TableColumn<CargoRow, String> colNombre;
    @FXML private TableColumn<CargoRow, String> colDescripcion;
    @FXML private TableColumn<CargoRow, String> colDepartamento;
    @FXML private TableColumn<CargoRow, String> colEstado;

    // Relación Nombre de departamento -> id
    private Map<String, Integer> mapaDepartamentos = new HashMap<>();

    @FXML
    public void initialize() {
        colNombre.setCellValueFactory(c -> c.getValue().nombre);
        colDescripcion.setCellValueFactory(c -> c.getValue().descripcion);
        colDepartamento.setCellValueFactory(c -> c.getValue().departamento);
        colEstado.setCellValueFactory(c -> c.getValue().estado);

        tablaCargos.getSelectionModel().selectedItemProperty().addListener(
                (obs, old, sel) -> {
                    if (sel == null) return;
                    TXTnombre.setText(sel.nombre.get());
                    TXTDescripcion.setText(sel.descripcion.get());
                    cmbDepartamento.setValue(sel.departamento.get());
                });
        cargarDepartamentos();
        cargarTabla();
    }
    //                  CARGA INICIAL DE COMBOS
    private void cargarDepartamentos() {
        ObservableList<String> nombresDepto = FXCollections.observableArrayList();
        String sql = "SELECT id_departamento, nombre FROM tbl_departamento ORDER BY nombre";

        try (Connection con = conexion.establecerConexion();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            mapaDepartamentos.clear();
            while (rs.next()) {
                int    id     = rs.getInt("id_departamento");
                String nombre = rs.getString("nombre");
                nombresDepto.add(nombre);
                mapaDepartamentos.put(nombre, id);
            }
            cmbDepartamento.setItems(nombresDepto);

        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error al cargar departamentos: " + e.getMessage());
        }
    }

    //                        GUARDAR
    @FXML
    public void FnGuardar() {
        String nombre            = TXTnombre.getText().trim();
        String descripcion       = TXTDescripcion.getText().trim();
        String deptoSeleccionado = cmbDepartamento.getValue();

        if (nombre.isEmpty() || descripcion.isEmpty() || deptoSeleccionado == null) {
            JOptionPane.showMessageDialog(null, "Todos los campos y el departamento son obligatorios.");
            return;
        }

        int idDepartamento = mapaDepartamentos.get(deptoSeleccionado);
        String sql = "INSERT INTO tbl_cargo (nombre, descripcion, id_departamento) VALUES (?, ?, ?)";

        try (Connection con = conexion.establecerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, nombre);
            ps.setString(2, descripcion);
            ps.setInt(3, idDepartamento);
            ps.executeUpdate();
            JOptionPane.showMessageDialog(null, "Cargo guardado correctamente.");
            limpiar();
            cargarTabla();

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error al guardar: " + e.getMessage());
        }
    }
    //                         BUSCAR
    @FXML
    public void FnBuscar() {
        String nombre = TXTnombre.getText().trim();
        if (nombre.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Ingrese un nombre para buscar.");
            return;
        }

        String sql = "SELECT c.nombre, c.descripcion, d.nombre as nombre_depto " +
                "FROM tbl_cargo c " +
                "LEFT JOIN tbl_departamento d ON c.id_departamento = d.id_departamento " +
                "WHERE c.nombre = ?";

        try (Connection con = conexion.establecerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, nombre);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    TXTnombre.setText(rs.getString("nombre"));
                    TXTDescripcion.setText(rs.getString("descripcion"));
                    cmbDepartamento.setValue(rs.getString("nombre_depto"));
                    JOptionPane.showMessageDialog(null, "Registro encontrado.");
                } else {
                    JOptionPane.showMessageDialog(null, "No se encontró ningún cargo con ese nombre.");
                }
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error al buscar: " + e.getMessage());
        }
    }

    //                         EDITAR
    @FXML
    public void FnEditar() {
        String nombre      = TXTnombre.getText().trim();
        String descripcion = TXTDescripcion.getText().trim();
        String depto       = cmbDepartamento.getValue();

        if (nombre.isEmpty()) {
            JOptionPane.showMessageDialog(null, "El nombre es obligatorio para editar.");
            return;
        }
        if (depto == null) {
            JOptionPane.showMessageDialog(null, "Seleccione un departamento.");
            return;
        }

        int idDepartamento = mapaDepartamentos.get(depto);
        String sql = "UPDATE tbl_cargo SET descripcion = ?, id_departamento = ? WHERE nombre = ?";

        try (Connection con = conexion.establecerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, descripcion);
            ps.setInt(2, idDepartamento);
            ps.setString(3, nombre);
            if (ps.executeUpdate() > 0) {
                JOptionPane.showMessageDialog(null, "Cargo actualizado correctamente.");
                cargarTabla();
            } else {
                JOptionPane.showMessageDialog(null, "No se encontró el cargo.");
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error al editar: " + e.getMessage());
        }
    }

    //                         ELIMINAR
    @FXML
    public void FnEliminar() {
        if (!com.example.demo1.Utils.Permisos_Util.verificarEliminar()) return;
        String nombre = TXTnombre.getText().trim();
        if (nombre.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Ingrese el nombre del cargo a eliminar.");
            return;
        }

        int c = JOptionPane.showConfirmDialog(null,
                "¿Seguro que desea eliminar el cargo '" + nombre + "'?",
                "Confirmar eliminación", JOptionPane.YES_NO_OPTION);
        if (c != JOptionPane.YES_OPTION) return;

        try (Connection con = conexion.establecerConexion();
             PreparedStatement ps = con.prepareStatement("DELETE FROM tbl_cargo WHERE nombre = ?")) {

            ps.setString(1, nombre);
            if (ps.executeUpdate() > 0) {
                JOptionPane.showMessageDialog(null, "Cargo eliminado correctamente.");
                limpiar();
                cargarTabla();
            } else {
                JOptionPane.showMessageDialog(null, "No se encontró el cargo.");
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error al eliminar: " + e.getMessage());
        }
    }

    @FXML
    public void FnInhabilitar() {
        String nombre = TXTnombre.getText().trim();
        if (nombre.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Ingrese el nombre del cargo a inhabilitar.");
            return;
        }
        int confirmar = JOptionPane.showConfirmDialog(null,
                "¿Inhabilitar '" + nombre + "'? No se eliminará, solo quedará inactivo.",
                "Confirmar", JOptionPane.YES_NO_OPTION);
        if (confirmar != JOptionPane.YES_OPTION) return;
        try (java.sql.Connection con = Conexion.establecerConexion();
             java.sql.PreparedStatement ps = con.prepareStatement(
                     "UPDATE tbl_cargo SET estado = 'Inactivo' WHERE nombre = ?")) {
            ps.setString(1, nombre);
            ps.executeUpdate();
            JOptionPane.showMessageDialog(null, "Cargo inhabilitado correctamente.");
            cargarTabla();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error al inhabilitar: " + e.getMessage());
        }
    }

    @FXML
    public void FnHabilitar() {
        String nombre = TXTnombre.getText().trim();
        if (nombre.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Ingrese el nombre del cargo a habilitar.");
            return;
        }
        try (java.sql.Connection con = Conexion.establecerConexion();
             java.sql.PreparedStatement ps = con.prepareStatement(
                     "UPDATE tbl_cargo SET estado = 'Activo' WHERE nombre = ?")) {
            ps.setString(1, nombre);
            ps.executeUpdate();
            JOptionPane.showMessageDialog(null, "Cargo habilitado correctamente.");
            cargarTabla();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error al habilitar: " + e.getMessage());
        }
    }

    //                         LIMPIAR
    @FXML
    public void FnLimpiar() {
        limpiar();
    }

    public void limpiar() {
        TXTnombre.clear();
        TXTDescripcion.clear();
        cmbDepartamento.getSelectionModel().clearSelection();
    }

    //                      EXPORTAR PDF
    @FXML
    public void FnExportarPDF() {
        if (!com.example.demo1.Utils.Permisos_Util.verificarReporte()) return;
        JasperUtil.exportarPDF(
                "/com/example/demo1/reportes/Reporte_Cargos.jrxml",
                "Reporte_Cargos.pdf"
        );
    }

    //                      CARGAR TABLA
    private void cargarTabla() {
        ObservableList<CargoRow> datos = FXCollections.observableArrayList();
        String sql = "SELECT c.nombre, c.descripcion, d.nombre as nombre_depto, c.estado " +
                "FROM tbl_cargo c " +
                "LEFT JOIN tbl_departamento d ON c.id_departamento = d.id_departamento " +
                "ORDER BY c.nombre";

        try (Connection con = conexion.establecerConexion();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                datos.add(new CargoRow(
                        rs.getString("nombre"),
                        rs.getString("descripcion"),
                        rs.getString("nombre_depto"),
                        rs.getString("estado")));
            }
            tablaCargos.setItems(datos);

        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error al cargar cargos: " + e.getMessage());
        }
    }
    //                   CLASE INTERNA DE FILA
    public static class CargoRow {
        final SimpleStringProperty nombre;
        final SimpleStringProperty descripcion;
        final SimpleStringProperty departamento;
        final SimpleStringProperty estado;

        public CargoRow(String n, String d, String dp, String est) {
            this.nombre       = new SimpleStringProperty(n);
            this.descripcion  = new SimpleStringProperty(d);
            this.departamento = new SimpleStringProperty(dp);
            this.estado       = new SimpleStringProperty(est != null ? est : "Activo");
        }

        public String getNombre()       { return nombre.get(); }
        public String getDescripcion()  { return descripcion.get(); }
        public String getDepartamento() { return departamento.get(); }
        public String getEstado()       { return estado.get(); }
    }
}
