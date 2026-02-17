module org.example.agendabd {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires org.mariadb.jdbc;

    opens UI to javafx.fxml;
    opens domain to javafx.fxml;

    exports UI;
    exports domain;
    exports servicio;
    exports repositorio;
    exports database;
}
