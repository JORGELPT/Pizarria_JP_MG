package com.example.demo1.Controllers;

import com.example.demo1.Database.Conexion;
import com.example.demo1.Utils.CONTROLLER_Seccion;
import com.example.demo1.Utils.JasperUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.view.JasperViewer;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    @FXML private ComboBox<Extra> cmbExtra;

    @FXML private Label        lblSubtotal;
    @FXML private Label        lblDescuento;
    @FXML private Label        lblItbsNombre;
    @FXML private Label        lblItbs;
    @FXML private Label        lblTotal;

    @FXML private ToggleButton tglEfectivo;
    @FXML private ToggleButton tglTarjeta;
    @FXML private ToggleButton tglEWallet;

    @FXML private Label        lblMensaje;
    @FXML private HBox         hboxCategorias;
    @FXML private HBox         hboxEntrega;
    @FXML private Button       btnFactura;

    // ── Pill activo actual ───────────────────────────────────────────────────
    private Button       btnCatActivo;
    private ToggleButton tglEntActivo;

    // ── Estado ───────────────────────────────────────────────────────────────
    private int    idClienteSeleccionado = -1;
    private int    ultimoIdPedido        = -1;
    private String tipoEntrega           = "En Local";
    private String metodoPago            = "Efectivo";
    private Extra  extraSeleccionado     = null;

    private final List<ItemCarrito>          carrito            = new ArrayList<>();
    private final ObservableList<Producto>   todosLosProductos  = FXCollections.observableArrayList();

    private static final BigDecimal TASA_ITBIS  = new BigDecimal("0.18");
    private              boolean    soloOfertas = false;

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

            // Cargar extras con precio desde tbl_ingrediente
            cargarExtras();

            // Si el usuario es cliente, prellenar sus datos
            CONTROLLER_Seccion s = CONTROLLER_Seccion.getInstancia();
            if (s.esCliente() && s.getIdCliente() != -1) {
                idClienteSeleccionado = s.getIdCliente();
                if (lblCliente != null) lblCliente.setText(s.getNombre());
                if (txtCedula  != null) txtCedula.setDisable(true);
            }

            // Cargar catálogo de productos desde BD
            cargarProductosBD(null);

            // Pills iniciales
            setupPillCategorias();
            setupPillEntrega();

        } catch (Exception e) {
            // Nunca dejar que una excepción en initialize() rompa la carga FXML
            System.err.println("[HacerPedido] Error en initialize: " + e.getMessage());
        }
    }

    // ── Extras ───────────────────────────────────────────────────────────────

    private void cargarExtras() {
        if (cmbExtra == null) return;

        ObservableList<Extra> lista = FXCollections.observableArrayList();
        lista.add(new Extra(0, "Sin extra", BigDecimal.ZERO));   // opción por defecto

        Connection con = null;
        try {
            con = Conexion.establecerConexion();
            if (con != null) {
                try (PreparedStatement ps = con.prepareStatement(
                        "SELECT id_ingrediente, nombre, tipo_ingrediente " +
                        "FROM tbl_ingrediente ORDER BY nombre");
                     ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String tipo  = rs.getString("tipo_ingrediente");
                        BigDecimal p = getPrecioExtra(tipo);
                        lista.add(new Extra(rs.getInt("id_ingrediente"),
                                            rs.getString("nombre"), p));
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[HacerPedido] cargarExtras: " + e.getMessage());
        } finally {
            cerrarConexion(con);
        }

        cmbExtra.setItems(lista);
        cmbExtra.getSelectionModel().selectFirst();
        // Deshabilitado por defecto hasta que se agregue una pizza
        cmbExtra.setDisable(true);
        cmbExtra.setStyle("-fx-background-color: #eeeeee; -fx-background-radius: 10;" +
                          "-fx-border-color: transparent; -fx-opacity: 0.5;");

        // Listener: cada vez que cambia el extra → recalcular totales
        cmbExtra.getSelectionModel().selectedItemProperty().addListener(
                (obs, anterior, nuevo) -> {
                    extraSeleccionado = (nuevo != null && nuevo.precio.compareTo(BigDecimal.ZERO) > 0)
                            ? nuevo : null;
                    refrescarPanel();
                });
    }

    /** Precio del extra según el tipo de ingrediente. */
    private BigDecimal getPrecioExtra(String tipo) {
        if (tipo == null) return new BigDecimal("70");
        switch (tipo.trim().toLowerCase()) {
            case "carne": case "res": case "pollo": case "cerdo":
            case "pepperoni": case "salchicha":
                return new BigDecimal("100");
            case "queso": case "lácteo": case "lacteo":
                return new BigDecimal("80");
            case "vegetal": case "verdura": case "veggie":
            case "maíz": case "maiz": case "piña": case "pina":
                return new BigDecimal("60");
            case "salsa": case "masa":
                return new BigDecimal("50");
            default:
                return new BigDecimal("70");
        }
    }

    // ── Carga de productos ───────────────────────────────────────────────────

    private void cargarProductosBD(String categoria) {
        todosLosProductos.clear();

        // ── 1. Cargar productos (query original, sin tocar) ──────────────────
        StringBuilder sql = new StringBuilder(
                "SELECT p.id_producto, p.nombre, p.tipo, MIN(pp.costo) AS precio " +
                "FROM   tbl_producto p " +
                "LEFT JOIN tbl_presentacion_producto pp ON p.id_producto = pp.id_producto " +
                "WHERE  p.disponibilidad = 1 ");

        boolean filtrarCategoria = (categoria != null && !categoria.isBlank()
                && !categoria.equalsIgnoreCase("todos"));
        if (filtrarCategoria) sql.append("AND LOWER(p.tipo) = ? ");
        sql.append("GROUP BY p.id_producto, p.nombre, p.tipo ORDER BY p.nombre");

        Connection con = null;
        try {
            con = Conexion.establecerConexion();
            if (con == null) { mostrarMensaje("No se pudo conectar."); return; }

            try (PreparedStatement ps = con.prepareStatement(sql.toString())) {
                if (filtrarCategoria) ps.setString(1, categoria.trim().toLowerCase());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        BigDecimal precio = rs.getBigDecimal("precio");
                        todosLosProductos.add(new Producto(
                                rs.getInt("id_producto"),
                                rs.getString("nombre"),
                                rs.getString("tipo"),
                                precio != null ? precio : BigDecimal.ZERO,
                                BigDecimal.ZERO));   // descuento se aplica abajo
                    }
                }
            }
        } catch (Exception e) {
            mostrarMensaje("Error al cargar productos: " + e.getMessage());
            System.err.println("[HacerPedido] cargarProductosBD: " + e.getMessage());
        } finally {
            cerrarConexion(con);
        }

        // ── 2. Cargar ofertas activas por separado y aplicar descuento ───────
        try {
            aplicarOfertas();
        } catch (Exception e) {
            mostrarMensaje("⚠ Ofertas: " + e.getMessage());
        }

        // ── 3. En modo ofertas: ordenar primero los que tienen descuento activo
        if (soloOfertas) {
            todosLosProductos.sort((a, b) ->
                    b.descuento.compareTo(a.descuento));   // con oferta al tope
        }

        mostrarCatalogo(todosLosProductos);
    }

    /** Consulta tbl_oferta y aplica el descuento a los productos ya cargados. */
    private void aplicarOfertas() throws Exception {
        String sql = "SELECT id_producto, descuento_porcentaje FROM tbl_oferta " +
                     "WHERE activa = 1 " +
                     "  AND (fecha_inicio IS NULL OR fecha_inicio <= CAST(GETDATE() AS DATE)) " +
                     "  AND (fecha_fin   IS NULL OR fecha_fin   >= CAST(GETDATE() AS DATE))";
        Connection con = null;
        try {
            con = Conexion.establecerConexion();
            if (con == null) return;
            try (PreparedStatement ps = con.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int        idProd    = rs.getInt("id_producto");
                    BigDecimal descuento = rs.getBigDecimal("descuento_porcentaje");
                    if (descuento == null || descuento.compareTo(BigDecimal.ZERO) <= 0) continue;
                    for (int i = 0; i < todosLosProductos.size(); i++) {
                        Producto p = todosLosProductos.get(i);
                        if (p.idProducto == idProd) {
                            todosLosProductos.set(i, new Producto(
                                    p.idProducto, p.nombre, p.tipo, p.precio, descuento));
                            break;
                        }
                    }
                }
            }
        } finally {
            cerrarConexion(con);
        }
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

        boolean tieneOfertaCard = p.descuento.compareTo(BigDecimal.ZERO) > 0;

        final String estiloBase = tieneOfertaCard
                ? "-fx-background-color: white; -fx-background-radius: 14;" +
                  "-fx-border-color: #004aad; -fx-border-width: 2; -fx-border-radius: 14;" +
                  "-fx-effect: dropshadow(gaussian,rgba(0,74,173,0.22),12,0,0,3);" +
                  "-fx-cursor: hand; -fx-padding: 10 10 10 10;"
                : "-fx-background-color: white; -fx-background-radius: 14;" +
                  "-fx-effect: dropshadow(gaussian,rgba(0,0,0,0.08),10,0,0,2);" +
                  "-fx-cursor: hand; -fx-padding: 10 10 10 10;";
        final String estiloHover = tieneOfertaCard
                ? "-fx-background-color: #f0f4ff; -fx-background-radius: 14;" +
                  "-fx-border-color: #004aad; -fx-border-width: 2; -fx-border-radius: 14;" +
                  "-fx-effect: dropshadow(gaussian,rgba(0,74,173,0.35),16,0,0,5);" +
                  "-fx-cursor: hand; -fx-padding: 10 10 10 10;"
                : "-fx-background-color: #f0f4ff; -fx-background-radius: 14;" +
                  "-fx-effect: dropshadow(gaussian,rgba(0,74,173,0.18),14,0,0,4);" +
                  "-fx-cursor: hand; -fx-padding: 10 10 10 10;";
        card.setStyle(estiloBase);
        card.setOnMouseEntered(e -> card.setStyle(estiloHover));
        card.setOnMouseExited (e -> card.setStyle(estiloBase));

        // Placeholder de imagen
        StackPane imgBox = new StackPane();
        imgBox.setPrefSize(145, 95);
        imgBox.setMaxWidth(Double.MAX_VALUE);

        // Buscar imagen: 1º por nombre exacto, 2º nombre normalizado, 3º por id
        final String BASE = "/com/example/demo1/imagenes/productos/";
        final String nombreNorm = p.nombre.trim().toLowerCase().replace(" ", "_");
        java.io.InputStream is = getClass().getResourceAsStream(BASE + p.nombre + ".png");
        if (is == null) is = getClass().getResourceAsStream(BASE + p.nombre + ".jpg");
        if (is == null) is = getClass().getResourceAsStream(BASE + nombreNorm + ".png");
        if (is == null) is = getClass().getResourceAsStream(BASE + nombreNorm + ".jpg");
        if (is == null) is = getClass().getResourceAsStream(BASE + p.idProducto + ".png");
        if (is == null) is = getClass().getResourceAsStream(BASE + p.idProducto + ".jpg");

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

        // Badge de tipo — si tiene oferta activa, muestra badge especial
        boolean tieneOferta = p.descuento.compareTo(BigDecimal.ZERO) > 0;
        Label badge = new Label(tieneOferta
                ? "⭐ " + p.descuento.setScale(0, RoundingMode.HALF_UP).toPlainString() + "% OFF"
                : (p.tipo != null ? p.tipo.toUpperCase() : "PRODUCTO"));
        badge.setStyle(tieneOferta
                ? "-fx-font-size: 9px; -fx-font-weight: bold; -fx-text-fill: white;" +
                  "-fx-background-color: #f0a500; -fx-background-radius: 6; -fx-padding: 2 6;"
                : "-fx-font-size: 9px; -fx-font-weight: bold; -fx-text-fill: #be1e1e;");

        Label nombre = new Label(p.nombre);
        nombre.setWrapText(true);
        nombre.setMaxWidth(Double.MAX_VALUE);
        nombre.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #1a1a2e;");

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        // Si tiene oferta: mostrar precio tachado + precio con descuento
        BigDecimal precioFinal = tieneOferta
                ? p.precio.multiply(BigDecimal.ONE.subtract(
                        p.descuento.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP)))
                          .setScale(2, RoundingMode.HALF_UP)
                : p.precio.setScale(2, RoundingMode.HALF_UP);

        Label precio = new Label("$" + precioFinal.toPlainString());
        precio.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #004aad;");

        Button btnAgregar = new Button("+ Agregar");
        btnAgregar.setMaxWidth(Double.MAX_VALUE);
        btnAgregar.setStyle(
                "-fx-background-color: #004aad; -fx-text-fill: white;" +
                "-fx-background-radius: 8; -fx-cursor: hand;" +
                "-fx-font-size: 11px; -fx-padding: 6 0;");
        btnAgregar.setOnAction(e -> agregarAlCarrito(p));

        // Agregar children en orden correcto
        card.getChildren().addAll(imgBox, badge, nombre, spacer);
        if (tieneOferta) {
            Label precioOriginal = new Label("$" + p.precio.setScale(2, RoundingMode.HALF_UP).toPlainString());
            precioOriginal.setStyle("-fx-font-size: 10px; -fx-text-fill: #aaa; -fx-strikethrough: true;");
            card.getChildren().add(precioOriginal);
        }
        card.getChildren().addAll(precio, btnAgregar);
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
        carrito.add(new ItemCarrito(p.idProducto, p.nombre, p.tipo, p.precio, 1));
        refrescarPanel();
    }

    private void refrescarPanel() {
        if (vboxItems == null) return;
        vboxItems.getChildren().clear();

        // Habilitar extras solo si hay al menos una pizza en el carrito
        boolean hayPizza = carrito.stream().anyMatch(i -> i.tipo.contains("pizza"));
        if (cmbExtra != null) {
            cmbExtra.setDisable(!hayPizza);
            cmbExtra.setStyle(hayPizza
                ? "-fx-background-color: #f0f4ff; -fx-background-radius: 10; -fx-border-color: transparent;"
                : "-fx-background-color: #eeeeee; -fx-background-radius: 10; -fx-border-color: transparent; -fx-opacity: 0.5;");
        }
        // Si ya no hay pizzas, quitar el extra seleccionado
        if (!hayPizza && extraSeleccionado != null) {
            extraSeleccionado = null;
            if (cmbExtra != null) cmbExtra.getSelectionModel().selectFirst();
        }

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

        // Fila del extra seleccionado
        if (extraSeleccionado != null) {
            HBox filaExtra = new HBox(8);
            filaExtra.setAlignment(Pos.CENTER_LEFT);
            filaExtra.setStyle("-fx-background-color: #fff8e1; -fx-background-radius: 10;" +
                               "-fx-padding: 8 10 8 10; -fx-border-color: #ffe082;" +
                               "-fx-border-radius: 10; -fx-border-width: 1;");

            VBox infoExtra = new VBox(2);
            HBox.setHgrow(infoExtra, Priority.ALWAYS);
            infoExtra.setMaxWidth(Double.MAX_VALUE);
            Label lExtraNombre = new Label("+ " + extraSeleccionado.nombre);
            lExtraNombre.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #856404;");
            Label lExtraTipo = new Label("Ingrediente extra");
            lExtraTipo.setStyle("-fx-font-size: 10px; -fx-text-fill: #a07800;");
            infoExtra.getChildren().addAll(lExtraNombre, lExtraTipo);

            Label lExtraPrecio = new Label("$" + extraSeleccionado.precio
                    .setScale(2, RoundingMode.HALF_UP).toPlainString());
            lExtraPrecio.setStyle("-fx-font-size: 13px; -fx-font-weight: bold;" +
                                  "-fx-text-fill: #856404; -fx-min-width: 58;");
            lExtraPrecio.setAlignment(Pos.CENTER_RIGHT);

            filaExtra.getChildren().addAll(infoExtra, lExtraPrecio);
            vboxItems.getChildren().add(filaExtra);

            subtotal = subtotal.add(extraSeleccionado.precio);
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
        soloOfertas = false;
        actualizarPillCat(btnTodos);
        cargarProductosBD(null);
    }

    @FXML
    private void FnFiltrarCategoria(ActionEvent event) {
        soloOfertas = false;
        Button src = (Button) event.getSource();
        actualizarPillCat(src);
        String raw       = src.getText();
        String categoria = raw.replaceAll("[^\\p{L}\\p{N} ]", "").trim();
        cargarProductosBD(categoria);
    }

    @FXML private void FnFiltrarOfertas() {
        soloOfertas = true;
        actualizarPillCat(btnOfertas);
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
        deselectToggle(tglEnLocal, tglRecoger, tglDelivery);
        actualizarPillEnt(tglEnLocal);
        setVisibleDelivery(false);
    }

    @FXML private void FnEntregaRecoger() {
        tipoEntrega = "Recoger";
        deselectToggle(tglEnLocal, tglRecoger, tglDelivery);
        actualizarPillEnt(tglRecoger);
        setVisibleDelivery(false);
    }

    @FXML private void FnEntregaDelivery() {
        tipoEntrega = "Delivery";
        deselectToggle(tglEnLocal, tglRecoger, tglDelivery);
        actualizarPillEnt(tglDelivery);
        setVisibleDelivery(true);
    }

    // ── Método de pago ───────────────────────────────────────────────────────

    @FXML private void FnPagoEfectivo() {
        metodoPago = "Efectivo";
        deselectToggle(tglEfectivo, tglTarjeta, tglEWallet);
        actualizarPillPago(tglEfectivo);
    }
    @FXML private void FnPagoTarjeta() {
        deselectToggle(tglEfectivo, tglTarjeta, tglEWallet);
        actualizarPillPago(tglEfectivo);
        metodoPago = "Efectivo";
        mostrarMensaje("⚠ Pago con Tarjeta no disponible por el momento.");
    }
    @FXML private void FnPagoEWallet() {
        deselectToggle(tglEfectivo, tglTarjeta, tglEWallet);
        actualizarPillPago(tglEfectivo);
        metodoPago = "Efectivo";
        mostrarMensaje("⚠ E-Wallet no disponible por el momento.");
    }

    private void actualizarPillPago(ToggleButton target) {
        for (ToggleButton tb : new ToggleButton[]{tglEfectivo, tglTarjeta, tglEWallet})
            tb.setStyle(tb == target ? TGL_ACTIVO : TGL_INACTIVO);
    }

    /** Evita que JavaFX aplique el estilo "selected" que sobreescribe el pill. */
    private void deselectToggle(ToggleButton... toggles) {
        for (ToggleButton tb : toggles) tb.setSelected(false);
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
            String notaExtra = (extraSeleccionado != null)
                    ? "Extra: " + extraSeleccionado.nombre +
                      " (+$" + extraSeleccionado.precio.toPlainString() + ")"
                    : "";

            final String sqlDet =
                    "INSERT INTO tbl_producto_pedido " +
                    "  (id_producto, id_pedido, cantidad_producto, id_presentacion, especificaciones)" +
                    "VALUES (?, ?, ?, " +
                    "  ISNULL((SELECT TOP 1 id_presentacion FROM tbl_presentacion_producto " +
                    "          WHERE id_producto = ?), 1), ?)";

            try (PreparedStatement ps = con.prepareStatement(sqlDet)) {
                boolean primeraFila = true;
                for (ItemCarrito item : carrito) {
                    ps.setInt   (1, item.idProducto);
                    ps.setInt   (2, idPedido);
                    ps.setInt   (3, item.cantidad);
                    ps.setInt   (4, item.idProducto);
                    // Solo la primera fila lleva la nota del extra
                    ps.setString(5, primeraFila ? notaExtra : "");
                    primeraFila = false;
                    ps.addBatch();
                }
                ps.executeBatch();
            }

            con.commit();
            ultimoIdPedido = idPedido;
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
        // Incluir precio del extra seleccionado
        if (extraSeleccionado != null) {
            subtotal = subtotal.add(extraSeleccionado.precio);
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
        // Resetear extra
        extraSeleccionado = null;
        if (cmbExtra != null) cmbExtra.getSelectionModel().selectFirst();
    }

    // ── Pill: categorías ─────────────────────────────────────────────────────

    private static final String PILL_ACTIVO   =
            "-fx-background-color: #004aad; -fx-text-fill: white; -fx-font-weight: bold;" +
            "-fx-background-radius: 20; -fx-padding: 5 18; -fx-cursor: hand; -fx-font-size: 12px;" +
            "-fx-effect: dropshadow(gaussian,rgba(0,74,173,0.25),8,0,0,2);";
    private static final String PILL_INACTIVO =
            "-fx-background-color: #e8eef8; -fx-text-fill: #555;" +
            "-fx-background-radius: 20; -fx-padding: 5 18; -fx-cursor: hand; -fx-font-size: 12px;";
    private static final String PILL_OFERTA_ACT =
            "-fx-background-color: #f0a500; -fx-text-fill: white; -fx-font-weight: bold;" +
            "-fx-background-radius: 20; -fx-padding: 5 18; -fx-cursor: hand; -fx-font-size: 12px;" +
            "-fx-effect: dropshadow(gaussian,rgba(240,165,0,0.30),8,0,0,2);";
    private static final String PILL_OFERTA_IN =
            "-fx-background-color: #fff3cd; -fx-text-fill: #856404;" +
            "-fx-background-radius: 20; -fx-padding: 5 18; -fx-cursor: hand; -fx-font-size: 12px;";

    private static final String TGL_ACTIVO  =
            "-fx-background-color: #004aad; -fx-text-fill: white; -fx-font-weight: bold;" +
            "-fx-background-radius: 20; -fx-padding: 6 4; -fx-cursor: hand; -fx-font-size: 11px;" +
            "-fx-effect: dropshadow(gaussian,rgba(0,74,173,0.25),6,0,0,2);";
    private static final String TGL_INACTIVO =
            "-fx-background-color: #e8eef8; -fx-text-fill: #555;" +
            "-fx-background-radius: 20; -fx-padding: 6 4; -fx-cursor: hand; -fx-font-size: 11px;";

    private void setupPillCategorias() {
        if (hboxCategorias == null) return;
        actualizarPillCat(btnTodos);
    }

    private void actualizarPillCat(Button target) {
        if (hboxCategorias == null) return;
        hboxCategorias.getChildren().forEach(n -> {
            if (n instanceof Button b) {
                boolean esOferta = b == btnOfertas;
                boolean activo   = b == target;
                b.setStyle(activo ? (esOferta ? PILL_OFERTA_ACT : PILL_ACTIVO)
                                  : (esOferta ? PILL_OFERTA_IN  : PILL_INACTIVO));
            }
        });
        btnCatActivo = target;
    }

    // ── Pill: entrega ─────────────────────────────────────────────────────────

    private void setupPillEntrega() {
        if (hboxEntrega == null) return;
        deselectToggle(tglEnLocal, tglRecoger, tglDelivery);
        actualizarPillEnt(tglEnLocal);
        deselectToggle(tglEfectivo, tglTarjeta, tglEWallet);
        actualizarPillPago(tglEfectivo);
    }

    private void actualizarPillEnt(ToggleButton target) {
        if (hboxEntrega == null) return;
        hboxEntrega.getChildren().forEach(n -> {
            if (n instanceof ToggleButton tb)
                tb.setStyle(tb == target ? TGL_ACTIVO : TGL_INACTIVO);
        });
        tglEntActivo = target;
    }

    // ── Factura Jasper ───────────────────────────────────────────────────────

    @FXML
    private void FnGenerarFactura() {
        if (ultimoIdPedido == -1) {
            mostrarMensaje("Registra un pedido primero para generar la factura.");
            return;
        }

        Connection con = Conexion.establecerConexion();
        if (con == null) { mostrarMensaje("Sin conexión."); return; }

        try {
            java.io.InputStream jrxml = getClass().getResourceAsStream(
                    "/com/example/demo1/reportes/factura.jrxml");
            if (jrxml == null) { mostrarMensaje("No se encontró factura.jrxml"); return; }

            // Datos del cliente con la misma conexión
            String nombre = "", cedula = "", tel = "";
            try (PreparedStatement ps = con.prepareStatement(
                    "SELECT p.nombre, p.cedula, p.tel " +
                    "FROM tbl_cliente c JOIN tbl_persona p ON c.id_persona = p.id_persona " +
                    "WHERE c.id_cliente = ?")) {
                ps.setInt(1, idClienteSeleccionado);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        nombre = rs.getString("nombre") != null ? rs.getString("nombre") : "";
                        cedula = rs.getString("cedula") != null ? rs.getString("cedula") : "";
                        tel    = rs.getString("tel")    != null ? rs.getString("tel")    : "";
                    }
                }
            }

            Map<String, Object> params = new HashMap<>();
            params.put("ID_PEDIDO",  ultimoIdPedido);
            params.put("LOGO_PATH",  "");
            params.put("P_CLIENTE",  nombre);
            params.put("P_CEDULA",   cedula);
            params.put("P_TEL",      tel);
            params.put("P_FECHA",    LocalDate.now().toString());
            params.put("P_EMPLEADO", CONTROLLER_Seccion.getInstancia().getNombre());

            JasperReport reporte = JasperCompileManager.compileReport(jrxml);
            JasperPrint  print   = JasperFillManager.fillReport(reporte, params, con);
            JasperViewer.viewReport(print, false);

            // Notificar por correo que se generó la factura
            com.example.demo1.Utils.CorreoUtil.notificarFacturaGenerada(
                    ultimoIdPedido,
                    nombre,
                    LocalDate.now().toString());

        } catch (Exception e) {
            mostrarMensaje("Error al generar factura: " + e.getMessage());
            System.err.println("[Factura] " + e.getMessage());
        } finally {
            cerrarConexion(con);
        }
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

    public static class Extra {
        final int        idIngrediente;
        final String     nombre;
        final BigDecimal precio;

        public Extra(int id, String nombre, BigDecimal precio) {
            this.idIngrediente = id;
            this.nombre        = nombre != null ? nombre : "";
            this.precio        = precio != null ? precio : BigDecimal.ZERO;
        }

        @Override
        public String toString() {
            if (precio.compareTo(BigDecimal.ZERO) == 0) return nombre;
            return nombre + "  (+$" + precio.setScale(2, RoundingMode.HALF_UP).toPlainString() + ")";
        }
    }

    public static class Producto {
        final int        idProducto;
        final String     nombre;
        final String     tipo;
        final BigDecimal precio;
        final BigDecimal descuento;   // porcentaje de descuento (0 = sin oferta)

        public Producto(int id, String nombre, String tipo,
                        BigDecimal precio, BigDecimal descuento) {
            this.idProducto = id;
            this.nombre     = nombre    != null ? nombre    : "";
            this.tipo       = tipo      != null ? tipo      : "";
            this.precio     = precio    != null ? precio    : BigDecimal.ZERO;
            this.descuento  = descuento != null ? descuento : BigDecimal.ZERO;
        }
    }

    public static class ItemCarrito {
        final int        idProducto;
        final String     nombre;
        final String     tipo;
        final BigDecimal precioUnitario;
        int              cantidad;

        public ItemCarrito(int id, String nombre, String tipo, BigDecimal precio, int cantidad) {
            this.idProducto     = id;
            this.nombre         = nombre != null ? nombre : "";
            this.tipo           = tipo   != null ? tipo.toLowerCase() : "";
            this.precioUnitario = precio != null ? precio : BigDecimal.ZERO;
            this.cantidad       = cantidad;
        }
    }

    @FXML
    private void FnExportarReporteVentas() {
        JasperUtil.exportarPDF(
                "/com/example/demo1/reportes/Reporte_Ventas.jrxml",
                "Reporte_Ventas.pdf"
        );
    }
}
