package com.example.demo1.Patrones;

import java.sql.Connection;

public abstract class FabricaConexion {

    public abstract Connection crearConexion();

    protected Connection obtenerConexionBase(String url, String usuario, String contrasena)
            throws java.sql.SQLException {
        return java.sql.DriverManager.getConnection(url, usuario, contrasena);
    }
}
