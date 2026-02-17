package servicio;



import database.ConexionDB;
import domain.*;
import repositorio.*;

import java.sql.Connection;
import java.util.List;

public class AgendaDataBase {
    private final ConexionDB cf;
    private final PersonaRepositorio personas;
    private final TelefonoRepositorio telefonos;
    private final DireccionRepositorio direcciones;

    public AgendaDataBase(ConexionDB cf,
                          PersonaRepositorio personas,
                          TelefonoRepositorio telefonos,
                          DireccionRepositorio direcciones) {
        this.cf = cf;
        this.personas = personas;
        this.telefonos = telefonos;
        this.direcciones = direcciones;
    }

    public List<Persona> listarPersonas() throws Exception {
        try (Connection c = cf.getConnection()) {
            return personas.findAll(c);
        }
    }

    public List<Telefono> listarTelefonos(int personaId) throws Exception {
        try (Connection c = cf.getConnection()) {
            return telefonos.findByPersonaId(c, personaId);
        }
    }

    public List<Direccion> listarDirecciones(int personaId) throws Exception {
        try (Connection c = cf.getConnection()) {
            return direcciones.findByPersonaId(c, personaId);
        }
    }

    public void altaPersona(String nombre) throws Exception {
        try (Connection c = cf.getConnection()) {
            personas.insert(c, nombre);
        }
    }

    public void cambiosPersona(int id, String nuevoNombre) throws Exception {
        try (Connection c = cf.getConnection()) {
            personas.updateNombre(c, id, nuevoNombre);
        }
    }

    public void bajaPersona(int personaId) throws Exception {
        try (Connection c = cf.getConnection()) {
            c.setAutoCommit(false);
            try {
                telefonos.deleteByPersonaId(c, personaId);

                personas.delete(c, personaId);

                direcciones.deleteDireccionesHuerfanas(c);

                c.commit();
            } catch (Exception e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(true);
            }
        }
    }

    public void agregarTelefono(int personaId, String tel) throws Exception {
        try (Connection c = cf.getConnection()) {
            telefonos.insert(c, personaId, tel);
        }
    }

    public void quitarTelefono(int telId) throws Exception {
        try (Connection c = cf.getConnection()) {
            telefonos.deleteById(c, telId);
        }
    }

    public void altaDireccionParaPersona(int personaId, String direccionTxt) throws Exception {
        try (Connection c = cf.getConnection()) {
            c.setAutoCommit(false);
            try {
                int dirId = direcciones.getOrCreateDireccionId(c, direccionTxt);
                direcciones.linkPersonaDireccion(c, personaId, dirId);
                c.commit();
            } catch (Exception e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(true);
            }
        }
    }

    public void bajaDireccionDePersona(int personaId, int direccionId) throws Exception {
        try (Connection c = cf.getConnection()) {
            c.setAutoCommit(false);
            try {
                direcciones.unlinkPersonaDireccion(c, personaId, direccionId);

                if (!direcciones.direccionEstaEnUso(c, direccionId)) {
                    direcciones.deleteDireccionById(c, direccionId);
                }

                direcciones.deleteDireccionesHuerfanas(c);
                c.commit();
            } catch (Exception e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(true);
            }
        }
    }
}
