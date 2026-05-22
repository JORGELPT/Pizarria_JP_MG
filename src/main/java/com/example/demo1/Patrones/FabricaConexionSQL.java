package com.example.demo1.Patrones;

import com.example.demo1.Database.Conexion;
import java.sql.Connection;

public class FabricaConexionSQL extends FabricaConexion {

    @Override
    public Connection crearConexion() {
        return Conexion.establecerConexion();
    }
}
