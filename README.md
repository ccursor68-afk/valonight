# ENightMarket

Valorant tarzi **Gece Pazari** konseptini Minecraft **1.18 – 1.21** (tum alt surumler) icin uyarlayan, **Paper / Spigot / Bukkit** uyumlu, **cok dilli (TR/EN/DE)** ve **planlanabilir** bir eklentidir.

> **Yapimci:** ArtfulMiner

Her oyuncuya ozel olarak rastgele 5 esya yarim ay seklinde belirir; sealed olarak nadirlik bazli custom kafalar gozukur, sag tikla patlama efektiyle gercek esyaya donusur. YAML kalici veri, Vault ekonomi, Discord webhook ve PlaceholderAPI entegrasyonu hazirdir.

---

## Desteklenen Surumler

| Minecraft | Sunucu yazilimi | Gorsel backend |
|---|---|---|
| 1.18.x – 1.19.3 | Paper, Spigot, Bukkit | ArmorStand (legacy) |
| 1.19.4 – 1.21.x | Paper, Spigot, Bukkit | ItemDisplay + TextDisplay + Interaction |

- **Java 17+** gerekir (1.20.5+ sunucular icin Java 21 onerilir)
- **Vault** + economy provider zorunlu
- Paper'da `hideEntity` ile kisiye ozel gorunum; Spigot/Bukkit'te diger oyuncular entity'leri gorebilir (bilgi mesaji konsola yazilir)

---

## Ozellikler

- **Tek butonlu Chest GUI** + `/gecepazari` veya `/enightmarket` ile pazara giris
- **Yarim ay (half-moon)** seklinde 5 esya (radius/arc-degrees ayarlanabilir)
- **Surume gore goruntuleme**: 1.19.4+ Display entity, 1.18–1.19.3 ArmorStand
- **Pururzssuz takip**: yaw kilitli, sadece pozisyon takibi + bob animasyonu
- **Kisiye ozel gorunum** (Paper): sadece sahibi gorur
- **Reveal animasyonu**: 4 nadirlik kafasi, sag tik -> patlama -> gercek esya
- **Agirlikli RNG + dinamik indirim**
- **CustomModelData destegi**
- **Vault**, **3 dil**, **otomatik plan**, **PlaceholderAPI**, **Discord webhook**

---

## Komutlar

| Komut | Aciklama | Yetki |
|---|---|---|
| `/gecepazari` veya `/enightmarket` | GUI'yi acar | `enightmarket.use` |
| `/gecepazari lang <en\|tr\|de>` | Dil degistirir | `enightmarket.lang` |
| `/enightmarket start` | Etkinligi baslatir + Discord | `enightmarket.admin` |
| `/enightmarket stop` | Etkinligi durdurur, pazarları kapatir | `enightmarket.admin` |
| `/enightmarket reload` | Config + dil yeniden yukler | `enightmarket.admin` |

Aliaslar: `/gp`, `/nm`, `/gece`, `/nightmarket`, `/em`

---

## Kurulum

1. **Paper, Spigot veya Bukkit** sunucusu (1.18+)
2. Vault + Economy provider (EssentialsX/CMI vb.)
3. (Opsiyonel) PlaceholderAPI
4. `mvn clean package` -> `target/ENightMarket-1.2.0.jar`
5. JAR'i `plugins/` klasorune at ve sunucuyu baslat
6. `config.yml` ayarla, `/enightmarket reload`, `/enightmarket start`

## Otomatik Plan

```yaml
schedule:
  enabled: true
  day: FRIDAY              # MONDAY..SUNDAY veya ANY (her gun)
  time: "20:00"
  auto-close-after-hours: 24
```

20 saniyede bir saat kontrolu yapilir. Eslesme aninda etkinlik aktif olur, Discord webhook gider, `enightmarket.notify` izni olanlara mesaj gonderilir.

## Dil Sistemi

- `plugins/ENightMarket/lang/en.yml`, `tr.yml`, `de.yml` JAR'dan otomatik cikartilir
- `default-language: en` sets the default language for new players
- Oyuncular `/gecepazari lang <code>` ile kendi dillerini secer
- Tercih `plugins/ENightMarket/playerlang.yml` icinde saklanir

## PlaceholderAPI

```
%enightmarket_active%           -> true / false
%enightmarket_status%           -> Aktif / Kapali (lokalize)
%enightmarket_next_open_day%    -> FRIDAY (veya "Her gun")
%enightmarket_next_open_time%   -> 20:00
%enightmarket_next_open_date%   -> 07/06 20:00
%enightmarket_time_remaining%   -> 2d 5h 30m
```

Scoreboard ornegi:

```
&7Durum: &f%enightmarket_status%
&7Sonraki: &e%enightmarket_next_open_date%
&7Kalan: &a%enightmarket_time_remaining%
```

## Bagimliliklar

| Lib | Scope | Versiyon |
|---|---|---|
| Spigot API | provided | 1.19.4-R0.1-SNAPSHOT (derleme) |
| Vault API | provided | 1.7 |
| PlaceholderAPI | provided (soft-depend) | 2.11.6 |

Java 17 + Maven 3.8+. Calisma: Minecraft **1.18 – 1.21** (Paper / Spigot / Bukkit).

## Mimari

```
com.emergent.gecepazari
├── compat/                       # Surum & platform uyumluluk
│   ├── ServerVersion
│   ├── PlatformCompat            # Paper hideEntity reflection
│   ├── ItemMetaCompat / InventoryCompat / ParticleCompat
├── market/display/
│   ├── ModernDisplayAdapter      # 1.19.4+ (Display entity)
│   ├── LegacyArmorStandAdapter   # 1.18 – 1.19.3
│   └── DisplayAdapterFactory
├── GecePazariPlugin
├── commands/GecePazariCommand    # Komut + tab complete
├── config/ConfigManager          # config.yml tipli erisim
├── data/
│   ├── MarketItemTemplate
│   ├── MarketItemInstance        # initialStock, remainingStock, revealed
│   ├── PlayerMarketData
│   ├── PlayerDataManager         # YAML kalici veri
│   └── Rarity                    # 4 nadirlik + skull base64
├── lang/
│   ├── LanguageManager           # en/tr/de yukleme + per-player resolve
│   └── PlayerLangStore           # playerlang.yml
├── market/
│   ├── MarketRoller              # agirlikli RNG
│   ├── MarketGUI                 # tek butonlu chest GUI (lang aware)
│   ├── PlayerMarket              # entity'ler + reveal animasyonu + bob
│   └── MarketManager             # event durumu + satin alma + reveal handling
├── listeners/MarketListener      # tum eventler
├── economy/EconomyHook           # Vault wrapper
├── discord/DiscordWebhook        # async HTTP POST embed
├── schedule/ScheduleManager      # gun + saat tabanli otomatik acilis
├── integration/PlaceholderHook   # PlaceholderAPI expansion
└── util/ SkullUtil, ColorUtil, ArcMath
```
