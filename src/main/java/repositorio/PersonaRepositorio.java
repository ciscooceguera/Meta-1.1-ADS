package repositorio;

import domain.Persona;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public interface PersonaRepositorio {
    List<Persona> findAll(Connection c) throws SQLException;
    int insert(Connection c, String nombre) throws SQLException;
    void updateNombre(Connection c, int id, String nuevoNombre) throws SQLException;
    void delete(Connection c, int id) throws SQLException;
}
