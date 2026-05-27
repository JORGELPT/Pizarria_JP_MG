package com.example.demo1.Controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import java.io.IOException;
import java.net.URL;

public class CONTROLLER_inicio {

    private static final String RUTA_PANTALLAS = "/com/example/demo1/Pantallas/";

    @FXML
    public void initialize() {}

    // Llamado desde onAction del botón "Ir →"
    @FXML
    private void abrirRegistrarPedido(ActionEvent event) {
        cargarEnContentArea((Node) event.getSource(), "Hacer_Un_Pedido.fxml", "Hacer un Pedido");
    }

    @FXML
    private void abrirReclamacion(ActionEvent event) {
        cargarEnContentArea((Node) event.getSource(), "Reclamacion.fxml", "Reclamación de Pedido");
    }

    @FXML
    private void abrirRegistrarCliente(ActionEvent event) {
        cargarEnContentArea((Node) event.getSource(), "Agregar_Cliente.fxml", "Registrar Cliente");
    }

    // Llamado desde onMouseClicked del VBox (clic en cualquier parte de la tarjeta)
    @FXML
    private void abrirRegistrarPedidoClick(MouseEvent event) {
        cargarEnContentArea((Node) event.getSource(), "Hacer_Un_Pedido.fxml", "Hacer un Pedido");
    }

    @FXML
    private void abrirReclamacionClick(MouseEvent event) {
        cargarEnContentArea((Node) event.getSource(), "Reclamacion.fxml", "Reclamación de Pedido");
    }

    @FXML
    private void abrirRegistrarClienteClick(MouseEvent event) {
        cargarEnContentArea((Node) event.getSource(), "Agregar_Cliente.fxml", "Registrar Cliente");
    }

    private void cargarEnContentArea(Node origen, String fxmlFile, String titulo) {
        try {
            StackPane contentArea = (StackPane) origen.getScene().lookup("#contentArea");
            if (contentArea == null) {
                mostrarAviso("No se pudo localizar el área de contenido.");
                return;
            }

            URL url = getClass().getResource(RUTA_PANTALLAS + fxmlFile);
            if (url == null) {
                mostrarAviso("La pantalla '" + titulo + "' aún no está implementada.");
                return;
            }

            FXMLLoader loader = new FXMLLoader(url);
            Node vista = loader.load();
            contentArea.getChildren().setAll(vista);

        } catch (Exception e) {
            e.printStackTrace();
            mostrarAviso("Error al abrir '" + titulo + "'.");
        }
    }

    private void mostrarAviso(String mensaje) {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle("Aviso");
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
}
