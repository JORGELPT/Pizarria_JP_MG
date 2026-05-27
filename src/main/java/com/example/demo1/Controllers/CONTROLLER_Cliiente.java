package com.example.demo1.Controllers;

import com.example.demo1.Database.Conexion;
import com.example.demo1.Utils.JasperUtil;
import com.example.demo1.Utils.Permisos_Util;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javax.swing.JOptionPane;
import java.sql.*;

public class CONTROLLER_Cliiente {

    Conexion conexion = new Conexion();

    @FXML private TextField TXTnombre;
    @FXML private TextField TXTcedula;
    @FXML private TextField TXTtelefono;
    @FXML private TextField TXTdireccion;
    @FXML private TableView<ClienteRow> tablaClientes;
    @FXML private TableColumn<ClienteRow, String> colNombre;
    @FXML private TableColumn<ClienteRow, String> colCedula;
    @FXML private TableColumn<ClienteRow, String> colTelefono;
    @FXML private TableColumn<ClienteRow, String> colEstado;

    @FXML
    public void initialize() {
        colNombre.setCellValueFactory(c -> c.getValue().nombre);
        colCedula.setCellValueFactory(c -> c.getValue().cedula);
        colTelefono.setCellValueFactory(c -> c.getValue().telefono);
        colEstado.setCellValueFactory(c -> c.getValue().estado);

        tablaClientes.getSelectionModel().selectedItemProperty().addListener(
                (obs, old, sel) -> {
                    if (sel != null) {
                        TXTnombre.setText(sel.nombre.get());
                        TXTcedula.setText(sel.cedula.get());
                        TXTtelefono.setText(sel.telefono.get());
                    }
                });

        cargarTabla();
    }

    @FXML
    public void FnGuardar() {
        if (!Permisos_Util.verificarInsertar()) return;

        String nombre    = TXTnombre.getText().trim();
        String cedula    = TXTcedula.getText().trim();
        String telefono  = TXTtelefono.getText().trim();
        String direccion = TXTdireccion.getText().trim();

        if (nombre.isEmpty() || cedula.isEmpty() || telefono.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Nombre, cédula y teléfono son obligatorios.");
            return;
        }

        Connection con = null;
        try {
            con = conexion.establecerConexion();
            con.setAutoCommit(false);

            // 1) Insertar en tbl_persona con rol_bd = 'cliente'
            String sqlP = "INSERT INTO tbl_persona (nombre, tel, cedula, direccion, rol_bd, contrasenia) " +
                    "VALUES (?, ?, ?, ?, 'cliente', ?)";
            int idPersona;
            try (PreparedStatement ps = con.prepareStatement(sqlP, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, nombre);
                ps.setString(2, telefono);
                ps.setString(3, cedula);
                ps.setString(4, direccion);
                // Contraseña por defecto = cédula (el cliente puede cambiarla después)
                ps.setString(5, cedula.replace("-", ""));
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    idPersona = keys.next() ? keys.getInt(1) : 0;
                }
            }

            try (PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO tbl_cliente (id_persona) VALUES (?)")) {
                ps.setInt(1, idPersona);
                ps.executeUpdate();
            }

            con.commit();
            JOptionPane.showMessageDialog(null,
                    "Cliente guardado correctamente.\n" +
                    "Usuario: " + cedula + "\n" +
                    "Contraseña inicial: " + cedula.replace("-", ""));
            limpiar();
            cargarTabla();

        } catch (Exception e) { //error al guardar cliente - pantalla Agregar Cliente
            try {
                if (con != null) con.rollback();
            } catch (Exception ignore) {}
            JOptionPane.showMessageDialog(null, "Error: " + e.getMessage());
        } finally {
            try {
                if (con != null) con.close();
            } catch (Exception ignore) {}
        }
    }

    @FXML
    public void FnBuscar() {
        if (!Permisos_Util.verificarBuscar()) return;

        String cedula = TXTcedula.getText().trim();
        if (cedula.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Ingrese una cédula para buscar.");
            return;
        }

        String sql = "SELECT p.nombre, p.tel, p.cedula, p.direccion " +
                "FROM tbl_cliente c INNER JOIN tbl_persona p ON c.id_persona = p.id_persona " +
                "WHERE p.cedula = ?";

        try (Connection con = conexion.establecerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, cedula);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    TXTnombre.setText(rs.getString("nombre"));
                    TXTtelefono.setText(rs.getString("tel"));
                    TXTcedula.setText(rs.getString("cedula"));
                    TXTdireccion.setText(rs.getString("direccion"));
                } else {
                    JOptionPane.showMessageDialog(null, "No se encontró el cliente.");
                }
            }

        } catch (Exception e) { //error al buscar cliente - pantalla Agregar Cliente
            JOptionPane.showMessageDialog(null, "Error: " + e.getMessage());
        }
    }

    @FXML
    public void FnEditar() {
        if (!Permisos_Util.verificarEditar()) return;

        String cedula = TXTcedula.getText().trim();
        if (cedula.isEmpty()) {
            JOptionPane.showMessageDialog(null, "La cédula es obligatoria para editar.");
            return;
        }

        String sql = "UPDATE tbl_persona SET nombre = ?, tel = ?, direccion = ? WHERE cedula = ?";

        try (Connection con = conexion.establecerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, TXTnombre.getText().trim());
            ps.setString(2, TXTtelefono.getText().trim());
            ps.setString(3, TXTdireccion.getText().trim());
            ps.setString(4, cedula);
            if (ps.executeUpdate() > 0) {
                JOptionPane.showMessageDialog(null, "Cliente actualizado.");
                cargarTabla();
            } else {
                JOptionPane.showMessageDialog(null, "No se encontró el cliente.");
            }

        } catch (Exception e) { //error al editar cliente - pantalla Agregar Cliente
            JOptionPane.showMessageDialog(null, "Error: " + e.getMessage());
        }
    }

    @FXML
    public void FnEliminar() {
        if (!Permisos_Util.verificarEliminar()) return;

        String cedula = TXTcedula.getText().trim();
        if (cedula.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Ingrese la cédula a eliminar.");
            return;
        }

        int c = JOptionPane.showConfirmDialog(null, "¿Eliminar cliente?",
                "Confirmar", JOptionPane.YES_NO_OPTION);
        if (c != JOptionPane.YES_OPTION) return;

        // Elimina en cascada: primero tbl_cliente, luego tbl_persona
        Connection con = null;
        try {
            con = conexion.establecerConexion();
            con.setAutoCommit(false);

            int idPersona;
            try (PreparedStatement ps = con.prepareStatement(
                    "SELECT id_persona FROM tbl_persona WHERE cedula = ?")) {
                ps.setString(1, cedula);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        JOptionPane.showMessageDialog(null, "No se encontró el cliente.");
                        return;
                    }
                    idPersona = rs.getInt("id_persona");
                }
            }

            try (PreparedStatement ps = con.prepareStatement(
                    "DELETE FROM tbl_cliente WHERE id_persona = ?")) {
                ps.setInt(1, idPersona);
                ps.executeUpdate();
            }

            try (PreparedStatement ps = con.prepareStatement(
                    "DELETE FROM tbl_persona WHERE id_persona = ?")) {
                ps.setInt(1, idPersona);
                ps.executeUpdate();
            }

            con.commit();
            JOptionPane.showMessageDialog(null, "Cliente eliminado.");
            limpiar();
            cargarTabla();

        } catch (Exception e) { //error al eliminar cliente - pantalla Agregar Cliente
            try {
                if (con != null) con.rollback();
            } catch (Exception ignore) {}
            JOptionPane.showMessageDialog(null, "Error: " + e.getMessage());
        } finally {
            try {
                if (con != null) con.close();
            } catch (Exception ignore) {}
        }
    }

    @FXML
    public void FnInhabilitar() {
        String cedula = TXTcedula.getText().trim();
        if (cedula.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Ingrese la cédula del cliente a inhabilitar.");
            return;
        }
        int confirmar = JOptionPane.showConfirmDialog(null,
                "¿Inhabilitar al cliente con cédula " + cedula + "? No se eliminará, solo quedará inactivo.",
                "Confirmar", JOptionPane.YES_NO_OPTION);
        if (confirmar != JOptionPane.YES_OPTION) return;
        try (java.sql.Connection con = Conexion.establecerConexion();
             java.sql.PreparedStatement ps = con.prepareStatement(
                     "UPDATE tbl_cliente SET estado = 'Inactivo' WHERE id_persona = " +
                     "(SELECT id_persona FROM tbl_persona WHERE cedula = ?)")) {
            ps.setString(1, cedula);
            ps.executeUpdate();
            JOptionPane.showMessageDialog(null, "Cliente inhabilitado correctamente.");
            limpiar();
            cargarTabla();
        } catch (Exception e) { //error al inhabilitar cliente - pantalla Agregar Cliente
            JOptionPane.showMessageDialog(null, "Error al inhabilitar: " + e.getMessage());
        }
    }

    @FXML
    public void FnHabilitar() {
        String cedula = TXTcedula.getText().trim();
        if (cedula.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Ingrese la cédula del cliente a habilitar.");
            return;
        }
        try (java.sql.Connection con = Conexion.establecerConexion();
             java.sql.PreparedStatement ps = con.prepareStatement(
                     "UPDATE tbl_cliente SET estado = 'Activo' WHERE id_persona = " +
                     "(SELECT id_persona FROM tbl_persona WHERE cedula = ?)")) {
            ps.setString(1, cedula);
            ps.executeUpdate();
            JOptionPane.showMessageDialog(null, "Cliente habilitado correctamente.");
            cargarTabla();
        } catch (Exception e) { //error al habilitar cliente - pantalla Agregar Cliente
            JOptionPane.showMessageDialog(null, "Error al habilitar: " + e.getMessage());
        }
    }

    @FXML
    public void FnExportarPDF() {
        if (!com.example.demo1.Utils.Permisos_Util.verificarReporte()) return;
        JasperUtil.exportarPDF(
                "/com/example/demo1/reportes/Reporte_Clientes.jrxml",
                "Reporte_Clientes.pdf"
        );
    }

    private void cargarTabla() {
        ObservableList<ClienteRow> datos = FXCollections.observableArrayList();
        String sql = "SELECT p.nombre, p.cedula, p.tel, c.estado " +
                "FROM tbl_cliente c INNER JOIN tbl_persona p ON c.id_persona = p.id_persona " +
                "ORDER BY p.nombre";

        try (Connection con = conexion.establecerConexion();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                datos.add(new ClienteRow(
                        rs.getString("nombre"),
                        rs.getString("cedula"),
                        rs.getString("tel"),
                        rs.getString("estado")));
            }
            tablaClientes.setItems(datos);

        } catch (Exception e) { //error al cargar tabla de clientes - pantalla Agregar Cliente
            JOptionPane.showMessageDialog(null, "Error al cargar clientes: " + e.getMessage());
        }
    }

    public void limpiar() {
        TXTnombre.clear();
        TXTcedula.clear();
        TXTtelefono.clear();
        TXTdireccion.clear();
    }

    public static class ClienteRow {
        final SimpleStringProperty nombre, cedula, telefono, estado;

        public ClienteRow(String n, String c, String t, String est) {
            nombre   = new SimpleStringProperty(n);
            cedula   = new SimpleStringProperty(c);
            telefono = new SimpleStringProperty(t);
            estado   = new SimpleStringProperty(est != null ? est : "Activo");
        }

        public String getNombre()   { return nombre.get(); }
        public String getCedula()   { return cedula.get(); }
        public String getTelefono() { return telefono.get(); }
        public String getEstado()   { return estado.get(); }
    }
}
