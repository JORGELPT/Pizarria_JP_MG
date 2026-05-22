package com.example.demo1.Controllers;

import com.example.demo1.Database.Conexion;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javax.swing.JOptionPane;
import java.sql.*;


public class CONTROLLER_Registro {

    Conexion conexion = new Conexion();

    @FXML private TextField TxtNombre;
    @FXML private TextField TxtTelefono;
    @FXML private TextField TxtCedula;
    @FXML private TextField TxtDireccion;

    @FXML
    public void registrarUsuario(ActionEvent actionEvent) {

        String nombre    = TxtNombre.getText().trim();
        String telefono  = TxtTelefono.getText().trim();
        String cedula    = TxtCedula.getText().trim();
        String direccion = TxtDireccion.getText().trim();

        if (nombre.isEmpty()) {
            JOptionPane.showMessageDialog(null, "El nombre es obligatorio.");
            return;
        }
        if (telefono.isEmpty()) {
            JOptionPane.showMessageDialog(null, "El teléfono es obligatorio.");
            return;
        }
        if (cedula.isEmpty()) {
            JOptionPane.showMessageDialog(null, "La cédula es obligatoria.");
            return;
        }

        Connection connection = null;
        try {
            connection = conexion.establecerConexion();
            connection.setAutoCommit(false);

            // 1) Insertar persona con rol_bd = 'cliente'
            // contrasenia inicial = cédula sin guiones
            String sqlPer = "INSERT INTO tbl_persona (nombre, tel, cedula, direccion, rol_bd, contrasenia) " +
                    "VALUES (?, ?, ?, ?, 'cliente', ?)";
            int idPersona;
            try (PreparedStatement ps = connection.prepareStatement(sqlPer, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, nombre);
                ps.setString(2, telefono);
                ps.setString(3, cedula);
                ps.setString(4, direccion.isEmpty() ? null : direccion);
                ps.setString(5, cedula.replace("-", ""));
                ps.executeUpdate();
                ResultSet keys = ps.getGeneratedKeys();
                idPersona = keys.next() ? keys.getInt(1) : 0;
            }

            // 2) Insertar en tbl_cliente
            try (PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO tbl_cliente (id_persona) VALUES (?)")) {
                ps.setInt(1, idPersona);
                ps.executeUpdate();
            }

            connection.commit();
            JOptionPane.showMessageDialog(null, "¡Registro Exitoso de Cliente!");
            limpiarCampos();

        } catch (Exception e) {
            try { if (connection != null) connection.rollback(); } catch (SQLException ex) { /* rollback silencioso */ }
            JOptionPane.showMessageDialog(null, "Error en registro: " + e.getMessage());
        } finally {
            try { if (connection != null) connection.close(); } catch (Exception ignore) {}
        }
    }

    @FXML
    public void limpiarCampos(ActionEvent actionEvent) {
        limpiarCampos();
    }

    public void limpiarCampos() {
        TxtNombre.clear();
        TxtTelefono.clear();
        TxtCedula.clear();
        TxtDireccion.clear();
    }
}
