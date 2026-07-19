package com.deruy.plugin.lifesteal.listeners;

import com.deruy.plugin.DeruyPlugin;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

/**
 * pending #5: 겉날개 장착 / 엔더상자 사용을 "컴벳태그 중일 때만" 제한.
 *
 * 겉날개는 장착 경로가 여러가지라(우클릭 자동장착, 쉬프트클릭, 드래그 등)
 * 클릭 종류별로 개별 분기하지 않고, 클릭이 끝난 "다음 틱"에 가슴 슬롯을 직접 검사해서
 * 겉날개가 들어가 있으면 무조건 되돌리는 방식으로 처리한다 (놓치는 경로가 없음).
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

    // ---------------- 겉날개: 우클릭 자동장착 차단 ----------------

    @EventHandler
    public void onRightClickEquip(PlayerInteractEvent event) {
        if (!plugin.getLifeStealManager().isSystemEnabled()) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        ItemStack item = event.getItem();
        if (item == null || item.getType() != Material.ELYTRA) return;

        boolean ruleActive = plugin.getConfig()
                .getBoolean("lifesteal.restrictions.elytra-blocked", true);
        if (!ruleActive) return;

        Player player = event.getPlayer();
        if (isCombatTagged(player)) {
            event.setCancelled(true);
            player.sendMessage("§c전투 중에는 겉날개를 장착할 수 없습니다.");
        }
    }

    // ---------------- 겉날개: 인벤토리 내 클릭/쉬프트클릭/드래그 전부 - 다음틱 검사방식 ----------------

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!plugin.getLifeStealManager().isSystemEnabled()) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;

        boolean ruleActive = plugin.getConfig()
                .getBoolean("lifesteal.restrictions.elytra-blocked", true);
        if (!ruleActive) return;
        if (!isCombatTagged(player)) return;

        plugin.getServer().getScheduler().runTask(plugin, () -> {
            ItemStack chest = player.getInventory().getChestplate();
            if (chest != null && chest.getType() == Material.ELYTRA) {
                player.getInventory().setChestplate(null);
                var leftover = player.getInventory().addItem(chest);
                leftover.values().forEach(remain ->
                        player.getWorld().dropItemNaturally(player.getLocation(), remain));
                player.sendMessage("§c전투 중에는 겉날개를 장착할 수 없습니다.");
            }
        });
    }
}
