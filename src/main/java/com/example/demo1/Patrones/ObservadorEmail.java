package com.example.demo1.Patrones;

public class ObservadorEmail implements Observador {

    @Override
    public void actualizar(String evento, Object datos) {
        if ("pedido_creado".equals(evento)) {
            System.out.println("[ObservadorEmail] Enviando correo de confirmación...");
            System.out.println("[ObservadorEmail] Pedido confirmado: " + datos);
            System.out.println("[ObservadorEmail] Correo enviado al cliente.");
        } else if ("pedido_cancelado".equals(evento)) {
            System.out.println("[ObservadorEmail] Enviando correo de cancelación...");
            System.out.println("[ObservadorEmail] Cancelación notificada: " + datos);
        }
    }
}
