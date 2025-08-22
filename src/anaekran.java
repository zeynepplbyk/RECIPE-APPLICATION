import javax.swing.*;
import java.awt.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class anaekran extends JFrame {
    // Veritabanı bilgileri
    private String dbName = "TarifDB";
    private String user = "postgres";
    private String password = "1";
    private Connection connection;
    private JPanel recipePanel; // Tariflerin kartlarla gösterileceği panel
    private DefaultListModel<String> categoryModel;
    private JList<String> categoryList;
    private JTextField searchField; // Arama alanı
    private RecipeManager recipeManager; // Tarif yönetimi için sınıf
    private SearchRecipe searchRecipe; // Arama işlevi
    private RecipeFunctions recipeFunctions; // Tarif fonksiyonları
    private Filtreleme filtreleme; // Filtreleme işlemi
    private List<String> selectedIngredients; // Seçilen malzemelerin listesi


    public anaekran() {
        // Ana pencere ayarları
        setTitle("BiTarif");
        setSize(1000, 700);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // DbFunctions sınıfı ile veritabanı bağlantısı
        DbFunctions dbFunctions = new DbFunctions();
        connection = dbFunctions.connect_to_db(dbName, user, password);

        // Tarif ve kategori yönetimi sınıfları
        recipeManager = new RecipeManager(dbFunctions);
        recipePanel = new JPanel(new GridLayout(0, 3, 10, 10)); // 3 sütunlu tarif paneli
        categoryModel = new DefaultListModel<>();
        searchRecipe = new SearchRecipe(connection, recipePanel);
        recipeFunctions = new RecipeFunctions(connection, recipePanel, categoryModel);
        filtreleme = new Filtreleme(connection, recipePanel);

        // Tarifleri ve kategorileri yükle
        recipeFunctions.tarifleriYukle();
        recipeFunctions.kategorileriYukle();

        // Üst panel: Arama ve filtreleme
        JPanel topPanel = new JPanel(new FlowLayout());
        topPanel.setBackground(new Color(240, 240, 240));

        searchField = new JTextField(20);
        JButton searchButton = new JButton("Ara");
        JButton filterButton = new JButton("Filtrele");
        JButton ingredientsButton = new JButton("Malzemelerim");

        Color buttonColor1 = new Color(200, 50, 80); // Arka plan rengi
        Color textColor1 = new Color(255, 150, 180); // Yazı rengi, categoryList arka plan rengi ile aynı
        Font buttonFont1 = new Font("Arial", Font.BOLD, 14); // Yazı tipi ve boyutu


        searchButton.setBackground(buttonColor1);
        searchButton.setForeground(textColor1);
        searchButton.setFont(buttonFont1);

        filterButton.setBackground(buttonColor1);
        filterButton.setForeground(textColor1);
        filterButton.setFont(buttonFont1);

        ingredientsButton.setBackground(buttonColor1);
        ingredientsButton.setForeground(textColor1);
        ingredientsButton.setFont(buttonFont1);



        topPanel.add(searchField);
        topPanel.add(searchButton);
        topPanel.add(filterButton);
        topPanel.add(ingredientsButton);

        // Sol menü paneli (kategoriler için)
        JPanel menuPanel = new JPanel(new BorderLayout());
        menuPanel.setPreferredSize(new Dimension(200, getHeight()));
        menuPanel.setBackground(Color.RED);

        // Kategori listesi
        categoryList = new JList<>(categoryModel);
        categoryList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        categoryList.addListSelectionListener(e -> filterRecipesByCategory(categoryList.getSelectedValue()));

        categoryList.setFixedCellHeight(40);
        categoryList.setCellRenderer(new CustomListCellRenderer());

        // Stil ayarları
        categoryList.setBackground(new Color(255, 150, 180));
        categoryList.setForeground(Color.WHITE);
        categoryList.setSelectionBackground(new Color(200, 200, 200));
        categoryList.setSelectionForeground(Color.WHITE);
        categoryList.setFont(new Font("Arial", Font.ITALIC, 20));

        // Sol menüye kategori listesi ekleyelim
        JScrollPane categoryScrollPane = new JScrollPane(categoryList);
        categoryScrollPane.setPreferredSize(new Dimension(500, 400));
        menuPanel.add(categoryScrollPane, BorderLayout.CENTER);

        // Tarif ekle/güncelle/sil butonları
        // Tarif ekle/güncelle/sil butonları
        JPanel buttonPanel = new JPanel(new GridLayout(3, 1, 5, 5));
        JButton addTarifButton = new JButton("Tarif Ekle");
        JButton updateTarifButton = new JButton("Tarif Güncelle");
        JButton deleteTarifButton = new JButton("Tarif Sil");

// Buton yazı ve arka plan renk ayarları
        Color textColor = new Color(255, 150, 180);
        Font buttonFont = new Font("Arial", Font.BOLD, 16);

        addTarifButton.setBackground(Color.WHITE);
        addTarifButton.setForeground(textColor);
        addTarifButton.setFont(buttonFont);

        updateTarifButton.setBackground(Color.WHITE);
        updateTarifButton.setForeground(textColor);
        updateTarifButton.setFont(buttonFont);

        deleteTarifButton.setBackground(Color.WHITE);
        deleteTarifButton.setForeground(textColor);
        deleteTarifButton.setFont(buttonFont);

        buttonPanel.add(addTarifButton);
        buttonPanel.add(updateTarifButton);
        buttonPanel.add(deleteTarifButton);
        menuPanel.add(buttonPanel, BorderLayout.SOUTH);


        // Buton işlevleri
        addTarifButton.addActionListener(e -> {
            recipeManager.tarifEkle();
            recipeFunctions.tarifleriYukle();
        });
        updateTarifButton.addActionListener(e -> {
            recipeManager.tarifGuncelle();
            recipeFunctions.tarifleriYukle();
        });
        deleteTarifButton.addActionListener(e -> {
            recipeManager.tarifSil();
            recipeFunctions.tarifleriYukle();
        });
        filterButton.addActionListener(e -> filtreleme.showFilterOptions(recipePanel));

        ingredientsButton.addActionListener(e -> {
            MalzemeSecimi malzemeSecimi = new MalzemeSecimi(this, connection);
            malzemeSecimi.setVisible(true);
        });

        // Tariflerin gösterileceği panel
        JScrollPane scrollPane = new JScrollPane(recipePanel);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        // Ana pencereye menü ve tarif panellerini ekle
        getContentPane().add(topPanel, BorderLayout.NORTH);
        getContentPane().add(menuPanel, BorderLayout.WEST);
        getContentPane().add(scrollPane, BorderLayout.CENTER);

        // Arama işlevi
        searchButton.addActionListener(e -> filterRecipes());
        searchField.addActionListener(e -> filterRecipes());

        setVisible(true);
    }

    // Kategoriye göre tarifleri filtreleme
    private void filterRecipesByCategory(String kategori) {
        if (kategori != null && !kategori.isEmpty()) {
            recipeFunctions.filterRecipesByCategory(kategori);
        } else {
            System.out.println("Geçersiz kategori");
        }
    }

    // Arama işlevi
    private void filterRecipes() {
        String searchTerm = searchField.getText().trim();
        searchRecipe.searchRecipes(searchTerm);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(anaekran::new);
    }

    private class CustomListCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
            JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            label.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); // Boşluk ekle
            return label;
        }
    }

}