# ENightMarket

Valorant tarzi **Gece Pazari** konseptini Minecraft Paper **1.21.4** icin uyarlayan, **cok dilli (TR/EN/DE)** ve **planlanabilir** bir eklentidir.

> **Yapimci:** ArtfulMiner

Her oyuncuya ozel olarak rastgele 5 esya yarim ay seklinde belirir; sealed olarak nadirlik bazli custom kafalar gozukur, sag tikla patlama efektiyle gercek esyaya donusur. YAML kalici veri, Vault ekonomi, Discord webhook ve PlaceholderAPI entegrasyonu hazirdir.

---

## Ozellikler

- **Tek butonlu Chest GUI** + `/gecepazari` ile pazara giris
- **Yarim ay (half-moon)** seklinde 5 esya (radius/arc-degrees ayarlanabilir)
- **Modern Display Entity'ler**: `ItemDisplay` + `TextDisplay` + `Interaction` (Armor Stand yok)
- **Pururzssuz takip**: yaw kilitli, sadece pozisyon takibi + bob animasyonu
- **Kisiye ozel gorunum**: `Player#hideEntity` ile sadece sahibi gorur
- **Reveal animasyonu**: 4 farkli nadirlik kafasi (SIRADAN / NADIR / DESTANSI / EFSANEVI), sag tik -> patlama -> gercek esya. Reveal kalici (2. acilista direkt esya gozukur)
- **Agirlikli RNG + dinamik indirim**: havuzda agirlik, esya basina min-max indirim
- **CustomModelData destegi**: hem ItemDisplay'de hem oduldeki ItemStack'te
- **Lore in hologram**: config'deki lore satirlari hologramda da gozukur
- **Vault entegrasyonu** (zorunlu)
- **3 Dil**: Turkce, Ingilizce, Almanca - `/gecepazari lang <code>`
- **Otomatik plan**: belirli gun + saat icin otomatik etkinlik baslatma, otomatik kapanis
- **PlaceholderAPI**: scoreboard / chat icin `%enightmarket_*%` placeholderlari
- **Shift ile kapanis** + duman efekti
- **Asenkron Discord webhook embed**

---

## Komutlar

| Komut | Aciklama | Yetki |
|---|---|---|
| `/gecepazari` | GUI'yi acar | `enightmarket.use` |
| `/gecepazari lang <en\|tr\|de>` | Kendi dilini degistirir | `enightmarket.lang` |
| `/gecepazari baslat` | Etkinligi baslatir + Discord | `enightmarket.admin` |
| `/gecepazari durdur` | Etkinligi durdurur + playerdata siler | `enightmarket.admin` |
| `/gecepazari reload` | Config + dil dosyalarini yeniden yukler | `enightmarket.admin` |

Aliaslar: `/enightmarket`, `/nightmarket`, `/gp`, `/nm`, `/gece`

---

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
- `default-language: tr` config'de varsayilan dili belirler
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

## Kurulum

1. Paper 1.21.4 sunucusu
2. Vault + Economy provider (EssentialsX/CMI vb.) zorunlu
3. (Opsiyonel) PlaceholderAPI - placeholderlar icin
4. `mvn clean package` -> `target/ENightMarket-1.1.0.jar`
5. JAR'i `plugins/` klasorune at
6. Sunucu baslatildiktan sonra `plugins/ENightMarket/config.yml` + `lang/*.yml` olusur
7. `config.yml` icinde `discord-webhook-url`, `schedule.enabled`, `default-language` ayarla
8. `/gecepazari reload`
9. `/gecepazari baslat` veya plan tetiklenmesini bekle

## Mimari

```
com.emergent.gecepazari
├── GecePazariPlugin              # Ana sinif (DI)
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
└── util/
    ├── ColorUtil
    ├── ArcMath
    └── SkullUtil                 # base64 -> PlayerProfile skull
```

---

## Bagimliliklar

| Lib | Scope | Versiyon |
|---|---|---|
| Paper API | provided | 1.21.4-R0.1-SNAPSHOT |
| Vault API | provided | 1.7 |
| PlaceholderAPI | provided (soft-depend) | 2.11.6 |

Java 21 + Maven 3.8+. Build sonucu JAR: ~79 KB.
