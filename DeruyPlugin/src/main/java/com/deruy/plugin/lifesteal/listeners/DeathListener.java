package com.deruy.plugin.lifesteal.listeners;

import com.deruy.plugin.DeruyPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;

/**
 * 하트 시스템 핵심: 킬 시 +1하트, 사망 시 -1하트.
 * PVP 데미지 발생 시 컴벳태그 부여 (다른 리스너들이 공통으로 참조).
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

        // 사망시 -1 하트 (0하트 도달시 LifeStealManager 내부에서 밴 처리)
        plugin.getLifeStealManager().removeHeart(victim);

        if (killer != null) {
            plugin.getLifeStealManager().addHeart(killer);
            plugin.getBountyManager().onKill(killer, victim);
        }
    }
}
