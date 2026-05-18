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

/**
 * Controlador de la pantalla "Gestión de Ofertas".
 *
 * Tabla requerida en BD (crear una sola vez):
 * ─────────────────────────────────────────────────────────────────
 *  CREATE TABLE tbl_oferta (
 *      id_oferta            INT IDENTITY(1,1) PRIMARY KEY,
 *      nombre               NVARCHAR(100) NOT NULL,
 *      descripcion          NVARCHAR(255),
 *      descuento_porcentaje DECIMAL(5,2)  NOT NULL DEFAULT 0,
 *      fecha_inicio         DATE,
 *      fecha_fin            DATE,
 *      activa               BIT NOT NULL DEFAULT 1,
 *      id_producto          INT REFERENCES tbl_producto(id_producto)
 *  );
 * ─────────────────────────────────────────────────────────────────
 *
 * Rol con acceso: admin, gerente  (marketing si se agrega ese rol).
 */
public class CONTROLLER_Ofertas {

    // ── FXML ─────────────────────────────────────────────────────────────────
    @FXML private TextField              TXTnombre;
    @FXML private TextField              TXTdescripcion;
    @FXML private TextField              TXTdescuento;
    @FXML private DatePicker             dpInicio;
    @FXML private DatePicker             dpFin;
    @FXML private ComboBox<String>       cmbProducto;
    @FXML private CheckBox               chkActiva;

    @FXML private TableView<String[]>    tablaOfertas;
    @FXML private TableColumn<String[], String> colId;
    @FXML private TableColumn<String[], String> colNombre;
    @FXML private TableColumn<String[], String> colProducto;
    @FXML private TableColumn<String[], String> colDescuento;
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
        colInicio   .setCellValueFactory(c -> new SimpleStringProperty(c.getValue()[4]));
        colFin      .setCellValueFactory(c -> new SimpleStringProperty(c.getValue()[5]));
        colActiva   .setCellValueFactory(c -> new SimpleStringProperty(c.getValue()[6]));

        cargarProductos();
        cargarOfertas();

        // Clic en fila → llenar formulario para editar
        tablaOfertas.getSelectionModel().selectedItemProperty()
                .addListener((obs, old, sel) -> { if (sel != null) cargarOfertaEnFormulario(); });
    }

    // ── Carga de combos ──────────────────────────────────────────────────────

    private void cargarProductos() {
        String sql = "SELECT id_producto, nombre FROM tbl_producto " +
                     "WHERE disponibilidad = 1 ORDER BY nombre";
        try (Connection con = Conexion.establecerConexion();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            ObservableList<String> lista = FXCollections.observableArrayList();
            while (rs.next())
                lista.add(rs.getInt("id_producto") + " — " + rs.getString("nombre"));
            cmbProducto.setItems(lista);

        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error al cargar productos: " + e.getMessage());
        }
    }

    // ── Carga de tabla ───────────────────────────────────────────────────────

    private void cargarOfertas() {
        ObservableList<String[]> datos = FXCollections.observableArrayList();
        String sql = """
            SELECT o.id_oferta, o.nombre, p.nombre AS producto,
                   o.descuento_porcentaje, o.fecha_inicio, o.fecha_fin, o.activa
            FROM tbl_oferta o
            LEFT JOIN tbl_producto p ON o.id_producto = p.id_producto
            ORDER BY o.activa DESC, o.id_oferta DESC
            """;
        try (Connection con = Conexion.establecerConexion();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                datos.add(new String[]{
                    String.valueOf(rs.getInt("id_oferta")),
                    nvl(rs.getString("nombre")),
                    nvl(rs.getString("producto")),
                    rs.getDouble("descuento_porcentaje") + "%",
                    nvl(rs.getString("fecha_inicio")),
                    nvl(rs.getString("fecha_fin")),
                    rs.getBoolean("activa") ? "✅ Activa" : "⏸ Pausada"
                });
            }
        } catch (Exception e) {
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

        String sql = "SELECT * FROM tbl_oferta WHERE id_oferta = ?";
        try (Connection con = Conexion.establecerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idOfertaSeleccionada);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    TXTnombre     .setText(nvl(rs.getString("nombre")));
                    TXTdescripcion.setText(nvl(rs.getString("descripcion")));
                    TXTdescuento  .setText(String.valueOf(rs.getDouble("descuento_porcentaje")));
                    chkActiva     .setSelected(rs.getBoolean("activa"));

                    Date fi = rs.getDate("fecha_inicio");
                    Date ff = rs.getDate("fecha_fin");
                    dpInicio.setValue(fi != null ? fi.toLocalDate() : null);
                    dpFin   .setValue(ff != null ? ff.toLocalDate() : null);

                    int idProd = rs.getInt("id_producto");
                    cmbProducto.getItems().stream()
                            .filter(s -> s.startsWith(idProd + " —"))
                            .findFirst()
                            .ifPresent(cmbProducto::setValue);
                }
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error: " + e.getMessage());
        }
    }

    // ── Guardar (INSERT o UPDATE) ────────────────────────────────────────────

    @FXML
    public void FnGuardar() {
        String nombre   = TXTnombre.getText().trim();
        String desc     = TXTdescripcion.getText().trim();
        String descuStr = TXTdescuento.getText().trim();

        if (nombre.isEmpty() || descuStr.isEmpty() || cmbProducto.getValue() == null) {
            JOptionPane.showMessageDialog(null,
                    "Nombre, descuento y producto son obligatorios.");
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

        int      idProducto = Integer.parseInt(cmbProducto.getValue().split(" — ")[0]);
        boolean  activa     = chkActiva.isSelected();
        LocalDate inicio    = dpInicio.getValue();
        LocalDate fin       = dpFin.getValue();

        if (fin != null && inicio != null && fin.isBefore(inicio)) {
            JOptionPane.showMessageDialog(null, "La fecha fin no puede ser anterior a la fecha inicio.");
            return;
        }

        boolean esNueva = (idOfertaSeleccionada == -1);
        String sql = esNueva
            ? "INSERT INTO tbl_oferta (nombre, descripcion, descuento_porcentaje, " +
              "fecha_inicio, fecha_fin, activa, id_producto) VALUES (?,?,?,?,?,?,?)"
            : "UPDATE tbl_oferta SET nombre=?, descripcion=?, descuento_porcentaje=?, " +
              "fecha_inicio=?, fecha_fin=?, activa=?, id_producto=? WHERE id_oferta=?";

        try (Connection con = Conexion.establecerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString (1, nombre);
            ps.setString (2, desc);
            ps.setDouble (3, descuento);
            ps.setDate   (4, inicio != null ? Date.valueOf(inicio) : null);
            ps.setDate   (5, fin    != null ? Date.valueOf(fin)    : null);
            ps.setBoolean(6, activa);
            ps.setInt    (7, idProducto);
            if (!esNueva) ps.setInt(8, idOfertaSeleccionada);

            ps.executeUpdate();
            JOptionPane.showMessageDialog(null, esNueva ? "✅ Oferta creada." : "✅ Oferta actualizada.");
            FnLimpiar();
            cargarOfertas();

        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error al guardar: " + e.getMessage());
        }
    }

    // ── Activar / Pausar ────────────────────────────────────────────────────

    @FXML
    public void FnToggleActiva() {
        if (idOfertaSeleccionada == -1) {
            JOptionPane.showMessageDialog(null,
                    "Selecciona una oferta de la tabla primero.");
            return;
        }
        // ~ en SQL Server hace NOT bit
        String sql = "UPDATE tbl_oferta SET activa = ~activa WHERE id_oferta = ?";
        try (Connection con = Conexion.establecerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idOfertaSeleccionada);
            ps.executeUpdate();
            JOptionPane.showMessageDialog(null, "Estado de la oferta actualizado.");
            FnLimpiar();
            cargarOfertas();

        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error: " + e.getMessage());
        }
    }

    // ── Eliminar ─────────────────────────────────────────────────────────────

    @FXML
    public void FnEliminar() {
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

        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error: " + e.getMessage());
        }
    }

    // ── Limpiar formulario ───────────────────────────────────────────────────

    @FXML
    public void FnLimpiar() {
        idOfertaSeleccionada = -1;
        TXTnombre     .clear();
        TXTdescripcion.clear();
        TXTdescuento  .clear();
        dpInicio      .setValue(null);
        dpFin         .setValue(null);
        cmbProducto   .getSelectionModel().clearSelection();
        chkActiva     .setSelected(true);
        tablaOfertas  .getSelectionModel().clearSelection();
    }

    // ── Utilidades ───────────────────────────────────────────────────────────
    private String nvl(String v) { return v != null ? v : ""; }
}
