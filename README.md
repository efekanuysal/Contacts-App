# Contacts App

**Contacts App**, modern mobil geliştirme standartları kullanılarak geliştirilmiş, ölçeklenebilir, performans odaklı ve "Clean Architecture" prensiplerine sadık bir Android kişi rehberi uygulamasıdır. Cihazın yerel rehberiyle senkronize çalışabilen ve bulut tabanlı entegrasyona sahip hibrit bir yapı sunar.

## 📱 Ürün Özellikleri

Uygulama, son kullanıcı deneyimini (UX) ön planda tutan aşağıdaki temel yeteneklere sahiptir:

- **Kişi Yönetimi:** Kullanıcılar; ad, soyad, telefon numarası ve profil fotoğrafı ile yeni kişi oluşturabilir.
- **Görsel Geri Bildirim:** Başarılı kayıt işlemleri sonrasında kullanıcıya özel bir **"Lottie"** animasyonu gösterilir.
- **Listeleme ve Gruplama:** Kayıtlı kişiler "Contacts" ekranında alfabetik sıraya dizilir ve isimlerinin baş harfine göre gruplandırılarak listelenir.
- **Hızlı Aksiyonlar (Swipe):** Liste üzerindeki kişi kartları sola kaydırıldığında **"Sil"** ve **"Düzenle"** butonları açığa çıkar.
- **Yerel Rehber Entegrasyonu:** Listelenen kişi eğer cihazın kendi rehberinde de kayıtlı ise, bunu belirten görsel bir ikon kart üzerinde görüntülenir.
- **Dinamik UI:** Kişi seçildiğinde açılan profil ekranında, profil fotoğrafının **en baskın rengi analiz edilerek** görselin gölgesi dinamik olarak bu renge bürünür.
- **Düzenleme ve Senkronizasyon:** Profil ekranı üzerinden bilgiler güncellenebilir veya "Rehbere Kaydet" butonu ile kişi cihazın yerel rehberine aktarılabilir. Yapılan değişiklikler liste ekranına anlık yansır.
- **Gelişmiş Kişi Arama:** Arama motoru boşluk içeren isimleri destekler ve arama çubuğuna tıklandığında geçmiş aramalar listelenir.

## 🛠 Teknik Mimari ve Geliştirme Standartları

Proje, sürdürülebilirlik ve modülerlik hedefleri doğrultusunda **Clean Architecture** prensiplerine sıkı sıkıya bağlı kalınarak geliştirilmiştir.

### Teknoloji Yığını
- **Dil & Framework:** Kotlin, Jetpack Compose
- **Mimari Desen:** MVVM (Model-View-ViewModel), Clean Architecture
- **State Yönetimi:** Event-State Pattern (Unidirectional Data Flow)
- **Ağ Katmanı:** Retrofit, OkHttp
- **Görsel İşleme:** Coil (Caching & Loading), Palette API
- **Animasyon:** Lottie Compose

### Tasarım Prensipleri (SOLID, DRY, KISS)
- **Modülerlik:** Kod tabanı; UI bileşenleri, veri modelleri ve iş mantığı olarak kesin çizgilerle ayrıştırılmıştır.
- **Clean Code:** Karmaşık bloklar yerine, tek sorumluluğa sahip (Single Responsibility) fonksiyonlar tercih edilmiştir.
- **Repository Pattern:** Veri kaynağı (Remote API veya Local Provider) soyutlanarak iş mantığına temiz veri sunulur.

## 🚀 Performans ve Optimizasyon

Uygulama "light-weight" ve sunucuyu yormayan bir yapıda kurgulanmıştır:

- **Görsel Optimizasyonu:** Görseller sunucuya gönderilmeden önce boyutları düşürülür (Resize) ve sıkıştırılır.
- **Caching:** Coil kütüphanesi ile görseller bellek ve disk üzerinde önbelleklenerek gereksiz ağ trafiği engellenir.
- **Responsive Tasarım:** Compose kullanımı sayesinde arayüz, farklı ekran boyutlarına otomatik uyum sağlar.
