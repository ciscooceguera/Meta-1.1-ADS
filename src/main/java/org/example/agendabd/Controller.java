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
import java.sql.Statement;

public class Controller {
    // Configuracion DB
    private static final String URL = "jdbc:mariadb://localhost:3306/agenda";
    private static final String USER = "usuario1";
    private static final String PASSWORD = "superpassword";

    // Elementos UI Personas
    @FXML private TableView<PersonaFila> tvPersonas;
    @FXML private TableColumn<PersonaFila, Integer> colPersonaId;
    @FXML private TableColumn<PersonaFila, String> colPersonaNombre;

    // Elementos UI Telefonos
    @FXML private TableView<TelefonoFila> tvTelefonos;
    @FXML private TableColumn<TelefonoFila, Integer> colTelId;
    @FXML private TableColumn<TelefonoFila, String> colTelNumero;

    // Elementos UI Direcciones
    @FXML private TableView<DireccionFila> tvDirecciones;
    @FXML private TableColumn<DireccionFila, Integer> colDirID;
    @FXML private TableColumn<DireccionFila, String> colDirDireccion;

    private final ObservableList<PersonaFila> personas = FXCollections.observableArrayList();
    private final ObservableList<TelefonoFila> telefonos = FXCollections.observableArrayList();
    private final ObservableList<DireccionFila> direcciones = FXCollections.observableArrayList();

    @FXML
    public void initialize() {

        // Relación columnas y getters tabla Personas
        colPersonaId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colPersonaNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));


        // Relación columnas y getters tabla Telefonos
        colTelId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colTelNumero.setCellValueFactory(new PropertyValueFactory<>("telefono"));

        // Relación columnas y getters tabla Direcciones
        if (colDirID != null) {
            colDirID.setCellValueFactory(new PropertyValueFactory<>("id"));
        }
        if (colDirDireccion != null) {
            colDirDireccion.setCellValueFactory(new PropertyValueFactory<>("direccion"));
        }

        // Set items
        tvPersonas.setItems(personas);
        tvTelefonos.setItems(telefonos);
        if (tvDirecciones != null) {
            tvDirecciones.setItems(direcciones);
        }

        cargarPersonas();

        // Cargar telefonos y direcciones de la persona
        tvPersonas.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            if (newSel == null) {
                telefonos.clear();
                if (tvDirecciones != null) direcciones.clear();
            } else {
                cargarTelefonosPorPersona(newSel.getId());
                if (tvDirecciones != null) cargarDireccionesPorPersona(newSel.getId());
            }
        });

        // Por default se selecciona la primer persona en la lista
        if (!personas.isEmpty()) {
            tvPersonas.getSelectionModel().selectFirst();
        } else {
            telefonos.clear();
            if (tvDirecciones != null) direcciones.clear();
        }
    }

    private Connection conectar() throws Exception {
        return (Connection) DriverManager.getConnection(URL, USER, PASSWORD);
    }

    @FXML
    private void onUpdate() {
        PersonaFila sel = tvPersonas.getSelectionModel().getSelectedItem();
        int selId = (sel != null) ? sel.getId() : -1;

        cargarPersonas();

        // Seleccion auto
        if (selId != -1) {
            for (PersonaFila p : personas) {
                if (p.getId() == selId) {
                    tvPersonas.getSelectionModel().select(p);
                    return;
                }
            }
        }

        telefonos.clear();
        if (tvDirecciones != null) direcciones.clear();
    }

    @FXML
    private void onAltas() {
        TextInputDialog dNombre = new TextInputDialog();
        dNombre.setTitle("Alta Persona");
        dNombre.setHeaderText("Nombre:");
        var nombreOpt = dNombre.showAndWait();
        if (nombreOpt.isEmpty() || nombreOpt.get().isBlank()) return;

        String nombre = nombreOpt.get().trim();

        try (Connection c = conectar();
             PreparedStatement ps = c.prepareStatement("INSERT INTO Personas(nombre) VALUES(?)")) {
            ps.setString(1, nombre);
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
        conf.setContentText("Se eliminarán también sus teléfonos y sus relaciones con direcciones.");
        if (conf.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;

        try (java.sql.Connection c = conectar()) {
            c.setAutoCommit(false);

            try (PreparedStatement ps = c.prepareStatement("DELETE FROM Personas WHERE id=?")) {
                ps.setInt(1, p.getId());
                ps.executeUpdate();
            }

            limpiarDireccionesHuerfanas((Connection) c);

            c.commit();

            onUpdate();
        } catch (Exception e) {
            alertaError("No se pudo dar de baja", e);
        }
    }

    @FXML
    private void onCambios() {
        PersonaFila p = tvPersonas.getSelectionModel().getSelectedItem();
        if (p == null) { alerta("Info", "Selecciona una persona."); return; }

        // Cambios  nombre
        TextInputDialog dNombre = new TextInputDialog(p.getNombre());
        dNombre.setTitle("Cambios Persona");
        dNombre.setHeaderText("Nuevo nombre:");
        var nombreOpt = dNombre.showAndWait();
        if (nombreOpt.isEmpty() || nombreOpt.get().isBlank()) return;

        try (Connection c = conectar();
             PreparedStatement ps = c.prepareStatement("UPDATE Personas SET nombre=? WHERE id=?")) {
            ps.setString(1, nombreOpt.get().trim());
            ps.setInt(2, p.getId());
            ps.executeUpdate();
            onUpdate();
        } catch (Exception e) {
            alertaError("No se pudo aplicar cambios", e);
        }
    }

    @FXML
    private void onAgregarTelefono() {
        PersonaFila p = tvPersonas.getSelectionModel().getSelectedItem();
        if (p == null) { alerta("Info", "Selecciona una persona primero."); return; }

        TextInputDialog dTel = new TextInputDialog();
        dTel.setTitle("Agregar Teléfono");
        dTel.setHeaderText("Ingrese el teléfono para " + p.getNombre() + " (ID " + p.getId() + ")");
        dTel.setContentText("Teléfono:");

        var telOpt = dTel.showAndWait();
        if (telOpt.isEmpty() || telOpt.get().isBlank()) return;

        String telefono = telOpt.get().trim();

        try (java.sql.Connection c = java.sql.DriverManager.getConnection(URL, USER, PASSWORD);
             java.sql.PreparedStatement ps = c.prepareStatement(
                     "INSERT INTO Telefonos(personaId, telefono) VALUES(?, ?)"
             )) {

            ps.setInt(1, p.getId());
            ps.setString(2, telefono);
            ps.executeUpdate();

            alerta("Éxito", "Teléfono agregado.");
            cargarTelefonosPorPersona(p.getId());

        } catch (Exception e) {
            alertaError("No se pudo agregar teléfono", e);
        }
    }

    @FXML
    private void onQuitarTelefono() {
        PersonaFila p = tvPersonas.getSelectionModel().getSelectedItem();
        if (p == null) { alerta("Info", "Selecciona una persona primero."); return; }

        TelefonoFila t = tvTelefonos.getSelectionModel().getSelectedItem();
        if (t == null) { alerta("Info", "Selecciona un teléfono en la tabla."); return; }

        Alert conf = new Alert(Alert.AlertType.CONFIRMATION);
        conf.setTitle("Eliminar Teléfono");
        conf.setHeaderText("Eliminar teléfono ID " + t.getId() + " ?");
        if (conf.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;

        try (Connection c = conectar();
             PreparedStatement ps = c.prepareStatement("DELETE FROM Telefonos WHERE id = ?")) {

            ps.setInt(1, t.getId());
            int filas = ps.executeUpdate();

            if (filas == 0) {
                alerta("Info", "No existe un teléfono con ese ID.");
            } else {
                alerta("Éxito", "Teléfono eliminado correctamente.");
                cargarTelefonosPorPersona(p.getId());
            }

        } catch (Exception e) {
            alertaError("No se pudo eliminar el teléfono", e);
        }
    }

    @FXML
    private void onAltaDireccion() {
        if (tvDirecciones == null) { alerta("Info", "Tu FXML no tiene tabla de direcciones."); return; }

        PersonaFila p = tvPersonas.getSelectionModel().getSelectedItem();
        if (p == null) { alerta("Info", "Selecciona una persona primero."); return; }

        TextInputDialog dDir = new TextInputDialog();
        dDir.setTitle("Agregar Dirección");
        dDir.setHeaderText("Ingrese la dirección para " + p.getNombre() + " (ID " + p.getId() + ")");
        dDir.setContentText("Dirección:");
        var dirOpt = dDir.showAndWait();
        if (dirOpt.isEmpty() || dirOpt.get().isBlank()) return;

        String direccionTxt = dirOpt.get().trim();

        try (java.sql.Connection c = conectar()) {
            c.setAutoCommit(false);

            int direccionId = getOrCreateDireccionId((Connection) c, direccionTxt);

            // asociar persona y direccion
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT IGNORE INTO PersonaDireccion(personaID, direccionID) VALUES(?, ?)"
            )) {
                ps.setInt(1, p.getId());
                ps.setInt(2, direccionId);
                ps.executeUpdate();
            }

            c.commit();

            alerta("Éxito", "Dirección agregada.");
            cargarDireccionesPorPersona(p.getId());

        } catch (Exception e) {
            alertaError("No se pudo agregar dirección", e);
        }
    }

    @FXML
    private void onBajaDireccion() {
        if (tvDirecciones == null) { alerta("Info", "Tu FXML no tiene tabla de direcciones."); return; }

        PersonaFila p = tvPersonas.getSelectionModel().getSelectedItem();
        if (p == null) { alerta("Info", "Selecciona una persona primero."); return; }

        DireccionFila d = tvDirecciones.getSelectionModel().getSelectedItem();
        if (d == null) { alerta("Info", "Selecciona una dirección en la tabla."); return; }

        Alert conf = new Alert(Alert.AlertType.CONFIRMATION);
        conf.setTitle("Eliminar Dirección");
        conf.setHeaderText("Quitar esta dirección de la persona?");
        conf.setContentText("Dirección ID: " + d.getId());
        if (conf.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;

        int direccionId = d.getId();

        try (java.sql.Connection c = conectar()) {
            c.setAutoCommit(false);

            // desasociar
            try (PreparedStatement ps = c.prepareStatement(
                    "DELETE FROM PersonaDireccion WHERE personaID=? AND direccionID=?"
            )) {
                ps.setInt(1, p.getId());
                ps.setInt(2, direccionId);
                ps.executeUpdate();
            }

            // si ya nadie la usa, borrar
            if (!direccionEstaEnUso((Connection) c, direccionId)) {
                try (PreparedStatement ps = c.prepareStatement("DELETE FROM DireccionesCatalogo WHERE id=?")) {
                    ps.setInt(1, direccionId);
                    ps.executeUpdate();
                }
            }

            limpiarDireccionesHuerfanas((Connection) c);

            c.commit();

            alerta("Éxito", "Dirección eliminada.");
            cargarDireccionesPorPersona(p.getId());

        } catch (Exception e) {
            alertaError("No se pudo eliminar la dirección", e);
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
        PersonaFila p = tvPersonas.getSelectionModel().getSelectedItem();
        if (p == null) {
            telefonos.clear();
            return;
        }
        cargarTelefonosPorPersona(p.getId());
    }

    private void cargarTelefonosPorPersona(int personaId) {
        telefonos.clear();
        try (Connection c = conectar();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT id, personaId, telefono FROM Telefonos WHERE personaId=? ORDER BY id"
             )) {

            ps.setInt(1, personaId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    telefonos.add(new TelefonoFila(
                            rs.getInt("id"),
                            rs.getInt("personaId"),
                            rs.getString("telefono")
                    ));
                }
            }

        } catch (Exception e) {
            alertaError("Error cargando telefonos", e);
        }
    }

    private void cargarDireccionesPorPersona(int personaId) {
        direcciones.clear();

        try (Connection c = conectar();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT dc.id, dc.direccion " +
                             "FROM PersonaDireccion pd " +
                             "JOIN DireccionesCatalogo dc ON dc.id = pd.direccionID " +
                             "WHERE pd.personaID=? " +
                             "ORDER BY dc.id"
             )) {

            ps.setInt(1, personaId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    direcciones.add(new DireccionFila(
                            rs.getInt("id"),
                            rs.getString("direccion")
                    ));
                }
            }

        } catch (Exception e) {
            alertaError("Error cargando direcciones", e);
        }
    }

    // Helpers direcciones
    private int getOrCreateDireccionId(Connection c, String direccionTxt) throws Exception {
        // buscar
        try (PreparedStatement ps = c.prepareStatement("SELECT id FROM DireccionesCatalogo WHERE direccion=?")) {
            ps.setString(1, direccionTxt);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("id");
            }
        }
        // insertar
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO DireccionesCatalogo(direccion) VALUES(?)",
                Statement.RETURN_GENERATED_KEYS
        )) {
            ps.setString(1, direccionTxt);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (!keys.next()) throw new RuntimeException("No se pudo obtener ID de la dirección");
                return keys.getInt(1);
            }
        }
    }

    private boolean direccionEstaEnUso(Connection c, int direccionId) throws Exception {
        try (PreparedStatement ps = c.prepareStatement("SELECT 1 FROM PersonaDireccion WHERE direccionID=? LIMIT 1")) {
            ps.setInt(1, direccionId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private void limpiarDireccionesHuerfanas(Connection c) throws Exception {
        // borrar direcciones que no estan en persona
        try (PreparedStatement ps = c.prepareStatement(
                "DELETE dc FROM DireccionesCatalogo dc " +
                        "LEFT JOIN PersonaDireccion pd ON pd.direccionID = dc.id " +
                        "WHERE pd.direccionID IS NULL"
        )) {
            ps.executeUpdate();
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
