package com.example.demo1.Controllers;

import com.example.demo1.Database.Conexion;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import javax.swing.JOptionPane;
import java.sql.*;

/**
 * Controller de Mantenimiento.
 *
 * tbl_mantenimiento: id_mantenimiento (PK IDENTITY), fecha_mantenimiento, costo, id_maquina, descripcion
 * tbl_pago_mantenimiento: cantidad (PK IDENTITY), metodo_pago, id_tecnico, id_mantenimiento, fecha, monto
 * tbl_tecnico: id_tecnico (PK), nombre_tecnico, especialidad_tecnico, tel, cedula
 */
public class CONTROLLER_Mantenimiento {

    Conexion conexion = new Conexion();

    @FXML private TextField TXTcosto;
    @FXML private ComboBox<String> cmbMetodoPago;
    @FXML private TextField TXTserieMaquina;
    @FXML private Label     lblIdMaquina;
    @FXML private TextField TXTcedulaTecnico;
    @FXML private Label     lblIdTecnico;
    @FXML private TextArea  TXTdescripcion;

    private int idMaquina        = -1;
    private int idTecnico        = -1;
    private int idMantenimiento  = -1;

    @FXML
    public void initialize() {
        cmbMetodoPago.setItems(FXCollections.observableArrayList(
                "Efectivo", "Tarjeta", "Transferencia", "Cheque"));
    }

    @FXML
    public void FnBuscarMaquina() {
        String serie = TXTserieMaquina.getText().trim();
        if (serie.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Ingrese la serie de la máquina.");
            return;
        }

        try (Connection con = conexion.establecerConexion();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT id_maquina FROM tbl_maquina WHERE serie = ?")) {
            ps.setString(1, serie);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                idMaquina = rs.getInt("id_maquina");
                lblIdMaquina.setText("ID máquina: " + idMaquina);
            } else {
                idMaquina = -1;
                lblIdMaquina.setText("ID máquina: — (no encontrada)");
                JOptionPane.showMessageDialog(null, "No se encontró máquina con esa serie.");
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error: " + e.getMessage());
        }
    }

    @FXML
    public void FnBuscarTecnico() {
        String cedula = TXTcedulaTecnico.getText().trim();
        if (cedula.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Ingrese la cédula del técnico.");
            return;
        }

        // tbl_tecnico tiene su propia columna cedula, sin JOIN a tbl_persona
        try (Connection con = conexion.establecerConexion();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT id_tecnico FROM tbl_tecnico WHERE cedula = ?")) {
            ps.setString(1, cedula);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                idTecnico = rs.getInt("id_tecnico");
                lblIdTecnico.setText("ID técnico: " + idTecnico);
            } else {
                idTecnico = -1;
                lblIdTecnico.setText("ID técnico: — (no encontrado)");
                JOptionPane.showMessageDialog(null, "No se encontró técnico con esa cédula.");
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error: " + e.getMessage());
        }
    }

    @FXML
    public void FnGuardar() {
        String costo  = TXTcosto.getText().trim();
        String metodo = cmbMetodoPago.getValue();
        String desc   = TXTdescripcion.getText().trim();

        if (idMaquina == -1 || idTecnico == -1) {
            JOptionPane.showMessageDialog(null, "Primero busque la máquina y el técnico.");
            return;
        }
        if (costo.isEmpty() || metodo == null) {
            JOptionPane.showMessageDialog(null, "Costo y método de pago son obligatorios.");
            return;
        }

        Connection con = null;
        try {
            con = conexion.establecerConexion();
            con.setAutoCommit(false);

            // 1) Insertar en tbl_mantenimiento
            // Columnas: fecha_mantenimiento, costo, id_maquina, descripcion
            String sqlMan = "INSERT INTO tbl_mantenimiento (fecha_mantenimiento, costo, id_maquina, descripcion) " +
                    "VALUES (GETDATE(), ?, ?, ?)";
            int idMantenimiento;
            try (PreparedStatement ps = con.prepareStatement(sqlMan, Statement.RETURN_GENERATED_KEYS)) {
                ps.setBigDecimal(1, new java.math.BigDecimal(costo));
                ps.setInt(2, idMaquina);
                ps.setString(3, desc);
                ps.executeUpdate();
                ResultSet keys = ps.getGeneratedKeys();
                idMantenimiento = keys.next() ? keys.getInt(1) : 0;
            }

            // 2) Insertar en tbl_pago_mantenimiento
            // Columnas: metodo_pago, id_tecnico, id_mantenimiento, fecha, monto
            // NO insertar 'cantidad' (es PK IDENTITY)
            String sqlPago = "INSERT INTO tbl_pago_mantenimiento (metodo_pago, id_tecnico, id_mantenimiento, fecha, monto) " +
                    "VALUES (?, ?, ?, GETDATE(), ?)";
            try (PreparedStatement ps2 = con.prepareStatement(sqlPago)) {
                ps2.setString(1, metodo);
                ps2.setInt(2, idTecnico);
                ps2.setInt(3, idMantenimiento);
                ps2.setBigDecimal(4, new java.math.BigDecimal(costo));
                ps2.executeUpdate();
            }

            con.commit();
            JOptionPane.showMessageDialog(null, "Mantenimiento registrado correctamente.");
            limpiar();

        } catch (Exception e) {
            try { if (con != null) con.rollback(); } catch (Exception ignore) {}
            JOptionPane.showMessageDialog(null, "Error al guardar: " + e.getMessage());
        } finally {
            try { if (con != null) con.close(); } catch (Exception ignore) {}
        }
    }

    @FXML
    public void FnBuscar() {
        if (idMaquina == -1) {
            JOptionPane.showMessageDialog(null, "Busque primero la máquina con su serie.");
            return;
        }
        String sql = "SELECT TOP 1 id_mantenimiento, costo, descripcion " +
                "FROM tbl_mantenimiento WHERE id_maquina = ? ORDER BY id_mantenimiento DESC";
        try (Connection con = conexion.establecerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idMaquina);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                idMantenimiento = rs.getInt("id_mantenimiento");
                TXTcosto.setText(rs.getBigDecimal("costo") != null
                        ? rs.getBigDecimal("costo").toPlainString() : "");
                TXTdescripcion.setText(rs.getString("descripcion") != null
                        ? rs.getString("descripcion") : "");
                lblIdMaquina.setText("ID máquina: " + idMaquina + " | ID mant: " + idMantenimiento);
                JOptionPane.showMessageDialog(null, "Mantenimiento #" + idMantenimiento + " cargado.");
            } else {
                JOptionPane.showMessageDialog(null, "No hay mantenimientos registrados para esta máquina.");
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error al buscar: " + e.getMessage());
        }
    }

    @FXML
    public void FnEditar() {
        if (idMantenimiento == -1) {
            JOptionPane.showMessageDialog(null, "Use el botón 🔍 para buscar el mantenimiento a editar.");
            return;
        }
        String costo  = TXTcosto.getText().trim();
        String metodo = cmbMetodoPago.getValue();
        String desc   = TXTdescripcion.getText().trim();

        if (costo.isEmpty()) {
            JOptionPane.showMessageDialog(null, "El costo es obligatorio.");
            return;
        }

        Connection con = null;
        try {
            con = conexion.establecerConexion();
            con.setAutoCommit(false);

            try (PreparedStatement ps = con.prepareStatement(
                    "UPDATE tbl_mantenimiento SET costo=?, descripcion=? WHERE id_mantenimiento=?")) {
                ps.setBigDecimal(1, new java.math.BigDecimal(costo));
                ps.setString(2, desc);
                ps.setInt(3, idMantenimiento);
                ps.executeUpdate();
            }

            if (metodo != null && idTecnico != -1) {
                try (PreparedStatement ps2 = con.prepareStatement(
                        "UPDATE tbl_pago_mantenimiento SET metodo_pago=?, monto=? WHERE id_mantenimiento=?")) {
                    ps2.setString(1, metodo);
                    ps2.setBigDecimal(2, new java.math.BigDecimal(costo));
                    ps2.setInt(3, idMantenimiento);
                    ps2.executeUpdate();
                }
            }

            con.commit();
            JOptionPane.showMessageDialog(null, "Mantenimiento actualizado correctamente.");
            idMantenimiento = -1;
            limpiar();
        } catch (Exception e) {
            try { if (con != null) con.rollback(); } catch (Exception ignore) {}
            JOptionPane.showMessageDialog(null, "Error al actualizar: " + e.getMessage());
        } finally {
            try { if (con != null) con.close(); } catch (Exception ignore) {}
        }
    }

    @FXML
    public void FnEliminar() {
        if (!com.example.demo1.Utils.Permisos_Util.verificarEliminar()) return;
        if (idMantenimiento == -1) {
            JOptionPane.showMessageDialog(null, "Use el botón 🔍 para buscar el mantenimiento a eliminar.");
            return;
        }
        int confirmar = JOptionPane.showConfirmDialog(null,
                "¿Eliminar el mantenimiento #" + idMantenimiento + "?",
                "Confirmar", JOptionPane.YES_NO_OPTION);
        if (confirmar != JOptionPane.YES_OPTION) return;

        Connection con = null;
        try {
            con = conexion.establecerConexion();
            con.setAutoCommit(false);
            try (PreparedStatement ps = con.prepareStatement(
                    "DELETE FROM tbl_pago_mantenimiento WHERE id_mantenimiento = ?")) {
                ps.setInt(1, idMantenimiento);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = con.prepareStatement(
                    "DELETE FROM tbl_mantenimiento WHERE id_mantenimiento = ?")) {
                ps.setInt(1, idMantenimiento);
                ps.executeUpdate();
            }
            con.commit();
            JOptionPane.showMessageDialog(null, "Mantenimiento eliminado correctamente.");
            idMantenimiento = -1;
            limpiar();
        } catch (Exception e) {
            try { if (con != null) con.rollback(); } catch (Exception ignore) {}
            JOptionPane.showMessageDialog(null, "Error al eliminar: " + e.getMessage());
        } finally {
            try { if (con != null) con.close(); } catch (Exception ignore) {}
        }
    }

    @FXML
    public void FnInhabilitar() {
        if (idMantenimiento == -1) {
            JOptionPane.showMessageDialog(null, "Use el botón 🔍 para buscar el mantenimiento primero.");
            return;
        }
        int confirmar = JOptionPane.showConfirmDialog(null,
                "¿Inhabilitar el mantenimiento #" + idMantenimiento + "? No se eliminará, solo quedará inactivo.",
                "Confirmar", JOptionPane.YES_NO_OPTION);
        if (confirmar != JOptionPane.YES_OPTION) return;
        try (java.sql.Connection con = Conexion.establecerConexion();
             java.sql.PreparedStatement ps = con.prepareStatement(
                     "UPDATE tbl_mantenimiento SET estado = 'Inactivo' WHERE id_mantenimiento = ?")) {
            ps.setInt(1, idMantenimiento);
            ps.executeUpdate();
            JOptionPane.showMessageDialog(null, "Mantenimiento inhabilitado correctamente.");
            idMantenimiento = -1;
            limpiar();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error al inhabilitar: " + e.getMessage());
        }
    }

    @FXML
    public void FnHabilitar() {
        if (idMantenimiento == -1) {
            JOptionPane.showMessageDialog(null, "Use el botón 🔍 para buscar el mantenimiento a habilitar.");
            return;
        }
        try (java.sql.Connection con = conexion.establecerConexion();
             java.sql.PreparedStatement ps = con.prepareStatement(
                     "UPDATE tbl_mantenimiento SET estado = 'Activo' WHERE id_mantenimiento = ?")) {
            ps.setInt(1, idMantenimiento);
            ps.executeUpdate();
            JOptionPane.showMessageDialog(null, "Mantenimiento habilitado correctamente.");
            idMantenimiento = -1;
            limpiar();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error al habilitar: " + e.getMessage());
        }
    }

    public void limpiar() {
        TXTcosto.clear();
        cmbMetodoPago.getSelectionModel().clearSelection();
        TXTserieMaquina.clear();
        TXTcedulaTecnico.clear();
        TXTdescripcion.clear();
        lblIdMaquina.setText("ID máquina: —");
        lblIdTecnico.setText("ID técnico: —");
        idMaquina = -1;
        idTecnico = -1;
    }
}
