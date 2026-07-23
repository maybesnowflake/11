package com.deruy.plugin.lifesteal.listeners;

import com.deruy.plugin.DeruyPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.util.List;

/**
 * 하트 시스템 핵심: 킬 시 +1하트, 사망 시 -1하트.
 * PVP 데미지 발생 시 컴벳태그 부여 (다른 리스너들이 공통으로 참조).
 *
 * config의 lifesteal.double-loss-pairs 에 등록된 (victim-role, killer-role) 조합이면
 * 해당 multiplier만큼 하트 손실이 배가된다. (예: LE가 FN에게 죽으면 2배)
 */
public class DeathListener implements Listener {

    private final DeruyPlugin plugin;

    public DeathListener(DeruyPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!plugin.getLifeStealManager().isSystemEnabled()) return;

        if (event.getEntity() instanceof Player victim && event.getDamager() instanceof Player attacker) {
            long durationMillis = plugin.getConfig().getLong("combat-tag.duration-seconds", 15) * 1000L;
            plugin.getLifeStealManager().tagCombat(victim.getUniqueId(), durationMillis);
            plugin.getLifeStealManager().tagCombat(attacker.getUniqueId(), durationMillis);
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        if (!plugin.getLifeStealManager().isSystemEnabled()) return;

        Player victim = event.getEntity();
        Player killer = victim.getKiller();

        boolean naturalDeathRemovesHeart = plugin.getConfig()
                .getBoolean("lifesteal.natural-death-removes-heart", false);

        boolean heartWasRemoved = killer != null || naturalDeathRemovesHeart;
        if (heartWasRemoved) {
            int amount = killer != null ? resolveLossAmount(victim, killer) : 1;
            plugin.getLifeStealManager().removeHeart(victim, amount);
        }

        if (killer != null) {
            plugin.getBountyManager().onKill(killer, victim);

            if (!plugin.getLifeStealManager().addHeart(killer)) {
                dropHeartItem(victim);
            }
        } else if (heartWasRemoved) {
            dropHeartItem(victim);
        }
    }

    /**
     * config의 lifesteal.double-loss-pairs 에서 (victim의 역할, killer의 역할) 조합과
     * 일치하는 항목을 찾아 그 배수를 적용한 하트 손실 개수를 반환. 없으면 기본 1.
     */
    private int resolveLossAmount(Player victim, Player killer) {
        List<?> rawList = plugin.getConfig().getList("lifesteal.double-loss-pairs");
        if (rawList == null || rawList.isEmpty()) return 1;

        String victimRole = plugin.getRoleManager().resolveConfiguredRole(victim);
        String killerRole = plugin.getRoleManager().resolveConfiguredRole(killer);
        if (victimRole == null || killerRole == null) return 1;

        for (Object raw : rawList) {
            if (!(raw instanceof java.util.Map<?, ?> rawMap)) continue;

            Object vRole = rawMap.get("victim-role");
            Object kRole = rawMap.get("killer-role");
            if (vRole == null || kRole == null) continue;

            if (victimRole.equalsIgnoreCase(String.valueOf(vRole))
                    && killerRole.equalsIgnoreCase(String.valueOf(kRole))) {
                Object multiplierRaw = rawMap.get("multiplier");
                double multiplier = multiplierRaw instanceof Number n ? n.doubleValue() : 2.0;
                return Math.max(1, (int) Math.round(multiplier));
            }
        }

        return 1;
    }

    private void dropHeartItem(Player victim) {
        victim.getWorld().dropItemNaturally(victim.getLocation(), plugin.getRecipeManager().createHeartItem());
    }
}
