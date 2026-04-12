# TDD-001: Genel Mimari Özeti (OVERVIEW.md)

**Belge Durumu:** Onaylandı (V1.1)  
**Proje Kapsamı:** Fabric API tabanlı, modüler, yüksek performanslı ve özelleştirilebilir Skyblock Sunucu Çekirdeği.  
**Hedef Kitle:** Çekirdek Geliştiricileri, Eklenti (Addon) Geliştiricileri ve Sunucu Yöneticileri.

---

## 1. Proje Özeti ve Vizyon (Executive Summary)

Bu proje, Minecraft (Fabric) ekosistemi için tasarlanmış kurumsal seviyede bir Skyblock sistemidir. Temel vizyonumuz; sunucu donmalarını (TPS lag) tamamen ortadan kaldıran yüksek performanslı bir çekirdek yazarken, aynı zamanda hem sunucu sahipleri hem de diğer eklenti geliştiricileri için muazzam bir esneklik sunmaktır.

Sistem, "Spagetti Kod" oluşumunu engelleyen katı ayrıştırılmış modüllerden oluşur. Kodun içine gömülü (hardcoded) kurallar yerine, tamamen **Veri Odaklı (Data-Driven)** ve **Yapılandırma Odaklı (Config-Driven)** bir yaklaşım benimsenmiştir.

---

## 2. Teknoloji Yığını ve Geliştirme Standartları (Tech Stack)

Açık kaynaklı ve genişletilebilir bir ekosistem yaratmak adına aşağıdaki küresel standartlar belirlenmiştir:

* **Çekirdek Platform:** Fabric API.
* **Dil Sürümü:** Java 21 (Modern Minecraft sürümleri ile tam uyumluluk ve performans artışı için).
* **Eşleştirme (Mappings):** **Mojang Mappings**. Projeye eklenti yazacak diğer geliştiricilerin Vanilla (orijinal) Minecraft kodlarına daha kolay ve standart bir yoldan erişebilmesi için `Yarn` yerine Mojang standartları tercih edilmiştir.
* **Konfigürasyon Formatı:** **HOCON** (veya desteklenen JSON türevleri). Sunucu sahiplerinin ayar dosyalarının içine yorum satırları yazabilmesi ve okunabilirliğin artırılması hedeflenmiştir.

---

## 3. Temel Mimari Prensipler (Core Principles)

Sistemin esnekliğini ve dayanıklılığını korumak için aşağıdaki mimari kurallardan asla taviz verilmeyecektir:

### 3.1. Modüler Monolit (Modular Monolith) & Jar-in-Jar
Proje, geliştirme aşamasında birbirinden tamamen izole edilmiş alt modüllere bölünmüştür. Ancak son kullanıcı (sunucu sahibi) deneyimini bozmamak adına Fabric'in `Jar-in-Jar` (JiJ) özelliği kullanılarak, bu modüller tek bir paket (Fat-JAR) halinde dağıtılır.

### 3.2. Olay Güdümlü İletişim (Event-Driven Architecture)
Modüllerin birbirine sıkı sıkıya bağlanmasını (Tight Coupling) önlemek için sistemdeki her kritik eylem bir Fabric Olayı (Event) olarak fırlatılır. Yeni bir özellik (örn: Görevler, Ekonomi) eklenmek istendiğinde ana koda dokunulmaz, sadece bu olaylar dinlenir (Open/Closed Principle).

### 3.3. Config-Odaklı ve Kişiselleştirilebilir Tasarım (Config-Driven)
Modun çalışma karakteristiği (oyuncu sınırları, ada limitleri, bekleme süreleri, varsayılan biyomlar, tüm mesajlar ve dil destekleri) doğrudan koddan değil, dışarıya açılmış konfigürasyon ve datapack dosyalarından yönetilir.

---

## 4. Dizin ve Dokümantasyon Hiyerarşisi

Projenin kök dizin yapısı, dokümantasyon (`docs/`) ve kaynak kod (`src/`) modüllerinin birbirini yansıtacağı endüstri standartlarına uygun bir düzende tasarlanmıştır:

```text
skyblock-project/
 ├── docs/                          (Sistem Anayasası ve Teknik Tasarım Belgesi)
 │    ├── OVERVIEW.md               (Şu an okuduğunuz belge)
 │    ├── api/                      (Domain Modelleri ve Event Sözleşmeleri)
 │    ├── core/                     (Uzamsal, Veritabanı ve Yükleme Algoritmaları)
 │    ├── protection/               (Rol ve Eylem Matrisleri, Sınır Korumaları)
 │    └── ui/                       (Komut Ağacı ve Arayüz Tasarımları)
 │
 ├── skyblock-api/                  (Alt Proje 1 - Bağımlılık YOK)
 ├── skyblock-core/                 (Alt Proje 2 - Bağımlılık: api)
 ├── skyblock-protection/           (Alt Proje 3 - Bağımlılık: api)
 └── skyblock-ui/                   (Alt Proje 4 - Bağımlılık: api)
```

---

## 5. Modül Sınırları ve Sorumluluklar (Bounded Contexts)

Her modülün kendi "Sorumluluk Alanı" vardır. Geliştirme aşamasında bu sınırların dışına çıkılması mimari bir ihlal kabul edilir (Separation of Concerns).

| Modül Adı | Temel Sorumluluk (Ne Yapar?) | Kısıtlama (Ne YAPAMAZ?) |
| :--- | :--- | :--- |
| **`skyblock-api`** | Domain nesnelerini (Modeller), Interface'leri ve Fabric Event tanımlarını barındırır. | Algoritma içeremez. Veritabanına bağlanamaz. Hiçbir iş mantığı (Business Logic) yürütmez. |
| **`skyblock-core`** | Uzamsal motoru (Grid), Asenkron Veritabanı I/O işlemlerini, NBT okumalarını ve boyut/portal yüklemelerini yönetir. | Oyuncuya arayüz gösteremez, mesaj gönderemez, Vanilla eventlerini iptal ederek (cancel) koruma sağlayamaz. |
| **`skyblock-protection`**| Rol bazlı izinleri kontrol eder. Blok kırma, patlama, sıvı yayılımı gibi Vanilla olaylarını dinler ve yetkisiz işlemleri engeller. | Yeni ada oluşturamaz, veritabanına veri yazamaz. |
| **`skyblock-ui`** | `/is` gibi komutları yönetir. Çoklu dil (i18n) sistemiyle oyunculara mesaj gönderir ve GUI (Menü) oluşturur. | Fiziksel dünyayı manipüle edemez, grid kordinatı hesaplayamaz. |