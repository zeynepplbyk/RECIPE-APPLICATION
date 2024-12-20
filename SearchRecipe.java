import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class SearchRecipe {
    private Connection connection;
    private JPanel recipePanel;

    public SearchRecipe(Connection connection, JPanel recipePanel) {
        this.connection = connection;
        this.recipePanel = recipePanel;
    }
    private Map<String, Integer> stockQuantities = new HashMap<>();
    private boolean isIngredientSufficient(int recipeId) {
        try {
            String query = "SELECT m.MalzemeAdi, tm.MalzemeMiktar " +
                    "FROM TarifMalzeme tm " +
                    "JOIN Malzemeler m ON tm.MalzemeID = m.MalzemeID " +
                    "WHERE tm.TarifID = ?";
            PreparedStatement pstmt = connection.prepareStatement(query);
            pstmt.setInt(1, recipeId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                String malzemeAdi = rs.getString("MalzemeAdi");
                double requiredQuantity = rs.getDouble("MalzemeMiktar");
                int availableStock = stockQuantities.getOrDefault(malzemeAdi, 0); // Stok miktarını al

                // Debugging için gerekli ve mevcut miktarları kontrol edin
                System.out.println("Malzeme: " + malzemeAdi + ", Gerekli: " + requiredQuantity + ", Stok: " + availableStock );

                // Eğer mevcut stok gerekli miktardan azsa, tarif karşılanamaz
                if (availableStock < requiredQuantity) {
                    System.out.println("Malzeme yeterli değil: " + malzemeAdi);
                    return false; // Yetersiz
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return true; // Tüm malzemeler yeterli
    }
    private void generateRandomStock() {
        try {
            String query = "SELECT MalzemeAdi FROM Malzemeler"; // Malzeme adlarını çekmek için sorgu
            PreparedStatement pstmt = connection.prepareStatement(query);
            ResultSet rs = pstmt.executeQuery();

            Random random = new Random();
            while (rs.next()) {
                String malzemeAdi = rs.getString("MalzemeAdi");
                // 1 ile 20 arasında rastgele stok miktarı oluştur
                int randomStock = random.nextInt(5) + 1; // 1 ile 20 arasında rastgele stok
                stockQuantities.put(malzemeAdi, randomStock); // Haritaya ekle
            }

            // Rastgele stok bilgilerini yazdırma (debugging için)
            System.out.println("Oluşturulan Rastgele Stok Bilgileri: " + stockQuantities);

        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public double calculateRecipeCost(int tarifId) {
        double toplamMaliyet = 0;

        try {
            String malzemeQuery = "SELECT m.MalzemeAdi, tm.MalzemeMiktar, m.BirimFiyat " +
                    "FROM TarifMalzeme tm " +
                    "JOIN Malzemeler m ON tm.MalzemeID = m.MalzemeID " +
                    "WHERE tm.TarifID = ?";
            PreparedStatement pstmt = connection.prepareStatement(malzemeQuery);
            pstmt.setInt(1, tarifId); // TarifID'yi ayarla
            ResultSet malzemeRs = pstmt.executeQuery();

            while (malzemeRs.next()) {
                String malzemeAdi = malzemeRs.getString("MalzemeAdi");
                double malzemeMiktar = malzemeRs.getDouble("MalzemeMiktar");
                double birimFiyat = malzemeRs.getDouble("BirimFiyat");
                double maliyet = malzemeMiktar * birimFiyat;

                // Malzeme bilgilerini yaz
                toplamMaliyet += maliyet; // Toplam maliyeti güncelle
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        return toplamMaliyet;
    }

    public void searchRecipes(String searchTerm) {
        recipePanel.removeAll(); // Mevcut tarif kartlarını temizle

        try {
            // Hem tarif adı hem de malzeme adı ile arama yapan sorgu
            String query = "SELECT DISTINCT tarifler.*, " +
                    "(SELECT SUM(m.BirimFiyat * tm.MalzemeMiktar) " +
                    " FROM TarifMalzeme tm " +
                    " JOIN Malzemeler m ON tm.MalzemeID = m.MalzemeID " +
                    " WHERE tm.TarifID = tarifler.TarifID) AS toplamMaliyet " +
                    " FROM Tarifler " +
                    " LEFT JOIN TarifMalzeme tm ON tarifler.TarifID = tm.TarifID " +
                    " LEFT JOIN Malzemeler m ON tm.MalzemeID = m.MalzemeID " +
                    " WHERE tarifler.TarifAdi ILIKE ? OR m.MalzemeAdi ILIKE ?";
            PreparedStatement pstmt = connection.prepareStatement(query);
            pstmt.setString(1, "%" + searchTerm + "%");
            pstmt.setString(2, "%" + searchTerm + "%");
            ResultSet rs = pstmt.executeQuery();

            populateRecipePanel(rs);

        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(recipePanel, "Arama sırasında bir hata oluştu: " + ex.getMessage(), "Hata", JOptionPane.ERROR_MESSAGE);
        }

        recipePanel.revalidate();
        recipePanel.repaint();
    }
    private Map<String, Double> getMissingIngredients(int recipeId) {
        Map<String, Double> missingIngredients = new HashMap<>();
        try {
            String query = "SELECT m.MalzemeAdi, tm.MalzemeMiktar " +
                    "FROM TarifMalzeme tm " +
                    "JOIN Malzemeler m ON tm.MalzemeID = m.MalzemeID " +
                    "WHERE tm.TarifID = ?";
            PreparedStatement pstmt = connection.prepareStatement(query);
            pstmt.setInt(1, recipeId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                String malzemeAdi = rs.getString("MalzemeAdi");
                double requiredQuantity = rs.getDouble("MalzemeMiktar");
                int availableStock = stockQuantities.getOrDefault(malzemeAdi, 0); // Get available stock

                // If available stock is less than required, track the missing ingredient
                if (availableStock < requiredQuantity) {
                    missingIngredients.put(malzemeAdi, requiredQuantity - availableStock);
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return missingIngredients; // Return map of missing ingredients and their required amounts
    }

    private void populateRecipePanel(ResultSet rs) throws SQLException {
        generateRandomStock(); // Rastgele stok bilgilerini oluştur

        if (!rs.isBeforeFirst()) { // Eğer sonuç seti boşsa
            JLabel noResultsLabel = new JLabel("Hiçbir tarif bulunamadı.", JLabel.CENTER);
            recipePanel.add(noResultsLabel);
        } else {
            while (rs.next()) {
                String ad = rs.getString("TarifAdi");
                String kategori = rs.getString("Kategori");
                String talimatlar = rs.getString("Talimatlar");
                String resimYolu = rs.getString("resim"); // Resim yolunu veritabanından al
                int id = rs.getInt("tarifid"); // Tarifin id'si
                int hazirlamaSuresi = rs.getInt("HazirlamaSuresi"); // Hazırlama süresi bilgisini al

                double maliyet = calculateRecipeCost(id); // Maliyeti hesapla

                // Tarif kartı oluştur
                JPanel card = new JPanel();
                card.setLayout(new BorderLayout());
                card.setBorder(new EmptyBorder(10, 10, 10, 10));
                card.setPreferredSize(new Dimension(300, 260)); // Kart boyutunu ayarla
                recipePanel.revalidate();
                recipePanel.repaint();

                // Check ingredient sufficiency and set background color
                if (!isIngredientSufficient(id)) {

                    card.setBackground(Color.RED); // Not sufficient
                } else {
                    card.setBackground(Color.GREEN); // Sufficient
                }
                // Tarif adı
                JLabel recipeLabel = new JLabel(ad, JLabel.CENTER);
                recipeLabel.setFont(new Font("Arial", Font.BOLD, 16)); // Yazı boyutunu büyüt
                card.add(recipeLabel, BorderLayout.NORTH);

                // Resim için ayrı bir panel oluştur
                JPanel imagePanel = new JPanel();
                imagePanel.setLayout(new BorderLayout());

                // Resim ekleme
                if (resimYolu != null && !resimYolu.isEmpty()) {
                    try {
                        File resimDosyasi = new File(resimYolu);
                        if (!resimDosyasi.exists()) {
                            // Resim dosyası yoksa
                            JLabel noImageLabel = new JLabel("Resim bulunamadı (ID: " + id + ")", JLabel.CENTER);
                            imagePanel.add(noImageLabel, BorderLayout.CENTER);
                        } else {
                            ImageIcon imageIcon = new ImageIcon(ImageIO.read(resimDosyasi));
                            imageIcon = new ImageIcon(imageIcon.getImage().getScaledInstance(300, 250, Image.SCALE_SMOOTH)); // Resim boyutunu büyüt
                            JLabel imageLabel = new JLabel(imageIcon);
                            imagePanel.add(imageLabel, BorderLayout.CENTER); // Resmi kartın tarafında göster
                        }
                    } catch (Exception e) {
                        System.out.println("Resim yüklenirken hata oluştu: " + e.getMessage());
                        e.printStackTrace(); // Resim yükleme hatalarını yakala
                    }
                } else {
                    // Resim yoksa ID'yi göster
                    JLabel noImageLabel = new JLabel("Resim yok (ID: " + id + ")", JLabel.CENTER);
                    imagePanel.add(noImageLabel, BorderLayout.CENTER);
                }

                card.add(imagePanel, BorderLayout.CENTER); // Resim panelini kartın merkezine ekle
                JPanel infoPanel = new JPanel();
                infoPanel.setLayout(new GridLayout(2, 1)); // 2 satır, 1 sütun

                JLabel costLabel = new JLabel("Maliyet: " + String.format("%.2f", maliyet) + " TL", JLabel.LEFT);
                JLabel timeLabel = new JLabel("Hazırlama Süresi: " + hazirlamaSuresi + " dakika", JLabel.LEFT);
                infoPanel.add(timeLabel);
                infoPanel.add(costLabel);
                recipePanel.add(card); // Kartı panele ekle

                // Tarife tıklandığında talimatları ve malzemeleri gösteren bir MouseListener ekle
// Inside the mouseClicked method for the recipe card
                card.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        // Yeni pencere açılıyor
                        JFrame detailFrame = new JFrame(ad);
                        detailFrame.setSize(400, 400);
                        detailFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                        detailFrame.setLocationRelativeTo(null);

                        // Talimatlar için JTextArea
                        JTextArea instructionsArea = new JTextArea(talimatlar);
                        instructionsArea.setWrapStyleWord(true);
                        instructionsArea.setLineWrap(true);
                        instructionsArea.setEditable(false);
                        instructionsArea.setFont(new Font("Arial", Font.PLAIN, 19));

                        // Malzeme bilgisi
                        StringBuilder ingredientsText = new StringBuilder("Malzemeler:\n");
                        double totalCost = 0;

                        try {
                            String ingredientQuery = "SELECT m.MalzemeAdi, tm.MalzemeMiktar, m.BirimFiyat " +
                                    "FROM TarifMalzeme tm " +
                                    "JOIN Malzemeler m ON tm.MalzemeID = m.MalzemeID " +
                                    "WHERE tm.TarifID = ?";
                            PreparedStatement pstmt = connection.prepareStatement(ingredientQuery);
                            pstmt.setInt(1, id);
                            ResultSet ingredientRs = pstmt.executeQuery();

                            while (ingredientRs.next()) {
                                String ingredientName = ingredientRs.getString("MalzemeAdi");
                                double ingredientAmount = ingredientRs.getDouble("MalzemeMiktar");
                                double unitPrice = ingredientRs.getDouble("BirimFiyat");
                                double cost = ingredientAmount * unitPrice;

                                // Malzeme listesi metni oluştur
                                ingredientsText.append(ingredientName).append(" - ").append(ingredientAmount).append(" kilo, Fiyat: ").append(cost).append(" TL\n");
                                totalCost += cost;
                            }
                        } catch (SQLException ex) {
                            ex.printStackTrace();
                        }

                        // Eksik malzemeler paneli ekleme
                        Map<String, Double> missingIngredients = getMissingIngredients(id);
                        StringBuilder missingIngredientsDisplay = new StringBuilder("\nEksik Malzemeler:\n");
                        double totalMissingCost = 0;

                        // Eksik malzemeleri döngüyle listele
                        for (Map.Entry<String, Double> entry : missingIngredients.entrySet()) {
                            String missingIngredientName = entry.getKey();
                            double missingIngredientAmount = entry.getValue();

                            // Eksik malzemenin birim fiyatını al
                            double missingIngredientCost = 0;
                            try {
                                String missingIngredientQuery = "SELECT BirimFiyat FROM Malzemeler WHERE MalzemeAdi = ?";
                                PreparedStatement pstmt = connection.prepareStatement(missingIngredientQuery);
                                pstmt.setString(1, missingIngredientName);
                                ResultSet rs = pstmt.executeQuery();
                                if (rs.next()) {
                                    double unitPrice = rs.getDouble("BirimFiyat");
                                    missingIngredientCost = unitPrice * missingIngredientAmount;
                                    totalMissingCost += missingIngredientCost; // Eksik maliyetin toplamını güncelle
                                }
                            } catch (SQLException ex) {
                                ex.printStackTrace();
                            }

                            // Eksik malzeme bilgisi ekle
                            missingIngredientsDisplay.append(missingIngredientName)
                                    .append(" - Gerekli Miktar: ")
                                    .append(missingIngredientAmount)
                                    .append(", Maliyet: ")
                                    .append(String.format("%.2f", missingIngredientCost))
                                    .append(" TL\n");
                        }

                        // Eksik malzemeler ve malzemeler için JTextArea
                        ingredientsText.append(missingIngredientsDisplay); // Eksik malzemeleri malzemeler altına ekle
                        JTextArea ingredientsArea = new JTextArea(ingredientsText.toString());
                        ingredientsArea.setWrapStyleWord(true);
                        ingredientsArea.setLineWrap(true);
                        ingredientsArea.setEditable(false);
                        ingredientsArea.setFont(new Font("Arial", Font.PLAIN, 19));

                        // Toplam maliyet etiketi
                        JLabel totalCostLabel = new JLabel("Toplam Maliyet: " + String.format("%.2f", totalCost) + " TL");
                        totalCostLabel.setFont(new Font("Arial", Font.BOLD, 16));

                        // Toplam eksik malzeme maliyeti için etiket
                        JLabel totalMissingCostLabel = new JLabel("Toplam Eksik Malzeme Maliyeti: " + String.format("%.2f", totalMissingCost) + " TL");
                        totalMissingCostLabel.setFont(new Font("Arial", Font.BOLD, 16));

                        // İçerik paneli oluştur
                        JPanel contentPanel = new JPanel();
                        contentPanel.setLayout(new BorderLayout());

                        // Talimatları ve malzemeleri ekleyin
                        contentPanel.add(new JScrollPane(instructionsArea), BorderLayout.NORTH);
                        contentPanel.add(new JScrollPane(ingredientsArea), BorderLayout.CENTER);

                        // Alt panel oluştur ve toplam maliyet etiketlerini ekle
                        JPanel bottomPanel = new JPanel(new GridLayout(2, 1));
                        bottomPanel.add(totalCostLabel);
                        bottomPanel.add(totalMissingCostLabel);

                        // Alt paneli içerik paneline ekle
                        contentPanel.add(bottomPanel, BorderLayout.SOUTH);

                        // İçerik panelini pencereye ekleyip göster
                        detailFrame.add(contentPanel);
                        detailFrame.pack(); // Pencere boyutunu ayarla
                        detailFrame.setVisible(true); // Pencereyi göster
                    }
                });




                // Kartın bilgilerini ekle
                card.add(infoPanel, BorderLayout.SOUTH);
            }
        }
    }
}