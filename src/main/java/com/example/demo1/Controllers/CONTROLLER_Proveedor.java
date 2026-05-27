package com.example.demo1.Controllers;

import com.example.demo1.Database.Conexion;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javax.swing.JOptionPane;
import java.sql.*;

public class CONTROLLER_Proveedor {

    Conexion conexion = new Conexion();

    @FXML private TextField TXTnombre;
    @FXML private TextField TXTemail;
    @FXML private TextField TXTtelefono;
    @FXML private TextField TXTrnc;
    @FXML private TextField TXTdireccion;
    @FXML private TextArea  TXTdescripcion;
    @FXML private TableView<ProveedorRow> tablaProveedores;
    @FXML private TableColumn<ProveedorRow, String> colNombre;
    @FXML private TableColumn<ProveedorRow, String> colTelefono;
    @FXML private TableColumn<ProveedorRow, String> colRnc;
    @FXML private TableColumn<ProveedorRow, String> colEstado;

    @FXML
    public void initialize() {
        colNombre.setCellValueFactory(c -> c.getValue().nombre);
        colTelefono.setCellValueFactory(c -> c.getValue().telefono);
        colRnc.setCellValueFactory(c -> c.getValue().rnc);
        colEstado.setCellValueFactory(c -> c.getValue().estado);

        tablaProveedores.getSelectionModel().selectedItemProperty().addListener(
                (obs, old, sel) -> {
                    if (sel == null) return;
                    TXTnombre.setText(sel.nombre.get());
                    TXTtelefono.setText(sel.telefono.get());
                    TXTrnc.setText(sel.rnc.get());
                });

        cargarTabla();
    }

    @FXML
    public void FnGuardar() {
        String nombre = TXTnombre.getText().trim();
        String tel    = TXTtelefono.getText().trim();
        String rnc    = TXTrnc.getText().trim();

        if (nombre.isEmpty() || tel.isEmpty() || rnc.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Nombre, teléfono y RNC son obligatorios.");
            return;
        }

        String sql = "INSERT INTO tbl_proveedor " +
                "(nombre_proveedor, tel_proveedor, rnc, email, direccion, descripcion) " +
                "VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection con = conexion.establecerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, nombre);
            ps.setString(2, tel);
            ps.setString(3, rnc);
            ps.setString(4, TXTemail.getText().trim());
            ps.setString(5, TXTdireccion.getText().trim());
            ps.setString(6, TXTdescripcion.getText().trim());
            ps.executeUpdate();
            JOptionPane.showMessageDialog(null, "Proveedor guardado correctamente.");
            limpiar();
            cargarTabla();

        } catch (SQLException e) { //error al guardar proveedor - pantalla Agregar Proveedor
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

        String sql = "SELECT * FROM tbl_proveedor WHERE nombre_proveedor = ?";

        try (Connection con = conexion.establecerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, nombre);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                TXTnombre.setText(rs.getString("nombre_proveedor"));
                TXTtelefono.setText(rs.getString("tel_proveedor"));
                TXTrnc.setText(rs.getString("rnc"));
                TXTemail.setText(rs.getString("email"));
                TXTdireccion.setText(rs.getString("direccion"));
                TXTdescripcion.setText(rs.getString("descripcion"));
            } else {
                JOptionPane.showMessageDialog(null, "No se encontró el proveedor.");
            }

        } catch (Exception e) { //error al buscar proveedor - pantalla Agregar Proveedor
            JOptionPane.showMessageDialog(null, "Error: " + e.getMessage());
        }
    }

    @FXML
    public void FnEditar() {
        String nombre = TXTnombre.getText().trim();
        if (nombre.isEmpty()) {
            JOptionPane.showMessageDialog(null, "El nombre es obligatorio para editar.");
            return;
        }

        String sql = "UPDATE tbl_proveedor " +
                "SET tel_proveedor=?, rnc=?, email=?, direccion=?, descripcion=? " +
                "WHERE nombre_proveedor=?";

        try (Connection con = conexion.establecerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, TXTtelefono.getText().trim());
            ps.setString(2, TXTrnc.getText().trim());
            ps.setString(3, TXTemail.getText().trim());
            ps.setString(4, TXTdireccion.getText().trim());
            ps.setString(5, TXTdescripcion.getText().trim());
            ps.setString(6, nombre);
            if (ps.executeUpdate() > 0) {
                JOptionPane.showMessageDialog(null, "Proveedor actualizado.");
                cargarTabla();
            } else {
                JOptionPane.showMessageDialog(null, "No se encontró el proveedor.");
            }

        } catch (Exception e) { //error al editar proveedor - pantalla Agregar Proveedor
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

        int confirm = JOptionPane.showConfirmDialog(null,
                "¿Eliminar '" + nombre + "'?", "Confirmar", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        String sql = "DELETE FROM tbl_proveedor WHERE nombre_proveedor=?";

        try (Connection con = conexion.establecerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, nombre);
            if (ps.executeUpdate() > 0) {
                JOptionPane.showMessageDialog(null, "Proveedor eliminado.");
                limpiar();
                cargarTabla();
            } else {
                JOptionPane.showMessageDialog(null, "No se encontró el proveedor.");
            }

        } catch (Exception e) { //error al eliminar proveedor - pantalla Agregar Proveedor
            JOptionPane.showMessageDialog(null, "Error: " + e.getMessage());
        }
    }

    @FXML
    public void FnInhabilitar() {
        String nombre = TXTnombre.getText().trim();
        if (nombre.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Ingrese el nombre del proveedor a inhabilitar.");
            return;
        }
        int confirmar = JOptionPane.showConfirmDialog(null,
                "¿Inhabilitar '" + nombre + "'? No se eliminará, solo quedará inactivo.",
                "Confirmar", JOptionPane.YES_NO_OPTION);
        if (confirmar != JOptionPane.YES_OPTION) return;
        try (java.sql.Connection con = Conexion.establecerConexion();
             java.sql.PreparedStatement ps = con.prepareStatement(
                     "UPDATE tbl_proveedor SET estado = 'Inactivo' WHERE nombre_proveedor = ?")) {
            ps.setString(1, nombre);
            ps.executeUpdate();
            JOptionPane.showMessageDialog(null, "Proveedor inhabilitado correctamente.");
            limpiar();
            cargarTabla();
        } catch (Exception e) { //error al inhabilitar proveedor - pantalla Agregar Proveedor
            JOptionPane.showMessageDialog(null, "Error al inhabilitar: " + e.getMessage());
        }
    }

    @FXML
    public void FnHabilitar() {
        String nombre = TXTnombre.getText().trim();
        if (nombre.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Ingrese el nombre del proveedor a habilitar.");
            return;
        }
        try (java.sql.Connection con = Conexion.establecerConexion();
             java.sql.PreparedStatement ps = con.prepareStatement(
                     "UPDATE tbl_proveedor SET estado = 'Activo' WHERE nombre_proveedor = ?")) {
            ps.setString(1, nombre);
            ps.executeUpdate();
            JOptionPane.showMessageDialog(null, "Proveedor habilitado correctamente.");
            cargarTabla();
        } catch (Exception e) { //error al habilitar proveedor - pantalla Agregar Proveedor
            JOptionPane.showMessageDialog(null, "Error al habilitar: " + e.getMessage());
        }
    }

    private void cargarTabla() {
        ObservableList<ProveedorRow> datos = FXCollections.observableArrayList();
        String sql = "SELECT nombre_proveedor, tel_proveedor, rnc, estado " +
                "FROM tbl_proveedor ORDER BY nombre_proveedor";

        try (Connection con = conexion.establecerConexion();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                datos.add(new ProveedorRow(
                        rs.getString("nombre_proveedor"),
                        rs.getString("tel_proveedor"),
                        rs.getString("rnc"),
                        rs.getString("estado")));
            }
            tablaProveedores.setItems(datos);

        } catch (Exception e) { //error al cargar tabla de proveedores - pantalla Agregar Proveedor
            JOptionPane.showMessageDialog(null, "Error al cargar proveedores: " + e.getMessage());
        }
    }

    public void limpiar() {
        TXTnombre.clear();
        TXTemail.clear();
        TXTtelefono.clear();
        TXTrnc.clear();
        TXTdireccion.clear();
        TXTdescripcion.clear();
    }

    public static class ProveedorRow {
        final SimpleStringProperty nombre;
        final SimpleStringProperty telefono;
        final SimpleStringProperty rnc;
        final SimpleStringProperty estado;

        public ProveedorRow(String n, String t, String r, String est) {
            this.nombre   = new SimpleStringProperty(n);
            this.telefono = new SimpleStringProperty(t);
            this.rnc      = new SimpleStringProperty(r);
            this.estado   = new SimpleStringProperty(est != null ? est : "Activo");
        }

        public String getNombre()   { return nombre.get(); }
        public String getTelefono() { return telefono.get(); }
        public String getRnc()      { return rnc.get(); }
        public String getEstado()   { return estado.get(); }
    }
}
