package com.example.demo1.Controllers;

import com.example.demo1.Database.Conexion;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.StackPane;

import java.io.IOException;
import java.net.URL;
import java.sql.*;

public class Controller_Inventario {

    Conexion conexion = new Conexion();

    @FXML private TextField TXTbuscar;
    @FXML private TableView<IngredienteRow> tablaInventario;
    @FXML private TableColumn<IngredienteRow, String> colId;
    @FXML private TableColumn<IngredienteRow, String> colNombre;
    @FXML private TableColumn<IngredienteRow, String> colCategoria;
    @FXML private TableColumn<IngredienteRow, String> colFechaExp;
    @FXML private TableColumn<IngredienteRow, String> colCantStock;
    @FXML private TableColumn<IngredienteRow, String> colCantMin;

    private static final String RUTA_PANTALLAS = "/com/example/demo1/Pantallas/";

    @FXML
    public void initialize() {
        colId.setCellValueFactory(c -> c.getValue().id);
        colNombre.setCellValueFactory(c -> c.getValue().nombre);
        colCategoria.setCellValueFactory(c -> c.getValue().categoria);
        colFechaExp.setCellValueFactory(c -> c.getValue().fechaExp);
        colCantStock.setCellValueFactory(c -> c.getValue().cantStock);
        colCantMin.setCellValueFactory(c -> c.getValue().cantMin);

        cargarInventario(null);
    }

    private void cargarInventario(String filtro) {
        ObservableList<IngredienteRow> datos = FXCollections.observableArrayList();

        String sql = "SELECT id_ingrediente, nombre, tipo_ingrediente, fecha_caducidad, " +
                "cantidad_stock, cantidad_minima_stock " +
                "FROM tbl_ingrediente ";

        if (filtro != null && !filtro.isEmpty()) {
            sql += "WHERE nombre LIKE ? ";
        }
        sql += "ORDER BY nombre";

        try (Connection con = conexion.establecerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {

            if (filtro != null && !filtro.isEmpty()) {
                ps.setString(1, "%" + filtro + "%");
            }

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Date fecha = rs.getDate("fecha_caducidad");
                datos.add(new IngredienteRow(
                        String.valueOf(rs.getInt("id_ingrediente")),
                        rs.getString("nombre"),
                        rs.getString("tipo_ingrediente"),
                        fecha != null ? fecha.toString() : "",
                        String.valueOf(rs.getInt("cantidad_stock")),
                        String.valueOf(rs.getInt("cantidad_minima_stock"))
                ));
            }
            tablaInventario.setItems(datos);

        } catch (Exception e) {
            mostrarAviso("Error al cargar inventario: " + e.getMessage());
        }
    }

    @FXML
    private void FnAgregarIngrediente() {
        try {
            StackPane contentArea = buscarContentArea(tablaInventario);
            if (contentArea == null) {
                mostrarAviso("No se pudo localizar el área de contenido del sistema.");
                return;
            }
            URL url = getClass().getResource(RUTA_PANTALLAS + "Agregar_Ingrediente.fxml");
            if (url == null) {
                mostrarAviso("No se encontró la pantalla Agregar_Ingrediente.fxml");
                return;
            }
            FXMLLoader loader = new FXMLLoader(url);
            Node vista = loader.load();
            contentArea.getChildren().setAll(vista);
        } catch (IOException e) {
            mostrarAviso("Error al abrir la pantalla: " + e.getMessage());
        }
    }

    private StackPane buscarContentArea(Node desde) {
        Node nodo = desde;
        while (nodo != null) {
            if (nodo instanceof StackPane && "contentArea".equals(nodo.getId())) {
                return (StackPane) nodo;
            }
            nodo = nodo.getParent();
        }
        return null;
    }

    @FXML
    private void FnBuscar() {
        cargarInventario(TXTbuscar.getText().trim());
    }

    private void mostrarAviso(String mensaje) {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle("Inventario");
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    public static class IngredienteRow {
        final SimpleStringProperty id, nombre, categoria, fechaExp, cantStock, cantMin;

        public IngredienteRow(String id, String nombre, String categoria,
                              String fechaExp, String cantStock, String cantMin) {
            this.id        = new SimpleStringProperty(id);
            this.nombre    = new SimpleStringProperty(nombre);
            this.categoria = new SimpleStringProperty(categoria);
            this.fechaExp  = new SimpleStringProperty(fechaExp);
            this.cantStock = new SimpleStringProperty(cantStock);
            this.cantMin   = new SimpleStringProperty(cantMin);
        }

        public String getId()        { return id.get(); }
        public String getNombre()    { return nombre.get(); }
        public String getCategoria() { return categoria.get(); }
        public String getFechaExp()  { return fechaExp.get(); }
        public String getCantStock() { return cantStock.get(); }
        public String getCantMin()   { return cantMin.get(); }
    }
}
