package repositorio;

import domain.Direccion;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public interface DireccionRepositorio {
    List<Direccion> findByPersonaId(Connection c, int personaId) throws SQLException;

    int getOrCreateDireccionId(Connection c, String direccionTxt) throws SQLException;

    void linkPersonaDireccion(Connection c, int personaId, int direccionId) throws SQLException;
    void unlinkPersonaDireccion(Connection c, int personaId, int direccionId) throws SQLException;

    boolean direccionEstaEnUso(Connection c, int direccionId) throws SQLException;

    void deleteDireccionById(Connection c, int direccionId) throws SQLException;

    int deleteDireccionesHuerfanas(Connection c) throws SQLException;
}
