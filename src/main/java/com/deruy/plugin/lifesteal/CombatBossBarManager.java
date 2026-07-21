package com.deruy.plugin.lifesteal;

import com.deruy.plugin.DeruyPlugin;
import com.deruy.plugin.events.koth.KothZone;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 컴벳태그 상태인 플레이어에게 보스바를 표시한다.
 * - 평소: 전투 상태 + 컴벳태그 남은 시간
 * - 활성화된 KOTH 구역 안에 있을 때: 누가 점령중인지 + 점령까지 남은 시간으로 교체 표시
 */
public class CombatBossBarManager {

    private final DeruyPlugin plugin;
    private final Map<UUID, BossBar> activeBars = new HashMap<>();
    private BukkitTask task;

    public CombatBossBarManager(DeruyPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        if (task != null) return;
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 10L); // 0.5초마다 갱신
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        for (BossBar bar : activeBars.values()) {
            bar.removeAll();
        }
        activeBars.clear();
    }

    private void tick() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID uuid = player.getUniqueId();
            boolean combatTagged = plugin.getLifeStealManager().isCombatTagged(uuid);

            if (!combatTagged) {
                BossBar existing = activeBars.remove(uuid);
                if (existing != null) existing.removeAll();
                continue;
            }

            KothZone activeZone = findActiveZoneContaining(player);

            if (activeZone != null) {
                updateKothBar(player, activeZone);
            } else {
                updateCombatBar(player);
            }
        }
    }

    private KothZone findActiveZoneContaining(Player player) {
        var koth = plugin.getKothManager();
        for (String id : koth.getActiveZoneIds()) {
            KothZone zone = koth.getZones().get(id);
            if (zone == null || !zone.isComplete()) continue;
            if (zone.contains(player.getLocation())) {
                return zone;
            }
        }
        return null;
    }

    private void updateCombatBar(Player player) {
        long remainingMillis = plugin.getLifeStealManager().getRemainingCombatMillis(player.getUniqueId());
        long totalMillis = plugin.getConfig().getLong("combat-tag.duration-seconds", 15) * 1000L;
        double progress = totalMillis > 0
                ? Math.max(0.0, Math.min(1.0, remainingMillis / (double) totalMillis))
                : 0.0;

        String title = "§c전투 중 §7(" + (int) Math.ceil(remainingMillis / 1000.0) + "초)";

        BossBar bar = activeBars.computeIfAbsent(player.getUniqueId(),
                id -> Bukkit.createBossBar(title, BarColor.RED, BarStyle.SOLID));

        bar.setColor(BarColor.RED);
        bar.setTitle(title);
        bar.setProgress(progress);
        if (!bar.getPlayers().contains(player)) {
            bar.addPlayer(player);
        }
    }

    private void updateKothBar(Player player, KothZone zone) {
        int requiredSeconds = plugin.getKothManager().getRequiredSeconds();
        int heldSeconds = zone.getHeldTicks() / 20;
        int remainingSeconds = Math.max(0, requiredSeconds - heldSeconds);
        double progress = requiredSeconds > 0
                ? Math.max(0.0, Math.min(1.0, heldSeconds / (double) requiredSeconds))
                : 0.0;

        String holderName = resolveHolderName(zone);
        String title = "§6[KOTH-" + zone.getId() + "] §e" + holderName + " §7점령중 §e(" + remainingSeconds + "초 남음)";

        BossBar bar = activeBars.computeIfAbsent(player.getUniqueId(),
                id -> Bukkit.createBossBar(title, BarColor.YELLOW, BarStyle.SOLID));

        bar.setColor(BarColor.YELLOW);
        bar.setTitle(title);
        bar.setProgress(progress);
        if (!bar.getPlayers().contains(player)) {
            bar.addPlayer(player);
        }
    }

    private String resolveHolderName(KothZone zone) {
        UUID holder = zone.getCurrentHolder();
        if (holder == null) return "§7(점령자 없음)";
        Player holderPlayer = Bukkit.getPlayer(holder);
        return holderPlayer != null ? holderPlayer.getName() : "알 수 없음";
    }
}
