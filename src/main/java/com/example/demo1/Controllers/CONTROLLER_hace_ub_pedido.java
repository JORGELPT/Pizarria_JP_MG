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
import java.util.Optional;
import javafx.geometry.Insets;
import javafx.scene.Node;


public class CONTROLLER_hace_ub_pedido {

    //  FXML
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

    @FXML private Label        lblSubtotal;
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
    @FXML private Label        lblOrderId;
    @FXML private TextField    txtCodigoPromo;
    @FXML private Label        lblPromoStatus;

    //  Paneles de pago
    @FXML private VBox         vboxPagoEfectivo;
    @FXML private Label        lblMontoEfectivo;
    @FXML private TextField    txtCantidadRecibida;
    @FXML private Label        lblCambio;

    @FXML private VBox         vboxPagoTarjeta;
    @FXML private Label        lblMontoTarjeta;
    @FXML private TextField    txtNombreTarjeta;
    @FXML private TextField    txtNumeroTarjeta;
    @FXML private TextField    txtVencimiento;
    @FXML private TextField    txtCVV;
    @FXML private Label        lblEstadoTarjeta;

    @FXML private VBox         vboxPagoEWallet;
    @FXML private Label        lblMontoEWallet;
    @FXML private TextField    txtReferenciaEWallet;
    @FXML private Label        lblEstadoEWallet;

    private boolean pagoTarjetaConfirmado = false;
    private boolean pagoEWalletConfirmado = false;

    // ── Pill activo actual ───────────────────────────────────────────────────
    private Button       btnCatActivo;
    private ToggleButton tglEntActivo;

    // ── Estado ───────────────────────────────────────────────────────────────
    private int    idClienteSeleccionado = -1;
    private int    ultimoIdPedido        = -1;
    private int    idPedidoCargado       = -1;
    private String tipoEntrega           = "En Local";
    private String metodoPago            = "Efectivo";

    private final ObservableList<Extra>      listaExtras        = FXCollections.observableArrayList();
    private final List<ItemCarrito>          carrito            = new ArrayList<>();
    private final ObservableList<Producto>   todosLosProductos  = FXCollections.observableArrayList();

    private static final BigDecimal TASA_ITBIS  = new BigDecimal("0.18");
    private              boolean    soloOfertas = false;

    private BigDecimal promoDescuento  = BigDecimal.ZERO;
    private String     promoNombre     = "";
    private int        promoIdProducto = -1;

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
        listaExtras.clear();
        listaExtras.add(new Extra(0, "Sin extra", BigDecimal.ZERO));

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
                        listaExtras.add(new Extra(rs.getInt("id_ingrediente"),
                                                  rs.getString("nombre"), p));
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[HacerPedido] cargarExtras: " + e.getMessage());
        } finally {
            cerrarConexion(con);
        }
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
                        String nombre = rs.getString("nombre");
                        String tipo   = rs.getString("tipo");
                        if (precio == null || precio.compareTo(BigDecimal.ZERO) == 0)
                            precio = precioDefecto(nombre, tipo);
                        todosLosProductos.add(new Producto(
                                rs.getInt("id_producto"),
                                nombre,
                                tipo,
                                precio,
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
        String sql = "SELECT op.id_producto, o.descuento_porcentaje " +
                     "FROM tbl_oferta o " +
                     "JOIN tbl_oferta_producto op ON op.id_oferta = o.id_oferta " +
                     "WHERE o.activa = 1 " +
                     "  AND (o.fecha_inicio IS NULL OR o.fecha_inicio <= CAST(GETDATE() AS DATE)) " +
                     "  AND (o.fecha_fin   IS NULL OR o.fecha_fin   >= CAST(GETDATE() AS DATE))";
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
    // Factory metod
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

        javafx.scene.image.Image imgCargada = buscarImagenProducto(p.nombre);
        if (imgCargada != null) {
            javafx.scene.image.ImageView iv = new javafx.scene.image.ImageView(imgCargada);
            iv.setFitWidth(145);
            iv.setFitHeight(95);
            iv.setPreserveRatio(true);
            imgBox.setStyle("-fx-background-color: #f4f6fb; -fx-background-radius: 10;");
            imgBox.getChildren().add(iv);
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

        for (ItemCarrito item : carrito) {
            BigDecimal lineaBase  = item.precioUnitario.multiply(BigDecimal.valueOf(item.cantidad));
            BigDecimal lineaExtra = (item.extra != null) ? item.extra.precio : BigDecimal.ZERO;
            BigDecimal lineaTotal = lineaBase.add(lineaExtra);

            // ── Imagen circular ──
            javafx.scene.layout.StackPane imgCircle = new javafx.scene.layout.StackPane();
            imgCircle.setPrefSize(52, 52);
            imgCircle.setMinSize(52, 52);
            imgCircle.setMaxSize(52, 52);
            imgCircle.setStyle("-fx-background-color: #252b45; -fx-background-radius: 26;");

            javafx.scene.image.Image img = buscarImagenProducto(item.nombre);
            if (img != null) {
                javafx.scene.image.ImageView iv = new javafx.scene.image.ImageView(img);
                iv.setFitWidth(52);
                iv.setFitHeight(52);
                iv.setPreserveRatio(true);
                javafx.scene.shape.Circle clip = new javafx.scene.shape.Circle(26, 26, 26);
                iv.setClip(clip);
                imgCircle.getChildren().add(iv);
            }

            // ── Info + controles cantidad ──
            Label lNombre = new Label(item.nombre);
            lNombre.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: white;" +
                             "-fx-font-family: 'Segoe UI';");
            lNombre.setWrapText(true);
            lNombre.setMaxWidth(Double.MAX_VALUE);

            String tipoDisplay = (item.tipo != null && !item.tipo.isBlank())
                    ? item.tipo.substring(0,1).toUpperCase() + item.tipo.substring(1).toLowerCase()
                    : "Producto";
            Label lTipo = new Label(tipoDisplay + "  •  $" +
                    item.precioUnitario.setScale(2, RoundingMode.HALF_UP).toPlainString() + " c/u");
            lTipo.setStyle("-fx-font-size: 10px; -fx-text-fill: rgba(255,255,255,0.42);");

            // Controles cantidad oscuros
            Button btnMenos = crearBtnCantidadDark("−");
            Label  lCant    = new Label(String.valueOf(item.cantidad));
            lCant.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: white;" +
                           "-fx-min-width: 22; -fx-alignment: center;");
            Button btnMas   = crearBtnCantidadDark("+");

            btnMenos.setOnAction(e -> {
                item.cantidad--;
                if (item.cantidad <= 0) carrito.remove(item);
                refrescarPanel();
            });
            btnMas.setOnAction(e -> { item.cantidad++; refrescarPanel(); });

            HBox qtyBox = new HBox(6, btnMenos, lCant, btnMas);
            qtyBox.setAlignment(Pos.CENTER_LEFT);

            // ── Chip del extra seleccionado (se muestra en la tarjeta) ──
            Label lExtraChip = new Label();
            lExtraChip.setStyle(
                    "-fx-font-size: 9.5px; -fx-font-weight: bold; -fx-text-fill: #4cd97b;" +
                    "-fx-background-color: rgba(76,217,123,0.13); -fx-background-radius: 5;" +
                    "-fx-padding: 1 7;");
            if (item.extra != null) {
                lExtraChip.setText("✚ " + item.extra.nombre);
                lExtraChip.setVisible(true);
                lExtraChip.setManaged(true);
            } else {
                lExtraChip.setVisible(false);
                lExtraChip.setManaged(false);
            }

            VBox infoBox = new VBox(3, lNombre, lTipo, lExtraChip, qtyBox);
            HBox.setHgrow(infoBox, Priority.ALWAYS);
            infoBox.setMaxWidth(Double.MAX_VALUE);

            // ── Lado derecho: total + botón editar ──
            Label lTotal = new Label("$" + lineaTotal.setScale(2, RoundingMode.HALF_UP).toPlainString());
            lTotal.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: white;");

            Button btnEdit = new Button("✎");
            final String EDIT_BASE = "-fx-background-color: #252b45; -fx-text-fill: rgba(255,255,255,0.65);" +
                    "-fx-background-radius: 15; -fx-min-width: 30; -fx-min-height: 30;" +
                    "-fx-max-width: 30; -fx-max-height: 30; -fx-cursor: hand; -fx-font-size: 13px;";
            final String EDIT_ACTIVE = "-fx-background-color: #004aad; -fx-text-fill: white;" +
                    "-fx-background-radius: 15; -fx-min-width: 30; -fx-min-height: 30;" +
                    "-fx-max-width: 30; -fx-max-height: 30; -fx-cursor: hand; -fx-font-size: 13px;";
            btnEdit.setStyle(EDIT_BASE);

            VBox rightBox = new VBox(6, lTotal, btnEdit);
            rightBox.setAlignment(Pos.CENTER_RIGHT);

            // ── Fila principal ──
            HBox mainRow = new HBox(10, imgCircle, infoBox, rightBox);
            mainRow.setAlignment(Pos.CENTER_LEFT);

            // ── Panel de extras (oculto por defecto) ──
            ComboBox<Extra> cmbItemExtra = new ComboBox<>(listaExtras);
            cmbItemExtra.setMaxWidth(Double.MAX_VALUE);
            cmbItemExtra.setStyle(
                    "-fx-background-color: rgba(255,255,255,0.08); -fx-background-radius: 8;" +
                    "-fx-border-color: rgba(255,255,255,0.18); -fx-border-radius: 8; -fx-border-width: 1;" +
                    "-fx-font-size: 11px;");
            if (item.extra != null) cmbItemExtra.getSelectionModel().select(item.extra);
            else                    cmbItemExtra.getSelectionModel().selectFirst();

            cmbItemExtra.getSelectionModel().selectedItemProperty().addListener(
                    (obs, anterior, nuevo) -> {
                        item.extra = (nuevo != null && nuevo.precio.compareTo(BigDecimal.ZERO) > 0)
                                ? nuevo : null;
                        // Actualizar chip visible en la tarjeta
                        if (item.extra != null) {
                            lExtraChip.setText("✚ " + item.extra.nombre);
                            lExtraChip.setVisible(true);
                            lExtraChip.setManaged(true);
                        } else {
                            lExtraChip.setVisible(false);
                            lExtraChip.setManaged(false);
                        }
                        // Actualizar total de línea en tiempo real
                        BigDecimal nuevaLinea = item.precioUnitario
                                .multiply(BigDecimal.valueOf(item.cantidad))
                                .add(item.extra != null ? item.extra.precio : BigDecimal.ZERO);
                        lTotal.setText("$" + nuevaLinea.setScale(2, RoundingMode.HALF_UP).toPlainString());
                        recalcularTotales();
                    });

            Label lExtraLabel = new Label("Extra:");
            lExtraLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: rgba(255,255,255,0.50);");
            HBox extraRow = new HBox(8, lExtraLabel, cmbItemExtra);
            HBox.setHgrow(cmbItemExtra, Priority.ALWAYS);
            extraRow.setAlignment(Pos.CENTER_LEFT);
            extraRow.setVisible(false);
            extraRow.setManaged(false);

            // Botón editar: alterna panel de extras
            btnEdit.setOnAction(e -> {
                boolean showing = extraRow.isVisible();
                extraRow.setVisible(!showing);
                extraRow.setManaged(!showing);
                btnEdit.setStyle(!showing ? EDIT_ACTIVE : EDIT_BASE);
            });

            // ── Tarjeta completa del ítem ──
            VBox itemCard = new VBox(8, mainRow, extraRow);
            itemCard.setStyle("-fx-background-color: #1c2138; -fx-background-radius: 14;" +
                              "-fx-padding: 12 12 12 12;");

            vboxItems.getChildren().add(itemCard);
        }

        // Tarjeta de descuento promo (aparece como una línea más del carrito)
        if (promoDescuento.compareTo(BigDecimal.ZERO) > 0) {
            vboxItems.getChildren().add(crearPromoCard());
        }

        recalcularTotales();
    }

    private VBox crearPromoCard() {
        VBox card = new VBox(4);
        card.setStyle("-fx-background-color: rgba(76,217,123,0.13); -fx-background-radius: 14;" +
                      "-fx-border-color: rgba(76,217,123,0.30); -fx-border-radius: 14; -fx-border-width: 1;" +
                      "-fx-padding: 10 12;");

        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);

        Label lTag = new Label("🏷");
        lTag.setStyle("-fx-font-size: 20px;");

        VBox info = new VBox(2);
        HBox.setHgrow(info, Priority.ALWAYS);
        Label lNombre = new Label(promoNombre);
        lNombre.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #4cd97b;");
        lNombre.setWrapText(true);
        Label lSub = new Label("Código promocional aplicado");
        lSub.setStyle("-fx-font-size: 9.5px; -fx-text-fill: rgba(255,255,255,0.40);");
        info.getChildren().addAll(lNombre, lSub);

        Label lDesc = new Label("-$" + promoDescuento.toPlainString());
        lDesc.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #4cd97b;");

        Button btnQuitar = new Button("✕");
        btnQuitar.setStyle("-fx-background-color: rgba(255,255,255,0.10); -fx-text-fill: rgba(255,255,255,0.55);" +
                           "-fx-background-radius: 50; -fx-cursor: hand; -fx-font-size: 10px;" +
                           "-fx-min-width: 22; -fx-min-height: 22; -fx-max-width: 22; -fx-max-height: 22;");
        btnQuitar.setOnAction(e -> {
            promoDescuento  = BigDecimal.ZERO;
            promoNombre     = "";
            promoIdProducto = -1;
            if (txtCodigoPromo != null) txtCodigoPromo.clear();
            if (lblPromoStatus != null) lblPromoStatus.setText("");
            refrescarPanel();
        });

        row.getChildren().addAll(lTag, info, lDesc, btnQuitar);
        card.getChildren().add(row);
        return card;
    }

    private void recalcularTotales() {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (ItemCarrito item : carrito) {
            subtotal = subtotal.add(
                    item.precioUnitario.multiply(BigDecimal.valueOf(item.cantidad)));
            if (item.extra != null) subtotal = subtotal.add(item.extra.precio);
        }
        subtotal = subtotal.subtract(promoDescuento);
        if (subtotal.compareTo(BigDecimal.ZERO) < 0) subtotal = BigDecimal.ZERO;
        BigDecimal itbis = subtotal.multiply(TASA_ITBIS).setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = subtotal.add(itbis).setScale(2, RoundingMode.HALF_UP);
        subtotal = subtotal.setScale(2, RoundingMode.HALF_UP);

        if (lblSubtotal  != null) lblSubtotal .setText("$" + subtotal.toPlainString());
        if (lblItbs      != null) lblItbs     .setText("$" + itbis.toPlainString());
        if (lblTotal     != null) lblTotal    .setText("$" + total.toPlainString());

        // Actualizar monto en el panel de pago que esté activo
        if ((vboxPagoEfectivo != null && vboxPagoEfectivo.isVisible()) ||
            (vboxPagoTarjeta  != null && vboxPagoTarjeta.isVisible())  ||
            (vboxPagoEWallet  != null && vboxPagoEWallet.isVisible())) {
            actualizarMontosPago();
        }
    }

    private Button crearBtnCantidad(String texto, String bgColor, String textColor) {
        Button btn = new Button(texto);
        btn.setStyle(
                "-fx-background-color: " + bgColor + "; -fx-text-fill: " + textColor + ";" +
                "-fx-min-width: 26; -fx-min-height: 26; -fx-max-width: 26; -fx-max-height: 26;" +
                "-fx-background-radius: 13; -fx-cursor: hand; -fx-font-weight: bold;");
        return btn;
    }

    private Button crearBtnCantidadDark(String texto) {
        Button btn = new Button(texto);
        btn.getStyleClass().add("btn-qty-dark");
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
        pagoTarjetaConfirmado = false;
        deselectToggle(tglEfectivo, tglTarjeta, tglEWallet);
        actualizarPillPago(tglEfectivo);
        mostrarPanelPago("efectivo");
        actualizarMontosPago();
        mostrarMensaje("");
    }

    @FXML private void FnPagoTarjeta() {
        metodoPago = "Tarjeta";
        pagoTarjetaConfirmado = false;
        deselectToggle(tglEfectivo, tglTarjeta, tglEWallet);
        actualizarPillPago(tglTarjeta);
        mostrarPanelPago("tarjeta");
        actualizarMontosPago();
        mostrarMensaje("");
    }

    @FXML private void FnPagoEWallet() {
        metodoPago = "E-Wallet";
        pagoTarjetaConfirmado = false;
        pagoEWalletConfirmado = false;
        deselectToggle(tglEfectivo, tglTarjeta, tglEWallet);
        actualizarPillPago(tglEWallet);
        mostrarPanelPago("ewallet");
        actualizarMontosPago();
        mostrarMensaje("");
    }

    /** Muestra el panel de pago correspondiente y oculta los otros. */
    private void mostrarPanelPago(String tipo) {
        boolean esEfectivo = "efectivo".equals(tipo);
        boolean esTarjeta  = "tarjeta".equals(tipo);
        boolean esEWallet  = "ewallet".equals(tipo);
        if (vboxPagoEfectivo != null) {
            vboxPagoEfectivo.setVisible(esEfectivo);
            vboxPagoEfectivo.setManaged(esEfectivo);
        }
        if (vboxPagoTarjeta != null) {
            vboxPagoTarjeta.setVisible(esTarjeta);
            vboxPagoTarjeta.setManaged(esTarjeta);
        }
        if (vboxPagoEWallet != null) {
            vboxPagoEWallet.setVisible(esEWallet);
            vboxPagoEWallet.setManaged(esEWallet);
        }
    }

    /** Actualiza los labels de monto en los paneles de pago. */
    private void actualizarMontosPago() {
        BigDecimal total = calcularTotal();
        String montoStr  = "$" + total.setScale(2, RoundingMode.HALF_UP).toPlainString();
        if (lblMontoEfectivo != null) lblMontoEfectivo.setText(montoStr);
        if (lblMontoTarjeta  != null) lblMontoTarjeta.setText(montoStr);
        if (lblMontoEWallet  != null) lblMontoEWallet.setText(montoStr);
        // Recalcular cambio si ya hay un valor ingresado
        if (txtCantidadRecibida != null && !txtCantidadRecibida.getText().trim().isEmpty()) {
            FnCalcularCambio();
        }
    }

    /** Calcula y muestra el cambio en efectivo. */
    @FXML
    private void FnCalcularCambio() {
        if (txtCantidadRecibida == null || lblCambio == null) return;
        try {
            BigDecimal total    = calcularTotal();
            String     inputStr = txtCantidadRecibida.getText().trim();
            if (inputStr.isEmpty()) { lblCambio.setText("$0.00"); return; }
            BigDecimal recibido = new BigDecimal(inputStr);
            BigDecimal cambio   = recibido.subtract(total);
            if (cambio.compareTo(BigDecimal.ZERO) < 0) {
                lblCambio.setText("⚠ Insuficiente");
                lblCambio.setStyle("-fx-text-fill: #ff6b6b; -fx-font-weight: bold; -fx-font-size: 14px;");
            } else {
                lblCambio.setText("$" + cambio.setScale(2, RoundingMode.HALF_UP).toPlainString());
                lblCambio.setStyle("-fx-text-fill: #4cd97b; -fx-font-weight: bold; -fx-font-size: 14px;");
            }
        } catch (NumberFormatException e) {
            lblCambio.setText("Monto inválido");
            lblCambio.setStyle("-fx-text-fill: #ff6b6b; -fx-font-weight: bold; -fx-font-size: 14px;");
        }
    }

    /** Valida y confirma el pago con tarjeta. */
    @FXML
    private void FnConfirmarTarjeta() {
        String nombre = txtNombreTarjeta != null ? txtNombreTarjeta.getText().trim() : "";
        String numero = txtNumeroTarjeta != null ? txtNumeroTarjeta.getText().trim() : "";
        String venc   = txtVencimiento   != null ? txtVencimiento.getText().trim()   : "";
        String cvv    = txtCVV           != null ? txtCVV.getText().trim()           : "";

        if (nombre.isEmpty() || numero.isEmpty() || venc.isEmpty() || cvv.isEmpty()) {
            if (lblEstadoTarjeta != null) {
                lblEstadoTarjeta.setText("⚠ Complete todos los campos de la tarjeta.");
                lblEstadoTarjeta.setStyle(
                        "-fx-text-fill: #ff6b6b; -fx-font-size: 11px; -fx-font-weight: bold;");
            }
            return;
        }
        pagoTarjetaConfirmado = true;
        if (lblEstadoTarjeta != null) {
            lblEstadoTarjeta.setText("✓ Pago con tarjeta confirmado.");
            lblEstadoTarjeta.setStyle(
                    "-fx-text-fill: #4cd97b; -fx-font-size: 11px; -fx-font-weight: bold;");
        }
    }

    /** Valida y confirma el pago con E-Wallet. */
    @FXML
    private void FnConfirmarEWallet() {
        String ref = txtReferenciaEWallet != null ? txtReferenciaEWallet.getText().trim() : "";
        if (ref.isEmpty()) {
            if (lblEstadoEWallet != null) {
                lblEstadoEWallet.setText("⚠ Ingrese el número de referencia.");
                lblEstadoEWallet.setStyle(
                        "-fx-text-fill: #ff6b6b; -fx-font-size: 11px; -fx-font-weight: bold;");
            }
            return;
        }
        pagoEWalletConfirmado = true;
        if (lblEstadoEWallet != null) {
            lblEstadoEWallet.setText("✓ Pago E-Wallet confirmado. Ref: " + ref);
            lblEstadoEWallet.setStyle(
                    "-fx-text-fill: #4cd97b; -fx-font-size: 11px; -fx-font-weight: bold;");
        }
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

    // ── Código promocional ───────────────────────────────────────────────────

    @FXML
    private void FnAplicarPromo() {
        String codigo = (txtCodigoPromo != null) ? txtCodigoPromo.getText().trim().toUpperCase() : "";
        if (codigo.isEmpty()) {
            setPromoStatus("Ingrese un código promocional.", false);
            return;
        }
        if (carrito.isEmpty()) {
            setPromoStatus("Agregue productos al carrito primero.", false);
            return;
        }

        String sql = "SELECT id_oferta, nombre, descuento_porcentaje, id_producto " +
                     "FROM tbl_oferta " +
                     "WHERE UPPER(codigo_promocional) = ? " +
                     "  AND activa = 1 " +
                     "  AND (fecha_inicio IS NULL OR fecha_inicio <= CAST(GETDATE() AS DATE)) " +
                     "  AND (fecha_fin   IS NULL OR fecha_fin   >= CAST(GETDATE() AS DATE))";

        Connection con = null;
        try {
            con = Conexion.establecerConexion();
            if (con == null) { setPromoStatus("Sin conexión.", false); return; }

            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, codigo);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        setPromoStatus("Código inválido o expirado.", false);
                        promoDescuento  = BigDecimal.ZERO;
                        promoNombre     = "";
                        promoIdProducto = -1;
                        refrescarPanel();
                        return;
                    }

                    String     nombre  = rs.getString("nombre");
                    BigDecimal pct     = rs.getBigDecimal("descuento_porcentaje");
                    int        idOf    = rs.getInt("id_oferta");

                    // Obtener productos asignados a esta oferta
                    java.util.List<Integer> prodIds = new java.util.ArrayList<>();
                    try (PreparedStatement psP = con.prepareStatement(
                            "SELECT id_producto FROM tbl_oferta_producto WHERE id_oferta = ?")) {
                        psP.setInt(1, idOf);
                        try (ResultSet rsP = psP.executeQuery()) {
                            while (rsP.next()) prodIds.add(rsP.getInt("id_producto"));
                        }
                    }

                    // Calcular base: sin productos = todos; con productos = solo los coincidentes
                    BigDecimal base = BigDecimal.ZERO;
                    if (prodIds.isEmpty()) {
                        for (ItemCarrito item : carrito)
                            base = base.add(item.precioUnitario.multiply(BigDecimal.valueOf(item.cantidad)));
                        promoIdProducto = -1;
                    } else {
                        for (ItemCarrito item : carrito) {
                            if (prodIds.contains(item.idProducto))
                                base = base.add(item.precioUnitario.multiply(BigDecimal.valueOf(item.cantidad)));
                        }
                        promoIdProducto = prodIds.get(0);
                        if (base.compareTo(BigDecimal.ZERO) == 0) {
                            setPromoStatus("Este código aplica a productos que no están en el carrito.", false);
                            return;
                        }
                    }

                    promoDescuento = base.multiply(pct.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP))
                                        .setScale(2, RoundingMode.HALF_UP);
                    promoNombre    = nombre + "  (" + pct.setScale(0, RoundingMode.HALF_UP).toPlainString() + "% OFF)";

                    setPromoStatus("✓ Aplicado: -$" + promoDescuento.toPlainString(), true);
                    refrescarPanel();
                }
            }
        } catch (Exception e) {
            setPromoStatus("Error: " + e.getMessage(), false);
        } finally {
            cerrarConexion(con);
        }
    }

    private void setPromoStatus(String msg, boolean exito) {
        if (lblPromoStatus == null) return;
        lblPromoStatus.setText(msg);
        lblPromoStatus.setStyle(exito
                ? "-fx-font-size: 10px; -fx-text-fill: #4cd97b; -fx-font-weight: bold;"
                : "-fx-font-size: 10px; -fx-text-fill: #ff6b6b;");
    }

    // ── Buscar cliente ───────────────────────────────────────────────────────

    @FXML
    private void FnBuscarCliente() {
        String input = (txtCedula != null) ? txtCedula.getText().trim() : "";
        if (input.isEmpty()) {
            mostrarMensaje("Ingrese la cédula o nombre del cliente.");
            return;
        }

        boolean esCedula = input.matches("\\d+");

        final String sql = esCedula
                ? "SELECT c.id_cliente, p.nombre " +
                  "FROM   tbl_cliente c " +
                  "INNER JOIN tbl_persona p ON c.id_persona = p.id_persona " +
                  "WHERE  p.cedula = ?"
                : "SELECT c.id_cliente, p.nombre " +
                  "FROM   tbl_cliente c " +
                  "INNER JOIN tbl_persona p ON c.id_persona = p.id_persona " +
                  "WHERE  LOWER(p.nombre) LIKE LOWER(?) " +
                  "ORDER BY p.nombre";

        Connection con = null;
        try {
            con = Conexion.establecerConexion();
            if (con == null) { mostrarMensaje("Sin conexión."); return; }

            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, esCedula ? input : "%" + input + "%");
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        idClienteSeleccionado = rs.getInt("id_cliente");
                        String nombreCliente  = rs.getString("nombre");
                        if (lblCliente != null) lblCliente.setText(nombreCliente);
                        // Si la búsqueda por nombre devolvió más de un resultado, informar
                        if (!esCedula && rs.next()) {
                            mostrarMensaje("Varios clientes encontrados, seleccionado: " + nombreCliente);
                        } else {
                            mostrarMensaje("");
                        }
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

        // ── Validar pago según método seleccionado ──
        if ("Efectivo".equals(metodoPago)) {
            String recibidoStr = txtCantidadRecibida != null
                    ? txtCantidadRecibida.getText().trim() : "";
            if (recibidoStr.isEmpty()) {
                mostrarMensaje("Ingrese la cantidad recibida en efectivo.");
                return;
            }
            try {
                BigDecimal recibido = new BigDecimal(recibidoStr);
                if (recibido.compareTo(calcularTotal()) < 0) {
                    mostrarMensaje("La cantidad recibida es menor al total a pagar.");
                    return;
                }
            } catch (NumberFormatException e) {
                mostrarMensaje("La cantidad recibida no es un número válido.");
                return;
            }
        } else if ("Tarjeta".equals(metodoPago) && !pagoTarjetaConfirmado) {
            mostrarMensaje("Confirme el pago con tarjeta antes de continuar.");
            return;
        } else if ("E-Wallet".equals(metodoPago) && !pagoEWalletConfirmado) {
            mostrarMensaje("Confirme el pago E-Wallet antes de continuar.");
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

            int idPedido;
            if (idPedidoCargado != -1) {
                // Actualizar pedido existente
                final String sqlPed =
                        "UPDATE tbl_pedido " +
                        "SET tipo_entrega = ?, precio_total = ?, id_cliente = ?, id_empleado = ?, id_itbs = ?, metodo_pago = ?, estado = 'Activo' " +
                        "WHERE id_pedido = ?";
                try (PreparedStatement ps = con.prepareStatement(sqlPed)) {
                    ps.setString(1, tipoEntrega);
                    ps.setDouble(2, total.doubleValue());
                    ps.setInt   (3, idClienteSeleccionado);
                    if (idEmpl == -1) ps.setNull(4, Types.INTEGER);
                    else              ps.setInt (4, idEmpl);
                    ps.setInt   (5, idItbs);
                    ps.setString(6, metodoPago);
                    ps.setInt   (7, idPedidoCargado);
                    ps.executeUpdate();
                }
                idPedido = idPedidoCargado;

                // Eliminar detalles anteriores
                try (PreparedStatement psDel = con.prepareStatement(
                        "DELETE FROM tbl_producto_pedido WHERE id_pedido = ?")) {
                    psDel.setInt(1, idPedido);
                    psDel.executeUpdate();
                }
            } else {
                // 1) Cabecera del pedido (Insert nuevo)
                final String sqlPed =
                        "INSERT INTO tbl_pedido " +
                        "  (tipo_entrega, tiempo_realizacion, precio_total, fecha_pedido," +
                        "   id_cliente, id_empleado, id_itbs, metodo_pago) " +
                        "VALUES (?, CAST('00:30:00' AS TIME), ?, CAST(GETDATE() AS DATE)," +
                        "        ?, ?, ?, ?)";

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
            }

            // 2) Detalles del pedido — precio_unitario se guarda al momento de la venta
            final String sqlDet =
                    "INSERT INTO tbl_producto_pedido " +
                    "  (id_producto, id_pedido, cantidad_producto, id_presentacion, especificaciones, precio_unitario)" +
                    "VALUES (?, ?, ?, " +
                    "  ISNULL((SELECT TOP 1 id_presentacion FROM tbl_presentacion_producto " +
                    "          WHERE id_producto = ?), " +
                    "         (SELECT TOP 1 id_presentacion FROM tbl_presentacion ORDER BY id_presentacion)), ?, ?)";

            try (PreparedStatement ps = con.prepareStatement(sqlDet)) {
                for (ItemCarrito item : carrito) {
                    String notaItem = (item.extra != null)
                            ? "Extra: " + item.extra.nombre +
                              " (+$" + item.extra.precio.toPlainString() + ")"
                            : "";
                    ps.setInt       (1, item.idProducto);
                    ps.setInt       (2, idPedido);
                    ps.setInt       (3, item.cantidad);
                    ps.setInt       (4, item.idProducto);
                    ps.setString    (5, notaItem);
                    ps.setBigDecimal(6, item.precioUnitario);
                    ps.addBatch();
                }
                ps.executeBatch();
            }

            // Guardar descuento promocional como línea negativa en el pedido
            if (promoDescuento.compareTo(BigDecimal.ZERO) > 0 && !carrito.isEmpty()) {
                int idProdPromo = (promoIdProducto != -1) ? promoIdProducto : carrito.get(0).idProducto;
                try (PreparedStatement psPromo = con.prepareStatement(
                        "INSERT INTO tbl_producto_pedido " +
                        "  (id_producto, id_pedido, cantidad_producto, id_presentacion, especificaciones, precio_unitario)" +
                        "VALUES (?, ?, 1, " +
                        "  ISNULL((SELECT TOP 1 id_presentacion FROM tbl_presentacion_producto WHERE id_producto = ?), " +
                        "         (SELECT TOP 1 id_presentacion FROM tbl_presentacion ORDER BY id_presentacion)), ?, ?)")) {
                    psPromo.setInt       (1, idProdPromo);
                    psPromo.setInt       (2, idPedido);
                    psPromo.setInt       (3, idProdPromo);
                    psPromo.setString    (4, "Descuento: " + promoNombre);
                    psPromo.setBigDecimal(5, promoDescuento.negate());
                    psPromo.executeUpdate();
                }
            }

            // Auto-crear envío cuando el tipo de entrega es Delivery
            if ("Delivery".equals(tipoEntrega)) {
                String dir = (txtDireccion   != null) ? txtDireccion.getText().trim()   : "";
                String obs = (txtObservacion != null) ? txtObservacion.getText().trim()  : "";
                if (!dir.isEmpty()) {
                    // Primero borrar si ya existe un envío anterior (caso cuenta abierta modificada)
                    try (PreparedStatement psDelEnv = con.prepareStatement("DELETE FROM tbl_envio WHERE id_pedido = ?")) {
                        psDelEnv.setInt(1, idPedido);
                        psDelEnv.executeUpdate();
                    }
                    try (PreparedStatement psEnv = con.prepareStatement(
                            "INSERT INTO tbl_envio (id_pedido, direccion, costo_servicio, id_metodo_envio, observacion) " +
                            "VALUES (?, ?, ?, 1, ?)")) {
                        psEnv.setInt      (1, idPedido);
                        psEnv.setString   (2, dir);
                        psEnv.setBigDecimal(3, java.math.BigDecimal.ZERO);
                        psEnv.setString   (4, obs.isEmpty() ? null : obs);
                        psEnv.executeUpdate();
                    }
                }
            }

            con.commit();
            ultimoIdPedido = idPedido;
            if (lblOrderId != null) lblOrderId.setText("Order ID: #" + idPedido);
            if (idPedidoCargado != -1) {
                mostrarMensaje("✅ Cuenta Abierta #" + idPedido + " pagada y registrada correctamente.");
            } else {
                mostrarMensaje("✅ Pedido #" + idPedido + " registrado correctamente.");
            }
            FnLimpiar();

        } catch (Exception e) {
            try { if (con != null) con.rollback(); } catch (Exception ignored) {}
            mostrarMensaje("Error al guardar: " + e.getMessage());
            System.err.println("[HacerPedido] FnGuardar: " + e.getMessage());
        } finally {
            cerrarConexion(con);
        }
    }


    @FXML
    private void FnCuentaAbierta() {
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

            int idItbs = 1;
            try (PreparedStatement ps = con.prepareStatement(
                    "SELECT TOP 1 id_itbs FROM tbl_itbs");
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) idItbs = rs.getInt(1);
            }

            int idPedido;
            if (idPedidoCargado != -1) {
                // Actualizar cuenta abierta existente
                final String sqlPed =
                        "UPDATE tbl_pedido " +
                        "SET tipo_entrega = ?, precio_total = ?, id_cliente = ?, id_empleado = ?, id_itbs = ?, metodo_pago = 'Cuenta Abierta', estado = 'abierta' " +
                        "WHERE id_pedido = ?";
                try (PreparedStatement ps = con.prepareStatement(sqlPed)) {
                    ps.setString(1, tipoEntrega);
                    ps.setDouble(2, total.doubleValue());
                    ps.setInt   (3, idClienteSeleccionado);
                    if (idEmpl == -1) ps.setNull(4, Types.INTEGER);
                    else              ps.setInt (4, idEmpl);
                    ps.setInt   (5, idItbs);
                    ps.setInt   (6, idPedidoCargado);
                    ps.executeUpdate();
                }
                idPedido = idPedidoCargado;

                // Eliminar detalles anteriores
                try (PreparedStatement psDel = con.prepareStatement(
                        "DELETE FROM tbl_producto_pedido WHERE id_pedido = ?")) {
                    psDel.setInt(1, idPedido);
                    psDel.executeUpdate();
                }
            } else {
                // Crear nueva cuenta abierta
                final String sqlPed =
                        "INSERT INTO tbl_pedido " +
                        "  (tipo_entrega, tiempo_realizacion, precio_total, fecha_pedido," +
                        "   id_cliente, id_empleado, id_itbs, metodo_pago, estado) " +
                        "VALUES (?, CAST('00:30:00' AS TIME), ?, CAST(GETDATE() AS DATE)," +
                        "        ?, ?, ?, ?, 'abierta')";

                try (PreparedStatement ps = con.prepareStatement(sqlPed,
                                                                 Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, tipoEntrega);
                    ps.setDouble(2, total.doubleValue());
                    ps.setInt   (3, idClienteSeleccionado);
                    if (idEmpl == -1) ps.setNull(4, Types.INTEGER);
                    else              ps.setInt (4, idEmpl);
                    ps.setInt   (5, idItbs);
                    ps.setString(6, "Cuenta Abierta");
                    ps.executeUpdate();

                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        idPedido = keys.next() ? keys.getInt(1) : 0;
                    }
                }
            }

            final String sqlDet =
                    "INSERT INTO tbl_producto_pedido " +
                    "  (id_producto, id_pedido, cantidad_producto, id_presentacion, especificaciones, precio_unitario)" +
                    "VALUES (?, ?, ?, " +
                    "  ISNULL((SELECT TOP 1 id_presentacion FROM tbl_presentacion_producto " +
                    "          WHERE id_producto = ?), " +
                    "         (SELECT TOP 1 id_presentacion FROM tbl_presentacion ORDER BY id_presentacion)), ?, ?)";

            try (PreparedStatement ps = con.prepareStatement(sqlDet)) {
                for (ItemCarrito item : carrito) {
                    String notaItem = (item.extra != null)
                            ? "Extra: " + item.extra.nombre +
                              " (+$" + item.extra.precio.toPlainString() + ")"
                            : "";
                    ps.setInt       (1, item.idProducto);
                    ps.setInt       (2, idPedido);
                    ps.setInt       (3, item.cantidad);
                    ps.setInt       (4, item.idProducto);
                    ps.setString    (5, notaItem);
                    ps.setBigDecimal(6, item.precioUnitario);
                    ps.addBatch();
                }
                ps.executeBatch();
            }

            if (promoDescuento.compareTo(BigDecimal.ZERO) > 0 && !carrito.isEmpty()) {
                int idProdPromo = (promoIdProducto != -1) ? promoIdProducto : carrito.get(0).idProducto;
                try (PreparedStatement psPromo = con.prepareStatement(
                        "INSERT INTO tbl_producto_pedido " +
                        "  (id_producto, id_pedido, cantidad_producto, id_presentacion, especificaciones, precio_unitario)" +
                        "VALUES (?, ?, 1, " +
                        "  ISNULL((SELECT TOP 1 id_presentacion FROM tbl_presentacion_producto WHERE id_producto = ?), " +
                        "         (SELECT TOP 1 id_presentacion FROM tbl_presentacion ORDER BY id_presentacion)), ?, ?)")) {
                    psPromo.setInt       (1, idProdPromo);
                    psPromo.setInt       (2, idPedido);
                    psPromo.setInt       (3, idProdPromo);
                    psPromo.setString    (4, "Descuento: " + promoNombre);
                    psPromo.setBigDecimal(5, promoDescuento.negate());
                    psPromo.executeUpdate();
                }
            }

            if ("Delivery".equals(tipoEntrega)) {
                String dir = (txtDireccion   != null) ? txtDireccion.getText().trim()   : "";
                String obs = (txtObservacion != null) ? txtObservacion.getText().trim()  : "";
                if (!dir.isEmpty()) {
                    // Primero borrar si ya existe un envío anterior (caso cuenta abierta modificada)
                    try (PreparedStatement psDelEnv = con.prepareStatement("DELETE FROM tbl_envio WHERE id_pedido = ?")) {
                        psDelEnv.setInt(1, idPedido);
                        psDelEnv.executeUpdate();
                    }
                    try (PreparedStatement psEnv = con.prepareStatement(
                            "INSERT INTO tbl_envio (id_pedido, direccion, costo_servicio, id_metodo_envio, observacion) " +
                            "VALUES (?, ?, ?, 1, ?)")) {
                        psEnv.setInt      (1, idPedido);
                        psEnv.setString   (2, dir);
                        psEnv.setBigDecimal(3, java.math.BigDecimal.ZERO);
                        psEnv.setString   (4, obs.isEmpty() ? null : obs);
                        psEnv.executeUpdate();
                    }
                }
            }

            con.commit();
            ultimoIdPedido = idPedido;
            if (lblOrderId != null) lblOrderId.setText("Order ID: #" + idPedido);
            if (idPedidoCargado != -1) {
                mostrarMensaje("✅ Cuenta Abierta #" + idPedido + " actualizada. El cliente paga después.");
            } else {
                mostrarMensaje("✅ Cuenta Abierta #" + idPedido + " registrada. El cliente paga después.");
            }
            FnLimpiar();

        } catch (Exception e) {
            try { if (con != null) con.rollback(); } catch (Exception ignored) {}
            mostrarMensaje("Error al guardar cuenta abierta: " + e.getMessage());
            System.err.println("[HacerPedido] FnCuentaAbierta: " + e.getMessage());
        } finally {
            cerrarConexion(con);
        }
    }

    @FXML
    private void FnVerCuentasAbiertas() {
        List<String> opciones = new ArrayList<>();
        Map<String, Integer> mapaIds = new HashMap<>();

        String sql = "SELECT p.id_pedido, per.nombre AS cliente, p.precio_total " +
                     "FROM tbl_pedido p " +
                     "INNER JOIN tbl_cliente c ON p.id_cliente = c.id_cliente " +
                     "INNER JOIN tbl_persona per ON c.id_persona = per.id_persona " +
                     "WHERE p.metodo_pago = 'Cuenta Abierta' AND p.estado = 'abierta' " +
                     "ORDER BY p.id_pedido DESC";

        Connection con = null;
        try {
            con = Conexion.establecerConexion();
            if (con == null) {
                mostrarMensaje("No se pudo conectar a la base de datos.");
                return;
            }
            try (PreparedStatement ps = con.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int idPed = rs.getInt("id_pedido");
                    String cliente = rs.getString("cliente");
                    double total = rs.getDouble("precio_total");
                    String desc = String.format("#%d - %s (RD$ %,.2f)", idPed, cliente, total);
                    opciones.add(desc);
                    mapaIds.put(desc, idPed);
                }
            }
        } catch (Exception e) {
            mostrarMensaje("Error al buscar cuentas abiertas: " + e.getMessage());
            e.printStackTrace();
            return;
        } finally {
            cerrarConexion(con);
        }

        if (opciones.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Cuentas Abiertas");
            alert.setHeaderText(null);
            alert.setContentText("No hay cuentas abiertas activas en este momento.");
            alert.showAndWait();
            return;
        }

        ChoiceDialog<String> dialog = new ChoiceDialog<>(opciones.get(0), opciones);
        dialog.setTitle("Ver Cuentas Abiertas");
        dialog.setHeaderText("Seleccione la Cuenta Abierta a cargar:");
        dialog.setContentText("Cuentas:");

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()) {
            String seleccion = result.get();
            int idPedido = mapaIds.get(seleccion);
            cargarPedidoEnCarrito(idPedido);
        }
    }

    private void cargarPedidoEnCarrito(int idPedido) {
        Connection con = null;
        try {
            con = Conexion.establecerConexion();
            if (con == null) {
                mostrarMensaje("No se pudo conectar a la base de datos.");
                return;
            }

            // 1. Limpiar carrito y promociones actuales
            carrito.clear();
            promoDescuento = BigDecimal.ZERO;
            promoNombre = "";
            promoIdProducto = -1;
            if (txtCodigoPromo != null) txtCodigoPromo.clear();
            if (lblPromoStatus != null) lblPromoStatus.setText("");

            // 2. Cargar datos del cliente del pedido
            String sqlCli = "SELECT c.id_cliente, per.nombre, per.cedula, p.tipo_entrega " +
                            "FROM tbl_pedido p " +
                            "INNER JOIN tbl_cliente c ON p.id_cliente = c.id_cliente " +
                            "INNER JOIN tbl_persona per ON c.id_persona = per.id_persona " +
                            "WHERE p.id_pedido = ?";
            try (PreparedStatement psCli = con.prepareStatement(sqlCli)) {
                psCli.setInt(1, idPedido);
                try (ResultSet rsCli = psCli.executeQuery()) {
                    if (rsCli.next()) {
                        idClienteSeleccionado = rsCli.getInt("id_cliente");
                        String nombreCli = rsCli.getString("nombre");
                        String cedulaCli = rsCli.getString("cedula");
                        String entrega = rsCli.getString("tipo_entrega");

                        if (lblCliente != null) lblCliente.setText(nombreCli);
                        if (txtCedula != null) {
                            txtCedula.setText(cedulaCli != null ? cedulaCli : "");
                            txtCedula.setDisable(true);
                        }

                        // Cargar el tipo de entrega
                        if (entrega != null) {
                            tipoEntrega = entrega;
                            actualizarVisualEntrega(entrega);
                        }
                    }
                }
            }

            // 3. Cargar productos asociados
            String sqlProd = "SELECT pp.id_producto, pp.cantidad_producto, pp.precio_unitario, pp.especificaciones, " +
                             "       p.nombre, p.tipo " +
                             "FROM tbl_producto_pedido pp " +
                             "INNER JOIN tbl_producto p ON pp.id_producto = p.id_producto " +
                             "WHERE pp.id_pedido = ?";
            try (PreparedStatement psProd = con.prepareStatement(sqlProd)) {
                psProd.setInt(1, idPedido);
                try (ResultSet rsProd = psProd.executeQuery()) {
                    while (rsProd.next()) {
                        int idProd = rsProd.getInt("id_producto");
                        int cant = rsProd.getInt("cantidad_producto");
                        BigDecimal precioUnit = rsProd.getBigDecimal("precio_unitario");
                        String espec = rsProd.getString("especificaciones");
                        String nombreProd = rsProd.getString("nombre");
                        String tipoProd = rsProd.getString("tipo");

                        if (espec != null && espec.startsWith("Descuento: ")) {
                            // Cargar descuento
                            promoDescuento = precioUnit.negate();
                            promoNombre = espec.substring("Descuento: ".length());
                            promoIdProducto = idProd;
                        } else {
                            ItemCarrito item = new ItemCarrito(idProd, nombreProd, tipoProd, precioUnit, cant);

                            // Verificar si tiene extra especificado
                            if (espec != null && espec.startsWith("Extra: ")) {
                                String extraNombre = espec;
                                if (espec.contains(" (+$")) {
                                    extraNombre = espec.substring("Extra: ".length(), espec.indexOf(" (+$")).trim();
                                } else {
                                    extraNombre = espec.substring("Extra: ".length()).trim();
                                }

                                // Buscar el extra correspondiente en listaExtras
                                for (Extra ex : listaExtras) {
                                    if (ex.nombre.equalsIgnoreCase(extraNombre)) {
                                        item.extra = ex;
                                        break;
                                    }
                                }
                            }
                            carrito.add(item);
                        }
                    }
                }
            }

            idPedidoCargado = idPedido;
            if (lblOrderId != null) {
                lblOrderId.setText("Cuenta Cargada: #" + idPedido);
            }

            refrescarPanel();
            recalcularTotales();
            mostrarMensaje("✅ Cuenta Abierta #" + idPedido + " cargada en el carrito.");

        } catch (Exception e) {
            mostrarMensaje("Error al cargar detalles de la cuenta: " + e.getMessage());
            e.printStackTrace();
        } finally {
            cerrarConexion(con);
        }
    }

    private void actualizarVisualEntrega(String entrega) {
        if ("En Local".equalsIgnoreCase(entrega)) {
            deselectToggle(tglEnLocal, tglRecoger, tglDelivery);
            actualizarPillEnt(tglEnLocal);
            setVisibleDelivery(false);
        } else if ("Recoger".equalsIgnoreCase(entrega)) {
            deselectToggle(tglEnLocal, tglRecoger, tglDelivery);
            actualizarPillEnt(tglRecoger);
            setVisibleDelivery(false);
        } else if ("Delivery".equalsIgnoreCase(entrega)) {
            deselectToggle(tglEnLocal, tglRecoger, tglDelivery);
            actualizarPillEnt(tglDelivery);
            setVisibleDelivery(true);
        }
    }

    private BigDecimal calcularTotal() {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (ItemCarrito item : carrito) {
            subtotal = subtotal.add(
                    item.precioUnitario.multiply(BigDecimal.valueOf(item.cantidad)));
            if (item.extra != null) subtotal = subtotal.add(item.extra.precio);
        }
        subtotal = subtotal.subtract(promoDescuento);
        if (subtotal.compareTo(BigDecimal.ZERO) < 0) subtotal = BigDecimal.ZERO;
        return subtotal.multiply(BigDecimal.ONE.add(TASA_ITBIS))
                       .setScale(2, RoundingMode.HALF_UP);
    }

    //  Limpiar formulario

    @FXML
    private void FnLimpiar() {
        carrito.clear();
        refrescarPanel();
        idPedidoCargado = -1;
        if (lblOrderId != null) lblOrderId.setText("Order ID: #—");

        if (!CONTROLLER_Seccion.getInstancia().esCliente()) {
            idClienteSeleccionado = -1;
            if (lblCliente != null) lblCliente.setText("");
            if (txtCedula  != null) { txtCedula.clear(); txtCedula.setDisable(false); }
        }

        if (lblMensaje     != null) lblMensaje.setText("");
        if (txtDireccion   != null) txtDireccion.clear();
        if (txtObservacion != null) txtObservacion.clear();

        // Resetear promo
        promoDescuento  = BigDecimal.ZERO;
        promoNombre     = "";
        promoIdProducto = -1;
        if (txtCodigoPromo != null) txtCodigoPromo.clear();
        if (lblPromoStatus != null) lblPromoStatus.setText("");

        // Resetear paneles de pago
        pagoTarjetaConfirmado = false;
        mostrarPanelPago("ninguno");
        metodoPago = "Efectivo";
        deselectToggle(tglEfectivo, tglTarjeta, tglEWallet);
        actualizarPillPago(tglEfectivo);
        if (txtCantidadRecibida != null) txtCantidadRecibida.clear();
        if (lblCambio           != null) lblCambio.setText("$0.00");
        if (txtNombreTarjeta    != null) txtNombreTarjeta.clear();
        if (txtNumeroTarjeta    != null) txtNumeroTarjeta.clear();
        if (txtVencimiento      != null) txtVencimiento.clear();
        if (txtCVV              != null) txtCVV.clear();
        if (lblEstadoTarjeta    != null) lblEstadoTarjeta.setText("");
        pagoEWalletConfirmado = false;
        if (txtReferenciaEWallet != null) txtReferenciaEWallet.clear();
        if (lblEstadoEWallet     != null) lblEstadoEWallet.setText("");
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
            "-fx-background-color: #be1e1e; -fx-text-fill: white; -fx-font-weight: bold;" +
            "-fx-background-radius: 20; -fx-padding: 7 4; -fx-cursor: hand; -fx-font-size: 11px;" +
            "-fx-border-color: transparent;" +
            "-fx-effect: dropshadow(gaussian,rgba(190,30,30,0.40),8,0,0,2);";
    private static final String TGL_INACTIVO =
            "-fx-background-color: rgba(255,255,255,0.09); -fx-text-fill: rgba(255,255,255,0.60);" +
            "-fx-background-radius: 20; -fx-padding: 7 4; -fx-cursor: hand; -fx-font-size: 11px;" +
            "-fx-border-color: transparent;";

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

    //  Factura Jasper

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

            // ── Obtener datos del cliente y del pedido directamente desde tbl_pedido
            // Se usa ultimoIdPedido en lugar de idClienteSeleccionado porque
            // FnGuardar() llama FnLimpiar() que resetea idClienteSeleccionado a -1
            String nombre = "", cedula = "", tel = "", entrega = "", pago = "", direccion = "";
            double precioTotal = 0.0;
            try (PreparedStatement ps = con.prepareStatement(
                    "SELECT per.nombre, per.cedula, per.tel, per.direccion, " +
                    "       ped.tipo_entrega, ped.metodo_pago, ped.precio_total " +
                    "FROM   tbl_pedido  ped " +
                    "JOIN   tbl_cliente cli ON cli.id_cliente = ped.id_cliente " +
                    "JOIN   tbl_persona per ON per.id_persona = cli.id_persona " +
                    "WHERE  ped.id_pedido = ?")) {
                ps.setInt(1, ultimoIdPedido);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        nombre      = nvl(rs.getString("nombre"));
                        cedula      = nvl(rs.getString("cedula"));
                        tel         = nvl(rs.getString("tel"));
                        direccion   = nvl(rs.getString("direccion"));
                        entrega     = nvl(rs.getString("tipo_entrega"));
                        pago        = nvl(rs.getString("metodo_pago"));
                        precioTotal = rs.getDouble("precio_total");
                    }
                }
            }

            Map<String, Object> params = new HashMap<>();
            params.put("ID_PEDIDO",   ultimoIdPedido);
            params.put("LOGO_PATH",   "");
            params.put("P_CLIENTE",   nombre);
            params.put("P_CEDULA",    cedula);
            params.put("P_TEL",       tel);
            params.put("P_FECHA",     LocalDate.now().toString());
            params.put("P_EMPLEADO",  CONTROLLER_Seccion.getInstancia().getNombre());
            params.put("P_ENTREGA",   entrega);
            params.put("P_PAGO",      pago);
            params.put("P_DIRECCION", direccion);
            params.put("P_TOTAL",     precioTotal);

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

    // = Helpers =

    private void mostrarMensaje(String msg) {
        if (lblMensaje != null) lblMensaje.setText(msg != null ? msg : "");
    }

    private String nvl(String v) { return v != null ? v : ""; }

    /** Precio de respaldo cuando la BD devuelve NULL o 0. */
    private BigDecimal precioDefecto(String nombre, String tipo) {
        String n = nombre != null ? nombre.toLowerCase() : "";
        String t = tipo   != null ? tipo.toLowerCase()   : "";

        // Por nombre (más específico primero)
        if (n.contains("hawayana") || n.contains("hawaiana"))  return new BigDecimal("360.00");
        if (n.contains("provolon") || n.contains("provolo"))   return new BigDecimal("375.00");
        if (n.contains("pepperoni"))                           return new BigDecimal("370.00");
        if (n.contains("suprema")  || n.contains("especial"))  return new BigDecimal("380.00");
        if (n.contains("miel mostaza"))                        return new BigDecimal("320.00");
        if (n.contains("picante"))                             return new BigDecimal("320.00");
        if (n.contains("mozzarella") || n.contains("palito"))  return new BigDecimal("250.00");
        if (n.contains("pan de ajo"))                          return new BigDecimal("180.00");
        if (n.equals("pan"))                                   return new BigDecimal("90.00");
        if (n.contains("limonada") || n.contains("jugo"))      return new BigDecimal("120.00");
        if (n.contains("agua"))                                return new BigDecimal("45.00");
        if (n.contains("bbq"))                                 return new BigDecimal("360.00");

        // Por tipo (fallback)
        if (t.contains("pizza"))    return new BigDecimal("360.00");
        if (t.contains("pasta"))    return new BigDecimal("280.00");
        if (t.contains("alita"))    return new BigDecimal("320.00");
        if (t.contains("bebida"))   return new BigDecimal("80.00");
        if (t.contains("postre"))   return new BigDecimal("150.00");
        if (t.contains("acompa"))   return new BigDecimal("180.00");

        return new BigDecimal("180.00");   // genérico
    }

    private void cerrarConexion(Connection con) {
        if (con != null) {
            try { con.close(); } catch (Exception ignored) {}
        }
    }

    //   Clases internas

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
        Extra            extra;   // null = sin extra

        public ItemCarrito(int id, String nombre, String tipo, BigDecimal precio, int cantidad) {
            this.idProducto     = id;
            this.nombre         = nombre != null ? nombre : "";
            this.tipo           = tipo   != null ? tipo.toLowerCase() : "";
            this.precioUnitario = precio != null ? precio : BigDecimal.ZERO;
            this.cantidad       = cantidad;
            this.extra          = null;
        }
    }

    @FXML
    private void FnExportarReporteVentas() {
        if (!com.example.demo1.Utils.Permisos_Util.verificarReporte()) return;
        JasperUtil.exportarPDF(
                "/com/example/demo1/reportes/Reporte_Ventas.jrxml",
                "Reporte_Ventas.pdf"
        );
    }

    //  Imagen de producto
    private javafx.scene.image.Image buscarImagenProducto(String nombre) {
        if (nombre == null) return null;
        final String BASE = "/com/example/demo1/imagenes/productos/";
        java.util.Map<String, String> mapa = new java.util.LinkedHashMap<>();
        mapa.put("pepperoni",     "Pizza Pepperoni.jpg");
        mapa.put("margarita",     "Pizza Margarita.jpg");
        mapa.put("4 quesos",      "Pizza 4 Quesos.jpg");
        mapa.put("bbq pollo",     "Pizza BBQ Pollo.jpg");
        mapa.put("hawayana",      "Pizza grande hawayana.jpg");
        mapa.put("hawaiana",      "Pizza grande hawayana.jpg");
        mapa.put("provolon",      "pizza provolonne.jpg");
        mapa.put("salami",        "Delicious Salami Pizza🍕.jpg");
        mapa.put("alitas bbq",    "Alitas BBQ.jpg");
        mapa.put("miel mostaza",  "Alitas Miel Mostaza.jpg");
        mapa.put("picantes",      "Alitas Picantes.jpg");
        mapa.put("alitas",        "Alitas BBQ.jpg");
        mapa.put("mozzarella",    "Palitos de Mozzarella.jpg");
        mapa.put("palitos",       "Palitos de Mozzarella.jpg");
        mapa.put("pan de ajo",    "Pan de Ajo.jpg");
        mapa.put("coca-cola 355", "Coca-Cola 355ml.jpg");
        mapa.put("coca-cola 600", "Coca-Cola 600ml.jpg");
        mapa.put("coca",          "Coca-Cola 600ml.jpg");
        mapa.put("sprite",        "sprite 350 ml.jpg");
        mapa.put("agua",          "Agua Mineral.png");
        mapa.put("pan",           "pan.jpg");

        String lower = nombre.trim().toLowerCase();
        for (java.util.Map.Entry<String, String> entry : mapa.entrySet()) {
            if (lower.contains(entry.getKey())) {
                java.net.URL url = getClass().getResource(BASE + entry.getValue());
                if (url != null) {
                    try {
                        return new javafx.scene.image.Image(url.toExternalForm());
                    } catch (Exception ignored) {}
                }
            }
        }
        // Fallback: intentar con el nombre exacto del producto
        for (String ext : new String[]{".jpg", ".png", ".jpeg"}) {
            java.net.URL url = getClass().getResource(BASE + nombre + ext);
            if (url != null) {
                try {
                    return new javafx.scene.image.Image(url.toExternalForm());
                } catch (Exception ignored) {}
            }
        }
        return null;
    }
}
