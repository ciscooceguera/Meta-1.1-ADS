package repositorio;

import domain.Telefono;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public interface TelefonoRepositorio {
    List<Telefono> findByPersonaId(Connection c, int personaId) throws SQLException;
    int insert(Connection c, int personaId, String telefono) throws SQLException;
    void deleteById(Connection c, int telId) throws SQLException;
    void deleteByPersonaId(Connection c, int personaId) throws SQLException;
}
