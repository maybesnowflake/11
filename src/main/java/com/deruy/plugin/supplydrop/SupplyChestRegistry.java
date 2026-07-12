package com.deruy.plugin.supplydrop;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Logger;

/**
 * 일반/슈퍼 서플라이드랍이 공유하는 상자 레지스트리.
 * - 상자를 "블록"으로 스폰하되, 실제 아이템 내용물은 여기서 미리 확률에 맞춰 굴려서 저장해둔다.
 * - 플레이어가 클릭하면(SupplyChestListener) 여기서 내용물을 꺼내 즉시 지급하고 등록을 해제한다.
 *
 * reward-items 설정 형식: "MATERIAL" 또는 "MATERIAL:확률" (예: "DIAMOND:0.1" = 10% 확률로 포함)
 * 콜론이 없으면 100% 확률로 취급한다.
 */
public class SupplyChestRegistry {

    private final Map<Location, List<ItemStack>> chests = new HashMap<>();

    /**
     * config의 reward-items 리스트를 확률에 맞게 굴려서 이번 상자에 들어갈 아이템 목록을 만든다.
     */
    public List<ItemStack> rollRewards(List<String> rewardEntries, Logger logger) {
        List<ItemStack> result = new ArrayList<>();

        for (String entry : rewardEntries) {
            String materialName = entry;
            double probability = 1.0;

            int colonIndex = entry.lastIndexOf(':');
            if (colonIndex > 0) {
                materialName = entry.substring(0, colonIndex);
                try {
                    probability = Double.parseDouble(entry.substring(colonIndex + 1));
                } catch (NumberFormatException e) {
                    logger.warning("잘못된 확률 형식: " + entry + " (예: DIAMOND:0.1)");
                    probability = 1.0;
                }
            }

            try {
                Material material = Material.valueOf(materialName.trim().toUpperCase());
                if (ThreadLocalRandom.current().nextDouble() < probability) {
                    result.add(new ItemStack(material));
                }
            } catch (IllegalArgumentException e) {
                logger.warning("존재하지 않는 아이템: " + materialName);
            }
        }

        return result;
    }

    public void register(Location location, List<ItemStack> items) {
        chests.put(location.getBlock().getLocation(), items);
    }

    public boolean isTracked(Location location) {
        return chests.containsKey(location.getBlock().getLocation());
    }

    /** 상자 내용물을 꺼내고 등록을 해제한다. 이미 처리된 상자면 null. */
    public List<ItemStack> consume(Location location) {
        return chests.remove(location.getBlock().getLocation());
    }
}
