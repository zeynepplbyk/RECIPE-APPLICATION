# RECIPE-APPLICATION

<img width="1470" alt="Ekran Resmi 2024-12-20 19 15 57" src="https://github.com/user-attachments/assets/71148c8a-7f28-414f-84b6-1173ab803d27" />




"Tarif Rehberi Uygulaması," kullanıcıların yemek
tariflerini düzenli bir şekilde saklamalarına ve mevcut
malzemeleriyle hangi yemekleri yapabileceklerini
kolayca görmelerine yardımcı olmayı amaçlamaktadır.Bu
uygulama, dinamik arama ve filtreleme özellikleri
sayesinde kullanıcıların aradıkları tariflere hızlı bir
şekilde ulaşmalarını sağlar. Veritabanı yönetimi ile
entegre çalışarak, tariflerin ve malzemelerin etkili bir
şekilde saklanmasını ve güncellenmesini mümkün kılar.
Ayrıca, kullanıcı dostu bir arayüz tasarımıyla, her
seviyeden kullanıcının rahatlıkla erişebileceği bir
deneyim sunmaktadır.
Uygulama, yemek tariflerinin yanı sıra, eksik
malzemeleri olan tarifleri belirleyerek kullanıcıların
pişirme sürecini daha verimli hale getirir. Bu sayede,
kullanıcılar hem zaman kazanacak hem de yaratıcı
mutfak deneyimlerinin kapılarını aralayacaklardır. Tarif
Rehberi, hem beslenme alışkanlıklarını geliştirmek hem
de mutfak becerilerini artırmak isteyen herkes için
vazgeçilmez bir yardımcı olmayı hedeflemektedir.
Tarif Rehberi Uygulaması'nın geliştirilmesi sürecinde
izlenen yöntemler aşağıda detaylandırılmıştır:
1. Analiz ve Planlama
◦ Uygulamanın gereksinimleri
belirlenecek ve hedef kullanıcı kitlesi
analiz edildi. Kullanıcıların ihtiyaçları,
beklentileri ve uygulamanın kullanım
senaryoları üzerine odaklanıldı.
◦ Gerekli işlevlerin ve özelliklerin
tanımlanması için bir gereksinim listesi
oluşturuldu.
2. Veritabanı Tasarımı
◦ Tarifler, malzemeler ve tarif-malzeme
ilişkilerini yönetmek için ilişkisel bir
veritabanı tasarlandı. Bu aşamada
aşağıdaki tablolar oluşturuldu:
▪ Tarifler Tablosu: TarifID,
TarifAdi, Kategori,
HazirlamaSuresi, Talimatlar gibi
alanları içerir.
▪ Malzemeler Tablosu:
MalzemeID, MalzemeAdi,
ToplamMiktar, MalzemeBirim,
BirimFiyat gibi alanlar yer aldı.
▪ Tarif-Malzeme İlişkisi
Tablosu: TarifID, MalzemeID
ve MalzemeMiktar bilgilerini
tuttu.
◦ Tablolar, veritabanı normalizasyon
kurallarına göre tasarlandı, many-to-
many ilişkiler doğru bir şekilde temsil
edildi.
