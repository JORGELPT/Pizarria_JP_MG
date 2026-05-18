package com.example.demo1.Utils;

/**
 * Utilidad de notificaciones del sistema Domino's Pizza.
 * Los eventos se registran en consola.
 */
public class CorreoUtil {

    private CorreoUtil() {}

    public static void notificarReclamacion(String cliente, String descripcion, String fecha) {
        log("⚠ Nueva Reclamación — Cliente: " + cliente
                + " | Fecha: " + fecha + " | " + descripcion);
    }

    public static void notificarFacturaGenerada(int idPedido, String cliente, String fecha) {
        log("🧾 Factura Generada — Pedido #" + idPedido
                + " | Cliente: " + cliente + " | Fecha: " + fecha);
    }

    public static void notificarStockBajo(String ingrediente, int stockActual, int minimo) {
        log("🔴 Stock Crítico — " + ingrediente
                + " | Stock actual: " + stockActual + " | Mínimo: " + minimo);
    }

    private static void log(String mensaje) {
        System.out.println("[CorreoUtil] " + mensaje);
    }
}
