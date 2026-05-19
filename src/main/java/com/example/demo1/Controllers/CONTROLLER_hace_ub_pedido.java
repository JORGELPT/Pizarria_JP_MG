package com.example.demo1.Controllers;

import com.example.demo1.Database.Conexion;
import com.example.demo1.Utils.CONTROLLER_Seccion;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Controlador de la pantalla "Hacer un Pedido".
 *
 * Tablas:
 *   tbl_pedido         → id_pedido (IDENTITY), tipo_entrega, tiempo_realizacion, precio_total,
 *                         fecha_pedido, id_cliente, id_empleado, id_itbs, metodo_pago
 *   tbl_producto_pedido → id_producto, id_pedido, cantidad_producto, id_presentacion, especificaciones
 *   tbl_producto        → id_producto, nombre, tipo, disponibilidad
 *   tbl_presentacion_producto → id_presentacion, id_producto, costo
 */
public class CONTROLLER_hace_ub_pedido {

    // ── FXML ─────────────────────────────────────────────────────────────────
    @FXML private Label        lblFecha;
    @FXML private TextField    txtBuscar;
    @FXML private FlowPane     flowProductos;

    @FXML private Button       btnTodos;
    @FXML private Button       btnPizza;
    @FXML private Button       btnBebida;
    @FXML private Button       btnPasta;
    @FXML private Button       btnPostre;
    @FXML private Button       btnOfertas;

    @FXML private TextField    txtCedula;
    @FXML private Label        lblCliente;
    @FXML private VBox         vboxItems;

    @FXML private ToggleButton tglEnLocal;
    @FXML private ToggleButton tglRecoger;
    @FXML private ToggleButton tglDelivery;

    @FXML private VBox         vboxDelivery;
    @FXML private ComboBox<String> cmbMetodoEnvio;
    @FXML private TextField    txtDireccion;
    @FXML private TextField    txtObservacion;

    @FXML private ComboBox<String> cmbExtra;

    @FXML private Label        lblSubtotal;
    @FXML private Label        lblDescuento;
    @FXML private Label        lblItbsNombre;
    @FXML private Label        lblItbs;
    @FXML private Label        lblTotal;

    @FXML private ToggleButton tglEfectivo;
    @FXML private ToggleButton tglTarjeta;
    @FXML private ToggleButton tglEWallet;

    @FXML private Label        lblMensaje;

    // ── Estado ───────────────────────────────────────────────────────────────
    private int    idClienteSeleccionado = -1;
    private String tipoEntrega           = "En Local";
    private String metodoPago            = "Efectivo";

    private final List<ItemCarrito>          carrito            = new ArrayList<>();
    private final ObservableList<Producto>   todosLosProductos  = FXCollections.observableArrayList();

    private static final BigDecimal TASA_ITBIS = new BigDecimal("0.18");

    // ── Inicialización ───────────────────────────────────────────────────────
    @FXML
    public void initialize() {
        try {
            if (lblFecha != null)
                lblFecha.setText(LocalDate.now()
                        .format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));

            if (cmbMetodoEnvio != null)
                cmbMetodoEnvio.setItems(
                        FXCollections.observableArrayList("Moto", "Carro", "Pie"));

            // Si el usuario es cliente, prellenar sus datos
            CONTROLLER_Seccion s = CONTROLLER_Seccion.getInstancia();
            if (s.esCliente() && s.getIdCliente() != -1) {
                idClienteSeleccionado = s.getIdCliente();
                if (lblCliente != null) lblCliente.setText(s.getNombre());
                if (txtCedula  != null) txtCedula.setDisable(true);
            }

            // Cargar catálogo de productos desde BD
            cargarProductosBD(null);

        } catch (Exception e) {
            // Nunca dejar que una excepción en initialize() rompa la carga FXML
            System.err.println("[HacerPedido] Error en initialize: " + e.getMessage());
        }
    }

    // ── Carga de productos ───────────────────────────────────────────────────

    private void cargarProductosBD(String categoria) {
        todosLosProductos.clear();

        StringBuilder sql = new StringBuilder(
                "SELECT p.id_producto, p.nombre, p.tipo, MIN(pp.costo) AS precio " +
                "FROM   tbl_producto p " +
                "LEFT JOIN tbl_presentacion_producto pp ON p.id_producto = pp.id_producto " +
                "WHERE  p.disponibilidad = 1 ");

        boolean filtrar = (categoria != null && !categoria.isBlank()
                && !categoria.equalsIgnoreCase("todos"));
        if (filtrar) sql.append("AND LOWER(p.tipo) = ? ");
        sql.append("GROUP BY p.id_producto, p.nombre, p.tipo ORDER BY p.nombre");

        Connection con = null;
        try {
            con = Conexion.establecerConexion();
            if (con == null) {
                mostrarMensaje("No se pudo conectar a la base de datos.");
                return;
            }
            try (PreparedStatement ps = con.prepareStatement(sql.toString())) {
                if (filtrar) ps.setString(1, categoria.trim().toLowerCase());

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        BigDecimal precio = rs.getBigDecimal("precio");
                        todosLosProductos.add(new Producto(
                                rs.getInt("id_producto"),
                                rs.getString("nombre"),
                                rs.getString("tipo"),
                                precio != null ? precio : BigDecimal.ZERO));
                    }
                }
            }
        } catch (Exception e) {
            mostrarMensaje("Error al cargar productos: " + e.getMessage());
            System.err.println("[HacerPedido] cargarProductosBD: " + e.getMessage());
        } finally {
            cerrarConexion(con);
        }

        mostrarCatalogo(todosLosProductos);
    }

    private void mostrarCatalogo(List<Producto> lista) {
        if (flowProductos == null) return;
        flowProductos.getChildren().clear();
        for (Producto p : lista) {
            flowProductos.getChildren().add(crearTarjeta(p));
        }
    }

    // ── Tarjeta de producto ──────────────────────────────────────────────────

    private VBox crearTarjeta(Producto p) {
        VBox card = new VBox(6);
        card.setPrefSize(165, 215);
        card.setMaxSize(165, 215);
        card.setAlignment(Pos.TOP_LEFT);

        final String estiloBase =
                "-fx-background-color: white; -fx-background-radius: 14;" +
                "-fx-effect: dropshadow(gaussian,rgba(0,0,0,0.08),10,0,0,2);" +
                "-fx-cursor: hand; -fx-padding: 10 10 10 10;";
        final String estiloHover =
                "-fx-background-color: #f0f4ff; -fx-background-radius: 14;" +
                "-fx-effect: dropshadow(gaussian,rgba(0,74,173,0.18),14,0,0,4);" +
                "-fx-cursor: hand; -fx-padding: 10 10 10 10;";
        card.setStyle(estiloBase);
        card.setOnMouseEntered(e -> card.setStyle(estiloHover));
        card.setOnMouseExited (e -> card.setStyle(estiloBase));

        // Placeholder de imagen
        StackPane imgBox = new StackPane();
        imgBox.setPrefSize(145, 95);
        imgBox.setMaxWidth(Double.MAX_VALUE);

        java.io.InputStream is = getClass().getResourceAsStream(
                "/com/example/demo1/imagenes/productos/" + p.idProducto + ".png");
        if (is == null) is = getClass().getResourceAsStream(
                "/com/example/demo1/imagenes/productos/" + p.idProducto + ".jpg");

        if (is != null) {
            try {
                javafx.scene.image.ImageView iv = new javafx.scene.image.ImageView(
                        new javafx.scene.image.Image(is));
                iv.setFitWidth(145);
                iv.setFitHeight(95);
                iv.setPreserveRatio(true);
                imgBox.setStyle("-fx-background-color: #f4f6fb; -fx-background-radius: 10;");
                imgBox.getChildren().add(iv);
            } catch (Exception ignored) {
                imgBox.setStyle("-fx-background-color: #dce6f5; -fx-background-radius: 10;");
            }
        } else {
            imgBox.setStyle("-fx-background-color: #dce6f5; -fx-background-radius: 10;");
        }

        Label badge = new Label(p.tipo != null ? p.tipo.toUpperCase() : "PRODUCTO");
        badge.setStyle("-fx-font-size: 9px; -fx-font-weight: bold; -fx-text-fill: #be1e1e;");

        Label nombre = new Label(p.nombre);
        nombre.setWrapText(true);
        nombre.setMaxWidth(Double.MAX_VALUE);
        nombre.setStyle("-fx-font-size: 12px; -fx-font-weight: bold;" +
                        "-fx-text-fill: #1a1a2e;");

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        Label precio = new Label("$" + p.precio.setScale(2, RoundingMode.HALF_UP).toPlainString());
        precio.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #004aad;");

        Button btnAgregar = new Button("+ Agregar");
        btnAgregar.setMaxWidth(Double.MAX_VALUE);
        btnAgregar.setStyle(
                "-fx-background-color: #004aad; -fx-text-fill: white;" +
                "-fx-background-radius: 8; -fx-cursor: hand;" +
                "-fx-font-size: 11px; -fx-padding: 6 0;");
        btnAgregar.setOnAction(e -> agregarAlCarrito(p));

        card.getChildren().addAll(imgBox, badge, nombre, spacer, precio, btnAgregar);
        return card;
    }

    // ── Carrito ──────────────────────────────────────────────────────────────

    private void agregarAlCarrito(Producto p) {
        for (ItemCarrito item : carrito) {
            if (item.idProducto == p.idProducto) {
                item.cantidad++;
                refrescarPanel();
                return;
            }
        }
        carrito.add(new ItemCarrito(p.idProducto, p.nombre, p.precio, 1));
        refrescarPanel();
    }

    private void refrescarPanel() {
        if (vboxItems == null) return;
        vboxItems.getChildren().clear();

        BigDecimal subtotal = BigDecimal.ZERO;

        for (ItemCarrito item : carrito) {
            BigDecimal lineaTotal = item.precioUnitario.multiply(BigDecimal.valueOf(item.cantidad));
            subtotal = subtotal.add(lineaTotal);

            HBox fila = new HBox(8);
            fila.setAlignment(Pos.CENTER_LEFT);
            fila.setStyle("-fx-background-color: #f8f9ff; -fx-background-radius: 10;" +
                          "-fx-padding: 8 10 8 10;");

            VBox info = new VBox(2);
            HBox.setHgrow(info, Priority.ALWAYS);
            info.setMaxWidth(Double.MAX_VALUE);
            Label lNombre = new Label(item.nombre);
            lNombre.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #1a1a2e;");
            lNombre.setWrapText(true);
            Label lUnitario = new Label("$" + item.precioUnitario.setScale(2, RoundingMode.HALF_UP).toPlainString() + " c/u");
            lUnitario.setStyle("-fx-font-size: 10px; -fx-text-fill: #999;");
            info.getChildren().addAll(lNombre, lUnitario);

            // Botones cantidad
            Button btnMenos = crearBtnCantidad("−", "#e8eef8", "#004aad");
            btnMenos.setOnAction(e -> {
                item.cantidad--;
                if (item.cantidad <= 0) carrito.remove(item);
                refrescarPanel();
            });

            Label lCant = new Label(String.valueOf(item.cantidad));
            lCant.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-min-width: 20;");
            lCant.setAlignment(Pos.CENTER);

            Button btnMas = crearBtnCantidad("+", "#004aad", "white");
            btnMas.setOnAction(e -> { item.cantidad++; refrescarPanel(); });

            HBox controles = new HBox(5, btnMenos, lCant, btnMas);
            controles.setAlignment(Pos.CENTER);

            Label lTotal = new Label("$" + lineaTotal.setScale(2, RoundingMode.HALF_UP).toPlainString());
            lTotal.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #004aad; -fx-min-width: 58;");
            lTotal.setAlignment(Pos.CENTER_RIGHT);

            fila.getChildren().addAll(info, controles, lTotal);
            vboxItems.getChildren().add(fila);
        }

        BigDecimal itbis = subtotal.multiply(TASA_ITBIS).setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = subtotal.add(itbis).setScale(2, RoundingMode.HALF_UP);
        subtotal = subtotal.setScale(2, RoundingMode.HALF_UP);

        if (lblSubtotal  != null) lblSubtotal .setText("$" + subtotal.toPlainString());
        if (lblDescuento != null) lblDescuento.setText("-$0.00");
        if (lblItbs      != null) lblItbs     .setText("$" + itbis.toPlainString());
        if (lblTotal     != null) lblTotal    .setText("$" + total.toPlainString());
    }

    private Button crearBtnCantidad(String texto, String bgColor, String textColor) {
        Button btn = new Button(texto);
        btn.setStyle(
                "-fx-background-color: " + bgColor + "; -fx-text-fill: " + textColor + ";" +
                "-fx-min-width: 26; -fx-min-height: 26; -fx-max-width: 26; -fx-max-height: 26;" +
                "-fx-background-radius: 13; -fx-cursor: hand; -fx-font-weight: bold;");
        return btn;
    }

    // ── Filtros del catálogo ─────────────────────────────────────────────────

    @FXML private void FnFiltrarTodos() {
        cargarProductosBD(null);
    }

    @FXML
    private void FnFiltrarCategoria(ActionEvent event) {
        // Extrae solo letras/números del texto del botón (quita emojis, espacios extra)
        String raw       = ((Button) event.getSource()).getText();
        String categoria = raw.replaceAll("[^\\p{L}\\p{N} ]", "").trim();
        cargarProductosBD(categoria);
    }

    @FXML private void FnFiltrarOfertas() {
        cargarProductosBD(null);
    }

    @FXML
    private void FnBuscar() {
        String q = (txtBuscar != null) ? txtBuscar.getText().trim().toLowerCase() : "";
        if (q.isEmpty()) {
            mostrarCatalogo(todosLosProductos);
            return;
        }
        List<Producto> filtrados = new ArrayList<>();
        for (Producto p : todosLosProductos) {
            if (p.nombre.toLowerCase().contains(q)) filtrados.add(p);
        }
        mostrarCatalogo(filtrados);
    }

    // ── Tipo de entrega ──────────────────────────────────────────────────────

    @FXML private void FnEntregaEnLocal() {
        tipoEntrega = "En Local";
        setVisibleDelivery(false);
    }

    @FXML private void FnEntregaRecoger() {
        tipoEntrega = "Recoger";
        setVisibleDelivery(false);
    }

    @FXML private void FnEntregaDelivery() {
        tipoEntrega = "Delivery";
        setVisibleDelivery(true);
    }

    private void setVisibleDelivery(boolean visible) {
        if (vboxDelivery != null) {
            vboxDelivery.setVisible(visible);
            vboxDelivery.setManaged(visible);
        }
    }

    // ── Buscar cliente ───────────────────────────────────────────────────────

    @FXML
    private void FnBuscarCliente() {
        String cedula = (txtCedula != null) ? txtCedula.getText().trim() : "";
        if (cedula.isEmpty()) {
            mostrarMensaje("Ingrese la cédula del cliente.");
            return;
        }

        final String sql =
                "SELECT c.id_cliente, p.nombre " +
                "FROM   tbl_cliente  c " +
                "INNER JOIN tbl_persona p ON c.id_persona = p.id_persona " +
                "WHERE  p.cedula = ?";

        Connection con = null;
        try {
            con = Conexion.establecerConexion();
            if (con == null) { mostrarMensaje("Sin conexión."); return; }

            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, cedula);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        idClienteSeleccionado = rs.getInt("id_cliente");
                        if (lblCliente != null) lblCliente.setText(rs.getString("nombre"));
                        mostrarMensaje("");
                    } else {
                        idClienteSeleccionado = -1;
                        if (lblCliente != null) lblCliente.setText("");
                        mostrarMensaje("Cliente no encontrado.");
                    }
                }
            }
        } catch (Exception e) {
            mostrarMensaje("Error: " + e.getMessage());
        } finally {
            cerrarConexion(con);
        }
    }

    // ── Guardar pedido ───────────────────────────────────────────────────────

    @FXML
    private void FnGuardar() {
        if (carrito.isEmpty()) {
            mostrarMensaje("Agregue al menos un producto.");
            return;
        }
        if (idClienteSeleccionado == -1) {
            mostrarMensaje("Busque y seleccione un cliente antes de registrar.");
            return;
        }

        BigDecimal total     = calcularTotal();
        int        idEmpl    = CONTROLLER_Seccion.getInstancia().getIdEmpleado();

        Connection con = null;
        try {
            con = Conexion.establecerConexion();
            if (con == null) { mostrarMensaje("Sin conexión."); return; }
            con.setAutoCommit(false);

            // Obtener primer id_itbs disponible
            int idItbs = 1;
            try (PreparedStatement ps = con.prepareStatement(
                    "SELECT TOP 1 id_itbs FROM tbl_itbs");
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) idItbs = rs.getInt(1);
            }

            // 1) Cabecera del pedido
            final String sqlPed =
                    "INSERT INTO tbl_pedido " +
                    "  (tipo_entrega, tiempo_realizacion, precio_total, fecha_pedido," +
                    "   id_cliente, id_empleado, id_itbs, metodo_pago) " +
                    "VALUES (?, CAST('00:30:00' AS TIME), ?, CAST(GETDATE() AS DATE)," +
                    "        ?, ?, ?, ?)";

            int idPedido;
            try (PreparedStatement ps = con.prepareStatement(sqlPed,
                                                             Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, tipoEntrega);
                ps.setDouble(2, total.doubleValue());
                ps.setInt   (3, idClienteSeleccionado);
                if (idEmpl == -1) ps.setNull(4, Types.INTEGER);
                else              ps.setInt (4, idEmpl);
                ps.setInt   (5, idItbs);
                ps.setString(6, metodoPago);
                ps.executeUpdate();

                try (ResultSet keys = ps.getGeneratedKeys()) {
                    idPedido = keys.next() ? keys.getInt(1) : 0;
                }
            }

            // 2) Detalles del pedido
            final String sqlDet =
                    "INSERT INTO tbl_producto_pedido " +
                    "  (id_producto, id_pedido, cantidad_producto, id_presentacion, especificaciones)" +
                    "VALUES (?, ?, ?, " +
                    "  ISNULL((SELECT TOP 1 id_presentacion FROM tbl_presentacion_producto " +
                    "          WHERE id_producto = ?), 1), '')";

            try (PreparedStatement ps = con.prepareStatement(sqlDet)) {
                for (ItemCarrito item : carrito) {
                    ps.setInt(1, item.idProducto);
                    ps.setInt(2, idPedido);
                    ps.setInt(3, item.cantidad);
                    ps.setInt(4, item.idProducto);
                    ps.addBatch();
                }
                ps.executeBatch();
            }

            con.commit();
            mostrarMensaje("✅ Pedido #" + idPedido + " registrado correctamente.");
            FnLimpiar();

        } catch (Exception e) {
            try { if (con != null) con.rollback(); } catch (Exception ignored) {}
            mostrarMensaje("Error al guardar: " + e.getMessage());
            System.err.println("[HacerPedido] FnGuardar: " + e.getMessage());
        } finally {
            cerrarConexion(con);
        }
    }

    private BigDecimal calcularTotal() {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (ItemCarrito item : carrito) {
            subtotal = subtotal.add(
                    item.precioUnitario.multiply(BigDecimal.valueOf(item.cantidad)));
        }
        return subtotal.multiply(BigDecimal.ONE.add(TASA_ITBIS))
                       .setScale(2, RoundingMode.HALF_UP);
    }

    // ── Limpiar formulario ───────────────────────────────────────────────────

    @FXML
    private void FnLimpiar() {
        carrito.clear();
        refrescarPanel();

        if (!CONTROLLER_Seccion.getInstancia().esCliente()) {
            idClienteSeleccionado = -1;
            if (lblCliente != null) lblCliente.setText("");
            if (txtCedula  != null) { txtCedula.clear(); txtCedula.setDisable(false); }
        }

        if (lblMensaje     != null) lblMensaje.setText("");
        if (txtDireccion   != null) txtDireccion.clear();
        if (txtObservacion != null) txtObservacion.clear();
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private void mostrarMensaje(String msg) {
        if (lblMensaje != null) lblMensaje.setText(msg != null ? msg : "");
    }

    private void cerrarConexion(Connection con) {
        if (con != null) {
            try { con.close(); } catch (Exception ignored) {}
        }
    }

    // ── Clases internas ──────────────────────────────────────────────────────

    public static class Producto {
        final int        idProducto;
        final String     nombre;
        final String     tipo;
        final BigDecimal precio;

        public Producto(int id, String nombre, String tipo, BigDecimal precio) {
            this.idProducto = id;
            this.nombre     = nombre != null ? nombre : "";
            this.tipo       = tipo   != null ? tipo   : "";
            this.precio     = precio != null ? precio : BigDecimal.ZERO;
        }
    }

    public static class ItemCarrito {
        final int        idProducto;
        final String     nombre;
        final BigDecimal precioUnitario;
        int              cantidad;

        public ItemCarrito(int id, String nombre, BigDecimal precio, int cantidad) {
            this.idProducto     = id;
            this.nombre         = nombre != null ? nombre : "";
            this.precioUnitario = precio != null ? precio : BigDecimal.ZERO;
            this.cantidad       = cantidad;
        }
    }
}
