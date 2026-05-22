package com.example.demo1.Controllers;

import com.example.demo1.Database.Conexion;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

import javax.swing.JOptionPane;
import java.sql.*;

/**
 * tbl_tecnico: id_tecnico (PK IDENTITY), nombre_tecnico, especialidad_tecnico, tel, cedula
 * Nota: tbl_tecnico NO tiene id_persona. Sus datos personales van directamente en la tabla.
 */
public class CONTROLLER_Tecnico {

    Conexion conexion = new Conexion();

    @FXML private TextField TXTnombre;
    @FXML private TextField TXTtelefono;
    @FXML private TextField TXTEspecialidad;
    @FXML private TextField TXTcedula;

    @FXML private TableView<TecnicoRow> tablaTecnicos;
    @FXML private TableColumn<TecnicoRow, String> colNombre;
    @FXML private TableColumn<TecnicoRow, String> colCedula;
    @FXML private TableColumn<TecnicoRow, String> colEspecialidad;
    @FXML private TableColumn<TecnicoRow, String> colEstado;

    @FXML
    public void initialize() {
        colNombre.setCellValueFactory(c -> c.getValue().nombre);
        colCedula.setCellValueFactory(c -> c.getValue().cedula);
        colEspecialidad.setCellValueFactory(c -> c.getValue().especialidad);
        colEstado.setCellValueFactory(c -> c.getValue().estado);

        tablaTecnicos.getSelectionModel().selectedItemProperty().addListener(
                (obs, old, sel) -> {
                    if (sel == null) return;
                    TXTnombre.setText(sel.nombre.get());
                    TXTcedula.setText(sel.cedula.get());
                    TXTEspecialidad.setText(sel.especialidad.get());
                });
        cargarTabla();
    }

    @FXML
    public void FnGuardar() {
        String nombre       = TXTnombre.getText().trim();
        String telefono     = TXTtelefono.getText().trim();
        String especialidad = TXTEspecialidad.getText().trim();
        String cedula       = TXTcedula.getText().trim();

        if (nombre.isEmpty() || telefono.isEmpty() || especialidad.isEmpty() || cedula.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Todos los campos son obligatorios.");
            return;
        }

        // Insertar directamente en tbl_tecnico (sin pasar por tbl_persona)
        String sql = "INSERT INTO tbl_tecnico (nombre_tecnico, especialidad_tecnico, tel, cedula) " +
                "VALUES (?, ?, ?, ?)";

        try (Connection con = conexion.establecerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, nombre);
            ps.setString(2, especialidad);
            ps.setString(3, telefono);
            ps.setString(4, cedula);
            ps.executeUpdate();
            JOptionPane.showMessageDialog(null, "Técnico guardado correctamente.");
            limpiar();
            cargarTabla();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error: " + e.getMessage());
        }
    }

    @FXML
    public void FnBuscar() {
        String cedula = TXTcedula.getText().trim();
        if (cedula.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Ingrese una cédula para buscar.");
            return;
        }

        String sql = "SELECT nombre_tecnico, tel, cedula, especialidad_tecnico " +
                "FROM tbl_tecnico WHERE cedula = ?";

        try (Connection con = conexion.establecerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, cedula);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                TXTnombre.setText(rs.getString("nombre_tecnico"));
                TXTtelefono.setText(rs.getString("tel"));
                TXTcedula.setText(rs.getString("cedula"));
                TXTEspecialidad.setText(rs.getString("especialidad_tecnico"));
            } else {
                JOptionPane.showMessageDialog(null, "No se encontró el técnico.");
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error: " + e.getMessage());
        }
    }

    @FXML public void FnEditar() {
        String cedula       = TXTcedula.getText().trim();
        String nombre       = TXTnombre.getText().trim();
        String telefono     = TXTtelefono.getText().trim();
        String especialidad = TXTEspecialidad.getText().trim();

        if (cedula.isEmpty() || nombre.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Busque un técnico primero (use FnBuscar por cédula).");
            return;
        }

        String sql = "UPDATE tbl_tecnico SET nombre_tecnico=?, especialidad_tecnico=?, tel=? WHERE cedula=?";
        try (Connection con = conexion.establecerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, nombre);
            ps.setString(2, especialidad);
            ps.setString(3, telefono);
            ps.setString(4, cedula);
            int filas = ps.executeUpdate();
            if (filas > 0) {
                JOptionPane.showMessageDialog(null, "Técnico actualizado correctamente.");
                cargarTabla();
            } else {
                JOptionPane.showMessageDialog(null, "No se encontró el técnico con esa cédula.");
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error al actualizar: " + e.getMessage());
        }
    }

    @FXML
    public void FnEliminar() {
        if (!com.example.demo1.Utils.Permisos_Util.verificarEliminar()) return;
        String cedula = TXTcedula.getText().trim();
        if (cedula.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Ingrese la cédula a eliminar.");
            return;
        }
        int c = JOptionPane.showConfirmDialog(null, "¿Eliminar técnico?",
                "Confirmar", JOptionPane.YES_NO_OPTION);
        if (c != JOptionPane.YES_OPTION) return;

        try (Connection con = conexion.establecerConexion();
             PreparedStatement ps = con.prepareStatement("DELETE FROM tbl_tecnico WHERE cedula = ?")) {
            ps.setString(1, cedula);
            if (ps.executeUpdate() > 0) {
                JOptionPane.showMessageDialog(null, "Técnico eliminado.");
                limpiar();
                cargarTabla();
            } else {
                JOptionPane.showMessageDialog(null, "No se encontró el técnico.");
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error: " + e.getMessage());
        }
    }

    @FXML
    public void FnInhabilitar() {
        String cedula = TXTcedula.getText().trim();
        if (cedula.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Ingrese la cédula del técnico a inhabilitar.");
            return;
        }
        int confirmar = JOptionPane.showConfirmDialog(null,
                "¿Inhabilitar al técnico con cédula " + cedula + "? No se eliminará, solo quedará inactivo.",
                "Confirmar", JOptionPane.YES_NO_OPTION);
        if (confirmar != JOptionPane.YES_OPTION) return;
        try (java.sql.Connection con = Conexion.establecerConexion();
             java.sql.PreparedStatement ps = con.prepareStatement(
                     "UPDATE tbl_tecnico SET estado = 'Inactivo' WHERE cedula = ?")) {
            ps.setString(1, cedula);
            ps.executeUpdate();
            JOptionPane.showMessageDialog(null, "Técnico inhabilitado correctamente.");
            limpiar();
            cargarTabla();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error al inhabilitar: " + e.getMessage());
        }
    }

    @FXML
    public void FnHabilitar() {
        String cedula = TXTcedula.getText().trim();
        if (cedula.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Ingrese la cédula del técnico a habilitar.");
            return;
        }
        try (java.sql.Connection con = Conexion.establecerConexion();
             java.sql.PreparedStatement ps = con.prepareStatement(
                     "UPDATE tbl_tecnico SET estado = 'Activo' WHERE cedula = ?")) {
            ps.setString(1, cedula);
            ps.executeUpdate();
            JOptionPane.showMessageDialog(null, "Técnico habilitado correctamente.");
            cargarTabla();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error al habilitar: " + e.getMessage());
        }
    }

    private void cargarTabla() {
        ObservableList<TecnicoRow> datos = FXCollections.observableArrayList();
        String sql = "SELECT nombre_tecnico, cedula, especialidad_tecnico, estado " +
                "FROM tbl_tecnico ORDER BY nombre_tecnico";

        try (Connection con = conexion.establecerConexion();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                datos.add(new TecnicoRow(
                        rs.getString("nombre_tecnico"),
                        rs.getString("cedula"),
                        rs.getString("especialidad_tecnico"),
                        rs.getString("estado")));
            }
            tablaTecnicos.setItems(datos);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error al cargar técnicos: " + e.getMessage());
        }
    }

    public void limpiar() {
        TXTnombre.clear(); TXTtelefono.clear();
        TXTEspecialidad.clear(); TXTcedula.clear();
    }

    public static class TecnicoRow {
        final SimpleStringProperty nombre, cedula, especialidad, estado;
        public TecnicoRow(String n, String c, String e, String est) {
            nombre       = new SimpleStringProperty(n);
            cedula       = new SimpleStringProperty(c);
            especialidad = new SimpleStringProperty(e);
            estado       = new SimpleStringProperty(est != null ? est : "Activo");
        }
        public String getNombre()       { return nombre.get(); }
        public String getCedula()       { return cedula.get(); }
        public String getEspecialidad() { return especialidad.get(); }
        public String getEstado()       { return estado.get(); }
    }
}
