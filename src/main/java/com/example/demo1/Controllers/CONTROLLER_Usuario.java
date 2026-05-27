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
import java.util.HashMap;
import java.util.Map;

public class CONTROLLER_Usuario {

    Conexion conexion = new Conexion();

    @FXML private TextField TXTcodigo;
    @FXML private TextField TXTcontrasena;
    @FXML private ComboBox<String> cmbRol;
    @FXML private ComboBox<String> cmbEmpleado;

    @FXML private RadioButton rbActivo;
    @FXML private RadioButton rbInactivo;
    private ToggleGroup grupoEstado;

    @FXML private TableView<UsuarioRow> tablaUsuarios;
    @FXML private TableColumn<UsuarioRow, String> colId;
    @FXML private TableColumn<UsuarioRow, String> colCodigo;
    @FXML private TableColumn<UsuarioRow, String> colRol;
    @FXML private TableColumn<UsuarioRow, String> colEmpleado;
    @FXML private TableColumn<UsuarioRow, String> colEstado;

    private Map<String, Integer> mapaEmpleados = new HashMap<>();
    private int idUsuarioSeleccionado = -1;

    private static final String[] ROLES = {"administrador", "gerente", "cajero", "delivery", "cliente"};

    @FXML
    public void initialize() {
        grupoEstado = new ToggleGroup();
        rbActivo.setToggleGroup(grupoEstado);
        rbInactivo.setToggleGroup(grupoEstado);
        rbActivo.setSelected(true);

        colId.setCellValueFactory(c -> c.getValue().id);
        colCodigo.setCellValueFactory(c -> c.getValue().codigo);
        colRol.setCellValueFactory(c -> c.getValue().rol);
        colEmpleado.setCellValueFactory(c -> c.getValue().empleado);
        colEstado.setCellValueFactory(c -> c.getValue().estado);

        // Observer
        tablaUsuarios.getSelectionModel().selectedItemProperty().addListener(
                (obs, old, sel) -> {
                    if (sel == null) return;
                    idUsuarioSeleccionado = Integer.parseInt(sel.id.get());
                    TXTcodigo.setText(sel.codigo.get());
                    cmbRol.setValue(sel.rol.get());

                    for (Map.Entry<String, Integer> e : mapaEmpleados.entrySet()) {
                        if (e.getValue().equals(sel.idEmpleado)) {
                            cmbEmpleado.setValue(e.getKey());
                            break;
                        }
                    }

                    if ("Activo".equals(sel.estado.get())) {
                        rbActivo.setSelected(true);
                    } else {
                        rbInactivo.setSelected(true);
                    }
                });

        cmbRol.setItems(FXCollections.observableArrayList(ROLES));
        cargarEmpleados();
        cargarTabla();
    }

    private void cargarEmpleados() {
        ObservableList<String> nombres = FXCollections.observableArrayList();
        String sql = "SELECT e.id_empleado, p.nombre, p.cedula " +
                "FROM tbl_empleado e " +
                "INNER JOIN tbl_persona p ON e.id_persona = p.id_persona " +
                "ORDER BY p.nombre";

        try (Connection con = conexion.establecerConexion();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            mapaEmpleados.clear();
            cmbEmpleado.getItems().clear();
            while (rs.next()) {
                int id = rs.getInt("id_empleado");
                String nombre = rs.getString("nombre");
                String cedula = rs.getString("cedula");
                String display = nombre + " (" + cedula + ")";
                nombres.add(display);
                mapaEmpleados.put(display, id);
            }
            cmbEmpleado.setItems(nombres);

        } catch (Exception e) { //error al cargar empleados - pantalla Gestión de Usuarios
            JOptionPane.showMessageDialog(null, "Error al cargar empleados: " + e.getMessage());
        }
    }

    @FXML
    public void FnGuardar() {
        if (!Permisos_Util.verificarCrearUsuario()) return;

        String codigo = TXTcodigo.getText().trim();
        String contrasena = TXTcontrasena.getText().trim();
        String rol = cmbRol.getValue();
        String empleadoDisplay = cmbEmpleado.getValue();

        if (codigo.isEmpty() || contrasena.isEmpty() || rol == null) {
            JOptionPane.showMessageDialog(null, "Código, contraseña y rol son obligatorios.");
            return;
        }

        Integer idEmpleado = mapaEmpleados.get(empleadoDisplay);

        String sql = "INSERT INTO tbl_usuario (codigo_usuario, contrasenia, rol, estado, id_empleado) " +
                "VALUES (?, ?, ?, ?, ?)";

        try (Connection con = conexion.establecerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, codigo);
            ps.setString(2, contrasena);
            ps.setString(3, rol);
            ps.setString(4, rbActivo.isSelected() ? "activo" : "inactivo");
            if (idEmpleado != null) {
                ps.setInt(5, idEmpleado);
            } else {
                ps.setNull(5, Types.INTEGER);
            }
            ps.executeUpdate();
            JOptionPane.showMessageDialog(null, "Usuario guardado correctamente.");
            limpiar();
            cargarTabla();

        } catch (SQLException e) { //error al guardar usuario - pantalla Gestión de Usuarios
            JOptionPane.showMessageDialog(null, "Error al guardar: " + e.getMessage());
        }
    }

    @FXML
    public void FnBuscar() {
        if (!Permisos_Util.verificarBuscar()) return;

        String codigo = TXTcodigo.getText().trim();
        if (codigo.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Ingrese un código de usuario para buscar.");
            return;
        }

        String sql = "SELECT u.id_usuario, u.codigo_usuario, u.contrasenia, u.rol, u.estado, " +
                "       u.id_empleado, p.nombre as nombre_emp " +
                "FROM tbl_usuario u " +
                "LEFT JOIN tbl_empleado e ON u.id_empleado = e.id_empleado " +
                "LEFT JOIN tbl_persona p ON e.id_persona = p.id_persona " +
                "WHERE u.codigo_usuario = ?";

        try (Connection con = conexion.establecerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, codigo);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    idUsuarioSeleccionado = rs.getInt("id_usuario");
                    TXTcodigo.setText(rs.getString("codigo_usuario"));
                    TXTcontrasena.setText(rs.getString("contrasenia"));
                    cmbRol.setValue(rs.getString("rol"));

                    String nombreEmp = rs.getString("nombre_emp");
                    if (nombreEmp != null) {
                        for (String key : mapaEmpleados.keySet()) {
                            if (key.startsWith(nombreEmp)) {
                                cmbEmpleado.setValue(key);
                                break;
                            }
                        }
                    }

                    String estado = rs.getString("estado");
                    rbActivo.setSelected("activo".equalsIgnoreCase(estado));
                    rbInactivo.setSelected("inactivo".equalsIgnoreCase(estado));

                    JOptionPane.showMessageDialog(null, "Usuario encontrado.");
                } else {
                    JOptionPane.showMessageDialog(null, "No se encontró el usuario.");
                }
            }

        } catch (Exception e) { //error al buscar usuario - pantalla Gestión de Usuarios
            JOptionPane.showMessageDialog(null, "Error al buscar: " + e.getMessage());
        }
    }

    @FXML
    public void FnEditar() {
        if (!Permisos_Util.verificarCrearUsuario()) return;

        if (idUsuarioSeleccionado == -1) {
            JOptionPane.showMessageDialog(null, "Seleccione un usuario de la tabla primero.");
            return;
        }

        if (esAdminProtegido(idUsuarioSeleccionado)) return;

        String codigo = TXTcodigo.getText().trim();
        String contrasena = TXTcontrasena.getText().trim();
        String rol = cmbRol.getValue();
        String empleadoDisplay = cmbEmpleado.getValue();

        if (codigo.isEmpty() || rol == null) {
            JOptionPane.showMessageDialog(null, "Código y rol son obligatorios.");
            return;
        }

        Integer idEmpleado = mapaEmpleados.get(empleadoDisplay);

        String sql = "UPDATE tbl_usuario SET codigo_usuario = ?, contrasenia = ?, rol = ?, " +
                "estado = ?, id_empleado = ? WHERE id_usuario = ?";

        try (Connection con = conexion.establecerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, codigo);
            ps.setString(2, contrasena.isEmpty() ? "default" : contrasena);
            ps.setString(3, rol);
            ps.setString(4, rbActivo.isSelected() ? "activo" : "inactivo");
            if (idEmpleado != null) {
                ps.setInt(5, idEmpleado);
            } else {
                ps.setNull(5, Types.INTEGER);
            }
            ps.setInt(6, idUsuarioSeleccionado);

            if (ps.executeUpdate() > 0) {
                JOptionPane.showMessageDialog(null, "Usuario actualizado correctamente.");
                idUsuarioSeleccionado = -1;
                limpiar();
                cargarTabla();
            } else {
                JOptionPane.showMessageDialog(null, "No se encontró el usuario.");
            }

        } catch (Exception e) { //error al editar usuario - pantalla Gestión de Usuarios
            JOptionPane.showMessageDialog(null, "Error al editar: " + e.getMessage());
        }
    }

    @FXML
    public void FnEliminar() {
        if (!Permisos_Util.verificarEliminar()) return;

        if (idUsuarioSeleccionado == -1) {
            JOptionPane.showMessageDialog(null, "Seleccione un usuario de la tabla primero.");
            return;
        }

        if (esAdminProtegido(idUsuarioSeleccionado)) return;

        int c = JOptionPane.showConfirmDialog(null,
                "¿Seguro que desea eliminar este usuario?",
                "Confirmar eliminación", JOptionPane.YES_NO_OPTION);
        if (c != JOptionPane.YES_OPTION) return;

        try (Connection con = conexion.establecerConexion();
             PreparedStatement ps = con.prepareStatement(
                     "DELETE FROM tbl_usuario WHERE id_usuario = ?")) {

            ps.setInt(1, idUsuarioSeleccionado);
            if (ps.executeUpdate() > 0) {
                JOptionPane.showMessageDialog(null, "Usuario eliminado correctamente.");
                idUsuarioSeleccionado = -1;
                limpiar();
                cargarTabla();
            } else {
                JOptionPane.showMessageDialog(null, "No se encontró el usuario.");
            }

        } catch (Exception e) { //error al eliminar usuario - pantalla Gestión de Usuarios
            JOptionPane.showMessageDialog(null, "Error al eliminar: " + e.getMessage());
        }
    }

    @FXML
    public void FnInhabilitar() {
        if (idUsuarioSeleccionado == -1) {
            JOptionPane.showMessageDialog(null, "Seleccione un usuario de la tabla primero.");
            return;
        }

        if (esAdminProtegido(idUsuarioSeleccionado)) return;

        int confirmar = JOptionPane.showConfirmDialog(null,
                "¿Inhabilitar este usuario? No se eliminará, solo quedará inactivo.",
                "Confirmar", JOptionPane.YES_NO_OPTION);
        if (confirmar != JOptionPane.YES_OPTION) return;

        try (Connection con = Conexion.establecerConexion();
             PreparedStatement ps = con.prepareStatement(
                     "UPDATE tbl_usuario SET estado = 'inactivo' WHERE id_usuario = ?")) {
            ps.setInt(1, idUsuarioSeleccionado);
            ps.executeUpdate();
            JOptionPane.showMessageDialog(null, "Usuario inhabilitado correctamente.");
            idUsuarioSeleccionado = -1;
            cargarTabla();
        } catch (Exception e) { //error al inhabilitar usuario - pantalla Gestión de Usuarios
            JOptionPane.showMessageDialog(null, "Error al inhabilitar: " + e.getMessage());
        }
    }

    @FXML
    public void FnHabilitar() {
        if (idUsuarioSeleccionado == -1) {
            JOptionPane.showMessageDialog(null, "Seleccione un usuario de la tabla primero.");
            return;
        }

        if (esAdminProtegido(idUsuarioSeleccionado)) return;

        try (Connection con = Conexion.establecerConexion();
             PreparedStatement ps = con.prepareStatement(
                     "UPDATE tbl_usuario SET estado = 'activo' WHERE id_usuario = ?")) {
            ps.setInt(1, idUsuarioSeleccionado);
            ps.executeUpdate();
            JOptionPane.showMessageDialog(null, "Usuario habilitado correctamente.");
            idUsuarioSeleccionado = -1;
            cargarTabla();
        } catch (Exception e) { //error al habilitar usuario - pantalla Gestión de Usuarios
            JOptionPane.showMessageDialog(null, "Error al habilitar: " + e.getMessage());
        }
    }

    @FXML
    public void FnLimpiar() {
        limpiar();
    }

    public void limpiar() {
        TXTcodigo.clear();
        TXTcontrasena.clear();
        cmbRol.getSelectionModel().clearSelection();
        cmbEmpleado.getSelectionModel().clearSelection();
        rbActivo.setSelected(true);
        idUsuarioSeleccionado = -1;
    }

    private boolean esAdminProtegido(int idUsuario) {
        String sql = "SELECT rol FROM tbl_usuario WHERE id_usuario = ?";
        try (Connection con = conexion.establecerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idUsuario);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next() && "administrador".equalsIgnoreCase(rs.getString("rol"))) {
                    JOptionPane.showMessageDialog(null,
                            "No se puede modificar o eliminar un usuario administrador desde esta pantalla.");
                    return true;
                }
            }
        } catch (Exception e) { //error al verificar admin protegido - pantalla Gestión de Usuarios
            JOptionPane.showMessageDialog(null, "Error: " + e.getMessage());
        }
        return false;
    }

    private void cargarTabla() {
        ObservableList<UsuarioRow> datos = FXCollections.observableArrayList();
        String sql = "SELECT u.id_usuario, u.codigo_usuario, u.rol, u.estado, " +
                "       COALESCE(p.nombre, '—') as nombre_empleado, u.id_empleado " +
                "FROM tbl_usuario u " +
                "LEFT JOIN tbl_empleado e ON u.id_empleado = e.id_empleado " +
                "LEFT JOIN tbl_persona p ON e.id_persona = p.id_persona " +
                "WHERE u.rol != 'administrador' " +
                "ORDER BY u.codigo_usuario";

        try (Connection con = conexion.establecerConexion();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                datos.add(new UsuarioRow(
                        String.valueOf(rs.getInt("id_usuario")),
                        rs.getString("codigo_usuario"),
                        rs.getString("rol"),
                        rs.getString("nombre_empleado"),
                        rs.getString("estado"),
                        rs.getInt("id_empleado")));
            }
            tablaUsuarios.setItems(datos);

        } catch (Exception e) { //error al cargar tabla de usuarios - pantalla Gestión de Usuarios
            JOptionPane.showMessageDialog(null, "Error al cargar usuarios: " + e.getMessage());
        }
    }

    public static class UsuarioRow {
        final SimpleStringProperty id;
        final SimpleStringProperty codigo;
        final SimpleStringProperty rol;
        final SimpleStringProperty empleado;
        final SimpleStringProperty estado;
        final int idEmpleado;

        public UsuarioRow(String id, String codigo, String rol, String empleado, String estado, int idEmpleado) {
            this.id = new SimpleStringProperty(id);
            this.codigo = new SimpleStringProperty(codigo);
            this.rol = new SimpleStringProperty(rol);
            this.empleado = new SimpleStringProperty(empleado);
            this.estado = new SimpleStringProperty(estado != null ? (estado.equalsIgnoreCase("activo") ? "Activo" : "Inactivo") : "Activo");
            this.idEmpleado = idEmpleado;
        }

        public String getId() { return id.get(); }
        public String getCodigo() { return codigo.get(); }
        public String getRol() { return rol.get(); }
        public String getEmpleado() { return empleado.get(); }
        public String getEstado() { return estado.get(); }
    }
}
