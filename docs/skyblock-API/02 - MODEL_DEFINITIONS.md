# Skyblock API: Veri Modelleri (Model Definitions) Teknik Tasarım Dokümanı (TDD)

## 1. Kapsam ve Genel Bakış
Bu doküman, sistemin "Domain Logic" (İş Mantığı) katmanında yer alan soyut veri modellerini tanımlar. Önceki iterasyondan farklı olarak, **Veritabanı (Storage/Infrastructure) katmanı tamamen bu dokümandan soyutlanmıştır.** Modeller; verinin nereye veya nasıl kaydedileceğini bilmez (Persistence Ignorance), sadece uygulamanın çalışma zamanındaki (In-Memory) durumunu, kimliklerini (Identity) ve aralarındaki sahiplik bağlarını (Aggregate Roots) temsil eder. Ayrıca, ada şablonları (Templates) bellek dostu "Flyweight" ve "Metadata" tasarım desenleriyle kurgulanmış olup, Minecraft'a özgü ağır blok verilerinden ve framework bağımlılıklarından arındırılmıştır.

## 2. Veri Mimarisi (Data Architecture)
Uygulama belleğinde (RAM) veriler "Smart-Lean" ve "Delta" prensiplerine göre tutulacaktır. Domain Driven Design (DDD) standartlarına uygun olarak `Island` nesnesi bir **Aggregate Root** (Kök Varlık) olarak kurgulanmıştır. Adanın konumu, üyeleri ve yetkileri dışarıdan bağımsız olarak değil, doğrudan `Island` nesnesi üzerinden yönetilir.

Çalışma zamanı bellek stratejisi:
* Modeller nesne yönelimli (OOP) kurgulanacak ancak DTO (Data Transfer Object) mantığına yakın, gereksiz davranışlardan arındırılmış yalın (Anemic/Rich kırması) bir yapıda olacaktır.
* `permission_delta` ve `settings_delta` byte dizileri (veya BitSet), adanın referans yetki haritasıyla birleştirilerek okunacaktır.
* Şablonlar (Templates), Minecraft'a özgü `StructureTemplate` veya `List<Block>` gibi ağır nesneleri içermez. API katmanı sadece şablonun kimliğini ve yerleşim matematiğini (Metadata) bilir.

## 3. Çekirdek Arayüzler ve Sınıflar (Core Interfaces & Classes)

Aşağıdaki şemalar sistemin kullanacağı veri modellerini temsil eder.

```yaml
Vector3i:
  type: "Value Object (Değişmez - Immutable)"
  description: "Tam sayı tabanlı 3 boyutlu matematiksel vektör. Blok hizalamaları ve merkez ofsetleri için kullanılır. Saf Java'dır."
  fields:
    x: "Integer"
    y: "Integer"
    z: "Integer"

Vector3f:
  type: "Value Object (Değişmez - Immutable)"
  description: "Ondalıklı sayı tabanlı 3 boyutlu matematiksel vektör. Oyuncu ışınlanma (Spawn) hassasiyeti için kullanılır."
  fields:
    x: "Float"
    y: "Float"
    z: "Float"
    
IslandTemplate:
  type: "Value Object (Değişmez - Immutable)"
  description: "Ağır NBT blok verilerini içermeyen, sadece şablonun kimliğini ve yerleşim matematiğini tutan Flyweight nesnesi."
  fields:
    templateId: "String (Örn: 'basic', 'desert', 'vip_1')"
    bedrockOffset: "Vector3i (NBT şablonunun başlangıç noktasına göre merkez Bedrock bloğunun göreceli konumu)"
    spawnOffset: "Vector3f (Merkez Bedrock bloğuna göre, [SPAWN] tabelasının veya belirlenen doğma noktasının göreceli konumu)"

IslandMember:
  type: "Entity"
  description: "Adaya ait olan bir oyuncunun kimliğini ve hiyerarşik rol ağırlığını tutar."
  fields:
    playerId: "UUID"
    roleWeight: "Integer (0: Member, 2: Builder, 3: Admin vs. Varsayılan ziyaretçi (0) için nesne oluşturulmaz.)"

Island:
  type: "Aggregate Root (Entity)"
  description: "Adanın yaşam döngüsünü, uzamsal verilerini ve alt varlıklarını (üyeler) barındıran kök nesne. Gereksiz yuvalamalardan (Nesting) kaçınılmış, veriye O(1) hızında doğrudan erişim için düzleştirilmiştir (Flattened)."
  fields:
    id: "UUID (Adanın benzersiz kimliği)"
    ownerId: "UUID (Ada sahibinin kimliği)"
    templateId: "String (Adanın ilk oluşturulurken kullandığı şablon referansı. Örn: 'basic')"
    worldKey: "String (Adanın bulunduğu dünya. Örn: 'skyblock_world')"
    center: "Vector3i (Adanın dünyadaki sabit merkez koordinatı)"
    radius: "Integer (Adanın güncellenebilir sınır yarıçapı)"
    members: "Map<UUID, Integer> (Oyuncu kimliği ve Rol Ağırlığı eşleşmesi)"
    settingsDelta: "byte[] (Varsayılan ada ayarlarından sapan durumlar)"
    permissionDelta: "byte[] (Varsayılan rol yetkilerinden sapan durumlar)"
    isLoaded: "Boolean (Adanın asenkron yüklenme durumunu belirten geçici RAM bayrağı)"
    isDirty: "Boolean (Verisinde değişiklik yapıldığını ve veritabanına yazılması gerektiğini belirten Cache bayrağı)"
    spawnX: "Double (Merkez Bedrock'a veya [SPAWN] tabelasına göre hesaplanmış kesin ışınlanma noktası)"
    spawnY: "Double"
    spawnZ: "Double"
    spawnYaw: "Float (Oyuncunun bakış açısı - Sağa/Sola)"
    spawnPitch: "Float (Oyuncunun bakış açısı - Aşağı/Yukarı)"

IslandSetting:
  type: "Enum"
  description: "Adanın global, rol bazlı olmayan durum ayarları."
  values:
    - DISABLE_FIRE_SPREAD
    - DISABLE_MOB_GRIEFING
    - ENABLE_PVP
    - DISABLE_MOB_SPAWN
    # ...
    
IslandAction:
  type: "Enum"
  description: "Oyuncuların ada üzerinde yapabileceği fiziksel/etkileşimsel eylemler (Ordinal sırası byte dizisindeki indeksi belirler)."
  values:
    - BLOCK_BREAK
    - BLOCK_PLACE
    - INTERACT_REDSTONE
    - OPEN_CONTAINERS
    - HURT_ANIMALS
    # ...
```

## 4. Olaylar ve Komutlar (Events & Commands)
Bu çekirdek modellerin sistem içindeki yaşam döngüsünü tetikleyen ana Olay (Event) sözleşmeleri:

* **Sistem İçi Olaylar (Domain Events):**
  * `IslandLoadEvent`: Veritabanından veriler çekilip `Island` nesnesi (isLoaded = true olarak) belleğe alındığında fırlatılır. Eklentinin diğer modülleri (Örn: Hologramlar, Spawnerlar) bu olayı dinleyerek kendi önbelleklerini hazırlar.
  * `IslandUnloadEvent`: Ada bellekten (Cache) atılmadan hemen önce tetiklenir.
  * `IslandDeltaChangeEvent`: Adanın `permissionDelta` veya `settingsDelta` verisi değiştiğinde ve `isDirty` bayrağı `true` yapıldığında tetiklenir.

## 5. Riskler ve Ödünleşimler (Risks & Trade-offs)
* **Risk (Bellek Sızıntısı - Memory Leak):** `Island` nesneleri önbellekte (Cache) tutulacağı için, oyuncular adadan çıktıktan sonra bu nesnelerin bellekte gereksiz yere kalması (zombi nesneler) RAM'in şişmesine neden olabilir.
* **Ödünleşim (Zaman vs Bellek):** `Island` içindeki `isDirty` bayrağı sayesinde sadece değişen adalar veritabanına kaydedilir (Write-Behind). Ancak bu durum, sistem aniden çökerse `isDirty = true` olan adaların RAM'deki güncel verilerinin kaybolması riskini doğurur (Eventual Consistency ödünleşimi).
* **Risk (Dairesel Bağımlılık):** Modeller birbirine sıkı sıkıya bağlanırsa (Örn: `IslandMember` sınıfının içine `Island` referansı koymak), JSON serileştirme/deserileştirme işlemleri sırasında "StackOverflow" (Sonsuz Döngü) hatası yaşanabilir. Bu yüzden `IslandMember` sadece `playerId` ve `roleWeight` tutacak şekilde tasarlanmış, yukarıdan aşağıya (Top-Down) bir sahiplik kurgulanmıştır.
* **Ödünleşim (Veri Ayrışması / Separation of Data):** `IslandTemplate` modelinin API içerisine sadece metadata (matematiksel ofsetler) olarak eklenmesi sebebiyle, asıl yapıştırma (Pasting) işleminde blok verisini almak için Adaptör (Fabric) katmanına güvenilmesi gerekir. Bu, modüller arası mesajlaşmayı artırsa da (overhead), API modülünün Minecraft sınıflarından %100 temiz kalmasını sağlayan hayati bir mimari karardır.