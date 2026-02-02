import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mariadb.jdbc.Connection;
import org.mariadb.jdbc.Statement;

import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class Prueba {
    private static final String URL = "jdbc:mariadb://localhost:3306/agenda";
    private static final String USUARIO = "usuario1";
    private static final String PASSWORD = "superpassword";

    private Connection conexion;

    @BeforeEach
    void conectar() throws Exception {
        conexion = (Connection) DriverManager.getConnection(URL, USUARIO, PASSWORD);
    }
    @AfterEach
    void cerrar() throws Exception {
        if (conexion != null) {
            conexion.close();
        }
    }
    @Test
    void insertarPersonaEnBD() throws Exception {
        String sql = "INSERT INTO Personas (nombre, direccion) VALUES (?, ?)";

        PreparedStatement ps =
                conexion.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);

        ps.setString(1, "Persona JUnit");
        ps.setString(2, "Calle Test 123");

        int filas = ps.executeUpdate();

        assertEquals(1, filas); // Se insertó 1 fila

        ResultSet rs = ps.getGeneratedKeys();
        assertTrue(rs.next()); // Se generó ID
    }
}
