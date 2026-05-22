package com.example.demo1.Controllers;

import com.example.demo1.Utils.CONTROLLER_Seccion;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TitledPane;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;
import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

/**
 * Controlador principal del sistema Domino's Pizza.
 * Carga pantallas según el rol del usuario logueado.
 *
 * Los roles y sus permisos de menú se obtienen directamente desde
 * CONTROLLER_Seccion, que a su vez refleja el campo tbl_persona.rol_bd.
 *
 * Reglas de visibilidad:
 *   admin    → ve TODO el menú
 *   gerente  → ve todo excepto secciones de "Otro" (crear usuarios, etc.)
 *   cajero   → Inicio, Inventario, Ventas (pedido/reclamación), Cliente
 *   delivery → Inicio, Ventas (solo Envío)
 *   cliente  → Inicio, Ventas (solo Registrar Pedido y Reclamación)
 */
public class Maincontroller {

    @FXML private StackPane contentArea;
    @FXML private Label     lblFecha;
    @FXML private Label     lblUsuario;
    @FXML private Label     lblEstado;
    @FXML private Label     lblAvatar;
    @FXML private VBox      sideMenu;

    private static final String RUTA_PANTALLAS = "/com/example/demo1/Pantallas/";

    // -----------------------------------------------------------------------
    //  Inicialización
    // -----------------------------------------------------------------------
    @FXML
    public void initialize() {
        // Mostrar fecha
        String fechaHoy = LocalDate.now()
                .format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        if (lblFecha != null) lblFecha.setText(fechaHoy);

        // Mostrar nombre y rol tal como están guardados en la sesión
        CONTROLLER_Seccion sesion = CONTROLLER_Seccion.getInstancia();

        // Capitalizar el rol para la etiqueta visual (ej. "admin" → "Admin")
        String rolMostrar = capitalize(sesion.getRol());
        if (lblUsuario != null)
            lblUsuario.setText(sesion.getNombre() + " (" + rolMostrar + ")");
        if (lblEstado != null)
            lblEstado.setText("Bienvenido, " + sesion.getNombre());

        // Inicial del avatar
        if (lblAvatar != null) {
            String inicial = sesion.getNombre().isEmpty() ? "U"
                    : String.valueOf(sesion.getNombre().charAt(0)).toUpperCase();
            lblAvatar.setText(inicial);
        }

        // Aplicar permisos de menú según el rol real de la BD
        aplicarPermisosMenu();

        // Pantalla de inicio por defecto
        cargarVista("Inicio.fxml", "Inicio");
    }

    // -----------------------------------------------------------------------
    //  Lógica de permisos de menú
    // -----------------------------------------------------------------------

    /**
     * Oculta / muestra opciones del menú lateral según el rol del usuario.
     * Los valores de rol_bd en la BD son: admin, gerente, cajero, delivery, cliente.
     */
    private void aplicarPermisosMenu() {
        CONTROLLER_Seccion s = CONTROLLER_Seccion.getInstancia();

        // ADMIN → ve absolutamente todo
        if (s.esAdmin()) return;

        if (s.esGerente()) {
            ocultarTitledPanesPorTexto(Arrays.asList("≡  Otro"));
            return;
        }

        if (s.esCajero()) {
            ocultarTitledPanesPorTexto(Arrays.asList(
                    "🛒  Compras", "🔧  Equipos y Mant.", "≡  Otro", "☰  Reportes"));
            ocultarBotonesPorTexto(Arrays.asList("✿  Ingredientes", "🍕  Agregar Producto", "🚚  Envío", "⭐  Ofertas"));
            return;
        }

        if (s.esDelivery()) {
            ocultarTitledPanesPorTexto(Arrays.asList(
                    "🛒  Compras", "🔧  Equipos y Mant.", "≡  Otro", "☰  Reportes"));
            ocultarBotonesPorTexto(Arrays.asList(
                    "☰  Inventario", "✿  Ingredientes", "🍕  Agregar Producto",
                    "✎  Hacer un Pedido", "⚠  Reclamación", "⭐  Ofertas",
                    "●  Registro de Cliente"));

            return;
        }

        if (s.esCliente()) {
            ocultarTitledPanesPorTexto(Arrays.asList(
                    "🛒  Compras", "🔧  Equipos y Mant.", "≡  Otro", "☰  Reportes"));
            ocultarBotonesPorTexto(Arrays.asList(
                    "☰  Inventario", "✿  Ingredientes", "🍕  Agregar Producto",
                    "🚚  Envío", "⭐  Ofertas"));
            return;
        }

        // Rol desconocido → ocultar todo excepto Inicio
        ocultarTitledPanesPorTexto(Arrays.asList(
                "🍕  Ventas", "🛒  Compras", "🔧  Equipos y Mant.", "≡  Otro", "☰  Reportes"));
    }

    // -----------------------------------------------------------------------
    //  Helpers para ocultar nodos del menú
    // -----------------------------------------------------------------------

    private void ocultarTitledPanesPorTexto(List<String> textos) {
        if (sideMenu == null) return;
        sideMenu.getChildren().removeIf(node -> {
            if (node instanceof TitledPane) {
                return textos.contains(((TitledPane) node).getText());
            }
            return false;
        });
    }

    private void ocultarBotonesPorTexto(List<String> textos) {
        if (sideMenu == null) return;
        sideMenu.getChildren().forEach(node -> {
            if (node instanceof TitledPane) {
                Object content = ((TitledPane) node).getContent();
                if (content instanceof VBox) {
                    ((VBox) content).getChildren().removeIf(child -> {
                        if (child instanceof Button) {
                            return textos.contains(((Button) child).getText());
                        }
                        return false;
                    });
                }
            } else if (node instanceof Button) {
                if (textos.contains(((Button) node).getText())) {
                    node.setVisible(false);
                    node.setManaged(false);
                }
            }
        });
    }

    // -----------------------------------------------------------------------
    //  Handlers de menú — INICIO
    // -----------------------------------------------------------------------
    @FXML private void abrirInicio() {
        cargarVista("Inicio.fxml", "Inicio");
    }

    // -----------------------------------------------------------------------
    //  INVENTARIO
    // -----------------------------------------------------------------------
    @FXML private void abrirInventario() {
        cargarVista("inventario.fxml", "Inventario");
    }

    @FXML private void abrirAgregarIngrediente() {
        cargarVista("Agregar_Ingrediente.fxml", "Ingredientes");
    }

    // -----------------------------------------------------------------------
    //  COMPRAS
    // -----------------------------------------------------------------------
    @FXML private void abrirAgregarProveedor() {
        cargarVista("Agregar_Proveedor.fxml", "Proveedores");
    }

    @FXML private void abrirComprasPago() {
        cargarVista("Compras_Pago.fxml", "Compras y pago");
    }

    @FXML private void abrirDevolucion() {
        cargarVista("Devolucion.fxml", "Devolución");
    }

    // -----------------------------------------------------------------------
    //  VENTAS
    // -----------------------------------------------------------------------
    @FXML private void abrirAgregarProducto() {
        cargarVista("Agregar_Producto.fxml", "Producto");
    }

    @FXML private void abrirHacerPedido() {
        cargarVista("Hacer_Un_Pedido.fxml", "Registrar Pedido");
    }

    @FXML private void abrirCuentasAbiertas() {
        cargarVista("Cuentas_Abiertas.fxml", "Cuentas Abiertas");
    }

    @FXML private void abrirEnvio() {
        cargarVista("Envio.fxml", "Envío");
    }

    @FXML private void abrirReclamacion() {
        cargarVista("Reclamacion.fxml", "Reclamación");
    }

    @FXML private void abrirOfertas() {
        cargarVista("Ofertas.fxml", "Ofertas");
    }

    // -----------------------------------------------------------------------
    //  EQUIPOS Y MANTENIMIENTO
    // -----------------------------------------------------------------------
    @FXML private void abrirAgregarMaquina() {
        cargarVista("Agregar_Maquina.fxml", "Máquina");
    }

    @FXML private void abrirMantenimiento() {
        cargarVista("Mantenimiento.fxml", "Mantenimiento");
    }

    @FXML private void abrirAgregarTecnico() {
        cargarVista("Agregar_Tecnico.fxml", "Técnico");
    }

    @FXML private void abrirAperturaCaja() {
        cargarVista("Apertura_Caja.fxml", "Apertura de Caja");
    }

    @FXML private void abrirFallosMaquina() {
        cargarVista("Fallos_Maquina.fxml", "Fallos de Máquina");
    }

    // -----------------------------------------------------------------------
    //  VENTAS (métodos adicionales)
    // -----------------------------------------------------------------------
    @FXML private void abrirRegistroCliente() {
        cargarVista("Agregar_Cliente.fxml", "Registro de Cliente");
    }

    // -----------------------------------------------------------------------
    //  OTRO
    // -----------------------------------------------------------------------
    @FXML private void abrirAgregarCliente() {
        cargarVista("Agregar_Cliente.fxml", "Cliente");
    }

    @FXML private void abrirAgregarEmpleado() {
        cargarVista("Agregar_Empleado.fxml", "Empleados");
    }

    @FXML private void abrirGestionUsuarios() {
        cargarVista("Agregar_Usuario.fxml", "Gestión de Usuarios");
    }

    @FXML private void abrirAgregarSucursal() {
        cargarVista("Agregar_Sucursal.fxml", "Sucursal");
    }

    @FXML private void abrirDepartamentosCargo() {
        cargarVista("Agregar_Departamento.fxml", "Departamentos y Cargo");
    }

    @FXML private void abrirAgregarCargo() {
        cargarVista("Agregar_Cargo.fxml", "Cargo");
    }

    @FXML private void abrirAgregarDepartamento() {
        cargarVista("Agregar_Departamento.fxml", "Departamento");
    }

    // -----------------------------------------------------------------------
    //  REPORTES
    // -----------------------------------------------------------------------
    @FXML private void abrirReporte1() { cargarVista("Reporte1.fxml", "Reporte 1"); }
    @FXML private void abrirReporte2() { cargarVista("Reporte2.fxml", "Reporte 2"); }
    @FXML private void abrirReporte3() { cargarVista("Reporte3.fxml", "Reporte 3"); }
    @FXML private void abrirReporte4() { cargarVista("Reporte4.fxml", "Reporte 4"); }
    @FXML private void abrirReporte5() { cargarVista("Reporte5.fxml", "Reporte 5"); }

    // -----------------------------------------------------------------------
    //  PERFIL DE USUARIO (popup al clic en avatar)
    // -----------------------------------------------------------------------
    @FXML
    private void mostrarPerfil(MouseEvent event) {
        CONTROLLER_Seccion s = CONTROLLER_Seccion.getInstancia();

        // --- Avatar ---
        Label avatar = new Label(lblAvatar.getText());
        avatar.setStyle(
                "-fx-font-size: 30px; -fx-font-weight: bold; -fx-text-fill: #004aad;" +
                "-fx-background-color: #e8f0fe; -fx-background-radius: 40;" +
                "-fx-min-width: 64; -fx-min-height: 64;" +
                "-fx-max-width: 64; -fx-max-height: 64;" +
                "-fx-alignment: center;");

        // --- Nombre ---
        Label nombre = new Label(s.getNombre().isEmpty() ? "Usuario" : s.getNombre());
        nombre.setStyle("-fx-font-size: 15px; -fx-font-weight: bold;" +
                        "-fx-text-fill: #1a1a2e; -fx-font-family: 'Segoe UI';");

        // --- Badge de rol ---
        Label rolLabel = new Label(capitalize(s.getRol()));
        rolLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: bold;" +
                          "-fx-text-fill: white; -fx-background-color: #004aad;" +
                          "-fx-background-radius: 20; -fx-padding: 3 14;");

        // --- Email ---
        String emailTxt = s.getEmail().isEmpty() ? "Sin correo registrado" : s.getEmail();
        Label emailLabel = new Label("✉  " + emailTxt);
        emailLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #555;" +
                            "-fx-font-family: 'Segoe UI';");

        // --- IDs de referencia ---
        String idInfo = "ID Persona: " + s.getIdPersona();
        if (s.getIdEmpleado() > 0) idInfo += "   |   ID Empleado: " + s.getIdEmpleado();
        if (s.getIdCliente()  > 0) idInfo += "   |   ID Cliente: "  + s.getIdCliente();
        Label idLabel = new Label(idInfo);
        idLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #aaa; -fx-font-family: 'Segoe UI';");

        Separator sep = new Separator();
        sep.setStyle("-fx-opacity: 0.25;");
        VBox.setMargin(sep, new Insets(4, 0, 4, 0));

        // --- Botón cerrar sesión ---
        Button btnCerrar = new Button("←  Cerrar Sesión");
        btnCerrar.setMaxWidth(Double.MAX_VALUE);
        btnCerrar.setStyle("-fx-background-color: #004aad; -fx-text-fill: white;" +
                           "-fx-font-weight: bold; -fx-font-size: 12px;" +
                           "-fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 8 12;");

        // --- Tarjeta ---
        VBox card = new VBox(10, avatar, nombre, rolLabel, emailLabel, idLabel, sep, btnCerrar);
        card.setAlignment(Pos.CENTER);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 14;" +
                      "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.22), 22, 0, 0, 6);" +
                      "-fx-padding: 22 26 18 26; -fx-min-width: 270;");

        Popup popup = new Popup();
        popup.setAutoHide(true);
        popup.getContent().add(card);

        btnCerrar.setOnAction(e -> { popup.hide(); cerrarSesion(); });

        Bounds b = lblAvatar.localToScreen(lblAvatar.getBoundsInLocal());
        popup.show(lblAvatar.getScene().getWindow(),
                   b.getMinX() - 220,
                   b.getMaxY() + 8);
    }

    // -----------------------------------------------------------------------
    //  SISTEMA
    // -----------------------------------------------------------------------
    @FXML
    private void salir() {
        Alert alert = new Alert(AlertType.CONFIRMATION);
        alert.setTitle("Salir");
        alert.setHeaderText("¿Deseas cerrar la aplicación?");
        alert.setContentText("Se cerrará el programa por completo.");
        alert.showAndWait().ifPresent(r -> {
            if (r == ButtonType.OK) {
                CONTROLLER_Seccion.getInstancia().cerrar();
                System.exit(0);
            }
        });
    }

    @FXML
    private void cerrarSesion() {
        Alert alert = new Alert(AlertType.CONFIRMATION);
        alert.setTitle("Cerrar Sesión");
        alert.setHeaderText("¿Deseas cerrar sesión?");
        alert.setContentText("Volverás a la pantalla de inicio de sesión.");
        alert.showAndWait().ifPresent(r -> {
            if (r == ButtonType.OK) {
                try {
                    CONTROLLER_Seccion.getInstancia().cerrar();
                    FXMLLoader loader = new FXMLLoader(
                            getClass().getResource(RUTA_PANTALLAS + "Login.fxml"));
                    javafx.scene.Parent root = loader.load();
                    javafx.stage.Stage stage =
                            (javafx.stage.Stage) contentArea.getScene().getWindow();
                    stage.setScene(new javafx.scene.Scene(root));
                    stage.setTitle("Domino's Pizza - Iniciar Sesión");
                    stage.setMaximized(false);
                    stage.setResizable(false);
                    stage.show();
                } catch (IOException e) {
                    mostrarError("No se pudo volver al login.", e.getMessage());
                }
            }
        });
    }

    // -----------------------------------------------------------------------
    //  Método genérico de carga de vistas
    // -----------------------------------------------------------------------
    private void cargarVista(String fxmlFile, String titulo) {
        try {
            URL url = getClass().getResource(RUTA_PANTALLAS + fxmlFile);
            if (url == null) {
                mostrarPlaceholder(titulo, "Pantalla pendiente de implementar: " + fxmlFile);
                return;
            }
            FXMLLoader loader = new FXMLLoader(url);
            Node vista = loader.load();
            aplicarFondo(vista);
            contentArea.getChildren().setAll(vista);
            if (lblEstado != null) lblEstado.setText("Pantalla actual: " + titulo);
        } catch (IOException e) {
            mostrarError("Error al cargar la vista: " + titulo, e.getMessage());
        }
    }

    private void mostrarPlaceholder(String titulo, String mensaje) {
        Label lbl1 = new Label("   " + titulo);
        lbl1.setStyle("-fx-font-size: 26px; -fx-font-weight: bold; -fx-text-fill: #004aad;");
        Label lbl2 = new Label(mensaje);
        lbl2.setStyle("-fx-font-size: 14px; -fx-text-fill: #666;");
        VBox box = new VBox(15, lbl1, lbl2);
        box.setStyle("-fx-alignment: center; -fx-padding: 40;");
        contentArea.getChildren().setAll(box);
    }

    private void mostrarError(String titulo, String detalle) {
        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(titulo);
        alert.setContentText(detalle);
        alert.showAndWait();
    }

    // -----------------------------------------------------------------------
    //  Utilidades
    // -----------------------------------------------------------------------
    private void aplicarFondo(Node node) {
        // BorderPane ya tiene su propio fondo definido en el FXML — no aplicar imagen
        if (!(node instanceof Pane) || node instanceof BorderPane) return;
        Pane pane = (Pane) node;

        URL imgUrl = getClass().getResource("/com/example/demo1/imagenes/fondop.png");
        if (imgUrl == null) return;

        ImageView iv = new ImageView(new Image(imgUrl.toExternalForm()));
        iv.setPreserveRatio(false);
        iv.fitWidthProperty().bind(pane.widthProperty());
        iv.fitHeightProperty().bind(pane.heightProperty());
        pane.getChildren().add(0, iv);
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return "";
        return Character.toUpperCase(str.charAt(0)) + str.substring(1);
    }
}
