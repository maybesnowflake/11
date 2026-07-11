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
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 일반 서플라이드랍: 지정 시간대(윈도우) 안에서 완전 랜덤한 시각에 자동 실행.
 * 그 시각값은 로그/커맨드로 절대 노출하지 않아 관리자도 재부팅 전까지는 알 수 없다.
 *
 * config: supplydrop.normal.*
 */
public class SupplyDropManager implements GameEvent {

    private final DeruyPlugin plugin;
    private boolean running = false;
    private BukkitTask checkTask;

    private LocalTime todayTarget;
    private int lastTriggeredDay = -1;

    public SupplyDropManager(DeruyPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "supplydrop";
    }

    @Override
    public void start() {
        if (running) return;
        running = true;
        rollTodayTarget();
        checkTask = Bukkit.getScheduler().runTaskTimer(plugin, this::check, 0L, 20L * 30);
        Bukkit.broadcastMessage("§6§l서플라이드랍 §e시스템이 활성화되었습니다. 언제 떨어질지는 아무도 모릅니다.");
    }

    @Override
    public void stop() {
        if (!running) return;
        running = false;
        if (checkTask != null) {
            checkTask.cancel();
            checkTask = null;
        }
        todayTarget = null;
        Bukkit.broadcastMessage("§6§l서플라이드랍 §e시스템이 비활성화되었습니다.");
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    private void rollTodayTarget() {
        int startHour = plugin.getConfig().getInt("supplydrop.normal.window-start-hour", 15);
        int endHour = plugin.getConfig().getInt("supplydrop.normal.window-end-hour", 24);

        Random random = ThreadLocalRandom.current();
        int hour = startHour + random.nextInt(Math.max(1, endHour - startHour));
        int minute = random.nextInt(60);
        if (hour >= 24) hour = 23;

        this.todayTarget = LocalTime.of(hour, minute);
        // 절대 로그로 남기지 않음 (관리자도 모르게)
    }

    private void check() {
        java.time.LocalDate today = java.time.LocalDate.now();
        int dayValue = today.getDayOfYear();

        if (dayValue != lastTriggeredDay && todayTarget != null) {
            LocalTime now = LocalTime.now();
            if (!now.isBefore(todayTarget)) {
                triggerDrop();
                lastTriggeredDay = dayValue;
                rollTodayTarget();
            }
        }
    }

    /** 관리자가 즉시 강제로 하나 떨어뜨리고 싶을 때 사용 (랜덤시각과 별개). */
    public void triggerDrop() {
        World world = Bukkit.getWorlds().get(0);
        double centerX = plugin.getConfig().getDouble("supplydrop.normal.center-x", 0);
        double centerZ = plugin.getConfig().getDouble("supplydrop.normal.center-z", 0);
        int radius = plugin.getConfig().getInt("supplydrop.normal.radius", 200);

        Random random = ThreadLocalRandom.current();
        double x = centerX + (random.nextDouble() * 2 - 1) * radius;
        double z = centerZ + (random.nextDouble() * 2 - 1) * radius;
        int y = world.getHighestBlockYAt((int) x, (int) z) + 1;

        Location loc = new Location(world, x, y, z);
        Block block = loc.getBlock();
        block.setType(Material.CHEST);

        if (block.getState() instanceof Chest chest) {
            List<String> rewardNames = plugin.getConfig().getStringList("supplydrop.normal.reward-items");
            for (String name : rewardNames) {
                try {
                    Material mat = Material.valueOf(name.toUpperCase());
                    chest.getInventory().addItem(new ItemStack(mat));
                } catch (IllegalArgumentException ignored) {
                }
            }
        }

        Bukkit.broadcastMessage("§6§l[서플라이드랍] §e보급상자가 등장했습니다! 근처를 수색하세요.");
    }
}
