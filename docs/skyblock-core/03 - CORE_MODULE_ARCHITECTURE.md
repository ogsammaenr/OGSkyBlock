# Skyblock Core Modülü Teknik Tasarım Dokümanı (TDD)

## 1. Kapsam ve Genel Bakış
Bu doküman, eklentinin kalbi ve beyni olan `skyblock-core` modülünün mimari sınırlarını tanımlar. Bu modül; ada oluşturma algoritmalarının, üye ve yetki yönetimi iş mantığının, önbellek (Cache) yaşam döngüsünün ve veri tabanı senkronizasyonunun (Write-Behind) tek merkezidir. `skyblock-api` içerisinde tanımlanan sözleşmeleri uygular. Hedefimiz, adalarla ilgili tüm operasyonları bu modülde toplayarak, sistemi yüksek performanslı, ölçeklenebilir ve Fabric olaylarına (Events/Mixins) bağımlı olmadan çalışabilen saf bir "Motor" (Engine) haline getirmektir.

## 2. Veri Mimarisi (Data Architecture)
Çekirdek modül, verileri yönetirken "Sıcak Bellek" (Hot Cache) ve "Soğuk Depolama" (Cold Storage) ayrımını sıkı bir şekilde uygular.

* **Sıcak Bellek Yönetimi:** Çalışma zamanındaki ada nesneleri yüksek performanslı bir bellek havuzunda (In-Memory) tutulacaktır. Bu bellek alanı, veritabanından bağımsız olarak uygulamanın kendi RAM alanında yaşar.
* **Yazma Senkronizasyonu (Write-Behind):** Modül, kendi içinde bir asenkron zamanlayıcı (Scheduler) barındırır. Bu görev, sadece "kirli" (üzerinde değişiklik yapılmış) bayrağına sahip adaları periyodik olarak toplar ve yığın (Batch) halinde alt katmandaki veri deposu arayüzüne iletir.
* **Uzamsal Veri İzleme:** Adaların dünyadaki (X, Z) koordinat çakışmalarını önlemek için, oluşturulan son adanın merkez koordinatı RAM'de tutulur. Veritabanına her seferinde tüm dünyayı sorgulatmak yerine, bir sonraki adanın konumu matematiksel bir algoritma ile anında hesaplanır.

## 3. Çekirdek Sınıflar ve Sorumluluklar (Core Classes & Responsibilities)
Bu modül, API sözleşmelerini hayata geçiren Yöneticileri (Managers) ve Motorları (Engines) barındırır. Saf Java ile tasarlanacak ana birimler şunlardır:

* **İş Mantığı Yöneticisi (Island Manager):** API'deki `IslandService` sözleşmesini uygulayan ana sınıftır. Ada oluşturma, adaya oyuncu davet etme, yetki maskelerini güncelleme gibi işlemleri koordine eder. Bağımlılıkları dışarıdan enjekte (Dependency Injection) alarak çalışır.
* **Uzamsal Yerleşim Motoru (Spatial Grid Engine):** Dünyada yeni oluşturulacak adaların konumlarını çakışma olmadan hesaplayan matematiksel algoritma merkezidir. Sarmal (Spiral) veya Izgara (Grid) mantığı ile bir sonraki boş konumu üretir.
* **Koruma ve Doğrulama Motoru (Protection & Validation Engine):** Dış adaptörlerden gelen eylemleri mikrosaniye seviyesinde doğrular. Belirli bir koordinattaki eylemin, ilgili adanın üye hiyerarşisi ve yetki deltasına (Bit-mask) uygun olup olmadığını denetler.
* **Önbellek Yöneticisi (Cache Manager):** Adaların belleğe alınması, bellekten atılması (Eviction) ve "kirli" verilerin takibi süreçlerini yönetir.

## 4. Olaylar ve Komutlar (Events & Commands)
Çekirdek modül, dış adaptör katmanı (Fabric Modülü) ile çift yönlü ama gevşek bağlı (Loosely Coupled) bir iletişim kurar:

* **İçeriye Doğru Akış (Inbound - Adaptörden Çekirdeğe):**
    * Fabric komut kayıt defterinden tetiklenen eylemler, doğrudan Çekirdek modülün Yöneticilerindeki metotları çağırır.
    * Oyuncu etkileşimleri (blok kırma/koyma), Fabric tarafından yakalanır ve sadece mantıksal bir soru olarak Çekirdek modülün Koruma Motoruna iletilir. Çekirdek modül sonucu döndürür, fiziksel iptal işlemini Fabric adaptörü yapar.
* **Dışarıya Doğru Akış (Outbound - Çekirdekten Adaptöre):**
    * Çekirdek modül, kendi içinde bir eylem gerçekleştiğinde (örn: adaya üye eklendiğinde) kendi iç Olay (Event) sistemini tetikler.
    * Fabric modülü veya ileride eklenebilecek eklentiler bu olayları dinleyerek oyun dünyasındaki fiziksel değişimleri (Hologram güncellemesi, mesaj gönderimi) gerçekleştirir.

## 5. Riskler ve Ödünleşimler (Risks & Trade-offs)
* **Ödünleşim (Mimari Saflık vs. Veri Dönüşümü Maliyeti):** Çekirdek modülün içine hiçbir şekilde Minecraft veya Fabric sınıflarını (örn: `BlockPos`, `ServerPlayerEntity`) dahil etmeyeceğimiz için, dış dünyadan gelen verilerin sürekli saf Java tiplerine (X,Y,Z koordinatları veya UUID) dönüştürülmesi gerekecektir. Bu çeviri katmanı, mimari esneklik sağlarken, ufak bir işlemci yükü (overhead) oluşturur.
* **Risk (Zamanlayıcı Darboğazı):** Write-Behind senkronizasyonunu yapan asenkron döngü, eğer sunucudaki binlerce adanın aynı anda güncellenmesi durumunda devasa bir veri yığınını işlemeye çalışabilir. Bu riski hafifletmek için, RAM'den veritabanı arayüzüne gönderilecek verilerin kendi içinde parçalara (Pagination/Chunking) bölünerek iletilmesi gerekmektedir.