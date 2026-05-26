module com.example.demo1 {

    // JavaFX
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;

    // SQL
    requires java.sql;

    // JOptionPane (javax.swing)
    requires java.desktop;

    // java.naming y java.xml los usa JasperReports internamente
    requires java.naming;
    requires java.xml;

    // JasperReports y JFreeChart NO son módulos JPMS — van al classpath (unnamed module).
    // El acceso se habilita con --add-reads en el compilador y en el runtime.

    requires jasperreports;
    requires jfreechart;

    // Abre los paquetes al módulo de FXML y de JavaFX
    opens com.example.demo1             to javafx.fxml, javafx.graphics;
    opens com.example.demo1.Controllers to javafx.fxml;
    opens com.example.demo1.app         to javafx.fxml, javafx.graphics;

    // Recursos (imágenes, FXML, estilos) — necesario para que JavaFX los lea
    opens com.example.demo1.imagenes    to javafx.fxml, javafx.graphics;
    opens com.example.demo1.Pantallas   to javafx.fxml, javafx.graphics;
    opens com.example.demo1.styles      to javafx.fxml, javafx.graphics;

    // Exporta los paquetes principales
    exports com.example.demo1;
    exports com.example.demo1.app;

    opens com.example.demo1.Utils to javafx.fxml;
}
