package com.example.demo1.Controllers;

import com.example.demo1.Database.Conexion;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import javax.swing.JOptionPane;
import java.sql.*;


public class CONTROLLER_Aperturacaja {

    Conexion conexion = new Conexion();

    @FXML private TextField TXTmonto;
    @FXML private TextField TXTempleado;

    @FXML private TableView<AperturaRow>           tablaAperturas;
    @FXML private TableColumn<AperturaRow, String> colIdApertura;
    @FXML private TableColumn<AperturaRow, String> colFechaApert;
    @FXML private TableColumn<AperturaRow, String> colMonto;
    @FXML private TableColumn<AperturaRow, String> colIdEmpleado;

    private int idAperturaSeleccionada = -1;

    @FXML
    public void initialize() {
        colIdApertura.setCellValueFactory(c -> c.getValue().idApertura);
        colFechaApert.setCellValueFactory(c -> c.getValue().fecha);
        colMonto.setCellValueFactory(c      -> c.getValue().monto);
        colIdEmpleado.setCellValueFactory(c -> c.getValue().empleado);

        tablaAperturas.getSelectionModel().selectedItemProperty().addListener(
                (obs, old, sel) -> {
                    if (sel == null) return;
                    idAperturaSeleccionada = Integer.parseInt(sel.idApertura.get());
                    TXTmonto.setText(sel.monto.get());
                    TXTempleado.setText(sel.empleado.get());
                });

        cargarTabla();
    }

    @FXML
    public void FnGuardar() {
        String monto      = TXTmonto.getText().trim();
        String idEmpleado = TXTempleado.getText().trim();

        if (monto.isEmpty() || idEmpleado.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Todos los campos son obligatorios.");
            return;
        }

        int idEmp;
        try {
            idEmp = Integer.parseInt(idEmpleado);
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(null, "El ID de empleado debe ser un número.");
            return;
        }

        String sql = "INSERT INTO tbl_apertura_caja (fecha_apertura, monto_inicial, id_empleado) VALUES (GETDATE(), ?, ?)";
        try (Connection con = conexion.establecerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setBigDecimal(1, new java.math.BigDecimal(monto));
            ps.setInt(2, idEmp);
            ps.executeUpdate();
            JOptionPane.showMessageDialog(null, "Apertura de caja registrada correctamente.");
            limpiar();
            cargarTabla();
        } catch (Exception e) { //error al registrar apertura de caja - pantalla Apertura de Caja
            JOptionPane.showMessageDialog(null, "Error al registrar apertura: " + e.getMessage());
        }
    }

    @FXML
    public void FnEditar() {
        if (idAperturaSeleccionada == -1) {
            JOptionPane.showMessageDialog(null, "Seleccione una apertura de la tabla primero.");
            return;
        }
        String monto      = TXTmonto.getText().trim();
        String idEmpleado = TXTempleado.getText().trim();

        if (monto.isEmpty() || idEmpleado.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Todos los campos son obligatorios.");
            return;
        }

        int idEmp;
        try {
            idEmp = Integer.parseInt(idEmpleado);
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(null, "El ID de empleado debe ser un número.");
            return;
        }

        String sql = "UPDATE tbl_apertura_caja SET monto_inicial=?, id_empleado=? WHERE id_apertura_caja=?";
        try (Connection con = conexion.establecerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setBigDecimal(1, new java.math.BigDecimal(monto));
            ps.setInt(2, idEmp);
            ps.setInt(3, idAperturaSeleccionada);
            ps.executeUpdate();
            JOptionPane.showMessageDialog(null, "Apertura actualizada correctamente.");
            idAperturaSeleccionada = -1;
            limpiar();
            cargarTabla();
        } catch (Exception e) { //error al actualizar apertura de caja - pantalla Apertura de Caja
            JOptionPane.showMessageDialog(null, "Error al actualizar: " + e.getMessage());
        }
    }

    @FXML
    public void FnEliminar() {
        if (!com.example.demo1.Utils.Permisos_Util.verificarEliminar()) return;
        if (idAperturaSeleccionada == -1) {
            JOptionPane.showMessageDialog(null, "Seleccione una apertura de la tabla primero.");
            return;
        }
        int confirmar = JOptionPane.showConfirmDialog(null,
                "¿Está seguro de eliminar esta apertura de caja?",
                "Confirmar eliminación", JOptionPane.YES_NO_OPTION);
        if (confirmar != JOptionPane.YES_OPTION) return;

        String sql = "DELETE FROM tbl_apertura_caja WHERE id_apertura_caja = ?";
        try (Connection con = conexion.establecerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idAperturaSeleccionada);
            ps.executeUpdate();
            JOptionPane.showMessageDialog(null, "Apertura eliminada correctamente.");
            idAperturaSeleccionada = -1;
            limpiar();
            cargarTabla();
        } catch (Exception e) { //error al eliminar apertura de caja - pantalla Apertura de Caja
            JOptionPane.showMessageDialog(null, "Error al eliminar: " + e.getMessage());
        }
    }

    private void cargarTabla() {
        ObservableList<AperturaRow> datos = FXCollections.observableArrayList();
        String sql = "SELECT a.id_apertura_caja, a.fecha_apertura, a.monto_inicial, " +
                "CAST(a.id_empleado AS VARCHAR) AS id_empleado " +
                "FROM tbl_apertura_caja a ORDER BY a.fecha_apertura DESC";
        try (Connection con = conexion.establecerConexion();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                datos.add(new AperturaRow(
                        String.valueOf(rs.getInt("id_apertura_caja")),
                        rs.getTimestamp("fecha_apertura") != null ? rs.getTimestamp("fecha_apertura").toString() : "",
                        rs.getBigDecimal("monto_inicial") != null ? rs.getBigDecimal("monto_inicial").toPlainString() : "",
                        rs.getString("id_empleado") != null ? rs.getString("id_empleado") : ""
                ));
            }
            tablaAperturas.setItems(datos);
        } catch (Exception e) { //error al cargar tabla de aperturas - pantalla Apertura de Caja
            JOptionPane.showMessageDialog(null, "Error al cargar aperturas: " + e.getMessage());
        }
    }

    public void limpiar() {
        TXTmonto.clear();
        TXTempleado.clear();
    }

    public static class AperturaRow {
        final SimpleStringProperty idApertura, fecha, monto, empleado;

        public AperturaRow(String idApertura, String fecha, String monto, String empleado) {
            this.idApertura = new SimpleStringProperty(idApertura);
            this.fecha      = new SimpleStringProperty(fecha);
            this.monto      = new SimpleStringProperty(monto);
            this.empleado   = new SimpleStringProperty(empleado);
        }
    }
}
