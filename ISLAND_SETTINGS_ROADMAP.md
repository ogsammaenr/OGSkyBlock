# OGSkyblock - Gelişmiş Ada Ayarları ve Yetki Sistemi (Roadmap)

## 🎯 Proje Özeti
Bu döküman, **OGSkyblock** (100% Server-Side Fabric Mod) projesine eklenecek olan kapsamlı **Ada Ayarları (General Settings)**, **Koruma/Yetki (Protection Settings)** sisteminin ve **MenuManager Mimari Refactor** işleminin kodlanma adımlarını içerir.

**Kritik Kurallar:**
1. **Sıfır İstemci Modu (Zero Client Mods):** Tüm menüler SGUI kullanılarak sunucu tarafında oluşturulmalıdır.
2. **Sayfalandırma (Pagination):** Koruma ayarları tek bir sandık arayüzüne sığmayacağı için SGUI ile "İleri/Geri" butonlarına sahip sayfalı bir menü tasarlanmalıdır.
3. **Modüler Koruma & Temiz Kod:** Tüm event (olay) dinleyicileri kategorize edilmeli, menü eylemleri ise (Single Responsibility prensibi gereği) tek bir dosyaya yığılmak yerine kendi sınıflarına ayrılmalıdır.

---

## 🛠️ Görev Dağılımı ve Kodlama Adımları

### Adım 1: Veri Modellerinin (Enums) Genişletilmesi
Aşağıdaki listeleri `me.ogsammenr.skyblock.model` paketi altındaki ilgili Java sınıflarına (Enum) dönüştürün.

- [ ] **`IslandSetting.java` (Genel Ayarlar - Aç/Kapat):**
  Aşağıdaki ayarları barındırmalıdır:
  - `ANIMAL_SPAWNING`, `MONSTER_SPAWNING`
  - `FIRE_SPREAD`
  - `OVERWORLD_PVP`, `NETHER_PVP`, `END_PVP`

- [ ] **`IslandAction.java` (Koruma Ayarları - Rütbe Seçimi):**
  Aşağıdaki eylemleri (Enum sabitlerini) ekleyin:
  - **Blok/Çevre:** `BREAK_BLOCK`, `PLACE_BLOCK`, `TRAMPLE_CROPS`, `FIRE_EXTINGUISH`, `TNT_DAMAGE`
  - **Kullanım/Etkileşim:** `USE_DOORS`, `USE_GATES`, `USE_TRAPDOORS`, `USE_ANVILS`, `USE_BEDS`, `USE_BEACONS`, `USE_BREWING_STANDS`, `USE_ENCHANTING_TABLE`, `USE_WORKBENCHES`, `USE_JUKEBOX`, `USE_NOTE_BLOCK`
  - **Depolama:** `USE_CONTAINERS`, `USE_DISPENSERS`, `USE_DROPPERS`, `USE_HOPPERS`
  - **Kızıltaş:** `USE_BUTTONS`, `USE_LEVERS`, `USE_PRESSURE_PLATES`, `USE_REDSTONE_ITEMS`
  - **Eşya Kullanımı:** `USE_BUCKETS`, `COLLECT_LAVA`, `COLLECT_WATER`, `USE_SPAWN_EGGS`, `THROW_EGGS`, `THROW_POTIONS`, `USE_ENDERPEARLS`, `EAT_CHORUS_FRUIT`, `USE_NAME_TAGS`, `USE_LEASH`
  - **Varlık (Entity) Etkileşimi:** `HURT_ANIMALS`, `HURT_MONSTERS`, `HURT_VILLAGERS`, `BREED_ANIMALS`, `MILK_ANIMALS`, `SHEAR_ANIMALS`, `RIDE_ANIMALS`, `MOUNT_INVENTORY`, `TRADE_WITH_VILLAGER`, `USE_ARMOR_STANDS`, `USE_ITEM_FRAMES`
  - **Dünya/Mekanik:** `LOCK_ISLAND`, `ITEM_DROP`, `ITEM_PICKUP`, `EXPERINCE_PICKUP`, `FISH_SCOOPING`, `FROST_WALKER`, `USE_NETHER_PORTAL`, `USE_END_PORTAL`, `TURTLE_EGGS`

- [ ] **`Island.java` Güncellemesi:**
  - `private final Map<IslandSetting, Boolean> settings = new HashMap<>();` haritası eklenmeli.
  - Sınıf oluşturulduğunda (`Constructor`) veya yeni bir metodla hem `settings` hem de `permissions` için *default* (varsayılan) değerler atanmalıdır.

### Adım 2: MenuManager Refactoring (UI Routing Architecture)
Şu anki `MenuManager` sınıfı tüm JSON parse, UI render ve tıklama olaylarını tek bir `switch-case` içinde yönetmektedir (God Class Anti-Pattern). Bu adımda, Strategy Pattern kullanılarak her menü kendi sınıfına taşınacak ve MenuManager sadece bir "Router (Yönlendirici)" olacaktır.

- [ ] **`BaseMenu.java` (Temel Menü Sınıfı) Oluşturulması:**
  - `me.ogsammenr.skyblock.ui` paketinde oluşturulmalı.
  - `MenuManager`'ın içindeki arayüz oluşturma, eşyaları dizme, PlaceholderAPI parse etme ve ZIRH (hata koruma) mantığını bu sınıfa taşıyın.
  - Constructor'ı `ServerPlayer player` ve `MenuData data` parametrelerini almalıdır.
  - Sınıfın içine `protected abstract void onAction(String action, SlotGuiInterface gui);` adında soyut bir metod ekleyin ve eşya tıklama olaylarını (`.setCallback`) bu metoda yönlendirin.

- [ ] **Spesifik Menü Sınıflarının Oluşturulması:**
  - `me.ogsammenr.skyblock.ui.menus` paketi altında `IslandMenu.java` ve `ValuesMenu.java` sınıflarını oluşturun.
  - İkisi de `BaseMenu` sınıfını extends etmeli. `onAction` metodunu override ederek kendi eylemlerini (Örn: `TELEPORT_ISLAND`, `BLOCK_VALUE`) içlerinde yönetmeliler.

- [ ] **`MenuManager.java` Sınıfının Router Olarak Düzenlenmesi:**
  - `handleAction` metodunu ve içindeki `switch-case` bloğunu tamamen silin.
  - Yeni bir kayıt haritası oluşturun: `private static final Map<String, MenuFactory> MENU_ROUTERS = new HashMap<>();`
  - Yüklenen JSON menü isimlerini ilgili sınıflarla eşleştirin (Örn: `island_menu` -> `IslandMenu.class`).
  - `openMenu` metodu çağrıldığında sistem sadece JSON datasını alıp, factory üzerinden doğru menü sınıfını yaratıp ekrana çizdirmelidir.

# Görev: Sayfalı Menüler İçin "Statik Sayfa" (Static Pages) Mimarisinin Uygulanması

## 🎯 Hedef
OGSkyblock projesindeki sayfalı menüler (özellikle Koruma/Aksiyon ayarları) için "Statik Sayfa" mimarisine geçiş yapılacaktır. Bu mimaride sunucu sahibi, JSON dosyası üzerinden her sayfanın tasarımını ve hangi eşyanın hangi tam slotta (0-53) duracağını `%100` özelleştirebilecektir.

Lütfen aşağıdaki adımları sırasıyla uygulayarak hem veri modellerini güncelleyin hem de arayüz (GUI) çizim mantığını bu yeni JSON yapısına göre kodlayın.

---

### Adım 1: Java Veri Modelinin Güncellenmesi (`MenuData.java`)
GSON kütüphanesinin yeni JSON yapısını okuyabilmesi için `me.ogsammenr.skyblock.model.MenuData` sınıfını aşağıdaki gibi güncelleyin:

- `MenuData` sınıfına `pages` adında yeni bir liste (List) ekleyin.
- Her bir sayfanın kendi slot-eşya haritasını tutması için `MenuPage` adında statik bir alt sınıf (inner class) oluşturun.

**Beklenen Yapı:**
```java
public class MenuData {
    public String title;
    public int rows;
    
    // (Opsiyonel) Tüm sayfalarda ortak kullanılacak arka plan eşyaları için
    public Map<String, MenuItem> items; 

    // YENİ: Her sayfanın kendi özel eşyalarını tutan liste
    public List<MenuPage> pages;

    public static class MenuPage {
        public Map<String, MenuItem> items; // Slot ID -> Eşya Verisi
    }

    public static class MenuItem {
        public String id;
        public int amount;
        public String name;
        public List<String> lore;
        public String action;
        public boolean hasPlaceholder;
    }
}
```
### Adım 2: JSON Dosyasının Oluşturulması (island_settings_action_menu.json)
IslandAction enum'unda bulunan ~60 eylemi barındıracak olan aksiyon menüsü JSON dosyasını (src/main/resources/default_menus/island_settings_action_menu.json) yeni "pages" dizisine uygun olarak oluşturun.

**JSON Kuralları:**

- Menü rows: 6 olmalıdır.

- En az 2 veya 3 sayfa (pages array'i içinde obje) oluşturulmalıdır.

- Her sayfanın içindeki items objesinde, eşyaların tam slot numaraları (Örn: "10", "11", "12") anahtar (key) olarak kullanılmalıdır.

- Navigasyon (Kritik): - 1. sayfada Sonraki Sayfa butonu olmalı (action: "next_page").

  - Orta sayfalarda hem Önceki Sayfa (action: "prev_page") hem Sonraki Sayfa olmalı.

  - Son sayfada sadece Önceki Sayfa olmalı.

  - Tüm sayfalarda Ana Menüye Dön (action: "open_menu:island_settings_menu") butonu bulunmalıdır.

### Adım 3: Menü Çizim (Render) Mantığının Kodlanması (ProtectionSettingsMenu.java)
`me.ogsammenr.skyblock.ui.menus` paketi altındaki ProtectionSettingsMenu (veya ilgili sayfalama menüsü) sınıfını, bu statik sayfa verilerini ekrana çizecek şekilde yazın.

**Gereksinimler:**

1. Sınıf içerisinde private int currentPage = 0; değişkeni tutun.

2. Ekrana eşyaları dizen (Örn: renderPage()) bir metod yazın.

   * Bu metod önce arayüzü (GUI) temizlemeli/sıfırlamalıdır.

   * Eğer MenuData.items (Global items) varsa, önce onları yerleştirmelidir.

   * Daha sonra MenuData.pages.get(currentPage).items haritasındaki eşyaları alıp, belirtilen anahtardaki (key) slotlara yerleştirmelidir.

3. onAction metodu içerisinde sayfa değişimlerini yakalayın:

   * action değeri "next_page" ise: Eğer currentPage toplam sayfa sayısından küçükse currentPage++ yapıp renderPage() çağırın.

   * action değeri "prev_page" ise: Eğer currentPage > 0 ise currentPage-- yapıp renderPage() çağırın.

4. Eylemler (cycle_action:BREAK_BLOCK vb.) tetiklendiğinde ilgili rütbeyi artırıp azaltma işlemini yapın ve oyuncunun anında değişimi görebilmesi için mevcut sayfayı (renderPage()) tekrar yenileyin (Refresh).

### 🚀 Beklenen Çıktılar
Lütfen yukarıdaki adımları tamamlayın ve şu dosyaların tam, güncellenmiş kodlarını üretin:

1. `MenuData.java`

2. `island_settings_action_menu.json` (Örnek eşyalar ve sayfalamalar ile)

3. `ProtectionSettingsMenu.java` (SGUI SimpleGui kullanılarak çizim ve sayfalama mantığı dahil)

### Adım 3: SGUI ile Ayar ve Sayfalı Menülerin (UI) Geliştirilmesi
Yeni ada ayarları, `Adım 2`'de kurulan `BaseMenu` yapısına uygun olarak `me.ogsammenr.skyblock.ui.menus` paketi içerisinde oluşturulmalıdır.

#### Kısım 1: Menü JSON Tasarımları ve Konfigürasyon Yapısı
Bu kısımdaki tüm dosyalar `src/main/resources/default_menus/` (veya sunucu yapınıza göre `config/menus/`) klasöründe oluşturulmalı ve mevcut `island_menu.json` syntax'ına (Title, rows, items formatına) birebir uymalıdır.

- [ ] **Ana Yönlendirme Menüsü (`island_settings_menu.json`):**
  - **İşlev:** Ada ayarları açıldığında oyuncunun karşısına çıkacak ilk köprü (hub) ekranıdır.
  - **Tasarım:** Küçük boyutlu (Örn: 3 satır / 27 slot) olabilir.
  - **İçerik:** Sadece 2 ana butona sahip olmalıdır:
    1. "Genel Ayarlar" sayfasına yönlendirecek buton (Örn: İkon olarak `minecraft:comparator`).
    2. "Koruma (Aksiyon) Ayarları" sayfasına yönlendirecek buton (Örn: İkon olarak `minecraft:shield` veya `minecraft:iron_chestplate`).

- [ ] **Genel Ayarlar Menüsü (`island_settings_general_menu.json`):**
  - **İşlev:** `me.ogsammenr.skyblock.model.IslandSetting` sınıfında belirtilen boolean (Aç/Kapat) kurallarının arayüzüdür.
  - **Tasarım:** 3 veya 4 satırlık bir GUI yeterlidir.
  - **İçerik:** `ANIMAL_SPAWNING`, `MONSTER_SPAWNING`, `FIRE_SPREAD`, `PVP` vb. ayarların her biri için mantıklı ikonlar seçilmeli (Örn: Ateş yayılması için `minecraft:campfire`, canavar doğumu için `minecraft:zombie_head`). Alt kısımlarında Placeholder destekli "Durum: Açık/Kapalı" lore'ları eklenebilecek boşluklar bırakılmalıdır.

- [ ] **Koruma/Aksiyon Ayarları Menüsü (`island_settings_action_menu.json`):**
  - **İşlev:** `me.ogsammenr.skyblock.model.IslandAction` sınıfındaki ~60 adet detaylı etkileşim kuralının arayüzüdür.
  - **Tasarım:** SGUI'nin desteklediği en büyük boyutta (6 satır / 54 slot) tasarlanmalıdır.
  - **İçerik:** Kırma, koyma, kapı açma, sandık açma gibi her eylem için eşyalar (`minecraft:chest`, `minecraft:oak_door` vb.) tanımlanmalıdır.
  - **Navigasyon (Kritik):** Menünün son satırı (45-53 arası slotlar) sayfalama (Pagination) butonlarına ayrılmalıdır. Önceki Sayfa (`minecraft:arrow`), Ana Menüye Dön (`minecraft:dark_oak_door`) ve Sonraki Sayfa (`minecraft:arrow`) butonları JSON içine sabit olarak yerleştirilmelidir.

- [ ] **Varsayılan Dosyaların Çıkartılması (ResourceExtractor):**
  - Hazırlanan bu 3 yeni `.json` dosyasının, sunucu ilk açıldığında fiziksel `config/skyblock_core/menus/` klasörüne otomatik olarak kopyalanması gerekmektedir.
  - `SkyblockMain.java` (veya `ResourceExtractor.java`) içerisine bu 3 dosya için çıkarma/kopyalama (`extractDefaultFile`) metodu eklenmelidir.
---
#### Kısım 2:
- [ ] **Ana Kategori Menüsü (`SettingsMainMenu.java`):**
  - Oyuncuya "Genel Ayarlar" ve "Koruma Ayarları" olarak iki farklı seçenek sunan geçiş (köprü) menüsü.

- [ ] **Genel Ayarlar Menüsü (`GeneralSettingsMenu.java`):**
  - Adanın `settings` (Aç/Kapat) değerlerini yönetecek sınıf.
  - Tıklandığında boolean değer tersine (`!value`) dönmeli, eşyanın altındaki lore (`§aAçık` / `§cKapalı`) GUI yenilenerek anında güncellenmelidir.

- [ ] **Koruma Ayarları Menüsü (`ProtectionSettingsMenu.java` - ZORLUK: YÜKSEK):**
  - `IslandAction` içerisindeki ~60 eylemi listeleyecek sınıf.
  - **Sayfalandırma (Pagination):** SGUI 6x9 (54 slot) destekler. Alt satır (son 9 slot) sayfa navigasyonu (Önceki Sayfa, Menü, Sonraki Sayfa) için ayrılmalı, üstteki 45 slot eylemler için kullanılmalı ve matematiksel sayfalama mantığı kurulmalıdır.
  - **Tıklama Mekaniği:** Sol tık -> Rütbeyi Artır, Sağ tık -> Rütbeyi Düşür.
  - **İkonlar:** Her aksiyon için JSON dosyasında mantıklı bir `Item` seçilmelidir (Örn: `USE_ANVILS` için `minecraft:anvil`).

### Adım 4: Event Listeners (Olay Dinleyicileri) ile Korumaların Uygulanması
Yazılan ayarların sunucuda fiziksel olarak çalışması için Fabric API event'leri kullanılmalıdır. Kodun şişmemesi için Listener'lar kategorilere ayrılmalıdır:

- [ ] **`BlockProtectionListener.java`:**
  - `PlayerBlockBreakEvents` ve `UseBlockCallback` kullanılarak kapılar, şalterler, sandıklar, kızıltaş vb. blok etkileşimleri (`IslandAction`) engellenmelidir.
  - Ateş yayılması (`FIRE_SPREAD`) dünyadaki blok güncellemeleri üzerinden kontrol edilmelidir.

- [ ] **`EntityProtectionListener.java`:**
  - `AttackEntityCallback` (Hayvan, canavar, köylü hasar koruması).
  - `UseEntityCallback` (İsim etiketi, tasma, zırh askısı, hayvan sağma/kırkma, köylü takası, binme).
  - Doğum koruması (Canavar ve hayvan doğumu `IslandSetting` üzerinden engellenmelidir).

- [ ] **`ItemProtectionListener.java`:**
  - Eşya atma, eşya toplama, XP toplama, kova kullanımı, iksir fırlatma, ender incisi kullanımı.

### Adım 5: Veri Kalıcılığı (Data Persistence)
- [ ] **`IslandStorage.java` Güncellemesi:**
  - GSON JSON'dan verileri okuduğunda, yeni eklenen `IslandSetting` ve `IslandAction` alanları eksik gelirse (Eski sunucu verisi), bunlar sistemde NullPointerException çökmesine yol açmamalıdır.
  - `null` olan haritalar veya eksik anahtarlar, yükleme sonrası otomatik olarak *varsayılan (default)* değerlerle doldurulmalıdır.