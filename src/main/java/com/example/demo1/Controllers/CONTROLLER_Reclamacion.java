package com.example.demo1.Controllers;

import com.example.demo1.Database.Conexion;
import com.example.demo1.Utils.JasperUtil;
import com.example.demo1.Utils.Permisos_Util;
import com.example.demo1.Utils.CONTROLLER_Seccion;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javax.swing.JOptionPane;
import java.sql.*;

/**
 * Controller de Hacer una Reclamación.
 * Adaptado a BD real:
 *   tbl_reclamacion: desc_reclamacion, id_cliente, estado, fecha, id_empleado, id_pedido
 *
 * NOTA: CLIENTE puede crear reclamaciones (su propio id_cliente se usa automáticamente).
 */
public class CONTROLLER_Reclamacion {

    Conexion conexion = new Conexion();

    @FXML private TextField    TXTasiste;
    @FXML private TextField    TXTclienteNombre;
    @FXML private Label        lblIdCliente;
    @FXML private ComboBox<String> cmbEstado;
    @FXML private TextField    TXTidPedido;
    @FXML private TextArea     TXTdescripcion;

    private int idClienteSeleccionado = -1;

    @FXML
    public void initialize() {
        cmbEstado.setItems(FXCollections.observableArrayList("Pendiente", "Resuelta"));

        // Si el usuario logueado es CLIENTE, auto-llenar su id
        CONTROLLER_Seccion s = CONTROLLER_Seccion.getInstancia();
        if (s.esCliente() && s.getIdCliente() != -1) {
            idClienteSeleccionado = s.getIdCliente();
            TXTclienteNombre.setText(s.getNombre());
            TXTclienteNombre.setDisable(true);
            lblIdCliente.setText("ID cliente: " + idClienteSeleccionado + " (tú)");
        }

        // El empleado que asiste siempre es el logueado (si es empleado)
        if (s.getIdEmpleado() != -1) {
            TXTasiste.setText(String.valueOf(s.getIdEmpleado()));
            TXTasiste.setDisable(true);
        }
    }

    @FXML
    public void FnBuscarCliente() {
        if (!Permisos_Util.verificarBuscar()) return;

        String nombre = TXTclienteNombre.getText().trim();
        if (nombre.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Ingrese el nombre del cliente.");
            return;
        }

        String sql = "SELECT c.id_cliente, p.nombre " +
                "FROM tbl_cliente c " +
                "INNER JOIN tbl_persona p ON c.id_persona = p.id_persona " +
                "WHERE p.nombre LIKE ?";

        try (Connection con = conexion.establecerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, "%" + nombre + "%");
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    idClienteSeleccionado = rs.getInt("id_cliente");
                    TXTclienteNombre.setText(rs.getString("nombre"));
                    lblIdCliente.setText("ID cliente: " + idClienteSeleccionado);
                } else {
                    idClienteSeleccionado = -1;
                    lblIdCliente.setText("ID cliente: — (no encontrado)");
                    JOptionPane.showMessageDialog(null, "No se encontró el cliente.");
                }
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error: " + e.getMessage());
        }
    }

    @FXML
    public void FnGuardar() {
        // Cliente, Cajero, Admin y Gerente pueden crear reclamaciones
        if (!CONTROLLER_Seccion.getInstancia().puedeCrearReclamacion()) {
            JOptionPane.showMessageDialog(null, "Tu rol no puede crear reclamaciones.");
            return;
        }

        String asiste      = TXTasiste.getText().trim();
        String estado      = cmbEstado.getValue();
        String idPedido    = TXTidPedido.getText().trim();
        String descripcion = TXTdescripcion.getText().trim();

        if (idClienteSeleccionado == -1) {
            JOptionPane.showMessageDialog(null, "Primero busque un cliente válido.");
            return;
        }
        if (estado == null || idPedido.isEmpty() || descripcion.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Estado, id de pedido y descripción son obligatorios.");
            return;
        }

        String sql = "INSERT INTO tbl_reclamacion " +
                "(desc_reclamacion, id_cliente, estado, fecha, id_empleado, id_pedido) " +
                "VALUES (?, ?, ?, GETDATE(), ?, ?)";

        try (Connection con = conexion.establecerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, descripcion);
            ps.setInt(2, idClienteSeleccionado);
            ps.setString(3, estado);
            int idEmpleado = asiste.isEmpty()
                    ? CONTROLLER_Seccion.getInstancia().getIdEmpleado()
                    : Integer.parseInt(asiste);
            if (idEmpleado == -1) ps.setNull(4, Types.INTEGER);
            else                  ps.setInt(4, idEmpleado);
            ps.setInt(5, Integer.parseInt(idPedido));
            ps.executeUpdate();
            JOptionPane.showMessageDialog(null, "Reclamación registrada correctamente.");

            // Notificar por correo
            String nombreCliente = CONTROLLER_Seccion.getInstancia().getNombre();
            String fechaHoy = java.time.LocalDate.now().toString();
            com.example.demo1.Utils.CorreoUtil.notificarReclamacion(nombreCliente, descripcion, fechaHoy);

            limpiar();

        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(null, "El id de pedido debe ser un número.");
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error al guardar: " + e.getMessage());
        }
    }

    @FXML
    public void FnBuscar() {
        if (!Permisos_Util.verificarBuscar()) return;

        String idPedido = TXTidPedido.getText().trim();
        if (idPedido.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Ingrese el id de pedido para buscar.");
            return;
        }

        String sql = "SELECT * FROM tbl_reclamacion WHERE id_pedido = ?";

        try (Connection con = conexion.establecerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, Integer.parseInt(idPedido));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    TXTasiste.setText(String.valueOf(rs.getInt("id_empleado")));
                    idClienteSeleccionado = rs.getInt("id_cliente");
                    lblIdCliente.setText("ID cliente: " + idClienteSeleccionado);
                    cmbEstado.setValue(rs.getString("estado"));
                    TXTdescripcion.setText(rs.getString("desc_reclamacion"));
                } else {
                    JOptionPane.showMessageDialog(null, "No se encontró la reclamación.");
                }
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error: " + e.getMessage());
        }
    }

    @FXML
    public void FnEditar() {
        if (!Permisos_Util.verificarEditar()) return;

        String idPedido = TXTidPedido.getText().trim();
        if (idPedido.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Id de pedido obligatorio para editar.");
            return;
        }

        String sql = "UPDATE tbl_reclamacion SET estado = ?, desc_reclamacion = ? WHERE id_pedido = ?";

        try (Connection con = conexion.establecerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, cmbEstado.getValue());
            ps.setString(2, TXTdescripcion.getText().trim());
            ps.setInt(3, Integer.parseInt(idPedido));
            if (ps.executeUpdate() > 0) {
                JOptionPane.showMessageDialog(null, "Reclamación actualizada.");
            } else {
                JOptionPane.showMessageDialog(null, "No se encontró la reclamación.");
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error: " + e.getMessage());
        }
    }

    @FXML
    public void FnEliminar() {
        if (!Permisos_Util.verificarEliminar()) return;

        String idPedido = TXTidPedido.getText().trim();
        if (idPedido.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Ingrese el id de pedido a eliminar.");
            return;
        }

        int c = JOptionPane.showConfirmDialog(null, "¿Eliminar reclamación?",
                "Confirmar", JOptionPane.YES_NO_OPTION);
        if (c != JOptionPane.YES_OPTION) return;

        try (Connection con = conexion.establecerConexion();
             PreparedStatement ps = con.prepareStatement(
                     "DELETE FROM tbl_reclamacion WHERE id_pedido = ?")) {

            ps.setInt(1, Integer.parseInt(idPedido));
            if (ps.executeUpdate() > 0) {
                JOptionPane.showMessageDialog(null, "Reclamación eliminada.");
                limpiar();
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error: " + e.getMessage());
        }
    }

    @FXML
    public void FnExportarPDF() {
        JasperUtil.exportarPDF(
                "/com/example/demo1/reportes/Reporte_Reclamaciones.jrxml",
                "Reporte_Reclamaciones.pdf"
        );
    }

    public void limpiar() {
        // No limpiar asiste ni cliente si son del usuario logueado
        if (!TXTasiste.isDisabled()) TXTasiste.clear();
        if (!TXTclienteNombre.isDisabled()) {
            TXTclienteNombre.clear();
            lblIdCliente.setText("ID cliente: —");
            idClienteSeleccionado = -1;
        }
        TXTidPedido.clear();
        TXTdescripcion.clear();
        cmbEstado.getSelectionModel().clearSelection();
    }
}
