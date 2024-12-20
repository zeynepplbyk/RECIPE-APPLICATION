import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.List;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.sql.*;
import java.util.*;

public class RecipeFunctions {
    private Connection connection;
    private JPanel recipePanel;
    private DefaultListModel<String> categoryModel;
    private Map<String, Integer> selectedIngredientQuantities; // Holds selected ingredient quantities
    //private IngredientsSelection ingredientsSelection;
    public RecipeFunctions(Connection connection, JPanel recipePanel, DefaultListModel<String> categoryModel) {
        this.connection = connection;
        this.recipePanel = recipePanel;
        this.categoryModel = categoryModel;
        this.selectedIngredientQuantities = new HashMap<>(); // Initialize the map
        //this.ingredientsSelection = new IngredientsSelection(new JFrame(), connection);

    }

    public void tarifleriYukle() {
        recipePanel.removeAll(); // Mevcut tarif kartlarını temizle
        kategorileriYukle(); // Kategorileri güncelle
        generateRandomStock(); // Rastgele stok bilgilerini oluştur

        try {
            String query = "SELECT * FROM Tarifler"; // Veritabanından tüm tarifleri al
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery(query);

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
                    card.setBackground(new Color(255, 182, 193)); // Pastel Kırmızı
                } else {
                    card.setBackground(new Color(152, 251, 152)); // Pastel Yeşil
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

        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        kategorileriYukle(); // Kategorileri güncelle

        recipePanel.revalidate();
        recipePanel.repaint();
    }





    public void setSelectedIngredientQuantities(Map<String, Integer> selectedIngredients) {
        this.selectedIngredientQuantities = selectedIngredients;
    }

    // Malzemeleri ve stok miktarlarını tutacak haritalar
    private Map<String, Integer> stockQuantities = new HashMap<>();

    // Veritabanından malzeme adlarını çekip rastgele stok bilgisi oluşturun
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

    // Güncellenmiş isIngredientSufficient metodu
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
                System.out.println("Malzeme: " + malzemeAdi + ", Gerekli: " + requiredQuantity + ", Stok: " + availableStock + ", Seçilen: " + selectedIngredientQuantities.getOrDefault(malzemeAdi, 0));

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
    private double calculateMissingIngredientsCost(Map<String, Double> missingIngredients) {
        double totalCost = 0;
        try {
            for (Map.Entry<String, Double> entry : missingIngredients.entrySet()) {
                String malzemeAdi = entry.getKey();
                double requiredQuantity = entry.getValue();

                String query = "SELECT BirimFiyat FROM Malzemeler WHERE MalzemeAdi = ?";
                PreparedStatement pstmt = connection.prepareStatement(query);
                pstmt.setString(1, malzemeAdi);
                ResultSet rs = pstmt.executeQuery();

                if (rs.next()) {
                    double unitPrice = rs.getDouble("BirimFiyat");
                    totalCost += unitPrice * requiredQuantity; // Calculate total cost for this missing ingredient
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return totalCost; // Return total cost for missing ingredients
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

    public void filterRecipesByCategory(String kategori) {
        recipePanel.removeAll(); // Mevcut tarif kartlarını temizle

        try {
            // Kategoriye göre sorgu hazırlama
            String query = "SELECT * FROM Tarifler";
            if (kategori != null && !kategori.isEmpty() && !"Tüm Kategoriler".equals(kategori)) {
                query += " WHERE Kategori = ?";
            }

            PreparedStatement pstmt = connection.prepareStatement(query);
            if (kategori != null && !kategori.isEmpty() && !"Tüm Kategoriler".equals(kategori)) {
                pstmt.setString(1, kategori);
            }

            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                String ad = rs.getString("TarifAdi");
                String talimatlar = rs.getString("Talimatlar");
                int hazirlamaSuresi = rs.getInt("HazirlamaSuresi");
                String resimYolu = rs.getString("resim");
                int id = rs.getInt("tarifid");

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
                    card.setBackground(new Color(255, 150, 150)); // Pastel Kırmızı
                } else {
                    card.setBackground(new Color(152, 251, 152)); // Pastel Yeşil
                }


                // Tarif adı
                JLabel recipeLabel = new JLabel(ad, JLabel.CENTER);
                recipeLabel.setFont(new Font("Arial", Font.BOLD, 16));
                card.add(recipeLabel, BorderLayout.NORTH);

                // Resim için ayrı bir panel oluştur
                JPanel imagePanel = new JPanel();
                imagePanel.setLayout(new BorderLayout());

                // Resim ekleme
                if (resimYolu != null && !resimYolu.isEmpty()) {
                    try {
                        File resimDosyasi = new File(resimYolu);
                        if (!resimDosyasi.exists()) {
                            JLabel noImageLabel = new JLabel("Resim bulunamadı (ID: " + id + ")", JLabel.CENTER);
                            imagePanel.add(noImageLabel, BorderLayout.CENTER);
                        } else {
                            ImageIcon imageIcon = new ImageIcon(ImageIO.read(resimDosyasi));
                            imageIcon = new ImageIcon(imageIcon.getImage().getScaledInstance(200, 200, Image.SCALE_SMOOTH));
                            JLabel imageLabel = new JLabel(imageIcon);
                            imagePanel.add(imageLabel, BorderLayout.CENTER);
                        }
                    } catch (Exception e) {
                        System.out.println("Resim yüklenirken hata oluştu: " + e.getMessage());
                        e.printStackTrace();
                    }
                } else {
                    JLabel noImageLabel = new JLabel("Resim yok (ID: " + id + ")", JLabel.CENTER);
                    imagePanel.add(noImageLabel, BorderLayout.CENTER);
                }

                card.add(imagePanel, BorderLayout.CENTER);

                JPanel infoPanel = new JPanel();
                infoPanel.setLayout(new GridLayout(2, 1)); // 2 satır, 1 sütun

                JLabel costLabel = new JLabel("Maliyet: " + String.format("%f", maliyet) + " TL", JLabel.LEFT);
                JLabel timeLabel = new JLabel("Hazırlama Süresi: " + hazirlamaSuresi + " dakika", JLabel.LEFT);
                infoPanel.add(timeLabel);
                infoPanel.add(costLabel);

                card.add(infoPanel, BorderLayout.SOUTH); // Bilgileri kartın altına ekle

                // Tarife tıklandığında talimatları ve malzemeleri gösteren bir MouseListener ekle
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


                recipePanel.add(card); // Kartı panele ekle
            }

        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        recipePanel.revalidate(); // Paneli yeniden düzenle
        recipePanel.repaint(); // Paneli yeniden çiz
    }
    public void kategorileriYukle() {
        categoryModel.clear(); // Mevcut kategorileri temizle
        try {
            String query = "SELECT DISTINCT Kategori FROM Tarifler";
            PreparedStatement pstmt = connection.prepareStatement(query);//sorguyu veri tabanı üzerinde çalıştırmayı seçer
            ResultSet rs = pstmt.executeQuery();

            // "Tüm Kategoriler" seçeneği ekleyin
            categoryModel.addElement("Tüm Kategoriler");


            // Kategorileri result set'ten ekleyin
            while (rs.next()) {
                String kategori = rs.getString("Kategori");
                categoryModel.addElement(kategori); // Kategoriyi ekle
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

}