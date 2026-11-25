package ua.tymaa.adminpanel;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Адмін панель (tymaa).
 * /admin_panel give <нік>   - видати доступ до панелі
 * /admin_panel revoke <нік> - забрати доступ
 * /admin_panel              - відкрити панель
 * Кнопки панелі виконують /apanel <дія> від імені гравця.
 */
public class AdminPanel extends JavaPlugin implements Listener, TabExecutor {

    private final Set<UUID> admins = new HashSet<>();
    private final Set<UUID> frozen = new HashSet<>();
    private final Set<UUID> vanished = new HashSet<>();
    private final Set<UUID> god = new HashSet<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        for (String s : getConfig().getStringList("admins")) {
            try { admins.add(UUID.fromString(s)); } catch (IllegalArgumentException ignored) { }
        }
        getServer().getPluginManager().registerEvents(this, this);
        for (String n : new String[]{"admin_panel", "apanel"}) {
            PluginCommand pc = getCommand(n);
            if (pc != null) { pc.setExecutor(this); pc.setTabCompleter(this); }
        }
        installDatapack();
    }

    /** Копіює датапак із діалогами у світ (потрібен один рестарт). */
    private void installDatapack() {
        try {
            if (Bukkit.getWorlds().isEmpty()) return;
            File dir = new File(Bukkit.getWorlds().get(0).getWorldFolder(), "datapacks");
            File f = new File(dir, "adminpanel_dialogs.zip");
            if (f.exists()) return;
            dir.mkdirs();
            try (InputStream in = getResource("adminpanel_dialogs.zip")) {
                if (in == null) return;
                Files.copy(in, f.toPath());
            }
            getLogger().warning("Датапак панелі встановлено. Перезапусти сервер ОДИН раз, щоб з'явилась кнопка в ESC.");
        } catch (IOException e) {
            getLogger().warning("Не вдалось встановити датапак: " + e.getMessage());
        }
    }

    private boolean isAdmin(Player p) {
        return p.isOp() || admins.contains(p.getUniqueId());
    }

    private void saveAdmins() {
        getConfig().set("admins", admins.stream().map(UUID::toString).collect(Collectors.toList()));
        saveConfig();
    }

    private void ok(CommandSender s, String m) { s.sendMessage("§a✔ §f" + m); }
    private void err(CommandSender s, String m) { s.sendMessage("§c✖ §f" + m); }

    private void open(Player p) {
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "dialog show " + p.getName() + " adminpanel:main");
    }

    private void sendPack(Player p) {
        String url = getConfig().getString("resource-pack.url", "");
        String sha = getConfig().getString("resource-pack.sha1", "");
        if (url == null || url.isBlank()) return;
        try {
            byte[] hash = (sha == null || sha.isBlank()) ? null : hex(sha);
            p.setResourcePack(UUID.nameUUIDFromBytes("tymaa-adminpanel".getBytes()), url, hash,
                    Component.text("Адмін панель"), false);
        } catch (Exception e) {
            getLogger().warning("Не вдалось надіслати ресурспак: " + e.getMessage());
        }
    }

    private static byte[] hex(String s) {
        int len = s.length();
        byte[] out = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            out[i / 2] = (byte) Integer.parseInt(s.substring(i, i + 2), 16);
        }
        return out;
    }

    // ------------------------------------------------------------------ команди

    @Override
    public boolean onCommand(CommandSender s, Command c, String label, String[] a) {
        if (c.getName().equalsIgnoreCase("admin_panel")) return manage(s, a);
        return panel(s, a);
    }

    private boolean manage(CommandSender s, String[] a) {
        if (a.length == 0) {
            if (s instanceof Player p && isAdmin(p)) open(p);
            else err(s, "Використання: /admin_panel give|revoke <нік>");
            return true;
        }
        boolean allowed = s instanceof ConsoleCommandSender || s.isOp();
        String sub = a[0].toLowerCase();
        if (sub.equals("open")) {
            if (s instanceof Player p && isAdmin(p)) open(p);
            return true;
        }
        if (!allowed) { err(s, "Керувати доступом можуть лише оператори."); return true; }

        if (sub.equals("list")) {
            List<String> names = new ArrayList<>();
            for (UUID id : admins) {
                OfflinePlayer op = Bukkit.getOfflinePlayer(id);
                names.add(op.getName() == null ? id.toString() : op.getName());
            }
            ok(s, "Доступ мають: " + (names.isEmpty() ? "(нікого, крім операторів)" : String.join(", ", names)));
            return true;
        }
        if ((sub.equals("give") || sub.equals("revoke")) && a.length >= 2) {
            Player online = Bukkit.getPlayerExact(a[1]);
            OfflinePlayer target = online != null ? online : Bukkit.getOfflinePlayerIfCached(a[1]);
            if (target == null) { err(s, "Гравця " + a[1] + " не знайдено (він має хоч раз зайти на сервер)."); return true; }
            if (sub.equals("give")) {
                admins.add(target.getUniqueId());
                saveAdmins();
                ok(s, "Доступ до адмін панелі видано: " + target.getName());
                if (online != null) {
                    sendPack(online);
                    ok(online, "Тобі видано доступ до адмін панелі. Натисни ESC → Адмін панель.");
                }
            } else {
                admins.remove(target.getUniqueId());
                saveAdmins();
                frozen.remove(target.getUniqueId());
                ok(s, "Доступ забрано: " + target.getName());
                if (online != null) err(online, "Твій доступ до адмін панелі забрано.");
            }
            return true;
        }
        err(s, "Використання: /admin_panel give|revoke <нік> | list");
        return true;
    }

    private double num(String s, double def) {
        try { return Double.parseDouble(s.replace(',', '.')); } catch (Exception e) { return def; }
    }

    private void toggle(Player p, GameRule<Boolean> rule, String name) {
        World w = p.getWorld();
        boolean n = !Boolean.TRUE.equals(w.getGameRuleValue(rule));
        w.setGameRule(rule, n);
        ok(p, name + ": " + (n ? "УВІМК" : "ВИМК"));
    }

    private void heal(Player t) {
        var attr = t.getAttribute(Attribute.MAX_HEALTH);
        t.setHealth(attr == null ? 20.0 : attr.getValue());
        t.setFoodLevel(20);
        t.setSaturation(20f);
        t.setFireTicks(0);
    }

    private boolean panel(CommandSender s, String[] a) {
        if (!(s instanceof Player p)) { err(s, "Тільки для гравців."); return true; }
        if (!isAdmin(p)) { err(p, "У тебе немає доступу до адмін панелі."); return true; }
        if (a.length == 0) { open(p); return true; }

        String act = a[0].toLowerCase();
        String arg = a.length > 1 ? a[1] : "";
        String rest = a.length > 1 ? String.join(" ", Arrays.copyOfRange(a, 1, a.length)) : "";
        World w = p.getWorld();

        switch (act) {
            // ---- я
            case "gm_creative" -> { p.setGameMode(GameMode.CREATIVE); ok(p, "Креатив"); }
            case "gm_survival" -> { p.setGameMode(GameMode.SURVIVAL); ok(p, "Виживання"); }
            case "gm_adventure" -> { p.setGameMode(GameMode.ADVENTURE); ok(p, "Пригода"); }
            case "gm_spectator" -> { p.setGameMode(GameMode.SPECTATOR); ok(p, "Спостерігач"); }
            case "fly" -> {
                boolean n = !p.getAllowFlight();
                p.setAllowFlight(n);
                if (!n) p.setFlying(false);
                ok(p, "Політ: " + (n ? "УВІМК" : "ВИМК"));
            }
            case "god" -> {
                if (god.remove(p.getUniqueId())) ok(p, "Безсмертя: ВИМК");
                else { god.add(p.getUniqueId()); ok(p, "Безсмертя: УВІМК"); }
            }
            case "vanish" -> {
                if (vanished.remove(p.getUniqueId())) {
                    for (Player o : Bukkit.getOnlinePlayers()) o.showPlayer(this, p);
                    ok(p, "Невидимка: ВИМК");
                } else {
                    vanished.add(p.getUniqueId());
                    for (Player o : Bukkit.getOnlinePlayers()) if (!isAdmin(o)) o.hidePlayer(this, p);
                    ok(p, "Невидимка: УВІМК");
                }
            }
            case "nv" -> {
                if (p.hasPotionEffect(PotionEffectType.NIGHT_VISION)) {
                    p.removePotionEffect(PotionEffectType.NIGHT_VISION);
                    ok(p, "Нічне бачення: ВИМК");
                } else {
                    p.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION,
                            PotionEffect.INFINITE_DURATION, 0, true, false));
                    ok(p, "Нічне бачення: УВІМК");
                }
            }
            case "speed" -> {
                double v = Math.max(1, Math.min(10, num(arg, 2)));
                p.setWalkSpeed((float) (v / 10.0));
                p.setFlySpeed((float) (v / 10.0));
                ok(p, "Швидкість: " + (int) v + " (стандарт 2)");
            }
            case "heal" -> { heal(p); ok(p, "Вилікувано й нагодовано"); }
            case "feed" -> { p.setFoodLevel(20); p.setSaturation(20f); ok(p, "Нагодовано"); }
            case "extinguish" -> { p.setFireTicks(0); ok(p, "Вогонь погашено"); }
            case "repair" -> {
                ItemStack it = p.getInventory().getItemInMainHand();
                ItemMeta m = it.getItemMeta();
                if (m instanceof Damageable d) { d.setDamage(0); it.setItemMeta(m); ok(p, "Предмет відремонтовано"); }
                else err(p, "Цей предмет не ламається");
            }
            case "top" -> {
                Location l = p.getLocation();
                l.setY(w.getHighestBlockYAt(l) + 1);
                p.teleport(l);
                ok(p, "Піднято на поверхню");
            }
            case "clear_inv" -> { p.getInventory().clear(); ok(p, "Інвентар очищено"); }

            // ---- час і погода
            case "time_day" -> { w.setTime(1000); ok(p, "День"); }
            case "time_noon" -> { w.setTime(6000); ok(p, "Полудень"); }
            case "time_night" -> { w.setTime(13000); ok(p, "Ніч"); }
            case "time_midnight" -> { w.setTime(18000); ok(p, "Північ"); }
            case "weather_clear" -> { w.setStorm(false); w.setThundering(false); ok(p, "Ясно"); }
            case "weather_rain" -> { w.setStorm(true); w.setThundering(false); ok(p, "Дощ"); }
            case "weather_thunder" -> { w.setStorm(true); w.setThundering(true); ok(p, "Гроза"); }
            case "freeze_time" -> toggle(p, GameRule.DO_DAYLIGHT_CYCLE, "Зміна дня і ночі");

            // ---- сервер
            case "broadcast" -> {
                if (rest.isBlank()) err(p, "Введи текст");
                else Bukkit.broadcast(Component.text("§c[Адмін] §f" + rest));
            }
            case "keepinv" -> toggle(p, GameRule.KEEP_INVENTORY, "Збереження інвентарю після смерті");
            case "mobgrief" -> toggle(p, GameRule.MOB_GRIEFING, "Моби ламають блоки");
            case "pvp" -> { w.setPVP(!w.getPVP()); ok(p, "PvP: " + (w.getPVP() ? "УВІМК" : "ВИМК")); }
            case "difficulty" -> {
                try {
                    w.setDifficulty(org.bukkit.Difficulty.valueOf(arg.toUpperCase()));
                    ok(p, "Складність: " + arg);
                } catch (IllegalArgumentException e) { err(p, "Невідома складність"); }
            }
            case "save" -> {
                Bukkit.savePlayers();
                for (World x : Bukkit.getWorlds()) x.save();
                ok(p, "Світ і гравців збережено");
            }
            case "tps" -> {
                double[] t = Bukkit.getTPS();
                ok(p, String.format("TPS: %.1f / %.1f / %.1f", t[0], t[1], t[2]));
            }
            case "online" -> ok(p, "Онлайн (" + Bukkit.getOnlinePlayers().size() + "): "
                    + Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.joining(", ")));

            // ---- гравці
            case "tp", "tphere", "heal_p", "feed_p", "kick", "freeze_p", "invsee", "echest" -> {
                Player t = Bukkit.getPlayerExact(arg);
                if (t == null) { err(p, "Гравця '" + arg + "' немає онлайн"); return true; }
                switch (act) {
                    case "tp" -> { p.teleport(t); ok(p, "Телепорт до " + t.getName()); }
                    case "tphere" -> { t.teleport(p); ok(p, t.getName() + " телепортовано до тебе"); }
                    case "heal_p" -> { heal(t); ok(p, t.getName() + " вилікувано"); }
                    case "feed_p" -> { t.setFoodLevel(20); t.setSaturation(20f); ok(p, t.getName() + " нагодовано"); }
                    case "kick" -> { t.kick(Component.text("Вас вигнав адміністратор")); ok(p, t.getName() + " кікнутий"); }
                    case "freeze_p" -> {
                        if (frozen.remove(t.getUniqueId())) ok(p, t.getName() + " розморожено");
                        else { frozen.add(t.getUniqueId()); ok(p, t.getName() + " заморожено"); }
                    }
                    case "invsee" -> p.openInventory(t.getInventory());
                    default -> p.openInventory(t.getEnderChest());
                }
            }

            // ---- моби і світ
            case "kill_hostile" -> {
                int r = (int) Math.max(1, Math.min(300, num(arg, 64)));
                int n = 0;
                for (Entity e : p.getNearbyEntities(r, r, r)) if (e instanceof Monster) { e.remove(); n++; }
                ok(p, "Вбито ворожих мобів: " + n);
            }
            case "clear_drops" -> {
                int n = 0;
                for (Item i : w.getEntitiesByClass(Item.class)) { i.remove(); n++; }
                ok(p, "Прибрано предметів на землі: " + n);
            }
            case "border" -> {
                double size = Math.max(10, Math.min(60000000, num(arg, 2000)));
                w.getWorldBorder().setSize(size);
                ok(p, "Межа світу: " + (int) size);
            }
            case "setspawn" -> { w.setSpawnLocation(p.getLocation()); ok(p, "Точку спавну світу встановлено тут"); }
            case "spawn" -> { p.teleport(w.getSpawnLocation()); ok(p, "На спавн"); }
            case "lightning" -> { w.strikeLightningEffect(p.getLocation()); ok(p, "Блискавка (ефект)"); }

            default -> err(p, "Невідома дія: " + act);
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender s, Command c, String label, String[] a) {
        if (c.getName().equalsIgnoreCase("admin_panel")) {
            if (a.length == 1) return List.of("give", "revoke", "list", "open");
            if (a.length == 2) return Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList());
        }
        return List.of();
    }

    // ------------------------------------------------------------------ події

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        if (!isAdmin(p)) {
            for (UUID id : vanished) {
                Player v = Bukkit.getPlayer(id);
                if (v != null) p.hidePlayer(this, v);
            }
        }
        if (getConfig().getBoolean("resource-pack.send-to-all", false) || isAdmin(p)) {
            Bukkit.getScheduler().runTaskLater(this, () -> sendPack(p), 40L);
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        if (frozen.contains(e.getPlayer().getUniqueId()) && e.hasChangedPosition()) {
            e.setTo(e.getFrom());
        }
    }

    @EventHandler
    public void onDamage(EntityDamageEvent e) {
        if (e.getEntity() instanceof Player p && god.contains(p.getUniqueId())) e.setCancelled(true);
    }
}
