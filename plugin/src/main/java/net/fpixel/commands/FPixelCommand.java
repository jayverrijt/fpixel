package net.fpixel.commands;

import net.fpixel.FPixelPlugin;
import net.fpixel.core.chunk.FPixelChunkHandler;
import net.fpixel.core.rank.Rank;
import net.fpixel.core.user.FPixelUser;
import net.fpixel.core.util.ChatUtil;
import net.fpixel.game.Arena;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class FPixelCommand implements CommandExecutor, TabCompleter {
    private final FPixelPlugin plugin;
    private final FPixelChunkHandler chunkHandler;

    public FPixelCommand(FPixelPlugin plugin, FPixelChunkHandler chunkHandler) {
        this.plugin = plugin;
        this.chunkHandler = chunkHandler;
    }

    private boolean checkAdminPermission(CommandSender sender) {
        if (sender.hasPermission("fpixel.admin") || sender.isOp()) return true;
        if (sender instanceof Player player) {
            Rank rank = plugin.getRankManager().getRank(player);
            return rank != null && rank.getWeight() >= 90;
        }
        return false;
    }

    private boolean checkModPermission(CommandSender sender) {
        if (sender.hasPermission("fpixel.admin") || sender.hasPermission("fpixel.mod") || sender.hasPermission("fpixel.moderator") || sender.isOp()) return true;
        if (sender instanceof Player player) {
            Rank rank = plugin.getRankManager().getRank(player);
            return rank != null && rank.getWeight() >= Rank.MODERATOR.getWeight();
        }
        return false;
    }

    private boolean checkOwnerPermission(CommandSender sender) {
        if (sender.hasPermission("fpixel.owner") || sender.isOp()) return true;
        if (sender instanceof Player player) {
            Rank rank = plugin.getRankManager().getRank(player);
            return rank != null && rank.getWeight() >= Rank.OWNER.getWeight();
        }
        return false;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] rawArgs) {
        String cmdLabel = label.toLowerCase();
        if (cmdLabel.startsWith("/")) cmdLabel = cmdLabel.substring(1);

        String[] args;
        if (cmdLabel.contains("fill")) {
            args = new String[rawArgs.length + 1];
            args[0] = "fill";
            System.arraycopy(rawArgs, 0, args, 1, rawArgs.length);
        } else if (cmdLabel.contains("empty")) {
            args = new String[rawArgs.length + 1];
            args[0] = "empty";
            System.arraycopy(rawArgs, 0, args, 1, rawArgs.length);
        } else if (cmdLabel.equals("undo")) {
            args = new String[]{"undo"};
        } else {
            args = rawArgs;
        }

        if (args.length == 0) {
            if (!checkModPermission(sender)) {
                sender.sendMessage(ChatUtil.colorize("§cYou do not have permission to execute this command."));
                return true;
            }
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase();

        // ------------------------------------------
        // ADVANCED PARKOUR STUDIO & REVIEW
        // ------------------------------------------
        if (sub.equals("parkour")) {
            return handleParkourCommand(sender, args);
        }

        if (sub.equals("test")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("Alleen spelers kunnen /fpixel test gebruiken.");
                return true;
            }
            if (!checkModPermission(sender)) {
                ChatUtil.sendMessage(player, "§cJe hebt minstens de §2[MOD] §crang nodig om parkour maps te reviewen.");
                return true;
            }
            if (args.length < 2) {
                ChatUtil.sendMessage(player, "§cGebruik: §e/fpixel test <map-id>");
                return true;
            }
            plugin.getParkourManager().startTestingSession(player, args[1]);
            return true;
        }

        if (sub.equals("approve")) {
            if (!checkOwnerPermission(sender)) {
                sender.sendMessage(ChatUtil.colorize("§cAlleen [OWNER] rang kan parkour maps goedkeuren."));
                return true;
            }
            if (args.length < 2) {
                sender.sendMessage(ChatUtil.colorize("§cGebruik: /fpixel approve <map-id>"));
                return true;
            }
            plugin.getParkourManager().approveMap(sender, args[1]);
            return true;
        }

        if (sub.equals("disable") || sub.equals("deactivate")) {
            if (!checkOwnerPermission(sender)) {
                sender.sendMessage(ChatUtil.colorize("§cAlleen [OWNER] rang kan parkour maps uit circulatie halen."));
                return true;
            }
            if (args.length < 2) {
                sender.sendMessage(ChatUtil.colorize("§cGebruik: /fpixel disable <map-id>"));
                return true;
            }
            plugin.getParkourManager().disableMap(sender, args[1]);
            return true;
        }

        if (sub.equals("hub")) {
            if (args.length > 1 && args[1].equalsIgnoreCase("rebuild")) {
                if (!checkOwnerPermission(sender)) {
                    sender.sendMessage(ChatUtil.colorize("§cAlleen [OWNER] rang kan de hub herbouwen."));
                    return true;
                }
                World world = (sender instanceof Player p) ? p.getWorld() : Bukkit.getWorlds().get(0);
                plugin.getHubGenerator().generateCourtyardHub(world);
                plugin.getHubPortalManager().setupPortals(world, 64);
                sender.sendMessage(ChatUtil.colorize("§a§l[Hub] §eDe Courtyard Hub en portalen zijn succesvol herbouwd!"));
                return true;
            }
        }

        if (sub.equals("wand")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("Alleen spelers kunnen /fpixel wand gebruiken.");
                return true;
            }
            plugin.getMapMakerManager().giveSetupWand(player);
            ChatUtil.sendMessage(player, "§a§l[Map Maker] §eJe hebt de Setup Wand ontvangen!");
            return true;
        }

        if (sub.equals("map")) {
            return handleMapCommand(sender, args);
        }

        if (sub.equals("world")) {
            return handleWorldCommand(sender, args);
        }

        // All other administrative commands require Admin permission (weight >= 90 or OP)
        if (!checkAdminPermission(sender)) {
            sender.sendMessage(ChatUtil.colorize("§cYou do not have permission to execute this command."));
            return true;
        }

        // ------------------------------------------
        // UNIFIED /fpixel fill
        // ------------------------------------------
        if (sub.equals("fill")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("Alleen spelers kunnen /fpixel fill gebruiken.");
                return true;
            }

            if (args.length < 3) {
                ChatUtil.sendMessage(player, "§c§lCHUNK §8» §cGebruik: §e/fpixel fill <startY> <endY> [blok] [all|safe|oneside]");
                return true;
            }

            int playerY = player.getLocation().getBlockY();
            int fromY, toY;
            try {
                fromY = parseY(args[1], playerY);
                toY = parseY(args[2], playerY);
            } catch (NumberFormatException e) {
                ChatUtil.sendMessage(player, "§c§lCHUNK §8» §cOngeldige Y-waarden! Gebruik getallen of ~ (bijv. 52 54)");
                return true;
            }

            int minY = Math.max(player.getWorld().getMinHeight(), Math.min(fromY, toY));
            int maxY = Math.min(player.getWorld().getMaxHeight() - 1, Math.max(fromY, toY));

            Material fillMaterial = Material.STONE;
            String mode = "all";

            for (int i = 3; i < args.length; i++) {
                String a = args[i].toLowerCase();
                if (a.equals("all") || a.equals("safe") || a.equals("oneside")) {
                    mode = a;
                    continue;
                }
                Material m = Material.matchMaterial(args[i]);
                if (m != null && m.isBlock()) {
                    fillMaterial = m;
                }
            }

            Chunk chunk;
            Block targetLook = player.getTargetBlockExact(64);
            if (targetLook != null && player.isSneaking()) {
                chunk = targetLook.getChunk();
            } else {
                chunk = player.getLocation().getChunk();
            }

            if (mode.equals("safe")) {
                chunkHandler.fillSafe(player, chunk, minY, maxY, fillMaterial);
            } else if (mode.equals("oneside")) {
                chunkHandler.fillOneSide(player, chunk, minY, maxY, fillMaterial);
            } else {
                chunkHandler.fillAll(player, chunk, minY, maxY, fillMaterial);
            }
            return true;
        }

        // ------------------------------------------
        // UNIFIED /fpixel empty
        // ------------------------------------------
        if (sub.equals("empty")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("Alleen spelers kunnen /fpixel empty gebruiken.");
                return true;
            }

            Chunk chunk;
            Block targetLook = player.getTargetBlockExact(64);
            if (targetLook != null && player.isSneaking()) {
                chunk = targetLook.getChunk();
            } else {
                chunk = player.getLocation().getChunk();
            }

            if (args.length == 1) {
                chunkHandler.emptyAll(player, chunk);
                return true;
            }

            String option = args[1].toLowerCase();
            if (option.equals("safe")) {
                chunkHandler.emptySafe(player, chunk);
            } else if (option.equals("ignore-water") || option.equals("ignorewater") || option.equals("keepwater")) {
                chunkHandler.emptyIgnoreWater(player, chunk);
            } else if (option.equals("ignore-water-and-box") || option.equals("ignorewaterandbox") || option.equals("boxwater")) {
                chunkHandler.emptyIgnoreWaterAndBox(player, chunk);
            } else if (option.equals("glass")) {
                chunkHandler.emptyGlass(player, chunk);
            } else if (option.equals("sponge") || option.equals("sponges")) {
                chunkHandler.emptySponge(player, chunk);
            } else {
                ChatUtil.sendMessage(player, "§c§lCHUNK §8» §cOnbekende optie: §e" + args[1]);
                ChatUtil.sendMessage(player, "§7» §eOpties: §fsafe, ignore-water, ignore-water-and-box, glass, sponge");
            }
            return true;
        }

        // ------------------------------------------
        // UNIFIED /fpixel undo
        // ------------------------------------------
        if (sub.equals("undo")) {
            if (sender instanceof Player player) {
                chunkHandler.undo(player);
            } else {
                sender.sendMessage("Alleen spelers kunnen undo gebruiken.");
            }
            return true;
        }

        // ------------------------------------------
        // Network Admin Commands
        // ------------------------------------------
        if (sub.equals("setlobby") && sender instanceof Player player) {
            plugin.getLobbyManager().setSpawn(player.getLocation());
            ChatUtil.sendMessage(player, "§aLobby spawn point updated to your current location!");
        } else if (sub.equals("reload")) {
            plugin.reloadConfig();
            plugin.getRankManager().loadRanks();
            plugin.getLobbyManager().loadSpawn();
            sender.sendMessage(ChatUtil.colorize("§aFPixel configuration and ranks reloaded successfully!"));
        } else if (sub.equals("rebuildhub") || sub.equals("hub")) {
            sender.sendMessage(ChatUtil.colorize("§eRebuilding Grand Hub with NPCs, launchpads, and survival-inspired architecture..."));
            plugin.getWorldManager().rebuildGrandHub(Bukkit.getWorlds().get(0));
            sender.sendMessage(ChatUtil.colorize("§aGrand Hub rebuild complete! All 8 gamemode pavilions, launchpads, and NPCs are live."));
        } else if (sub.equals("buildmaps")) {
            sender.sendMessage(ChatUtil.colorize("§eBuilding Grand Hub and 30 custom minigame maps..."));
            plugin.getWorldManager().generateAllMaps(Bukkit.getWorlds().get(0));
            sender.sendMessage(ChatUtil.colorize("§aAll 30 maps generated and registered successfully!"));
        } else if (sub.equals("list")) {
            sender.sendMessage(ChatUtil.colorize("§6§lActive Arenas:"));
            for (Arena arena : plugin.getGameManager().getArenas()) {
                sender.sendMessage(ChatUtil.colorize(" §e• §f" + arena.getId() + " §7(" + arena.getGameType() + "): "
                        + arena.getState().getDisplay() + " §7[" + arena.getPlayerCount() + "/" + arena.getMaxPlayers() + "]"));
            }
        } else if (sub.equals("addcoins") && args.length >= 3) {
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage(ChatUtil.colorize("§cPlayer not found."));
                return true;
            }
            try {
                int amount = Integer.parseInt(args[2]);
                FPixelUser user = plugin.getUserManager().getUser(target);
                user.addCoins(amount);
                sender.sendMessage(ChatUtil.colorize("§aGave " + amount + " coins to " + target.getName() + "."));
                ChatUtil.sendMessage(target, "§6+" + amount + " Coins §7added to your balance!");
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatUtil.colorize("§cInvalid amount."));
            }
        } else if (sub.equals("import")) {
            if (args.length < 2 || args[1].equalsIgnoreCase("list")) {
                List<java.io.File> zips = plugin.getMapImporter() != null ? plugin.getMapImporter().getAvailableZipFiles() : Collections.emptyList();
                ChatUtil.sendMessage(sender, "§6§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                ChatUtil.sendMessage(sender, "§e§l           MAP & HUB IMPORTER");
                ChatUtil.sendMessage(sender, "§7Plaats .zip bestanden in §bimport_worlds/ §7of §bimport_maps/§7.");
                ChatUtil.sendMessage(sender, "§fBeschikbare ZIP bestanden:");
                if (zips.isEmpty()) {
                    ChatUtil.sendMessage(sender, " §7(Geen gevonden in import_worlds/)");
                } else {
                    for (java.io.File z : zips) {
                        ChatUtil.sendMessage(sender, " §a• " + z.getName() + " §7(" + (z.length() / (1024 * 1024)) + "MB)");
                    }
                }
                ChatUtil.sendMessage(sender, "§fActieve geimporteerde maps:");
                if (plugin.getMapImporter() != null) {
                    for (String m : plugin.getMapImporter().getImportedMaps()) {
                        ChatUtil.sendMessage(sender, " §b• " + m);
                    }
                }
                ChatUtil.sendMessage(sender, "§7Commando's:");
                ChatUtil.sendMessage(sender, " §e/fpixel import hub <zipnaam> §7- Importeer custom Hub");
                ChatUtil.sendMessage(sender, " §e/fpixel import map <gametype> <naam> <zipnaam> §7- Importeer minigame map");
                ChatUtil.sendMessage(sender, " §e/fpixel import scan §7- Scan & verwerk alle nieuwe maps");
                ChatUtil.sendMessage(sender, "§6§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                return true;
            }

            String action = args[1].toLowerCase();
            if (action.equals("scan") || action.equals("auto")) {
                if (plugin.getMapImporter() != null) {
                    plugin.getMapImporter().autoScanAndImport();
                    ChatUtil.sendMessage(sender, "§aAuto-scan voltooid!");
                }
            } else if (action.equals("hub")) {
                if (args.length < 3) {
                    ChatUtil.sendMessage(sender, "§cGebruik: §e/fpixel import hub <bestand.zip>");
                    return true;
                }
                if (plugin.getMapImporter() != null) {
                    plugin.getMapImporter().importHub(sender, args[2]);
                }
            } else if (action.equals("map")) {
                if (args.length < 5) {
                    ChatUtil.sendMessage(sender, "§cGebruik: §e/fpixel import map <BEDWARS|SKYWARS|BRIDGE|SUMO|TNTRUN|PARKOUR|MM|PH> <MapNaam> <bestand.zip>");
                    return true;
                }
                String gameType = args[2];
                String displayName = args[3];
                String zipFile = args[4];
                if (plugin.getMapImporter() != null) {
                    plugin.getMapImporter().importMinigameMap(sender, gameType, displayName, zipFile);
                }
            }
            return true;
        } else if (sub.equals("update")) {
            if (!sender.hasPermission("fpixel.owner") && !sender.isOp()) {
                sender.sendMessage(ChatUtil.colorize("§cAlleen Owners kunnen de server updaten/stoppen."));
                return true;
            }
            Bukkit.dispatchCommand(sender, "serverupdate");
            return true;
        } else if (sub.equals("backup")) {
            if (!sender.hasPermission("fpixel.owner") && !sender.isOp()) {
                sender.sendMessage(ChatUtil.colorize("§cAlleen Owners kunnen handmatige server backups uitvoeren."));
                return true;
            }
            Bukkit.dispatchCommand(sender, "serverbackup");
            return true;
        } else {
            sendHelp(sender);
        }

        return true;
    }

    private boolean handleParkourCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sendParkourHelp(sender);
            return true;
        }

        String action = args[1].toLowerCase();

        switch (action) {
            case "create" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("Alleen spelers kunnen /fpixel parkour create gebruiken.");
                    return true;
                }
                if (!checkModPermission(player)) {
                    ChatUtil.sendMessage(player, "§cJe hebt minstens de §2[MOD] §crang nodig om een parkour map te ontwerpen.");
                    return true;
                }
                String name = "";
                if (args.length > 2) {
                    StringBuilder sb = new StringBuilder();
                    for (int i = 2; i < args.length; i++) {
                        if (i > 2) sb.append(" ");
                        sb.append(args[i]);
                    }
                    name = sb.toString();
                }
                plugin.getParkourManager().createMap(player, name);
                return true;
            }
            case "test" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("Alleen spelers kunnen test gebruiken.");
                    return true;
                }
                if (!checkModPermission(player)) {
                    ChatUtil.sendMessage(player, "§cJe hebt minstens de §2[MOD] §crang nodig om parkour maps te reviewen.");
                    return true;
                }
                if (args.length < 3) {
                    ChatUtil.sendMessage(player, "§cGebruik: §e/fpixel parkour test <map-id>");
                    return true;
                }
                plugin.getParkourManager().startTestingSession(player, args[2]);
                return true;
            }
            case "approve" -> {
                if (!checkOwnerPermission(sender)) {
                    sender.sendMessage(ChatUtil.colorize("§cAlleen spelers met de [OWNER] rang kunnen parkour maps goedkeuren."));
                    return true;
                }
                if (args.length < 3) {
                    sender.sendMessage(ChatUtil.colorize("§cGebruik: /fpixel parkour approve <map-id>"));
                    return true;
                }
                plugin.getParkourManager().approveMap(sender, args[2]);
                return true;
            }
            case "reject" -> {
                if (!checkOwnerPermission(sender)) {
                    sender.sendMessage(ChatUtil.colorize("§cAlleen spelers met de [OWNER] rang kunnen parkour maps afkeuren."));
                    return true;
                }
                if (args.length < 3) {
                    sender.sendMessage(ChatUtil.colorize("§cGebruik: /fpixel parkour reject <map-id> [reden]"));
                    return true;
                }
                String reason = "";
                if (args.length > 3) {
                    StringBuilder sb = new StringBuilder();
                    for (int i = 3; i < args.length; i++) {
                        if (i > 3) sb.append(" ");
                        sb.append(args[i]);
                    }
                    reason = sb.toString();
                }
                plugin.getParkourManager().rejectMap(sender, args[2], reason);
                return true;
            }
            case "disable", "deactivate", "unapprove" -> {
                if (!checkOwnerPermission(sender)) {
                    sender.sendMessage(ChatUtil.colorize("§cAlleen spelers met de [OWNER] rang kunnen parkour maps uit circulatie halen."));
                    return true;
                }
                if (args.length < 3) {
                    sender.sendMessage(ChatUtil.colorize("§cGebruik: /fpixel parkour disable <map-id>"));
                    return true;
                }
                plugin.getParkourManager().disableMap(sender, args[2]);
                return true;
            }
            case "enable" -> {
                if (!checkOwnerPermission(sender)) {
                    sender.sendMessage(ChatUtil.colorize("§cAlleen spelers met de [OWNER] rang kunnen parkour maps activeren."));
                    return true;
                }
                if (args.length < 3) {
                    sender.sendMessage(ChatUtil.colorize("§cGebruik: /fpixel parkour enable <map-id>"));
                    return true;
                }
                plugin.getParkourManager().approveMap(sender, args[2]);
                return true;
            }
            case "setstart" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("Alleen spelers kunnen setstart gebruiken.");
                    return true;
                }
                if (!checkModPermission(player)) {
                    ChatUtil.sendMessage(player, "§cJe hebt minstens de §2[MOD] §crang nodig.");
                    return true;
                }
                String mapId = plugin.getParkourManager().getEditingMapId(player);
                if (mapId == null) {
                    ChatUtil.sendMessage(player, "§cJe bent momenteel geen map aan het bewerken! Gebruik eerst §e/fpixel parkour build <id> §cof §e/fpixel parkour create§c.");
                    return true;
                }
                net.fpixel.games.parkour.CustomParkourMap map = plugin.getParkourManager().getMap(mapId);
                if (map != null) {
                    map.setStart(player.getLocation());
                    plugin.getParkourManager().saveMaps();
                    ChatUtil.sendMessage(player, "§aStartpunt voor map §e" + mapId + " §abijgewerkt naar je huidige positie!");
                }
                return true;
            }
            case "checkpoint", "addcheckpoint" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("Alleen spelers kunnen checkpoints toevoegen.");
                    return true;
                }
                if (!checkModPermission(player)) {
                    ChatUtil.sendMessage(player, "§cJe hebt minstens de §2[MOD] §crang nodig.");
                    return true;
                }
                String mapId = plugin.getParkourManager().getEditingMapId(player);
                if (mapId == null) {
                    ChatUtil.sendMessage(player, "§cJe bent momenteel geen map aan het bewerken! Gebruik eerst §e/fpixel parkour build <id>§c.");
                    return true;
                }
                net.fpixel.games.parkour.CustomParkourMap map = plugin.getParkourManager().getMap(mapId);
                if (map != null) {
                    map.addCheckpoint(player.getLocation());
                    plugin.getParkourManager().saveMaps();
                    ChatUtil.sendMessage(player, "§aCheckpoint #" + map.getCheckpoints().size() + " toegevoegd op je huidige positie!");
                }
                return true;
            }
            case "setfinish" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("Alleen spelers kunnen setfinish gebruiken.");
                    return true;
                }
                if (!checkModPermission(player)) {
                    ChatUtil.sendMessage(player, "§cJe hebt minstens de §2[MOD] §crang nodig.");
                    return true;
                }
                String mapId = plugin.getParkourManager().getEditingMapId(player);
                if (mapId == null) {
                    ChatUtil.sendMessage(player, "§cJe bent momenteel geen map aan het bewerken! Gebruik eerst §e/fpixel parkour build <id>§c.");
                    return true;
                }
                net.fpixel.games.parkour.CustomParkourMap map = plugin.getParkourManager().getMap(mapId);
                if (map != null) {
                    map.setFinish(player.getLocation());
                    plugin.getParkourManager().saveMaps();
                    ChatUtil.sendMessage(player, "§6Finish lijn voor map §e" + mapId + " §6ingesteld op je huidige positie!");
                }
                return true;
            }
            case "clearcheckpoints" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("Alleen spelers kunnen checkpoints wissen.");
                    return true;
                }
                String mapId = plugin.getParkourManager().getEditingMapId(player);
                if (mapId == null) {
                    ChatUtil.sendMessage(player, "§cJe bent momenteel geen map aan het bewerken!");
                    return true;
                }
                net.fpixel.games.parkour.CustomParkourMap map = plugin.getParkourManager().getMap(mapId);
                if (map != null) {
                    map.getCheckpoints().clear();
                    plugin.getParkourManager().saveMaps();
                    ChatUtil.sendMessage(player, "§cAlle checkpoints gewist voor map §e" + mapId + "§c.");
                }
                return true;
            }
            case "build" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("Alleen spelers kunnen bouwen.");
                    return true;
                }
                if (!checkModPermission(player)) {
                    ChatUtil.sendMessage(player, "§cJe hebt minstens de §2[MOD] §crang nodig.");
                    return true;
                }
                if (args.length < 3) {
                    ChatUtil.sendMessage(player, "§cGebruik: §e/fpixel parkour build <map-id>");
                    return true;
                }
                net.fpixel.games.parkour.CustomParkourMap map = plugin.getParkourManager().getMap(args[2]);
                if (map == null) {
                    ChatUtil.sendMessage(player, "§cGeen parkour map gevonden met ID: §e" + args[2]);
                    return true;
                }
                World w = plugin.getParkourManager().getOrCreateParkourWorld();
                Location start = map.getStartLocation(w);
                if (start != null) {
                    plugin.getParkourManager().setEditingMapId(player, map.getId());
                    player.teleport(start.clone().add(0, 0.2, 0));
                    player.setGameMode(GameMode.CREATIVE);
                    player.setAllowFlight(true);
                    player.setFlying(true);
                    plugin.getParkourManager().giveBuilderTools(player, map);
                    ChatUtil.sendMessage(player, "§aJe bewerkt nu map: §e" + map.getName() + " §7(" + map.getId() + ")");
                }
                return true;
            }
            case "list" -> {
                sender.sendMessage(ChatUtil.colorize("§6§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
                sender.sendMessage(ChatUtil.colorize("§e§l         COMMUNITY PARKOUR MAPS"));
                Collection<net.fpixel.games.parkour.CustomParkourMap> all = plugin.getParkourManager().getAllMaps();
                if (all.isEmpty()) {
                    sender.sendMessage(ChatUtil.colorize(" §7(Nog geen custom maps aangemaakt. Gebruik /fpixel parkour create)"));
                } else {
                    for (net.fpixel.games.parkour.CustomParkourMap m : all) {
                        String stColor = m.isApproved() ? "§a" : (m.getStatus().equals("REJECTED") ? "§c" : "§e");
                        sender.sendMessage(ChatUtil.colorize(" §e• §b" + m.getId() + " §f\"" + m.getName() + "\" §7door §a" + m.getCreatorName() +
                                " " + stColor + "[" + m.getStatus() + "] §7(" + m.getCheckpoints().size() + " CPs)"));
                    }
                }
                sender.sendMessage(ChatUtil.colorize("§7Testen: §e/fpixel test <id> §7| Goedkeuren: §e/fpixel approve <id>"));
                sender.sendMessage(ChatUtil.colorize("§6§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
                return true;
            }
            case "reload" -> {
                plugin.getParkourManager().loadMaps();
                sender.sendMessage(ChatUtil.colorize("§aParkour maps herladen en actieve arena's gesynchroniseerd!"));
                return true;
            }
            default -> {
                sendParkourHelp(sender);
                return true;
            }
        }
    }

    private void sendParkourHelp(CommandSender sender) {
        sender.sendMessage(ChatUtil.colorize("§6§lParkour Creator Studio Commando's:"));
        sender.sendMessage(ChatUtil.colorize(" §e/fpixel parkour create [naam] §7- Maak een nieuwe lege parkour canvas (MOD+)"));
        sender.sendMessage(ChatUtil.colorize(" §e/fpixel parkour build <id> §7- Bewerk een bestaande parkour map"));
        sender.sendMessage(ChatUtil.colorize(" §e/fpixel parkour setstart §7- Verplaats startpunt naar jouw locatie"));
        sender.sendMessage(ChatUtil.colorize(" §e/fpixel parkour checkpoint §7- Voeg checkpoint toe"));
        sender.sendMessage(ChatUtil.colorize(" §e/fpixel parkour setfinish §7- Verplaats finishlijn"));
        sender.sendMessage(ChatUtil.colorize(" §e/fpixel parkour list §7- Bekijk overzicht van alle custom maps"));
        sender.sendMessage(ChatUtil.colorize(" §e/fpixel test <id> §7- Test / review een parkour parcours"));
        sender.sendMessage(ChatUtil.colorize(" §e/fpixel parkour approve <id> §7- Keur map goed & activeer in games (OWNER)"));
        sender.sendMessage(ChatUtil.colorize(" §e/fpixel parkour disable <id> §7- Haal map uit circulatie (OWNER)"));
        sender.sendMessage(ChatUtil.colorize(" §e/fpixel parkour enable <id> §7- Activeer map opnieuw in games (OWNER)"));
        sender.sendMessage(ChatUtil.colorize(" §e/fpixel parkour reject <id> [reden] §7- Wijs map af (OWNER)"));
    }

    private int parseY(String arg, int playerY) {
        if (arg.equals("~")) return playerY;
        if (arg.startsWith("~")) return playerY + Integer.parseInt(arg.substring(1));
        return Integer.parseInt(arg);
    }

    private boolean handleMapCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatUtil.colorize("§cGebruik: /fpixel map <create|test|approve|reject|disable|list>"));
            return true;
        }

        String action = args[1].toLowerCase();
        Player staff = (sender instanceof Player p) ? p : null;

        if (action.equals("create")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("Alleen spelers kunnen /fpixel map create uitvoeren.");
                return true;
            }
            if (args.length < 4) {
                ChatUtil.sendMessage(player, "§cGebruik: /fpixel map create <naam> <BEDWARS|SKYWARS|PARKOUR|...> [teams]");
                return true;
            }
            String name = args[2];
            net.fpixel.games.maker.CustomMinigameMap.GameType type = net.fpixel.games.maker.CustomMinigameMap.GameType.fromString(args[3]);
            int teams = 2;
            if (args.length > 4) {
                try { teams = Integer.parseInt(args[4]); } catch (Exception ignored) {}
            }
            plugin.getMapMakerManager().createMap(null, name, type, teams, player);
            return true;
        }

        if (action.equals("test")) {
            if (staff == null) {
                sender.sendMessage("Alleen spelers kunnen een map testen.");
                return true;
            }
            if (!checkModPermission(sender)) {
                sender.sendMessage(ChatUtil.colorize("§cJe hebt minstens de [MODERATOR] rang nodig om maps te testen."));
                return true;
            }
            if (args.length < 3) {
                sender.sendMessage(ChatUtil.colorize("§cGebruik: /fpixel map test <id>"));
                return true;
            }
            plugin.getMapMakerManager().testMap(args[2], staff);
            return true;
        }

        if (action.equals("approve")) {
            if (!checkModPermission(sender)) {
                sender.sendMessage(ChatUtil.colorize("§cJe hebt minstens de [MODERATOR] rang nodig."));
                return true;
            }
            if (args.length < 3) {
                sender.sendMessage(ChatUtil.colorize("§cGebruik: /fpixel map approve <id>"));
                return true;
            }
            plugin.getMapMakerManager().approveMap(args[2], staff);
            sender.sendMessage(ChatUtil.colorize("§aMap " + args[2] + " goedgekeurd!"));
            return true;
        }

        if (action.equals("reject")) {
            if (!checkModPermission(sender)) {
                sender.sendMessage(ChatUtil.colorize("§cJe hebt minstens de [MODERATOR] rang nodig."));
                return true;
            }
            if (args.length < 3) {
                sender.sendMessage(ChatUtil.colorize("§cGebruik: /fpixel map reject <id> [reden]"));
                return true;
            }
            String reason = args.length > 3 ? String.join(" ", Arrays.copyOfRange(args, 3, args.length)) : "Niet goedgekeurd door staff";
            plugin.getMapMakerManager().rejectMap(args[2], reason, staff);
            sender.sendMessage(ChatUtil.colorize("§cMap " + args[2] + " afgekeurd met reden: " + reason));
            return true;
        }

        if (action.equals("disable") || action.equals("archive")) {
            if (!checkModPermission(sender)) {
                sender.sendMessage(ChatUtil.colorize("§cJe hebt minstens de [MODERATOR] rang nodig."));
                return true;
            }
            if (args.length < 3) {
                sender.sendMessage(ChatUtil.colorize("§cGebruik: /fpixel map disable <id>"));
                return true;
            }
            plugin.getMapMakerManager().disableMap(args[2], staff);
            return true;
        }

        sender.sendMessage(ChatUtil.colorize("§cOnbekend map subcommando: " + action));
        return true;
    }

    private boolean handleWorldCommand(CommandSender sender, String[] args) {
        if (!checkAdminPermission(sender)) {
            sender.sendMessage(ChatUtil.colorize("§cAlleen administrators en RCON hebben toegang tot /fpixel world."));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatUtil.colorize("§cGebruik: /fpixel world <create|delete|edit|share|unshare|backup|restore>"));
            return true;
        }

        String action = args[1].toLowerCase();
        var wm = plugin.getPlayerWorldManager();
        if (wm == null) {
            sender.sendMessage("PlayerWorldManager is niet beschikbaar.");
            return true;
        }

        if (action.equals("create")) {
            if (args.length < 5) {
                sender.sendMessage("Gebruik: /fpixel world create <uid> <name> <creator> [seed] [cheats]");
                return true;
            }
            String uid = args[2];
            String name = args[3].replace("_", " ");
            String creator = args[4];
            long seed = args.length > 5 ? Long.parseLong(args[5]) : 0L;
            boolean cheats = args.length > 6 && Boolean.parseBoolean(args[6]);

            wm.createWorld(uid, name, creator, null, seed, cheats);
            sender.sendMessage("OK: World " + uid + " created successfully.");
            return true;
        }

        if (action.equals("delete")) {
            if (args.length < 3) {
                sender.sendMessage("Gebruik: /fpixel world delete <uid>");
                return true;
            }
            boolean ok = wm.deleteWorld(args[2]);
            sender.sendMessage(ok ? "OK: World " + args[2] + " deleted." : "ERROR: World could not be deleted.");
            return true;
        }

        if (action.equals("edit")) {
            if (args.length < 5) {
                sender.sendMessage("Gebruik: /fpixel world edit <uid> <name> <cheatsEnabled>");
                return true;
            }
            var pw = wm.getWorld(args[2]);
            if (pw == null) {
                sender.sendMessage("ERROR: World not found.");
                return true;
            }
            pw.setName(args[3].replace("_", " "));
            pw.setCheatsEnabled(Boolean.parseBoolean(args[4]));
            wm.saveWorlds();
            sender.sendMessage("OK: World " + args[2] + " updated.");
            return true;
        }

        if (action.equals("share")) {
            if (args.length < 5) {
                sender.sendMessage("Gebruik: /fpixel world share <uid> <player> <allowCheats>");
                return true;
            }
            var pw = wm.getWorld(args[2]);
            if (pw == null) {
                sender.sendMessage("ERROR: World not found.");
                return true;
            }
            pw.getSharedPlayers().put(args[3].toLowerCase(), Boolean.parseBoolean(args[4]));
            wm.saveWorlds();
            sender.sendMessage("OK: Shared world " + args[2] + " with " + args[3]);
            return true;
        }

        if (action.equals("unshare")) {
            if (args.length < 4) {
                sender.sendMessage("Gebruik: /fpixel world unshare <uid> <player>");
                return true;
            }
            var pw = wm.getWorld(args[2]);
            if (pw == null) {
                sender.sendMessage("ERROR: World not found.");
                return true;
            }
            pw.getSharedPlayers().remove(args[3].toLowerCase());
            wm.saveWorlds();
            sender.sendMessage("OK: Unshared " + args[3] + " from world " + args[2]);
            return true;
        }

        if (action.equals("backup")) {
            if (args.length < 3) {
                sender.sendMessage("Gebruik: /fpixel world backup <uid> [requester]");
                return true;
            }
            String uid = args[2];
            String req = args.length > 3 ? args[3] : "Console";
            wm.createGitBackup(uid, req,
                    () -> sender.sendMessage("OK: Backup created for world " + uid),
                    err -> sender.sendMessage("ERROR: " + err));
            return true;
        }

        if (action.equals("restore")) {
            if (args.length < 4) {
                sender.sendMessage("Gebruik: /fpixel world restore <uid> <commitHash>");
                return true;
            }
            wm.restoreGitBackup(args[2], args[3],
                    () -> sender.sendMessage("OK: World " + args[2] + " successfully restored to " + args[3]),
                    err -> sender.sendMessage("ERROR: " + err));
            return true;
        }

        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatUtil.colorize("§6§lFPixel Unified Commands:"));
        sender.sendMessage(ChatUtil.colorize(" §e/fpixel parkour create [naam] §7- Bouw een parkour map in de void (MOD+)"));
        sender.sendMessage(ChatUtil.colorize(" §e/fpixel parkour list §7- Bekijk alle gemaakte parkour maps"));
        sender.sendMessage(ChatUtil.colorize(" §e/fpixel test <id> §7- Test / review een parkour parcours"));
        sender.sendMessage(ChatUtil.colorize(" §e/fpixel parkour approve <id> §7- Keur parkour map goed (OWNER)"));
        sender.sendMessage(ChatUtil.colorize(" §e/fpixel fill <vanY> <totY> [blok] [all|safe|oneside] §7- Vul chunk"));
        sender.sendMessage(ChatUtil.colorize("   §f• all: §7vult hele chunk tussen Y hoogtes"));
        sender.sendMessage(ChatUtil.colorize("   §f• safe: §7behoudt geplaatste blokken"));
        sender.sendMessage(ChatUtil.colorize("   §f• oneside: §7stopt per Y-laag bij afwijkend blok/muur"));
        sender.sendMessage(ChatUtil.colorize(" §e/fpixel empty [safe|ignore-water|ignore-water-and-box|glass|sponge] §7- Maak chunk leeg"));
        sender.sendMessage(ChatUtil.colorize("   §f• (leeg): §7maakt hele chunk leeg"));
        sender.sendMessage(ChatUtil.colorize("   §f• safe: §7verwijdert alles boven de speler Y"));
        sender.sendMessage(ChatUtil.colorize("   §f• ignore-water: §7behoudt water"));
        sender.sendMessage(ChatUtil.colorize("   §f• ignore-water-and-box: §7behoudt water en plaatst glazen wand"));
        sender.sendMessage(ChatUtil.colorize("   §f• glass: §7verwijdert alle glas in chunk"));
        sender.sendMessage(ChatUtil.colorize("   §f• sponge: §7verwijdert alle sponzen in chunk"));
        sender.sendMessage(ChatUtil.colorize(" §e/fpixel undo §7- Herstel laatste actie"));
        sender.sendMessage(ChatUtil.colorize(" §e/fpixel rebuildhub §7- Rebuild Hub"));
        sender.sendMessage(ChatUtil.colorize(" §e/fpixel setlobby §7- Set hub spawn point"));
        sender.sendMessage(ChatUtil.colorize(" §e/fpixel import §7- Map & Hub Importer (/fpixel import list|hub|map|scan)"));
        sender.sendMessage(ChatUtil.colorize(" §e/fpixel update §7- Start whole-server update (backup, git, compile, restart)"));
        sender.sendMessage(ChatUtil.colorize(" §e/fpixel backup §7- Create full server backup snapshot"));
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] rawArgs) {
        String cmdLabel = alias.toLowerCase();
        if (cmdLabel.startsWith("/")) cmdLabel = cmdLabel.substring(1);

        String[] args;
        if (cmdLabel.contains("fill")) {
            args = new String[rawArgs.length + 1];
            args[0] = "fill";
            System.arraycopy(rawArgs, 0, args, 1, rawArgs.length);
        } else if (cmdLabel.contains("empty")) {
            args = new String[rawArgs.length + 1];
            args[0] = "empty";
            System.arraycopy(rawArgs, 0, args, 1, rawArgs.length);
        } else {
            args = rawArgs;
        }

        List<String> list = new ArrayList<>();
        if (args.length == 1) {
            list.addAll(Arrays.asList("parkour", "test", "approve", "disable", "fill", "empty", "undo", "import", "update", "backup", "rebuildhub", "setlobby", "buildmaps", "list", "addcoins", "reload"));
        } else if (args[0].equalsIgnoreCase("parkour")) {
            if (args.length == 2) {
                list.addAll(Arrays.asList("create", "build", "setstart", "checkpoint", "setfinish", "clearcheckpoints", "list", "test", "approve", "disable", "enable", "reject", "reload"));
            } else if (args.length == 3) {
                String sub = args[1].toLowerCase();
                if (sub.equals("test") || sub.equals("approve") || sub.equals("disable") || sub.equals("enable") || sub.equals("reject") || sub.equals("build")) {
                    for (net.fpixel.games.parkour.CustomParkourMap m : plugin.getParkourManager().getAllMaps()) {
                        list.add(m.getId());
                    }
                }
            }
        } else if (args[0].equalsIgnoreCase("test") || args[0].equalsIgnoreCase("approve") || args[0].equalsIgnoreCase("disable")) {
            if (args.length == 2) {
                for (net.fpixel.games.parkour.CustomParkourMap m : plugin.getParkourManager().getAllMaps()) {
                    list.add(m.getId());
                }
            }
        } else if (args[0].equalsIgnoreCase("import")) {
            if (args.length == 2) {
                list.addAll(Arrays.asList("list", "hub", "map", "scan"));
            } else if (args.length == 3 && args[1].equalsIgnoreCase("map")) {
                list.addAll(Arrays.asList("BEDWARS", "SKYWARS", "BRIDGE", "SUMO", "TNTRUN", "PARKOUR", "MURDER_MYSTERY", "PROP_HUNT"));
            } else if (args.length == 3 && args[1].equalsIgnoreCase("hub")) {
                if (plugin.getMapImporter() != null) {
                    for (java.io.File f : plugin.getMapImporter().getAvailableZipFiles()) {
                        list.add(f.getName());
                    }
                }
            } else if (args.length == 5 && args[1].equalsIgnoreCase("map")) {
                if (plugin.getMapImporter() != null) {
                    for (java.io.File f : plugin.getMapImporter().getAvailableZipFiles()) {
                        list.add(f.getName());
                    }
                }
            }
        } else if (args[0].equalsIgnoreCase("fill")) {
            if (args.length == 2) {
                list.add("52");
                list.add("~");
            } else if (args.length == 3) {
                list.add("54");
                list.add("55");
                list.add("81");
            } else if (args.length == 4) {
                list.add("stone");
                list.add("deepslate");
                list.add("dirt");
                list.add("glass");
                list.add("all");
                list.add("safe");
                list.add("oneside");
            } else if (args.length == 5) {
                list.add("all");
                list.add("safe");
                list.add("oneside");
            }
        } else if (args[0].equalsIgnoreCase("empty")) {
            if (args.length == 2) {
                list.add("safe");
                list.add("ignore-water");
                list.add("ignore-water-and-box");
                list.add("glass");
                list.add("sponge");
            }
        }

        List<String> filtered = new ArrayList<>();
        String current = args[args.length - 1].toLowerCase();
        for (String s : list) {
            if (s.toLowerCase().startsWith(current)) {
                filtered.add(s);
            }
        }
        return filtered;
    }
}
