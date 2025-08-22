# Tarif Rehberi Uygulaması 🍲 - Recipe Application


---

## Özet
"Tarif Rehberi Uygulaması," kullanıcıların yemek tariflerini düzenli bir şekilde saklamalarına ve mevcut malzemeleriyle hangi yemekleri yapabileceklerini kolayca görmelerine yardımcı olan bir masaüstü uygulamasıdır.  

Uygulama; dinamik arama ve filtreleme, veritabanı yönetimi, kullanıcı dostu arayüz tasarımı ve tariflerin malzeme durumuna göre renk kodlaması gibi özellikler sunar. Eksik malzemeleri olan tarifler kırmızı, yeterli malzemeleri olanlar yeşil olarak gösterilir.  

---

## Özellikler
- Tarif ekleme, güncelleme ve silme  
- Malzemelere göre dinamik tarif önerisi  
- Malzeme yetersizliği durumunda tarifin kırmızı renkte gösterilmesi  
- Tariflerin kategori, hazırlama süresi ve maliyete göre filtrelenmesi  
- Duplicate kontrolü ile aynı tarifin tekrar eklenmesinin önlenmesi  
- Kullanıcı dostu GUI (Grafiksel Kullanıcı Arayüzü)  

---

## Kullanılan Teknolojiler
- Java Swing (GUI tasarımı)  
- SQLite (Veritabanı)  
- Algoritmalar ve filtreleme sistemi  

---

## Veritabanı Tasarımı

**Tablolar:**

### 1. Tarifler
- TarifID (Primary Key)  
- TarifAdi  
- Kategori  
- HazirlamaSuresi  
- Talimatlar  

### 2. Malzemeler
- MalzemeID (Primary Key)  
- MalzemeAdi  
- ToplamMiktar  
- MalzemeBirim  
- BirimFiyat  

### 3. Tarif-Malzeme İlişkisi
- TarifID (Foreign Key)  
- MalzemeID (Foreign Key)  
- MalzemeMiktar  

> Veritabanı normalizasyon kurallarına uygun şekilde tasarlanmış ve many-to-many ilişkiler doğru şekilde temsil edilmiştir.  

---

## Kurulum
1. Depoyu klonlayın:  
```bash
git clone https://github.com/zeynepplbyk/RECIPE-APPLICATION.git
