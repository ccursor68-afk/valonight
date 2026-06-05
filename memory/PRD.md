# Gece Pazari - PRD

## Original Problem Statement
Paper 1.21.4 icin **Gece Pazari** (Valorant Night Market konsepti) eklentisi. Tek butonlu GUI ile baslar, oyuncunun etrafinda yarim ay seklinde 5 esya (ItemDisplay + TextDisplay + Interaction) belirir, indirimli ve sinirli stoklu satin alma yapilir, Vault entegrasyonu ile odeme, YAML kalici veri, Shift ile duman efektli kapanis, asenkron Discord webhook ile etkinlik baslangic duyurusu.

## Architecture (OOP, 16 classes)
- **GecePazariPlugin** (main + DI)
- **commands.GecePazariCommand** (/gecepazari baslat|durdur|reload + GUI ac)
- **config.ConfigManager** (config.yml type-safe okuyucu)
- **data.MarketItemTemplate / MarketItemInstance / PlayerMarketData / PlayerDataManager** (YAML kalici veri)
- **market.MarketRoller** (agirlikli RNG, indirim)
- **market.MarketGUI** (tek butonlu Chest GUI)
- **market.PlayerMarket** (ItemDisplay+TextDisplay+Interaction, follow task, hideEntity)
- **market.MarketManager** (aktif pazarlar, etkinlik durumu, satin alma)
- **listeners.MarketListener** (InventoryClick, InteractEntity, ToggleSneak, Join, Quit)
- **economy.EconomyHook** (Vault wrapper, zorunlu)
- **discord.DiscordWebhook** (async HttpURLConnection POST)
- **util.ColorUtil / ArcMath**

## User Choices (2026-06-05)
- Veri saklama: **YAML** (`playerdata/<uuid>.yml`)
- Discord Webhook: **Config placeholder**
- Vault: **Zorunlu** (yoksa plugin disable)
- Build: **Maven** (Java 21, Paper 1.21.4-R0.1-SNAPSHOT)
- Tema: **Notr** (jenerik MC esyalari: elmas kilic, totem, elytra, beacon vb.)

## What's Implemented (2026-06-05)
- [x] Chest GUI tek butonlu (`Pazari Ac`)
- [x] Buton tiklamasinda GUI kapanir + fiziksel pazar dunyada belirir
- [x] Yarim ay (160 derece) seklinde 5 esya pozisyonu (ArcMath)
- [x] ItemDisplay + TextDisplay + Interaction entity'leri (Armor Stand yok)
- [x] `setTeleportDuration` + `BukkitRunnable` ile pururzssuz takip
- [x] `Player#hideEntity` ile sadece sahibe gorunum (Join event'inde de uygulanir)
- [x] Agirlikli RNG ile 5 esya secimi, esya basina min-max indirim
- [x] YAML kalici veri (UUID -> playerdata/<uuid>.yml)
- [x] Re-open ile ayni esyalar + kalan stok
- [x] Sag tik satin alma, Vault para kontrolu/kesintisi
- [x] ITEM odulu envantere, COMMAND odulu konsol komutu (%player% destegi)
- [x] Stok 0 -> `[TUKENDI]` hologram + alim engelli
- [x] Shift (sneak) -> Particle.SMOKE + LARGE_SMOKE + entity remove
- [x] /gecepazari baslat -> async Discord webhook embed (timestamp, color, footer, thumbnail)
- [x] /gecepazari durdur -> tum playerdata sil + etkinlik kapat
- [x] /gecepazari reload -> config yeniden yukle
- [x] event.yml ile etkinlik durumu kalici
- [x] Maven build (JAR: 53KB, target/GecePazari-1.0.0.jar)

## Prioritized Backlog (P1)
- Multi-language (messages_xx.yml)
- Esya basina ozel ses efekti (satin alma onayi, kapanis)
- Hologramda kalan etkinlik suresi (countdown)
- `/gecepazari yenile <player>` admin override

## Future / Backlog (P2)
- Web admin panel (REST + dashboard)
- PlaceholderAPI desteki
- Bedrock client uyumu (Geyser TextDisplay nuanslari)

## Build & Run
```bash
mvn clean package
# target/GecePazari-1.0.0.jar -> plugins/
```
Vault + Economy provider (EssentialsX/CMI) zorunlu.

## Validation Status
Bu bir Minecraft plugin'idir (web app degil), bu nedenle `testing_agent_v3` (browser/curl tabanli) kullanilmadi. Validasyon yontemleri:
- [x] Java 21 + Maven 3.8 ile **temiz derleme** (0 warning, 0 error)
- [x] plugin.yml `${project.version}` interpolasyonu dogrulanmis
- [x] JAR icerigi (`unzip -l`) ile tum siniflarin + resources'larin paketlendigi teyit edildi
- [ ] Canli Paper 1.21.4 sunucusunda runtime testi (kullanici lokalde yapacak)

## Next Action Items
- Kullanici lokal Paper 1.21.4 sunucusunda JAR'i test edebilir
- Discord webhook URL config'e eklenip /gecepazari baslat denemesi yapilabilir
