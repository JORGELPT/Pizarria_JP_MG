package com.example.demo1.Patrones;

public class ObservadorInventario implements Observador {

    @Override
    public void actualizar(String evento, Object datos) {
        if ("pedido_creado".equals(evento)) {
            System.out.println("[ObservadorInventario] Actualizando inventario por: " + datos);
            System.out.println("[ObservadorInventario] Stock reducido correctamente.");
        } else if ("pedido_cancelado".equals(evento)) {
            System.out.println("[ObservadorInventario] Restaurando inventario por: " + datos);
        }
    }
}
