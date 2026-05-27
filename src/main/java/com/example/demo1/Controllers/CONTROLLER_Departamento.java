package com.example.demo1.Controllers;

import com.example.demo1.Database.Conexion;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javax.swing.JOptionPane;
import java.sql.*;

public class CONTROLLER_Departamento {

    Conexion conexion = new Conexion();

    @FXML private TextField TXTnombre;
    @FXML private TextField TXTDescripcion;

    @FXML private TableView<DepartamentoRow> tablaDepartamentos;
    @FXML private TableColumn<DepartamentoRow, String> colNombre;
    @FXML private TableColumn<DepartamentoRow, String> colDescripcion;
    @FXML private TableColumn<DepartamentoRow, String> colEstado;

    @FXML
    public void initialize() {
        colNombre.setCellValueFactory(c -> c.getValue().nombre);
        colDescripcion.setCellValueFactory(c -> c.getValue().descripcion);
        colEstado.setCellValueFactory(c -> c.getValue().estado);

        tablaDepartamentos.getSelectionModel().selectedItemProperty().addListener(
                (obs, old, sel) -> {
                    if (sel == null) return;
                    TXTnombre.setText(sel.nombre.get());
                    TXTDescripcion.setText(sel.descripcion.get());
                });
        cargarTabla();
    }

    @FXML
    public void FnGuardar(ActionEvent actionEvent) {
        String nombre      = TXTnombre.getText().trim();
        String descripcion = TXTDescripcion.getText().trim();

        if (nombre.isEmpty()) {
            JOptionPane.showMessageDialog(null, "El nombre del departamento es obligatorio.");
            return;
        }
        if (descripcion.isEmpty()) {
            JOptionPane.showMessageDialog(null, "La descripción es obligatoria.");
            return;
        }

        String sql = "INSERT INTO tbl_departamento (nombre, descripcion) VALUES (?, ?)";

        try (Connection connection = conexion.establecerConexion();
             PreparedStatement pstmt = connection.prepareStatement(sql)) {

            pstmt.setString(1, nombre);
            pstmt.setString(2, descripcion);
            pstmt.executeUpdate();

            JOptionPane.showMessageDialog(null, "Departamento guardado correctamente.");
            limpiar();
            cargarTabla();

        } catch (SQLException e) { //error al guardar departamento - pantalla Agregar Departamento
            JOptionPane.showMessageDialog(null, "Error de SQL: " + e.getMessage());
        }
    }

    @FXML
    public void FnBuscar(ActionEvent actionEvent) {
        String nombre = TXTnombre.getText().trim();

        if (nombre.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Ingrese un nombre para buscar.");
            return;
        }

        String sql = "SELECT nombre, descripcion FROM tbl_departamento WHERE nombre = ?";

        try (Connection connection = conexion.establecerConexion();
             PreparedStatement pstmt = connection.prepareStatement(sql)) {

            pstmt.setString(1, nombre);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                TXTnombre.setText(rs.getString("nombre"));
                TXTDescripcion.setText(rs.getString("descripcion"));
                JOptionPane.showMessageDialog(null, "Departamento encontrado.");
            } else {
                JOptionPane.showMessageDialog(null, "No se encontró ningún departamento con ese nombre.");
            }

        } catch (SQLException e) { //error al buscar departamento - pantalla Agregar Departamento
            JOptionPane.showMessageDialog(null, "Error de búsqueda: " + e.getMessage());
        }
    }

    @FXML
    public void FnEditar(ActionEvent actionEvent) {
        String nombre      = TXTnombre.getText().trim();
        String descripcion = TXTDescripcion.getText().trim();

        if (nombre.isEmpty()) {
            JOptionPane.showMessageDialog(null, "El nombre es obligatorio para editar.");
            return;
        }

        String sql = "UPDATE tbl_departamento SET descripcion = ? WHERE nombre = ?";

        try (Connection con = conexion.establecerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, descripcion);
            ps.setString(2, nombre);

            if (ps.executeUpdate() > 0) {
                JOptionPane.showMessageDialog(null, "Departamento actualizado correctamente.");
                cargarTabla();
            } else {
                JOptionPane.showMessageDialog(null, "No se encontró el departamento.");
            }

        } catch (Exception e) { //error al editar departamento - pantalla Agregar Departamento
            JOptionPane.showMessageDialog(null, "Error al editar: " + e.getMessage());
        }
    }

    @FXML
    public void FnEliminar(ActionEvent actionEvent) {
        if (!com.example.demo1.Utils.Permisos_Util.verificarEliminar()) return;
        String nombre = TXTnombre.getText().trim();

        if (nombre.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Ingrese el nombre del departamento a eliminar.");
            return;
        }

        int c = JOptionPane.showConfirmDialog(null,
                "¿Eliminar el departamento '" + nombre + "'?",
                "Confirmar eliminación", JOptionPane.YES_NO_OPTION);
        if (c != JOptionPane.YES_OPTION) return;

        try (Connection con = conexion.establecerConexion();
             PreparedStatement ps = con.prepareStatement(
                     "DELETE FROM tbl_departamento WHERE nombre = ?")) {

            ps.setString(1, nombre);
            if (ps.executeUpdate() > 0) {
                JOptionPane.showMessageDialog(null, "Departamento eliminado correctamente.");
                limpiar();
                cargarTabla();
            } else {
                JOptionPane.showMessageDialog(null, "No se encontró el departamento.");
            }

        } catch (Exception e) { //error al eliminar departamento - pantalla Agregar Departamento
            JOptionPane.showMessageDialog(null, "Error al eliminar: " + e.getMessage());
        }
    }

    @FXML
    public void FnInhabilitar() {
        String nombre = TXTnombre.getText().trim();
        if (nombre.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Ingrese el nombre del departamento a inhabilitar.");
            return;
        }
        int confirmar = JOptionPane.showConfirmDialog(null,
                "¿Inhabilitar '" + nombre + "'? No se eliminará, solo quedará inactivo.",
                "Confirmar", JOptionPane.YES_NO_OPTION);
        if (confirmar != JOptionPane.YES_OPTION) return;
        try (java.sql.Connection con = Conexion.establecerConexion();
             java.sql.PreparedStatement ps = con.prepareStatement(
                     "UPDATE tbl_departamento SET estado = 'Inactivo' WHERE nombre = ?")) {
            ps.setString(1, nombre);
            ps.executeUpdate();
            JOptionPane.showMessageDialog(null, "Departamento inhabilitado correctamente.");
            cargarTabla();
        } catch (Exception e) { //error al inhabilitar departamento - pantalla Agregar Departamento
            JOptionPane.showMessageDialog(null, "Error al inhabilitar: " + e.getMessage());
        }
    }

    @FXML
    public void FnHabilitar() {
        String nombre = TXTnombre.getText().trim();
        if (nombre.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Ingrese el nombre del departamento a habilitar.");
            return;
        }
        try (java.sql.Connection con = Conexion.establecerConexion();
             java.sql.PreparedStatement ps = con.prepareStatement(
                     "UPDATE tbl_departamento SET estado = 'Activo' WHERE nombre = ?")) {
            ps.setString(1, nombre);
            ps.executeUpdate();
            JOptionPane.showMessageDialog(null, "Departamento habilitado correctamente.");
            cargarTabla();
        } catch (Exception e) { //error al habilitar departamento - pantalla Agregar Departamento
            JOptionPane.showMessageDialog(null, "Error al habilitar: " + e.getMessage());
        }
    }

    @FXML
    public void FnLimpiar(ActionEvent actionEvent) {
        limpiar();
    }

    public void limpiar() {
        TXTnombre.clear();
        TXTDescripcion.clear();
    }

    private void cargarTabla() {
        ObservableList<DepartamentoRow> datos = FXCollections.observableArrayList();
        String sql = "SELECT nombre, descripcion, estado FROM tbl_departamento ORDER BY nombre";

        try (Connection con = conexion.establecerConexion();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                datos.add(new DepartamentoRow(
                        rs.getString("nombre"),
                        rs.getString("descripcion"),
                        rs.getString("estado")));
            }
            tablaDepartamentos.setItems(datos);

        } catch (Exception e) { //error al cargar tabla de departamentos - pantalla Agregar Departamento
            JOptionPane.showMessageDialog(null, "Error al cargar departamentos: " + e.getMessage());
        }
    }

    public static class DepartamentoRow {
        final SimpleStringProperty nombre, descripcion, estado;

        public DepartamentoRow(String n, String d, String est) {
            nombre      = new SimpleStringProperty(n);
            descripcion = new SimpleStringProperty(d);
            estado      = new SimpleStringProperty(est != null ? est : "Activo");
        }

        public String getNombre()      { return nombre.get(); }
        public String getDescripcion() { return descripcion.get(); }
        public String getEstado()      { return estado.get(); }
    }
}
