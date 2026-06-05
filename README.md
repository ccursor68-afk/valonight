# Gece Pazari (GecePazari)

Valorant'in **Gece Pazari** konseptini Minecraft'a uyarlayan, Paper **1.21.4** icin gelistirilmis modern bir eklenti. Her oyuncuya ozel olarak rastgele 5 esya, indirimli fiyatlar ve sinirli stoklarla yarim ay seklinde oyuncunun etrafinda belirir. Esyalar oyuncuyu pururzssuzce takip eder, sadece o oyuncu tarafindan gorulebilir ve oyuncu **Shift** tusuna basinca duman efektiyle kaybolur.

---

## Ozellikler

- **Tek butonlu Chest GUI**: `/gecepazari` ile basit bir onay menusu
- **Yarim ay (half-moon)**: 5 esya oyuncunun bakis yonunde yari cember halinde belirir
- **Modern Display Entity'ler**: `ItemDisplay` + `TextDisplay` (hologram) + `Interaction` (tiklama)
- **Pururzssuz takip**: `setTeleportDuration()` ve `BukkitRunnable` ile oyuncuyu suzulerek izler
- **Kisiye ozel gorunum**: Paper'in `Player#hideEntity` API'si ile sadece sahibi gorur
- **YAML veri kalici**: `playerdata/<uuid>.yml` icinde sabit pazar bilgisi
- **Agirlikli RNG + dinamik indirim**: Her oyuncuya config'deki sans agirliklariyla 5 esya, esya basina rastgele indirim (config'de min-max)
- **Vault entegrasyonu**: Para kontrolu, para kesintisi
- **Esya veya komut odulu**: Her esya envanter veya konsol komutu olabilir
- **Stok takibi + canli hologram guncelleme**: Stok 0 olunca `[TUKENDI]` yazar
- **Shift ile kapatma**: Duman efekti (`Particle.SMOKE`) ile pazar yok olur
- **Asenkron Discord Webhook**: `/gecepazari baslat` calistiginda sik bir embed mesaj gonderir
- **MySQL yok**: Tum veriler yerel YAML dosyalarinda
- **Temiz OOP mimarisi**: 16 sinif, paketlere ayrilmis, spagetti kod yok

---

## Komutlar

| Komut | Aciklama | Yetki |
|---|---|---|
| `/gecepazari` | Tek butonlu GUI'yi acar (etkinlik aktifse) | `gecepazari.use` |
| `/gecepazari baslat` | Etkinligi baslatir + Discord webhook gonderir | `gecepazari.admin` |
| `/gecepazari durdur` | Etkinligi kapatir, tum aktif pazarlari ve playerdata'yi siler | `gecepazari.admin` |
| `/gecepazari reload` | config.yml'i yeniden yukler | `gecepazari.admin` |

**Aliaslar:** `/gp`, `/gece`

---

## Kurulum

1. **Vault** ve bir economy provider (ornegin EssentialsX veya CMI) sunucuda bulunmali.
2. Build:
   ```bash
   mvn clean package
   ```
3. Olusan `target/GecePazari-1.0.0.jar` dosyasini sunucunun `plugins/` klasorune at.
4. Sunucuyu baslat. `plugins/GecePazari/config.yml` ve `playerdata/` klasoru otomatik olusur.
5. `config.yml` icindeki `discord-webhook-url` alanini doldur.
6. `/gecepazari reload` calistir.
7. `/gecepazari baslat` ile etkinligi ac.

---

## Mimari (Paketler)

```
com.emergent.gecepazari
├── GecePazariPlugin           # Ana sinif, dependency injection
├── commands/
│   └── GecePazariCommand      # /gecepazari komut handler'i
├── config/
│   └── ConfigManager          # config.yml'i type-safe okur
├── data/
│   ├── MarketItemTemplate     # Config'den okunmus immutable sablon
│   ├── MarketItemInstance     # Oyuncuya ozel uretilmis indirimli/stoklu esya
│   ├── PlayerMarketData       # Bir oyuncunun tum pazar verisi
│   └── PlayerDataManager      # YAML okuma/yazma
├── market/
│   ├── MarketRoller           # Agirlikli RNG ile esya secimi
│   ├── MarketGUI              # Tek butonlu Chest GUI
│   ├── PlayerMarket           # Display + Interaction entity yonetimi, takip task'i
│   └── MarketManager          # Aktif pazarlar, etkinlik durumu, satin alma
├── listeners/
│   └── MarketListener         # Tum event'leri tek listener'da
├── economy/
│   └── EconomyHook            # Vault wrapper
├── discord/
│   └── DiscordWebhook         # Asenkron HTTP POST webhook
└── util/
    ├── ColorUtil              # Renk kodlari -> Adventure Component
    └── ArcMath                # Yarim ay pozisyon hesabi
```

---

## Onemli Teknik Detaylar

### Display Entity'ler
- `ItemDisplay` ile esya 3D olarak gosterilir (Armor Stand kullanilmaz).
- `TextDisplay` ile fiyat, indirim ve stok hologramda yazar.
- `Interaction` entity sayesinde fiziksel sag tik algilanir.
- `setTeleportDuration(int)` ile pozisyon degisimleri pururzssuz interpolasyona ugrar.
- `setInterpolationDuration` ile esyalar yumusak donus efekti uygular.

### Pururzssuz Takip
`PlayerMarket#startFollowTask()` icindeki `BukkitRunnable`:
- Her `market.update-interval-ticks` (varsayilan: 2 tick)
- Yarim ay noktalarini yeniden hesaplar
- `ItemDisplay`, `TextDisplay` ve `Interaction` entity'lerini `teleport()` ile yeni konuma surukler
- Teleport `teleport-duration` tick sureli olarak interpolasyon ile gerceklesir

### Kisiye Ozel Gorunum
- Spawn anlarinda diger tum online oyunculara `player.hideEntity(plugin, entity)` cagrisi yapilir.
- `PlayerJoinEvent` ile yeni katilan oyunculardan da entity'ler gizlenir.

### Yarim Ay (Half-Moon) Hesabi
`util/ArcMath.calculateArcPoints` oyuncu yaw'inin yon vektorlerini hesaplar ve `arc-degrees`'a yayilmis 5 nokta dondurur.

### Veri Akisi
1. `/gecepazari baslat` -> etkinlik aktif + webhook
2. `/gecepazari` -> Chest GUI
3. Butona tik -> `MarketManager.openMarket(player)`
4. Eger playerdata yoksa `MarketRoller.roll(uuid)` ile uretilir ve YAML'a kaydedilir
5. `PlayerMarket.spawn()` -> entity'ler dunyada belirir
6. Sag tik -> `MarketManager.attemptPurchase` -> Vault checks -> reward + stock-- + YAML guncelle + hologram refresh
7. Shift -> `MarketListener.onSneak` -> `MarketManager.closeMarket(true)` -> duman + remove

---

## Config Ornegi

`src/main/resources/config.yml` icinde tam yapilandirma mevcut. Her esya icin:

```yaml
items:
  diamond-sword:
    material: DIAMOND_SWORD
    display-name: "&bElmas Kilic"
    lore: ["&7Keskinlestirilmis bir elmas kilic."]
    base-price: 1200.0
    chance: 25            # Agirlikli sans
    max-stock: 2
    discount:
      min: 10
      max: 40
    reward:
      type: ITEM           # ITEM veya COMMAND
      amount: 1
      command: ""          # type=COMMAND ise: "give %player% diamond 1"
```

Komutlu bir esya ornegi:
```yaml
  fly-command:
    material: FEATHER
    display-name: "&bUcus Hakki (30dk)"
    base-price: 800.0
    chance: 10
    max-stock: 1
    discount:
      min: 20
      max: 50
    reward:
      type: COMMAND
      command: "effect give %player% minecraft:levitation 1800 0 true"
```

---

## Bagimliliklar

- Paper API: `1.21.4-R0.1-SNAPSHOT`
- Vault API: `1.7` (zorunlu)
- Java 21
- Maven 3.8+

Build edildikten sonra olusacak JAR `~53 KB` boyutundadir; tum bagimliliklar `provided` scope'tadir (Paper ve Vault sunucudan saglanir).
