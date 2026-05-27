package com.example.demo1.Patrones;

import com.example.demo1.Database.Conexion_Local;
import java.sql.Connection;

public class FabricaConexionLocal extends FabricaConexion {

    @Override
    public Connection crearConexion() {
        return Conexion_Local.establecerConexion();
    }
}
