package com.example.demo1.Controllers;

import com.example.demo1.Database.Conexion;
import com.example.demo1.Utils.CorreoUtil;
import com.example.demo1.Utils.JasperUtil;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javax.swing.JOptionPane;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.HashMap;

public class CONTROLLER_ComprasPago {

    Conexion conexion = new Conexion();

    //  Campos del formulario
    @FXML private ComboBox<String> cmbProveedor;
    @FXML private ComboBox<String> cmbSucursal;
    @FXML private ComboBox<String> cmbEstado;
    @FXML private TextField TXTfecha;
    @FXML private TextField TXTmonto;
    @FXML private TextField TXTmontoPendiente;
    @FXML private TextField TXTprecio;
    @FXML private TextField TXTcantidad;
    @FXML private TextField TXTobservacion;

    //  Panel de pago
    @FXML private TextField txtAbono;
    @FXML private Label     lblFacturaId;
    @FXML private Label     lblInfoPago;

    //  Tabla
    @FXML private TableView<CompraRow>           tablaCompras;
    @FXML private TableColumn<CompraRow, String> colFactura;
    @FXML private TableColumn<CompraRow, String> colProveedor;
    @FXML private TableColumn<CompraRow, String> colFecha;
    @FXML private TableColumn<CompraRow, String> colMonto;
    @FXML private TableColumn<CompraRow, String> colMontoPendiente;
    @FXML private TableColumn<CompraRow, String> colCantidad;
    @FXML private TableColumn<CompraRow, String> colEstado;

    //  Estado
    private int idCompraSeleccionada = -1;

    //  Inicialización
    @FXML
    public void initialize() {
        colFactura.setCellValueFactory(c        -> c.getValue().facturaId);
        colProveedor.setCellValueFactory(c      -> c.getValue().proveedor);
        colFecha.setCellValueFactory(c          -> c.getValue().fecha);
        colMonto.setCellValueFactory(c          -> c.getValue().monto);
        colMontoPendiente.setCellValueFactory(c -> c.getValue().montoPendiente);
        colCantidad.setCellValueFactory(c       -> c.getValue().cantidad);
        colEstado.setCellValueFactory(c         -> c.getValue().estado);

        cmbEstado.setItems(FXCollections.observableArrayList(
                "Pendiente", "Pagado", "Parcial", "Cancelado"));

        // Al seleccionar fila → rellenar formulario
        tablaCompras.getSelectionModel().selectedItemProperty().addListener(
                (obs, old, sel) -> {
                    if (sel == null) return;
                    idCompraSeleccionada = sel.idCompra;
                    TXTfecha.setText(sel.fecha.get());
                    TXTmonto.setText(sel.monto.get());
                    TXTmontoPendiente.setText(sel.montoPendiente.get());
                    TXTcantidad.setText(sel.cantidad.get());
                    cmbEstado.setValue(sel.estado.get());
                    cmbProveedor.setValue(sel.proveedor.get());
                    // Mostrar el número de factura de la fila seleccionada
                    if (lblFacturaId != null) lblFacturaId.setText(sel.facturaId.get());
                    if (lblInfoPago  != null) lblInfoPago.setText("");
                });

        cargarProveedores();
        cargarSucursales();
        cargarTabla();
    }

    //  Cargar combos

    private void cargarProveedores() {
        ObservableList<String> lista = FXCollections.observableArrayList();
        try (Connection con = conexion.establecerConexion();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT nombre_proveedor FROM tbl_proveedor ORDER BY nombre_proveedor");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) lista.add(rs.getString("nombre_proveedor"));
            cmbProveedor.setItems(lista);
        } catch (Exception e) { //error al cargar proveedores - pantalla Compras y Pago
            JOptionPane.showMessageDialog(null, "Error al cargar proveedores: " + e.getMessage());
        }
    }

    private void cargarSucursales() {
        ObservableList<String> lista = FXCollections.observableArrayList();
        try (Connection con = conexion.establecerConexion();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT nombre_sucursal FROM tbl_sucursal ORDER BY nombre_sucursal");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) lista.add(rs.getString("nombre_sucursal"));
            cmbSucursal.setItems(lista);
        } catch (Exception e) { //error al cargar sucursales - pantalla Compras y Pago
            JOptionPane.showMessageDialog(null, "Error al cargar sucursales: " + e.getMessage());
        }
    }

    //  Helpers BD

    private int resolverIdProveedor(Connection con, String nombre) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT id_proveedor FROM tbl_proveedor WHERE nombre_proveedor = ?")) {
            ps.setString(1, nombre);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getInt("id_proveedor") : -1;
        }
    }

    private int resolverIdSucursal(Connection con, String nombre) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT id_sucursal FROM tbl_sucursal WHERE nombre_sucursal = ?")) {
            ps.setString(1, nombre);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getInt("id_sucursal") : -1;
        }
    }

    private String emailProveedor(Connection con, int idProv) {
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT email FROM tbl_proveedor WHERE id_proveedor = ?")) {
            ps.setInt(1, idProv);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String email = rs.getString("email");
                return (email != null && !email.isBlank()) ? email : null;
            }
        } catch (Exception ignored) {}
        return null;
    }

    //  Guardar compra
    @FXML
    public void FnGuardar() {
        String proveedor    = cmbProveedor.getValue();
        String sucursal     = cmbSucursal.getValue();
        String estado       = cmbEstado.getValue();
        String fecha        = TXTfecha.getText().trim();
        String montoStr     = TXTmonto.getText().trim();
        String pendienteStr = TXTmontoPendiente.getText().trim();
        String precioStr    = TXTprecio.getText().trim();
        String cantidadStr  = TXTcantidad.getText().trim();
        String observacion  = TXTobservacion.getText().trim();

        if (proveedor == null || fecha.isEmpty() || montoStr.isEmpty() || cantidadStr.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Proveedor, fecha, monto y cantidad son obligatorios.");
            return;
        }

        String sql = "INSERT INTO tbl_compra " +
                "(id_proveedor, monto, monto_pendiente, fecha, cantidad, id_sucursal, precio, observacion, estado) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection con = conexion.establecerConexion();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            int idProv = resolverIdProveedor(con, proveedor);
            if (idProv == -1) {
                JOptionPane.showMessageDialog(null, "No se encontró el proveedor seleccionado.");
                return;
            }
            int idSuc = sucursal != null ? resolverIdSucursal(con, sucursal) : 1;

            ps.setInt(1, idProv);
            ps.setBigDecimal(2, new BigDecimal(montoStr));
            ps.setBigDecimal(3, pendienteStr.isEmpty() ? BigDecimal.ZERO : new BigDecimal(pendienteStr));
            ps.setDate(4, java.sql.Date.valueOf(fecha));
            ps.setInt(5, Integer.parseInt(cantidadStr));
            ps.setInt(6, idSuc == -1 ? 1 : idSuc);
            ps.setDouble(7, precioStr.isEmpty() ? 0 : Double.parseDouble(precioStr));
            ps.setString(8, observacion.isEmpty() ? null : observacion);
            ps.setString(9, estado != null ? estado : "Pendiente");
            ps.executeUpdate();

            //  Capturar el ID generado → número de factura
            int nuevoId = -1;
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) nuevoId = keys.getInt(1);
            }
            idCompraSeleccionada = nuevoId;

            String numFactura = formatearFactura(nuevoId);
            if (lblFacturaId != null) lblFacturaId.setText(numFactura);
            if (lblInfoPago  != null) lblInfoPago.setText("✅ Compra registrada");

            JOptionPane.showMessageDialog(null,
                    "Compra registrada correctamente.\nN° Factura: " + numFactura);
            limpiarFormulario();
            cargarTabla();

        } catch (Exception e) { //error al guardar compra - pantalla Compras y Pago
            JOptionPane.showMessageDialog(null, "Error al guardar: " + e.getMessage());
        }
    }

    //  Pagar / Abonar al monto pendiente

    @FXML
    public void FnPagar() {
        if (idCompraSeleccionada == -1) {
            JOptionPane.showMessageDialog(null,
                    "Seleccione una compra de la tabla primero.");
            return;
        }

        String abonoStr = txtAbono != null ? txtAbono.getText().trim() : "";
        if (abonoStr.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Ingrese el monto a abonar.");
            return;
        }

        BigDecimal abono;
        try {
            abono = new BigDecimal(abonoStr);
            if (abono.compareTo(BigDecimal.ZERO) <= 0)
                throw new NumberFormatException();
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(null, "El monto a abonar no es válido.");
            return;
        }

        Connection con = null;
        try {
            con = conexion.establecerConexion();
            con.setAutoCommit(false);

            // Leer estado actual
            BigDecimal pendienteActual = BigDecimal.ZERO;
            String   proveedorNombre  = "";
            int      idProvActual     = -1;
            BigDecimal montoTotal     = BigDecimal.ZERO;

            try (PreparedStatement ps = con.prepareStatement(
                    "SELECT c.monto_pendiente, c.monto, c.id_proveedor, p.nombre_proveedor " +
                    "FROM tbl_compra c " +
                    "JOIN tbl_proveedor p ON p.id_proveedor = c.id_proveedor " +
                    "WHERE c.id_compra = ?")) {
                ps.setInt(1, idCompraSeleccionada);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        pendienteActual = rs.getBigDecimal("monto_pendiente");
                        montoTotal      = rs.getBigDecimal("monto");
                        idProvActual    = rs.getInt("id_proveedor");
                        proveedorNombre = rs.getString("nombre_proveedor");
                        if (pendienteActual == null) pendienteActual = BigDecimal.ZERO;
                        if (montoTotal      == null) montoTotal      = BigDecimal.ZERO;
                    }
                }
            }

            if (abono.compareTo(pendienteActual) > 0) {
                JOptionPane.showMessageDialog(null,
                        "El abono (RD$ " + abono.toPlainString() + ") supera el monto pendiente " +
                        "(RD$ " + pendienteActual.toPlainString() + ").");
                con.rollback();
                return;
            }

            BigDecimal nuevoPendiente = pendienteActual.subtract(abono);
            String nuevoEstado = nuevoPendiente.compareTo(BigDecimal.ZERO) <= 0
                    ? "Pagado" : "Parcial";

            // Actualizar compra
            try (PreparedStatement ps = con.prepareStatement(
                    "UPDATE tbl_compra SET monto_pendiente = ?, estado = ? WHERE id_compra = ?")) {
                ps.setBigDecimal(1, nuevoPendiente);
                ps.setString(2, nuevoEstado);
                ps.setInt(3, idCompraSeleccionada);
                ps.executeUpdate();
            }

            con.commit();

            //  Número de factura del pago
            String numFactura = formatearFactura(idCompraSeleccionada);
            if (lblFacturaId != null) lblFacturaId.setText(numFactura);
            if (lblInfoPago  != null) {
                lblInfoPago.setText("✅ Abonado RD$ " + abono.toPlainString()
                        + " — Estado: " + nuevoEstado);
            }

            //  Correo de notificación
            String emailProv = emailProveedor(con, idProvActual);
            CorreoUtil.notificarPagoProveedor(
                    numFactura,
                    proveedorNombre,
                    emailProv,
                    idCompraSeleccionada,
                    montoTotal,
                    abono,
                    nuevoPendiente,
                    nuevoEstado,
                    LocalDate.now().toString()
            );

            JOptionPane.showMessageDialog(null,
                    "Pago registrado correctamente.\n" +
                    "N° Factura: " + numFactura + "\n" +
                    "Abono: RD$ " + abono.toPlainString() + "\n" +
                    "Pendiente restante: RD$ " + nuevoPendiente.toPlainString() + "\n" +
                    "Estado: " + nuevoEstado);

            if (txtAbono != null) txtAbono.clear();
            cargarTabla();

        } catch (Exception e) { //error al registrar pago de compra - pantalla Compras y Pago
            try { if (con != null) con.rollback(); } catch (Exception ignored) {}
            JOptionPane.showMessageDialog(null, "Error al registrar pago: " + e.getMessage());
        } finally {
            try { if (con != null) con.close(); } catch (Exception ignored) {}
        }
    }

    //    Generar Factura PDF de la compra seleccionada

    @FXML
    public void FnGenerarFacturaPago() {
        if (idCompraSeleccionada == -1) {
            JOptionPane.showMessageDialog(null,
                    "Seleccione o guarde una compra primero.");
            return;
        }
        HashMap<String, Object> params = new HashMap<>();
        params.put("ID_COMPRA", idCompraSeleccionada);
        JasperUtil.exportarPDF(
                "/com/example/demo1/reportes/Reporte_Compras.jrxml",
                "Factura_Compra_" + formatearFactura(idCompraSeleccionada) + ".pdf",
                params
        );
    }

    // Eliminar
    @FXML
    public void FnEliminar() {
        if (!com.example.demo1.Utils.Permisos_Util.verificarEliminar()) return;
        if (idCompraSeleccionada == -1) {
            JOptionPane.showMessageDialog(null, "Seleccione una compra de la tabla primero.");
            return;
        }
        int confirmar = JOptionPane.showConfirmDialog(null,
                "¿Está seguro de eliminar esta compra?",
                "Confirmar eliminación", JOptionPane.YES_NO_OPTION);
        if (confirmar != JOptionPane.YES_OPTION) return;

        try (Connection con = conexion.establecerConexion();
             PreparedStatement ps = con.prepareStatement(
                     "DELETE FROM tbl_compra WHERE id_compra = ?")) {
            ps.setInt(1, idCompraSeleccionada);
            ps.executeUpdate();
            JOptionPane.showMessageDialog(null, "Compra eliminada correctamente.");
            idCompraSeleccionada = -1;
            limpiarFormulario();
            cargarTabla();
        } catch (Exception e) { //error al eliminar compra - pantalla Compras y Pago
            JOptionPane.showMessageDialog(null, "Error al eliminar: " + e.getMessage());
        }
    }

    // ── Inhabilitar / Habilitar ──────────────────────────────────────────────

    @FXML
    public void FnInhabilitar() {
        cambiarEstadoCompra("Inactivo", "Compra inhabilitada correctamente.");
    }

    @FXML
    public void FnHabilitar() {
        cambiarEstadoCompra("Activo", "Compra habilitada correctamente.");
    }

    private void cambiarEstadoCompra(String nuevoEstado, String mensaje) {
        if (idCompraSeleccionada == -1) {
            JOptionPane.showMessageDialog(null, "Seleccione una compra de la tabla primero.");
            return;
        }
        int confirmar = JOptionPane.showConfirmDialog(null,
                "¿" + mensaje + "?", "Confirmar", JOptionPane.YES_NO_OPTION);
        if (confirmar != JOptionPane.YES_OPTION) return;

        try (Connection con = conexion.establecerConexion();
             PreparedStatement ps = con.prepareStatement(
                     "UPDATE tbl_compra SET estado = ? WHERE id_compra = ?")) {
            ps.setString(1, nuevoEstado);
            ps.setInt(2, idCompraSeleccionada);
            ps.executeUpdate();
            JOptionPane.showMessageDialog(null, mensaje);
            idCompraSeleccionada = -1;
            limpiarFormulario();
            cargarTabla();
        } catch (Exception e) { //error al cambiar estado de compra - pantalla Compras y Pago
            JOptionPane.showMessageDialog(null, "Error: " + e.getMessage());
        }
    }

    //  Editar

    @FXML
    public void FnEditar() {
        if (idCompraSeleccionada == -1) {
            JOptionPane.showMessageDialog(null, "Seleccione una compra de la tabla primero.");
            return;
        }
        String proveedor    = cmbProveedor.getValue();
        String estado       = cmbEstado.getValue();
        String fecha        = TXTfecha.getText().trim();
        String montoStr     = TXTmonto.getText().trim();
        String pendienteStr = TXTmontoPendiente.getText().trim();
        String precioStr    = TXTprecio.getText().trim();
        String cantidadStr  = TXTcantidad.getText().trim();
        String observacion  = TXTobservacion.getText().trim();

        if (proveedor == null || fecha.isEmpty() || montoStr.isEmpty() || cantidadStr.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Proveedor, fecha, monto y cantidad son obligatorios.");
            return;
        }

        String sql = "UPDATE tbl_compra SET id_proveedor=?, monto=?, monto_pendiente=?, fecha=?, " +
                "cantidad=?, precio=?, observacion=?, estado=? WHERE id_compra=?";
        try (Connection con = conexion.establecerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            int idProv = resolverIdProveedor(con, proveedor);
            if (idProv == -1) {
                JOptionPane.showMessageDialog(null, "No se encontró el proveedor seleccionado.");
                return;
            }
            ps.setInt(1, idProv);
            ps.setBigDecimal(2, new BigDecimal(montoStr));
            ps.setBigDecimal(3, pendienteStr.isEmpty() ? BigDecimal.ZERO : new BigDecimal(pendienteStr));
            ps.setDate(4, java.sql.Date.valueOf(fecha));
            ps.setInt(5, Integer.parseInt(cantidadStr));
            ps.setDouble(6, precioStr.isEmpty() ? 0 : Double.parseDouble(precioStr));
            ps.setString(7, observacion.isEmpty() ? null : observacion);
            ps.setString(8, estado);
            ps.setInt(9, idCompraSeleccionada);
            ps.executeUpdate();
            JOptionPane.showMessageDialog(null, "Compra actualizada correctamente.");
            idCompraSeleccionada = -1;
            limpiarFormulario();
            cargarTabla();
        } catch (Exception e) { //error al actualizar compra - pantalla Compras y Pago
            JOptionPane.showMessageDialog(null, "Error al actualizar: " + e.getMessage());
        }
    }

    //  Buscar

    @FXML
    public void FnBuscar() {
        String proveedor = cmbProveedor.getValue();
        ObservableList<CompraRow> datos = FXCollections.observableArrayList();

        String sql = "SELECT c.id_compra, p.nombre_proveedor, c.fecha, c.monto, c.monto_pendiente, " +
                "c.cantidad, c.estado " +
                "FROM tbl_compra c " +
                "INNER JOIN tbl_proveedor p ON c.id_proveedor = p.id_proveedor ";
        if (proveedor != null && !proveedor.isEmpty())
            sql += "WHERE p.nombre_proveedor LIKE ? ";
        sql += "ORDER BY c.fecha DESC";

        try (Connection con = conexion.establecerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            if (proveedor != null && !proveedor.isEmpty())
                ps.setString(1, "%" + proveedor + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) datos.add(filaDesdeRS(rs));
            }
            tablaCompras.setItems(datos);
        } catch (Exception e) { //error al buscar compras - pantalla Compras y Pago
            JOptionPane.showMessageDialog(null, "Error al buscar: " + e.getMessage());
        }
    }

    //  Cargar tabla

    private void cargarTabla() {
        ObservableList<CompraRow> datos = FXCollections.observableArrayList();
        String sql = "SELECT c.id_compra, p.nombre_proveedor, c.fecha, c.monto, c.monto_pendiente, " +
                "c.cantidad, c.estado " +
                "FROM tbl_compra c " +
                "INNER JOIN tbl_proveedor p ON c.id_proveedor = p.id_proveedor " +
                "ORDER BY c.fecha DESC";
        try (Connection con = conexion.establecerConexion();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) datos.add(filaDesdeRS(rs));
            tablaCompras.setItems(datos);
        } catch (Exception e) { //error al cargar tabla de compras - pantalla Compras y Pago
            JOptionPane.showMessageDialog(null, "Error al cargar compras: " + e.getMessage());
        }
    }

    private CompraRow filaDesdeRS(ResultSet rs) throws SQLException {
        return new CompraRow(
                rs.getInt("id_compra"),
                rs.getString("nombre_proveedor"),
                rs.getDate("fecha") != null ? rs.getDate("fecha").toString() : "",
                rs.getBigDecimal("monto")          != null ? rs.getBigDecimal("monto").toPlainString()          : "",
                rs.getBigDecimal("monto_pendiente") != null ? rs.getBigDecimal("monto_pendiente").toPlainString() : "",
                String.valueOf(rs.getInt("cantidad")),
                rs.getString("estado") != null ? rs.getString("estado") : ""
        );
    }

    //  Limpiar formulario

    private void limpiarFormulario() {
        cmbProveedor.getSelectionModel().clearSelection();
        cmbSucursal.getSelectionModel().clearSelection();
        cmbEstado.getSelectionModel().clearSelection();
        TXTfecha.clear(); TXTmonto.clear(); TXTmontoPendiente.clear();
        TXTprecio.clear(); TXTcantidad.clear(); TXTobservacion.clear();
        if (txtAbono   != null) txtAbono.clear();
        if (lblInfoPago != null) lblInfoPago.setText("");
    }

    //  Reportes

    @FXML
    public void FnExportarReporteCompras() {
        if (!com.example.demo1.Utils.Permisos_Util.verificarReporte()) return;
        JasperUtil.exportarPDF(
                "/com/example/demo1/reportes/Reporte_Compras.jrxml",
                "Reporte_Compras.pdf"
        );
    }

    @FXML
    public void FnExportarReporteComprasVentas() {
        if (!com.example.demo1.Utils.Permisos_Util.verificarReporte()) return;
        JasperUtil.exportarPDF(
                "/com/example/demo1/reportes/Reporte_ComprasVentas.jrxml",
                "Reporte_ComprasVentas.pdf"
        );
    }

    //  Utilidades

    /** Formatea el id_compra como número de factura legible: FC-00042 */
    private String formatearFactura(int idCompra) {
        return "FC-" + String.format("%05d", idCompra);
    }

    //  Clase interna de fila

    public static class CompraRow {
        final int idCompra;
        final SimpleStringProperty facturaId, proveedor, fecha, monto, montoPendiente, cantidad, estado;

        public CompraRow(int idCompra, String proveedor, String fecha, String monto,
                         String montoPendiente, String cantidad, String estado) {
            this.idCompra       = idCompra;
            this.facturaId      = new SimpleStringProperty("FC-" + String.format("%05d", idCompra));
            this.proveedor      = new SimpleStringProperty(proveedor      != null ? proveedor      : "");
            this.fecha          = new SimpleStringProperty(fecha          != null ? fecha          : "");
            this.monto          = new SimpleStringProperty(monto          != null ? monto          : "");
            this.montoPendiente = new SimpleStringProperty(montoPendiente != null ? montoPendiente : "");
            this.cantidad       = new SimpleStringProperty(cantidad       != null ? cantidad       : "");
            this.estado         = new SimpleStringProperty(estado         != null ? estado         : "");
        }
    }
}
