package com.deruy.plugin.lifesteal.listeners;

import com.deruy.plugin.DeruyPlugin;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

/**
 * pending #5: 겉날개 장착 / 엔더상자 사용을 "컴벳태그 중일 때만" 제한.
 * (트라이던트 #4 와 동일한 패턴)
 *
 * lifesteal.restrictions.elytra-blocked      : 이 규칙 자체를 켜고 끔 (기본 true)
 * lifesteal.restrictions.ender-chest-blocked : 이 규칙 자체를 켜고 끔 (기본 true)
 * 규칙이 켜져 있어도 컴벳태그 상태가 아니면 항상 허용됨.
 */
public class RestrictionListener implements Listener {

    private final DeruyPlugin plugin;

    public RestrictionListener(DeruyPlugin plugin) {
        this.plugin = plugin;
    }

    private boolean isCombatTagged(Player player) {
        return plugin.getLifeStealManager().isCombatTagged(player.getUniqueId());
    }

    @EventHandler
    public void onEnderChestUse(PlayerInteractEvent event) {
        if (!plugin.getLifeStealManager().isSystemEnabled()) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getClickedBlock() == null) return;
        if (event.getClickedBlock().getType() != Material.ENDER_CHEST) return;

        boolean ruleActive = plugin.getConfig()
                .getBoolean("lifesteal.restrictions.ender-chest-blocked", true);
        if (!ruleActive) return;

        Player player = event.getPlayer();
        if (isCombatTagged(player)) {
            event.setCancelled(true);
            player.sendMessage("§c전투 중에는 엔더상자를 사용할 수 없습니다.");
        }
    }

    @EventHandler
    public void onElytraEquip(InventoryClickEvent event) {
        if (!plugin.getLifeStealManager().isSystemEnabled()) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;

        boolean ruleActive = plugin.getConfig()
                .getBoolean("lifesteal.restrictions.elytra-blocked", true);
        if (!ruleActive) return;

        ItemStack cursor = event.getCursor();
        boolean equippingElytra = cursor != null
                && cursor.getType() == Material.ELYTRA
                && event.getSlotType() == InventoryType.SlotType.ARMOR;

        if (equippingElytra && isCombatTagged(player)) {
            event.setCancelled(true);
            player.sendMessage("§c전투 중에는 겉날개를 장착할 수 없습니다.");
        }
    }
}
