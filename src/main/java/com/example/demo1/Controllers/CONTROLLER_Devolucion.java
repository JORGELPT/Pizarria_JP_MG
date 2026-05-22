package com.example.demo1.Controllers;

import com.example.demo1.Database.Conexion;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javax.swing.JOptionPane;
import java.sql.*;

public class CONTROLLER_Devolucion {

    Conexion conexion = new Conexion();

    @FXML private TextField        TXTidPedido;
    @FXML private TextField        TXTmonto;
    @FXML private TextField        TXTfecha;
    @FXML private TextField        TXTdescripcion;
    @FXML private ComboBox<String> cmbMotivo;
    @FXML private Label            lblCliente;

    @FXML private TableView<DevolucionRow>           tablaDevoluciones;
    @FXML private TableColumn<DevolucionRow, String> colIdPedido;
    @FXML private TableColumn<DevolucionRow, String> colCliente;
    @FXML private TableColumn<DevolucionRow, String> colMotivo;
    @FXML private TableColumn<DevolucionRow, String> colMonto;
    @FXML private TableColumn<DevolucionRow, String> colFecha;
    @FXML private TableColumn<DevolucionRow, String> colEstado;

    private int idDevolucionSeleccionado = -1;

    @FXML
    public void initialize() {
        colIdPedido.setCellValueFactory(c -> c.getValue().idPedido);
        colCliente.setCellValueFactory(c  -> c.getValue().cliente);
        colMotivo.setCellValueFactory(c   -> c.getValue().motivo);
        colMonto.setCellValueFactory(c    -> c.getValue().monto);
        colFecha.setCellValueFactory(c    -> c.getValue().fecha);
        colEstado.setCellValueFactory(c   -> c.getValue().estado);

        cmbMotivo.setItems(FXCollections.observableArrayList(
                "Producto dañado", "Pedido incorrecto", "Producto vencido",
                "Error en el cobro", "Insatisfacción del cliente", "Otro"));

        tablaDevoluciones.getSelectionModel().selectedItemProperty().addListener(
                (obs, old, sel) -> {
                    if (sel == null) return;
                    idDevolucionSeleccionado = sel.idDevolucion;
                    TXTidPedido.setText(sel.idPedido.get());
                    TXTmonto.setText(sel.monto.get());
                    TXTfecha.setText(sel.fecha.get());
                    TXTdescripcion.setText(sel.motivo.get());
                    cmbMotivo.setValue(sel.motivo.get());
                    lblCliente.setText("Cliente: " + sel.cliente.get());
                });

        cargarTabla();
    }

    @FXML
    public void FnBuscarPedido() {
        String idStr = TXTidPedido.getText().trim();
        if (idStr.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Ingrese el ID del pedido.");
            return;
        }
        String sql = "SELECT ped.id_pedido, ISNULL(per.nombre, '—') AS nombre " +
                "FROM tbl_pedido ped " +
                "LEFT JOIN tbl_persona per ON ped.id_cliente = per.id_persona " +
                "WHERE ped.id_pedido = ?";
        try (Connection con = conexion.establecerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, Integer.parseInt(idStr));
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                lblCliente.setText("Cliente: " + rs.getString("nombre"));
            } else {
                lblCliente.setText("Cliente: — no encontrado");
                JOptionPane.showMessageDialog(null, "No se encontró el pedido.");
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error al buscar pedido: " + e.getMessage());
        }
    }

    @FXML
    public void FnGuardar() {
        String idPedido    = TXTidPedido.getText().trim();
        String motivo      = cmbMotivo.getValue();
        String montoStr    = TXTmonto.getText().trim();
        String fecha       = TXTfecha.getText().trim();
        String descripcion = TXTdescripcion.getText().trim();

        if (idPedido.isEmpty() || motivo == null || montoStr.isEmpty()) {
            JOptionPane.showMessageDialog(null, "ID pedido, motivo y monto son obligatorios.");
            return;
        }

        String sql = "INSERT INTO tbl_devolucion (id_pedido, motivo, monto, fecha, descripcion) " +
                "VALUES (?, ?, ?, ?, ?)";
        try (Connection con = conexion.establecerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, Integer.parseInt(idPedido));
            ps.setString(2, motivo);
            ps.setBigDecimal(3, new java.math.BigDecimal(montoStr));
            ps.setDate(4, fecha.isEmpty() ? new java.sql.Date(System.currentTimeMillis())
                    : java.sql.Date.valueOf(fecha));
            ps.setString(5, descripcion.isEmpty() ? null : descripcion);
            ps.executeUpdate();
            JOptionPane.showMessageDialog(null, "Devolución registrada correctamente.");
            limpiar();
            cargarTabla();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error al guardar: " + e.getMessage());
        }
    }

    @FXML
    public void FnEliminar() {
        if (!com.example.demo1.Utils.Permisos_Util.verificarEliminar()) return;
        if (idDevolucionSeleccionado == -1) {
            JOptionPane.showMessageDialog(null, "Seleccione una devolución de la tabla primero.");
            return;
        }
        int confirmar = JOptionPane.showConfirmDialog(null,
                "¿Está seguro de eliminar esta devolución?",
                "Confirmar eliminación", JOptionPane.YES_NO_OPTION);
        if (confirmar != JOptionPane.YES_OPTION) return;

        String sql = "DELETE FROM tbl_devolucion WHERE id_devolucion = ?";
        try (Connection con = conexion.establecerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idDevolucionSeleccionado);
            ps.executeUpdate();
            JOptionPane.showMessageDialog(null, "Devolución eliminada correctamente.");
            idDevolucionSeleccionado = -1;
            limpiar();
            cargarTabla();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error al eliminar: " + e.getMessage());
        }
    }

    @FXML
    public void FnInhabilitar() {
        if (idDevolucionSeleccionado == -1) {
            JOptionPane.showMessageDialog(null, "Seleccione una devolución de la tabla primero.");
            return;
        }
        int confirmar = JOptionPane.showConfirmDialog(null,
                "¿Inhabilitar esta devolución? No se eliminará, solo quedará inactiva.",
                "Confirmar", JOptionPane.YES_NO_OPTION);
        if (confirmar != JOptionPane.YES_OPTION) return;
        try (java.sql.Connection con = Conexion.establecerConexion();
             java.sql.PreparedStatement ps = con.prepareStatement(
                     "UPDATE tbl_devolucion SET estado = 'Inactivo' WHERE id_devolucion = ?")) {
            ps.setInt(1, idDevolucionSeleccionado);
            ps.executeUpdate();
            JOptionPane.showMessageDialog(null, "Devolución inhabilitada correctamente.");
            idDevolucionSeleccionado = -1;
            cargarTabla();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error al inhabilitar: " + e.getMessage());
        }
    }

    @FXML
    public void FnHabilitar() {
        if (idDevolucionSeleccionado == -1) {
            JOptionPane.showMessageDialog(null, "Seleccione una devolución de la tabla primero.");
            return;
        }
        try (java.sql.Connection con = Conexion.establecerConexion();
             java.sql.PreparedStatement ps = con.prepareStatement(
                     "UPDATE tbl_devolucion SET estado = 'Activo' WHERE id_devolucion = ?")) {
            ps.setInt(1, idDevolucionSeleccionado);
            ps.executeUpdate();
            JOptionPane.showMessageDialog(null, "Devolución habilitada correctamente.");
            idDevolucionSeleccionado = -1;
            cargarTabla();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error al habilitar: " + e.getMessage());
        }
    }

    @FXML
    public void FnEditar() {
        if (idDevolucionSeleccionado == -1) {
            JOptionPane.showMessageDialog(null, "Seleccione una devolución de la tabla primero.");
            return;
        }
        String motivo      = cmbMotivo.getValue();
        String montoStr    = TXTmonto.getText().trim();
        String fecha       = TXTfecha.getText().trim();
        String descripcion = TXTdescripcion.getText().trim();

        if (motivo == null || montoStr.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Motivo y monto son obligatorios para editar.");
            return;
        }

        String sql = "UPDATE tbl_devolucion SET motivo=?, monto=?, fecha=?, descripcion=? WHERE id_devolucion=?";
        try (Connection con = conexion.establecerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, motivo);
            ps.setBigDecimal(2, new java.math.BigDecimal(montoStr));
            ps.setDate(3, fecha.isEmpty() ? new java.sql.Date(System.currentTimeMillis())
                    : java.sql.Date.valueOf(fecha));
            ps.setString(4, descripcion.isEmpty() ? null : descripcion);
            ps.setInt(5, idDevolucionSeleccionado);
            ps.executeUpdate();
            JOptionPane.showMessageDialog(null, "Devolución actualizada correctamente.");
            idDevolucionSeleccionado = -1;
            limpiar();
            cargarTabla();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error al actualizar: " + e.getMessage());
        }
    }

    @FXML
    public void FnBuscar() {
        String idStr = TXTidPedido.getText().trim();
        if (idStr.isEmpty()) { cargarTabla(); return; }

        ObservableList<DevolucionRow> datos = FXCollections.observableArrayList();
        String sql = "SELECT d.id_devolucion, d.id_pedido, ISNULL(per.nombre, '—') AS nombre, d.motivo, d.monto, d.fecha " +
                "FROM tbl_devolucion d " +
                "INNER JOIN tbl_pedido ped ON d.id_pedido = ped.id_pedido " +
                "LEFT JOIN tbl_persona per ON ped.id_cliente = per.id_persona " +
                "WHERE d.id_pedido = ?";
        try (Connection con = conexion.establecerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, Integer.parseInt(idStr));
            ResultSet rs = ps.executeQuery();
            while (rs.next()) datos.add(filaDesdeRS(rs));
            tablaDevoluciones.setItems(datos);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error al buscar: " + e.getMessage());
        }
    }

    private void cargarTabla() {
        ObservableList<DevolucionRow> datos = FXCollections.observableArrayList();
        String sql = "SELECT d.id_devolucion, d.id_pedido, ISNULL(per.nombre, '—') AS nombre, d.motivo, d.monto, d.fecha, d.estado " +
                "FROM tbl_devolucion d " +
                "INNER JOIN tbl_pedido ped ON d.id_pedido = ped.id_pedido " +
                "LEFT JOIN tbl_persona per ON ped.id_cliente = per.id_persona " +
                "ORDER BY d.fecha DESC";
        try (Connection con = conexion.establecerConexion();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) datos.add(filaDesdeRS(rs));
            tablaDevoluciones.setItems(datos);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error al cargar devoluciones: " + e.getMessage());
        }
    }

    private DevolucionRow filaDesdeRS(ResultSet rs) throws SQLException {
        return new DevolucionRow(
                rs.getInt("id_devolucion"),
                String.valueOf(rs.getInt("id_pedido")),
                rs.getString("nombre") != null ? rs.getString("nombre") : "",
                rs.getString("motivo") != null ? rs.getString("motivo") : "",
                rs.getBigDecimal("monto") != null ? rs.getBigDecimal("monto").toPlainString() : "",
                rs.getDate("fecha") != null ? rs.getDate("fecha").toString() : "",
                rs.getString("estado") != null ? rs.getString("estado") : "Activo"
        );
    }

    private void limpiar() {
        TXTidPedido.clear(); TXTmonto.clear();
        TXTfecha.clear(); TXTdescripcion.clear();
        cmbMotivo.getSelectionModel().clearSelection();
        lblCliente.setText("Cliente: —");
    }

    public static class DevolucionRow {
        final int idDevolucion;
        final SimpleStringProperty idPedido, cliente, motivo, monto, fecha, estado;

        public DevolucionRow(int idDevolucion, String idPedido, String cliente,
                             String motivo, String monto, String fecha, String estado) {
            this.idDevolucion = idDevolucion;
            this.idPedido = new SimpleStringProperty(idPedido);
            this.cliente  = new SimpleStringProperty(cliente);
            this.motivo   = new SimpleStringProperty(motivo);
            this.monto    = new SimpleStringProperty(monto);
            this.fecha    = new SimpleStringProperty(fecha);
            this.estado   = new SimpleStringProperty(estado);
        }
    }
}
