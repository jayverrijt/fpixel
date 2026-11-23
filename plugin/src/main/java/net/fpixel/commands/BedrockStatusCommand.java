package net.fpixel.commands;

import net.fpixel.FPixelPlugin;
import net.fpixel.core.util.ChatUtil;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Bedrock status management and Bedrock Named Error Code mapping (aligned with standard HTTP codes).
 * References: https://help.minecraft.net/hc/en-us/articles/38555497091469-Named-Error-Code-Troubleshooting-for-Minecraft-Bedrock-Edition
 */
public class BedrockStatusCommand implements CommandExecutor, TabCompleter {
    private final FPixelPlugin plugin;

    public record BedrockErrorDef(int httpCode, String name, String bedrockName, String description, String fix) {}

    private static final Map<Integer, BedrockErrorDef> ERRORS_BY_CODE = new LinkedHashMap<>();
    private static final Map<String, BedrockErrorDef> ERRORS_BY_NAME = new LinkedHashMap<>();

    static {
        registerError(200, "OK", "Geen", "Verbinding gezond, server bereikbaar, spelers kunnen joinen.", "Alles werkt optimaal.");
        registerError(100, "Outdated", "Outdated Client / Server", "Bedrock client of server versie verouderd / protocol mismatch.", "Update je Minecraft Bedrock app of wacht op server Geyser update.");
        registerError(400, "Bad Request", "Block, Creeper", "Ongeldig verbindingspakket, corrupte handshake of packet data parse error.", "Herstart Minecraft en wis tijdelijke gecachte serverdata.");
        registerError(401, "Unauthorized", "Drowned, Blaze, Axolotl, Glowstone", "Microsoft/Xbox authenticatie mislukt, token verlopen of Floodgate sessie ongeldig.", "Log opnieuw in op je Microsoft-account via het Minecraft hoofdmenu.");
        registerError(403, "Forbidden", "Spyglass, Coal", "Toegang geweigerd: niet op whitelist, verbannen of ouderlijk toezicht restrictie.", "Vraag toegang via fpixel.nl of controleer je Xbox Live privacy-instellingen.");
        registerError(404, "Not Found", "Goat", "Wereld of minigame arena niet gevonden of doellocatie bestaat niet.", "Controleer of de wereld/arena online staat of meld dit bij staff.");
        registerError(408, "Request Timeout", "Bat, Silverfish", "Verbindingstime-out: netwerkping te hoog of server reageert te traag.", "Controleer je wifi/4G-verbinding of herstart je thuisrouter.");
        registerError(410, "Gone", "Armadillo", "De wereld of arena is permanent afgesloten of uit de roulatie gehaald.", "Kies een andere minigame arena of lobby.");
        registerError(413, "Payload Too Large", "Chest, Rabbit", "Opslagruimte op toestel vol of resource pack / add-on te groot.", "Maak opslagruimte vrij via Bedrock Instellingen -> Opslag.");
        registerError(429, "Too Many Requests", "Kelp", "Te veel verbindingsverzoeken in korte tijd (rate-limited).", "Wacht 30 seconden voordat je opnieuw probeert te verbinden.");
        registerError(500, "Internal Server Error", "Emerald", "Interne Geyser / Paper serverfout bij packet-vertaling.", "Gebruik /fixinv of meld het probleem aan de beheerder.");
        registerError(502, "Bad Gateway", "Snowball", "Java backend server is offline of herstart terwijl Bedrock gateway luistert.", "Wacht 10 seconden tot de Java-server volledig is doorgestart.");
        registerError(503, "Service Unavailable", "Sand", "Bedrock service is down, poort 19132 onbereikbaar of onderhoud.", "De server is momenteel down of in onderhoud.");
        registerError(504, "Gateway Timeout", "Terracotta, NetherNet, Door", "Geen netwerkpad gevonden / STUN WebRTC signaling time-out.", "Gebruik poort 19132 (RakNet UDP) of controleer NAT/firewall instellingen.");
    }

    private static void registerError(int httpCode, String name, String bedrockName, String description, String fix) {
        BedrockErrorDef def = new BedrockErrorDef(httpCode, name, bedrockName, description, fix);
        ERRORS_BY_CODE.put(httpCode, def);
        ERRORS_BY_NAME.put(name.toLowerCase(), def);
        for (String bName : bedrockName.split(",")) {
            ERRORS_BY_NAME.put(bName.trim().toLowerCase(), def);
        }
    }

    public BedrockStatusCommand(FPixelPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length > 0) {
            String sub = args[0].toLowerCase();
            if (sub.equals("error") || sub.equals("err") || sub.equals("code") || sub.equals("help")) {
                if (args.length > 1) {
                    showErrorDetail(sender, args[1]);
                } else {
                    sender.sendMessage(ChatUtil.colorize("§eGebruik: §f/bedrock error <naam of code> §7(bijv. §e/bedrock error terracotta §7of §e/bedrock error 404§7)"));
                }
                return true;
            } else if (sub.equals("list") || sub.equals("errors") || sub.equals("all")) {
                showAllErrors(sender);
                return true;
            }
        }

        Plugin geyser = Bukkit.getPluginManager().getPlugin("Geyser-Spigot");
        Plugin floodgate = Bukkit.getPluginManager().getPlugin("floodgate");

        boolean geyserActive = geyser != null && geyser.isEnabled();
        boolean floodgateActive = floodgate != null && floodgate.isEnabled();

        int statusCode;
        String statusText;
        String color;

        if (!geyserActive) {
            statusCode = 503;
            statusText = "OFFLINE (Bedrock service is down)";
            color = "§c";
        } else {
            String geyserVer = geyser.getDescription().getVersion();
            if (geyserVer.contains("OUTDATED") || geyserVer.contains("deprecated")) {
                statusCode = 100;
                statusText = "OUTDATED (Bedrock versie is verouderd)";
                color = "§e";
            } else {
                statusCode = 200;
                statusText = "OK (Het spel is goed en spelers kunnen joinen)";
                color = "§a";
            }
        }

        List<String> bedrockPlayers = new ArrayList<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            boolean isBedrock = (plugin.getBedrockManager() != null && plugin.getBedrockManager().isBedrock(p))
                    || p.getName().startsWith(".");
            if (isBedrock) {
                bedrockPlayers.add(p.getName());
            }
        }

        sender.sendMessage(ChatUtil.colorize("§8━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        sender.sendMessage(ChatUtil.colorize("§6§l✦ FPIXEL BEDROCK STATUS & HTTP CODES ✦"));
        sender.sendMessage(ChatUtil.colorize(" §7Live Status: " + color + "§l[HTTP " + statusCode + "] " + statusText));
        sender.sendMessage(ChatUtil.colorize(" §7Server Adres: §fmc.fpixel.nl"));
        sender.sendMessage(ChatUtil.colorize(" §7Bedrock Poort: §e19132 §7(RakNet UDP + TCP Signaling)"));
        sender.sendMessage(ChatUtil.colorize(" §7NetherNet WebRTC: §e19133 §7(UDP Gameplay Stream)"));
        sender.sendMessage(ChatUtil.colorize(" §7Geyser: " + (geyserActive ? "§aActief (" + geyser.getDescription().getVersion() + ")" : "§cInactief")));
        sender.sendMessage(ChatUtil.colorize(" §7Floodgate: " + (floodgateActive ? "§aActief" : "§cInactief")));
        sender.sendMessage(ChatUtil.colorize(" §7Online Bedrock Spelers: §f" + bedrockPlayers.size() + (bedrockPlayers.isEmpty() ? "" : " §8(" + String.join(", ", bedrockPlayers) + "§8)")));
        sender.sendMessage(ChatUtil.colorize(" §eTip: §7Type §f/bedrock error <naam|code> §7voor uitleg (bijv. §e/bedrock error terracotta§7)"));
        sender.sendMessage(ChatUtil.colorize(" §eLijst: §7Type §f/bedrock list §7voor alle HTTP & Bedrock foutcodes"));
        sender.sendMessage(ChatUtil.colorize("§8━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));

        if (sender instanceof Player p) {
            ChatUtil.playSound(p, Sound.BLOCK_NOTE_BLOCK_PLING, 1f, statusCode == 200 ? 1.5f : 0.8f);
        }

        return true;
    }

    private void showErrorDetail(CommandSender sender, String query) {
        BedrockErrorDef def = null;
        try {
            int code = Integer.parseInt(query);
            def = ERRORS_BY_CODE.get(code);
        } catch (NumberFormatException ignored) {}

        if (def == null) {
            def = ERRORS_BY_NAME.get(query.toLowerCase());
        }

        if (def == null) {
            // Partial match
            for (Map.Entry<String, BedrockErrorDef> entry : ERRORS_BY_NAME.entrySet()) {
                if (entry.getKey().contains(query.toLowerCase()) || entry.getValue().bedrockName().toLowerCase().contains(query.toLowerCase())) {
                    def = entry.getValue();
                    break;
                }
            }
        }

        if (def == null) {
            sender.sendMessage(ChatUtil.colorize("§cOnbekende Bedrock foutcode of naam: '§e" + query + "§c'. Type §e/bedrock list §cvoor de lijst."));
            return;
        }

        String color = def.httpCode() == 200 ? "§a" : (def.httpCode() < 400 ? "§e" : "§c");
        sender.sendMessage(ChatUtil.colorize("§8━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        sender.sendMessage(ChatUtil.colorize("§6§l✦ BEDROCK FOUTCODE: " + color + "[HTTP " + def.httpCode() + " " + def.name() + "] §6§l✦"));
        sender.sendMessage(ChatUtil.colorize(" §7Bedrock Foutnaam: §f" + def.bedrockName()));
        sender.sendMessage(ChatUtil.colorize(" §7Oorzaak: §e" + def.description()));
        sender.sendMessage(ChatUtil.colorize(" §7Oplossing: §a" + def.fix()));
        sender.sendMessage(ChatUtil.colorize(" §8Bron: help.minecraft.net/hc/en-us/articles/38555497091469"));
        sender.sendMessage(ChatUtil.colorize("§8━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));

        if (sender instanceof Player p) {
            ChatUtil.playSound(p, Sound.UI_BUTTON_CLICK, 1f, 1.2f);
        }
    }

    private void showAllErrors(CommandSender sender) {
        sender.sendMessage(ChatUtil.colorize("§8━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        sender.sendMessage(ChatUtil.colorize("§6§l✦ BEDROCK ERROR -> HTTP CODES OVERZICHT ✦"));
        for (BedrockErrorDef def : ERRORS_BY_CODE.values()) {
            String col = def.httpCode() == 200 ? "§a" : (def.httpCode() < 400 ? "§e" : "§c");
            sender.sendMessage(ChatUtil.colorize(" " + col + "§l" + def.httpCode() + " §7[" + def.name() + "] §8-> §fBedrock: §e" + def.bedrockName()));
        }
        sender.sendMessage(ChatUtil.colorize(" §7Type §f/bedrock error <code> §7voor gedetailleerde oplossingen"));
        sender.sendMessage(ChatUtil.colorize("§8━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));

        if (sender instanceof Player p) {
            ChatUtil.playSound(p, Sound.BLOCK_NOTE_BLOCK_CHIME, 1f, 1f);
        }
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> options = List.of("error", "list", "help");
            List<String> matches = new ArrayList<>();
            for (String opt : options) {
                if (opt.startsWith(args[0].toLowerCase())) matches.add(opt);
            }
            return matches;
        } else if (args.length == 2 && args[0].equalsIgnoreCase("error")) {
            List<String> suggestions = new ArrayList<>();
            for (int code : ERRORS_BY_CODE.keySet()) {
                suggestions.add(String.valueOf(code));
            }
            suggestions.addAll(List.of("terracotta", "drowned", "creeper", "goat", "door", "nethernet", "block", "chest", "spyglass", "coal", "bat", "silverfish", "outdated"));
            List<String> matches = new ArrayList<>();
            for (String s : suggestions) {
                if (s.toLowerCase().startsWith(args[1].toLowerCase())) matches.add(s);
            }
            return matches;
        }
        return Collections.emptyList();
    }
}
