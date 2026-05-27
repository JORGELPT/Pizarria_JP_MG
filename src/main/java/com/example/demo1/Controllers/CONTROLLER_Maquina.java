package com.example.demo1.Controllers;

import com.example.demo1.Database.Conexion;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import javax.swing.JOptionPane;
import java.sql.*;

public class CONTROLLER_Maquina {

    Conexion conexion = new Conexion();

    @FXML private TextField TXTnombre;
    @FXML private TextField TXTMarca;
    @FXML private TextField TXTModelo;
    @FXML private TextField TXTfechaAdquisicion;
    @FXML private TextField TXTserie;

    @FXML private RadioButton RDoperativa;
    @FXML private RadioButton RDfueradeservicio;
    @FXML private RadioButton RDenreparacion;
    @FXML private RadioButton RDaveriada;
    private ToggleGroup grupoEstado;

    @FXML private RadioButton rbGarantiaSi;
    @FXML private RadioButton rbGarantiaNo;
    private ToggleGroup grupoGarantia;

    @FXML private TableView<MaquinaRow> tablaMaquinas;
    @FXML private TableColumn<MaquinaRow, String> colNombre;
    @FXML private TableColumn<MaquinaRow, String> colMarca;
    @FXML private TableColumn<MaquinaRow, String> colEstado;

    // lugar_compra: FK a tbl_proveedor. Valor por defecto (ajustar según BD).
    private static final int ID_LUGAR_COMPRA_FIJO = 1;

    @FXML
    public void initialize() {
        grupoEstado = new ToggleGroup();
        RDoperativa.setToggleGroup(grupoEstado);
        RDfueradeservicio.setToggleGroup(grupoEstado);
        RDenreparacion.setToggleGroup(grupoEstado);
        RDaveriada.setToggleGroup(grupoEstado);

        grupoGarantia = new ToggleGroup();
        rbGarantiaSi.setToggleGroup(grupoGarantia);
        rbGarantiaNo.setToggleGroup(grupoGarantia);

        colNombre.setCellValueFactory(c -> c.getValue().nombre);
        colMarca.setCellValueFactory(c -> c.getValue().marca);
        colEstado.setCellValueFactory(c -> c.getValue().estado);

        tablaMaquinas.getSelectionModel().selectedItemProperty().addListener(
                (obs, old, sel) -> {
                    if (sel == null) return;
                    TXTnombre.setText(sel.nombre.get());
                    TXTMarca.setText(sel.marca.get());
                    String est = sel.estado.get();
                    grupoEstado.getToggles().stream()
                            .filter(t -> ((RadioButton) t).getText().equals(est))
                            .findFirst().ifPresent(grupoEstado::selectToggle);
                });

        cargarTabla();
    }

    @FXML
    public void FnGuardar() {
        String nombre = TXTnombre.getText().trim();
        String marca  = TXTMarca.getText().trim();
        String modelo = TXTModelo.getText().trim();
        String fecha  = TXTfechaAdquisicion.getText().trim();
        String serie  = TXTserie.getText().trim();

        String estado = "";
        if (grupoEstado.getSelectedToggle() != null)
            estado = ((RadioButton) grupoEstado.getSelectedToggle()).getText();

        int garantia = rbGarantiaSi.isSelected() ? 1 : 0;

        if (nombre.isEmpty() || marca.isEmpty() || modelo.isEmpty() || estado.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Nombre, marca, modelo y estado son obligatorios.");
            return;
        }

        // Columna correcta: lugar_compra (no id_lugar_compra)
        String sql = "INSERT INTO tbl_maquina " +
                "(nombre_maquina, marca_maquina, modelo_maquina, estado_maquina, fecha_adquisicion, serie, garantia, lugar_compra) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection con = conexion.establecerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, nombre);
            ps.setString(2, marca);
            ps.setString(3, modelo);
            ps.setString(4, estado);
            ps.setDate(5, fecha.isEmpty() ? null : Date.valueOf(fecha));
            ps.setString(6, serie);
            ps.setInt(7, garantia);
            ps.setInt(8, ID_LUGAR_COMPRA_FIJO);

            ps.executeUpdate();
            JOptionPane.showMessageDialog(null, "Máquina guardada correctamente.");
            limpiar();
            cargarTabla();

        } catch (IllegalArgumentException e) { //error de formato de fecha al guardar máquina - pantalla Agregar Máquina
            JOptionPane.showMessageDialog(null, "Formato de fecha inválido. Use YYYY-MM-DD");
        } catch (SQLException e) { //error al guardar máquina - pantalla Agregar Máquina
            JOptionPane.showMessageDialog(null, "Error al guardar: " + e.getMessage());
        }
    }

    @FXML
    public void FnBuscar() {
        String nombre = TXTnombre.getText().trim();
        if (nombre.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Ingrese un nombre para buscar.");
            return;
        }

        String sql = "SELECT nombre_maquina, marca_maquina, modelo_maquina, estado_maquina, " +
                "fecha_adquisicion, serie, garantia FROM tbl_maquina WHERE nombre_maquina = ?";

        try (Connection con = conexion.establecerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, nombre);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                TXTnombre.setText(rs.getString("nombre_maquina"));
                TXTMarca.setText(rs.getString("marca_maquina"));
                TXTModelo.setText(rs.getString("modelo_maquina"));
                TXTserie.setText(rs.getString("serie"));
                Date f = rs.getDate("fecha_adquisicion");
                TXTfechaAdquisicion.setText(f != null ? f.toString() : "");

                String estadoVal = rs.getString("estado_maquina");
                if (estadoVal != null) {
                    grupoEstado.getToggles().stream()
                            .filter(t -> ((RadioButton) t).getText().equals(estadoVal))
                            .findFirst().ifPresent(grupoEstado::selectToggle);
                }
                int gar = rs.getInt("garantia");
                if (gar == 1) rbGarantiaSi.setSelected(true);
                else rbGarantiaNo.setSelected(true);

            } else {
                JOptionPane.showMessageDialog(null, "No se encontró la máquina.");
            }
        } catch (Exception e) { //error al buscar máquina - pantalla Agregar Máquina
            JOptionPane.showMessageDialog(null, "Error: " + e.getMessage());
        }
    }

    @FXML
    public void FnEditar() {
        String nombre = TXTnombre.getText().trim();
        if (nombre.isEmpty()) {
            JOptionPane.showMessageDialog(null, "El nombre es obligatorio para editar.");
            return;
        }

        String estado = grupoEstado.getSelectedToggle() != null
                ? ((RadioButton) grupoEstado.getSelectedToggle()).getText() : "";
        int garantia = rbGarantiaSi.isSelected() ? 1 : 0;

        String sql = "UPDATE tbl_maquina SET marca_maquina=?, modelo_maquina=?, estado_maquina=?, " +
                "fecha_adquisicion=?, serie=?, garantia=? WHERE nombre_maquina=?";

        try (Connection con = conexion.establecerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, TXTMarca.getText().trim());
            ps.setString(2, TXTModelo.getText().trim());
            ps.setString(3, estado);
            String f = TXTfechaAdquisicion.getText().trim();
            ps.setDate(4, f.isEmpty() ? null : Date.valueOf(f));
            ps.setString(5, TXTserie.getText().trim());
            ps.setInt(6, garantia);
            ps.setString(7, nombre);

            if (ps.executeUpdate() > 0) {
                JOptionPane.showMessageDialog(null, "Máquina actualizada.");
                cargarTabla();
            } else {
                JOptionPane.showMessageDialog(null, "No se encontró la máquina.");
            }
        } catch (Exception e) { //error al editar máquina - pantalla Agregar Máquina
            JOptionPane.showMessageDialog(null, "Error: " + e.getMessage());
        }
    }

    @FXML
    public void FnEliminar() {
        if (!com.example.demo1.Utils.Permisos_Util.verificarEliminar()) return;
        String nombre = TXTnombre.getText().trim();
        if (nombre.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Ingrese el nombre a eliminar.");
            return;
        }
        int c = JOptionPane.showConfirmDialog(null, "¿Eliminar '" + nombre + "'?",
                "Confirmar", JOptionPane.YES_NO_OPTION);
        if (c != JOptionPane.YES_OPTION) return;

        try (Connection con = conexion.establecerConexion();
             PreparedStatement ps = con.prepareStatement("DELETE FROM tbl_maquina WHERE nombre_maquina = ?")) {
            ps.setString(1, nombre);
            if (ps.executeUpdate() > 0) {
                JOptionPane.showMessageDialog(null, "Máquina eliminada.");
                limpiar();
                cargarTabla();
            } else {
                JOptionPane.showMessageDialog(null, "No se encontró la máquina.");
            }
        } catch (Exception e) { //error al eliminar máquina - pantalla Agregar Máquina
            JOptionPane.showMessageDialog(null, "Error: " + e.getMessage());
        }
    }

    @FXML
    public void FnInhabilitar() {
        String nombre = TXTnombre.getText().trim();
        if (nombre.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Ingrese el nombre de la máquina a inhabilitar.");
            return;
        }
        int confirmar = JOptionPane.showConfirmDialog(null,
                "¿Inhabilitar '" + nombre + "'? No se eliminará, solo quedará inactiva.",
                "Confirmar", JOptionPane.YES_NO_OPTION);
        if (confirmar != JOptionPane.YES_OPTION) return;
        try (java.sql.Connection con = Conexion.establecerConexion();
             java.sql.PreparedStatement ps = con.prepareStatement(
                     "UPDATE tbl_maquina SET estado = 'Inactivo' WHERE nombre_maquina = ?")) {
            ps.setString(1, nombre);
            ps.executeUpdate();
            JOptionPane.showMessageDialog(null, "Máquina inhabilitada correctamente.");
            limpiar();
            cargarTabla();
        } catch (Exception e) { //error al inhabilitar máquina - pantalla Agregar Máquina
            JOptionPane.showMessageDialog(null, "Error al inhabilitar: " + e.getMessage());
        }
    }

    @FXML
    public void FnHabilitar() {
        String nombre = TXTnombre.getText().trim();
        if (nombre.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Ingrese el nombre de la máquina a habilitar.");
            return;
        }
        try (java.sql.Connection con = Conexion.establecerConexion();
             java.sql.PreparedStatement ps = con.prepareStatement(
                     "UPDATE tbl_maquina SET estado = 'Activo' WHERE nombre_maquina = ?")) {
            ps.setString(1, nombre);
            ps.executeUpdate();
            JOptionPane.showMessageDialog(null, "Máquina habilitada correctamente.");
            cargarTabla();
        } catch (Exception e) { //error al habilitar máquina - pantalla Agregar Máquina
            JOptionPane.showMessageDialog(null, "Error al habilitar: " + e.getMessage());
        }
    }

    private void cargarTabla() {
        ObservableList<MaquinaRow> datos = FXCollections.observableArrayList();
        String sql = "SELECT nombre_maquina, marca_maquina, estado_maquina FROM tbl_maquina ORDER BY nombre_maquina";

        try (Connection con = conexion.establecerConexion();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                datos.add(new MaquinaRow(
                        rs.getString("nombre_maquina"),
                        rs.getString("marca_maquina"),
                        rs.getString("estado_maquina")));
            }
            tablaMaquinas.setItems(datos);
        } catch (Exception e) { //error al cargar tabla de máquinas - pantalla Agregar Máquina
            JOptionPane.showMessageDialog(null, "Error al cargar máquinas: " + e.getMessage());
        }
    }

    public void limpiar() {
        TXTnombre.clear(); TXTMarca.clear(); TXTModelo.clear();
        TXTfechaAdquisicion.clear(); TXTserie.clear();
        grupoEstado.selectToggle(null);
        grupoGarantia.selectToggle(null);
    }

    public static class MaquinaRow {
        final SimpleStringProperty nombre, marca, estado;
        public MaquinaRow(String n, String m, String e) {
            nombre = new SimpleStringProperty(n);
            marca  = new SimpleStringProperty(m);
            estado = new SimpleStringProperty(e);
        }
        public String getNombre() { return nombre.get(); }
        public String getMarca()  { return marca.get(); }
        public String getEstado() { return estado.get(); }
    }
}
