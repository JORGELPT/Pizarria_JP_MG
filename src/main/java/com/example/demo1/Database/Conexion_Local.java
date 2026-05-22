package com.example.demo1.Database;

import javax.swing.*;
import java.sql.*;

public class Conexion_Local {

    // ── Datos de conexión ──────────────────────────────────────────────────
    private static final String db     = "dominospizza_RA5";
    private static final String server = "LAPTOP-UMA7JLDA";
    private static final String puerto = "1433";

    // ── Establece y retorna una nueva conexión por cada llamada ───────────
    public static Connection establecerConexion() {
        try {
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
            String cadena = "jdbc:sqlserver://" + server + ":" + puerto + ";"
                    + "databaseName="        + db + ";"
                    + "integratedSecurity=true;"
                    + "encrypt=true;"
                    + "trustServerCertificate=true";
            return DriverManager.getConnection(cadena);
        } catch (Exception e) {
            System.out.println(e.toString());
            JOptionPane.showMessageDialog(null, "Error en la conexión local: " + e.toString());
            return null;
        }
    }

    public void leerDato(int i) {
        // placeholder — implementar según necesidad
    }
}
