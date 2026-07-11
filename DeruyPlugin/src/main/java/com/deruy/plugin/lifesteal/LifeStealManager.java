package com.deruy.plugin.lifesteal;

import com.deruy.plugin.DeruyPlugin;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * LifeSteal 시스템의 중심 매니저.
 * - 하트(최대 체력) 증감
 * - 0하트 밴 처리
 * - 컴벳태그 상태 관리 (다른 매니저/리스너에서 공통으로 참조)
 * - 시스템 전체 on/off 토글 (pending #12)
 */
public class LifeStealManager {

    private static final double HEART_VALUE = 2.0; // 1하트 = 2 체력포인트
    private static final double MIN_HEALTH = 2.0;  // 최소 1하트는 유지 (0이면 밴)

    private final DeruyPlugin plugin;

    // 컴벳태그: uuid -> 태그 만료 시각(epoch millis)
    private final Map<UUID, Long> combatTagged = new HashMap<>();

    // 시스템 전체 활성화 여부 (pending #12)
    private boolean systemEnabled;

    public LifeStealManager(DeruyPlugin plugin) {
        this.plugin = plugin;
        this.systemEnabled = plugin.getConfig().getBoolean("lifesteal.enabled", true);
    }

    // ---------------- 시스템 on/off ----------------

    public boolean isSystemEnabled() {
        return systemEnabled;
    }

    public void setSystemEnabled(boolean enabled) {
        this.systemEnabled = enabled;
        plugin.getConfig().set("lifesteal.enabled", enabled);
        plugin.saveConfig();
    }

    // ---------------- 하트 시스템 ----------------

    public void addHeart(Player player) {
        addHeart(player, 1);
    }

    public void addHeart(Player player, int amount) {
        var attr = player.getAttribute(Attribute.MAX_HEALTH);
        if (attr == null) return;
        double newMax = attr.getBaseValue() + (HEART_VALUE * amount);
        attr.setBaseValue(newMax);
    }

    /**
     * 하트를 제거한다. 0하트(체력 <= MIN_HEALTH 도달) 시 밴 처리.
     */
    public void removeHeart(Player player) {
        removeHeart(player, 1);
    }

    public void removeHeart(Player player, int amount) {
        var attr = player.getAttribute(Attribute.MAX_HEALTH);
        if (attr == null) return;

        double newMax = attr.getBaseValue() - (HEART_VALUE * amount);

        if (newMax <= MIN_HEALTH) {
            attr.setBaseValue(MIN_HEALTH);
            banZeroHeart(player);
            return;
        }

        attr.setBaseValue(newMax);
        if (player.getHealth() > newMax) {
            player.setHealth(newMax);
        }
    }

    private void banZeroHeart(Player player) {
        String kickMessage = plugin.getConfig().getString(
                "lifesteal.zero-heart-ban-message",
                "§c하트를 모두 잃어 서버에서 추방되었습니다."
        );
        player.kickPlayer(kickMessage);

        Bukkit.getBanList(org.bukkit.BanList.Type.NAME).addBan(
                player.getName(), "0 하트 도달", null, "LifeSteal"
        );
    }

    // ---------------- 컴벳태그 ----------------

    public void tagCombat(UUID uuid, long durationMillis) {
        combatTagged.put(uuid, System.currentTimeMillis() + durationMillis);
    }

    public boolean isCombatTagged(UUID uuid) {
        Long expiry = combatTagged.get(uuid);
        if (expiry == null) return false;
        if (System.currentTimeMillis() > expiry) {
            combatTagged.remove(uuid);
            return false;
        }
        return true;
    }

    public void clearCombatTag(UUID uuid) {
        combatTagged.remove(uuid);
    }

    public long getRemainingCombatMillis(UUID uuid) {
        Long expiry = combatTagged.get(uuid);
        if (expiry == null) return 0L;
        return Math.max(0L, expiry - System.currentTimeMillis());
    }
}
