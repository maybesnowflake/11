package com.deruy.plugin.events.koth;

import com.deruy.plugin.DeruyPlugin;
import com.deruy.plugin.events.GameEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * KOTH(King of the Hill) 거점점령 이벤트. 여러 구역을 등록해두고,
 * 라운드마다 원하는 구역만 골라서 시작할 수 있다.
 * 예: 1번만, 3번만, 2·3번만 이런 식으로 선택적으로 활성화 가능.
 *
 * - enabled: KOTH 기능 자체의 on/off (마스터 스위치)
 * - start()/stop(): 실제 라운드의 시작/종료
 *   인자 없이 start() 호출 시 등록된 모든 구역이 활성화됨.
 *   특정 id만 넘기면 그 구역들만 활성화되고 나머지는 대기 상태로 유지됨.
 */
public class KothManager implements GameEvent {

    private final DeruyPlugin plugin;

    private boolean enabled;
    private final Map<String, KothZone> zones = new LinkedHashMap<>();
    private final Set<String> activeZoneIds = new LinkedHashSet<>();

    private int requiredTicks = 20 * 60; // 기본 60초
    private BukkitTask task;

    public KothManager(DeruyPlugin plugin) {
        this.plugin = plugin;
        this.enabled = plugin.getConfig().getBoolean("koth.enabled", false);
        this.requiredTicks = plugin.getConfig().getInt("koth.required-seconds", 60) * 20;
    }

    @Override
    public String getName() {
        return "koth";
    }

    // ---------------- on/off (기능 자체 활성화 여부) ----------------

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        plugin.getConfig().set("koth.enabled", enabled);
        plugin.saveConfig();
        if (!enabled && isRunning()) {
            stop();
        }
    }

    // ---------------- 구역 관리 (다중 구역) ----------------

    public KothZone getOrCreateZone(String id) {
        return zones.computeIfAbsent(id, KothZone::new);
    }

    public boolean removeZone(String id) {
        activeZoneIds.remove(id);
        return zones.remove(id) != null;
    }

    public Map<String, KothZone> getZones() {
        return zones;
    }

    public Set<String> getActiveZoneIds() {
        return activeZoneIds;
    }

    public void setRequiredSeconds(int seconds) {
        this.requiredTicks = seconds * 20;
        plugin.getConfig().set("koth.required-seconds", seconds);
        plugin.saveConfig();
    }

    public int getRequiredSeconds() {
        return requiredTicks / 20;
    }

    // ---------------- GameEvent (전체 구역 대상) ----------------

    @Override
    public void start() {
        start(zones.keySet().toArray(new String[0]));
    }

    /**
     * 지정한 id의 구역만 골라서 시작한다. 인자를 비우면 전체 구역이 대상이 된다.
     * 잘못된 id나 미완성 구역은 건너뛴다.
     */
    public void start(String... zoneIds) {
        if (!enabled) {
            plugin.getLogger().warning("KOTH가 비활성화되어 있어 시작할 수 없습니다. /koth on 먼저 실행하세요.");
            return;
        }

        String[] targets = (zoneIds == null || zoneIds.length == 0)
                ? zones.keySet().toArray(new String[0])
                : zoneIds;

        Set<String> toActivate = new LinkedHashSet<>();
        for (String id : targets) {
            KothZone zone = zones.get(id);
            if (zone == null) {
                plugin.getLogger().warning("존재하지 않는 KOTH 구역 id: " + id + " (건너뜀)");
                continue;
            }
            if (!zone.isComplete()) {
                plugin.getLogger().warning("구역 '" + id + "'은 좌표 설정이 완료되지 않아 건너뜁니다.");
                continue;
            }
            toActivate.add(id);
        }

        if (toActivate.isEmpty()) {
            plugin.getLogger().warning("시작할 수 있는 KOTH 구역이 없습니다.");
            return;
        }

        boolean wasRunning = isRunning();
        for (String id : toActivate) {
            zones.get(id).resetProgress();
            activeZoneIds.add(id);
        }

        Bukkit.broadcastMessage("§6§lKOTH §e거점점령 이벤트 " +
                (toActivate.size() == zones.size() ? "(전체 " + zones.size() + "구역)" : "(구역 " + String.join(", ", toActivate) + ")")
                + " 시작되었습니다! 지정 구역을 점령하세요.");

        if (!wasRunning) {
            task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 0L, 20L);
        }
    }

    @Override
    public void stop() {
        stop((String[]) null);
    }

    /**
     * 지정한 id의 구역만 종료한다. 인자를 비우면 전체 라운드를 종료한다.
     */
    public void stop(String... zoneIds) {
        if (!isRunning()) return;

        if (zoneIds == null || zoneIds.length == 0) {
            activeZoneIds.clear();
            zones.values().forEach(KothZone::resetProgress);
            if (task != null) {
                task.cancel();
                task = null;
            }
            Bukkit.broadcastMessage("§6§lKOTH §e이벤트가 종료되었습니다.");
            return;
        }

        for (String id : zoneIds) {
            if (activeZoneIds.remove(id)) {
                KothZone zone = zones.get(id);
                if (zone != null) zone.resetProgress();
                Bukkit.broadcastMessage("§6[KOTH-" + id + "] §e구역이 종료되었습니다.");
            }
        }

        if (activeZoneIds.isEmpty() && task != null) {
            task.cancel();
            task = null;
        }
    }

    @Override
    public boolean isRunning() {
        return !activeZoneIds.isEmpty();
    }

    private void tick() {
        for (String id : new java.util.ArrayList<>(activeZoneIds)) {
            KothZone zone = zones.get(id);
            if (zone == null || !zone.isComplete()) continue;
            tickZone(zone);
        }
    }

    private void tickZone(KothZone zone) {
        Player playerInside = findPlayerInZone(zone);

        if (playerInside == null) {
            if (zone.getCurrentHolder() != null) {
                zone.resetProgress();
            }
            return;
        }

        if (!playerInside.getUniqueId().equals(zone.getCurrentHolder())) {
            zone.setCurrentHolder(playerInside.getUniqueId());
            zone.setHeldTicks(0);
            Bukkit.broadcastMessage("§6[KOTH-" + zone.getId() + "] §e" + playerInside.getName() + "님이 점령을 시작했습니다!");
        } else {
            zone.setHeldTicks(zone.getHeldTicks() + 20);
        }

        int remaining = (requiredTicks - zone.getHeldTicks()) / 20;
        if (remaining > 0 && remaining % 10 == 0) {
            playerInside.sendMessage("§6[KOTH-" + zone.getId() + "] §e점령까지 " + remaining + "초 남았습니다.");
        }

        if (zone.getHeldTicks() >= requiredTicks) {
            Bukkit.broadcastMessage("§6§l[KOTH-" + zone.getId() + "] §a" + playerInside.getName() + "님이 거점을 점령했습니다! 승리!");
            // TODO: 여기에 보상 지급 로직 연결 (config 커맨드 실행 등)
            zone.resetProgress();
            activeZoneIds.remove(zone.getId()); // 점령된 구역은 자동으로 비활성화(off)
            Bukkit.broadcastMessage("§7[KOTH-" + zone.getId() + "] §7이 구역은 비활성화되었습니다. (다시 열려면 /koth start " + zone.getId() + ")");

            if (activeZoneIds.isEmpty() && task != null) {
                task.cancel();
                task = null;
            }
        }
    }

    private Player findPlayerInZone(KothZone zone) {
        Player fallback = null;
        for (Player p : zone.getCorner1().getWorld().getPlayers()) {
            if (zone.contains(p.getLocation())) {
                if (p.getUniqueId().equals(zone.getCurrentHolder())) {
                    return p;
                }
                if (fallback == null) fallback = p;
            }
        }
        return fallback;
    }
}
