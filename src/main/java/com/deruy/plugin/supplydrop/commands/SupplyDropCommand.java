package com.deruy.plugin.supplydrop.commands;

import com.deruy.plugin.DeruyPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

/**
 * /supplydrop start|stop|force        - 일반 서플라이드랍 관리
 * /supersupplydrop start|stop|force   - 슈퍼 서플라이드랍 관리
 */
public class SupplyDropCommand implements CommandExecutor {

    private final DeruyPlugin plugin;
    private final boolean superMode;

    public SupplyDropCommand(DeruyPlugin plugin, boolean superMode) {
        this.plugin = plugin;
        this.superMode = superMode;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("§e사용법: /" + label + " start|stop|force");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "start" -> {
                if (superMode) plugin.getSuperSupplyDropManager().start();
                else plugin.getSupplyDropManager().start();
                return true;
            }
            case "stop" -> {
                if (superMode) plugin.getSuperSupplyDropManager().stop();
                else plugin.getSupplyDropManager().stop();
                return true;
            }
            case "force" -> {
                if (superMode) plugin.getSuperSupplyDropManager().triggerDrop();
                else plugin.getSupplyDropManager().triggerDrop();
                sender.sendMessage("§a강제로 드랍을 실행했습니다.");
                return true;
            }
            default -> {
                sender.sendMessage("§c알 수 없는 하위 명령어입니다.");
                return true;
            }
        }
    }
}
