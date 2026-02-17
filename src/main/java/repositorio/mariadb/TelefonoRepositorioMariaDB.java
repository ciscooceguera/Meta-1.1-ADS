package repositorio.mariadb;



import domain.Telefono;
import repositorio.TelefonoRepositorio;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TelefonoRepositorioMariaDB implements TelefonoRepositorio {

    @Override
    public List<Telefono> findByPersonaId(Connection c, int personaId) throws SQLException {
        String sql = "SELECT id, personaId, telefono FROM Telefonos WHERE personaId=? ORDER BY id";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, personaId);
            try (ResultSet rs = ps.executeQuery()) {
                List<Telefono> out = new ArrayList<>();
                while (rs.next()) {
                    out.add(new Telefono(
                            rs.getInt("id"),
                            rs.getInt("personaId"),
                            rs.getString("telefono")
                    ));
                }
                return out;
            }
        }
    }

    @Override
    public int insert(Connection c, int personaId, String telefono) throws SQLException {
        String sql = "INSERT INTO Telefonos(personaId, telefono) VALUES(?, ?)";
        try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, personaId);
            ps.setString(2, telefono);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (!keys.next()) throw new SQLException("No se generó ID");
                return keys.getInt(1);
            }
        }
    }

    @Override
    public void deleteById(Connection c, int telId) throws SQLException {
        String sql = "DELETE FROM Telefonos WHERE id=?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, telId);
            ps.executeUpdate();
        }
    }

    @Override
    public void deleteByPersonaId(Connection c, int personaId) throws SQLException {
        String sql = "DELETE FROM Telefonos WHERE personaId=?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, personaId);
            ps.executeUpdate();
        }
    }
}
