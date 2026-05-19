package com.example.demo1.Utils;

public class CorreoUtil {

    public static void enviar(String asunto, String cuerpo) {
        System.out.println("[CorreoUtil] Asunto : " + asunto);
        System.out.println("[CorreoUtil] Cuerpo  : " + cuerpo);
    }

    public static void notificarReclamacion(String cliente, String descripcion, String fecha) {
        enviar("Nueva Reclamación — " + cliente,
                "Fecha: " + fecha + "\n" + descripcion);
    }

    public static void notificarFacturaGenerada(int idPedido, String cliente, String fecha) {
        enviar("Factura Generada — Pedido #" + idPedido,
                "Cliente: " + cliente + " | Fecha: " + fecha);
    }

    public static void notificarStockBajo(String ingrediente, int stockActual, int minimo) {
        enviar("Stock Crítico — " + ingrediente,
                "Stock actual: " + stockActual + " | Mínimo: " + minimo);
    }
}
