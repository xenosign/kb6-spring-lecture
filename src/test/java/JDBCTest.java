import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;

import static org.junit.jupiter.api.Assertions.fail;


@Slf4j
public class JDBCTest {
    @BeforeAll
    public static void setup() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    @DisplayName("JDBC 드라이버 연결이 된다.")
    public void testConnection() {
        String url = "jdbc:mysql://localhost:3306/tetzdb";
        try (Connection con = DriverManager.getConnection(url,"root","1234")) {
            log.info(con.toString());
        }catch(Exception e) {
                fail(e.getMessage());
 }
    }
}