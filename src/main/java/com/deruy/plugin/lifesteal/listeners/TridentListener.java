package com.deruy.plugin.lifesteal.listeners;

import com.deruy.plugin.DeruyPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRiptideEvent;

/**
 * pending #4: 기존에는 트라이던트(리피터틴) 사용 자체를 전면 금지했으나,
 * 이제는 "컴벳태그 상태일 때만" 금지한다. config.yml의
 * lifesteal.trident.restrict-only-in-combat 로 켜고 끌 수 있다.
 */
public class TridentListener implements Listener {

    private final DeruyPlugin plugin;

    public TridentListener(DeruyPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onRiptide(PlayerRiptideEvent event) {
        if (!plugin.getLifeStealManager().isSystemEnabled()) return;

        boolean restrictOnlyInCombat = plugin.getConfig()
                .getBoolean("lifesteal.trident.restrict-only-in-combat", true);

        if (!restrictOnlyInCombat) return; // 제한 자체가 꺼져있으면 통과

        Player player = event.getPlayer();
        if (plugin.getLifeStealManager().isCombatTagged(player.getUniqueId())) {
            player.sendMessage("§c전투 중에는 트라이던트 순간이동을 사용할 수 없습니다.");
            player.setVelocity(player.getVelocity().multiply(0));
        }
    }
}
