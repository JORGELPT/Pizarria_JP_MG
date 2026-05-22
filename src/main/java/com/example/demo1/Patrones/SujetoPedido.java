package com.example.demo1.Patrones;

public class SujetoPedido extends Sujeto {

    public void pedidoCreado(int idPedido, String cliente, double total) {
        String evento = "pedido_creado";
        String datos  = "Pedido #" + idPedido + " | Cliente: " + cliente + " | Total: RD$" + total;
        System.out.println("[SujetoPedido] Pedido creado — notificando observadores...");
        notificarObservadores(evento, datos);
    }

    public void pedidoCancelado(int idPedido) {
        String evento = "pedido_cancelado";
        String datos  = "Pedido #" + idPedido + " ha sido cancelado.";
        System.out.println("[SujetoPedido] Pedido cancelado — notificando observadores...");
        notificarObservadores(evento, datos);
    }
}
