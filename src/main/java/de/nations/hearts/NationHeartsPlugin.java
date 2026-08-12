package de.nations.hearts;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class NationHeartsPlugin extends JavaPlugin implements Listener, CommandExecutor, TabCompleter {

    // Exact glyphs from the supplied resource pack's assets/minecraft/font/default.json:
    // U+E01B -> risiko:font/active_heart.png
    // U+E01C -> risiko:font/inactive_heart.png
    private static final char ACTIVE_HEART = '\uE01B';
    private static final char INACTIVE_HEART = '\uE01C';

    private final Map<UUID, Integer> lives = new ConcurrentHashMap<>();
    private File dataFile;
    private org.bukkit.configuration.file.YamlConfiguration dataConfig;
    private BukkitTask displayTask;

    private int startingLives;
    private int maxLives;
    private boolean showActionbar;
    private long refreshTicks;
    private String kickMessage;
    private String loginMessage;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadSettings();
        loadData();

        Bukkit.getPluginManager().registerEvents(this, this);
        if (getCommand("nation") != null) {
            getCommand("nation").setExecutor(this);
            getCommand("nation").setTabCompleter(this);
        }

        if (showActionbar) {
            displayTask = Bukkit.getScheduler().runTaskTimer(this, this::updateAllDisplays, 0L, refreshTicks);
        }

        getLogger().info("NationHearts aktiviert. Blaue Leben: " + startingLives + ", Maximum: " + maxLives);
    }

    @Override
    public void onDisable() {
        if (displayTask != null) {
            displayTask.cancel();
        }
        saveData();
    }

    private void loadSettings() {
        startingLives = Math.max(1, Math.min(3, getConfig().getInt("starting-lives", 3)));
        maxLives = Math.max(startingLives, Math.min(3, getConfig().getInt("max-lives", 3)));
        showActionbar = getConfig().getBoolean("show-actionbar", true);
        refreshTicks = Math.max(1L, getConfig().getLong("actionbar-refresh-ticks", 10L));
        kickMessage = getConfig().getString("kick-message", "Du hast keine Leben mehr!");
        loginMessage = getConfig().getString("login-message", "Du hast keine Leben mehr!");
    }

    private void loadData() {
        dataFile = new File(getDataFolder(), "data.yml");
        if (!dataFile.exists()) {
            return;
        }

        dataConfig = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(dataFile);
        var section = dataConfig.getConfigurationSection("players");
        if (section == null) {
            return;
        }

        for (String key : section.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                int value = section.getInt(key, startingLives);
                lives.put(uuid, Math.max(0, Math.min(maxLives, value)));
            } catch (IllegalArgumentException ignored) {
                getLogger().warning("Ungültige UUID in data.yml: " + key);
            }
        }
    }

    private synchronized void saveData() {
        if (!getDataFolder().exists() && !getDataFolder().mkdirs()) {
            getLogger().warning("Konnte Plugin-Ordner nicht erstellen.");
        }

        if (dataConfig == null) {
            dataConfig = new org.bukkit.configuration.file.YamlConfiguration();
        }

        dataConfig.set("players", null);
        for (Map.Entry<UUID, Integer> entry : lives.entrySet()) {
            dataConfig.set("players." + entry.getKey(), entry.getValue());
        }

        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            getLogger().severe("Konnte data.yml nicht speichern: " + e.getMessage());
        }
    }

    private int getLives(UUID uuid) {
        return lives.getOrDefault(uuid, startingLives);
    }

    private boolean hasStoredLives(UUID uuid) {
        return lives.containsKey(uuid);
    }

    private void setLives(UUID uuid, int amount) {
        lives.put(uuid, Math.max(0, Math.min(maxLives, amount)));
        saveData();
        Player player = Bukkit.getPlayer(uuid);
        if (player != null && player.isOnline()) {
            updateDisplay(player);
        }
    }

    private String heartDisplay(int amount) {
        StringBuilder builder = new StringBuilder(3);
        for (int i = 1; i <= maxLives; i++) {
            builder.append(i <= amount ? ACTIVE_HEART : INACTIVE_HEART);
        }
        return builder.toString();
    }

    private Component heartComponent(int amount) {
        // Do not modify player health: vanilla's 10 red hearts stay completely unchanged.
        return Component.text(heartDisplay(amount));
    }

    private void updateDisplay(Player player) {
        if (!showActionbar) {
            return;
        }
        player.sendActionBar(heartComponent(getLives(player.getUniqueId())));
    }

    private void updateAllDisplays() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            updateDisplay(player);
        }
    }

    @EventHandler
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {
        int playerLives = getLives(event.getUniqueId());
        if (hasStoredLives(event.getUniqueId()) && playerLives <= 0) {
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, Component.text(loginMessage, NamedTextColor.RED));
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        // First join: exactly 3 blue lives. Existing values are never overwritten.
        if (!hasStoredLives(uuid)) {
            lives.put(uuid, startingLives);
            saveData();
        }

        updateDisplay(player);
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getPlayer();
        Player killer = victim.getKiller();

        // A blue life is lost only when another player gets the kill.
        if (killer == null || killer.getUniqueId().equals(victim.getUniqueId())) {
            return;
        }

        int newLives = Math.max(0, getLives(victim.getUniqueId()) - 1);
        lives.put(victim.getUniqueId(), newLives);
        saveData();

        if (newLives <= 0) {
            event.deathMessage(null);
            Bukkit.getScheduler().runTask(this, () -> {
                if (victim.isOnline()) {
                    victim.kick(Component.text(kickMessage, NamedTextColor.RED));
                }
            });
        } else {
            updateDisplay(victim);
        }
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            sender.sendMessage(Component.text("/nation hearts set <Spieler> <1|2|3>", NamedTextColor.YELLOW));
            return true;
        }

        if (!args[0].equalsIgnoreCase("hearts")) {
            sender.sendMessage(Component.text("Nutze: /nation hearts set <Spieler> <1|2|3>", NamedTextColor.RED));
            return true;
        }

        if (!sender.hasPermission("nationhearts.admin")) {
            sender.sendMessage(Component.text("Keine Berechtigung.", NamedTextColor.RED));
            return true;
        }

        if (args.length != 4 || !args[1].equalsIgnoreCase("set")) {
            sender.sendMessage(Component.text("Nutze: /nation hearts set <Spieler> <1|2|3>", NamedTextColor.YELLOW));
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[2]);
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            sender.sendMessage(Component.text("Spieler nicht gefunden: " + args[2], NamedTextColor.RED));
            return true;
        }

        int amount;
        try {
            amount = Integer.parseInt(args[3]);
        } catch (ArrayIndexOutOfBoundsException | NumberFormatException ignored) {
            sender.sendMessage(Component.text("Die Anzahl muss 1, 2 oder 3 sein.", NamedTextColor.RED));
            return true;
        }

        if (amount < 1 || amount > maxLives) {
            sender.sendMessage(Component.text("Die Anzahl muss 1, 2 oder 3 sein.", NamedTextColor.RED));
            return true;
        }

        setLives(target.getUniqueId(), amount);
        sender.sendMessage(Component.text("Herzen von " + target.getName() + " auf " + amount + " gesetzt.", NamedTextColor.GREEN));
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return partial(args[0], List.of("hearts"));
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("hearts")) {
            return partial(args[1], List.of("set"));
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("hearts") && args[1].equalsIgnoreCase("set")) {
            List<String> names = new ArrayList<>();
            for (Player player : Bukkit.getOnlinePlayers()) {
                names.add(player.getName());
            }
            return partial(args[2], names);
        }
        return Collections.emptyList();
    }

    private List<String> partial(String input, List<String> values) {
        String lower = input.toLowerCase();
        return values.stream().filter(value -> value.toLowerCase().startsWith(lower)).sorted().toList();
    }
}
