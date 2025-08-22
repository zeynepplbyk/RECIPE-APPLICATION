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
<img width="1470" height="956" alt="Ekran Resmi 2025-08-22 12 19 27" src="https://github.com/user-attachments/assets/7406df91-2da1-4e11-a01e-dadd62f8b778" />
<img width="468" height="246" alt="Ekran Resmi 2025-08-22 12 20 50" src="https://github.com/user-attachments/assets/b46c10fd-2620-493e-b281-f509a55d0092" />
<img width="420" height="323" alt="Ekran Resmi 2025-08-22 12 21 04" src="https://github.com/user-attachments/assets/d262e24d-1da6-4fd0-8ea0-d53d2bf1dda8" />
<img width="541" height="581" alt="Ekran Resmi 2025-08-22 12 21 17" src="https://github.com/user-attachments/assets/a6ba1399-2632-4980-b984-bb95d7650f43" />

<img width="1470" height="956" alt="Ekran Resmi 2025-08-22 12 21 57" src="https://github.com/user-attachments/assets/a8b6a4f6-eec2-4b78-8d3e-e5fbed415b6a" />


## Kurulum
1. Depoyu klonlayın:  
```bash
git clone https://github.com/zeynepplbyk/RECIPE-APPLICATION.git
