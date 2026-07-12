package com.deruy.plugin.bounty;

import com.deruy.plugin.DeruyPlugin;
import com.deruy.plugin.events.GameEvent;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

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
        a.sendMessage("§4[바운티] §e당신의 타겟: " + b.getName());
        b.sendMessage("§4[바운티] §e당신의 타겟: " + a.getName());
    }

    public void unpair(Player a) {
        UUID targetId = bounties.remove(a.getUniqueId());
        if (targetId != null) {
            bounties.remove(targetId);
        }
    }

    public UUID getTarget(Player player) {
        return bounties.get(player.getUniqueId());
    }

    public void onKill(Player killer, Player victim) {
        if (!running) return;
        UUID target = bounties.get(killer.getUniqueId());
        if (target == null || !target.equals(victim.getUniqueId())) return;

        Bukkit.broadcastMessage("§4§l[바운티] §a" + killer.getName() + "님이 타겟 " + victim.getName() + "님을 처치하고 보상을 획득했습니다!");

        for (String name : plugin.getConfig().getStringList("bounty.reward-items")) {
            try {
                Material mat = Material.valueOf(name.toUpperCase());
                killer.getInventory().addItem(new ItemStack(mat));
            } catch (IllegalArgumentException ignored) {
            }
        }

        bounties.remove(killer.getUniqueId());
        bounties.remove(victim.getUniqueId());
    }
}
