package com.example.demo1.Utils;

import javax.mail.*;
import javax.mail.internet.*;
import java.util.Properties;

/**
 * Utilidad para envío de correos desde el sistema Domino's Pizza.
 * Usa Gmail con autenticación de aplicación (App Password).
 */
public class CorreoUtil {

    // ── Configura aquí tu correo y contraseña de aplicación de Gmail ──────────
    private static final String CORREO_ORIGEN  = "jorgeluispolanco2009@gmail.com";
    private static final String APP_PASSWORD   = "vfrv qcvc bule hsne";
    private static final String CORREO_DESTINO = "jorgeluispolanco2009@gmail.com, jorgeluispolancotavares@gmail.com, haeming14@gmail.com, 8230011@ipisa.edu.do, 2230024@ipisa.edu.do";

    // ── Propiedades SMTP de Gmail ─────────────────────────────────────────────
    private static Session crearSesion() {
        Properties props = new Properties();
        props.put("mail.smtp.host",            "smtp.gmail.com");
        props.put("mail.smtp.port",            "587");
        props.put("mail.smtp.auth",            "true");
        props.put("mail.smtp.starttls.enable", "true");

        return Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(CORREO_ORIGEN, APP_PASSWORD);
            }
        });
    }

    /**
     * Envía un correo en un hilo separado para no bloquear la UI.
     */
    public static void enviar(String asunto, String cuerpo) {
        new Thread(() -> {
            try {
                Session session = crearSesion();
                Message msg = new MimeMessage(session);
                msg.setFrom(new InternetAddress(CORREO_ORIGEN));
                msg.setRecipients(Message.RecipientType.TO,
                        InternetAddress.parse(CORREO_DESTINO, false));
                msg.setSubject(asunto);
                msg.setContent(cuerpo, "text/html; charset=utf-8");
                Transport.send(msg);
                System.out.println("[CorreoUtil] Correo enviado: " + asunto);
            } catch (Exception e) {
                System.err.println("[CorreoUtil] Error al enviar correo: " + e.getMessage());
            }
        }, "hilo-correo").start();
    }

    // ── Plantillas de correo ──────────────────────────────────────────────────

    public static void notificarReclamacion(String cliente, String descripcion, String fecha) {
        String asunto = "⚠ Nueva Reclamación — Domino's Pizza";
        String cuerpo = """
                <div style="font-family:Arial,sans-serif;max-width:600px;margin:auto;
                            border:1px solid #ddd;border-radius:10px;overflow:hidden;">
                  <div style="background:#be1e1e;padding:20px;text-align:center;">
                    <h2 style="color:white;margin:0;">⚠ Nueva Reclamación Recibida</h2>
                  </div>
                  <div style="padding:24px;">
                    <p><b>Cliente:</b> %s</p>
                    <p><b>Fecha:</b> %s</p>
                    <p><b>Descripción:</b></p>
                    <div style="background:#fff3f3;border-left:4px solid #be1e1e;
                                padding:12px;border-radius:4px;">%s</div>
                    <br>
                    <p style="color:#666;font-size:12px;">
                      Este mensaje fue generado automáticamente por el sistema de Domino's Pizza.
                    </p>
                  </div>
                </div>
                """.formatted(cliente, fecha, descripcion);
        enviar(asunto, cuerpo);
    }

    public static void notificarFacturaGenerada(int idPedido, String cliente, String fecha) {
        String asunto = "🧾 Factura Generada — Pedido #" + idPedido;
        String cuerpo = """
                <div style="font-family:Arial,sans-serif;max-width:600px;margin:auto;
                            border:1px solid #ddd;border-radius:10px;overflow:hidden;">
                  <div style="background:#1a6b2e;padding:20px;text-align:center;">
                    <h2 style="color:white;margin:0;">🧾 Factura Generada</h2>
                  </div>
                  <div style="padding:24px;">
                    <p><b>N° de Pedido:</b> %d</p>
                    <p><b>Cliente:</b> %s</p>
                    <p><b>Fecha:</b> %s</p>
                    <div style="background:#f0fff4;border-left:4px solid #1a6b2e;
                                padding:12px;border-radius:4px;">
                      ✅ La factura fue generada y visualizada correctamente en el sistema.
                    </div>
                    <br>
                    <p style="color:#666;font-size:12px;">
                      Este mensaje fue generado automáticamente por el sistema de Domino's Pizza.
                    </p>
                  </div>
                </div>
                """.formatted(idPedido, cliente, fecha);
        enviar(asunto, cuerpo);
    }

    /**
     * Notifica el registro de un pago/abono a un proveedor.
     * Se envía al correo del sistema y, si existe, al correo del proveedor.
     */
    public static void notificarPagoProveedor(String numFactura,
                                               String proveedor,
                                               String emailProveedor,
                                               int    idCompra,
                                               java.math.BigDecimal montoTotal,
                                               java.math.BigDecimal abono,
                                               java.math.BigDecimal pendienteRestante,
                                               String estado,
                                               String fecha) {
        String asunto = "💳 Pago Registrado — Factura " + numFactura + " | " + proveedor;
        String estadoColor = "Pagado".equalsIgnoreCase(estado) ? "#1a6b2e" : "#e68a00";
        String cuerpo = """
                <div style="font-family:Arial,sans-serif;max-width:620px;margin:auto;
                            border:1px solid #ddd;border-radius:10px;overflow:hidden;">
                  <div style="background:#004aad;padding:20px;text-align:center;">
                    <h2 style="color:white;margin:0;">💳 Pago Registrado al Proveedor</h2>
                  </div>
                  <div style="padding:24px;">
                    <table style="width:100%%;border-collapse:collapse;">
                      <tr style="background:#f0f4ff;">
                        <td style="padding:10px;font-weight:bold;width:45%%;border-bottom:1px solid #e0e0e0;">N° Factura</td>
                        <td style="padding:10px;border-bottom:1px solid #e0e0e0;font-size:18px;
                                   font-weight:bold;color:#004aad;">%s</td>
                      </tr>
                      <tr>
                        <td style="padding:10px;font-weight:bold;border-bottom:1px solid #e0e0e0;">Proveedor</td>
                        <td style="padding:10px;border-bottom:1px solid #e0e0e0;">%s</td>
                      </tr>
                      <tr style="background:#f0f4ff;">
                        <td style="padding:10px;font-weight:bold;border-bottom:1px solid #e0e0e0;">N° Compra</td>
                        <td style="padding:10px;border-bottom:1px solid #e0e0e0;">#%d</td>
                      </tr>
                      <tr>
                        <td style="padding:10px;font-weight:bold;border-bottom:1px solid #e0e0e0;">Monto total compra</td>
                        <td style="padding:10px;border-bottom:1px solid #e0e0e0;">RD$ %s</td>
                      </tr>
                      <tr style="background:#f0f4ff;">
                        <td style="padding:10px;font-weight:bold;border-bottom:1px solid #e0e0e0;">Abono realizado</td>
                        <td style="padding:10px;border-bottom:1px solid #e0e0e0;
                                   font-weight:bold;color:#1a6b2e;">RD$ %s</td>
                      </tr>
                      <tr>
                        <td style="padding:10px;font-weight:bold;border-bottom:1px solid #e0e0e0;">Pendiente restante</td>
                        <td style="padding:10px;border-bottom:1px solid #e0e0e0;
                                   font-weight:bold;color:#be1e1e;">RD$ %s</td>
                      </tr>
                      <tr style="background:#f0f4ff;">
                        <td style="padding:10px;font-weight:bold;">Estado</td>
                        <td style="padding:10px;font-weight:bold;color:%s;">%s</td>
                      </tr>
                    </table>
                    <br>
                    <p style="color:#555;font-size:12px;">Fecha del pago: <b>%s</b></p>
                    <div style="background:#f0fff4;border-left:4px solid #1a6b2e;
                                padding:12px;border-radius:4px;margin-top:8px;">
                      ✅ Este pago fue registrado en el sistema Domino's Pizza.
                    </div>
                    <br>
                    <p style="color:#888;font-size:11px;">
                      Mensaje generado automáticamente — Sistema Domino's Pizza.
                    </p>
                  </div>
                </div>
                """.formatted(numFactura, proveedor, idCompra,
                              montoTotal.toPlainString(), abono.toPlainString(),
                              pendienteRestante.toPlainString(),
                              estadoColor, estado, fecha);

        // Enviar al sistema
        enviar(asunto, cuerpo);

        // Enviar también al correo del proveedor si tiene uno registrado
        if (emailProveedor != null && !emailProveedor.isBlank()) {
            new Thread(() -> {
                try {
                    javax.mail.Session session = crearSesion();
                    javax.mail.Message msg = new javax.mail.internet.MimeMessage(session);
                    msg.setFrom(new javax.mail.internet.InternetAddress(CORREO_ORIGEN));
                    msg.setRecipients(javax.mail.Message.RecipientType.TO,
                            javax.mail.internet.InternetAddress.parse(emailProveedor, false));
                    msg.setSubject(asunto);
                    msg.setContent(cuerpo, "text/html; charset=utf-8");
                    javax.mail.Transport.send(msg);
                    System.out.println("[CorreoUtil] Correo proveedor enviado: " + emailProveedor);
                } catch (Exception e) {
                    System.err.println("[CorreoUtil] Error correo proveedor: " + e.getMessage());
                }
            }, "hilo-correo-prov").start();
        }
    }

    public static void notificarStockBajo(String ingrediente, int stockActual, int minimo) {
        String asunto = "🔴 Stock Crítico — " + ingrediente;
        String cuerpo = """
                <div style="font-family:Arial,sans-serif;max-width:600px;margin:auto;
                            border:1px solid #ddd;border-radius:10px;overflow:hidden;">
                  <div style="background:#004aad;padding:20px;text-align:center;">
                    <h2 style="color:white;margin:0;">🔴 Alerta de Stock Bajo</h2>
                  </div>
                  <div style="padding:24px;">
                    <p><b>Ingrediente:</b> %s</p>
                    <p><b>Stock actual:</b>
                      <span style="color:#be1e1e;font-weight:bold;font-size:18px;">%d</span>
                    </p>
                    <p><b>Mínimo requerido:</b> %d</p>
                    <div style="background:#fff3cd;border-left:4px solid #f0a500;
                                padding:12px;border-radius:4px;">
                      ⚠ Se recomienda realizar una compra a proveedores lo antes posible.
                    </div>
                    <br>
                    <p style="color:#666;font-size:12px;">
                      Este mensaje fue generado automáticamente por el sistema de Domino's Pizza.
                    </p>
                  </div>
                </div>
                """.formatted(ingrediente, stockActual, minimo);
        enviar(asunto, cuerpo);
    }
}
