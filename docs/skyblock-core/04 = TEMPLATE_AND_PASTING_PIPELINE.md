# Şablon ve Yapıştırma Boru Hattı (Template & Pasting Pipeline) Teknik Tasarım Dokümanı (TDD)

## 1. Kapsam ve Genel Bakış
Bu doküman, Skyblock sistemindeki ada şablonlarının (NBT) okunması ve oyuncu talebi üzerine dünyaya gecikmesiz (lag-free) bir şekilde inşa edilmesi sürecini tanımlar. Önceki mimari kararlarımız doğrultusunda "Hibrit Sorumluluk" modeli benimsenmiştir: Çekirdek (Core) modül, uzamsal hesaplamaları ve yetkileri yönetirken; adaptör (Fabric) katmanı, Minecraft'ın yerleşik `StructureTemplate` yapısını kullanarak ağır blok yerleştirme işlemlerini üstlenir. Bu boru hattı, TPS (Tick Per Second) düşüşlerini sıfıra indirmek için asenkron kotalı yerleştirme (throttled pasting) ve hata durumlarında hayalet adaları önlemek için katı bir geri alma (rollback) mekanizması içerir.

## 2. Veri Mimarisi (Data Architecture)
Boru hattı boyunca veri, platform bağımlı NBT formatından soyutlanarak saf veri transfer nesnelerine (DTO) dönüştürülür. Toleranslı ayrıştırma (Fallback Parsing) prensibi sayesinde, hatalı NBT'ler sunucuyu çökertmez.

```yaml
# Fabric'ten Core Modülüne Aktarılan Saf Veri (DTO) Şeması
TemplateMetadataDTO:
  id: "basic_grass"          # templates.yml dosyasından alınır
  settings:
    displayName: "&aTemel Ada"
    icon: "minecraft:grass_block"
    permissionNode: ""
  dimensions:                # NBT'den okunan boyutlar
    width: 32
    height: 15
    length: 32
  offsets:
# Eğer NBT'de bedrock yoksa, tolerans devreye girer: x=(width/2), y=0, z=(length/2)
    bedrockVector: { x: 16, y: 0, z: 16 }
# Eğer NBT'de [SPAWN] tabelası yoksa: x=(bedrock.x), y=(bedrock.y + 1), z=(bedrock.z)
    spawnVector: { x: 16, y: 1, z: 16 }
```

Oluşturma sürecindeki geçici durum yönetimi:

```yaml
# Adanın Bellek Üzerindeki İşlem Durumu (State Machine)
IslandCreationState:
  islandId: "uuid-1234"
  status: "CREATION_PENDING" # İşlem bitene kadar diğer etkileşimlere kapalıdır
  targetCenter: { x: 1000, y: 0, z: 1000 }
  allocatedTemplate: "basic_grass"
```

## 3. Çekirdek Arayüzler ve Sınıflar (Core Interfaces & Classes)
Mimaride kod blokları yerine olay güdümlü adaptör sözleşmeleri (Port & Adapters) esastır. Süreci yöneten üç temel sözleşme yapısı şunlardır:

```yaml
# 1. Kayıt Kapısı (Inbound Port)
TemplateRegistrar:
  sorumluluk: "Fabric'in açılışta taradığı DTO'ları RAM'e kaydetmek."
  parametreler: [ TemplateMetadataDTO ]

# 2. Görev Dağıtıcı (Outbound Port)
PastingEngineAdapter:
  sorumluluk: "Core modülünün Fabric'e 'Şu şablonu şuraya yapıştır' emrini iletmesi."
  parametreler: [ TemplateId, TargetVector3i ]
  donus_tipi: "Asenkron Başarı/Hata Sinyali (Promise/CompletableFuture)"

# 3. Kotalı Yapıştırıcı (Fabric Internal)
ThrottledPastingTask:
  sorumluluk: "TPS düşürmemek için StructureTemplate bloklarını zamana veya hacme yaymak."
  ayarlar:
    blocksPerTick: 500       # Config-Driven: Sunucu sahibi bunu ayarlayabilir
    disablePhysics: true     # Su/Kum düşmesini engelleyerek lagı önler
```

## 4. Olaylar ve Komutlar (Events & Commands)
Yaşam döngüsü 5 kesin adımdan oluşan bir işlem sırasına (Atomicity) sahiptir:

1. **Açılış ve Ayrıştırma (Boot & Parse):** Sunucu açılır, Fabric `templates.yml` ve NBT dosyalarını okur, metadataları çıkartır (tolerans kurallarını uygular) ve Core modülündeki `TemplateRegistry`'ye kaydeder.
2. **Talep (Command):** Oyuncu `/is create basic_grass` yazar. Çekirdek modül, sarmal algoritma (Spatial Grid) ile boş bir merkez koordinatı bulur.
3. **Kilitleme (Locking):** `Island` nesnesi `CREATION_PENDING` statüsünde belleğe alınır.
4. **Fiziksel İnşa (Throttled Pasting):** Çekirdek, `PastingEngineAdapter` üzerinden Fabric'e emir gönderir. Fabric, ilgili `StructureTemplate` verisini alır ve `blocksPerTick` kotasına uyarak (örn: her sunucu tikinde 500 blok) asenkron olarak dünyaya yerleştirir.
5. **Sonuç (Callback & Event):**
    * **Başarılı:** Fabric işlem bittiğinde `Promise`'i tamamlar. Core, ada durumunu `ACTIVE` yapar, `isDirty = true` ile veritabanı yazım sırasına ekler ve oyuncuyu `spawnVector`'e ışınlar.
    * **Hatalı (Rollback):** Dosya bozukluğu veya engel durumunda Fabric hata fırlatır. Core modülü `CREATION_PENDING` statüsündeki adayı RAM'den siler, tahsis edilen grid koordinatını serbest bırakır ve oyuncuya hata mesajı gösterir (Hayalet ada önlenir).

## 5. Riskler ve Ödünleşimler (Risks & Trade-offs)
* **Ödünleşim (Zaman vs. Performans):** Yapıştırma işleminin sunucu performansını (TPS) etkilememesi için `blocksPerTick` kotası kullanılır. 10.000 blokluk devasa bir VIP şablonunun oluşturulması (saniyede 20 tick * 500 blok = 10.000) tam 1 saniye sürebilir. Bu "gecikme", sunucunun donmamasının (lag-free) bedelidir ve UX açısından kabul edilebilir.
* **Risk (Orphaned / Yarım Adalar):** Eğer asenkron yapıştırma işlemi sırasında sunucu aniden çökerse (Crash/Power Loss), dünyada yarım bir ada kalacaktır. Veritabanında ise `isDirty` bayrağı asenkron olarak yazılmadığı için ada hiç var olmamış kabul edilecektir. Bu senaryo "Eventual Consistency" (Nihai Tutarlılık) doğası gereği kabul edilmiş bir risktir; sunucu açıldığında bu koordinat tekrar başka bir adaya tahsis edildiğinde, yeni şablon eski yarım adanın üzerine (Override) yazılarak alanı temizleyecektir.
* **Ödünleşim (Fizik Güncellemeleri):** Yapıştırma sırasında Vanilla fizik kuralları (Block Updates) kapatılır (`disablePhysics: true`). Bu işlem hız kazandırır ancak NBT içindeki kızıltaş (redstone) devrelerinin veya bazı observer bloklarının ilk başta çalışmamasına sebep olabilir.