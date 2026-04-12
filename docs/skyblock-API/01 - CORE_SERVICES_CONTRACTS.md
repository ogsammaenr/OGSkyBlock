# Core Services & API Contracts Teknik Tasarım Dokümanı (TDD)

## 1. Kapsam ve Genel Bakış
Bu doküman, `skyblock-api` dizini altında yer alacak olan çekirdek servislerin ve API sözleşmelerinin (Interfaces) mimari sınırlarını çizer. Sistemimiz, Bağımlılıkların Ters Çevrilmesi (Dependency Inversion) ve Bağlantı Noktaları ve Adaptörler (Ports and Adapters) prensiplerine göre tasarlanmıştır. Bu sözleşmeler, iş mantığını (Domain Logic) altyapıdan tamamen izole ederek sistemin esnekliğini korur. Programlama dili bağımsız olarak, sadece sistemin "Fiillerini" (Davranışlarını) tanımlar.

## 2. Veri Mimarisi (Data Architecture)
Veri akışı katı bir şekilde yukarıdan aşağıya (Top-Down) kurgulanmıştır. Sunucu komutları doğrudan veritabanı ile konuşamaz. Tüm okuma ve yazma işlemleri önbellek (Cache) katmanı üzerinden geçer.

Veri Akış Döngüsü (Write-Behind Flow):
1. **Okuma:** İş Mantığı -> Önbellek -> (Yoksa) -> Veritabanı -> RAM'e yükle.
2. **Yazma (In-Memory):** İş Mantığı -> RAM'deki nesneyi güncelle -> `isDirty` bayrağını aktif et.
3. **Senkronizasyon (Asenkron):** Arka plan zamanlayıcısı (Scheduler) uyanır -> Önbellekten `isDirty` olanları toplar -> Toplu halde (Batch) Veritabanına yazar -> Bayrakları temizler.

## 3. Çekirdek Arayüzler ve Sözleşmeler (Core Interfaces & Contracts)
Sistemin çalışması için gereken dört ana sözleşme (Interface) ve bu sözleşmelerin garanti etmesi gereken davranışlar aşağıda listelenmiştir.

### A. Island Repository (Veri Kaynağı Sözleşmesi)
**Rolü:** Sistemin dış dünya (SQLite, PostgreSQL vb.) ile konuşmasını sağlayan bağlantı noktasıdır (Outbound Port). Bu arayüz, verinin "nasıl" kaydedildiğiyle ilgilenmez, sadece "kaydedilmesi gerektiğini" bilir.
**Sözleşme Metotları:**
* `loadIsland (Ada ID)`: Verilen kimliğe sahip adayı kalıcı depolamadan bulur ve nesne olarak döndürür.
* `loadIslandByOwner (Oyuncu ID)`: Verilen oyuncunun sahip olduğu adayı bulur.
* `saveIsland (Ada Nesnesi)`: Tekil bir adanın güncel durumunu kalıcı depolamaya yazar.
* `saveBatch (Ada Listesi)`: Write-Behind senkronizasyonu için kritik metottur. Verilen ada listesini tek bir "Transaction" (Toplu işlem) içinde veritabanına yazar. Disk I/O darboğazını engeller.
* `deleteIsland (Ada ID)`: Adayı ve ona bağlı tüm alt verileri (üyeler, deltalar) kalıcı depolamadan siler (Cascade Delete mantığı).

### B. Island Cache Service (Önbellek Sözleşmesi)
**Rolü:** RAM içi veri yönetimini sağlayan iç bağlantı noktasıdır. Adaların çalışma zamanındaki yaşam döngüsünü kontrol eder.
**Sözleşme Metotları:**
* `getCachedIsland (Ada ID)`: Ada bellekteyse anında döndürür, değilse boş döner (Veritabanına gitmez).
* `cacheIsland (Ada Nesnesi)`: Veritabanından çekilen veya yeni oluşturulan adayı belleğe alır.
* `invalidateIsland (Ada ID)`: Adayı bellekten zorla siler (Örn: oyuncular adadan çıkınca).
* `getDirtyIslands ()`: `isDirty` bayrağı aktif olan (değişikliğe uğramış) tüm adaların listesini döndürür. Zamanlayıcı (Scheduler) tarafından kullanılır.

### C. Island Service (İş Mantığı Sözleşmesi)
**Rolü:** Sunucu komutlarının veya arayüzlerin (UI) sistemi tetiklemek için kullanacağı tek giriş noktasıdır (Inbound Port / Use Case). Tüm iş kuralları (Business Rules) ve güvenlik kontrolleri burada yapılır.
**Sözleşme Metotları:**
* `createIsland (Sahip ID)`: Yeni bir ada nesnesi oluşturur, varsayılan deltaları tanımlar, önbelleğe ekler ve asenkron kaydetme döngüsüne dahil eder.
* `addMemberToIsland (Ada ID, Oyuncu ID, Rol Ağırlığı)`: Belirtilen adayı önbellekten bulur, yeni üyeyi ekler ve adanın `isDirty` bayrağını tetikler.
* `removeMemberFromIsland (Ada ID, Oyuncu ID)`: Üyeyi adadan çıkartır ve `isDirty` bayrağını tetikler.
* `updatePermissionWeight (Ada ID, Aksiyon Enum, Yeni Ağırlık)`: Adanın delta yetki byte dizisini günceller. Maskeleme işlemleri başarılı olursa `isDirty` bayrağını tetikler.
* `triggerAsyncSave ()`: Sistem kapanırken veya acil durumlarda Write-Behind döngüsünü beklemeden tüm "kirli" adaları anında veritabanına yazdırmak için kullanılır.

### D. Template Registry (Şablon Kayıt Sözleşmesi)
**Rolü:** Sistemdeki ada şablonlarının (Templates) matematiksel metadatalarını (merkez ofsetleri, doğma noktaları) çalışma zamanında (RAM) tutan veri sözleşmesidir. Core ve Adaptör (Fabric) katmanları arasındaki şablon köprüsünü oluşturur.
**Sözleşme Metotları:**
* `registerTemplate (Şablon Nesnesi)`: Dış adaptör (Fabric) tarafından dosya sisteminden okunup hesaplanan şablon metadatasını Core modülünün belleğine kaydeder.
* `getTemplate (Şablon ID)`: İstenilen şablonun verilerini O(1) hızında döndürür.
* `getDefaultTemplate ()`: Özel bir şablon belirtilmediğinde kullanılacak varsayılan şablonu (Örn: 'basic') döndürür.
* `hasTemplate (Şablon ID)`: Belirtilen isimde bir şablonun RAM'de kayıtlı olup olmadığını kontrol eder. (Hata yönetimi ve Fail-Safe/Güvenli Çöküş mekanizması için kullanılır).

## 4. Olaylar ve Komutlar (Events & Commands)
Sistem arayüzleri, komutlar üzerinden tetiklendiğinde olay güdümlü (Event-Driven) bir haberleşme başlatır.

* **Etkileşim Akışı (Sequence Örneği):**
  1. Oyuncu `/is invite <oyuncu>` komutunu girer.
  2. Komut yakalayıcı (Command Handler), `IslandService` üzerindeki `addMemberToIsland` metodunu çağırır.
  3. İş mantığı, `IslandCacheService` üzerinden adayı RAM'den çeker, yeni üyeyi ekler ve `isDirty` bayrağını aktif eder.
  4. Sistem içi `IslandMemberAddedEvent` olayı fırlatılır (Hologramları veya UI'ı anında güncellemek için).
  5. Zamanlayıcı döngüsü geldiğinde, `IslandRepository.saveBatch()` tetiklenir ve veri diskteki SQLite'a yansır.

## 5. Riskler ve Ödünleşimler (Risks & Trade-offs)
* **Risk (Eventual Consistency & Data Loss):** Sistem "Write-Behind" (Gecikmeli Yazma) kullandığı için, `IslandService` sadece RAM'deki `Island` nesnesini günceller. Eğer `saveBatch()` çalışmadan hemen önce sunucu fiziksel olarak çökerse, RAM'de bekleyen son dakika değişiklikleri kaybolacaktır. Bu risk, yüksek performans için kabul edilmiştir.
* **Ödünleşim (Cold Start vs. Pre-loading):** Sunucu ilk açıldığında önbellek boştur. Oyuncular bağlandıkça veri tabanından tekil okumalar (Cold Start) yapılır. İlk girişlerde mikro saniyelik gecikmeler olabilir, ancak 10.000 adayı baştan RAM'e yüklemekten (Pre-loading) kaynaklanacak devasa bellek tüketiminden kaçınılmıştır.