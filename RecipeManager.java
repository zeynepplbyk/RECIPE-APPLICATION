import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class RecipeManager {
    private DbFunctions dbFunctions;
    private Connection connection;

    public RecipeManager(DbFunctions dbFunctions) {
        this.dbFunctions = dbFunctions;
        this.connection = dbFunctions.connect_to_db("TarifDB", "postgres", "1");
    }

    private JPanel createTarifEklePanel() {
        JTextField tarifAdiField = new JTextField(15);
        JTextField kategoriField = new JTextField(15);
        JTextField hazirlamaSuresiField = new JTextField(5);
        JTextArea talimatlarArea = new JTextArea(5, 15);
        JScrollPane talimatlarScrollPane = new JScrollPane(talimatlarArea);

        JPanel panel = new JPanel(new GridLayout(4, 2));
        panel.add(new JLabel("Tarif Adı:"));
        panel.add(tarifAdiField);
        panel.add(new JLabel("Kategori:"));
        panel.add(kategoriField);
        panel.add(new JLabel("Hazırlama Süresi (dk):"));
        panel.add(hazirlamaSuresiField);
        panel.add(new JLabel("Talimatlar:"));
        panel.add(talimatlarScrollPane);

        return panel;
    }

    private JPanel createImageSelectionPanel() {
        JPanel imagePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        JButton selectImageButton = new JButton("Resim Seç");
        JLabel imagePathLabel = new JLabel("Resim yolu: Yok");
        imagePanel.add(selectImageButton);
        imagePanel.add(imagePathLabel);

        // Action to choose an image
        selectImageButton.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            int returnVal = fileChooser.showOpenDialog(null);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                File selectedFile = fileChooser.getSelectedFile();
                imagePathLabel.setText("Resim yolu: " + selectedFile.getAbsolutePath());
            }
        });

        return imagePanel;
    }

    void tarifEkle() {
        JPanel tarifPanel = createTarifEklePanel();
        JPanel imagePanel = createImageSelectionPanel();

        // Create a main panel to hold both tarifPanel and imagePanel
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.add(tarifPanel);
        mainPanel.add(imagePanel);

        int result = JOptionPane.showConfirmDialog(null, mainPanel, "Tarif Ekle", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            String tarifAdi = ((JTextField) tarifPanel.getComponent(1)).getText();
            String kategori = ((JTextField) tarifPanel.getComponent(3)).getText();
            String talimatlar = ((JTextArea) ((JScrollPane) tarifPanel.getComponent(7)).getViewport().getView()).getText();
            String resim = ((JLabel) imagePanel.getComponent(1)).getText().replace("Resim yolu: ", ""); // Extract the file path

            int hazirlamaSuresi;
            try {
                hazirlamaSuresi = Integer.parseInt(((JTextField) tarifPanel.getComponent(5)).getText());
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(null, "Hazırlama süresi geçerli bir sayı olmalıdır.");
                return;
            }

            try {
                // Duplicate control
                String checkQuery = "SELECT COUNT(*) FROM Tarifler WHERE TarifAdi = ?";
                PreparedStatement checkStmt = connection.prepareStatement(checkQuery);
                checkStmt.setString(1, tarifAdi);
                ResultSet rs = checkStmt.executeQuery();
                if (rs.next() && rs.getInt(1) > 0) {
                    JOptionPane.showMessageDialog(null, "Bu tarif zaten mevcut.");
                    return;
                }

                // Insert the recipe, including the image path
                String insertQuery = "INSERT INTO Tarifler (TarifAdi, Kategori, Talimatlar, HazirlamaSuresi, resim) VALUES (?, ?, ?, ?, ?) RETURNING TarifID";
                PreparedStatement pstmt = connection.prepareStatement(insertQuery);
                pstmt.setString(1, tarifAdi);
                pstmt.setString(2, kategori);
                pstmt.setString(3, talimatlar);
                pstmt.setInt(4, hazirlamaSuresi);
                pstmt.setString(5, resim); // Save the image path
                ResultSet tarifResult = pstmt.executeQuery();

                if (tarifResult.next()) {
                    int tarifID = tarifResult.getInt("TarifID");
                    JOptionPane.showMessageDialog(null, "Tarif başarıyla eklendi!");

                    // Add ingredients
                    malzemeEkle(tarifID);
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(null, "Tarif eklenirken bir hata oluştu: " + ex.getMessage());
            }
        }
    }


    void malzemeEkle(int tarifID) {
        try {
            // Mevcut malzemeleri çekin
            String malzemeQuery = "SELECT MalzemeAdi FROM Malzemeler";
            PreparedStatement malzemeStmt = connection.prepareStatement(malzemeQuery);
            ResultSet rs = malzemeStmt.executeQuery();

            // Mevcut malzemeleri tutacak liste
            List<String> malzemeList = new ArrayList<>();

            while (rs.next()) {
                malzemeList.add(rs.getString("MalzemeAdi"));
            }

            // Mevcut malzemeleri listeleyin
            String[] malzemeArray = malzemeList.toArray(new String[0]);
            JList<String> malzemeJList = new JList<>(malzemeArray);
            malzemeJList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
            JScrollPane scrollPane = new JScrollPane(malzemeJList);

            // Malzeme ekleme paneli
            JPanel panel = new JPanel(new BorderLayout());
            panel.add(scrollPane, BorderLayout.CENTER);

            // Butonlar
            JPanel buttonPanel = new JPanel();
            JButton ekleButon = new JButton("Seçilenleri Ekle");
            JButton yeniMalzemeButon = new JButton("Yeni Malzeme Ekle");
            buttonPanel.add(ekleButon);
            buttonPanel.add(yeniMalzemeButon);
            panel.add(buttonPanel, BorderLayout.SOUTH);

            ekleButon.addActionListener(e -> {
                List<MalzemeBilgileri> malzemeBilgileriList = new ArrayList<>();
                for (String selectedMalzeme : malzemeJList.getSelectedValuesList()) {
                    // Sadece malzeme miktarını sormak için çağır
                    MalzemeBilgileri malzemeBilgileri = yeniMalzemeMiktar(selectedMalzeme);
                    if (malzemeBilgileri != null) {
                        malzemeBilgileriList.add(malzemeBilgileri);
                    }
                }
                if (!malzemeBilgileriList.isEmpty()) {
                    for (MalzemeBilgileri bilgiler : malzemeBilgileriList) {
                        ekleMalzeme(tarifID, bilgiler);
                    }
                    JOptionPane.showMessageDialog(null, "Seçilen malzemeler başarıyla eklendi!");
                } else {
                    JOptionPane.showMessageDialog(null, "Hiç malzeme seçilmedi.");
                }
            });

            yeniMalzemeButon.addActionListener(e -> {
                String malzemeAdi = JOptionPane.showInputDialog("Yeni Malzeme Adı:");
                if (malzemeAdi != null && !malzemeAdi.trim().isEmpty()) {
                    MalzemeBilgileri malzemeBilgileri = yeniMalzemeBilgisi(malzemeAdi);
                    if (malzemeBilgileri != null) {
                        // Yeni malzeme ekleme
                        ekleMalzeme(tarifID, malzemeBilgileri);
                        JOptionPane.showMessageDialog(null, "Yeni malzeme başarıyla eklendi!");
                    }
                }
            });

            JOptionPane.showMessageDialog(null, panel, "Malzeme Seç", JOptionPane.PLAIN_MESSAGE);
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Malzeme eklenirken bir hata oluştu: " + e.getMessage());
        }
    }

    private MalzemeBilgileri yeniMalzemeMiktar(String malzemeAdi) {
        JTextField miktarField = new JTextField(5);
        JPanel panel = new JPanel(new GridLayout(2, 2));
        panel.add(new JLabel("Malzeme Miktarı:"));
        panel.add(miktarField);

        int result = JOptionPane.showConfirmDialog(null, panel, "Malzeme Miktarı: " + malzemeAdi, JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result == JOptionPane.OK_OPTION) {
            String miktar = miktarField.getText();
            return new MalzemeBilgileri(malzemeAdi, miktar, null, null); // Birim ve fiyat null
        }
        return null; // Kullanıcı iptal ederse null döner
    }
    private MalzemeBilgileri yeniMalzemeBilgisi(String malzemeAdi) {
        JTextField miktarField = new JTextField(5);
        JTextField birimField = new JTextField(5);
        JTextField fiyatField = new JTextField(5);

        JPanel panel = new JPanel(new GridLayout(4, 2));
        panel.add(new JLabel("Malzeme Miktarı:"));
        panel.add(miktarField);
        panel.add(new JLabel("Malzeme Birimi:"));
        panel.add(birimField);
        panel.add(new JLabel("Birim Fiyat:"));
        panel.add(fiyatField);

        int result = JOptionPane.showConfirmDialog(null, panel, "Malzeme Bilgileri: " + malzemeAdi, JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result == JOptionPane.OK_OPTION) {
            String miktar = miktarField.getText();
            String birim = birimField.getText();
            String fiyat = fiyatField.getText();

            return new MalzemeBilgileri(malzemeAdi, miktar, birim, fiyat);
        }
        return null; // Kullanıcı iptal ederse null döner
    }

    private void ekleMalzeme(int tarifID, MalzemeBilgileri malzemeBilgileri) {
        try {
            // MalzemeID'yi kontrol et
            String malzemeCheckQuery = "SELECT MalzemeID FROM Malzemeler WHERE MalzemeAdi = ?";
            PreparedStatement malzemeCheckStmt = connection.prepareStatement(malzemeCheckQuery);
            malzemeCheckStmt.setString(1, malzemeBilgileri.getMalzemeAdi());
            ResultSet malzemeRs = malzemeCheckStmt.executeQuery();
            int malzemeID;

            if (malzemeRs.next()) {
                malzemeID = malzemeRs.getInt("MalzemeID");
            } else {
                // Yeni malzeme ekleme
                String insertMalzemeQuery = "INSERT INTO Malzemeler (MalzemeAdi, ToplamMiktar, MalzemeBirim, BirimFiyat) VALUES (?, ?, ?, ?) RETURNING MalzemeID";
                PreparedStatement insertMalzemeStmt = connection.prepareStatement(insertMalzemeQuery);
                insertMalzemeStmt.setString(1, malzemeBilgileri.getMalzemeAdi());
                insertMalzemeStmt.setString(2, malzemeBilgileri.getMiktar());
                insertMalzemeStmt.setString(3, malzemeBilgileri.getBirim());
                insertMalzemeStmt.setDouble(4, Double.parseDouble(malzemeBilgileri.getFiyat()));
                ResultSet newMalzemeRs = insertMalzemeStmt.executeQuery();

                if (newMalzemeRs.next()) {
                    malzemeID = newMalzemeRs.getInt("MalzemeID");
                } else {
                    JOptionPane.showMessageDialog(null, "Yeni malzeme eklenirken bir hata oluştu.");
                    return;
                }
            }

            // TarifMalzeme tablosuna ekleme
            String insertTarifMalzemeQuery = "INSERT INTO TarifMalzeme (TarifID, MalzemeID, MalzemeMiktar) VALUES (?, ?, ?)";
            PreparedStatement insertTarifMalzemeStmt = connection.prepareStatement(insertTarifMalzemeQuery);
            insertTarifMalzemeStmt.setInt(1, tarifID);
            insertTarifMalzemeStmt.setInt(2, malzemeID);
            insertTarifMalzemeStmt.setDouble(3, Double.parseDouble(malzemeBilgileri.getMiktar()));
            insertTarifMalzemeStmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Malzeme eklenirken bir hata oluştu: " + e.getMessage());
        }
    }
    void tarifGuncelle() {
        JPanel panel = createTarifEklePanel();
        int result = JOptionPane.showConfirmDialog(null, panel, "Tarif Güncelle", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            String tarifAdi = ((JTextField) panel.getComponent(1)).getText();
            String kategori = ((JTextField) panel.getComponent(3)).getText();
            String talimatlar = ((JTextArea) ((JScrollPane) panel.getComponent(7)).getViewport().getView()).getText();
            int hazirlamaSuresi;

            try {
                hazirlamaSuresi = Integer.parseInt(((JTextField) panel.getComponent(5)).getText());
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(null, "Hazırlama süresi geçerli bir sayı olmalıdır.");
                return;
            }

            try {
                // Tarif güncelleme işlemi
                String query = "UPDATE Tarifler SET Kategori = ?, Talimatlar = ?, HazirlamaSuresi = ? WHERE TarifAdi = ?";
                PreparedStatement pstmt = connection.prepareStatement(query);
                pstmt.setString(1, kategori);
                pstmt.setString(2, talimatlar);
                pstmt.setInt(3, hazirlamaSuresi);
                pstmt.setString(4, tarifAdi);
                int rowsAffected = pstmt.executeUpdate();

                if (rowsAffected > 0) {
                    JOptionPane.showMessageDialog(null, "Tarif başarıyla güncellendi!");

                    // Güncellenen tarifin ID'sini alın
                    String getTarifIDQuery = "SELECT TarifID FROM Tarifler WHERE TarifAdi = ?";
                    PreparedStatement getTarifIDStmt = connection.prepareStatement(getTarifIDQuery);
                    getTarifIDStmt.setString(1, tarifAdi);
                    ResultSet tarifIDResult = getTarifIDStmt.executeQuery();

                    if (tarifIDResult.next()) {
                        int tarifID = tarifIDResult.getInt("TarifID");

                        // Malzeme güncelleme işlemini başlat
                        malzemeGuncelle(tarifID);
                    }
                } else {
                    JOptionPane.showMessageDialog(null, "Belirtilen tarif bulunamadı.");
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(null, "Tarif güncellenirken bir hata oluştu: " + ex.getMessage());
            }
        }
    }

    void malzemeGuncelle(int tarifID) {
        try {
            // Tarifin mevcut malzemelerini al
            String mevcutMalzemelerQuery = "SELECT m.MalzemeAdi FROM TarifMalzeme tm JOIN Malzemeler m ON tm.MalzemeID = m.MalzemeID WHERE tm.TarifID = ?";
            PreparedStatement mevcutMalzemelerStmt = connection.prepareStatement(mevcutMalzemelerQuery);
            mevcutMalzemelerStmt.setInt(1, tarifID);
            ResultSet mevcutMalzemelerRS = mevcutMalzemelerStmt.executeQuery();

            // Mevcut malzemeleri bir listeye koy
            List<String> mevcutMalzemeList = new ArrayList<>();
            while (mevcutMalzemelerRS.next()) {
                mevcutMalzemeList.add(mevcutMalzemelerRS.getString("MalzemeAdi"));
            }

            // Tüm malzemeleri al
            String malzemeQuery = "SELECT MalzemeAdi FROM Malzemeler";
            PreparedStatement malzemeStmt = connection.prepareStatement(malzemeQuery);
            ResultSet malzemeRS = malzemeStmt.executeQuery();

            List<String> malzemeList = new ArrayList<>();
            while (malzemeRS.next()) {
                malzemeList.add(malzemeRS.getString("MalzemeAdi"));
            }

            // Mevcut malzemeler ile tüm malzemeler arasındaki farkı hesapla
            List<String> mevcutOlmayanMalzemeler = new ArrayList<>(malzemeList);
            mevcutOlmayanMalzemeler.removeAll(mevcutMalzemeList);

            // GUI elemanları oluştur
            JPanel panel = new JPanel(new BorderLayout());

            // Mevcut tarifin malzemeleri listesi
            JList<String> mevcutMalzemeJList = new JList<>(mevcutMalzemeList.toArray(new String[0]));
            mevcutMalzemeJList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
            JScrollPane mevcutMalzemeScrollPane = new JScrollPane(mevcutMalzemeJList);

            // Eklenecek malzemelerin listesi
            JList<String> mevcutOlmayanMalzemeJList = new JList<>(mevcutOlmayanMalzemeler.toArray(new String[0]));
            mevcutOlmayanMalzemeJList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
            JScrollPane mevcutOlmayanMalzemeScrollPane = new JScrollPane(mevcutOlmayanMalzemeJList);

            // Mevcut ve eklenebilir malzemeleri yan yana eklemek için panel
            JPanel malzemePaneli = new JPanel(new GridLayout(1, 2));
            malzemePaneli.add(new JScrollPane(mevcutMalzemeJList));
            malzemePaneli.add(new JScrollPane(mevcutOlmayanMalzemeJList));

            // Butonlar
            JButton malzemeEkleButon = new JButton("Seçilen Malzemeleri Ekle");
            JButton malzemeSilButon = new JButton("Seçilen Malzemeleri Çıkar");

            JPanel buttonPanel = new JPanel();
            buttonPanel.add(malzemeEkleButon);
            buttonPanel.add(malzemeSilButon);

            // Panele ekle
            panel.add(malzemePaneli, BorderLayout.CENTER);
            panel.add(buttonPanel, BorderLayout.SOUTH);

            // Malzeme ekleme işlemi
            malzemeEkleButon.addActionListener(e -> {
                List<String> secilenMalzemeler = mevcutOlmayanMalzemeJList.getSelectedValuesList();
                for (String malzeme : secilenMalzemeler) {
                    MalzemeBilgileri malzemeBilgisi = yeniMalzemeBilgisi(malzeme); // Yeni malzeme bilgilerini al
                    if (malzemeBilgisi != null) {
                        ekleMalzeme(tarifID, malzemeBilgisi);  // Seçilen malzemeleri tarif malzemelerine ekle
                    }
                }

                // Listeleri güncelle
                mevcutMalzemeList.addAll(secilenMalzemeler);
                mevcutOlmayanMalzemeler.removeAll(secilenMalzemeler);
                mevcutMalzemeJList.setListData(mevcutMalzemeList.toArray(new String[0]));
                mevcutOlmayanMalzemeJList.setListData(mevcutOlmayanMalzemeler.toArray(new String[0]));

                JOptionPane.showMessageDialog(null, "Seçilen malzemeler başarıyla eklendi!");
            });

            // Malzeme çıkarma işlemi
            malzemeSilButon.addActionListener(e -> {
                List<String> secilenMalzemeler = mevcutMalzemeJList.getSelectedValuesList();
                for (String malzeme : secilenMalzemeler) {
                    malzemeCikar(tarifID, malzeme);  // Seçilen malzemeleri tariften çıkar
                }

                // Listeleri güncelle
                mevcutOlmayanMalzemeler.addAll(secilenMalzemeler);
                mevcutMalzemeList.removeAll(secilenMalzemeler);
                mevcutMalzemeJList.setListData(mevcutMalzemeList.toArray(new String[0]));
                mevcutOlmayanMalzemeJList.setListData(mevcutOlmayanMalzemeler.toArray(new String[0]));

                JOptionPane.showMessageDialog(null, "Seçilen malzemeler başarıyla çıkarıldı!");
            });

            JOptionPane.showMessageDialog(null, panel, "Malzeme Güncelle", JOptionPane.PLAIN_MESSAGE);
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Malzeme güncellenirken bir hata oluştu: " + e.getMessage());
        }
    }

    private void malzemeCikar(int tarifID, String malzemeAdi) {
        try {
            String malzemeIDQuery = "SELECT MalzemeID FROM Malzemeler WHERE MalzemeAdi = ?";
            PreparedStatement malzemeIDStmt = connection.prepareStatement(malzemeIDQuery);
            malzemeIDStmt.setString(1, malzemeAdi);
            ResultSet rs = malzemeIDStmt.executeQuery();

            if (rs.next()) {
                int malzemeID = rs.getInt("MalzemeID");

                String deleteQuery = "DELETE FROM TarifMalzeme WHERE TarifID = ? AND MalzemeID = ?";
                PreparedStatement deleteStmt = connection.prepareStatement(deleteQuery);
                deleteStmt.setInt(1, tarifID);
                deleteStmt.setInt(2, malzemeID);
                deleteStmt.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Malzeme silinirken bir hata oluştu: " + e.getMessage());
        }
    }


    void tarifSil() {
        JTextField tarifAdiField = new JTextField(15);

        JPanel panel = new JPanel(new GridLayout(1, 2));
        panel.add(new JLabel("Silinecek Tarif Adı:"));
        panel.add(tarifAdiField);

        int result = JOptionPane.showConfirmDialog(null, panel, "Tarif Sil", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result == JOptionPane.OK_OPTION) {
            String tarifAdi = tarifAdiField.getText();

            try {
                String query = "DELETE FROM Tarifler WHERE TarifAdi = ?";
                PreparedStatement pstmt = connection.prepareStatement(query);
                pstmt.setString(1, tarifAdi);
                int rowsAffected = pstmt.executeUpdate();

                if (rowsAffected > 0) {
                    JOptionPane.showMessageDialog(null, "Tarif başarıyla silindi!");
                } else {
                    JOptionPane.showMessageDialog(null, "Belirtilen tarif bulunamadı.");
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(null, "Tarif silinirken bir hata oluştu: " + ex.getMessage());
            }
        }
    }


    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Malzeme bilgilerini tutacak bir sınıf
    private class MalzemeBilgileri {
        private String malzemeAdi;
        private String miktar;
        private String birim;
        private String fiyat;

        public MalzemeBilgileri(String malzemeAdi, String miktar, String birim, String fiyat) {
            this.malzemeAdi = malzemeAdi;
            this.miktar = miktar;
            this.birim = birim;
            this.fiyat = fiyat;
        }

        public String getMalzemeAdi() {
            return malzemeAdi;
        }

        public String getMiktar() {
            return miktar;
        }

        public String getBirim() {
            return birim;
        }

        public String getFiyat() {
            return fiyat;
        }
    }
}