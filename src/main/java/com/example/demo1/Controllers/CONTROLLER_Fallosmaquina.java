package com.example.demo1.Controllers;

import com.example.demo1.Database.Conexion;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import javax.swing.JOptionPane;
import java.sql.*;


public class CONTROLLER_Fallosmaquina {

    Conexion conexion = new Conexion();

    @FXML private ComboBox<String> cmbTipo;
    @FXML private TextField TXTserieMaquina;
    @FXML private Label     lblIdMaquina;
    @FXML private TextField TXTdescripcion;
    @FXML private TextField TXTidEmpleado;

    private int idMaquina = -1;
    private int idFallo   = -1;

    @FXML
    public void initialize() {
        cmbTipo.setItems(FXCollections.observableArrayList("Mecánico", "Eléctrico", "Software"));
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
    public void FnGuardar() {
        String tipo       = cmbTipo.getValue();
        String desc       = TXTdescripcion.getText().trim();
        String idEmpleado = TXTidEmpleado.getText().trim();

        if (idMaquina == -1 || tipo == null || desc.isEmpty() || idEmpleado.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Todos los campos son obligatorios. Busque primero la máquina.");
            return;
        }

        // Columna correcta: fecha_gen (no fecha)
        String sql = "INSERT INTO tbl_fallo (tipo, descripcion, fecha_gen, id_maquina, id_empleado) " +
                "VALUES (?, ?, GETDATE(), ?, ?)";

        try (Connection con = conexion.establecerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, tipo);
            ps.setString(2, desc);
            ps.setInt(3, idMaquina);
            ps.setInt(4, Integer.parseInt(idEmpleado));

            ps.executeUpdate();
            JOptionPane.showMessageDialog(null, "Fallo registrado correctamente.");
            limpiar();

        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error al guardar: " + e.getMessage());
        }
    }

    @FXML
    public void FnBuscar() {
        if (idMaquina == -1) {
            JOptionPane.showMessageDialog(null, "Busque primero la máquina con su serie.");
            return;
        }
        String sql = "SELECT TOP 1 id_fallo, tipo, descripcion, id_empleado " +
                "FROM tbl_fallo WHERE id_maquina = ? ORDER BY id_fallo DESC";
        try (Connection con = conexion.establecerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idMaquina);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                idFallo = rs.getInt("id_fallo");
                cmbTipo.setValue(rs.getString("tipo"));
                TXTdescripcion.setText(rs.getString("descripcion") != null
                        ? rs.getString("descripcion") : "");
                TXTidEmpleado.setText(String.valueOf(rs.getInt("id_empleado")));
                lblIdMaquina.setText("ID máquina: " + idMaquina + " | ID fallo: " + idFallo);
                JOptionPane.showMessageDialog(null, "Fallo #" + idFallo + " cargado.");
            } else {
                JOptionPane.showMessageDialog(null, "No hay fallos registrados para esta máquina.");
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error al buscar: " + e.getMessage());
        }
    }

    @FXML
    public void FnEditar() {
        if (idFallo == -1) {
            JOptionPane.showMessageDialog(null, "Use el botón 🔍 para cargar el fallo a editar.");
            return;
        }
        String tipo       = cmbTipo.getValue();
        String desc       = TXTdescripcion.getText().trim();
        String idEmpleado = TXTidEmpleado.getText().trim();

        if (tipo == null || desc.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Tipo y descripción son obligatorios.");
            return;
        }

        String sql = "UPDATE tbl_fallo SET tipo=?, descripcion=?" +
                (idEmpleado.isEmpty() ? "" : ", id_empleado=?") + " WHERE id_fallo=?";
        try (Connection con = conexion.establecerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, tipo);
            ps.setString(2, desc);
            if (idEmpleado.isEmpty()) {
                ps.setInt(3, idFallo);
            } else {
                ps.setInt(3, Integer.parseInt(idEmpleado));
                ps.setInt(4, idFallo);
            }
            ps.executeUpdate();
            JOptionPane.showMessageDialog(null, "Fallo actualizado correctamente.");
            idFallo = -1;
            limpiar();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error al actualizar: " + e.getMessage());
        }
    }

    @FXML
    public void FnEliminar() {
        if (!com.example.demo1.Utils.Permisos_Util.verificarEliminar()) return;
        if (idFallo == -1) {
            JOptionPane.showMessageDialog(null, "Use el botón 🔍 para cargar el fallo a eliminar.");
            return;
        }
        int confirmar = JOptionPane.showConfirmDialog(null,
                "¿Eliminar el fallo #" + idFallo + "?",
                "Confirmar", JOptionPane.YES_NO_OPTION);
        if (confirmar != JOptionPane.YES_OPTION) return;

        try (Connection con = conexion.establecerConexion();
             PreparedStatement ps = con.prepareStatement(
                     "DELETE FROM tbl_fallo WHERE id_fallo = ?")) {
            ps.setInt(1, idFallo);
            ps.executeUpdate();
            JOptionPane.showMessageDialog(null, "Fallo eliminado correctamente.");
            idFallo = -1;
            limpiar();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error al eliminar: " + e.getMessage());
        }
    }

    @FXML
    public void FnInhabilitar() {
        if (idFallo == -1) {
            JOptionPane.showMessageDialog(null, "Use el botón 🔍 para cargar el fallo primero.");
            return;
        }
        int confirmar = JOptionPane.showConfirmDialog(null,
                "¿Inhabilitar el fallo #" + idFallo + "? No se eliminará, solo quedará inactivo.",
                "Confirmar", JOptionPane.YES_NO_OPTION);
        if (confirmar != JOptionPane.YES_OPTION) return;
        try (java.sql.Connection con = Conexion.establecerConexion();
             java.sql.PreparedStatement ps = con.prepareStatement(
                     "UPDATE tbl_fallo SET estado = 'Inactivo' WHERE id_fallo = ?")) {
            ps.setInt(1, idFallo);
            ps.executeUpdate();
            JOptionPane.showMessageDialog(null, "Fallo inhabilitado correctamente.");
            idFallo = -1;
            limpiar();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error al inhabilitar: " + e.getMessage());
        }
    }

    @FXML
    public void FnHabilitar() {
        if (idFallo == -1) {
            JOptionPane.showMessageDialog(null, "Use el botón 🔍 para cargar el fallo a habilitar.");
            return;
        }
        try (java.sql.Connection con = conexion.establecerConexion();
             java.sql.PreparedStatement ps = con.prepareStatement(
                     "UPDATE tbl_fallo SET estado = 'Activo' WHERE id_fallo = ?")) {
            ps.setInt(1, idFallo);
            ps.executeUpdate();
            JOptionPane.showMessageDialog(null, "Fallo habilitado correctamente.");
            idFallo = -1;
            limpiar();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error al habilitar: " + e.getMessage());
        }
    }

    public void limpiar() {
        cmbTipo.getSelectionModel().clearSelection();
        TXTserieMaquina.clear();
        TXTdescripcion.clear();
        TXTidEmpleado.clear();
        lblIdMaquina.setText("ID máquina: —");
        idMaquina = -1;
    }
}
