# Görev: SGUI "Statik Sayfalı Menü" (Static Paginated Menu) Altyapısının Kurulması

## 🎯 Hedef
Projemize ileride eklenecek olan devasa ayar menüleri için, eu.pb4:sgui kütüphanesi üzerinde çalışacak esnek ve %100 özelleştirilebilir bir "Statik Sayfalı Menü" (Static Pagination) motoru inşa edilecektir.
Şu aşamada hiçbir oyun içi mantık (Ada ayarları, korumalar vb.) EKLENMEYECEKTİR. Sadece JSON üzerinden sayfaları okuyup ekranda sorunsuzca değiştirebilen çekirdek altyapı kodlanacaktır.

Lütfen aşağıdaki adımları sırasıyla uygulayın:

---

### Adım 1: Veri Modelinin Sayfalamaya Uyarlanması (MenuData.java)
GSON kütüphanesinin çok sayfalı JSON yapısını okuyabilmesi için me.ogsammenr.skyblock.model.MenuData sınıfını güncelleyin.

- pages adında yeni bir liste (List) ekleyin.
- Her sayfanın kendi slot haritasını tutması için MenuPage adında statik bir alt sınıf oluşturun.

**Beklenen Yapı**:
```java
public class MenuData {
public String title;
public int rows;

    // Tüm sayfalarda sabit kalacak ortak eşyalar (Örn: Arka plan camları)
    public Map<String, MenuItem> items; 

    // Her sayfanın kendi özel eşyalarını tutan liste
    public List<MenuPage> pages;

    public static class MenuPage {
        public Map<String, MenuItem> items; // Slot ID -> Eşya Verisi
    }

    // Mevcut MenuItem sınıfı aynen kalmalı...
    public static class MenuItem { ... }
}
```

---

### Adım 2: Sayfalama Motorunun Kodlanması (PaginatedMenu.java)
`me.ogsammenr.skyblock.ui` paketi altına, sayfalı menülerin miras alacağı (extends) PaginatedMenu adında soyut (abstract) bir sınıf oluşturun. (Eğer projenizde BaseMenu varsa, bu sınıf BaseMenu'den türemelidir).

Gereksinimler:
1. Sınıf içerisinde protected int currentPage = 0; değişkenini tutun.
2. public void renderPage() adında bir metod yazın. Bu metod:
    - SGUI arayüzündeki tüm slotları temizlemelidir (gui.clear() veya boş slot ataması).
    - Eğer MenuData.items boş değilse, bu global eşyaları belirtilen slotlara yerleştirmelidir.
    - Ardından MenuData.pages.get(currentPage).items haritasını çekip, o sayfaya özel eşyaları ilgili slotlara yerleştirmelidir.
3. Menü eşyalarına .setCallback atanırken, eğer eşyanın action değeri "next_page" veya "prev_page" ise, bu sınıf sayfa değişimini otomatik yapmalıdır:
    - "next_page": currentPage < data.pages.size() - 1 ise currentPage++ yap ve renderPage() çağır.
    - "prev_page": currentPage > 0 ise currentPage-- yap ve renderPage() çağır.
    - Diğer tüm action değerleri, alt sınıfların işlemesi için soyut bir handleCustomAction(String action, ...) metoduna devredilmelidir.

---

### Adım 3: Test İçin Örnek Sınıf ve JSON Oluşturulması
Yazdığınız bu altyapının çalıştığını kanıtlamak için örnek bir menü oluşturun.

1. Test JSON Dosyası (src/main/resources/default_menus/test_pagination_menu.json):
    - 6 satırlık bir menü olsun.
    - İçerisinde en az 2 sayfa (pages array'inde 2 obje) bulunsun.
    - Sayfa 1'de 10. slotta bir Elmas, 50. slotta action: "next_page" olan bir buton olsun.
    - Sayfa 2'de 10. slotta bir Zümrüt, 48. slotta action: "prev_page" olan bir buton olsun.

2. Test Sınıfı (me.ogsammenr.skyblock.ui.menus.TestPaginationMenu.java):
    - PaginatedMenu sınıfını extends etsin.
    - handleCustomAction metodunu override etsin (Şu anlık içi boş kalabilir veya konsola debug logu basabilir).

## 🚀 Beklenen Çıktılar
Sadece bu sayfalama altyapısına odaklanın ve aşağıdaki 4 dosyanın tam kodunu üretin:
1. MenuData.java (Güncellenmiş hali)
2. PaginatedMenu.java (Sayfalama ve Çizim motoru)
3. test_pagination_menu.json (2 sayfalık örnek tasarım)
4. TestPaginationMenu.java (Örnek test sınıfı)