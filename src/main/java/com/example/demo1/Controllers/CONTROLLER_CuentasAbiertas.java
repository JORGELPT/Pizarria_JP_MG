package com.example.demo1.Controllers;

import com.example.demo1.Database.Conexion;
import com.example.demo1.Utils.Permisos_Util;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javax.swing.JOptionPane;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class CONTROLLER_CuentasAbiertas {

    Conexion conexion = new Conexion();

    @FXML private TextField txtBuscar;
    @FXML private TableView<CuentaRow> tablaCuentas;
    @FXML private TableColumn<CuentaRow, String> colId;
    @FXML private TableColumn<CuentaRow, String> colCliente;
    @FXML private TableColumn<CuentaRow, String> colFecha;
    @FXML private TableColumn<CuentaRow, String> colTotal;
    @FXML private TableColumn<CuentaRow, String> colEntrega;

    @FXML private VBox vboxPago;
    @FXML private Label lblPedidoSel;
    @FXML private Label lblTotalPago;

    @FXML private ToggleButton tglEfectivo;
    @FXML private ToggleButton tglTarjeta;
    @FXML private ToggleButton tglEWallet;

    @FXML private VBox vboxPagoEfectivo;
    @FXML private Label lblMontoEfectivo;
    @FXML private TextField txtCantidadRecibida;
    @FXML private Label lblCambio;

    @FXML private VBox vboxPagoTarjeta;
    @FXML private Label lblMontoTarjeta;
    @FXML private TextField txtNombreTarjeta;
    @FXML private TextField txtNumeroTarjeta;
    @FXML private TextField txtVencimiento;
    @FXML private TextField txtCVV;
    @FXML private Label lblEstadoTarjeta;

    @FXML private VBox vboxPagoEWallet;
    @FXML private Label lblMontoEWallet;
    @FXML private TextField txtReferenciaEWallet;
    @FXML private Label lblEstadoEWallet;

    @FXML private Label lblMensaje;

    private int idPedidoSeleccionado = -1;
    private double totalSeleccionado = 0;
    private String metodoPago = "Efectivo";
    private boolean pagoTarjetaConfirmado = false;
    private boolean pagoEWalletConfirmado = false;

    private static final String TGL_ACTIVO =
            "-fx-background-color: #be1e1e; -fx-text-fill: white; -fx-font-weight: bold;" +
            "-fx-background-radius: 20; -fx-cursor: hand; -fx-font-size: 12px;" +
            "-fx-padding: 7 0; -fx-border-color: transparent;" +
            "-fx-effect: dropshadow(gaussian,rgba(190,30,30,0.40),8,0,0,2);";
    private static final String TGL_INACTIVO =
            "-fx-background-color: rgba(0,0,0,0.09); -fx-text-fill: rgba(0,0,0,0.60);" +
            "-fx-background-radius: 20; -fx-padding: 7 0; -fx-cursor: hand; -fx-font-size: 12px;" +
            "-fx-border-color: transparent;";

    @FXML
    public void initialize() {
        colId.setCellValueFactory(c -> c.getValue().id);
        colCliente.setCellValueFactory(c -> c.getValue().cliente);
        colFecha.setCellValueFactory(c -> c.getValue().fecha);
        colTotal.setCellValueFactory(c -> c.getValue().total);
        colEntrega.setCellValueFactory(c -> c.getValue().entrega);

        tablaCuentas.getSelectionModel().selectedItemProperty().addListener(
                (obs, old, sel) -> {
                    if (sel == null) return;
                    idPedidoSeleccionado = Integer.parseInt(sel.id.get());
                    totalSeleccionado = Double.parseDouble(sel.total.get().replace(",", ""));
                    lblPedidoSel.setText("#" + idPedidoSeleccionado);
                    lblTotalPago.setText("RD$ " + sel.total.get());

                    lblMontoEfectivo.setText("RD$ " + sel.total.get());
                    lblMontoTarjeta.setText("RD$ " + sel.total.get());
                    lblMontoEWallet.setText("RD$ " + sel.total.get());
                    vboxPago.setVisible(true);
                    vboxPago.setManaged(true);
                });

        cargarCuentasAbiertas();
    }

    private void cargarCuentasAbiertas() {
        ObservableList<CuentaRow> datos = FXCollections.observableArrayList();
        String sql = "SELECT p.id_pedido, per.nombre as cliente, " +
                "       CONVERT(VARCHAR, p.fecha_pedido, 103) as fecha, " +
                "       FORMAT(p.precio_total, 'N2') as total, p.tipo_entrega " +
                "FROM tbl_pedido p " +
                "INNER JOIN tbl_cliente c ON p.id_cliente = c.id_cliente " +
                "INNER JOIN tbl_persona per ON c.id_persona = per.id_persona " +
                "WHERE p.metodo_pago = 'Cuenta Abierta' " +
                "ORDER BY p.id_pedido DESC";

        try (Connection con = conexion.establecerConexion();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                datos.add(new CuentaRow(
                        String.valueOf(rs.getInt("id_pedido")),
                        rs.getString("cliente"),
                        rs.getString("fecha"),
                        rs.getString("total"),
                        rs.getString("tipo_entrega")));
            }
            tablaCuentas.setItems(datos);

        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error al cargar cuentas abiertas: " + e.getMessage());
        }
    }

    @FXML
    private void FnBuscar() {
        String filtro = txtBuscar.getText().trim();
        if (filtro.isEmpty()) {
            cargarCuentasAbiertas();
            return;
        }

        ObservableList<CuentaRow> datos = FXCollections.observableArrayList();
        String sql = "SELECT p.id_pedido, per.nombre as cliente, " +
                "       CONVERT(VARCHAR, p.fecha_pedido, 103) as fecha, " +
                "       FORMAT(p.precio_total, 'N2') as total, p.tipo_entrega " +
                "FROM tbl_pedido p " +
                "INNER JOIN tbl_cliente c ON p.id_cliente = c.id_cliente " +
                "INNER JOIN tbl_persona per ON c.id_persona = per.id_persona " +
                "WHERE p.metodo_pago = 'Cuenta Abierta' " +
                "  AND (per.nombre LIKE ? OR per.cedula LIKE ?) " +
                "ORDER BY p.id_pedido DESC";

        try (Connection con = conexion.establecerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, "%" + filtro + "%");
            ps.setString(2, "%" + filtro + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    datos.add(new CuentaRow(
                            String.valueOf(rs.getInt("id_pedido")),
                            rs.getString("cliente"),
                            rs.getString("fecha"),
                            rs.getString("total"),
                            rs.getString("tipo_entrega")));
                }
                tablaCuentas.setItems(datos);
                if (datos.isEmpty()) {
                    lblMensaje.setText("No se encontraron cuentas con ese criterio.");
                } else {
                    lblMensaje.setText("");
                }
            }
        } catch (Exception e) {
            lblMensaje.setText("Error al buscar: " + e.getMessage());
        }
    }

    @FXML
    private void FnMostrarTodas() {
        txtBuscar.clear();
        cargarCuentasAbiertas();
        lblMensaje.setText("");
    }

    @FXML
    private void FnPagoEfectivo() {
        metodoPago = "Efectivo";
        actualizarPillPago(tglEfectivo);
        vboxPagoEfectivo.setVisible(true);
        vboxPagoEfectivo.setManaged(true);
        vboxPagoTarjeta.setVisible(false);
        vboxPagoTarjeta.setManaged(false);
        vboxPagoEWallet.setVisible(false);
        vboxPagoEWallet.setManaged(false);
    }

    @FXML
    private void FnPagoTarjeta() {
        metodoPago = "Tarjeta";
        pagoTarjetaConfirmado = false;
        actualizarPillPago(tglTarjeta);
        vboxPagoEfectivo.setVisible(false);
        vboxPagoEfectivo.setManaged(false);
        vboxPagoTarjeta.setVisible(true);
        vboxPagoTarjeta.setManaged(true);
        vboxPagoEWallet.setVisible(false);
        vboxPagoEWallet.setManaged(false);
    }

    @FXML
    private void FnPagoEWallet() {
        metodoPago = "E-Wallet";
        pagoEWalletConfirmado = false;
        actualizarPillPago(tglEWallet);
        vboxPagoEfectivo.setVisible(false);
        vboxPagoEfectivo.setManaged(false);
        vboxPagoTarjeta.setVisible(false);
        vboxPagoTarjeta.setManaged(false);
        vboxPagoEWallet.setVisible(true);
        vboxPagoEWallet.setManaged(true);
    }

    @FXML
    private void FnCalcularCambio() {
        if (txtCantidadRecibida == null) return;
        try {
            double recibido = Double.parseDouble(txtCantidadRecibida.getText().trim());
            double cambio = recibido - totalSeleccionado;
            lblCambio.setText(String.format("RD$ %.2f", Math.max(cambio, 0)));
        } catch (NumberFormatException e) {
            lblCambio.setText("RD$ 0.00");
        }
    }

    @FXML
    private void FnConfirmarTarjeta() {
        if (txtNombreTarjeta == null || txtNumeroTarjeta == null ||
            txtVencimiento == null || txtCVV == null) return;
        String nom = txtNombreTarjeta.getText().trim();
        String num = txtNumeroTarjeta.getText().trim();
        String ven = txtVencimiento.getText().trim();
        String cvv = txtCVV.getText().trim();
        if (nom.isEmpty() || num.isEmpty() || ven.isEmpty() || cvv.isEmpty()) {
            lblEstadoTarjeta.setText("Complete todos los campos de la tarjeta.");
            return;
        }
        pagoTarjetaConfirmado = true;
        lblEstadoTarjeta.setText("✅ Tarjeta confirmada.");
    }

    @FXML
    private void FnConfirmarEWallet() {
        if (txtReferenciaEWallet == null) return;
        String ref = txtReferenciaEWallet.getText().trim();
        if (ref.isEmpty()) {
            lblEstadoEWallet.setText("Ingrese el número de referencia.");
            return;
        }
        pagoEWalletConfirmado = true;
        lblEstadoEWallet.setText("✅ Referencia registrada.");
    }

    @FXML
    private void FnProcedePago() {
        if (idPedidoSeleccionado == -1) {
            lblMensaje.setText("Seleccione una cuenta abierta de la tabla.");
            return;
        }

        if ("Efectivo".equals(metodoPago)) {
            String recibidoStr = txtCantidadRecibida != null
                    ? txtCantidadRecibida.getText().trim() : "";
            if (recibidoStr.isEmpty()) {
                lblMensaje.setText("Ingrese la cantidad recibida en efectivo.");
                return;
            }
            try {
                BigDecimal recibido = new BigDecimal(recibidoStr);
                if (recibido.compareTo(BigDecimal.valueOf(totalSeleccionado)) < 0) {
                    lblMensaje.setText("La cantidad recibida es menor al total.");
                    return;
                }
            } catch (NumberFormatException e) {
                lblMensaje.setText("Cantidad recibida no válida.");
                return;
            }
        } else if ("Tarjeta".equals(metodoPago) && !pagoTarjetaConfirmado) {
            lblMensaje.setText("Confirme el pago con tarjeta antes de continuar.");
            return;
        } else if ("E-Wallet".equals(metodoPago) && !pagoEWalletConfirmado) {
            lblMensaje.setText("Confirme el pago E-Wallet antes de continuar.");
            return;
        }

        String sql = "UPDATE tbl_pedido SET metodo_pago = ?, estado = 'completado' WHERE id_pedido = ? AND metodo_pago = 'Cuenta Abierta'";

        try (Connection con = conexion.establecerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, metodoPago);
            ps.setInt(2, idPedidoSeleccionado);
            int rows = ps.executeUpdate();

            if (rows > 0) {
                JOptionPane.showMessageDialog(null,
                        "✅ Pago procesado correctamente para el pedido #" + idPedidoSeleccionado);
                idPedidoSeleccionado = -1;
                totalSeleccionado = 0;
                lblPedidoSel.setText("—");
                lblTotalPago.setText("RD$ 0.00");
                vboxPago.setVisible(false);
                vboxPago.setManaged(false);
                limpiarPago();
                cargarCuentasAbiertas();
            } else {
                lblMensaje.setText("La cuenta ya fue pagada o no existe.");
            }
        } catch (Exception e) {
            lblMensaje.setText("Error al procesar pago: " + e.getMessage());
        }
    }

    private void actualizarPillPago(ToggleButton target) {
        for (ToggleButton tb : new ToggleButton[]{tglEfectivo, tglTarjeta, tglEWallet}) {
            if (tb != null) tb.setStyle(tb == target ? TGL_ACTIVO : TGL_INACTIVO);
        }
    }

    private void limpiarPago() {
        metodoPago = "Efectivo";
        pagoTarjetaConfirmado = false;
        pagoEWalletConfirmado = false;
        actualizarPillPago(tglEfectivo);
        if (txtCantidadRecibida != null) txtCantidadRecibida.clear();
        if (lblCambio != null) lblCambio.setText("RD$ 0.00");
        if (txtNombreTarjeta != null) txtNombreTarjeta.clear();
        if (txtNumeroTarjeta != null) txtNumeroTarjeta.clear();
        if (txtVencimiento != null) txtVencimiento.clear();
        if (txtCVV != null) txtCVV.clear();
        if (lblEstadoTarjeta != null) lblEstadoTarjeta.setText("");
        if (txtReferenciaEWallet != null) txtReferenciaEWallet.clear();
        if (lblEstadoEWallet != null) lblEstadoEWallet.setText("");
        vboxPagoEfectivo.setVisible(false);
        vboxPagoEfectivo.setManaged(false);
        vboxPagoTarjeta.setVisible(false);
        vboxPagoTarjeta.setManaged(false);
        vboxPagoEWallet.setVisible(false);
        vboxPagoEWallet.setManaged(false);
    }

    public static class CuentaRow {
        final SimpleStringProperty id, cliente, fecha, total, entrega;

        public CuentaRow(String id, String cliente, String fecha, String total, String entrega) {
            this.id = new SimpleStringProperty(id);
            this.cliente = new SimpleStringProperty(cliente);
            this.fecha = new SimpleStringProperty(fecha);
            this.total = new SimpleStringProperty(total);
            this.entrega = new SimpleStringProperty(entrega);
        }

        public String getId() { return id.get(); }
        public String getCliente() { return cliente.get(); }
        public String getFecha() { return fecha.get(); }
        public String getTotal() { return total.get(); }
        public String getEntrega() { return entrega.get(); }
    }
}
