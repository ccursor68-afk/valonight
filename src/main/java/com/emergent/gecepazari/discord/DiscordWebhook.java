package com.emergent.gecepazari.discord;

import com.emergent.gecepazari.GecePazariPlugin;
import com.emergent.gecepazari.config.ConfigManager;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.logging.Level;

/**
 * Discord webhook'una asenkron olarak embed mesaj gonderir.
 * Vault'a benzer sekilde harici bir kutuphane gerekmez; HttpURLConnection ile JSON gonderir.
 */
public final class DiscordWebhook {

    private final GecePazariPlugin plugin;
    private final ConfigManager config;

    public DiscordWebhook(GecePazariPlugin plugin, ConfigManager config) {
        this.plugin = plugin;
        this.config = config;
    }

    /**
     * Asenkron olarak webhook gonderir. Webhook URL bos ise sessizce devre disi kalir.
     */
    public void sendEventStartedEmbed() {
        final String url = config.getDiscordWebhookUrl();
        if (url == null || url.isBlank()) {
            plugin.getLogger().info("Discord webhook URL bos, gonderim atlandi.");
            return;
        }

        final String username = escape(config.getDiscordUsername());
        final String avatar = config.getDiscordAvatarUrl();
        final String title = escape(config.getDiscordEmbedTitle());
        final String desc = escape(config.getDiscordEmbedDescription());
        final int color = config.getDiscordEmbedColor();
        final String footer = escape(config.getDiscordEmbedFooter());
        final String thumb = config.getDiscordEmbedThumbnail();
        final String timestamp = DateTimeFormatter.ISO_INSTANT.format(Instant.now());

        new BukkitRunnable() {
            @Override
            public void run() {
                String payload = buildPayload(username, avatar, title, desc, color, footer, thumb, timestamp);
                postPayload(url, payload);
            }
        }.runTaskAsynchronously(plugin);
    }

    private String buildPayload(String username, String avatar,
                                String title, String desc, int color,
                                String footer, String thumb, String timestamp) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"username\":\"").append(username).append("\",");
        if (avatar != null && !avatar.isBlank()) {
            sb.append("\"avatar_url\":\"").append(avatar).append("\",");
        }
        sb.append("\"embeds\":[{");
        sb.append("\"title\":\"").append(title).append("\",");
        sb.append("\"description\":\"").append(desc).append("\",");
        sb.append("\"color\":").append(color).append(",");
        sb.append("\"timestamp\":\"").append(timestamp).append("\"");
        if (footer != null && !footer.isBlank()) {
            sb.append(",\"footer\":{\"text\":\"").append(footer).append("\"}");
        }
        if (thumb != null && !thumb.isBlank()) {
            sb.append(",\"thumbnail\":{\"url\":\"").append(thumb).append("\"}");
        }
        sb.append("}]}");
        return sb.toString();
    }

    private void postPayload(String urlStr, String payload) {
        HttpURLConnection conn = null;
        try {
            URL url = URI.create(urlStr).toURL();
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            conn.setRequestProperty("User-Agent", "GecePazari-Plugin/1.0");
            conn.setDoOutput(true);
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            byte[] body = payload.getBytes(StandardCharsets.UTF_8);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(body);
            }

            int code = conn.getResponseCode();
            if (code >= 200 && code < 300) {
                plugin.getLogger().info("Discord webhook basariyla gonderildi (" + code + ").");
            } else {
                plugin.getLogger().warning("Discord webhook hata kodu: " + code);
            }
        } catch (Exception ex) {
            plugin.getLogger().log(Level.WARNING, "Discord webhook gonderilemedi", ex);
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "");
    }
}
