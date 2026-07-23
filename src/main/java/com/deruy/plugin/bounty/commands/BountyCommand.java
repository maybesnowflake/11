package com.deruy.plugin.bounty.commands;

import com.deruy.plugin.DeruyPlugin;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * /bounty pair <player1> <player2>  - 서로를 바운티 타겟으로 지정
 * /bounty unpair <player>           - 바운티 해제
 * /bounty start | stop              - 이벤트 시작/종료
 */
public class BountyCommand implements CommandExecutor, TabCompleter {

    private final DeruyPlugin plugin;

    public BountyCommand(DeruyPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("§e/bounty pair <player1> <player2> | unpair <player> | start | stop");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "pair" -> {
                if (args.length < 3) {
                    sender.sendMessage("§c사용법: /bounty pair <player1> <player2>");
                    return true;
                }
                Player a = Bukkit.getPlayer(args[1]);
                Player b = Bukkit.getPlayer(args[2]);
                if (a == null || b == null) {
                    sender.sendMessage("§c플레이어를 찾을 수 없습니다.");
                    return true;
                }
                plugin.getBountyManager().pair(a, b);
                sender.sendMessage("§a" + a.getName() + " ↔ " + b.getName() + " 바운티 매칭 완료.");
                return true;
            }
            case "unpair" -> {
                if (args.length < 2) {
                    sender.sendMessage("§c사용법: /bounty unpair <player>");
                    return true;
                }
                Player p = Bukkit.getPlayer(args[1]);
                if (p == null) {
                    sender.sendMessage("§c플레이어를 찾을 수 없습니다.");
                    return true;
                }
                plugin.getBountyManager().unpair(p);
                sender.sendMessage("§a" + p.getName() + "님의 바운티를 해제했습니다.");
                return true;
            }
            case "start" -> {
                plugin.getBountyManager().start();
                return true;
            }
            case "stop" -> {
                plugin.getBountyManager().stop();
                return true;
            }
            default -> {
                sender.sendMessage("§c알 수 없는 하위 명령어입니다.");
                return true;
            }
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> result = new ArrayList<>();
        List<String> playerNames = Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList());

        if (args.length == 1) {
            StringUtil.copyPartialMatches(args[0], List.of("pair", "unpair", "start", "stop"), result);
        } else if (args.length == 2 && (args[0].equalsIgnoreCase("pair") || args[0].equalsIgnoreCase("unpair"))) {
            StringUtil.copyPartialMatches(args[1], playerNames, result);
        } else if (args.length == 3 && args[0].equalsIgnoreCase("pair")) {
            StringUtil.copyPartialMatches(args[2], playerNames, result);
        }
        return result;
    }
}
