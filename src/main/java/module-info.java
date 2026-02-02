module org.example.agendabd {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;


    opens org.example.agendabd to javafx.fxml;
    exports org.example.agendabd;
}