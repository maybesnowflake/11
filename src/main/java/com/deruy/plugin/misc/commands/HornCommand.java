package com.deruy.plugin.misc.commands;

import com.deruy.plugin.DeruyPlugin;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * /horn <개수 1~8> - config의 horn.allowed-roles에 등록된 역할만 사용 가능.
 * 소실의 저주(VANISHING_CURSE)가 인첸트된 염소뿔을 지정한 개수만큼 지급한다.
 */
public class HornCommand implements CommandExecutor, TabCompleter {

    private final DeruyPlugin plugin;

    public HornCommand(DeruyPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§c이 명령어는 인게임에서만 사용 가능합니다.");
            return true;
        }

        if (!hasAllowedRole(player)) {
            sender.sendMessage(plugin.getMessage("horn-no-permission", "&c이 명령어를 사용할 권한이 없습니다."));
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage("§c사용법: /horn <개수 1~8>");
            return true;
        }

        int count;
        try {
            count = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            sender.sendMessage("§c숫자를 입력해주세요. (1~8)");
            return true;
        }

        if (count < 1 || count > 8) {
            sender.sendMessage("§c개수는 1~8 사이여야 합니다.");
            return true;
        }

        for (int i = 0; i < count; i++) {
            var leftover = player.getInventory().addItem(createEnchantedHorn());
            leftover.values().forEach(remain -> player.getWorld().dropItemNaturally(player.getLocation(), remain));
        }

        player.sendMessage("§a소실의 저주가 인첸트된 염소뿔 " + count + "개를 받았습니다.");
        return true;
    }

    private ItemStack createEnchantedHorn() {
        ItemStack horn = new ItemStack(Material.GOAT_HORN);
        ItemMeta meta = horn.getItemMeta();
        meta.addEnchant(Enchantment.VANISHING_CURSE, 1, true);
        horn.setItemMeta(meta);
        return horn;
    }

    /** config의 horn.allowed-roles 목록 중 하나라도 해당되면 true */
    private boolean hasAllowedRole(Player player) {
        List<String> allowedRoles = plugin.getConfig().getStringList("horn.allowed-roles");
        if (allowedRoles.isEmpty()) return true; // 설정 없으면 전원 허용

        for (String role : allowedRoles) {
            if (plugin.hasRole(player, role)) return true;
        }
        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> result = new ArrayList<>();
        if (args.length == 1) {
            StringUtil.copyPartialMatches(args[0], List.of("1", "2", "3", "4", "5", "6", "7", "8"), result);
        }
        return result;
    }
}
