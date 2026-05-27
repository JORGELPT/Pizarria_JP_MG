package com.example.demo1.Controllers;

import com.example.demo1.Database.Conexion;
import com.example.demo1.Utils.Permisos_Util;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javax.swing.JOptionPane;
import java.sql.*;

public class CONTROLLER_Empleado {

    Conexion conexion = new Conexion();

    @FXML private TextField TXTnombre;
    @FXML private TextField TXTfechaIngreso;
    @FXML private TextField TXTtelefono;
    @FXML private TextField TXTcedula;
    @FXML private TextField TXTdireccion;
    @FXML private TextField TXTemail;
    @FXML private TextField TXTsalario;
    @FXML private TextField TXTcontrasena;
    @FXML private ComboBox<String> cmbSucursal;

    // Cargo
    @FXML private RadioButton rbCajero;
    @FXML private RadioButton rbCocinero;
    @FXML private RadioButton rbSupervisor;
    @FXML private RadioButton rbGerente;
    private ToggleGroup grupoCargo;

    // Estado
    @FXML private RadioButton rbActivo;
    @FXML private RadioButton rbInactivo;
    private ToggleGroup grupoEstado;

    // Horario
    @FXML private RadioButton rbHorarioDia;
    @FXML private RadioButton rbHorarioNoche;
    private ToggleGroup grupoHorario;

    @FXML private TableView<EmpleadoRow> tablaEmpleados;
    @FXML private TableColumn<EmpleadoRow, String> colNombre;
    @FXML private TableColumn<EmpleadoRow, String> colCedula;
    @FXML private TableColumn<EmpleadoRow, String> colCargo;
    @FXML private TableColumn<EmpleadoRow, String> colEstado;

    private int idEmpleadoSeleccionado = -1;
    private int idPersonaSeleccionada  = -1;

    @FXML
    public void initialize() {
        grupoCargo = new ToggleGroup();
        rbCajero.setToggleGroup(grupoCargo);
        rbCocinero.setToggleGroup(grupoCargo);
        rbSupervisor.setToggleGroup(grupoCargo);
        rbGerente.setToggleGroup(grupoCargo);

        grupoEstado = new ToggleGroup();
        rbActivo.setToggleGroup(grupoEstado);
        rbInactivo.setToggleGroup(grupoEstado);

        grupoHorario = new ToggleGroup();
        rbHorarioDia.setToggleGroup(grupoHorario);
        rbHorarioNoche.setToggleGroup(grupoHorario);

        colNombre.setCellValueFactory(c -> c.getValue().nombre);
        colCedula.setCellValueFactory(c -> c.getValue().cedula);
        colCargo.setCellValueFactory(c  -> c.getValue().cargo);
        colEstado.setCellValueFactory(c -> c.getValue().estado);

        tablaEmpleados.getSelectionModel().selectedItemProperty().addListener(
                (obs, old, sel) -> {
                    if (sel == null) return;
                    idEmpleadoSeleccionado = sel.idEmpleado;
                    idPersonaSeleccionada  = sel.idPersona;
                    TXTnombre.setText(sel.nombre.get());
                    TXTcedula.setText(sel.cedula.get());
                });

        cargarSucursales();
        cargarTabla();
    }

    private void cargarSucursales() {
        ObservableList<String> datos = FXCollections.observableArrayList();
        try (Connection con = conexion.establecerConexion();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT nombre_sucursal FROM tbl_sucursal ORDER BY nombre_sucursal");
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                datos.add(rs.getString("nombre_sucursal"));
            }
            cmbSucursal.setItems(datos);

        } catch (Exception e) { //error al cargar sucursales - pantalla Agregar Empleado
            JOptionPane.showMessageDialog(null, "Error al cargar sucursales: " + e.getMessage());
        }
    }

    private int obtenerIdCargo(Connection con, String nombreCargo) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT id_cargo FROM tbl_cargo WHERE nombre = ?")) {
            ps.setString(1, nombreCargo);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt("id_cargo") : -1;
            }
        }
    }

    private int obtenerIdSucursal(Connection con, String nombreSucursal) throws SQLException {
        if (nombreSucursal == null) return 1;
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT id_sucursal FROM tbl_sucursal WHERE nombre_sucursal = ?")) {
            ps.setString(1, nombreSucursal);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt("id_sucursal") : 1;
            }
        }
    }

    @FXML
    public void fnGuardar() {
        // Solo ADMIN puede crear empleados
        if (!Permisos_Util.verificarCrearUsuario()) return;

        String nombre       = TXTnombre.getText().trim();
        String fechaIngreso = TXTfechaIngreso.getText().trim();
        String telefono     = TXTtelefono.getText().trim();
        String cedula       = TXTcedula.getText().trim();
        String direccion    = TXTdireccion.getText().trim();
        String email        = TXTemail.getText().trim();
        String salario      = TXTsalario.getText().trim();
        String contrasena   = TXTcontrasena.getText().trim();
        String sucursal     = cmbSucursal.getValue();

        String cargo = grupoCargo.getSelectedToggle() != null
                ? ((RadioButton) grupoCargo.getSelectedToggle()).getText() : "";
        String estado = rbActivo.isSelected() ? "Activo"
                : rbInactivo.isSelected() ? "Inactivo" : "";
        String horario = grupoHorario.getSelectedToggle() != null
                ? ((RadioButton) grupoHorario.getSelectedToggle()).getText() : "";

        if (nombre.isEmpty() || cedula.isEmpty() || cargo.isEmpty() ||
                estado.isEmpty() || salario.isEmpty() || email.isEmpty() ||
                contrasena.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Complete todos los campos obligatorios.");
            return;
        }

        Connection con = null;
        try {
            con = conexion.establecerConexion();
            con.setAutoCommit(false);

            // 1) Insertar persona con rol_bd = cargo (minúsculas)
            String sqlP = "INSERT INTO tbl_persona (nombre, tel, cedula, direccion, rol_bd, contrasenia) " +
                    "VALUES (?, ?, ?, ?, ?, ?)";
            int idPersona;
            try (PreparedStatement ps = con.prepareStatement(sqlP, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, nombre);
                ps.setString(2, telefono);
                ps.setString(3, cedula);
                ps.setString(4, direccion);
                ps.setString(5, cargo.toLowerCase()); // rol_bd = cajero / cocinero / gerente...
                ps.setString(6, contrasena);
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    idPersona = keys.next() ? keys.getInt(1) : 0;
                }
            }

            int idCargo    = obtenerIdCargo(con, cargo);
            int idSucursal = obtenerIdSucursal(con, sucursal);

            if (idCargo == -1) {
                con.rollback();
                JOptionPane.showMessageDialog(null, "No existe el cargo '" + cargo + "' en tbl_cargo.");
                return;
            }

            // 2) Insertar empleado
            String sqlE = "INSERT INTO tbl_empleado " +
                    "(id_persona, id_cargo, horario, salario, id_sucursal, fecha_ingreso, estado, email) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement ps = con.prepareStatement(sqlE)) {
                ps.setInt(1, idPersona);
                ps.setInt(2, idCargo);
                ps.setString(3, horario);
                ps.setBigDecimal(4, new java.math.BigDecimal(salario));
                ps.setInt(5, idSucursal);
                ps.setDate(6, fechaIngreso.isEmpty()
                        ? new java.sql.Date(System.currentTimeMillis())
                        : Date.valueOf(fechaIngreso));
                ps.setString(7, estado);
                ps.setString(8, email);
                ps.executeUpdate();
            }

            con.commit();
            JOptionPane.showMessageDialog(null, "Empleado guardado correctamente.");
            limpiar();
            cargarTabla();

        } catch (Exception e) { //error al guardar empleado - pantalla Agregar Empleado
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
    public void fnBuscar() {
        if (!Permisos_Util.verificarBuscar()) return;

        String cedula = TXTcedula.getText().trim();
        if (cedula.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Ingrese una cédula para buscar.");
            return;
        }

        String sql = "SELECT p.nombre, p.tel, p.cedula, p.direccion, " +
                "       e.salario, e.horario, e.estado, e.email, c.nombre as cargo " +
                "FROM tbl_empleado e " +
                "INNER JOIN tbl_persona p ON e.id_persona = p.id_persona " +
                "LEFT JOIN tbl_cargo c ON e.id_cargo = c.id_cargo " +
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
                    TXTsalario.setText(String.valueOf(rs.getBigDecimal("salario")));
                    TXTemail.setText(rs.getString("email"));
                } else {
                    JOptionPane.showMessageDialog(null, "No se encontró el empleado.");
                }
            }

        } catch (Exception e) { //error al buscar empleado - pantalla Agregar Empleado
            JOptionPane.showMessageDialog(null, "Error: " + e.getMessage());
        }
    }

    @FXML
    public void fnEditar() {
        if (!Permisos_Util.verificarEditar()) return;
        if (idEmpleadoSeleccionado == -1) {
            JOptionPane.showMessageDialog(null, "Seleccione un empleado de la tabla primero.");
            return;
        }
        String nombre    = TXTnombre.getText().trim();
        String telefono  = TXTtelefono.getText().trim();
        String cedula    = TXTcedula.getText().trim();
        String direccion = TXTdireccion.getText().trim();
        String salario   = TXTsalario.getText().trim();
        String email     = TXTemail.getText().trim();
        String horario   = grupoHorario.getSelectedToggle() != null
                ? ((RadioButton) grupoHorario.getSelectedToggle()).getText() : "";
        String estado    = rbActivo.isSelected() ? "Activo"
                : rbInactivo.isSelected() ? "Inactivo" : "";

        if (nombre.isEmpty() || cedula.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Nombre y cédula son obligatorios.");
            return;
        }

        Connection con = null;
        try {
            con = conexion.establecerConexion();
            con.setAutoCommit(false);

            String sqlP = "UPDATE tbl_persona SET nombre=?, tel=?, cedula=?, direccion=? WHERE id_persona=?";
            try (PreparedStatement ps = con.prepareStatement(sqlP)) {
                ps.setString(1, nombre);
                ps.setString(2, telefono);
                ps.setString(3, cedula);
                ps.setString(4, direccion);
                ps.setInt(5, idPersonaSeleccionada);
                ps.executeUpdate();
            }

            String sqlE = "UPDATE tbl_empleado SET horario=?, estado=?, email=?" +
                    (salario.isEmpty() ? "" : ", salario=?") + " WHERE id_empleado=?";
            try (PreparedStatement ps = con.prepareStatement(sqlE)) {
                ps.setString(1, horario);
                ps.setString(2, estado.isEmpty() ? "Activo" : estado);
                ps.setString(3, email);
                if (salario.isEmpty()) {
                    ps.setInt(4, idEmpleadoSeleccionado);
                } else {
                    ps.setBigDecimal(4, new java.math.BigDecimal(salario));
                    ps.setInt(5, idEmpleadoSeleccionado);
                }
                ps.executeUpdate();
            }

            con.commit();
            JOptionPane.showMessageDialog(null, "Empleado actualizado correctamente.");
            idEmpleadoSeleccionado = -1;
            idPersonaSeleccionada  = -1;
            limpiar();
            cargarTabla();
        } catch (Exception e) { //error al editar empleado - pantalla Agregar Empleado
            try { if (con != null) con.rollback(); } catch (Exception ignore) {}
            JOptionPane.showMessageDialog(null, "Error al editar: " + e.getMessage());
        } finally {
            try { if (con != null) con.close(); } catch (Exception ignore) {}
        }
    }

    @FXML
    public void fnEliminar() {
        if (!Permisos_Util.verificarEliminar()) return;
        if (idEmpleadoSeleccionado == -1) {
            JOptionPane.showMessageDialog(null, "Seleccione un empleado de la tabla primero.");
            return;
        }
        int confirmar = JOptionPane.showConfirmDialog(null,
                "¿Está seguro de eliminar este empleado? Esta acción no se puede deshacer.",
                "Confirmar eliminación", JOptionPane.YES_NO_OPTION);
        if (confirmar != JOptionPane.YES_OPTION) return;

        Connection con = null;
        try {
            con = conexion.establecerConexion();
            con.setAutoCommit(false);

            try (PreparedStatement ps = con.prepareStatement(
                    "DELETE FROM tbl_empleado WHERE id_empleado = ?")) {
                ps.setInt(1, idEmpleadoSeleccionado);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = con.prepareStatement(
                    "DELETE FROM tbl_persona WHERE id_persona = ?")) {
                ps.setInt(1, idPersonaSeleccionada);
                ps.executeUpdate();
            }

            con.commit();
            JOptionPane.showMessageDialog(null, "Empleado eliminado correctamente.");
            idEmpleadoSeleccionado = -1;
            idPersonaSeleccionada  = -1;
            limpiar();
            cargarTabla();
        } catch (Exception e) { //error al eliminar empleado - pantalla Agregar Empleado
            try { if (con != null) con.rollback(); } catch (Exception ignore) {}
            JOptionPane.showMessageDialog(null, "Error al eliminar: " + e.getMessage());
        } finally {
            try { if (con != null) con.close(); } catch (Exception ignore) {}
        }
    }

    @FXML
    public void FnInhabilitar() {
        if (idEmpleadoSeleccionado == -1) {
            JOptionPane.showMessageDialog(null, "Seleccione un empleado de la tabla primero.");
            return;
        }
        int confirmar = JOptionPane.showConfirmDialog(null,
                "¿Inhabilitar este empleado? No se eliminará, solo quedará inactivo.",
                "Confirmar", JOptionPane.YES_NO_OPTION);
        if (confirmar != JOptionPane.YES_OPTION) return;
        try (java.sql.Connection con = Conexion.establecerConexion();
             java.sql.PreparedStatement ps = con.prepareStatement(
                     "UPDATE tbl_empleado SET estado = 'Inactivo' WHERE id_empleado = ?")) {
            ps.setInt(1, idEmpleadoSeleccionado);
            ps.executeUpdate();
            JOptionPane.showMessageDialog(null, "Empleado inhabilitado correctamente.");
            idEmpleadoSeleccionado = -1;
        } catch (Exception e) { //error al inhabilitar empleado - pantalla Agregar Empleado
            JOptionPane.showMessageDialog(null, "Error al inhabilitar: " + e.getMessage());
        }
    }

    @FXML
    public void FnHabilitar() {
        if (idEmpleadoSeleccionado == -1) {
            JOptionPane.showMessageDialog(null, "Seleccione un empleado de la tabla primero.");
            return;
        }
        try (java.sql.Connection con = Conexion.establecerConexion();
             java.sql.PreparedStatement ps = con.prepareStatement(
                     "UPDATE tbl_empleado SET estado = 'Activo' WHERE id_empleado = ?")) {
            ps.setInt(1, idEmpleadoSeleccionado);
            ps.executeUpdate();
            JOptionPane.showMessageDialog(null, "Empleado habilitado correctamente.");
            idEmpleadoSeleccionado = -1;
            cargarTabla();
        } catch (Exception e) { //error al habilitar empleado - pantalla Agregar Empleado
            JOptionPane.showMessageDialog(null, "Error al habilitar: " + e.getMessage());
        }
    }

    private void cargarTabla() {
        ObservableList<EmpleadoRow> datos = FXCollections.observableArrayList();
        String sql = "SELECT e.id_empleado, e.id_persona, p.nombre, p.cedula, c.nombre as cargo, e.estado " +
                "FROM tbl_empleado e " +
                "INNER JOIN tbl_persona p ON e.id_persona = p.id_persona " +
                "LEFT JOIN tbl_cargo c ON e.id_cargo = c.id_cargo " +
                "ORDER BY p.nombre";

        try (Connection con = conexion.establecerConexion();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                datos.add(new EmpleadoRow(
                        rs.getInt("id_empleado"),
                        rs.getInt("id_persona"),
                        rs.getString("nombre"),
                        rs.getString("cedula"),
                        rs.getString("cargo"),
                        rs.getString("estado")));
            }
            tablaEmpleados.setItems(datos);

        } catch (Exception e) { //error al cargar tabla de empleados - pantalla Agregar Empleado
            JOptionPane.showMessageDialog(null, "Error al cargar empleados: " + e.getMessage());
        }
    }

    public void limpiar() {
        TXTnombre.clear();
        TXTfechaIngreso.clear();
        TXTtelefono.clear();
        TXTcedula.clear();
        TXTdireccion.clear();
        TXTemail.clear();
        TXTsalario.clear();
        TXTcontrasena.clear();
        cmbSucursal.getSelectionModel().clearSelection();
        grupoCargo.selectToggle(null);
        grupoEstado.selectToggle(null);
        grupoHorario.selectToggle(null);
    }

    public static class EmpleadoRow {
        final int idEmpleado, idPersona;
        final SimpleStringProperty nombre, cedula, cargo, estado;

        public EmpleadoRow(int idEmpleado, int idPersona, String n, String c, String ca, String est) {
            this.idEmpleado = idEmpleado;
            this.idPersona  = idPersona;
            nombre = new SimpleStringProperty(n);
            cedula = new SimpleStringProperty(c);
            cargo  = new SimpleStringProperty(ca != null ? ca : "");
            estado = new SimpleStringProperty(est != null ? est : "Activo");
        }

        public String getNombre() { return nombre.get(); }
        public String getCedula() { return cedula.get(); }
        public String getCargo()  { return cargo.get(); }
        public String getEstado() { return estado.get(); }
    }
}
