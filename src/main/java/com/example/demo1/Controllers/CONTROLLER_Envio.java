package com.example.demo1.Controllers;

import com.example.demo1.Database.Conexion;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javax.swing.JOptionPane;
import java.sql.*;

public class CONTROLLER_Envio {

    Conexion conexion = new Conexion();

    @FXML private TextField TXTidPedido;
    @FXML private TextField TXTdireccion;
    @FXML private Label     lblCliente;

    @FXML private TableView<EnvioRow>           tablaEnvios;
    @FXML private TableColumn<EnvioRow, String> colIdPedido;
    @FXML private TableColumn<EnvioRow, String> colCliente;
    @FXML private TableColumn<EnvioRow, String> colDireccion;
    @FXML private TableColumn<EnvioRow, String> colMetodoEnvio;
    @FXML private TableColumn<EnvioRow, String> colEstado;

    private int idEnvioSeleccionado = -1;

    @FXML
    public void initialize() {
        colIdPedido.setCellValueFactory(c    -> c.getValue().idPedido);
        colCliente.setCellValueFactory(c     -> c.getValue().cliente);
        colDireccion.setCellValueFactory(c   -> c.getValue().direccion);
        colMetodoEnvio.setCellValueFactory(c -> c.getValue().metodoEnvio);
        colEstado.setCellValueFactory(c      -> c.getValue().estado);

        tablaEnvios.getSelectionModel().selectedItemProperty().addListener(
                (obs, old, sel) -> {
                    if (sel == null) return;
                    idEnvioSeleccionado = sel.idEnvio;
                    TXTidPedido.setText(sel.idPedido.get());
                    TXTdireccion.setText(sel.direccion.get());
                    lblCliente.setText("Cliente: " + sel.cliente.get());
                });

        cargarTabla();
    }

    @FXML
    public void FnActualizarDireccion() {
        if (idEnvioSeleccionado == -1) {
            JOptionPane.showMessageDialog(null, "Seleccione un envío de la tabla primero.");
            return;
        }
        String nuevaDireccion = TXTdireccion.getText().trim();
        if (nuevaDireccion.isEmpty()) {
            JOptionPane.showMessageDialog(null, "La dirección no puede estar vacía.");
            return;
        }
        try (Connection con = conexion.establecerConexion();
             PreparedStatement ps = con.prepareStatement(
                     "UPDATE tbl_envio SET direccion = ? WHERE id_envio = ?")) {
            ps.setString(1, nuevaDireccion);
            ps.setInt(2, idEnvioSeleccionado);
            ps.executeUpdate();
            JOptionPane.showMessageDialog(null, "Dirección actualizada correctamente.");
            idEnvioSeleccionado = -1;
            limpiar();
            cargarTabla();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error al actualizar: " + e.getMessage());
        }
    }

    @FXML
    public void FnEliminar() {
        if (!com.example.demo1.Utils.Permisos_Util.verificarEliminar()) return;
        if (idEnvioSeleccionado == -1) {
            JOptionPane.showMessageDialog(null, "Seleccione un envío de la tabla primero.");
            return;
        }
        int confirmar = JOptionPane.showConfirmDialog(null,
                "¿Está seguro de eliminar este envío?",
                "Confirmar eliminación", JOptionPane.YES_NO_OPTION);
        if (confirmar != JOptionPane.YES_OPTION) return;

        try (Connection con = conexion.establecerConexion();
             PreparedStatement ps = con.prepareStatement(
                     "DELETE FROM tbl_envio WHERE id_envio = ?")) {
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
    public void FnInhabilitar() {
        if (idEnvioSeleccionado == -1) {
            JOptionPane.showMessageDialog(null, "Seleccione un envío de la tabla primero.");
            return;
        }
        int confirmar = JOptionPane.showConfirmDialog(null,
                "¿Inhabilitar este envío? No se eliminará, solo quedará inactivo.",
                "Confirmar", JOptionPane.YES_NO_OPTION);
        if (confirmar != JOptionPane.YES_OPTION) return;
        try (Connection con = conexion.establecerConexion();
             PreparedStatement ps = con.prepareStatement(
                     "UPDATE tbl_envio SET estado = 'Inactivo' WHERE id_envio = ?")) {
            ps.setInt(1, idEnvioSeleccionado);
            ps.executeUpdate();
            JOptionPane.showMessageDialog(null, "Envío inhabilitado correctamente.");
            idEnvioSeleccionado = -1;
            limpiar();
            cargarTabla();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error al inhabilitar: " + e.getMessage());
        }
    }

    @FXML
    public void FnHabilitar() {
        if (idEnvioSeleccionado == -1) {
            JOptionPane.showMessageDialog(null, "Seleccione un envío de la tabla primero.");
            return;
        }
        try (Connection con = conexion.establecerConexion();
             PreparedStatement ps = con.prepareStatement(
                     "UPDATE tbl_envio SET estado = 'Activo' WHERE id_envio = ?")) {
            ps.setInt(1, idEnvioSeleccionado);
            ps.executeUpdate();
            JOptionPane.showMessageDialog(null, "Envío habilitado correctamente.");
            idEnvioSeleccionado = -1;
            cargarTabla();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error al habilitar: " + e.getMessage());
        }
    }

    @FXML
    public void FnBuscar() {
        String idStr = TXTidPedido.getText().trim();
        if (idStr.isEmpty()) { cargarTabla(); return; }

        ObservableList<EnvioRow> datos = FXCollections.observableArrayList();
        try (Connection con = conexion.establecerConexion();
             PreparedStatement ps = con.prepareStatement(
                     buildSelectSQL() + " WHERE e.id_pedido = ?")) {
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
        try (Connection con = conexion.establecerConexion();
             PreparedStatement ps = con.prepareStatement(
                     buildSelectSQL() + " ORDER BY e.id_envio DESC");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) datos.add(filaDesdeRS(rs));
            tablaEnvios.setItems(datos);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error al cargar envíos: " + e.getMessage());
        }
    }

    private String buildSelectSQL() {
        return "SELECT e.id_envio, e.id_pedido, e.direccion, " +
                "CAST(e.id_metodo_envio AS VARCHAR) AS metodo_envio, " +
                "ISNULL(e.estado, 'Activo') AS estado, " +
                "ISNULL(per.nombre, '—') AS nombre_cliente " +
                "FROM tbl_envio e " +
                "LEFT JOIN tbl_pedido  ped ON ped.id_pedido  = e.id_pedido " +
                "LEFT JOIN tbl_cliente cli ON cli.id_cliente = ped.id_cliente " +
                "LEFT JOIN tbl_persona per ON per.id_persona = cli.id_persona";
    }

    private EnvioRow filaDesdeRS(ResultSet rs) throws SQLException {
        return new EnvioRow(
                rs.getInt("id_envio"),
                String.valueOf(rs.getInt("id_pedido")),
                rs.getString("nombre_cliente") != null ? rs.getString("nombre_cliente") : "—",
                rs.getString("direccion")      != null ? rs.getString("direccion")      : "",
                rs.getString("metodo_envio")   != null ? rs.getString("metodo_envio")   : "",
                rs.getString("estado")         != null ? rs.getString("estado")         : "Activo"
        );
    }

    private void limpiar() {
        TXTidPedido.clear();
        TXTdireccion.clear();
        lblCliente.setText("Cliente: —");
    }

    public static class EnvioRow {
        final int idEnvio;
        final SimpleStringProperty idPedido, cliente, direccion, metodoEnvio, estado;

        public EnvioRow(int idEnvio, String idPedido, String cliente,
                        String direccion, String metodoEnvio, String estado) {
            this.idEnvio     = idEnvio;
            this.idPedido    = new SimpleStringProperty(idPedido);
            this.cliente     = new SimpleStringProperty(cliente);
            this.direccion   = new SimpleStringProperty(direccion);
            this.metodoEnvio = new SimpleStringProperty(metodoEnvio);
            this.estado      = new SimpleStringProperty(estado);
        }
    }
}
