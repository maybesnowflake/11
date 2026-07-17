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

        boolean naturalDeathRemovesHeart = plugin.getConfig()
                .getBoolean("lifesteal.natural-death-removes-heart", false);

        // PVP 사망이거나, "자연사도 하트 깎임" 옵션이 켜져있으면 -1 하트
        boolean heartWasRemoved = killer != null || naturalDeathRemovesHeart;
        if (heartWasRemoved) {
            plugin.getLifeStealManager().removeHeart(victim);
        }

        if (killer != null) {
            plugin.getBountyManager().onKill(killer, victim);

            // 킬러가 이미 하트 상한선(max-hearts)에 도달했으면 지급 대신 아이템으로 드롭
            if (!plugin.getLifeStealManager().addHeart(killer)) {
                dropHeartItem(victim);
            }
        } else if (heartWasRemoved) {
            // 자연사로 하트가 깎였는데 받을 사람(킬러)이 없으므로 아이템으로 드롭
            dropHeartItem(victim);
        }
    }

    private void dropHeartItem(Player victim) {
        victim.getWorld().dropItemNaturally(victim.getLocation(), plugin.getRecipeManager().createHeartItem());
    }
}
