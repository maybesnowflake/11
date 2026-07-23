package com.deruy.plugin.bounty;

import com.deruy.plugin.DeruyPlugin;
import com.deruy.plugin.events.GameEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 바운티 이벤트: 서로 다른 팀의 두 플레이어를 지정하면, 서로가 서로의 타겟이 된다.
 * 지정된 타겟을 처치하면 추가 보상 지급.
 *
 * config: bounty.reward-items
 */
public class BountyManager implements GameEvent {

    private final DeruyPlugin plugin;
    private boolean running = false;

    private final Map<UUID, UUID> bounties = new HashMap<>();

    public BountyManager(DeruyPlugin plugin) {
        this.plugin = plugin;
        bounties.putAll(plugin.getDataStore().loadBountyPairs());
        if (!bounties.isEmpty()) {
            plugin.getLogger().info("바운티 페어 " + (bounties.size() / 2) + "쌍을 data.yml에서 불러왔습니다.");
        }
    }

    @Override
    public String getName() {
        return "bounty";
    }

    @Override
    public void start() {
        if (running) return;
        running = true;
        Bukkit.broadcastMessage("§4§l바운티 §e이벤트가 시작되었습니다. 관리자가 지정한 타겟을 처치하면 추가 보상이 지급됩니다.");
    }

    @Override
    public void stop() {
        if (!running) return;
        running = false;
        bounties.clear();
        Bukkit.broadcastMessage("§4§l바운티 §e이벤트가 종료되었습니다.");
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    public void pair(Player a, Player b) {
        bounties.put(a.getUniqueId(), b.getUniqueId());
        bounties.put(b.getUniqueId(), a.getUniqueId());
        plugin.getDataStore().saveBountyPair(a.getUniqueId(), b.getUniqueId());
        a.sendMessage("§4[바운티] §e당신의 타겟: " + b.getName());
        b.sendMessage("§4[바운티] §e당신의 타겟: " + a.getName());
    }

    public void unpair(Player a) {
        UUID targetId = bounties.remove(a.getUniqueId());
        if (targetId != null) {
            bounties.remove(targetId);
        }
        plugin.getDataStore().removeBountyPair(a.getUniqueId());
    }

    public UUID getTarget(Player player) {
        return bounties.get(player.getUniqueId());
    }

    public void onKill(Player killer, Player victim) {
        if (!running) return;
        UUID target = bounties.get(killer.getUniqueId());
        if (target == null || !target.equals(victim.getUniqueId())) return;

        String deathMessage = plugin.getMessage("bounty-target-killed",
                "&4&l[바운티] &a{killer}님이 타겟 {victim}님을 처치했습니다!")
                .replace("{killer}", killer.getName())
                .replace("{victim}", victim.getName());
        Bukkit.broadcastMessage(deathMessage);

        var items = plugin.getSupplyChestRegistry().rollRewards(
                plugin.getConfig().getStringList("bounty.reward-items"), plugin.getLogger());
        for (var item : items) {
            var leftover = killer.getInventory().addItem(item);
            leftover.values().forEach(remain -> killer.getWorld().dropItemNaturally(killer.getLocation(), remain));
        }

        bounties.remove(killer.getUniqueId());
        bounties.remove(victim.getUniqueId());
        plugin.getDataStore().removeBountyPair(killer.getUniqueId());

        if (plugin.getConfig().getBoolean("bounty.end-on-death", false)) {
            Bukkit.broadcastMessage("§4§l[바운티] §e바운티 대상이 사망하여 이벤트가 종료됩니다.");
            stop();
        }
    }
}
