package com.example.demo1.Controllers;

import com.example.demo1.Database.Conexion;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javax.swing.JOptionPane;
import java.io.File;
import java.sql.*;

/**
 * tbl_producto : id_producto (IDENTITY), nombre, tipo, disponibilidad (bit), id_proveedor (nullable)
 * tbl_presentacion_producto : id_producto, id_presentacion, costo
 * tbl_presentacion : id_presentacion (IDENTITY), presentacion, pedazo
 */
public class CONTROLLER_Producto {

    Conexion conexion = new Conexion();

    @FXML private TextField  Txtnombre;
    @FXML private TextField  Txtprecio;
    @FXML private TextField  txtporcion;
    @FXML private ComboBox<String> cmbtipo;
    @FXML private Label     lblRutaImagen;
    @FXML private ImageView imgProducto;
    private String rutaImagenActual = null;

    @FXML private RadioButton rbSi;
    @FXML private RadioButton rbNo;
    private ToggleGroup grupoDisponible;

    @FXML private RadioButton rbPequeno;
    @FXML private RadioButton rbMediana;
    @FXML private RadioButton rbGrande;
    @FXML private RadioButton rbFamiliar;
    private ToggleGroup grupoPresentacion;

    @FXML private CheckBox chkMasa;
    @FXML private CheckBox chkSalsa;
    @FXML private CheckBox chkQueso;
    @FXML private CheckBox chkPepperoni;
    @FXML private CheckBox chkMaiz;
    @FXML private CheckBox chkSalchicha;
    @FXML private CheckBox chkVegetales;
    @FXML private CheckBox chkPina;
    @FXML private CheckBox chkBebida;
    @FXML private CheckBox chkOtro;

    @FXML private TableView<ProductoRow>           tablaProductos;
    @FXML private TableColumn<ProductoRow, String> colNombre;
    @FXML private TableColumn<ProductoRow, String> colTipo;
    @FXML private TableColumn<ProductoRow, String> colPrecio;

    private int idProductoActual = -1;

    @FXML
    public void initialize() {
        grupoDisponible = new ToggleGroup();
        rbSi.setToggleGroup(grupoDisponible);
        rbNo.setToggleGroup(grupoDisponible);

        grupoPresentacion = new ToggleGroup();
        rbPequeno.setToggleGroup(grupoPresentacion);
        rbMediana.setToggleGroup(grupoPresentacion);
        rbGrande.setToggleGroup(grupoPresentacion);
        rbFamiliar.setToggleGroup(grupoPresentacion);

        cmbtipo.setItems(FXCollections.observableArrayList("pizza", "alitas", "bebida", "otro"));

        colNombre.setCellValueFactory(c -> c.getValue().nombre);
        colTipo.setCellValueFactory(c -> c.getValue().tipo);
        colPrecio.setCellValueFactory(c -> c.getValue().precio);

        tablaProductos.getSelectionModel().selectedItemProperty().addListener(
                (obs, old, sel) -> { if (sel != null) rellenarFormulario(sel); });

        cargarTabla();
    }

    // ================================================================
    //  INSERT — guarda en tbl_producto y, si hay precio+presentación,
    //           también en tbl_presentacion_producto
    // ================================================================
    @FXML
    public void FnGuardar() {
        if (com.example.demo1.Utils.CONTROLLER_Seccion.getInstancia().esCajero()) {
            javax.swing.JOptionPane.showMessageDialog(null,
                    "Los cajeros no pueden agregar productos nuevos.",
                    "Acceso denegado", javax.swing.JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (!com.example.demo1.Utils.Permisos_Util.verificarInsertar()) return;
        String nombre   = Txtnombre.getText().trim();
        String tipo     = cmbtipo.getValue();
        boolean disp    = rbSi.isSelected();

        if (nombre.isEmpty() || tipo == null) {
            JOptionPane.showMessageDialog(null, "Nombre y tipo son obligatorios.");
            return;
        }

        Connection con = null;
        try {
            con = conexion.establecerConexion();
            con.setAutoCommit(false);

            // 1) tbl_producto
            String sqlProd = "INSERT INTO tbl_producto (nombre, tipo, disponibilidad) VALUES (?, ?, ?)";
            int idProducto;
            try (PreparedStatement ps = con.prepareStatement(sqlProd, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, nombre);
                ps.setString(2, tipo);
                ps.setBoolean(3, disp);
                ps.executeUpdate();
                ResultSet k = ps.getGeneratedKeys();
                idProducto = k.next() ? k.getInt(1) : -1;
            }

            // 2) tbl_presentacion_producto — sólo si el usuario llenó precio y eligió presentación
            String precioStr    = Txtprecio != null ? Txtprecio.getText().trim() : "";
            RadioButton selPres = grupoPresentacion.getSelectedToggle() != null
                    ? (RadioButton) grupoPresentacion.getSelectedToggle() : null;

            if (idProducto != -1 && !precioStr.isEmpty() && selPres != null) {
                String nombrePres = selPres.getText(); // Pequeño / Mediana / Grande / Familiar

                // Buscar id_presentacion por nombre
                int idPresentacion = -1;
                try (PreparedStatement psP = con.prepareStatement(
                        "SELECT id_presentacion FROM tbl_presentacion WHERE presentacion = ?")) {
                    psP.setString(1, nombrePres);
                    ResultSet rp = psP.executeQuery();
                    if (rp.next()) idPresentacion = rp.getInt(1);
                }

                if (idPresentacion != -1) {
                    try (PreparedStatement psI = con.prepareStatement(
                            "INSERT INTO tbl_presentacion_producto (id_producto, id_presentacion, costo) " +
                            "VALUES (?, ?, ?)")) {
                        psI.setInt(1, idProducto);
                        psI.setInt(2, idPresentacion);
                        psI.setDouble(3, Double.parseDouble(precioStr));
                        psI.executeUpdate();
                    }
                }
            }

            con.commit();
            JOptionPane.showMessageDialog(null, "Producto guardado correctamente.");
            limpiar();
            cargarTabla();

        } catch (NumberFormatException e) { //error de formato en precio al guardar producto - pantalla Agregar Producto
            try { if (con != null) con.rollback(); } catch (Exception ignore) {}
            JOptionPane.showMessageDialog(null, "El precio debe ser un número válido.");
        } catch (Exception e) { //error al guardar producto - pantalla Agregar Producto
            try { if (con != null) con.rollback(); } catch (Exception ignore) {}
            JOptionPane.showMessageDialog(null, "Error al guardar: " + e.getMessage());
        } finally {
            try { if (con != null) con.close(); } catch (Exception ignore) {}
        }
    }

    // ================================================================
    //  SELECT — busca por nombre
    // ================================================================
    @FXML
    public void FnBuscar() {
        String nombre = Txtnombre.getText().trim();
        if (nombre.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Ingrese un nombre para buscar.");
            return;
        }

        String sql =
            "SELECT p.id_producto, p.nombre, p.tipo, p.disponibilidad, " +
            "       MIN(pp.costo) AS precio " +
            "FROM tbl_producto p " +
            "LEFT JOIN tbl_presentacion_producto pp ON p.id_producto = pp.id_producto " +
            "WHERE p.nombre LIKE ? " +
            "GROUP BY p.id_producto, p.nombre, p.tipo, p.disponibilidad";

        try (Connection con = conexion.establecerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, "%" + nombre + "%");
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                idProductoActual = rs.getInt("id_producto");
                String nombreEncontrado = rs.getString("nombre");
                Txtnombre.setText(nombreEncontrado);
                cmbtipo.setValue(rs.getString("tipo"));
                boolean disp = rs.getBoolean("disponibilidad");
                if (disp) rbSi.setSelected(true); else rbNo.setSelected(true);
                double precio = rs.getDouble("precio");
                if (Txtprecio != null && precio > 0) Txtprecio.setText(String.valueOf(precio));
                autoCargarImagen(nombreEncontrado);
            } else {
                JOptionPane.showMessageDialog(null, "No se encontró el producto.");
                idProductoActual = -1;
            }

        } catch (Exception e) { //error al buscar producto - pantalla Agregar Producto
            JOptionPane.showMessageDialog(null, "Error: " + e.getMessage());
        }
    }

    // ================================================================
    //  UPDATE
    // ================================================================
    @FXML
    public void FnEditar() {
        if (idProductoActual == -1) {
            JOptionPane.showMessageDialog(null, "Seleccione un producto de la tabla o use 🔍.");
            return;
        }

        String nombre = Txtnombre.getText().trim();
        String tipo   = cmbtipo.getValue();
        if (nombre.isEmpty() || tipo == null) {
            JOptionPane.showMessageDialog(null, "Nombre y tipo son obligatorios.");
            return;
        }

        String sql = "UPDATE tbl_producto SET nombre=?, tipo=?, disponibilidad=? WHERE id_producto=?";
        try (Connection con = conexion.establecerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, nombre);
            ps.setString(2, tipo);
            ps.setBoolean(3, rbSi.isSelected());
            ps.setInt(4, idProductoActual);

            if (ps.executeUpdate() > 0) {
                JOptionPane.showMessageDialog(null, "Producto actualizado.");
                cargarTabla();
            } else {
                JOptionPane.showMessageDialog(null, "No se encontró el registro.");
            }

        } catch (Exception e) { //error al editar producto - pantalla Agregar Producto
            JOptionPane.showMessageDialog(null, "Error al editar: " + e.getMessage());
        }
    }

    // ================================================================
    //  DELETE
    // ================================================================
    @FXML
    public void FnEliminar() {
        if (!com.example.demo1.Utils.Permisos_Util.verificarEliminar()) return;
        if (idProductoActual == -1) {
            JOptionPane.showMessageDialog(null, "Seleccione un producto de la tabla o use 🔍.");
            return;
        }

        String nombre = Txtnombre.getText().trim();
        int c = JOptionPane.showConfirmDialog(null, "¿Eliminar '" + nombre + "'?",
                "Confirmar", JOptionPane.YES_NO_OPTION);
        if (c != JOptionPane.YES_OPTION) return;

        try (Connection con = conexion.establecerConexion();
             PreparedStatement ps = con.prepareStatement(
                     "DELETE FROM tbl_producto WHERE id_producto = ?")) {

            ps.setInt(1, idProductoActual);
            if (ps.executeUpdate() > 0) {
                JOptionPane.showMessageDialog(null, "Producto eliminado.");
                limpiar();
                cargarTabla();
            } else {
                JOptionPane.showMessageDialog(null, "No se encontró el producto.");
            }

        } catch (Exception e) { //error al eliminar producto - pantalla Agregar Producto
            JOptionPane.showMessageDialog(null, "Error: " + e.getMessage());
        }
    }

    @FXML
    public void FnInhabilitar() {
        if (idProductoActual == -1) {
            JOptionPane.showMessageDialog(null, "Seleccione un producto de la tabla o use 🔍.");
            return;
        }
        int confirmar = JOptionPane.showConfirmDialog(null,
                "¿Inhabilitar este producto? No se eliminará, solo quedará no disponible.",
                "Confirmar", JOptionPane.YES_NO_OPTION);
        if (confirmar != JOptionPane.YES_OPTION) return;
        try (java.sql.Connection con = Conexion.establecerConexion();
             java.sql.PreparedStatement ps = con.prepareStatement(
                     "UPDATE tbl_producto SET disponibilidad = 0 WHERE id_producto = ?")) {
            ps.setInt(1, idProductoActual);
            ps.executeUpdate();
            JOptionPane.showMessageDialog(null, "Producto inhabilitado correctamente.");
            idProductoActual = -1;
            limpiar();
            cargarTabla();
        } catch (Exception e) { //error al inhabilitar producto - pantalla Agregar Producto
            JOptionPane.showMessageDialog(null, "Error al inhabilitar: " + e.getMessage());
        }
    }

    @FXML
    public void FnHabilitar() {
        if (idProductoActual == -1) {
            JOptionPane.showMessageDialog(null, "Seleccione un producto de la tabla o use 🔍.");
            return;
        }
        try (java.sql.Connection con = conexion.establecerConexion();
             java.sql.PreparedStatement ps = con.prepareStatement(
                     "UPDATE tbl_producto SET disponibilidad = 1 WHERE id_producto = ?")) {
            ps.setInt(1, idProductoActual);
            ps.executeUpdate();
            JOptionPane.showMessageDialog(null, "Producto habilitado correctamente.");
            idProductoActual = -1;
            limpiar();
            cargarTabla();
        } catch (Exception e) { //error al habilitar producto - pantalla Agregar Producto
            JOptionPane.showMessageDialog(null, "Error al habilitar: " + e.getMessage());
        }
    }

    @FXML
    public void FnSeleccionarImagen() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Seleccionar imagen del producto");
        fc.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Imágenes", "*.png", "*.jpg", "*.jpeg", "*.gif"));
        File file = fc.showOpenDialog(null);
        if (file != null) {
            rutaImagenActual = file.getAbsolutePath();
            if (lblRutaImagen != null) lblRutaImagen.setText(file.getName());
            mostrarImagen(rutaImagenActual);
        }
    }

    private void mostrarImagen(String ruta) {
        if (imgProducto == null || ruta == null || ruta.isBlank()) return;
        try {
            Image img = null;
            if (ruta.startsWith("/")) {
                // Ruta de classpath (imágenes empaquetadas en resources)
                var stream = getClass().getResourceAsStream(ruta);
                if (stream != null) img = new Image(stream);
            } else {
                // Ruta absoluta del sistema de archivos
                File f = new File(ruta);
                if (f.exists()) img = new Image(f.toURI().toString());
            }
            if (img != null) imgProducto.setImage(img);
        } catch (Exception ignored) {}
    }

    // ================================================================
    //  TABLA — muestra nombre, tipo y precio mínimo de presentaciones
    // ================================================================
    private void cargarTabla() {
        ObservableList<ProductoRow> datos = FXCollections.observableArrayList();
        String sql =
            "SELECT p.nombre, p.tipo, MIN(pp.costo) AS precio " +
            "FROM tbl_producto p " +
            "LEFT JOIN tbl_presentacion_producto pp ON p.id_producto = pp.id_producto " +
            "GROUP BY p.id_producto, p.nombre, p.tipo " +
            "ORDER BY p.nombre";

        try (Connection con = conexion.establecerConexion();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                double precio = rs.getDouble("precio");
                String precioStr = rs.wasNull() ? "—" : String.format("%.2f", precio);
                datos.add(new ProductoRow(
                        rs.getString("nombre"),
                        rs.getString("tipo"),
                        precioStr));
            }
            tablaProductos.setItems(datos);

        } catch (Exception e) { //error al cargar tabla de productos - pantalla Agregar Producto
            JOptionPane.showMessageDialog(null, "Error al cargar tabla: " + e.getMessage());
        }
    }

    private void rellenarFormulario(ProductoRow row) {
        Txtnombre.setText(row.nombre.get());
        cmbtipo.setValue(row.tipo.get());
        if (Txtprecio != null) Txtprecio.setText(row.precio.get().equals("—") ? "" : row.precio.get());
        try (Connection con = conexion.establecerConexion();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT id_producto, disponibilidad FROM tbl_producto WHERE nombre = ?")) {
            ps.setString(1, row.nombre.get());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                idProductoActual = rs.getInt("id_producto");
                if (rs.getBoolean("disponibilidad")) rbSi.setSelected(true);
                else rbNo.setSelected(true);
            }
        } catch (Exception ignore) {}
        autoCargarImagen(row.nombre.get());
    }

    private void autoCargarImagen(String nombre) {
        if (imgProducto == null || nombre == null) return;
        String base = "/com/example/demo1/imagenes/productos/";
        String[] exts = {".jpg", ".png", ".jpeg"};
        // 1. Coincidencia exacta
        for (String ext : exts) {
            var stream = getClass().getResourceAsStream(base + nombre + ext);
            if (stream != null) {
                imgProducto.setImage(new Image(stream));
                if (lblRutaImagen != null) lblRutaImagen.setText(nombre + ext);
                return;
            }
        }
        // 2. Coincidencia parcial: alguna imagen cuyo nombre contiene una palabra del producto
        String[] palabras = nombre.toLowerCase().split("\\s+");
        try {
            var dir = getClass().getResource(base);
            if (dir != null) {
                File folder = new File(dir.toURI());
                File[] files = folder.listFiles();
                if (files == null) return;
                for (File img : files) {
                    String fn = img.getName().toLowerCase();
                    for (String palabra : palabras) {
                        if (palabra.length() > 3 && fn.contains(palabra)) {
                            imgProducto.setImage(new Image(img.toURI().toString()));
                            if (lblRutaImagen != null) lblRutaImagen.setText(img.getName());
                            return;
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
        // 3. Sin coincidencia
        imgProducto.setImage(null);
        if (lblRutaImagen != null) lblRutaImagen.setText("Sin imagen");
    }

    public void limpiar() {
        Txtnombre.clear();
        if (Txtprecio  != null) Txtprecio.clear();
        if (txtporcion != null) txtporcion.clear();
        cmbtipo.getSelectionModel().clearSelection();
        grupoDisponible.selectToggle(null);
        grupoPresentacion.selectToggle(null);
        chkMasa.setSelected(false);     chkSalsa.setSelected(false);
        chkQueso.setSelected(false);    chkPepperoni.setSelected(false);
        chkMaiz.setSelected(false);     chkSalchicha.setSelected(false);
        chkVegetales.setSelected(false); chkPina.setSelected(false);
        chkBebida.setSelected(false);   chkOtro.setSelected(false);
        if (lblRutaImagen != null) lblRutaImagen.setText("Sin imagen seleccionada");
        if (imgProducto   != null) imgProducto.setImage(null);
        rutaImagenActual = null;
        idProductoActual = -1;
    }

    // ================================================================
    //  ROW MODEL
    // ================================================================
    public static class ProductoRow {
        final SimpleStringProperty nombre, tipo, precio;

        public ProductoRow(String n, String t, String p) {
            nombre = new SimpleStringProperty(n);
            tipo   = new SimpleStringProperty(t);
            precio = new SimpleStringProperty(p);
        }
        public String getNombre() { return nombre.get(); }
        public String getTipo()   { return tipo.get(); }
        public String getPrecio() { return precio.get(); }
    }
}
