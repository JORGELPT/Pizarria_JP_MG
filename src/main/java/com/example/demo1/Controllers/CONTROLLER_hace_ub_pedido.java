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
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * tbl_pedido:
 *   id_pedido (IDENTITY), tipo_entrega, tiempo_realizacion (time), precio_total (float),
 *   fecha_pedido (date), id_cliente, id_empleado, id_itbs, extra (nullable),
 *   rnc (nullable), metodo_pago
 *
 * tbl_producto_pedido:
 *   id_producto, id_pedido, cantidad_producto, id_presentacion, especificaciones
 *
 * tbl_producto:
 *   id_producto, nombre, tipo, disponibilidad (bit), id_proveedor (nullable)
 *   — el precio está en tbl_presentacion_producto (costo)
 */
public class CONTROLLER_hace_ub_pedido {

    Conexion conexion = new Conexion();

    @FXML private Label       lblFecha;
    @FXML private TextField   txtBuscar;
    @FXML private FlowPane    flowProductos;
    @FXML private Button      btnTodos;
    @FXML private Button      btnPizza;
    @FXML private Button      btnBebida;
    @FXML private Button      btnPasta;
    @FXML private Button      btnPostre;
    @FXML private Button      btnOfertas;

    @FXML private TextField   txtCedula;
    @FXML private Label       lblCliente;
    @FXML private VBox        vboxItems;

    @FXML private ToggleButton tglEnLocal;
    @FXML private ToggleButton tglRecoger;
    @FXML private ToggleButton tglDelivery;

    @FXML private VBox        vboxDelivery;
    @FXML private ComboBox<String> cmbMetodoEnvio;
    @FXML private TextField   txtDireccion;
    @FXML private TextField   txtObservacion;

    @FXML private ComboBox<String> cmbExtra;

    @FXML private Label lblSubtotal;
    @FXML private Label lblDescuento;
    @FXML private Label lblItbsNombre;
    @FXML private Label lblItbs;
    @FXML private Label lblTotal;

    @FXML private ToggleButton tglEfectivo;
    @FXML private ToggleButton tglTarjeta;
    @FXML private ToggleButton tglEWallet;

    @FXML private Label lblMensaje;

    private int    idClienteSeleccionado = -1;
    private String tipoEntrega = "En Local";
    private String metodoPago  = "Efectivo";

    private final List<ItemCarrito> carrito = new ArrayList<>();
    private final ObservableList<Producto> todosLosProductos = FXCollections.observableArrayList();

    private static final BigDecimal TASA_ITBIS = new BigDecimal("0.18");

    @FXML
    public void initialize() {
        if (lblFecha != null) {
            lblFecha.setText(LocalDate.now()
                    .format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        }

        if (cmbMetodoEnvio != null) {
            cmbMetodoEnvio.setItems(FXCollections.observableArrayList("Moto", "Carro", "Pie"));
        }

        CONTROLLER_Seccion s = CONTROLLER_Seccion.getInstancia();
        if (s.esCliente() && s.getIdCliente() != -1) {
            idClienteSeleccionado = s.getIdCliente();
            if (lblCliente != null) lblCliente.setText(s.getNombre());
            if (txtCedula  != null) txtCedula.setDisable(true);
        }

        cargarProductosBD(null);
    }

    // -----------------------------------------------------------------------
    //  Cargar productos — precio mínimo desde tbl_presentacion_producto
    // -----------------------------------------------------------------------
    private void cargarProductosBD(String categoria) {
        todosLosProductos.clear();

        StringBuilder sb = new StringBuilder(
                "SELECT p.id_producto, p.nombre, p.tipo, MIN(pp.costo) AS precio " +
                "FROM tbl_producto p " +
                "LEFT JOIN tbl_presentacion_producto pp ON p.id_producto = pp.id_producto " +
                "WHERE p.disponibilidad = 1");
        if (categoria != null && !categoria.equalsIgnoreCase("todos")) {
            sb.append(" AND p.tipo = ?");
        }
        sb.append(" GROUP BY p.id_producto, p.nombre, p.tipo ORDER BY p.nombre");

        try (Connection con = conexion.establecerConexion();
             PreparedStatement ps = con.prepareStatement(sb.toString())) {

            if (categoria != null && !categoria.equalsIgnoreCase("todos")) {
                ps.setString(1, categoria);
            }
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                todosLosProductos.add(new Producto(
                        rs.getInt("id_producto"),
                        rs.getString("nombre"),
                        rs.getString("tipo"),
                        rs.wasNull() ? BigDecimal.ZERO : rs.getBigDecimal("precio")));
            }

        } catch (Exception e) {
            if (lblMensaje != null) lblMensaje.setText("Error al cargar productos: " + e.getMessage());
        }

        refrescarCatalogo(todosLosProductos);
    }

    private void refrescarCatalogo(List<Producto> lista) {
        if (flowProductos == null) return;
        flowProductos.getChildren().clear();
        for (Producto p : lista) {
            flowProductos.getChildren().add(crearTarjetaProducto(p));
        }
    }

    private VBox crearTarjetaProducto(Producto p) {
        VBox card = new VBox(8);
        card.setPrefSize(160, 210);
        card.setMaxSize(160, 210);
        card.setAlignment(Pos.TOP_LEFT);
        String estiloNormal =
            "-fx-background-color: white;" +
            "-fx-background-radius: 16;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 10, 0, 0, 2);" +
            "-fx-cursor: hand; -fx-padding: 10 10 12 10;";
        String estiloHover =
            "-fx-background-color: #f0f4ff;" +
            "-fx-background-radius: 16;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,74,173,0.18), 14, 0, 0, 4);" +
            "-fx-cursor: hand; -fx-padding: 10 10 12 10;";
        card.setStyle(estiloNormal);
        card.setOnMouseEntered(e -> card.setStyle(estiloHover));
        card.setOnMouseExited(e  -> card.setStyle(estiloNormal));

        // Imagen del producto (carga desde imagenes/productos/{id}.png o placeholder gris)
        StackPane imgBox = new StackPane();
        imgBox.setPrefSize(140, 95);
        imgBox.setMaxWidth(Double.MAX_VALUE);

        java.io.InputStream is = getClass().getResourceAsStream(
            "/com/example/demo1/imagenes/productos/" + p.idProducto + ".png");
        if (is == null) is = getClass().getResourceAsStream(
            "/com/example/demo1/imagenes/productos/" + p.idProducto + ".jpg");

        if (is != null) {
            javafx.scene.image.ImageView iv = new javafx.scene.image.ImageView(
                new javafx.scene.image.Image(is));
            iv.setFitWidth(140);
            iv.setFitHeight(95);
            iv.setPreserveRatio(true);
            imgBox.setStyle("-fx-background-color: #f4f6fb; -fx-background-radius: 10;");
            imgBox.getChildren().add(iv);
        } else {
            imgBox.setStyle(
                "-fx-background-color: #dce6f5;" +
                "-fx-background-radius: 10;");
        }

        Label badge = new Label(p.tipo != null ? p.tipo.toUpperCase() : "PRODUCTO");
        badge.setStyle("-fx-font-size: 9px; -fx-font-weight: bold; -fx-text-fill: #be1e1e;");

        Label nombre = new Label(p.nombre);
        nombre.setWrapText(true);
        nombre.setMaxWidth(Double.MAX_VALUE);
        nombre.setStyle(
            "-fx-font-size: 12px; -fx-font-weight: bold;" +
            "-fx-text-fill: #1a1a2e; -fx-font-family: 'Segoe UI';");

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        Label precio = new Label("$" + p.precio.toPlainString());
        precio.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #004aad;");

        Button btnAdd = new Button("+ Agregar");
        btnAdd.setMaxWidth(Double.MAX_VALUE);
        btnAdd.setStyle(
            "-fx-background-color: #004aad; -fx-text-fill: white;" +
            "-fx-background-radius: 8; -fx-cursor: hand;" +
            "-fx-font-size: 11px; -fx-padding: 6 0;");
        btnAdd.setOnAction(e -> agregarAlCarrito(p));

        card.getChildren().addAll(imgBox, badge, nombre, spacer, precio, btnAdd);
        return card;
    }

    // -----------------------------------------------------------------------
    //  Carrito
    // -----------------------------------------------------------------------
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
            BigDecimal lineaTotal = item.precioUnitario.multiply(new BigDecimal(item.cantidad));
            subtotal = subtotal.add(lineaTotal);

            HBox fila = new HBox(8);
            fila.setAlignment(Pos.CENTER_LEFT);
            fila.setStyle("-fx-background-color: #f8f9ff; -fx-background-radius: 10; -fx-padding: 10 12 10 12;");

            VBox infoBox = new VBox(2);
            HBox.setHgrow(infoBox, Priority.ALWAYS);
            infoBox.setMaxWidth(Double.MAX_VALUE);
            Label lNombre = new Label(item.nombre);
            lNombre.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #1a1a2e;");
            lNombre.setWrapText(true);
            Label lPrecioUnit = new Label("$" + item.precioUnitario.toPlainString() + " c/u");
            lPrecioUnit.setStyle("-fx-font-size: 10px; -fx-text-fill: #999;");
            infoBox.getChildren().addAll(lNombre, lPrecioUnit);

            HBox controls = new HBox(6);
            controls.setAlignment(Pos.CENTER);

            Button btnMenos = new Button("−");
            btnMenos.setStyle(
                "-fx-background-color: #e8eef8; -fx-text-fill: #004aad;" +
                "-fx-min-width: 26; -fx-min-height: 26; -fx-max-width: 26; -fx-max-height: 26;" +
                "-fx-background-radius: 13; -fx-cursor: hand; -fx-font-weight: bold;");
            btnMenos.setOnAction(e -> {
                item.cantidad--;
                if (item.cantidad <= 0) carrito.remove(item);
                refrescarPanel();
            });

            Label lCant = new Label(String.valueOf(item.cantidad));
            lCant.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #1a1a2e; -fx-min-width: 20;");
            lCant.setAlignment(Pos.CENTER);

            Button btnMas = new Button("+");
            btnMas.setStyle(
                "-fx-background-color: #004aad; -fx-text-fill: white;" +
                "-fx-min-width: 26; -fx-min-height: 26; -fx-max-width: 26; -fx-max-height: 26;" +
                "-fx-background-radius: 13; -fx-cursor: hand; -fx-font-weight: bold;");
            btnMas.setOnAction(e -> { item.cantidad++; refrescarPanel(); });

            controls.getChildren().addAll(btnMenos, lCant, btnMas);

            Label lTotal = new Label("$" + lineaTotal.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString());
            lTotal.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #004aad; -fx-min-width: 55;");
            lTotal.setAlignment(Pos.CENTER_RIGHT);

            fila.getChildren().addAll(infoBox, controls, lTotal);
            vboxItems.getChildren().add(fila);
        }

        BigDecimal itbis = subtotal.multiply(TASA_ITBIS).setScale(2, java.math.RoundingMode.HALF_UP);
        BigDecimal total = subtotal.add(itbis).setScale(2, java.math.RoundingMode.HALF_UP);

        if (lblSubtotal  != null) lblSubtotal.setText("$" + subtotal.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString());
        if (lblDescuento != null) lblDescuento.setText("-$0.00");
        if (lblItbs      != null) lblItbs.setText("$" + itbis.toPlainString());
        if (lblTotal     != null) lblTotal.setText("$" + total.toPlainString());
    }

    // -----------------------------------------------------------------------
    //  Filtros
    // -----------------------------------------------------------------------
    @FXML private void FnFiltrarTodos() { cargarProductosBD(null); }

    @FXML
    private void FnFiltrarCategoria(ActionEvent event) {
        // Quitar el emoji y espacios del texto del botón (ej. "🍕 Pizza" → "pizza")
        String raw = ((Button) event.getSource()).getText();
        String categoria = raw.replaceAll("[^\\p{L}\\p{N} ]", "").trim().toLowerCase();
        cargarProductosBD(categoria);
    }

    @FXML private void FnFiltrarOfertas() { cargarProductosBD(null); }

    @FXML
    private void FnBuscar() {
        String q = txtBuscar != null ? txtBuscar.getText().trim().toLowerCase() : "";
        if (q.isEmpty()) { refrescarCatalogo(todosLosProductos); return; }
        List<Producto> filtrados = new ArrayList<>();
        for (Producto p : todosLosProductos) {
            if (p.nombre.toLowerCase().contains(q)) filtrados.add(p);
        }
        refrescarCatalogo(filtrados);
    }

    // -----------------------------------------------------------------------
    //  Tipo de entrega
    // -----------------------------------------------------------------------
    @FXML private void FnEntregaEnLocal() {
        tipoEntrega = "En Local";
        if (vboxDelivery != null) { vboxDelivery.setVisible(false); vboxDelivery.setManaged(false); }
    }

    @FXML private void FnEntregaRecoger() {
        tipoEntrega = "Recoger";
        if (vboxDelivery != null) { vboxDelivery.setVisible(false); vboxDelivery.setManaged(false); }
    }

    @FXML private void FnEntregaDelivery() {
        tipoEntrega = "Delivery";
        if (vboxDelivery != null) { vboxDelivery.setVisible(true); vboxDelivery.setManaged(true); }
    }

    // -----------------------------------------------------------------------
    //  Buscar cliente por cédula
    // -----------------------------------------------------------------------
    @FXML
    private void FnBuscarCliente() {
        String cedula = txtCedula != null ? txtCedula.getText().trim() : "";
        if (cedula.isEmpty()) {
            if (lblMensaje != null) lblMensaje.setText("Ingrese la cédula del cliente.");
            return;
        }

        String sql = "SELECT c.id_cliente, p.nombre " +
                     "FROM tbl_cliente c " +
                     "INNER JOIN tbl_persona p ON c.id_persona = p.id_persona " +
                     "WHERE p.cedula = ?";

        try (Connection con = conexion.establecerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, cedula);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                idClienteSeleccionado = rs.getInt("id_cliente");
                if (lblCliente != null) lblCliente.setText(rs.getString("nombre"));
                if (lblMensaje != null) lblMensaje.setText("");
            } else {
                idClienteSeleccionado = -1;
                if (lblCliente != null) lblCliente.setText("");
                if (lblMensaje != null) lblMensaje.setText("Cliente no encontrado.");
            }

        } catch (Exception e) {
            if (lblMensaje != null) lblMensaje.setText("Error: " + e.getMessage());
        }
    }

    // -----------------------------------------------------------------------
    //  Guardar pedido
    //  tbl_pedido columnas reales:
    //    tipo_entrega, tiempo_realizacion (time), precio_total (float),
    //    fecha_pedido (date), id_cliente, id_empleado, id_itbs, metodo_pago
    //  tbl_producto_pedido columnas reales:
    //    id_producto, id_pedido, cantidad_producto, id_presentacion, especificaciones
    // -----------------------------------------------------------------------
    @FXML
    private void FnGuardar() {
        if (carrito.isEmpty()) {
            if (lblMensaje != null) lblMensaje.setText("Agregue al menos un producto.");
            return;
        }
        if (idClienteSeleccionado == -1) {
            if (lblMensaje != null) lblMensaje.setText("Busque un cliente antes de registrar.");
            return;
        }

        BigDecimal total   = calcularTotal();
        int idEmpleado     = CONTROLLER_Seccion.getInstancia().getIdEmpleado();

        Connection con = null;
        try {
            con = conexion.establecerConexion();
            con.setAutoCommit(false);

            // Obtener el primer id_itbs disponible
            int idItbs = 1;
            try (PreparedStatement psItbs = con.prepareStatement(
                    "SELECT TOP 1 id_itbs FROM tbl_itbs");
                 ResultSet rsItbs = psItbs.executeQuery()) {
                if (rsItbs.next()) idItbs = rsItbs.getInt(1);
            }

            // 1) Insertar cabecera en tbl_pedido
            String sqlPed =
                "INSERT INTO tbl_pedido " +
                "(tipo_entrega, tiempo_realizacion, precio_total, fecha_pedido, " +
                " id_cliente, id_empleado, id_itbs, metodo_pago) " +
                "VALUES (?, CAST('00:30:00' AS TIME), ?, CAST(GETDATE() AS DATE), ?, ?, ?, ?)";

            int idPedido;
            try (PreparedStatement ps = con.prepareStatement(sqlPed, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, tipoEntrega);
                ps.setDouble(2, total.doubleValue());
                ps.setInt(3, idClienteSeleccionado);
                if (idEmpleado == -1) ps.setNull(4, Types.INTEGER);
                else                  ps.setInt(4, idEmpleado);
                ps.setInt(5, idItbs);
                ps.setString(6, metodoPago);
                ps.executeUpdate();
                ResultSet keys = ps.getGeneratedKeys();
                idPedido = keys.next() ? keys.getInt(1) : 0;
            }

            // 2) Insertar detalles en tbl_producto_pedido
            String sqlDet =
                "INSERT INTO tbl_producto_pedido " +
                "(id_producto, id_pedido, cantidad_producto, id_presentacion, especificaciones) " +
                "VALUES (?, ?, ?, " +
                "  ISNULL((SELECT TOP 1 id_presentacion FROM tbl_presentacion_producto " +
                "          WHERE id_producto = ?), 1), '')";

            try (PreparedStatement ps = con.prepareStatement(sqlDet)) {
                for (ItemCarrito item : carrito) {
                    ps.setInt(1, item.idProducto);
                    ps.setInt(2, idPedido);
                    ps.setInt(3, item.cantidad);
                    ps.setInt(4, item.idProducto); // para el subquery
                    ps.addBatch();
                }
                ps.executeBatch();
            }

            con.commit();
            if (lblMensaje != null)
                lblMensaje.setText("Pedido #" + idPedido + " registrado correctamente.");
            FnLimpiar();

        } catch (Exception e) {
            try { if (con != null) con.rollback(); } catch (Exception ignore) {}
            if (lblMensaje != null) lblMensaje.setText("Error: " + e.getMessage());
        } finally {
            try { if (con != null) con.close(); } catch (Exception ignore) {}
        }
    }

    private BigDecimal calcularTotal() {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (ItemCarrito item : carrito) {
            subtotal = subtotal.add(item.precioUnitario.multiply(new BigDecimal(item.cantidad)));
        }
        return subtotal.add(subtotal.multiply(TASA_ITBIS))
                .setScale(2, java.math.RoundingMode.HALF_UP);
    }

    // -----------------------------------------------------------------------
    //  Limpiar
    // -----------------------------------------------------------------------
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

    // -----------------------------------------------------------------------
    //  Clases internas
    // -----------------------------------------------------------------------
    public static class Producto {
        final int        idProducto;
        final String     nombre;
        final String     tipo;
        final BigDecimal precio;

        public Producto(int idProducto, String nombre, String tipo, BigDecimal precio) {
            this.idProducto = idProducto;
            this.nombre     = nombre;
            this.tipo       = tipo;
            this.precio     = precio != null ? precio : BigDecimal.ZERO;
        }
    }

    public static class ItemCarrito {
        final int        idProducto;
        final String     nombre;
        final BigDecimal precioUnitario;
        int              cantidad;

        public ItemCarrito(int idProducto, String nombre, BigDecimal precioUnitario, int cantidad) {
            this.idProducto     = idProducto;
            this.nombre         = nombre;
            this.precioUnitario = precioUnitario;
            this.cantidad       = cantidad;
        }
    }
}
