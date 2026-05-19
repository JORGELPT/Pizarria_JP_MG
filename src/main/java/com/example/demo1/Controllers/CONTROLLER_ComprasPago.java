package com.example.demo1.Controllers;

import com.example.demo1.Database.Conexion;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javax.swing.JOptionPane;
import java.sql.*;

/**
 * tbl_compra: id_compra, id_proveedor, monto, monto_pendiente, fecha,
 *             cantidad, id_sucursal, precio, observacion, estado
 */
public class CONTROLLER_ComprasPago {

    Conexion conexion = new Conexion();

    @FXML private ComboBox<String> cmbProveedor;
    @FXML private ComboBox<String> cmbSucursal;
    @FXML private ComboBox<String> cmbEstado;
    @FXML private TextField TXTfecha;
    @FXML private TextField TXTmonto;
    @FXML private TextField TXTmontoPendiente;
    @FXML private TextField TXTprecio;
    @FXML private TextField TXTcantidad;
    @FXML private TextField TXTobservacion;

    @FXML private TableView<CompraRow>              tablaCompras;
    @FXML private TableColumn<CompraRow, String>    colProveedor;
    @FXML private TableColumn<CompraRow, String>    colFecha;
    @FXML private TableColumn<CompraRow, String>    colMonto;
    @FXML private TableColumn<CompraRow, String>    colMontoPendiente;
    @FXML private TableColumn<CompraRow, String>    colCantidad;
    @FXML private TableColumn<CompraRow, String>    colEstado;

    private int idProveedorSel = -1;
    private int idSucursalSel  = -1;
    private int idCompraSeleccionada = -1;

    @FXML
    public void initialize() {
        colProveedor.setCellValueFactory(c      -> c.getValue().proveedor);
        colFecha.setCellValueFactory(c          -> c.getValue().fecha);
        colMonto.setCellValueFactory(c          -> c.getValue().monto);
        colMontoPendiente.setCellValueFactory(c -> c.getValue().montoPendiente);
        colCantidad.setCellValueFactory(c       -> c.getValue().cantidad);
        colEstado.setCellValueFactory(c         -> c.getValue().estado);

        cmbEstado.setItems(FXCollections.observableArrayList(
                "Pendiente", "Pagado", "Parcial", "Cancelado"));

        tablaCompras.getSelectionModel().selectedItemProperty().addListener(
                (obs, old, sel) -> {
                    if (sel == null) return;
                    idCompraSeleccionada = sel.idCompra;
                    TXTfecha.setText(sel.fecha.get());
                    TXTmonto.setText(sel.monto.get());
                    TXTmontoPendiente.setText(sel.montoPendiente.get());
                    TXTcantidad.setText(sel.cantidad.get());
                    cmbEstado.setValue(sel.estado.get());
                    cmbProveedor.setValue(sel.proveedor.get());
                });

        cargarProveedores();
        cargarSucursales();
        cargarTabla();
    }

    private void cargarProveedores() {
        ObservableList<String> lista = FXCollections.observableArrayList();
        try (Connection con = conexion.establecerConexion();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT nombre_proveedor FROM tbl_proveedor ORDER BY nombre_proveedor");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) lista.add(rs.getString("nombre_proveedor"));
            cmbProveedor.setItems(lista);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error al cargar proveedores: " + e.getMessage());
        }
    }

    private void cargarSucursales() {
        ObservableList<String> lista = FXCollections.observableArrayList();
        try (Connection con = conexion.establecerConexion();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT nombre_sucursal FROM tbl_sucursal ORDER BY nombre_sucursal");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) lista.add(rs.getString("nombre_sucursal"));
            cmbSucursal.setItems(lista);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error al cargar sucursales: " + e.getMessage());
        }
    }

    private int resolverIdProveedor(Connection con, String nombre) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT id_proveedor FROM tbl_proveedor WHERE nombre_proveedor = ?")) {
            ps.setString(1, nombre);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getInt("id_proveedor") : -1;
        }
    }

    private int resolverIdSucursal(Connection con, String nombre) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT id_sucursal FROM tbl_sucursal WHERE nombre_sucursal = ?")) {
            ps.setString(1, nombre);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getInt("id_sucursal") : -1;
        }
    }

    @FXML
    public void FnGuardar() {
        String proveedor      = cmbProveedor.getValue();
        String sucursal       = cmbSucursal.getValue();
        String estado         = cmbEstado.getValue();
        String fecha          = TXTfecha.getText().trim();
        String montoStr       = TXTmonto.getText().trim();
        String pendienteStr   = TXTmontoPendiente.getText().trim();
        String precioStr      = TXTprecio.getText().trim();
        String cantidadStr    = TXTcantidad.getText().trim();
        String observacion    = TXTobservacion.getText().trim();

        if (proveedor == null || fecha.isEmpty() || montoStr.isEmpty() || cantidadStr.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Proveedor, fecha, monto y cantidad son obligatorios.");
            return;
        }

        String sql = "INSERT INTO tbl_compra " +
                "(id_proveedor, monto, monto_pendiente, fecha, cantidad, id_sucursal, precio, observacion, estado) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection con = conexion.establecerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {

            int idProv = resolverIdProveedor(con, proveedor);
            if (idProv == -1) {
                JOptionPane.showMessageDialog(null, "No se encontró el proveedor seleccionado.");
                return;
            }
            int idSuc = sucursal != null ? resolverIdSucursal(con, sucursal) : 1;

            ps.setInt(1, idProv);
            ps.setBigDecimal(2, new java.math.BigDecimal(montoStr));
            ps.setBigDecimal(3, pendienteStr.isEmpty() ? java.math.BigDecimal.ZERO
                    : new java.math.BigDecimal(pendienteStr));
            ps.setDate(4, java.sql.Date.valueOf(fecha));
            ps.setInt(5, Integer.parseInt(cantidadStr));
            ps.setInt(6, idSuc == -1 ? 1 : idSuc);
            ps.setDouble(7, precioStr.isEmpty() ? 0 : Double.parseDouble(precioStr));
            ps.setString(8, observacion.isEmpty() ? null : observacion);
            ps.setString(9, estado);
            ps.executeUpdate();

            JOptionPane.showMessageDialog(null, "Compra registrada correctamente.");
            limpiar();
            cargarTabla();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error al guardar: " + e.getMessage());
        }
    }

    @FXML
    public void FnEliminar() {
        if (idCompraSeleccionada == -1) {
            JOptionPane.showMessageDialog(null, "Seleccione una compra de la tabla primero.");
            return;
        }
        int confirmar = JOptionPane.showConfirmDialog(null,
                "¿Está seguro de eliminar esta compra?",
                "Confirmar eliminación", JOptionPane.YES_NO_OPTION);
        if (confirmar != JOptionPane.YES_OPTION) return;

        String sql = "DELETE FROM tbl_compra WHERE id_compra = ?";
        try (Connection con = conexion.establecerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idCompraSeleccionada);
            ps.executeUpdate();
            JOptionPane.showMessageDialog(null, "Compra eliminada correctamente.");
            idCompraSeleccionada = -1;
            limpiar();
            cargarTabla();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error al eliminar: " + e.getMessage());
        }
    }

    @FXML
    public void FnEditar() {
        if (idCompraSeleccionada == -1) {
            JOptionPane.showMessageDialog(null, "Seleccione una compra de la tabla primero.");
            return;
        }
        String proveedor    = cmbProveedor.getValue();
        String estado       = cmbEstado.getValue();
        String fecha        = TXTfecha.getText().trim();
        String montoStr     = TXTmonto.getText().trim();
        String pendienteStr = TXTmontoPendiente.getText().trim();
        String precioStr    = TXTprecio.getText().trim();
        String cantidadStr  = TXTcantidad.getText().trim();
        String observacion  = TXTobservacion.getText().trim();

        if (proveedor == null || fecha.isEmpty() || montoStr.isEmpty() || cantidadStr.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Proveedor, fecha, monto y cantidad son obligatorios.");
            return;
        }

        String sql = "UPDATE tbl_compra SET id_proveedor=?, monto=?, monto_pendiente=?, fecha=?, " +
                "cantidad=?, precio=?, observacion=?, estado=? WHERE id_compra=?";
        try (Connection con = conexion.establecerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            int idProv = resolverIdProveedor(con, proveedor);
            if (idProv == -1) {
                JOptionPane.showMessageDialog(null, "No se encontró el proveedor seleccionado.");
                return;
            }
            ps.setInt(1, idProv);
            ps.setBigDecimal(2, new java.math.BigDecimal(montoStr));
            ps.setBigDecimal(3, pendienteStr.isEmpty() ? java.math.BigDecimal.ZERO
                    : new java.math.BigDecimal(pendienteStr));
            ps.setDate(4, java.sql.Date.valueOf(fecha));
            ps.setInt(5, Integer.parseInt(cantidadStr));
            ps.setDouble(6, precioStr.isEmpty() ? 0 : Double.parseDouble(precioStr));
            ps.setString(7, observacion.isEmpty() ? null : observacion);
            ps.setString(8, estado);
            ps.setInt(9, idCompraSeleccionada);
            ps.executeUpdate();
            JOptionPane.showMessageDialog(null, "Compra actualizada correctamente.");
            idCompraSeleccionada = -1;
            limpiar();
            cargarTabla();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error al actualizar: " + e.getMessage());
        }
    }

    @FXML
    public void FnBuscar() {
        String proveedor = cmbProveedor.getValue();
        ObservableList<CompraRow> datos = FXCollections.observableArrayList();

        String sql = "SELECT c.id_compra, p.nombre_proveedor, c.fecha, c.monto, c.monto_pendiente, " +
                "c.cantidad, c.estado " +
                "FROM tbl_compra c " +
                "INNER JOIN tbl_proveedor p ON c.id_proveedor = p.id_proveedor ";
        if (proveedor != null && !proveedor.isEmpty())
            sql += "WHERE p.nombre_proveedor LIKE ? ";
        sql += "ORDER BY c.fecha DESC";

        try (Connection con = conexion.establecerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            if (proveedor != null && !proveedor.isEmpty())
                ps.setString(1, "%" + proveedor + "%");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) datos.add(filaDesdeRS(rs));
            tablaCompras.setItems(datos);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error al buscar: " + e.getMessage());
        }
    }

    private void cargarTabla() {
        ObservableList<CompraRow> datos = FXCollections.observableArrayList();
        String sql = "SELECT c.id_compra, p.nombre_proveedor, c.fecha, c.monto, c.monto_pendiente, " +
                "c.cantidad, c.estado " +
                "FROM tbl_compra c " +
                "INNER JOIN tbl_proveedor p ON c.id_proveedor = p.id_proveedor " +
                "ORDER BY c.fecha DESC";
        try (Connection con = conexion.establecerConexion();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) datos.add(filaDesdeRS(rs));
            tablaCompras.setItems(datos);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error al cargar compras: " + e.getMessage());
        }
    }

    private CompraRow filaDesdeRS(ResultSet rs) throws SQLException {
        return new CompraRow(
                rs.getInt("id_compra"),
                rs.getString("nombre_proveedor"),
                rs.getDate("fecha") != null ? rs.getDate("fecha").toString() : "",
                rs.getBigDecimal("monto") != null ? rs.getBigDecimal("monto").toPlainString() : "",
                rs.getBigDecimal("monto_pendiente") != null ? rs.getBigDecimal("monto_pendiente").toPlainString() : "",
                String.valueOf(rs.getInt("cantidad")),
                rs.getString("estado") != null ? rs.getString("estado") : ""
        );
    }

    private void limpiar() {
        cmbProveedor.getSelectionModel().clearSelection();
        cmbSucursal.getSelectionModel().clearSelection();
        cmbEstado.getSelectionModel().clearSelection();
        TXTfecha.clear(); TXTmonto.clear(); TXTmontoPendiente.clear();
        TXTprecio.clear(); TXTcantidad.clear(); TXTobservacion.clear();
    }

    public static class CompraRow {
        final int idCompra;
        final SimpleStringProperty proveedor, fecha, monto, montoPendiente, cantidad, estado;

        public CompraRow(int idCompra, String proveedor, String fecha, String monto,
                         String montoPendiente, String cantidad, String estado) {
            this.idCompra       = idCompra;
            this.proveedor      = new SimpleStringProperty(proveedor);
            this.fecha          = new SimpleStringProperty(fecha);
            this.monto          = new SimpleStringProperty(monto);
            this.montoPendiente = new SimpleStringProperty(montoPendiente);
            this.cantidad       = new SimpleStringProperty(cantidad);
            this.estado         = new SimpleStringProperty(estado);
        }
    }
}
