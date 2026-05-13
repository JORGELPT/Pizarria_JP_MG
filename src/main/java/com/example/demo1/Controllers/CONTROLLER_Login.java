package com.example.demo1.Controllers;

import com.example.demo1.Database.Conexion;
import com.example.demo1.Utils.CONTROLLER_Seccion;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Controller de Login.
 *
 * Autentica al usuario contra la tabla tbl_usuario de la BD dominospizza_RA5.
 *
 * Login esperado:
 *   codigo_usuario → campo "Usuario"
 *   contrasenia    → campo "Contraseña"
 *
 * Reglas:
 *   - Solo se aceptan usuarios con estado = 'activo'.
 *   - Los roles válidos en la BD (CK_tbl_usuario_rol) son:
 *     cliente, cajero, gerente, administrador.
 *   - Cada usuario pertenece a un empleado O a un cliente (CK_tbl_usuario_tipo).
 *   - Al autenticar, se hace JOIN con tbl_persona para obtener el nombre.
 *   - Se actualiza ultimo_acceso y se resetean intentos_fallidos.
 */
public class CONTROLLER_Login {

    @FXML private TextField     TXTusuario;
    @FXML private PasswordField TXTcontrasena;
    @FXML private Label         lblError;

    @FXML
    public void initialize() {
        if (lblError != null) lblError.setText("");
    }

    // -------------------------------------------------------------------------
    //  Acción del botón "Iniciar Sesión"
    // -------------------------------------------------------------------------
    @FXML
    public void FnIniciarSesion(ActionEvent event) {
        String codigo     = TXTusuario.getText().trim();
        String contrasena = TXTcontrasena.getText(); // NO trim en password

        if (codigo.isEmpty() || contrasena.isEmpty()) {
            mostrarError("Ingrese usuario y contraseña.");
            return;
        }

        /*
         * Consulta contra tbl_usuario con JOIN a tbl_persona para obtener el
         * nombre del dueño del usuario (ya sea empleado o cliente).
         *
         *   u.id_empleado NOT NULL  →  JOIN por tbl_empleado.id_persona
         *   u.id_cliente  NOT NULL  →  JOIN por tbl_cliente.id_persona
         */
        final String SQL =
                "SELECT  u.id_usuario, " +
                "        u.codigo_usuario, " +
                "        u.contrasenia, " +
                "        u.rol, " +
                "        u.estado, " +
                "        u.id_empleado, " +
                "        u.id_cliente, " +
                "        COALESCE(pe.nombre, pc.nombre) AS nombre, " +
                "        COALESCE(pe.id_persona, pc.id_persona) AS id_persona, " +
                "        e.email " +
                "FROM    tbl_usuario u " +
                "LEFT JOIN tbl_empleado e  ON e.id_empleado = u.id_empleado " +
                "LEFT JOIN tbl_persona  pe ON pe.id_persona = e.id_persona " +
                "LEFT JOIN tbl_cliente  c  ON c.id_cliente  = u.id_cliente " +
                "LEFT JOIN tbl_persona  pc ON pc.id_persona = c.id_persona " +
                "WHERE   u.codigo_usuario = ?";

        try (Connection con = Conexion.establecerConexion()) {
            if (con == null) {
                mostrarError("No se pudo conectar a la base de datos.");
                return;
            }

            try (PreparedStatement ps = con.prepareStatement(SQL)) {
                ps.setString(1, codigo);
                try (ResultSet rs = ps.executeQuery()) {

                    // ---- 1. Usuario no existe --------------------------------
                    if (!rs.next()) {
                        mostrarError("Usuario no encontrado.");
                        return;
                    }

                    // ---- 2. Verificar estado --------------------------------
                    String estado = rs.getString("estado");
                    if (estado == null || !estado.equalsIgnoreCase("activo")) {
                        mostrarError("El usuario está " + estado + ". Contacte al administrador.");
                        return;
                    }

                    // ---- 3. Verificar contraseña (texto plano) --------------
                    String contrasenaBD = rs.getString("contrasenia");
                    if (contrasenaBD == null || !contrasenaBD.equals(contrasena)) {
                        registrarIntentoFallido(con, codigo);
                        mostrarError("Contraseña incorrecta.");
                        return;
                    }

                    // ---- 4. Credenciales correctas --------------------------
                    int    idUsuario  = rs.getInt("id_usuario");
                    String rol        = rs.getString("rol");
                    String nombre     = rs.getString("nombre");
                    String email      = rs.getString("email");
                    int    idEmpleado = rs.getInt("id_empleado");
                    if (rs.wasNull()) idEmpleado = -1;
                    int idCliente = rs.getInt("id_cliente");
                    if (rs.wasNull()) idCliente = -1;
                    int idPersona = rs.getInt("id_persona");
                    if (rs.wasNull()) idPersona = -1;

                    // Normalizar rol a minúsculas
                    rol = (rol == null) ? "cliente" : rol.trim().toLowerCase();

                    // Nombre por defecto si la persona no existe
                    if (nombre == null || nombre.isBlank()) nombre = codigo;

                    // ---- 5. Guardar sesión ----------------------------------
                    CONTROLLER_Seccion.getInstancia()
                            .iniciar(idPersona, idEmpleado, idCliente,
                                    nombre, rol, email != null ? email : "");

                    // ---- 6. Registrar último acceso y resetear intentos -----
                    registrarAccesoExitoso(con, idUsuario);

                    // ---- 7. Abrir ventana principal -------------------------
                    abrirMainApp(event);
                }
            }

        } catch (SQLException e) {
            mostrarError("Error de base de datos: " + e.getMessage());
        } catch (Exception e) {
            mostrarError("Error inesperado: " + e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    //  Actualización de metadatos del usuario
    // -------------------------------------------------------------------------

    /** Pone ultimo_acceso=GETDATE() y intentos_fallidos=0. */
    private void registrarAccesoExitoso(Connection con, int idUsuario) {
        final String SQL =
                "UPDATE tbl_usuario " +
                "   SET ultimo_acceso = GETDATE(), " +
                "       intentos_fallidos = 0 " +
                " WHERE id_usuario = ?";
        try (PreparedStatement ps = con.prepareStatement(SQL)) {
            ps.setInt(1, idUsuario);
            ps.executeUpdate();
        } catch (SQLException ex) {
            // No bloqueamos el login por un error de auditoría
        }
    }

    /** Incrementa intentos_fallidos cuando la contraseña es incorrecta. */
    private void registrarIntentoFallido(Connection con, String codigo) {
        final String SQL =
                "UPDATE tbl_usuario " +
                "   SET intentos_fallidos = intentos_fallidos + 1 " +
                " WHERE codigo_usuario = ?";
        try (PreparedStatement ps = con.prepareStatement(SQL)) {
            ps.setString(1, codigo);
            ps.executeUpdate();
        } catch (SQLException ex) {
            // Error de auditoría no crítico
        }
    }

    // -------------------------------------------------------------------------
    //  Cierra el Login y abre la ventana principal (MainView)
    // -------------------------------------------------------------------------
    private void abrirMainApp(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/example/demo1/Pantallas/MainView.fxml"));
            Parent root = loader.load();

            Stage stageActual = (Stage) ((javafx.scene.Node) event.getSource())
                    .getScene().getWindow();

            javafx.geometry.Rectangle2D pantalla =
                    javafx.stage.Screen.getPrimary().getVisualBounds();
            Scene scene = new Scene(root,
                    pantalla.getWidth(), pantalla.getHeight());
            stageActual.setScene(scene);
            stageActual.setTitle("Domino's Pizza - Sistema de Gestión");
            stageActual.setMaximized(true);
            stageActual.setResizable(true);
            stageActual.show();

        } catch (Exception e) {
            mostrarError("Error al abrir el sistema: " + e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    //  Helpers UI
    // -------------------------------------------------------------------------
    private void mostrarError(String msg) {
        if (lblError != null) lblError.setText(msg);
    }
}
