package com.example.demo1.Controllers;

import com.example.demo1.Database.Conexion;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javax.swing.JOptionPane;
import java.sql.*;

/**
 * Controller de Agregar Ingrediente (pantalla Ingrediente.fxml).
 * tbl_ingrediente: nombre, tipo_ingrediente, cantidad_stock, fecha_caducidad,
 *                  cantidad_minima_stock, id_proveedor
 */
public class CONTROLLER_Inv_Ingrediente {

    Conexion conexion = new Conexion();

    @FXML private TextField TXTnombre;
    @FXML private TextField TXTcantidad;
    @FXML private TextField TXTfechaCaducidad;
    @FXML private TextField TXTcantMin;
    @FXML private ComboBox<String> cmbTipoIngrediente;
    @FXML private ComboBox<String> cmbProveedor;

    @FXML
    public void initialize() {
        cmbTipoIngrediente.setItems(FXCollections.observableArrayList(
                "Lácteo", "Cárnico", "Vegetal", "Harina", "Condimento", "Bebida", "Otro"));
        cargarProveedores();
    }

    private void cargarProveedores() {
        try (Connection con = conexion.establecerConexion();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT nombre_proveedor FROM tbl_proveedor ORDER BY nombre_proveedor");
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                cmbProveedor.getItems().add(rs.getString("nombre_proveedor"));
            }
        } catch (Exception ignore) {}
    }

    @FXML
    public void FnGuardar() {
        String nombre     = TXTnombre.getText().trim();
        String cantStr    = TXTcantidad.getText().trim();
        String fechaStr   = TXTfechaCaducidad.getText().trim();
        String cantMinStr = TXTcantMin.getText().trim();
        String tipo       = cmbTipoIngrediente.getValue();

        if (nombre.isEmpty() || cantStr.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Nombre y cantidad son obligatorios.");
            return;
        }

        int cantStock;
        try {
            cantStock = Integer.parseInt(cantStr);
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(null, "La cantidad debe ser un número entero.");
            return;
        }

        int cantMin = 0;
        if (!cantMinStr.isEmpty()) {
            try {
                cantMin = Integer.parseInt(cantMinStr);
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(null, "La cantidad mínima debe ser un número entero.");
                return;
            }
        }

        // Obtener id_proveedor si se seleccionó uno
        String proveedorSeleccionado = cmbProveedor.getValue();
        Integer idProveedor = null;
        if (proveedorSeleccionado != null) {
            try (Connection con = conexion.establecerConexion();
                 PreparedStatement ps = con.prepareStatement(
                         "SELECT id_proveedor FROM tbl_proveedor WHERE nombre_proveedor = ?")) {
                ps.setString(1, proveedorSeleccionado);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) idProveedor = rs.getInt("id_proveedor");
            } catch (Exception ignore) {}
        }

        String sql = "INSERT INTO tbl_ingrediente " +
                "(nombre, tipo_ingrediente, cantidad_stock, fecha_caducidad, cantidad_minima_stock, id_proveedor) " +
                "VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection con = conexion.establecerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, nombre);
            ps.setString(2, tipo);
            ps.setInt(3, cantStock);
            if (fechaStr.isEmpty()) {
                ps.setNull(4, Types.DATE);
            } else {
                ps.setDate(4, Date.valueOf(fechaStr));
            }
            ps.setInt(5, cantMin);
            if (idProveedor == null) {
                ps.setNull(6, Types.INTEGER);
            } else {
                ps.setInt(6, idProveedor);
            }
            ps.executeUpdate();
            JOptionPane.showMessageDialog(null, "Ingrediente guardado correctamente.");
            limpiar();

        } catch (IllegalArgumentException e) {
            JOptionPane.showMessageDialog(null, "Formato de fecha inválido. Use YYYY-MM-DD");
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

        String sql = "SELECT nombre, tipo_ingrediente, cantidad_stock, fecha_caducidad, cantidad_minima_stock " +
                "FROM tbl_ingrediente WHERE nombre = ?";

        try (Connection con = conexion.establecerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, nombre);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                TXTnombre.setText(rs.getString("nombre"));
                cmbTipoIngrediente.setValue(rs.getString("tipo_ingrediente"));
                TXTcantidad.setText(String.valueOf(rs.getInt("cantidad_stock")));
                Date f = rs.getDate("fecha_caducidad");
                TXTfechaCaducidad.setText(f != null ? f.toString() : "");
                TXTcantMin.setText(String.valueOf(rs.getInt("cantidad_minima_stock")));
            } else {
                JOptionPane.showMessageDialog(null, "No se encontró el ingrediente.");
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error: " + e.getMessage());
        }
    }

    @FXML
    public void FnEditar() {
        String nombre     = TXTnombre.getText().trim();
        String cantStr    = TXTcantidad.getText().trim();
        String fechaStr   = TXTfechaCaducidad.getText().trim();
        String cantMinStr = TXTcantMin.getText().trim();
        String tipo       = cmbTipoIngrediente.getValue();

        if (nombre.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Busque primero el ingrediente (FnBuscar).");
            return;
        }
        if (cantStr.isEmpty()) {
            JOptionPane.showMessageDialog(null, "La cantidad es obligatoria.");
            return;
        }

        String sql = "UPDATE tbl_ingrediente SET tipo_ingrediente=?, cantidad_stock=?, " +
                "fecha_caducidad=?, cantidad_minima_stock=? WHERE nombre=?";
        try (Connection con = conexion.establecerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, tipo);
            ps.setInt(2, Integer.parseInt(cantStr));
            if (fechaStr.isEmpty()) {
                ps.setNull(3, java.sql.Types.DATE);
            } else {
                ps.setDate(3, java.sql.Date.valueOf(fechaStr));
            }
            ps.setInt(4, cantMinStr.isEmpty() ? 0 : Integer.parseInt(cantMinStr));
            ps.setString(5, nombre);
            int filas = ps.executeUpdate();
            if (filas > 0) {
                JOptionPane.showMessageDialog(null, "Ingrediente actualizado correctamente.");
            } else {
                JOptionPane.showMessageDialog(null, "No se encontró el ingrediente.");
            }
        } catch (IllegalArgumentException e) {
            JOptionPane.showMessageDialog(null, "Formato de fecha inválido. Use YYYY-MM-DD");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error al actualizar: " + e.getMessage());
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

        int c = JOptionPane.showConfirmDialog(null, "¿Eliminar '" + nombre + "'?",
                "Confirmar", JOptionPane.YES_NO_OPTION);
        if (c != JOptionPane.YES_OPTION) return;

        try (Connection con = conexion.establecerConexion();
             PreparedStatement ps = con.prepareStatement(
                     "DELETE FROM tbl_ingrediente WHERE nombre = ?")) {

            ps.setString(1, nombre);
            if (ps.executeUpdate() > 0) {
                JOptionPane.showMessageDialog(null, "Ingrediente eliminado.");
                limpiar();
            } else {
                JOptionPane.showMessageDialog(null, "No se encontró el ingrediente.");
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error: " + e.getMessage());
        }
    }

    public void limpiar() {
        TXTnombre.clear();
        TXTcantidad.clear();
        TXTfechaCaducidad.clear();
        TXTcantMin.clear();
        cmbTipoIngrediente.getSelectionModel().clearSelection();
        cmbProveedor.getSelectionModel().clearSelection();
    }
}
