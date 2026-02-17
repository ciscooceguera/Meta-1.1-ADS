package repositorio.mariadb;


import domain.Persona;
import repositorio.PersonaRepositorio;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PersonaRepositorioMariaDB implements PersonaRepositorio {

    @Override
    public List<Persona> findAll(Connection c) throws SQLException {
        String sql = "SELECT id, nombre FROM Personas ORDER BY id";
        try (PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            List<Persona> out = new ArrayList<>();
            while (rs.next()) {
                out.add(new Persona(rs.getInt("id"), rs.getString("nombre")));
            }
            return out;
        }
    }

    @Override
    public int insert(Connection c, String nombre) throws SQLException {
        String sql = "INSERT INTO Personas(nombre) VALUES(?)";
        try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, nombre);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (!keys.next()) throw new SQLException("No se generó ID");
                return keys.getInt(1);
            }
        }
    }

    @Override
    public void updateNombre(Connection c, int id, String nuevoNombre) throws SQLException {
        String sql = "UPDATE Personas SET nombre=? WHERE id=?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, nuevoNombre);
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }

    @Override
    public void delete(Connection c, int id) throws SQLException {
        String sql = "DELETE FROM Personas WHERE id=?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }
}
