package org.example.agendabd;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import org.mariadb.jdbc.Connection;

import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class Controller {
    // Configuracion DB
    private static final String URL = "jdbc:mariadb://localhost:3306/agenda";
    private static final String USER = "usuario1";
    private static final String PASSWORD = "superpassword";
    // Elementos UI Personas
    @FXML private TableView<PersonaFila> tvPersonas;
    @FXML private TableColumn<PersonaFila, Integer> colPersonaId;
    @FXML private TableColumn<PersonaFila, String> colPersonaNombre;
    @FXML private TableColumn<PersonaFila, String> colPersonaDireccion;
    // Elementos UI Telefonos
    @FXML private TableView<TelefonoFila> tvTelefonos;
    @FXML private TableColumn<TelefonoFila, Integer> colTelId;
    @FXML private TableColumn<TelefonoFila, Integer> colTelPersonaId;
    @FXML private TableColumn<TelefonoFila, String> colTelNumero;

    private final ObservableList<PersonaFila> personas = FXCollections.observableArrayList();
    private final ObservableList<TelefonoFila> telefonos = FXCollections.observableArrayList();

    @FXML
    public void initialize() {

        // Relación columnas y getters tabla Personas
        colPersonaId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colPersonaNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colPersonaDireccion.setCellValueFactory(new PropertyValueFactory<>("direccion"));

        // Relación columnas y getters tabla Telefonos
        colTelId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colTelPersonaId.setCellValueFactory(new PropertyValueFactory<>("personaId"));
        colTelNumero.setCellValueFactory(new PropertyValueFactory<>("telefono"));

        // Set items
        tvPersonas.setItems(personas);
        tvTelefonos.setItems(telefonos);
        cargarPersonas();
        cargarTelefonos();
    }

    private Connection conectar() throws Exception {
        return (Connection) DriverManager.getConnection(URL, USER, PASSWORD);
    }


    @FXML
    private void onUpdate() {
        cargarPersonas();
        cargarTelefonos();
    }
    @FXML
    private void onAltas() {
        TextInputDialog dNombre = new TextInputDialog();
        dNombre.setTitle("Alta Persona");
        dNombre.setHeaderText("Nombre:");
        var nombreOpt = dNombre.showAndWait();
        if (nombreOpt.isEmpty() || nombreOpt.get().isBlank()) return;

        TextInputDialog dDir = new TextInputDialog();
        dDir.setTitle("Alta Persona");
        dDir.setHeaderText("Dirección (opcional):");
        var dirOpt = dDir.showAndWait();
        if (dirOpt.isEmpty()) return;

        String nombre = nombreOpt.get().trim();
        String direccion = dirOpt.get().trim();

        try (Connection c = conectar();
             PreparedStatement ps = c.prepareStatement(
                     "INSERT INTO Personas(nombre, direccion) VALUES(?, ?)"
             )) {
            ps.setString(1, nombre);
            ps.setString(2, direccion);
            ps.executeUpdate();
            onUpdate();
        } catch (Exception e) {
            alertaError("No se pudo dar de alta", e);
        }
    }
    @FXML
    private void onBajas() {
        PersonaFila p = tvPersonas.getSelectionModel().getSelectedItem();
        if (p == null) { alerta("Info", "Selecciona una persona."); return; }

        Alert conf = new Alert(Alert.AlertType.CONFIRMATION);
        conf.setTitle("Confirmar baja");
        conf.setHeaderText("Eliminar persona ID " + p.getId() + " ?");
        conf.setContentText("Se eliminarán también sus teléfonos (CASCADE).");
        if (conf.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;

        try (Connection c = conectar();
             PreparedStatement ps = c.prepareStatement("DELETE FROM Personas WHERE id=?")) {
            ps.setInt(1, p.getId());
            ps.executeUpdate();
            onUpdate();
        } catch (Exception e) {
            alertaError("No se pudo dar de baja", e);
        }
    }
    @FXML
    private void onCambios() {
        PersonaFila p = tvPersonas.getSelectionModel().getSelectedItem();
        if (p == null) { alerta("Info", "Selecciona una persona."); return; }

        TextInputDialog dNombre = new TextInputDialog(p.getNombre());
        dNombre.setTitle("Cambios Persona");
        dNombre.setHeaderText("Nuevo nombre:");
        var nombreOpt = dNombre.showAndWait();
        if (nombreOpt.isEmpty() || nombreOpt.get().isBlank()) return;

        TextInputDialog dDir = new TextInputDialog(p.getDireccion());
        dDir.setTitle("Cambios Persona");
        dDir.setHeaderText("Nueva dirección:");
        var dirOpt = dDir.showAndWait();
        if (dirOpt.isEmpty()) return;

        try (Connection c = conectar();
             PreparedStatement ps = c.prepareStatement(
                     "UPDATE Personas SET nombre=?, direccion=? WHERE id=?"
             )) {
            ps.setString(1, nombreOpt.get().trim());
            ps.setString(2, dirOpt.get().trim());
            ps.setInt(3, p.getId());
            ps.executeUpdate();
            onUpdate();
        } catch (Exception e) {
            alertaError("No se pudo aplicar cambios", e);
        }
    }
    @FXML
    private void onAgregarTelefono() {
        TextInputDialog dId = new TextInputDialog();
        dId.setTitle("Agregar Teléfono");
        dId.setHeaderText("Ingrese el ID de la persona");
        dId.setContentText("Persona ID:");

        var idOpt = dId.showAndWait();
        if (idOpt.isEmpty()) return;

        int personaId;
        try {
            personaId = Integer.parseInt(idOpt.get().trim());
        } catch (NumberFormatException e) {
            alerta("Error", "El Persona ID debe ser un número.");
            return;
        }

        TextInputDialog dTel = new TextInputDialog();
        dTel.setTitle("Agregar Teléfono");
        dTel.setHeaderText("Ingrese el teléfono para la persona ID " + personaId);
        dTel.setContentText("Teléfono:");

        var telOpt = dTel.showAndWait();
        if (telOpt.isEmpty() || telOpt.get().isBlank()) return;

        String telefono = telOpt.get().trim();

        try (java.sql.Connection c = java.sql.DriverManager.getConnection(URL, USER, PASSWORD);
             java.sql.PreparedStatement ps = c.prepareStatement(
                     "INSERT INTO Telefonos(personaId, telefono) VALUES(?, ?)"
             )) {

            ps.setInt(1, personaId);
            ps.setString(2, telefono);
            ps.executeUpdate();

            alerta("Éxito", "Teléfono agregado.");
            cargarTelefonos();

        } catch (Exception e) {
            alertaError("No se pudo agregar teléfono", e);
        }
    }
    @FXML
    private void onQuitarTelefono() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Eliminar Teléfono");
        dialog.setHeaderText("Ingrese el ID del teléfono a eliminar");
        dialog.setContentText("ID:");

        var resultado = dialog.showAndWait();

        if (resultado.isEmpty()) {
            return;
        }

        int idTelefono;
        try {
            idTelefono = Integer.parseInt(resultado.get().trim());
        } catch (NumberFormatException e) {
            alerta("Error", "El ID debe ser un número.");
            return;
        }

        try (Connection c = conectar();
             PreparedStatement ps =
                     c.prepareStatement("DELETE FROM Telefonos WHERE id = ?")) {

            ps.setInt(1, idTelefono);
            int filas = ps.executeUpdate();

            if (filas == 0) {
                alerta("Info", "No existe un teléfono con ese ID.");
            } else {
                alerta("Éxito", "Teléfono eliminado correctamente.");
                cargarTelefonos();
            }

        } catch (Exception e) {
            alertaError("No se pudo eliminar el teléfono", e);
        }
    }
    private void cargarPersonas() {
        personas.clear();
        try (Connection c = conectar();
             PreparedStatement ps = c.prepareStatement("SELECT id, nombre, direccion FROM Personas ORDER BY id");
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                personas.add(new PersonaFila(
                        rs.getInt("id"),
                        rs.getString("nombre"),
                        rs.getString("direccion")
                ));
            }
        } catch (Exception e) {
            alertaError("Error cargando personas", e);
        }
    }
    private void cargarTelefonos() {
        telefonos.clear();
        try (Connection c = conectar();
             PreparedStatement ps = c.prepareStatement("SELECT id, personaId, telefono FROM Telefonos ORDER BY id");
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                telefonos.add(new TelefonoFila(
                        rs.getInt("id"),
                        rs.getInt("personaId"),
                        rs.getString("telefono")
                ));
            }
        } catch (Exception e) {
            alertaError("Error cargando telefonos", e);
        }
    }

    // Alertas
    private void alerta(String titulo, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(titulo);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

    private void alertaError(String titulo, Exception e) {
        e.printStackTrace();
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle("Error");
        a.setHeaderText(titulo);
        a.setContentText(e.getMessage());
        a.showAndWait();
    }

}