package com.example.demo1.Utils;

import javax.swing.JOptionPane;

/**
 * Helper centralizado para validar permisos antes de realizar acciones.
 * Todos los controllers deben llamar a estos métodos antes de guardar/editar/eliminar.
 *
 * Depende de CONTROLLER_Seccion para leer el rol activo.
 * Los permisos se resuelven según tbl_persona.rol_bd:
 *
 *   puedeInsertar()      → admin, gerente, cajero, cliente
 *   puedeEditar()        → admin, gerente
 *   puedeEliminar()      → admin, gerente
 *   puedeBuscar()        → todos (sesión activa)
 *   puedeCrearUsuarios() → solo admin
 */
public class Permisos_Util {

    private Permisos_Util() {} // No instanciable

    /**
     * Verifica si el usuario puede INSERTAR.
     * Retorna true si puede, false y muestra mensaje si no puede.
     */
    public static boolean verificarInsertar() {
        if (!CONTROLLER_Seccion.getInstancia().puedeInsertar()) {
            JOptionPane.showMessageDialog(null,
                    "Tu rol (" + CONTROLLER_Seccion.getInstancia().getRol() + ") no tiene permisos para insertar.",
                    "Acceso denegado", JOptionPane.WARNING_MESSAGE);
            return false;
        }
        return true;
    }

    /**
     * Verifica si el usuario puede EDITAR.
     * Solo ADMIN y GERENTE.
     */
    public static boolean verificarEditar() {
        if (!CONTROLLER_Seccion.getInstancia().puedeEditar()) {
            JOptionPane.showMessageDialog(null,
                    "Tu rol (" + CONTROLLER_Seccion.getInstancia().getRol() + ") no tiene permisos para editar.\n" +
                    "Solo los roles ADMIN y GERENTE pueden hacerlo.",
                    "Acceso denegado", JOptionPane.WARNING_MESSAGE);
            return false;
        }
        return true;
    }

    /**
     * Verifica si el usuario puede ELIMINAR.
     * Solo ADMIN y GERENTE.
     */
    public static boolean verificarEliminar() {
        if (!CONTROLLER_Seccion.getInstancia().puedeEliminar()) {
            JOptionPane.showMessageDialog(null,
                    "Tu rol (" + CONTROLLER_Seccion.getInstancia().getRol() + ") no tiene permisos para eliminar.\n" +
                    "Solo los roles ADMIN y GERENTE pueden hacerlo.",
                    "Acceso denegado", JOptionPane.WARNING_MESSAGE);
            return false;
        }
        return true;
    }

    /**
     * Verifica si el usuario puede BUSCAR.
     */
    public static boolean verificarBuscar() {
        if (!CONTROLLER_Seccion.getInstancia().puedeBuscar()) {
            JOptionPane.showMessageDialog(null,
                    "Tu rol (" + CONTROLLER_Seccion.getInstancia().getRol() + ") no tiene permisos para buscar.",
                    "Acceso denegado", JOptionPane.WARNING_MESSAGE);
            return false;
        }
        return true;
    }

    /**
     * Solo ADMIN y GERENTE pueden generar reportes/exportar PDF.
     */
    public static boolean verificarReporte() {
        CONTROLLER_Seccion s = CONTROLLER_Seccion.getInstancia();
        if (!s.esAdmin() && !s.esGerente()) {
            JOptionPane.showMessageDialog(null,
                    "Solo los roles ADMIN y GERENTE pueden generar reportes.",
                    "Acceso denegado", JOptionPane.WARNING_MESSAGE);
            return false;
        }
        return true;
    }

    /**
     * Solo ADMIN puede crear usuarios/empleados.
     */
    public static boolean verificarCrearUsuario() {
        if (!CONTROLLER_Seccion.getInstancia().puedeCrearUsuarios()) {
            JOptionPane.showMessageDialog(null,
                    "Solo el rol ADMIN puede crear usuarios.",
                    "Acceso denegado", JOptionPane.WARNING_MESSAGE);
            return false;
        }
        return true;
    }
}
