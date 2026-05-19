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
 * tbl_envio: id_envio, costo_servicio, id_metodo_envio, id_pedido, observacion, direccion
 */
public class CONTROLLER_Envio {

    Conexion conexion = new Conexion();

    @FXML private TextField        TXTidPedido;
    @FXML private TextField        TXTdireccion;
    @FXML private TextField        TXTcostoServicio;
    @FXML private TextField        TXTobservacion;
    @FXML private ComboBox<String> cmbMetodoEnvio;
    @FXML private Label            lblCliente;

    @FXML private TableView<EnvioRow>           tablaEnvios;
    @FXML private TableColumn<EnvioRow, String> colIdPedido;
    @FXML private TableColumn<EnvioRow, String> colDireccion;
    @FXML private TableColumn<EnvioRow, String> colCosto;
    @FXML private TableColumn<EnvioRow, String> colMetodoEnvio;
    @FXML private TableColumn<EnvioRow, String> colObservacion;

    private int idEnvioSeleccionado = -1;

    @FXML
    public void initialize() {
        colIdPedido.setCellValueFactory(c    -> c.getValue().idPedido);
        colDireccion.setCellValueFactory(c   -> c.getValue().direccion);
        colCosto.setCellValueFactory(c       -> c.getValue().costo);
        colMetodoEnvio.setCellValueFactory(c -> c.getValue().metodoEnvio);
        colObservacion.setCellValueFactory(c -> c.getValue().observacion);

        cargarMetodosEnvio();

        tablaEnvios.getSelectionModel().selectedItemProperty().addListener(
                (obs, old, sel) -> {
                    if (sel == null) return;
                    idEnvioSeleccionado = sel.idEnvio;
                    TXTidPedido.setText(sel.idPedido.get());
                    TXTdireccion.setText(sel.direccion.get());
                    TXTcostoServicio.setText(sel.costo.get());
                    TXTobservacion.setText(sel.observacion.get());
                });

        cargarTabla();
    }

    private void cargarMetodosEnvio() {
        // Intenta cargar desde tbl_metodo_envio; si no existe usa valores fijos
        ObservableList<String> lista = FXCollections.observableArrayList();
        try (Connection con = conexion.establecerConexion();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT id_metodo_envio, descripcion FROM tbl_metodo_envio ORDER BY id_metodo_envio");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next())
                lista.add(rs.getInt("id_metodo_envio") + " - " + rs.getString("descripcion"));
        } catch (Exception ignored) {
            lista.setAll("1 - Propio", "2 - Externo", "3 - Recogida en tienda");
        }
        cmbMetodoEnvio.setItems(lista);
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
        String direccion   = TXTdireccion.getText().trim();
        String costoStr    = TXTcostoServicio.getText().trim();
        String observacion = TXTobservacion.getText().trim();
        String metodoSel   = cmbMetodoEnvio.getValue();

        if (idPedido.isEmpty() || direccion.isEmpty()) {
            JOptionPane.showMessageDialog(null, "ID pedido y dirección son obligatorios.");
            return;
        }

        int idMetodo = 1;
        if (metodoSel != null && !metodoSel.isEmpty()) {
            try { idMetodo = Integer.parseInt(metodoSel.split(" - ")[0].trim()); }
            catch (NumberFormatException ignored) {}
        }

        String sql = "INSERT INTO tbl_envio (id_pedido, direccion, costo_servicio, id_metodo_envio, observacion) " +
                "VALUES (?, ?, ?, ?, ?)";
        try (Connection con = conexion.establecerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, Integer.parseInt(idPedido));
            ps.setString(2, direccion);
            ps.setBigDecimal(3, costoStr.isEmpty() ? java.math.BigDecimal.ZERO
                    : new java.math.BigDecimal(costoStr));
            ps.setInt(4, idMetodo);
            ps.setString(5, observacion.isEmpty() ? null : observacion);
            ps.executeUpdate();
            JOptionPane.showMessageDialog(null, "Envío registrado correctamente.");
            limpiar();
            cargarTabla();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error al guardar: " + e.getMessage());
        }
    }

    @FXML
    public void FnEliminar() {
        if (idEnvioSeleccionado == -1) {
            JOptionPane.showMessageDialog(null, "Seleccione un envío de la tabla primero.");
            return;
        }
        int confirmar = JOptionPane.showConfirmDialog(null,
                "¿Está seguro de eliminar este envío?",
                "Confirmar eliminación", JOptionPane.YES_NO_OPTION);
        if (confirmar != JOptionPane.YES_OPTION) return;

        String sql = "DELETE FROM tbl_envio WHERE id_envio = ?";
        try (Connection con = conexion.establecerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idEnvioSeleccionado);
            ps.executeUpdate();
            JOptionPane.showMessageDialog(null, "Envío eliminado correctamente.");
            idEnvioSeleccionado = -1;
            limpiar();
            cargarTabla();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error al eliminar: " + e.getMessage());
        }
    }

    @FXML
    public void FnEditar() {
        if (idEnvioSeleccionado == -1) {
            JOptionPane.showMessageDialog(null, "Seleccione un envío de la tabla primero.");
            return;
        }
        String idPedido    = TXTidPedido.getText().trim();
        String direccion   = TXTdireccion.getText().trim();
        String costoStr    = TXTcostoServicio.getText().trim();
        String observacion = TXTobservacion.getText().trim();
        String metodoSel   = cmbMetodoEnvio.getValue();

        if (direccion.isEmpty()) {
            JOptionPane.showMessageDialog(null, "La dirección es obligatoria.");
            return;
        }

        int idMetodo = 1;
        if (metodoSel != null && !metodoSel.isEmpty()) {
            try { idMetodo = Integer.parseInt(metodoSel.split(" - ")[0].trim()); }
            catch (NumberFormatException ignored) {}
        }

        String sql = "UPDATE tbl_envio SET direccion=?, costo_servicio=?, id_metodo_envio=?, observacion=? WHERE id_envio=?";
        try (Connection con = conexion.establecerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, direccion);
            ps.setBigDecimal(2, costoStr.isEmpty() ? java.math.BigDecimal.ZERO
                    : new java.math.BigDecimal(costoStr));
            ps.setInt(3, idMetodo);
            ps.setString(4, observacion.isEmpty() ? null : observacion);
            ps.setInt(5, idEnvioSeleccionado);
            ps.executeUpdate();
            JOptionPane.showMessageDialog(null, "Envío actualizado correctamente.");
            idEnvioSeleccionado = -1;
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

        ObservableList<EnvioRow> datos = FXCollections.observableArrayList();
        String sql = buildSelectSQL() + " WHERE e.id_pedido = ?";
        try (Connection con = conexion.establecerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, Integer.parseInt(idStr));
            ResultSet rs = ps.executeQuery();
            while (rs.next()) datos.add(filaDesdeRS(rs));
            tablaEnvios.setItems(datos);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error al buscar: " + e.getMessage());
        }
    }

    private void cargarTabla() {
        ObservableList<EnvioRow> datos = FXCollections.observableArrayList();
        String sql = buildSelectSQL() + " ORDER BY e.id_envio DESC";
        try (Connection con = conexion.establecerConexion();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) datos.add(filaDesdeRS(rs));
            tablaEnvios.setItems(datos);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error al cargar envíos: " + e.getMessage());
        }
    }

    private String buildSelectSQL() {
        return "SELECT e.id_envio, e.id_pedido, e.direccion, e.costo_servicio, " +
                "CAST(e.id_metodo_envio AS VARCHAR) AS metodo_envio, e.observacion " +
                "FROM tbl_envio e";
    }

    private EnvioRow filaDesdeRS(ResultSet rs) throws SQLException {
        return new EnvioRow(
                rs.getInt("id_envio"),
                String.valueOf(rs.getInt("id_pedido")),
                rs.getString("direccion") != null ? rs.getString("direccion") : "",
                rs.getBigDecimal("costo_servicio") != null
                        ? rs.getBigDecimal("costo_servicio").toPlainString() : "",
                rs.getString("metodo_envio") != null ? rs.getString("metodo_envio") : "",
                rs.getString("observacion") != null ? rs.getString("observacion") : ""
        );
    }

    private void limpiar() {
        TXTidPedido.clear(); TXTdireccion.clear();
        TXTcostoServicio.clear(); TXTobservacion.clear();
        cmbMetodoEnvio.getSelectionModel().clearSelection();
        lblCliente.setText("Cliente: —");
    }

    public static class EnvioRow {
        final int idEnvio;
        final SimpleStringProperty idPedido, direccion, costo, metodoEnvio, observacion;

        public EnvioRow(int idEnvio, String idPedido, String direccion, String costo,
                        String metodoEnvio, String observacion) {
            this.idEnvio     = idEnvio;
            this.idPedido    = new SimpleStringProperty(idPedido);
            this.direccion   = new SimpleStringProperty(direccion);
            this.costo       = new SimpleStringProperty(costo);
            this.metodoEnvio = new SimpleStringProperty(metodoEnvio);
            this.observacion = new SimpleStringProperty(observacion);
        }
    }
}
