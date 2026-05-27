package com.example.demo1.Controllers;

import com.example.demo1.Database.Conexion;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javax.swing.JOptionPane;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Controlador de la pantalla "Gestión de Ofertas".
 *
 * Tablas:
 *   tbl_oferta          — cabecera de la oferta (nombre, descuento, código, fechas, activa)
 *   tbl_oferta_producto — N:N relación oferta ↔ producto (crear con el script del README)
 *
 * Script SQL requerido (ejecutar una sola vez):
 * ─────────────────────────────────────────────────────────────────
 *  CREATE TABLE tbl_oferta_producto (
 *      id_oferta   INT NOT NULL REFERENCES tbl_oferta(id_oferta) ON DELETE CASCADE,
 *      id_producto INT NOT NULL REFERENCES tbl_producto(id_producto),
 *      CONSTRAINT PK_oferta_producto PRIMARY KEY (id_oferta, id_producto)
 *  );
 *  INSERT INTO tbl_oferta_producto (id_oferta, id_producto)
 *  SELECT id_oferta, id_producto FROM tbl_oferta WHERE id_producto IS NOT NULL;
 *  ALTER TABLE tbl_oferta ADD codigo_promocional VARCHAR(50) NULL;
 * ─────────────────────────────────────────────────────────────────
 */
public class CONTROLLER_Ofertas {

    // ── FXML ─────────────────────────────────────────────────────────────────
    @FXML private TextField              TXTnombre;
    @FXML private TextField              TXTdescripcion;
    @FXML private TextField              TXTcodigo;
    @FXML private TextField              TXTdescuento;
    @FXML private DatePicker             dpInicio;
    @FXML private DatePicker             dpFin;
    @FXML private ListView<String>       lstProductos;
    @FXML private CheckBox               chkActiva;

    @FXML private TableView<String[]>    tablaOfertas;
    @FXML private TableColumn<String[], String> colId;
    @FXML private TableColumn<String[], String> colNombre;
    @FXML private TableColumn<String[], String> colProducto;
    @FXML private TableColumn<String[], String> colDescuento;
    @FXML private TableColumn<String[], String> colCodigo;
    @FXML private TableColumn<String[], String> colInicio;
    @FXML private TableColumn<String[], String> colFin;
    @FXML private TableColumn<String[], String> colActiva;
    @FXML private Label                  lblTotal;

    // ── Estado ───────────────────────────────────────────────────────────────
    private int idOfertaSeleccionada = -1;

    // ── Inicialización ───────────────────────────────────────────────────────
    @FXML
    public void initialize() {
        colId       .setCellValueFactory(c -> new SimpleStringProperty(c.getValue()[0]));
        colNombre   .setCellValueFactory(c -> new SimpleStringProperty(c.getValue()[1]));
        colProducto .setCellValueFactory(c -> new SimpleStringProperty(c.getValue()[2]));
        colDescuento.setCellValueFactory(c -> new SimpleStringProperty(c.getValue()[3]));
        colCodigo   .setCellValueFactory(c -> new SimpleStringProperty(c.getValue()[4]));
        colInicio   .setCellValueFactory(c -> new SimpleStringProperty(c.getValue()[5]));
        colFin      .setCellValueFactory(c -> new SimpleStringProperty(c.getValue()[6]));
        colActiva   .setCellValueFactory(c -> new SimpleStringProperty(c.getValue()[7]));

        lstProductos.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        cargarProductos();
        cargarOfertas();

        dpInicio.valueProperty().addListener((obs, anterior, inicio) -> {
            actualizarRestriccionFin(inicio);
            if (inicio != null && dpFin.getValue() != null
                    && dpFin.getValue().isBefore(inicio)) {
                dpFin.setValue(null);
            }
        });

        tablaOfertas.getSelectionModel().selectedItemProperty()
                .addListener((obs, old, sel) -> { if (sel != null) cargarOfertaEnFormulario(); });
    }

    // ── Restricción de fecha fin ─────────────────────────────────────────────

    private void actualizarRestriccionFin(LocalDate inicio) {
        dpFin.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate fecha, boolean vacio) {
                super.updateItem(fecha, vacio);
                if (inicio != null && fecha.isBefore(inicio)) {
                    setDisable(true);
                    setStyle("-fx-background-color: #f0f0f0; -fx-text-fill: #bbb;");
                }
            }
        });
    }

    // ── Carga de lista de productos ──────────────────────────────────────────

    private void cargarProductos() {
        ObservableList<String> lista = FXCollections.observableArrayList();
        try (Connection con = Conexion.establecerConexion();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT id_producto, nombre FROM tbl_producto " +
                     "WHERE disponibilidad = 1 ORDER BY nombre");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next())
                lista.add(rs.getInt("id_producto") + " — " + rs.getString("nombre"));
            lstProductos.setItems(lista);
        } catch (Exception e) { //error al cargar productos para oferta - pantalla Ofertas
            JOptionPane.showMessageDialog(null, "Error al cargar productos: " + e.getMessage());
        }
    }

    // ── Carga de tabla de ofertas ────────────────────────────────────────────

    private void cargarOfertas() {
        ObservableList<String[]> datos = FXCollections.observableArrayList();

        // Productos por oferta como lista separada por comas (SQL Server 2017+)
        String sql = "SELECT o.id_oferta, o.nombre, " +
                     "ISNULL(STUFF((SELECT ', ' + p.nombre " +
                     "              FROM tbl_oferta_producto op2 " +
                     "              JOIN tbl_producto p ON p.id_producto = op2.id_producto " +
                     "              WHERE op2.id_oferta = o.id_oferta " +
                     "              FOR XML PATH(''), TYPE).value('.','NVARCHAR(MAX)'), 1, 2, ''), " +
                     "       '— Todos —') AS productos, " +
                     "o.descuento_porcentaje, " +
                     "ISNULL(o.codigo_promocional, '—') AS codigo_promocional, " +
                     "o.fecha_inicio, o.fecha_fin, o.activa " +
                     "FROM tbl_oferta o " +
                     "ORDER BY o.activa DESC, o.id_oferta DESC";

        try (Connection con = Conexion.establecerConexion();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                datos.add(new String[]{
                    String.valueOf(rs.getInt("id_oferta")),
                    nvl(rs.getString("nombre")),
                    nvl(rs.getString("productos")),
                    rs.getDouble("descuento_porcentaje") + "%",
                    nvl(rs.getString("codigo_promocional")),
                    nvl(rs.getString("fecha_inicio")),
                    nvl(rs.getString("fecha_fin")),
                    rs.getBoolean("activa") ? "✅ Activa" : "⏸ Pausada"
                });
            }
        } catch (Exception e) { //error al cargar tabla de ofertas - pantalla Ofertas
            JOptionPane.showMessageDialog(null, "Error al cargar ofertas: " + e.getMessage());
        }
        tablaOfertas.setItems(datos);
        if (lblTotal != null) lblTotal.setText(String.valueOf(datos.size()));
    }

    // ── Llenar formulario desde BD ───────────────────────────────────────────

    private void cargarOfertaEnFormulario() {
        String[] row = tablaOfertas.getSelectionModel().getSelectedItem();
        if (row == null) return;
        idOfertaSeleccionada = Integer.parseInt(row[0]);

        try (Connection con = Conexion.establecerConexion()) {
            // Datos del encabezado de la oferta
            try (PreparedStatement ps = con.prepareStatement(
                    "SELECT * FROM tbl_oferta WHERE id_oferta = ?")) {
                ps.setInt(1, idOfertaSeleccionada);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        TXTnombre     .setText(nvl(rs.getString("nombre")));
                        TXTdescripcion.setText(nvl(rs.getString("descripcion")));
                        TXTcodigo     .setText(nvl(rs.getString("codigo_promocional")));
                        TXTdescuento  .setText(String.valueOf(rs.getDouble("descuento_porcentaje")));
                        chkActiva     .setSelected(rs.getBoolean("activa"));

                        Date fi = rs.getDate("fecha_inicio");
                        Date ff = rs.getDate("fecha_fin");
                        dpInicio.setValue(fi != null ? fi.toLocalDate() : null);
                        dpFin   .setValue(ff != null ? ff.toLocalDate() : null);
                    }
                }
            }

            // IDs de productos asignados a esta oferta
            List<Integer> idsAsignados = new ArrayList<>();
            try (PreparedStatement ps = con.prepareStatement(
                    "SELECT id_producto FROM tbl_oferta_producto WHERE id_oferta = ?")) {
                ps.setInt(1, idOfertaSeleccionada);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) idsAsignados.add(rs.getInt("id_producto"));
                }
            }

            // Pre-seleccionar en la ListView
            lstProductos.getSelectionModel().clearSelection();
            ObservableList<String> items = lstProductos.getItems();
            for (int i = 0; i < items.size(); i++) {
                int idProd = Integer.parseInt(items.get(i).split(" — ")[0]);
                if (idsAsignados.contains(idProd))
                    lstProductos.getSelectionModel().select(i);
            }

        } catch (Exception e) { //error al cargar oferta en formulario - pantalla Ofertas
            JOptionPane.showMessageDialog(null, "Error: " + e.getMessage());
        }
    }

    // ── Guardar (INSERT o UPDATE) ────────────────────────────────────────────

    @FXML
    public void FnGuardar() {
        String nombre   = TXTnombre.getText().trim();
        String desc     = TXTdescripcion.getText().trim();
        String codigo   = TXTcodigo.getText().trim().toUpperCase();
        String descuStr = TXTdescuento.getText().trim();

        if (nombre.isEmpty() || descuStr.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Nombre y descuento son obligatorios.");
            return;
        }

        double descuento;
        try {
            descuento = Double.parseDouble(descuStr);
            if (descuento < 0 || descuento > 100) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(null, "Descuento debe ser un número entre 0 y 100.");
            return;
        }

        LocalDate inicio = dpInicio.getValue();
        LocalDate fin    = dpFin.getValue();

        if (fin != null && inicio != null && fin.isBefore(inicio)) {
            JOptionPane.showMessageDialog(null, "La fecha fin no puede ser anterior a la fecha inicio.");
            return;
        }

        java.sql.Date fechaVigencia = fin != null
                ? Date.valueOf(fin)
                : Date.valueOf(java.time.LocalDate.now().plusMonths(1));

        boolean activa  = chkActiva.isSelected();
        boolean esNueva = (idOfertaSeleccionada == -1);

        // IDs de productos seleccionados (puede ser vacío = todos)
        List<Integer> prodSeleccionados = new ArrayList<>();
        for (String sel : lstProductos.getSelectionModel().getSelectedItems()) {
            prodSeleccionados.add(Integer.parseInt(sel.split(" — ")[0]));
        }

        Connection con = null;
        try {
            con = Conexion.establecerConexion();
            if (con == null) { JOptionPane.showMessageDialog(null, "Sin conexión."); return; }
            con.setAutoCommit(false);

            int idOferta;

            if (esNueva) {
                String sqlIns =
                    "INSERT INTO tbl_oferta (nombre, descripcion, descuento_porcentaje, " +
                    "fecha_inicio, fecha_fin, fecha_vigencia, activa, codigo_promocional) " +
                    "VALUES (?,?,?,?,?,?,?,?)";
                try (PreparedStatement ps = con.prepareStatement(sqlIns, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString (1, nombre);
                    ps.setString (2, desc);
                    ps.setDouble (3, descuento);
                    ps.setDate   (4, inicio != null ? Date.valueOf(inicio) : null);
                    ps.setDate   (5, fin    != null ? Date.valueOf(fin)    : null);
                    ps.setDate   (6, fechaVigencia);
                    ps.setBoolean(7, activa);
                    ps.setString (8, codigo.isEmpty() ? null : codigo);
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (!keys.next()) throw new SQLException("No se obtuvo el ID de la oferta.");
                        idOferta = keys.getInt(1);
                    }
                }
            } else {
                idOferta = idOfertaSeleccionada;
                String sqlUpd =
                    "UPDATE tbl_oferta SET nombre=?, descripcion=?, descuento_porcentaje=?, " +
                    "fecha_inicio=?, fecha_fin=?, fecha_vigencia=?, activa=?, codigo_promocional=? " +
                    "WHERE id_oferta=?";
                try (PreparedStatement ps = con.prepareStatement(sqlUpd)) {
                    ps.setString (1, nombre);
                    ps.setString (2, desc);
                    ps.setDouble (3, descuento);
                    ps.setDate   (4, inicio != null ? Date.valueOf(inicio) : null);
                    ps.setDate   (5, fin    != null ? Date.valueOf(fin)    : null);
                    ps.setDate   (6, fechaVigencia);
                    ps.setBoolean(7, activa);
                    ps.setString (8, codigo.isEmpty() ? null : codigo);
                    ps.setInt    (9, idOferta);
                    ps.executeUpdate();
                }
            }

            // Reemplazar productos asignados
            try (PreparedStatement psDel = con.prepareStatement(
                    "DELETE FROM tbl_oferta_producto WHERE id_oferta = ?")) {
                psDel.setInt(1, idOferta);
                psDel.executeUpdate();
            }
            if (!prodSeleccionados.isEmpty()) {
                try (PreparedStatement psIns = con.prepareStatement(
                        "INSERT INTO tbl_oferta_producto (id_oferta, id_producto) VALUES (?, ?)")) {
                    for (int idProd : prodSeleccionados) {
                        psIns.setInt(1, idOferta);
                        psIns.setInt(2, idProd);
                        psIns.addBatch();
                    }
                    psIns.executeBatch();
                }
            }

            con.commit();
            JOptionPane.showMessageDialog(null, esNueva ? "✅ Oferta creada." : "✅ Oferta actualizada.");
            FnLimpiar();
            cargarOfertas();

        } catch (Exception e) { //error al guardar oferta - pantalla Ofertas
            try { if (con != null) con.rollback(); } catch (Exception ignored) {}
            JOptionPane.showMessageDialog(null, "Error al guardar: " + e.getMessage());
        } finally {
            try { if (con != null) con.close(); } catch (Exception ignored) {}
        }
    }

    // ── Activar / Pausar ────────────────────────────────────────────────────

    @FXML
    public void FnToggleActiva() {
        if (idOfertaSeleccionada == -1) {
            JOptionPane.showMessageDialog(null, "Selecciona una oferta de la tabla primero.");
            return;
        }
        try (Connection con = Conexion.establecerConexion();
             PreparedStatement ps = con.prepareStatement(
                     "UPDATE tbl_oferta SET activa = ~activa WHERE id_oferta = ?")) {
            ps.setInt(1, idOfertaSeleccionada);
            ps.executeUpdate();
            JOptionPane.showMessageDialog(null, "Estado de la oferta actualizado.");
            FnLimpiar();
            cargarOfertas();
        } catch (Exception e) { //error al activar/pausar oferta - pantalla Ofertas
            JOptionPane.showMessageDialog(null, "Error: " + e.getMessage());
        }
    }

    // ── Eliminar ─────────────────────────────────────────────────────────────

    @FXML
    public void FnEliminar() {
        if (!com.example.demo1.Utils.Permisos_Util.verificarEliminar()) return;
        if (idOfertaSeleccionada == -1) {
            JOptionPane.showMessageDialog(null, "Selecciona una oferta de la tabla.");
            return;
        }
        int conf = JOptionPane.showConfirmDialog(null,
                "¿Eliminar esta oferta permanentemente?",
                "Confirmar", JOptionPane.YES_NO_OPTION);
        if (conf != JOptionPane.YES_OPTION) return;

        try (Connection con = Conexion.establecerConexion();
             PreparedStatement ps = con.prepareStatement(
                     "DELETE FROM tbl_oferta WHERE id_oferta = ?")) {
            ps.setInt(1, idOfertaSeleccionada);
            ps.executeUpdate();
            JOptionPane.showMessageDialog(null, "Oferta eliminada.");
            FnLimpiar();
            cargarOfertas();
        } catch (Exception e) { //error al eliminar oferta - pantalla Ofertas
            JOptionPane.showMessageDialog(null, "Error: " + e.getMessage());
        }
    }

    // ── Limpiar formulario ───────────────────────────────────────────────────

    @FXML
    public void FnLimpiar() {
        idOfertaSeleccionada = -1;
        TXTnombre     .clear();
        TXTdescripcion.clear();
        TXTcodigo     .clear();
        TXTdescuento  .clear();
        dpInicio      .setValue(null);
        dpFin         .setValue(null);
        lstProductos  .getSelectionModel().clearSelection();
        chkActiva     .setSelected(true);
        tablaOfertas  .getSelectionModel().clearSelection();
    }

    // ── Utilidades ───────────────────────────────────────────────────────────
    private String nvl(String v) { return v != null ? v : ""; }
}
