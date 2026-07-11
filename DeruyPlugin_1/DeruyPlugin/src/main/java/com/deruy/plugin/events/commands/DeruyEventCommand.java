package com.deruy.plugin.events.commands;

import com.deruy.plugin.DeruyPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

/**
 * /devent start <이벤트이름>  - 등록된 이벤트를 수동으로 시작 (콘솔/인게임 둘 다 가능)
 * /devent stop <이벤트이름>   - 진행중인 이벤트 종료
 * /devent list                - 등록된 이벤트 + 실행상태 목록
 *
 * 일정표의 날짜/시각은 참고용이고, 실제 시작은 관리자가 이 커맨드로 직접 트리거한다.
 */
public class DeruyEventCommand implements CommandExecutor {

    private final DeruyPlugin plugin;

    public DeruyEventCommand(DeruyPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("§e/devent start <name> | stop <name> | list");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "list" -> {
                sender.sendMessage("§6=== 등록된 이벤트 ===");
                plugin.getEventManager().getAll().forEach((name, event) ->
                        sender.sendMessage("§e- " + name + ": " + (event.isRunning() ? "§a실행중" : "§7대기중"))
                );
                return true;
            }
            case "start" -> {
                if (args.length < 2) {
                    sender.sendMessage("§c사용법: /devent start <이벤트이름>");
                    return true;
                }
                boolean ok = plugin.getEventManager().start(args[1]);
                sender.sendMessage(ok
                        ? "§a이벤트 '" + args[1] + "' 시작 명령을 전달했습니다."
                        : "§c존재하지 않는 이벤트입니다: " + args[1]);
                return true;
            }
            case "stop" -> {
                if (args.length < 2) {
                    sender.sendMessage("§c사용법: /devent stop <이벤트이름>");
                    return true;
                }
                boolean ok = plugin.getEventManager().stop(args[1]);
                sender.sendMessage(ok
                        ? "§a이벤트 '" + args[1] + "' 종료 명령을 전달했습니다."
                        : "§c존재하지 않는 이벤트입니다: " + args[1]);
                return true;
            }
            default -> {
                sender.sendMessage("§c알 수 없는 하위 명령어입니다.");
                return true;
            }
        }
    }
}
