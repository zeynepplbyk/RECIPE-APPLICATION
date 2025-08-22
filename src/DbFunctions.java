import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DbFunctions {

    public Connection connect_to_db(String TarifDB, String user, String pass) {
        Connection conn = null; //connection nesnesi oluşturup başlangıç için null atıyoruz bu nesne ile veri tabanına bağlanabiliriz
        try {
            // PostgreSQL JDBC sürücüsünü yükle
            Class.forName("org.postgresql.Driver");

            // Bağlantıyı kur
            conn = DriverManager.getConnection("jdbc:postgresql://localhost:5432/" + TarifDB, user, pass);

            if (conn != null) {
                System.out.println("Connection established");
            } else {
                System.out.println("Connection failed");
            }
        } catch (ClassNotFoundException e) {
            System.out.println("JDBC Driver bulunamadı: " + e.getMessage());
        } catch (SQLException e) {
            System.out.println("Bağlantı hatası: " + e.getMessage());
        }
        return conn; //conn (yani veritabanı bağlantı nesnesi) geri döndürüldükten sonra, bu nesneye erişen kod parçaları, artık o veritabanı bağlantısını kullanarak SQL sorguları çalıştırabilir, veri ekleyebilir, güncelleyebilir veya veritabanından veri okuyabilir. Yani, başka bir metot ya da sınıf, bu Connection nesnesini alıp, veritabanı ile etkileşime geçebilir.


    }
}