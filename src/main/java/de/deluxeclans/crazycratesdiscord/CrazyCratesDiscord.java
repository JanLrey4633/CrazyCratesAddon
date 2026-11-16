package de.deluxeclans.crazycratesdiscord;

import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Method;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class CrazyCratesDiscord extends JavaPlugin {
    private final HttpClient http = HttpClient.newHttpClient();
    private ExecutorService executor;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "CrazyCratesDiscord-Webhook");
            t.setDaemon(true);
            return t;
        });
        if (!getConfig().getBoolean("webhook.enabled", true)) return;

        var crazy = Bukkit.getPluginManager().getPlugin("CrazyCrates");
        if (crazy == null) {
            getLogger().severe("CrazyCrates was not found. Install CrazyCrates 5.0 and restart.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        try {
            Class<?> raw = Class.forName(
                "com.badbones69.crazycrates.paper.api.events.PlayerPrizeEvent",
                true, crazy.getClass().getClassLoader());
            if (!Event.class.isAssignableFrom(raw)) throw new IllegalStateException("Not a Bukkit event");
            @SuppressWarnings("unchecked")
            Class<? extends Event> eventClass = (Class<? extends Event>) raw;
            Listener listener = new Listener() {};
            EventExecutor handler = (ignored, event) -> handlePrize(event);
            Bukkit.getPluginManager().registerEvent(eventClass, listener, EventPriority.MONITOR, handler, this, true);
            getLogger().info("Hooked into CrazyCrates PlayerPrizeEvent; webhook integration enabled.");
        } catch (ReflectiveOperationException | LinkageError | IllegalArgumentException ex) {
            getLogger().severe("Could not hook CrazyCrates prize event: " + ex.getMessage());
            Bukkit.getPluginManager().disablePlugin(this);
        }
    }

    private void handlePrize(Event event) {
        try {
            Object player = call(event, "getPlayer");
            Object crate = call(event, "getCrate");
            Object prize = call(event, "getPrize");
            String playerName = String.valueOf(call(player, "getName"));
            String crateName = String.valueOf(call(crate, "getCrateName"));
            String prizeName = String.valueOf(call(prize, "getPrizeName"));
            LocalDateTime now = LocalDateTime.now();
            String content = String.join("\n", getConfig().getStringList("webhook.message"))
                .replace("%player%", playerName)
                .replace("%crate%", crateName)
                .replace("%prize%", prizeName)
                .replace("%time%", now.format(DateTimeFormatter.ofPattern("HH:mm:ss")))
                .replace("%date%", now.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")));
            send(content);
        } catch (ReflectiveOperationException ex) {
            getLogger().warning("Could not read CrazyCrates prize event: " + ex.getMessage());
        }
    }

    private Object call(Object target, String method) throws ReflectiveOperationException {
        Method m = target.getClass().getMethod(method);
        return m.invoke(target);
    }

    private void send(String content) {
        String url = getConfig().getString("webhook.url", "").trim();
        if (!(url.startsWith("https://discord.com/api/webhooks/") ||
              url.startsWith("https://discordapp.com/api/webhooks/"))) {
            getLogger().warning("Set a valid Discord webhook URL in config.yml.");
            return;
        }
        String username = getConfig().getString("webhook.username", "DELUXECLANS | Kisten");
        String avatar = getConfig().getString("webhook.avatar-url", "");
        StringBuilder json = new StringBuilder("{\"username\":\"").append(escape(username))
            .append("\",\"content\":\"").append(escape(content)).append("\"");
        if (!avatar.isBlank()) json.append(",\"avatar_url\":\"").append(escape(avatar)).append("\"");
        json.append("}");
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
            .header("Content-Type", "application/json; charset=utf-8")
            .POST(HttpRequest.BodyPublishers.ofString(json.toString(), StandardCharsets.UTF_8)).build();
        executor.submit(() -> http.sendAsync(request, HttpResponse.BodyHandlers.discarding())
            .thenAccept(response -> {
                if (response.statusCode() < 200 || response.statusCode() >= 300)
                    getLogger().warning("Discord webhook HTTP " + response.statusCode());
            }).exceptionally(error -> {
                getLogger().warning("Webhook failed: " + error.getMessage());
                return null;
            }));
    }

    private String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"")
            .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }

    @Override public void onDisable() {
        if (executor != null) executor.shutdownNow();
    }
}
