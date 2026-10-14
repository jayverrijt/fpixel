package net.fpixel.core.security;

import com.destroystokyo.paper.event.player.PlayerHandshakeEvent;
import net.fpixel.FPixelPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class HostnameListener implements Listener {

    private final FPixelPlugin plugin;
    private final Set<String> allowedHosts = new HashSet<>();
    private boolean enforce = true;

    public HostnameListener(FPixelPlugin plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    public void loadConfig() {
        allowedHosts.clear();
        this.enforce = plugin.getConfig().getBoolean("security.enforce-hostname", true);
        List<String> hosts = plugin.getConfig().getStringList("security.allowed-hosts");
        if (hosts != null && !hosts.isEmpty()) {
            for (String h : hosts) {
                if (h != null && !h.isBlank()) {
                    allowedHosts.add(cleanHost(h));
                }
            }
        } else {
            // Default enforcement to mc.fpixel.nl
            allowedHosts.add("mc.fpixel.nl");
        }

        // Always allow internal loopback and Docker networking for healthchecks & Geyser internal bridge
        allowedHosts.add("127.0.0.1");
        allowedHosts.add("localhost");
        allowedHosts.add("minecraft");
        allowedHosts.add("0.0.0.0");
    }

    private String cleanHost(String host) {
        if (host == null) return "";
        if (host.contains("\0")) {
            host = host.substring(0, host.indexOf("\0"));
        }
        if (host.contains(":")) {
            host = host.substring(0, host.indexOf(":"));
        }
        if (host.endsWith(".")) {
            host = host.substring(0, host.length() - 1);
        }
        return host.trim().toLowerCase();
    }

    public boolean isAllowed(String host) {
        if (!enforce) return true;
        if (host == null || host.isBlank()) return false;
        String clean = cleanHost(host);
        return allowedHosts.contains(clean);
    }

    public Component getRejectMessage() {
        return Component.text()
            .append(Component.text("✦ FPIXEL NETWORK ✦\n\n", NamedTextColor.GOLD, TextDecoration.BOLD))
            .append(Component.text("Verbinding geweigerd!\n", NamedTextColor.RED, TextDecoration.BOLD))
            .append(Component.text("Je kunt alleen inloggen via het officiële adres:\n", NamedTextColor.GRAY))
            .append(Component.text("mc.fpixel.nl\n\n", NamedTextColor.AQUA, TextDecoration.BOLD))
            .append(Component.text("Gebruik ", NamedTextColor.DARK_GRAY))
            .append(Component.text("mc.fpixel.nl", NamedTextColor.YELLOW))
            .append(Component.text(" in je Minecraft serverlijst!", NamedTextColor.DARK_GRAY))
            .build();
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerHandshake(PlayerHandshakeEvent event) {
        if (!enforce) return;
        String raw = event.getServerHostname();
        if (!isAllowed(raw)) {
            event.setCancelled(true);
            event.setFailed(true);
            event.failMessage(getRejectMessage());
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerLogin(PlayerLoginEvent event) {
        if (!enforce) return;
        String host = event.getHostname();
        if (!isAllowed(host)) {
            event.disallow(PlayerLoginEvent.Result.KICK_OTHER, getRejectMessage());
        }
    }
}
