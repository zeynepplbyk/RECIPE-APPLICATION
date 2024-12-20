import javax.swing.*;
import java.awt.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MalzemeSecimi extends JDialog {
    private JPanel ingredientsPanel; // Malzeme paneli
    private List<JCheckBox> checkBoxes; // Seçenekler için checkbox'lar
    private Connection connection;

    public MalzemeSecimi(Frame parent, Connection connection) {
        super(parent, "Malzeme Seçimi", true);
        this.connection = connection;

        ingredientsPanel = new JPanel();
        ingredientsPanel.setLayout(new BoxLayout(ingredientsPanel, BoxLayout.Y_AXIS)); // Dikey yerleşim
        checkBoxes = new ArrayList<>();

        loadIngredients(); // Malzemeleri yükle

        JButton applyButton = new JButton("Uygula");
        JButton cancelButton = new JButton("İptal");
        applyButton.addActionListener(e -> applySelections());
        cancelButton.addActionListener(e -> dispose());

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(applyButton);
        buttonPanel.add(cancelButton);

        add(new JScrollPane(ingredientsPanel), BorderLayout.CENTER); // Malzemeleri panel içerisinde kaydırma ile göster
        add(buttonPanel, BorderLayout.SOUTH);

        setSize(400, 300);
        setLocationRelativeTo(parent);
    }

    private void loadIngredients() {
        try {
            String query = "SELECT malzemeadi FROM malzemeler"; // Malzeme adını almak için sorgu
            PreparedStatement statement = connection.prepareStatement(query);
            ResultSet resultSet = statement.executeQuery();

            ingredientsPanel.removeAll(); // Önceki içerikleri temizle
            checkBoxes.clear(); // Checkbox listesini temizle
            while (resultSet.next()) {
                String ingredient = resultSet.getString("malzemeadi");
                JCheckBox checkBox = new JCheckBox(ingredient);
                checkBoxes.add(checkBox); // Yeni checkbox'ı listeye ekle
                ingredientsPanel.add(checkBox); // Checkbox'ı panel içine ekle
            }
            ingredientsPanel.revalidate(); // Panelin yeniden düzenlenmesini sağla
            ingredientsPanel.repaint(); // Paneli yeniden çiz
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Malzemeler yüklenirken bir hata oluştu: " + e.getMessage(), "Hata", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void applySelections() {
        List<String> selectedIngredients = new ArrayList<>();
        for (JCheckBox checkBox : checkBoxes) {
            if (checkBox.isSelected()) {
                selectedIngredients.add(checkBox.getText()); // Seçili olan malzemelerin adını al
            }
        }

        // Seçilen malzemelerin ID'lerini al ve tarifleri göster
        showSelectedIngredientIds(selectedIngredients);
    }

    private void showSelectedIngredientIds(List<String> selectedIngredients) {
        StringBuilder selectedIds = new StringBuilder("Seçilen Malzeme Eşleşmeleri:\n");
        try {
            Set<Integer> displayedRecipeIds = new HashSet<>();

            for (String ingredient : selectedIngredients) {
                String query = "SELECT malzemeid FROM malzemeler WHERE malzemeadi = ?";
                PreparedStatement statement = connection.prepareStatement(query);
                statement.setString(1, ingredient);
                ResultSet resultSet = statement.executeQuery();

                while (resultSet.next()) {
                    int malzemeId = resultSet.getInt("malzemeid");

                    String recipeQuery = "SELECT tarifid FROM tarifmalzeme WHERE malzemeid = ?";
                    PreparedStatement recipeStatement = connection.prepareStatement(recipeQuery);
                    recipeStatement.setInt(1, malzemeId);
                    ResultSet recipeResultSet = recipeStatement.executeQuery();

                    while (recipeResultSet.next()) {
                        int tarifId = recipeResultSet.getInt("tarifid");

                        if (displayedRecipeIds.add(tarifId)) {
                            String tarifAdQuery = "SELECT tarifadi FROM tarifler WHERE tarifid = ?";
                            PreparedStatement tarifAdStatement = connection.prepareStatement(tarifAdQuery);
                            tarifAdStatement.setInt(1, tarifId);
                            ResultSet tarifAdResultSet = tarifAdStatement.executeQuery();

                            String tarifAd = "";
                            if (tarifAdResultSet.next()) {
                                tarifAd = tarifAdResultSet.getString("tarifadi");
                            }

                            // Toplam malzeme sayısını al
                            String countQuery = "SELECT COUNT(*) AS malzemeSayisi FROM tarifmalzeme WHERE tarifid = ?";
                            PreparedStatement countStatement = connection.prepareStatement(countQuery);
                            countStatement.setInt(1, tarifId);
                            ResultSet countResultSet = countStatement.executeQuery();

                            int totalIngredientsCount = 0;
                            if (countResultSet.next()) {
                                totalIngredientsCount = countResultSet.getInt("malzemeSayisi");
                            }

                            // Eşleşen malzeme sayısını hesapla
                            int matchingIngredientsCount = 0;
                            for (String selectedIngredient : selectedIngredients) {
                                String matchingQuery = "SELECT COUNT(*) AS matchedCount FROM tarifmalzeme tm " +
                                        "JOIN malzemeler m ON tm.malzemeid = m.malzemeid " +
                                        "WHERE tm.tarifid = ? AND m.malzemeadi = ?";
                                PreparedStatement matchingStatement = connection.prepareStatement(matchingQuery);
                                matchingStatement.setInt(1, tarifId);
                                matchingStatement.setString(2, selectedIngredient);
                                ResultSet matchingResultSet = matchingStatement.executeQuery();

                                if (matchingResultSet.next()) {
                                    matchingIngredientsCount += matchingResultSet.getInt("matchedCount");
                                }
                            }

                            // Yüzdeyi hesapla
                            double percentage = (double) matchingIngredientsCount / totalIngredientsCount * 100;

                            // Tarif adı ve eşleşme yüzdesini ekle
                            selectedIds.append("Tarif Adı: ").append(tarifAd)
                                    .append(", Eşleşme Yüzdesi: ").append(String.format("%.2f", percentage)).append("%\n");
                        }
                    }
                }
            }
            JOptionPane.showMessageDialog(this, selectedIds.toString(), "Seçilen Malzeme Eşleşmeleri", JOptionPane.INFORMATION_MESSAGE);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Malzeme eşleşmeleri yüklenirken bir hata oluştu: " + e.getMessage(), "Hata", JOptionPane.ERROR_MESSAGE);
        }
    }


}
