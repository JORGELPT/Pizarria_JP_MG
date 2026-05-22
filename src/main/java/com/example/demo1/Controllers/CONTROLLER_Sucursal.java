package com.example.demo1.Controllers;

import com.example.demo1.Database.Conexion;
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

/**
 * Controller de Agregar Sucursal.
 * tbl_sucursal: id_sucursal (PK IDENTITY), nombre_sucursal, tipo_servicio, direccion
 */
public class CONTROLLER_Sucursal {

    Conexion conexion = new Conexion();

    @FXML private TextField TXTnombre;
    @FXML private TextField TXTdireccion;
    @FXML private ComboBox<String> cmbTipoServicio;

    @FXML private TableView<SucursalRow> tablaSucursales;
    @FXML private TableColumn<SucursalRow, String> colNombre;
    @FXML private TableColumn<SucursalRow, String> colTipo;
    @FXML private TableColumn<SucursalRow, String> colDireccion;
    @FXML private TableColumn<SucursalRow, String> colEstado;

    @FXML
    public void initialize() {
        cmbTipoServicio.setItems(FXCollections.observableArrayList(
                "Delivery", "Local", "Ambos"));

        colNombre.setCellValueFactory(c -> c.getValue().nombre);
        colTipo.setCellValueFactory(c -> c.getValue().tipo);
        colDireccion.setCellValueFactory(c -> c.getValue().direccion);
        colEstado.setCellValueFactory(c -> c.getValue().estado);

        tablaSucursales.getSelectionModel().selectedItemProperty().addListener(
                (obs, old, sel) -> {
                    if (sel == null) return;
                    TXTnombre.setText(sel.nombre.get());
                    cmbTipoServicio.setValue(sel.tipo.get());
                    TXTdireccion.setText(sel.direccion.get());
                });

        cargarTabla();
    }

    @FXML
    public void FnGuardar() {
        String nombre    = TXTnombre.getText().trim();
        String direccion = TXTdireccion.getText().trim();
        String tipo      = cmbTipoServicio.getValue();

        if (nombre.isEmpty()) {
            JOptionPane.showMessageDialog(null, "El nombre es obligatorio.");
            return;
        }

        String sql = "INSERT INTO tbl_sucursal (nombre_sucursal, tipo_servicio, direccion) " +
                "VALUES (?, ?, ?)";

        try (Connection con = conexion.establecerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, nombre);
            ps.setString(2, tipo);
            ps.setString(3, direccion.isEmpty() ? null : direccion);
            ps.executeUpdate();
            JOptionPane.showMessageDialog(null, "Sucursal guardada correctamente.");
            limpiar();
            cargarTabla();

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error al guardar: " + e.getMessage());
        }
    }

    @FXML
    public void FnBuscar() {
        String nombre = TXTnombre.getText().trim();
        if (nombre.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Ingrese un nombre para buscar.");
            return;
        }

        String sql = "SELECT nombre_sucursal, tipo_servicio, direccion " +
                "FROM tbl_sucursal WHERE nombre_sucursal = ?";

        try (Connection con = conexion.establecerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, nombre);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                TXTnombre.setText(rs.getString("nombre_sucursal"));
                cmbTipoServicio.setValue(rs.getString("tipo_servicio"));
                TXTdireccion.setText(rs.getString("direccion"));
            } else {
                JOptionPane.showMessageDialog(null, "No se encontró la sucursal.");
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error: " + e.getMessage());
        }
    }

    @FXML
    public void FnEditar() {
        String nombre    = TXTnombre.getText().trim();
        String direccion = TXTdireccion.getText().trim();
        String tipo      = cmbTipoServicio.getValue();

        if (nombre.isEmpty()) {
            JOptionPane.showMessageDialog(null, "El nombre es obligatorio para editar.");
            return;
        }

        String sql = "UPDATE tbl_sucursal SET tipo_servicio = ?, direccion = ? " +
                "WHERE nombre_sucursal = ?";

        try (Connection con = conexion.establecerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, tipo);
            ps.setString(2, direccion.isEmpty() ? null : direccion);
            ps.setString(3, nombre);
            if (ps.executeUpdate() > 0) {
                JOptionPane.showMessageDialog(null, "Sucursal actualizada.");
                cargarTabla();
            } else {
                JOptionPane.showMessageDialog(null, "No se encontró la sucursal.");
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error: " + e.getMessage());
        }
    }

    @FXML
    public void FnEliminar() {
        if (!com.example.demo1.Utils.Permisos_Util.verificarEliminar()) return;
        String nombre = TXTnombre.getText().trim();
        if (nombre.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Ingrese el nombre a eliminar.");
            return;
        }

        int c = JOptionPane.showConfirmDialog(null,
                "¿Eliminar la sucursal '" + nombre + "'?",
                "Confirmar", JOptionPane.YES_NO_OPTION);
        if (c != JOptionPane.YES_OPTION) return;

        try (Connection con = conexion.establecerConexion();
             PreparedStatement ps = con.prepareStatement(
                     "DELETE FROM tbl_sucursal WHERE nombre_sucursal = ?")) {

            ps.setString(1, nombre);
            if (ps.executeUpdate() > 0) {
                JOptionPane.showMessageDialog(null, "Sucursal eliminada.");
                limpiar();
                cargarTabla();
            } else {
                JOptionPane.showMessageDialog(null, "No se encontró la sucursal.");
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error: " + e.getMessage());
        }
    }

    @FXML
    public void FnInhabilitar() {
        String nombre = TXTnombre.getText().trim();
        if (nombre.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Ingrese el nombre de la sucursal a inhabilitar.");
            return;
        }
        int confirmar = JOptionPane.showConfirmDialog(null,
                "¿Inhabilitar '" + nombre + "'? No se eliminará, solo quedará inactiva.",
                "Confirmar", JOptionPane.YES_NO_OPTION);
        if (confirmar != JOptionPane.YES_OPTION) return;
        try (java.sql.Connection con = Conexion.establecerConexion();
             java.sql.PreparedStatement ps = con.prepareStatement(
                     "UPDATE tbl_sucursal SET estado = 'Inactivo' WHERE nombre_sucursal = ?")) {
            ps.setString(1, nombre);
            ps.executeUpdate();
            JOptionPane.showMessageDialog(null, "Sucursal inhabilitada correctamente.");
            limpiar();
            cargarTabla();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error al inhabilitar: " + e.getMessage());
        }
    }

    @FXML
    public void FnHabilitar() {
        String nombre = TXTnombre.getText().trim();
        if (nombre.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Ingrese el nombre de la sucursal a habilitar.");
            return;
        }
        try (java.sql.Connection con = Conexion.establecerConexion();
             java.sql.PreparedStatement ps = con.prepareStatement(
                     "UPDATE tbl_sucursal SET estado = 'Activo' WHERE nombre_sucursal = ?")) {
            ps.setString(1, nombre);
            ps.executeUpdate();
            JOptionPane.showMessageDialog(null, "Sucursal habilitada correctamente.");
            cargarTabla();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error al habilitar: " + e.getMessage());
        }
    }

    private void cargarTabla() {
        ObservableList<SucursalRow> datos = FXCollections.observableArrayList();
        String sql = "SELECT nombre_sucursal, tipo_servicio, direccion, estado " +
                "FROM tbl_sucursal ORDER BY nombre_sucursal";

        try (Connection con = conexion.establecerConexion();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                datos.add(new SucursalRow(
                        rs.getString("nombre_sucursal"),
                        rs.getString("tipo_servicio"),
                        rs.getString("direccion"),
                        rs.getString("estado")));
            }
            tablaSucursales.setItems(datos);

        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error al cargar sucursales: " + e.getMessage());
        }
    }

    public void limpiar() {
        TXTnombre.clear();
        TXTdireccion.clear();
        cmbTipoServicio.getSelectionModel().clearSelection();
    }

    public static class SucursalRow {
        final SimpleStringProperty nombre, tipo, direccion, estado;

        public SucursalRow(String n, String t, String d, String est) {
            nombre    = new SimpleStringProperty(n);
            tipo      = new SimpleStringProperty(t);
            direccion = new SimpleStringProperty(d != null ? d : "");
            estado    = new SimpleStringProperty(est != null ? est : "Activo");
        }

        public String getNombre()    { return nombre.get(); }
        public String getTipo()      { return tipo.get(); }
        public String getDireccion() { return direccion.get(); }
        public String getEstado()    { return estado.get(); }
    }
}
