package com.deruy.plugin.bingo.commands;

import com.deruy.plugin.DeruyPlugin;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * /bingo team <플레이어> <팀이름>  - 팀 배정
 * /bingo start | stop             - 이벤트 시작/종료
 * /bingo excludeitem <머티리얼>     - 제외 아이템 추가 (config에도 반영)
 * /bingo excludebiome <바이옴>      - 제외 바이옴 추가 (config에도 반영)
 */
public class BingoCommand implements CommandExecutor {

    private final DeruyPlugin plugin;

    public BingoCommand(DeruyPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("§e/bingo team <player> <team> | start | stop | excludeitem <material> | excludebiome <biome>");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "team" -> {
                if (args.length < 3) {
                    sender.sendMessage("§c사용법: /bingo team <player> <team>");
                    return true;
                }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    sender.sendMessage("§c플레이어를 찾을 수 없습니다: " + args[1]);
                    return true;
                }
                plugin.getBingoManager().setTeam(target, args[2]);
                sender.sendMessage("§a" + target.getName() + "님을 팀 '" + args[2] + "'에 배정했습니다.");
                return true;
            }
            case "start" -> {
                plugin.getBingoManager().start();
                return true;
            }
            case "stop" -> {
                plugin.getBingoManager().stop();
                return true;
            }
            case "excludeitem" -> {
                if (args.length < 2) {
                    sender.sendMessage("§c사용법: /bingo excludeitem <머티리얼이름>");
                    return true;
                }
                var list = plugin.getConfig().getStringList("bingo.excluded-items");
                list.add(args[1].toUpperCase());
                plugin.getConfig().set("bingo.excluded-items", list);
                plugin.saveConfig();
                sender.sendMessage("§a제외 아이템에 " + args[1].toUpperCase() + " 추가됨. (다음 /bingo start 부터 반영)");
                return true;
            }
            case "excludebiome" -> {
                if (args.length < 2) {
                    sender.sendMessage("§c사용법: /bingo excludebiome <바이옴이름>");
                    return true;
                }
                var list = plugin.getConfig().getStringList("bingo.excluded-biomes");
                list.add(args[1].toUpperCase());
                plugin.getConfig().set("bingo.excluded-biomes", list);
                plugin.saveConfig();
                sender.sendMessage("§a제외 바이옴에 " + args[1].toUpperCase() + " 추가됨. (다음 /bingo start 부터 반영)");
                return true;
            }
            default -> {
                sender.sendMessage("§c알 수 없는 하위 명령어입니다.");
                return true;
            }
        }
    }
}
