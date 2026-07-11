package com.deruy.plugin.lifesteal.listeners;

import com.deruy.plugin.DeruyPlugin;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

/**
 * 기본룰: 엔더진주 사용 금지.
 * config: lifesteal.restrictions.ender-pearl-blocked (기본 true)
 */
public class EnderPearlListener implements Listener {

    private final DeruyPlugin plugin;

    public EnderPearlListener(DeruyPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (!plugin.getLifeStealManager().isSystemEnabled()) return;

        boolean blocked = plugin.getConfig().getBoolean("lifesteal.restrictions.ender-pearl-blocked", true);
        if (!blocked) return;

        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        ItemStack item = event.getItem();
        if (item == null || item.getType() != Material.ENDER_PEARL) return;

        Player player = event.getPlayer();
        event.setCancelled(true);
        player.sendMessage("§c엔더진주 사용이 금지되어 있습니다.");
    }
}
