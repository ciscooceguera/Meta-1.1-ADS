package UI;

import database.ConexionDB;
import database.ConexionMariaDB;
import domain.Direccion;
import domain.Persona;
import domain.Telefono;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import repositorio.DireccionRepositorio;
import repositorio.PersonaRepositorio;
import repositorio.TelefonoRepositorio;
import repositorio.mariadb.DireccionRepositorioMariaDB;
import repositorio.mariadb.PersonaRepositorioMariaDB;
import repositorio.mariadb.TelefonoRepositorioMariaDB;
import servicio.AgendaDataBase;

public class Controller {

    private final AgendaDataBase servicio;

    // UI Personas
    @FXML private TableView<Persona> tvPersonas;
    @FXML private TableColumn<Persona, Integer> colPersonaId;
    @FXML private TableColumn<Persona, String> colPersonaNombre;

    // UI Teléfonos
    @FXML private TableView<Telefono> tvTelefonos;
    @FXML private TableColumn<Telefono, Integer> colTelId;
    @FXML private TableColumn<Telefono, String> colTelNumero;

    // UI Direcciones
    @FXML private TableView<Direccion> tvDirecciones;
    @FXML private TableColumn<Direccion, Integer> colDirID;
    @FXML private TableColumn<Direccion, String> colDirDireccion;

    private final ObservableList<Persona> personas = FXCollections.observableArrayList();
    private final ObservableList<Telefono> telefonos = FXCollections.observableArrayList();
    private final ObservableList<Direccion> direcciones = FXCollections.observableArrayList();

    public Controller() {
        ConexionDB conexionDB = new ConexionMariaDB(
                "jdbc:mariadb://localhost:3306/agenda",
                "usuario1",
                "superpassword"
        );
        PersonaRepositorio personaRepo = new PersonaRepositorioMariaDB();
        TelefonoRepositorio telefonoRepo = new TelefonoRepositorioMariaDB();
        DireccionRepositorio direccionRepo = new DireccionRepositorioMariaDB();
        this.servicio = new AgendaDataBase(conexionDB, personaRepo, telefonoRepo, direccionRepo);
    }

    @FXML
    public void initialize() {
        colPersonaId.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().id()));
        colPersonaNombre.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().nombre()));
        colTelId.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().id()));
        colTelNumero.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().telefono()));
        if (colDirID != null) {
            colDirID.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().id()));
        }
        if (colDirDireccion != null) {
            colDirDireccion.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().direccion()));
        }
        tvPersonas.setItems(personas);
        tvTelefonos.setItems(telefonos);
        if (tvDirecciones != null) tvDirecciones.setItems(direcciones);

        cargarPersonas();

        tvPersonas.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            if (newSel == null) {
                telefonos.clear();
                if (tvDirecciones != null) direcciones.clear();
                return;
            }
            cargarTelefonosPorPersona(newSel.id());
            if (tvDirecciones != null) cargarDireccionesPorPersona(newSel.id());
        });

        // Selección default
        if (!personas.isEmpty()) {
            tvPersonas.getSelectionModel().selectFirst();
        } else {
            telefonos.clear();
            if (tvDirecciones != null) direcciones.clear();
        }
    }

    // Events

    @FXML
    private void onUpdate() {
        Persona sel = tvPersonas.getSelectionModel().getSelectedItem();
        int selId = (sel != null) ? sel.id() : -1;

        cargarPersonas();

        // selección
        if (selId != -1) {
            for (Persona p : personas) {
                if (p.id() == selId) {
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

        try {
            servicio.altaPersona(nombre);
            onUpdate();
        } catch (Exception e) {
            alertaError("No se pudo dar de alta", e);
        }
    }

    @FXML
    private void onBajas() {
        Persona p = tvPersonas.getSelectionModel().getSelectedItem();
        if (p == null) { alerta("Info", "Selecciona una persona."); return; }

        Alert conf = new Alert(Alert.AlertType.CONFIRMATION);
        conf.setTitle("Confirmar baja");
        conf.setHeaderText("Eliminar persona ID " + p.id() + " ?");
        conf.setContentText("Se eliminarán también sus teléfonos y sus relaciones con direcciones.");
        if (conf.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;

        try {
            servicio.bajaPersona(p.id());
            onUpdate();
        } catch (Exception e) {
            alertaError("No se pudo dar de baja", e);
        }
    }

    @FXML
    private void onCambios() {
        Persona p = tvPersonas.getSelectionModel().getSelectedItem();
        if (p == null) { alerta("Info", "Selecciona una persona."); return; }

        TextInputDialog dNombre = new TextInputDialog(p.nombre());
        dNombre.setTitle("Cambios Persona");
        dNombre.setHeaderText("Nuevo nombre:");
        var nombreOpt = dNombre.showAndWait();
        if (nombreOpt.isEmpty() || nombreOpt.get().isBlank()) return;

        try {
            servicio.cambiosPersona(p.id(), nombreOpt.get().trim());
            onUpdate();
        } catch (Exception e) {
            alertaError("No se pudo aplicar cambios", e);
        }
    }

    @FXML
    private void onAgregarTelefono() {
        Persona p = tvPersonas.getSelectionModel().getSelectedItem();
        if (p == null) { alerta("Info", "Selecciona una persona primero."); return; }

        TextInputDialog dTel = new TextInputDialog();
        dTel.setTitle("Agregar Teléfono");
        dTel.setHeaderText("Ingrese el teléfono para " + p.nombre() + " (ID " + p.id() + ")");
        dTel.setContentText("Teléfono:");
        var telOpt = dTel.showAndWait();
        if (telOpt.isEmpty() || telOpt.get().isBlank()) return;

        String telefono = telOpt.get().trim();

        try {
            servicio.agregarTelefono(p.id(), telefono);
            alerta("Éxito", "Teléfono agregado.");
            cargarTelefonosPorPersona(p.id());
        } catch (Exception e) {
            alertaError("No se pudo agregar teléfono", e);
        }
    }

    @FXML
    private void onQuitarTelefono() {
        Persona p = tvPersonas.getSelectionModel().getSelectedItem();
        if (p == null) { alerta("Info", "Selecciona una persona primero."); return; }

        Telefono t = tvTelefonos.getSelectionModel().getSelectedItem();
        if (t == null) { alerta("Info", "Selecciona un teléfono en la tabla."); return; }

        Alert conf = new Alert(Alert.AlertType.CONFIRMATION);
        conf.setTitle("Eliminar Teléfono");
        conf.setHeaderText("Eliminar teléfono ID " + t.id() + " ?");
        if (conf.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;

        try {
            servicio.quitarTelefono(t.id());
            alerta("Éxito", "Teléfono eliminado correctamente.");
            cargarTelefonosPorPersona(p.id());
        } catch (Exception e) {
            alertaError("No se pudo eliminar el teléfono", e);
        }
    }

    @FXML
    private void onAltaDireccion() {
        if (tvDirecciones == null) { alerta("Info", "Tu FXML no tiene tabla de direcciones."); return; }

        Persona p = tvPersonas.getSelectionModel().getSelectedItem();
        if (p == null) { alerta("Info", "Selecciona una persona primero."); return; }

        TextInputDialog dDir = new TextInputDialog();
        dDir.setTitle("Agregar Dirección");
        dDir.setHeaderText("Ingrese la dirección para " + p.nombre() + " (ID " + p.id() + ")");
        dDir.setContentText("Dirección:");
        var dirOpt = dDir.showAndWait();
        if (dirOpt.isEmpty() || dirOpt.get().isBlank()) return;

        String direccionTxt = dirOpt.get().trim();

        try {
            servicio.altaDireccionParaPersona(p.id(), direccionTxt);
            alerta("Éxito", "Dirección agregada.");
            cargarDireccionesPorPersona(p.id());
        } catch (Exception e) {
            alertaError("No se pudo agregar dirección", e);
        }
    }

    @FXML
    private void onBajaDireccion() {
        if (tvDirecciones == null) { alerta("Info", "Tu FXML no tiene tabla de direcciones."); return; }

        Persona p = tvPersonas.getSelectionModel().getSelectedItem();
        if (p == null) { alerta("Info", "Selecciona una persona primero."); return; }

        Direccion d = tvDirecciones.getSelectionModel().getSelectedItem();
        if (d == null) { alerta("Info", "Selecciona una dirección en la tabla."); return; }

        Alert conf = new Alert(Alert.AlertType.CONFIRMATION);
        conf.setTitle("Eliminar Dirección");
        conf.setHeaderText("Quitar esta dirección de la persona?");
        conf.setContentText("Dirección ID: " + d.id());
        if (conf.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;

        try {
            servicio.bajaDireccionDePersona(p.id(), d.id());
            alerta("Éxito", "Dirección eliminada.");
            cargarDireccionesPorPersona(p.id());
        } catch (Exception e) {
            alertaError("No se pudo eliminar la dirección", e);
        }
    }

    // Cargas AgendaDataBase

    private void cargarPersonas() {
        personas.clear();
        try {
            personas.addAll(servicio.listarPersonas());
        } catch (Exception e) {
            alertaError("Error cargando personas", e);
        }
    }

    private void cargarTelefonosPorPersona(int personaId) {
        telefonos.clear();
        try {
            telefonos.addAll(servicio.listarTelefonos(personaId));
        } catch (Exception e) {
            alertaError("Error cargando teléfonos", e);
        }
    }

    private void cargarDireccionesPorPersona(int personaId) {
        direcciones.clear();
        try {
            direcciones.addAll(servicio.listarDirecciones(personaId));
        } catch (Exception e) {
            alertaError("Error cargando direcciones", e);
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
