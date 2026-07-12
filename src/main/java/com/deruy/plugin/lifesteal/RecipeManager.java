package com.deruy.plugin.lifesteal;

import com.deruy.plugin.DeruyPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * LifeSteal 커스텀 조합법 전담 관리자.
 * 하트/토템/메이스/황금사과 레시피 전부 config.yml (lifesteal.recipes.*) 에서 읽어와 등록한다.
 * 서버 재시작만으로 조합법 모양/재료를 자유롭게 바꿀 수 있다.
 */
public class RecipeManager {

    /** "하트" 아이템(빨간 염료 아이콘)을 식별하기 위한 PDC 키 */
    public static final NamespacedKey HEART_ITEM_KEY = new NamespacedKey("deruy", "heart_item");

    private final DeruyPlugin plugin;

    public RecipeManager(DeruyPlugin plugin) {
        this.plugin = plugin;
    }

    public void registerAll() {
        Bukkit.removeRecipe(NamespacedKey.minecraft("mace"));
        Bukkit.removeRecipe(NamespacedKey.minecraft("golden_apple"));

        ConfigurationSection recipesSection = plugin.getConfig().getConfigurationSection("lifesteal.recipes");
        if (recipesSection == null) {
            plugin.getLogger().warning("lifesteal.recipes 설정이 없어 커스텀 레시피를 등록하지 않습니다.");
            return;
        }

        for (String recipeName : recipesSection.getKeys(false)) {
            ConfigurationSection section = recipesSection.getConfigurationSection(recipeName);
            if (section == null) continue;
            registerFromConfig(recipeName, section);
        }
    }

    private void registerFromConfig(String recipeName, ConfigurationSection section) {
        String resultMaterialName = section.getString("result-material");
        if (resultMaterialName == null) {
            plugin.getLogger().warning("레시피 '" + recipeName + "'에 result-material이 없어 건너뜁니다.");
            return;
        }

        Material resultMaterial;
        try {
            resultMaterial = Material.valueOf(resultMaterialName.toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("레시피 '" + recipeName + "'의 result-material이 잘못됨: " + resultMaterialName);
            return;
        }

        List<String> shapeList = section.getStringList("shape");
        if (shapeList.size() != 3) {
            plugin.getLogger().warning("레시피 '" + recipeName + "'의 shape은 반드시 3줄이어야 합니다.");
            return;
        }

        ConfigurationSection ingredientsSection = section.getConfigurationSection("ingredients");
        if (ingredientsSection == null) {
            plugin.getLogger().warning("레시피 '" + recipeName + "'에 ingredients가 없습니다.");
            return;
        }

        ItemStack result = buildResultItem(recipeName, resultMaterial, section);

        NamespacedKey key = new NamespacedKey(plugin, recipeName);
        ShapedRecipe recipe = new ShapedRecipe(key, result);
        recipe.shape(shapeList.get(0), shapeList.get(1), shapeList.get(2));

        for (String symbol : ingredientsSection.getKeys(false)) {
            String materialName = ingredientsSection.getString(symbol);
            if (materialName == null) continue;
            try {
                Material ingredientMaterial = Material.valueOf(materialName.toUpperCase());
                recipe.setIngredient(symbol.charAt(0), ingredientMaterial);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("레시피 '" + recipeName + "'의 재료가 잘못됨: " + materialName);
            }
        }

        Bukkit.addRecipe(recipe);
    }

    /**
     * "heart" 레시피는 결과물이 그냥 아이템이 아니라, 우클릭시 소비되어 하트를 지급하는
     * 커스텀 아이템(빨간 염료 아이콘 + PDC 태그)이어야 하므로 별도 처리한다.
     */
    private ItemStack buildResultItem(String recipeName, Material resultMaterial, ConfigurationSection section) {
        ItemStack item = new ItemStack(resultMaterial);

        if (recipeName.equalsIgnoreCase("heart")) {
            ItemMeta meta = item.getItemMeta();
            String displayName = section.getString("result-name", "&c하트");
            meta.setDisplayName(org.bukkit.ChatColor.translateAlternateColorCodes('&', displayName));
            meta.setLore(List.of("§7우클릭하여 하트 1개를 획득합니다."));
            meta.getPersistentDataContainer().set(HEART_ITEM_KEY, PersistentDataType.BYTE, (byte) 1);
            item.setItemMeta(meta);
        }

        return item;
    }

    public static boolean isHeartItem(ItemStack item) {
        if (item == null || item.getItemMeta() == null) return false;
        return item.getItemMeta().getPersistentDataContainer().has(HEART_ITEM_KEY, PersistentDataType.BYTE);
    }

    // ---------------- 인게임 커맨드로 레시피 재료 변경 (런타임 임시 변경, 영구 반영은 config.yml 직접수정) ----------------

    public boolean addIngredientToRecipe(String recipeName, char slot, Material material) {
        NamespacedKey key = new NamespacedKey(plugin, recipeName);
        var existing = Bukkit.getRecipe(key);
        if (!(existing instanceof ShapedRecipe shaped)) {
            return false;
        }

        Bukkit.removeRecipe(key);
        ShapedRecipe updated = new ShapedRecipe(key, shaped.getResult());
        updated.shape(shaped.getShape());

        Map<Character, ItemStack> ingredients = shaped.getIngredientMap();
        for (Map.Entry<Character, ItemStack> entry : ingredients.entrySet()) {
            ItemStack ingredient = entry.getValue();
            if (ingredient != null) {
                updated.setIngredient(entry.getKey(), ingredient.getType());
            }
        }
        updated.setIngredient(slot, material);
        Bukkit.addRecipe(updated);
        return true;
    }

    public Set<String> getRegisteredRecipeNames() {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("lifesteal.recipes");
        return section != null ? section.getKeys(false) : Set.of();
    }
}
