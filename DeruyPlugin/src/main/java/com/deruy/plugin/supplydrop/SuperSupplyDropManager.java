package com.deruy.plugin.supplydrop;

import com.deruy.plugin.DeruyPlugin;
import com.deruy.plugin.events.GameEvent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 슈퍼 서플라이드랍: 일반 드랍과 별개로, 하루에 여러 개의 "정확한 시간대"를
 * config로 지정해두면 그 시간대마다(윈도우당 1회) 자동으로 더 좋은 보상의 드랍을 실행.
 * 예: 12:45~13:00, 16:45~17:00, 18:45~19:00 이런 식으로 여러 개 등록 가능.
 *
 * config: supplydrop.super.*
 *   windows: ["12:45-13:00", "16:45-17:00", "18:45-19:00"]
 */
public class SuperSupplyDropManager implements GameEvent {

    private final DeruyPlugin plugin;
    private boolean running = false;
    private BukkitTask checkTask;

    private final Set<Integer> triggeredWindowsToday = new HashSet<>();
    private int lastResetDay = -1;

    public SuperSupplyDropManager(DeruyPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "supersupplydrop";
    }

    @Override
    public void start() {
        if (running) return;
        running = true;
        checkTask = Bukkit.getScheduler().runTaskTimer(plugin, this::check, 0L, 20L * 20);
        Bukkit.broadcastMessage("§c§l슈퍼 서플라이드랍 §e시스템이 활성화되었습니다.");
    }

    @Override
    public void stop() {
        if (!running) return;
        running = false;
        if (checkTask != null) {
            checkTask.cancel();
            checkTask = null;
        }
        Bukkit.broadcastMessage("§c§l슈퍼 서플라이드랍 §e시스템이 비활성화되었습니다.");
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    private void check() {
        int dayValue = java.time.LocalDate.now().getDayOfYear();
        if (dayValue != lastResetDay) {
            triggeredWindowsToday.clear();
            lastResetDay = dayValue;
        }

        List<String> windows = plugin.getConfig().getStringList("supplydrop.super.windows");
        LocalTime now = LocalTime.now();

        for (int i = 0; i < windows.size(); i++) {
            if (triggeredWindowsToday.contains(i)) continue;

            String[] parts = windows.get(i).split("-");
            if (parts.length != 2) continue;

            try {
                LocalTime start = LocalTime.parse(parts[0].trim());
                LocalTime end = LocalTime.parse(parts[1].trim());

                if (!now.isBefore(start) && now.isBefore(end)) {
                    triggerDrop();
                    triggeredWindowsToday.add(i);
                }
            } catch (Exception e) {
                plugin.getLogger().warning("잘못된 슈퍼 서플라이드랍 시간대 형식: " + windows.get(i) + " (예: 12:45-13:00)");
            }
        }
    }

    public void triggerDrop() {
        World world = Bukkit.getWorlds().get(0);
        double centerX = plugin.getConfig().getDouble("supplydrop.super.center-x", 0);
        double centerZ = plugin.getConfig().getDouble("supplydrop.super.center-z", 0);
        int radius = plugin.getConfig().getInt("supplydrop.super.radius", 300);

        Random random = ThreadLocalRandom.current();
        double x = centerX + (random.nextDouble() * 2 - 1) * radius;
        double z = centerZ + (random.nextDouble() * 2 - 1) * radius;
        int y = world.getHighestBlockYAt((int) x, (int) z) + 1;

        Location loc = new Location(world, x, y, z);
        Block block = loc.getBlock();
        block.setType(Material.CHEST);

        if (block.getState() instanceof Chest chest) {
            List<String> rewardNames = plugin.getConfig().getStringList("supplydrop.super.reward-items");
            for (String name : rewardNames) {
                try {
                    Material mat = Material.valueOf(name.toUpperCase());
                    chest.getInventory().addItem(new ItemStack(mat));
                } catch (IllegalArgumentException ignored) {
                }
            }
        }

        Bukkit.broadcastMessage("§c§l[슈퍼 서플라이드랍] §e고급 보급상자가 등장했습니다!");
    }
}
