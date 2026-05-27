package com.example.demo1.Patrones;

public class GestorConfiguracion {

    private static GestorConfiguracion instancia;

    private String servidor;
    private String puerto;
    private String baseDatos;
    private String usuario;
    private String contrasena;

    private GestorConfiguracion() {
        this.servidor   = "LAPTOP-UMA7JLDA";
        this.puerto     = "1433";
        this.baseDatos  = "dominospizza_RA5";
        this.usuario    = "jorge_local";
        this.contrasena = "Dominos26!";
    }

    public static GestorConfiguracion getInstancia() {
        if (instancia == null) {
            instancia = new GestorConfiguracion();
        }
        return instancia;
    }

    public String getServidor()  { return servidor; }
    public String getPuerto()    { return puerto; }
    public String getBaseDatos() { return baseDatos; }
    public String getUsuario()   { return usuario; }
    public String getContrasena(){ return contrasena; }

    public void setServidor(String servidor)   { this.servidor = servidor; }
    public void setPuerto(String puerto)       { this.puerto = puerto; }
    public void setBaseDatos(String baseDatos) { this.baseDatos = baseDatos; }
    public void setUsuario(String usuario)     { this.usuario = usuario; }
    public void setContrasena(String contrasena) { this.contrasena = contrasena; }

    public String getUrlConexion() {
        return "jdbc:sqlserver://" + servidor + ":" + puerto
                + ";databaseName=" + baseDatos
                + ";encrypt=true;trustServerCertificate=true";
    }
}
