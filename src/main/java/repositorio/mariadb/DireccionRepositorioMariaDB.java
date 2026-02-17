package repositorio.mariadb;



import domain.Direccion;
import repositorio.DireccionRepositorio;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DireccionRepositorioMariaDB implements DireccionRepositorio {

    @Override
    public List<Direccion> findByPersonaId(Connection c, int personaId) throws SQLException {
        String sql =
                "SELECT dc.id, dc.direccion " +
                        "FROM PersonaDireccion pd " +
                        "JOIN DireccionesCatalogo dc ON dc.id = pd.direccionID " +
                        "WHERE pd.personaID=? ORDER BY dc.id";

        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, personaId);
            try (ResultSet rs = ps.executeQuery()) {
                List<Direccion> out = new ArrayList<>();
                while (rs.next()) {
                    out.add(new Direccion(rs.getInt("id"), rs.getString("direccion")));
                }
                return out;
            }
        }
    }

    @Override
    public int getOrCreateDireccionId(Connection c, String direccionTxt) throws SQLException {
        String find = "SELECT id FROM DireccionesCatalogo WHERE direccion=?";
        try (PreparedStatement ps = c.prepareStatement(find)) {
            ps.setString(1, direccionTxt);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("id");
            }
        }

        String ins = "INSERT INTO DireccionesCatalogo(direccion) VALUES(?)";
        try (PreparedStatement ps = c.prepareStatement(ins, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, direccionTxt);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (!keys.next()) throw new SQLException("No se generó ID");
                return keys.getInt(1);
            }
        }
    }

    @Override
    public void linkPersonaDireccion(Connection c, int personaId, int direccionId) throws SQLException {
        String sql = "INSERT IGNORE INTO PersonaDireccion(personaID, direccionID) VALUES(?, ?)";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, personaId);
            ps.setInt(2, direccionId);
            ps.executeUpdate();
        }
    }

    @Override
    public void unlinkPersonaDireccion(Connection c, int personaId, int direccionId) throws SQLException {
        String sql = "DELETE FROM PersonaDireccion WHERE personaID=? AND direccionID=?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, personaId);
            ps.setInt(2, direccionId);
            ps.executeUpdate();
        }
    }

    @Override
    public boolean direccionEstaEnUso(Connection c, int direccionId) throws SQLException {
        String sql = "SELECT 1 FROM PersonaDireccion WHERE direccionID=? LIMIT 1";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, direccionId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    @Override
    public void deleteDireccionById(Connection c, int direccionId) throws SQLException {
        String sql = "DELETE FROM DireccionesCatalogo WHERE id=?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, direccionId);
            ps.executeUpdate();
        }
    }

    @Override
    public int deleteDireccionesHuerfanas(Connection c) throws SQLException {
        String sql =
                "DELETE dc FROM DireccionesCatalogo dc " +
                        "LEFT JOIN PersonaDireccion pd ON pd.direccionID = dc.id " +
                        "WHERE pd.direccionID IS NULL";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            return ps.executeUpdate();
        }
    }
}
