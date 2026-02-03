module org.example.agendabd {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires org.mariadb.jdbc;


    opens org.example.agendabd to javafx.fxml;
    exports org.example.agendabd;
}