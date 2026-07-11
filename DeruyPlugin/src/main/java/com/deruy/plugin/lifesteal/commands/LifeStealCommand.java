package com.deruy.plugin.lifesteal.commands;

import com.deruy.plugin.DeruyPlugin;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

/**
 * /lifesteal toggle           - 시스템 전체 on/off (#12)
 * /lifesteal status           - 현재 상태 확인
 * /lifesteal recipe additem <레시피이름> <슬롯문자> <머티리얼>  - 레시피 재료 커스터마이징 (#8)
 */
public class LifeStealCommand implements CommandExecutor {

    private final DeruyPlugin plugin;

    public LifeStealCommand(DeruyPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("§e/lifesteal toggle | status | recipe additem <name> <slot> <material>");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "toggle" -> {
                boolean newState = !plugin.getLifeStealManager().isSystemEnabled();
                plugin.getLifeStealManager().setSystemEnabled(newState);
                sender.sendMessage("§aLifeSteal 시스템: " + (newState ? "§a활성화" : "§c비활성화"));
                return true;
            }
            case "status" -> {
                boolean enabled = plugin.getLifeStealManager().isSystemEnabled();
                sender.sendMessage("§eLifeSteal 시스템 현재 상태: " + (enabled ? "§a활성화" : "§c비활성화"));
                return true;
            }
            case "recipe" -> {
                if (args.length < 5 || !args[1].equalsIgnoreCase("additem")) {
                    sender.sendMessage("§c사용법: /lifesteal recipe additem <레시피이름> <슬롯문자> <머티리얼>");
                    sender.sendMessage("§7레시피이름: totem_of_undying_recipe, custom_mace_recipe, custom_golden_apple_recipe");
                    return true;
                }
                String recipeName = args[2];
                char slot = args[3].charAt(0);
                Material material;
                try {
                    material = Material.valueOf(args[4].toUpperCase());
                } catch (IllegalArgumentException e) {
                    sender.sendMessage("§c존재하지 않는 아이템: " + args[4]);
                    return true;
                }

                boolean success = plugin.getRecipeManager().addIngredientToRecipe(recipeName, slot, material);
                if (success) {
                    sender.sendMessage("§a레시피 '" + recipeName + "'의 슬롯 '" + slot + "'를 " + material + "(으)로 변경했습니다.");
                } else {
                    sender.sendMessage("§c해당 이름의 셰이프드 레시피를 찾을 수 없습니다: " + recipeName);
                }
                return true;
            }
            default -> {
                sender.sendMessage("§e알 수 없는 하위 명령어입니다.");
                return true;
            }
        }
    }
}
