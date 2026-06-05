# ENightMarket - Permissions

| Permission Node | Description | Default |
|---|---|---|
| `enightmarket.use` | Gece Pazari'ni kullanma + `/gecepazari` ile GUI acma izni. | `true` (herkes) |
| `enightmarket.lang` | `/gecepazari lang <en\|tr\|de>` komutuyla kendi dilini degistirebilir. | `true` (herkes) |
| `enightmarket.notify` | Etkinlik otomatik olarak baslatildiginda chat'te bildirim alir. | `true` (herkes) |
| `enightmarket.admin` | Tum admin komutlari: `baslat`, `durdur`, `reload`. Ayrica `use`, `lang`, `notify` haklarini icerir. | `op` |
| `enightmarket.bypass` | Etkinlik kapali olsa bile pazari acabilir (yetkili test icin). | `op` |

## LuckPerms Ornegi

```
/lp group default permission set enightmarket.use true
/lp group default permission set enightmarket.lang true
/lp group default permission set enightmarket.notify true

/lp group vip permission set enightmarket.bypass true

/lp group admin permission set enightmarket.admin true
```

## Komut - Permission Eslestirme

| Komut | Gerekli Permission |
|---|---|
| `/gecepazari` (GUI acma) | `enightmarket.use` |
| `/gecepazari lang <code>` | `enightmarket.lang` |
| `/gecepazari baslat` | `enightmarket.admin` |
| `/gecepazari durdur` | `enightmarket.admin` |
| `/gecepazari reload` | `enightmarket.admin` |

## PlaceholderAPI

PlaceholderAPI yuklu ise asagidaki placeholderlar otomatik kayit olur:

| Placeholder | Donus |
|---|---|
| `%enightmarket_active%` | `true` / `false` |
| `%enightmarket_status%` | Oyuncunun dilinde "Aktif" / "Kapali" |
| `%enightmarket_next_open_day%` | Sonraki acilis gunu (ornek: `FRIDAY`, "Her gun") |
| `%enightmarket_next_open_time%` | Sonraki acilis saati (ornek: `20:00`) |
| `%enightmarket_next_open_date%` | Tarih + saat (ornek: `07/06 20:00`) |
| `%enightmarket_time_remaining%` | Geri sayim (`2d 5h 30m` veya `HH:MM:SS`) |

### Scoreboard Ornegi (Featherboard / DeluxeMenus)

```yaml
title: "&8&l| &dGECE PAZARI"
lines:
  - "&7Durum: &f%enightmarket_status%"
  - "&7Sonraki: &e%enightmarket_next_open_date%"
  - "&7Kalan: &a%enightmarket_time_remaining%"
```

## Otomatik Plan (`schedule`)

`config.yml` icindeki `schedule` bolumu:

```yaml
schedule:
  enabled: true
  day: FRIDAY               # MONDAY..SUNDAY veya ANY
  time: "20:00"             # 24 saat formati
  auto-close-after-hours: 24 # 0 ise sadece manuel kapanir
```

Plan etkinse plugin her 20 saniyede bir saat kontrolu yapar; eslesme aninda etkinligi baslatir, Discord webhook'unu gonderir ve `enightmarket.notify` izni olan oyunculara mesaj atar. Aksi belirtilmedikce 24 saat sonra otomatik kapatir.
