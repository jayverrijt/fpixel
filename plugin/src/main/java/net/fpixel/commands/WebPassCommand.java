package net.fpixel.commands;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fpixel.FPixelPlugin;
import net.fpixel.core.util.ChatUtil;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;

public class WebPassCommand implements CommandExecutor {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final FPixelPlugin plugin;

    public WebPassCommand(FPixelPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            // Console support: webpass <username> <password>
            if (args.length < 2) {
                sender.sendMessage("Gebruik: webpass <username> <password>");
                return true;
            }
            savePassword(args[0], args[1]);
            sender.sendMessage("Wachtwoord succesvol ingesteld voor " + args[0]);
            return true;
        }

        if (args.length < 1) {
            ChatUtil.sendMessage(player, "§c§lBEVEILIGING §8» §cGebruik: /webpass <wachtwoord>");
            ChatUtil.sendMessage(player, "§7Hiermee beveilig je jouw account op het webportaal (§ehttps://mc.fpixel.nl§7).");
            return true;
        }

        String password = args[0];
        if (password.length() < 4) {
            ChatUtil.sendMessage(player, "§c§lBEVEILIGING §8» §cHet wachtwoord moet minimaal 4 tekens bevatten.");
            return true;
        }

        savePassword(player.getName(), password);
        ChatUtil.sendMessage(player, "§a§lBEVEILIGING §8» §aWachtwoord succesvol ingesteld!");
        ChatUtil.sendMessage(player, "§7Je kunt nu veilig inloggen op §ehttps://mc.fpixel.nl §7met je spelersnaam en wachtwoord.");
        ChatUtil.playSound(player, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
        return true;
    }

    private void savePassword(String username, String password) {
        try {
            File dataFolder = plugin.getDataFolder();
            if (!dataFolder.exists()) {
                dataFolder.mkdirs();
            }
            File authFile = new File(dataFolder, "web_auth.json");

            JsonObject root = new JsonObject();
            if (authFile.exists()) {
                try {
                    String json = Files.readString(authFile.toPath());
                    if (!json.isBlank()) {
                        root = JsonParser.parseString(json).getAsJsonObject();
                    }
                } catch (Exception ignored) {}
            }

            // Generate 16-byte random salt
            byte[] saltBytes = new byte[16];
            new SecureRandom().nextBytes(saltBytes);
            String salt = HexFormat.of().formatHex(saltBytes);

            // Compute SHA-256 of (salt + password)
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = md.digest((salt + password).getBytes(StandardCharsets.UTF_8));
            String hash = HexFormat.of().formatHex(hashBytes);

            JsonObject userObj = new JsonObject();
            userObj.addProperty("username", username);
            userObj.addProperty("salt", salt);
            userObj.addProperty("hash", hash);
            userObj.addProperty("updatedAt", Instant.now().toString());

            root.add(username.toLowerCase(), userObj);

            Files.writeString(authFile.toPath(), GSON.toJson(root), StandardCharsets.UTF_8);
            plugin.getLogger().info("Web authentication updated for player: " + username);
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to save web password for " + username + ": " + e.getMessage());
        }
    }
}
