package com.example.demo1.Controllers;

import com.example.demo1.Database.Conexion;
import com.example.demo1.Utils.JasperUtil;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javax.swing.JOptionPane;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;

public class CONTROLLER_Ingrediente {

    Conexion conexion = new Conexion();

    @FXML private TextField  TXTnombre;
    @FXML private TextField  TXTcantidad;
    @FXML private TextField  TXTfecha;
    @FXML private ComboBox<String> cmbTipo;
    @FXML private TextField  TXTcantMin;
    @FXML private ComboBox<String> cmbUnidad;
    @FXML private ComboBox<String> cmbProveedor;

    @FXML private TableView<IngredienteRow>      tablaIngredientes;
    @FXML private TableColumn<IngredienteRow, String> colNombre;
    @FXML private TableColumn<IngredienteRow, String> colTipo;
    @FXML private TableColumn<IngredienteRow, String> colStock;
    @FXML private TableColumn<IngredienteRow, String> colCantMin;
    @FXML private TableColumn<IngredienteRow, String> colUnidad;
    @FXML private TableColumn<IngredienteRow, String> colFecha;
    @FXML private TableColumn<IngredienteRow, String> colProveedor;
    @FXML private TableColumn<IngredienteRow, String> colEstado;

    private int idIngredienteActual = -1;
    private final Map<String, Integer> mapaProveedores = new HashMap<>();

    // ================================================================
    //                     INITIALIZE
    // ================================================================
    @FXML
    public void initialize() {
        cmbTipo.setItems(FXCollections.observableArrayList(
                "Base", "Salsa", "Lacteo", "Embutido", "Vegetal", "Fruta", "Bebida"));

        cmbUnidad.setItems(FXCollections.observableArrayList(
                "lb",       // libras  → carnes, queso, masa, embutidos
                "paquete",  // paquete → pepperoni, salchicha, masa pre-empacada
                "kg",       // kilos   → vegetales, harina a granel
                "oz",       // onzas   → especias, condimentos
                "L",        // litros  → salsas, aceites, bebidas
                "lata",     // lata    → maíz, piña, salsa de tomate
                "bolsa",    // bolsa   → harina, azúcar, sal
                "unidad",   // unidad  → huevos, botellas individuales
                "caja"      // caja    → bebidas empacadas, materiales
        ));

        colNombre.setCellValueFactory(   c -> c.getValue().nombre);
        colTipo.setCellValueFactory(     c -> c.getValue().tipo);
        colStock.setCellValueFactory(    c -> c.getValue().stock);
        colCantMin.setCellValueFactory(  c -> c.getValue().cantMin);
        colUnidad.setCellValueFactory(   c -> c.getValue().unidad);
        colFecha.setCellValueFactory(    c -> c.getValue().fecha);
        colProveedor.setCellValueFactory(c -> c.getValue().proveedor);
        colEstado.setCellValueFactory(   c -> c.getValue().estado);

        // Al hacer clic en una fila, rellena el formulario
        tablaIngredientes.getSelectionModel().selectedItemProperty().addListener(
                (obs, old, sel) -> { if (sel != null) rellenarFormulario(sel); });

        cargarProveedores();
        cargarTabla();
    }

    // ================================================================
    //                  CARGA TABLA
    // ================================================================
    private void cargarTabla() {
        ObservableList<IngredienteRow> lista = FXCollections.observableArrayList();
        String sql =
                "SELECT i.id_ingrediente, i.nombre, i.tipo_ingrediente, " +
                "       i.cantidad_stock, i.cantidad_minima_stock, i.unidad_medida, " +
                "       CONVERT(varchar, i.fecha_caducidad, 23) AS fecha, " +
                "       p.nombre_proveedor, i.estado " +
                "FROM tbl_ingrediente i " +
                "LEFT JOIN tbl_proveedor p ON i.id_proveedor = p.id_proveedor " +
                "ORDER BY i.nombre";

        try (Connection con = conexion.establecerConexion();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                lista.add(new IngredienteRow(
                        rs.getInt("id_ingrediente"),
                        rs.getString("nombre"),
                        rs.getString("tipo_ingrediente"),
                        String.valueOf(rs.getInt("cantidad_stock")),
                        String.valueOf(rs.getInt("cantidad_minima_stock")),
                        rs.getString("unidad_medida") != null ? rs.getString("unidad_medida") : "",
                        rs.getString("fecha") != null ? rs.getString("fecha") : "",
                        rs.getString("nombre_proveedor") != null ? rs.getString("nombre_proveedor") : "",
                        rs.getString("estado") != null ? rs.getString("estado") : "Activo"
                ));
            }
            tablaIngredientes.setItems(lista);

        } catch (Exception e) { //error al cargar tabla - pantalla Agregar Ingrediente
            JOptionPane.showMessageDialog(null, "Error al cargar tabla: " + e.getMessage());
        }
    }

    // ================================================================
    //                  CARGA COMBO PROVEEDORES
    // ================================================================
    private void cargarProveedores() {
        ObservableList<String> nombres = FXCollections.observableArrayList();
        String sql = "SELECT id_proveedor, nombre_proveedor FROM tbl_proveedor ORDER BY nombre_proveedor";

        try (Connection con = conexion.establecerConexion();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            mapaProveedores.clear();
            while (rs.next()) {
                String nombre = rs.getString("nombre_proveedor");
                int    id     = rs.getInt("id_proveedor");
                nombres.add(nombre);
                mapaProveedores.put(nombre, id);
            }
            cmbProveedor.setItems(nombres);

        } catch (Exception e) { //error al cargar proveedores - pantalla Agregar Ingrediente
            JOptionPane.showMessageDialog(null, "Error al cargar proveedores: " + e.getMessage());
        }
    }

    // ================================================================
    //                  RELLENA FORMULARIO AL SELECCIONAR FILA
    // ================================================================
    private void rellenarFormulario(IngredienteRow row) {
        idIngredienteActual = row.id;
        TXTnombre.setText(row.nombre.get());
        TXTcantidad.setText(row.stock.get());
        TXTfecha.setText(row.fecha.get());
        TXTcantMin.setText(row.cantMin.get());
        cmbTipo.setValue(row.tipo.get());
        String unid = row.unidad.get();
        cmbUnidad.setValue(unid.isEmpty() ? null : unid);
        String prov = row.proveedor.get();
        cmbProveedor.setValue(prov.isEmpty() ? null : prov);
    }

    // ================================================================
    //                         INSERT  (Enviar)
    // ================================================================
    @FXML
    public void FnGuardar() {
        String nombre  = TXTnombre.getText().trim();
        String cantStr = TXTcantidad.getText().trim();
        String fecha   = TXTfecha.getText().trim();
        String tipo    = cmbTipo.getValue();
        String minStr  = TXTcantMin.getText().trim();
        String unidad  = cmbUnidad.getValue();
        String prov    = cmbProveedor.getValue();

        if (nombre.isEmpty() || cantStr.isEmpty() || tipo == null) {
            JOptionPane.showMessageDialog(null, "Nombre, cantidad y tipo son obligatorios.");
            return;
        }

        int cantidad, cantMin;
        try {
            cantidad = Integer.parseInt(cantStr);
        } catch (NumberFormatException e) { //error formato cantidad en stock - pantalla Agregar Ingrediente
            JOptionPane.showMessageDialog(null, "La cantidad en stock debe ser un número entero.");
            return;
        }
        try {
            cantMin = minStr.isEmpty() ? 0 : Integer.parseInt(minStr);
        } catch (NumberFormatException e) { //error formato cantidad mínima - pantalla Agregar Ingrediente
            JOptionPane.showMessageDialog(null, "La cantidad mínima debe ser un número entero.");
            return;
        }

        String sqlFecha = fecha.isEmpty() ? "DATEADD(YEAR, 1, GETDATE())" : "?";
        String sql = "INSERT INTO tbl_ingrediente " +
                "(nombre, tipo_ingrediente, cantidad_stock, cantidad_minima_stock, unidad_medida, fecha_caducidad, id_proveedor) " +
                "VALUES (?, ?, ?, ?, ?, " + sqlFecha + ", ?)";

        try (Connection con = conexion.establecerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {

            int p = 1;
            ps.setString(p++, nombre);
            ps.setString(p++, tipo);
            ps.setInt(p++, cantidad);
            ps.setInt(p++, cantMin);
            if (unidad != null && !unidad.isEmpty()) ps.setString(p++, unidad);
            else ps.setNull(p++, Types.VARCHAR);
            if (!fecha.isEmpty()) {
                ps.setDate(p++, Date.valueOf(fecha));
            }
            if (prov != null && mapaProveedores.containsKey(prov)) {
                ps.setInt(p, mapaProveedores.get(prov));
            } else {
                ps.setNull(p, Types.INTEGER);
            }

            ps.executeUpdate();
            JOptionPane.showMessageDialog(null, "Ingrediente guardado correctamente.");

            // Alerta de stock bajo
            try {
                int stock = Integer.parseInt(TXTcantidad.getText().trim());
                int min   = Integer.parseInt(TXTcantMin.getText().trim());
                if (stock < 20 || stock < min) {
                    com.example.demo1.Utils.CorreoUtil.notificarStockBajo(
                            TXTnombre.getText().trim(), stock, min);
                }
            } catch (NumberFormatException ignored) {}

            limpiar();
            cargarTabla();

        } catch (Exception e) { //error al guardar ingrediente - pantalla Agregar Ingrediente
            JOptionPane.showMessageDialog(null, "Error al guardar: " + e.getMessage());
        }
    }

    // ================================================================
    //                         SELECT  (Buscar)
    // ================================================================
    @FXML
    public void FnBuscar() {
        String nombre = TXTnombre.getText().trim();
        if (nombre.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Ingrese el nombre del ingrediente a buscar.");
            return;
        }

        String sql = "SELECT i.id_ingrediente, i.nombre, i.tipo_ingrediente, " +
                "       i.cantidad_stock, i.cantidad_minima_stock, i.unidad_medida, " +
                "       CONVERT(varchar,i.fecha_caducidad,23) as fecha, " +
                "       p.nombre_proveedor " +
                "FROM tbl_ingrediente i " +
                "LEFT JOIN tbl_proveedor p ON i.id_proveedor = p.id_proveedor " +
                "WHERE i.nombre LIKE ?";

        try (Connection con = conexion.establecerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, "%" + nombre + "%");
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    idIngredienteActual = rs.getInt("id_ingrediente");
                    TXTnombre.setText(rs.getString("nombre"));
                    TXTcantidad.setText(String.valueOf(rs.getInt("cantidad_stock")));
                    TXTfecha.setText(rs.getString("fecha") != null ? rs.getString("fecha") : "");
                    cmbTipo.setValue(rs.getString("tipo_ingrediente"));
                    TXTcantMin.setText(String.valueOf(rs.getInt("cantidad_minima_stock")));
                    String unidadVal = rs.getString("unidad_medida");
                    cmbUnidad.setValue(unidadVal != null ? unidadVal : null);
                    String provNombre = rs.getString("nombre_proveedor");
                    cmbProveedor.setValue(provNombre != null ? provNombre : null);
                } else {
                    JOptionPane.showMessageDialog(null, "No se encontró ningún ingrediente con ese nombre.");
                    idIngredienteActual = -1;
                }
            }

        } catch (Exception e) { //error al buscar ingrediente - pantalla Agregar Ingrediente
            JOptionPane.showMessageDialog(null, "Error al buscar: " + e.getMessage());
        }
    }

    // ================================================================
    //                         UPDATE  (Editar)
    // ================================================================
    @FXML
    public void FnEditar() {
        if (idIngredienteActual == -1) {
            JOptionPane.showMessageDialog(null, "Seleccione un ingrediente de la tabla o use 🔍.");
            return;
        }

        String nombre  = TXTnombre.getText().trim();
        String cantStr = TXTcantidad.getText().trim();
        String fecha   = TXTfecha.getText().trim();
        String tipo    = cmbTipo.getValue();
        String minStr  = TXTcantMin.getText().trim();
        String unidad  = cmbUnidad.getValue();
        String prov    = cmbProveedor.getValue();

        if (nombre.isEmpty() || cantStr.isEmpty() || tipo == null) {
            JOptionPane.showMessageDialog(null, "Nombre, cantidad y tipo son obligatorios.");
            return;
        }

        int cantidad, cantMin;
        try {
            cantidad = Integer.parseInt(cantStr);
            cantMin  = minStr.isEmpty() ? 0 : Integer.parseInt(minStr);
        } catch (NumberFormatException e) { //error formato cantidades al editar - pantalla Agregar Ingrediente
            JOptionPane.showMessageDialog(null, "Las cantidades deben ser números enteros.");
            return;
        }

        String sqlFecha = fecha.isEmpty() ? "fecha_caducidad" : "?";
        String sql = "UPDATE tbl_ingrediente " +
                "SET nombre = ?, tipo_ingrediente = ?, cantidad_stock = ?, " +
                "    cantidad_minima_stock = ?, unidad_medida = ?, " +
                "    fecha_caducidad = " + sqlFecha + ", id_proveedor = ? " +
                "WHERE id_ingrediente = ?";

        try (Connection con = conexion.establecerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {

            int p = 1;
            ps.setString(p++, nombre);
            ps.setString(p++, tipo);
            ps.setInt(p++, cantidad);
            ps.setInt(p++, cantMin);
            if (unidad != null && !unidad.isEmpty()) ps.setString(p++, unidad);
            else ps.setNull(p++, Types.VARCHAR);
            if (!fecha.isEmpty()) {
                ps.setDate(p++, Date.valueOf(fecha));
            }
            if (prov != null && mapaProveedores.containsKey(prov)) {
                ps.setInt(p++, mapaProveedores.get(prov));
            } else {
                ps.setNull(p++, Types.INTEGER);
            }
            ps.setInt(p, idIngredienteActual);

            if (ps.executeUpdate() > 0) {
                JOptionPane.showMessageDialog(null, "Ingrediente actualizado correctamente.");

                // Alerta de stock bajo
                try {
                    int stock = Integer.parseInt(TXTcantidad.getText().trim());
                    int min   = Integer.parseInt(TXTcantMin.getText().trim());
                    if (stock < 20 || stock < min) {
                        com.example.demo1.Utils.CorreoUtil.notificarStockBajo(
                                TXTnombre.getText().trim(), stock, min);
                    }
                } catch (NumberFormatException ignored) {}

                cargarTabla();
            } else {
                JOptionPane.showMessageDialog(null, "No se encontró el registro para actualizar.");
            }

        } catch (Exception e) { //error al editar ingrediente - pantalla Agregar Ingrediente
            JOptionPane.showMessageDialog(null, "Error al editar: " + e.getMessage());
        }
    }

    // ================================================================
    //                         DELETE  (Eliminar)
    // ================================================================
    @FXML
    public void FnEliminar() {
        if (!com.example.demo1.Utils.Permisos_Util.verificarEliminar()) return;
        if (idIngredienteActual == -1) {
            JOptionPane.showMessageDialog(null, "Seleccione un ingrediente de la tabla o use 🔍.");
            return;
        }

        String nombre = TXTnombre.getText().trim();
        int c = JOptionPane.showConfirmDialog(null,
                "¿Eliminar el ingrediente '" + nombre + "'?",
                "Confirmar eliminación", JOptionPane.YES_NO_OPTION);
        if (c != JOptionPane.YES_OPTION) return;

        try (Connection con = conexion.establecerConexion();
             PreparedStatement ps = con.prepareStatement(
                     "DELETE FROM tbl_ingrediente WHERE id_ingrediente = ?")) {

            ps.setInt(1, idIngredienteActual);
            if (ps.executeUpdate() > 0) {
                JOptionPane.showMessageDialog(null, "Ingrediente eliminado correctamente.");
                limpiar();
                cargarTabla();
            } else {
                JOptionPane.showMessageDialog(null, "No se encontró el registro para eliminar.");
            }

        } catch (Exception e) { //error al eliminar ingrediente - pantalla Agregar Ingrediente
            JOptionPane.showMessageDialog(null, "Error al eliminar: " + e.getMessage());
        }
    }

    @FXML
    public void FnInhabilitar() {
        if (idIngredienteActual == -1) {
            JOptionPane.showMessageDialog(null, "Seleccione un ingrediente de la tabla o use 🔍.");
            return;
        }
        int confirmar = JOptionPane.showConfirmDialog(null,
                "¿Inhabilitar este ingrediente? No se eliminará, solo quedará inactivo.",
                "Confirmar", JOptionPane.YES_NO_OPTION);
        if (confirmar != JOptionPane.YES_OPTION) return;
        try (java.sql.Connection con = Conexion.establecerConexion();
             java.sql.PreparedStatement ps = con.prepareStatement(
                     "UPDATE tbl_ingrediente SET estado = 'Inactivo' WHERE id_ingrediente = ?")) {
            ps.setInt(1, idIngredienteActual);
            ps.executeUpdate();
            JOptionPane.showMessageDialog(null, "Ingrediente inhabilitado correctamente.");
            idIngredienteActual = -1;
            cargarTabla();
        } catch (Exception e) { //error al inhabilitar ingrediente - pantalla Agregar Ingrediente
            JOptionPane.showMessageDialog(null, "Error al inhabilitar: " + e.getMessage());
        }
    }

    @FXML
    public void FnHabilitar() {
        if (idIngredienteActual == -1) {
            JOptionPane.showMessageDialog(null, "Seleccione un ingrediente de la tabla o use 🔍.");
            return;
        }
        try (java.sql.Connection con = Conexion.establecerConexion();
             java.sql.PreparedStatement ps = con.prepareStatement(
                     "UPDATE tbl_ingrediente SET estado = 'Activo' WHERE id_ingrediente = ?")) {
            ps.setInt(1, idIngredienteActual);
            ps.executeUpdate();
            JOptionPane.showMessageDialog(null, "Ingrediente habilitado correctamente.");
            idIngredienteActual = -1;
            cargarTabla();
        } catch (Exception e) { //error al habilitar ingrediente - pantalla Agregar Ingrediente
            JOptionPane.showMessageDialog(null, "Error al habilitar: " + e.getMessage());
        }
    }

    // ================================================================
    //                       EXPORTAR PDF
    // ================================================================
    @FXML
    public void FnExportarPDF() {
        if (!com.example.demo1.Utils.Permisos_Util.verificarReporte()) return;
        JasperUtil.exportarPDF(
                "/com/example/demo1/reportes/Reporte_Ingredientes.jrxml",
                "Reporte_Ingredientes.pdf"
        );
    }

    // ================================================================
    //                           LIMPIAR
    // ================================================================
    public void limpiar() {
        TXTnombre.clear();
        TXTcantidad.clear();
        TXTfecha.clear();
        TXTcantMin.clear();
        cmbTipo.getSelectionModel().clearSelection();
        cmbUnidad.getSelectionModel().clearSelection();
        cmbProveedor.getSelectionModel().clearSelection();
        idIngredienteActual = -1;
    }

    // ================================================================
    //                       ROW MODEL
    // ================================================================
    public static class IngredienteRow {
        int id;
        final SimpleStringProperty nombre;
        final SimpleStringProperty tipo;
        final SimpleStringProperty stock;
        final SimpleStringProperty cantMin;
        final SimpleStringProperty unidad;
        final SimpleStringProperty fecha;
        final SimpleStringProperty proveedor;
        final SimpleStringProperty estado;

        IngredienteRow(int id, String nombre, String tipo, String stock,
                       String cantMin, String unidad, String fecha, String proveedor, String estado) {
            this.id        = id;
            this.nombre    = new SimpleStringProperty(nombre);
            this.tipo      = new SimpleStringProperty(tipo);
            this.stock     = new SimpleStringProperty(stock);
            this.cantMin   = new SimpleStringProperty(cantMin);
            this.unidad    = new SimpleStringProperty(unidad  != null ? unidad  : "");
            this.fecha     = new SimpleStringProperty(fecha);
            this.proveedor = new SimpleStringProperty(proveedor);
            this.estado    = new SimpleStringProperty(estado);
        }
    }
}
