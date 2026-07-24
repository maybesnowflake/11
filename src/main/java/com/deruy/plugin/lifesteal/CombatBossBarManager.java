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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 두 종류의 보스바를 관리한다.
 * - 개인 전투 보스바: 컴벳태그 걸린 사람 본인에게만, "전투 중" + 남은시간
 * - KOTH 점령 보스바: 활성 구역에서 누군가 점령 중이면 "서버 전체 플레이어"에게 표시.
 *   (컴벳태그 여부, 그 구역 안에 있는지 여부와 무관하게 모두에게 보임)
 */
public class CombatBossBarManager {

    private final DeruyPlugin plugin;
    private final Map<UUID, BossBar> combatBars = new HashMap<>();
    private final Map<String, BossBar> kothBars = new HashMap<>(); // zoneId -> 전체공개 보스바
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
        for (BossBar bar : combatBars.values()) bar.removeAll();
        combatBars.clear();
        for (BossBar bar : kothBars.values()) bar.removeAll();
        kothBars.clear();
    }

    private void tick() {
        updateKothBars();
        updateCombatBars();
    }

    // ---------------- KOTH 점령 보스바 (전체공개) ----------------

    private void updateKothBars() {
        var koth = plugin.getKothManager();
        Set<String> stillActive = new HashSet<>();

        for (String zoneId : koth.getActiveZoneIds()) {
            KothZone zone = koth.getZones().get(zoneId);
            if (zone == null || !zone.isComplete()) continue;
            if (zone.getCurrentHolder() == null) continue; // 지금 아무도 점령중이 아니면 표시 안 함

            stillActive.add(zoneId);

            int requiredSeconds = koth.getRequiredSeconds();
            int heldSeconds = zone.getHeldTicks() / 20;
            int remainingSeconds = Math.max(0, requiredSeconds - heldSeconds);
            double progress = requiredSeconds > 0
                    ? Math.max(0.0, Math.min(1.0, heldSeconds / (double) requiredSeconds))
                    : 0.0;

            String holderName = resolveHolderName(zone);
            String title = "§6[KOTH-" + zoneId + "] §e" + holderName + " §7점령중 §e("
                    + remainingSeconds + "초 남음 / 총 " + requiredSeconds + "초)";

            BossBar bar = kothBars.computeIfAbsent(zoneId,
                    id -> Bukkit.createBossBar(title, BarColor.YELLOW, BarStyle.SOLID));

            bar.setTitle(title);
            bar.setProgress(progress);

            for (Player player : Bukkit.getOnlinePlayers()) {
                if (!bar.getPlayers().contains(player)) {
                    bar.addPlayer(player);
                }
            }
        }

        // 더 이상 점령중이 아닌 구역의 보스바는 제거
        kothBars.entrySet().removeIf(entry -> {
            if (stillActive.contains(entry.getKey())) return false;
            entry.getValue().removeAll();
            return true;
        });
    }

    private String resolveHolderName(KothZone zone) {
        UUID holder = zone.getCurrentHolder();
        if (holder == null) return "§7(점령자 없음)";
        Player holderPlayer = Bukkit.getPlayer(holder);
        return holderPlayer != null ? holderPlayer.getName() : "알 수 없음";
    }

    // ---------------- 개인 전투 보스바 (컴벳태그 걸린 본인에게만) ----------------

    private void updateCombatBars() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID uuid = player.getUniqueId();
            boolean combatTagged = plugin.getLifeStealManager().isCombatTagged(uuid);

            if (!combatTagged) {
                BossBar existing = combatBars.remove(uuid);
                if (existing != null) existing.removeAll();
                continue;
            }

            long remainingMillis = plugin.getLifeStealManager().getRemainingCombatMillis(uuid);
            long totalMillis = plugin.getConfig().getLong("combat-tag.duration-seconds", 15) * 1000L;
            double progress = totalMillis > 0
                    ? Math.max(0.0, Math.min(1.0, remainingMillis / (double) totalMillis))
                    : 0.0;

            String title = "§c전투 중 §7(" + (int) Math.ceil(remainingMillis / 1000.0) + "초)";

            BossBar bar = combatBars.computeIfAbsent(uuid,
                    id -> Bukkit.createBossBar(title, BarColor.RED, BarStyle.SOLID));

            bar.setColor(BarColor.RED);
            bar.setTitle(title);
            bar.setProgress(progress);
            if (!bar.getPlayers().contains(player)) {
                bar.addPlayer(player);
            }
        }
    }
}
