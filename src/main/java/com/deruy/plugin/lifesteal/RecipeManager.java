package com.deruy.plugin.lifesteal;

import com.deruy.plugin.DeruyPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;

import java.util.Map;

/**
 * LifeSteal 커스텀 조합법 전담 관리자.
 *
 * pending #6  : 바닐라 기본 메이스(Mace) 레시피 제거
 * pending #7  : 바닐라 부활의 토템(Totem of Undying)을 새 레시피로 제작 가능하게 추가
 * pending #8  : 커스텀 메이스 조합법 추가 (+ 재료 커스터마이징은 /lifesteal recipe additem 커맨드로)
 * pending #11 : 기본 황금사과 조합법 대체
 */
public class RecipeManager {

    private final DeruyPlugin plugin;

    public RecipeManager(DeruyPlugin plugin) {
        this.plugin = plugin;
    }

    public void registerAll() {
        removeDefaultMaceRecipe();     // #6
        registerTotemRecipe();          // #7
        registerCustomMaceRecipe();     // #8
        registerCustomGoldenAppleRecipe(); // #11
    }

    // ---------------- #6 기본 메이스 레시피 제거 ----------------

    private void removeDefaultMaceRecipe() {
        NamespacedKey vanillaMace = NamespacedKey.minecraft("mace");
        boolean removed = Bukkit.removeRecipe(vanillaMace);
        plugin.getLogger().info("기본 메이스 레시피 제거: " + removed);
    }

    // ---------------- #7 부활의 토템 제작 레시피 ----------------

    /**
     * 별도의 커스텀 아이템이 아니라, 바닐라 Totem of Undying 자체를
     * 이 레시피로 제작할 수 있게 한다. (토템을 재료로 쓰지 않는 순수 제작 레시피)
     */
    private void registerTotemRecipe() {
        NamespacedKey key = new NamespacedKey(plugin, "totem_of_undying_recipe");
        ShapedRecipe recipe = new ShapedRecipe(key, new ItemStack(Material.TOTEM_OF_UNDYING));
        recipe.shape("DED", "ESE", "DED");
        recipe.setIngredient('D', Material.DIAMOND_BLOCK);
        recipe.setIngredient('E', Material.ENCHANTED_GOLDEN_APPLE);
        recipe.setIngredient('S', Material.NETHER_STAR);
        Bukkit.addRecipe(recipe);
    }

    // ---------------- #8 커스텀 메이스 레시피 ----------------

    private void registerCustomMaceRecipe() {
        NamespacedKey key = new NamespacedKey(plugin, "custom_mace_recipe");
        ShapedRecipe recipe = new ShapedRecipe(key, new ItemStack(Material.MACE));
        recipe.shape("HNH", " B ", " B ");
        recipe.setIngredient('H', Material.HEAVY_CORE);
        recipe.setIngredient('N', Material.NETHERITE_BLOCK);
        recipe.setIngredient('B', Material.BREEZE_ROD);
        Bukkit.addRecipe(recipe);
    }

    // ---------------- #11 커스텀 황금사과 레시피 ----------------

    private void registerCustomGoldenAppleRecipe() {
        NamespacedKey vanillaKey = NamespacedKey.minecraft("golden_apple");
        Bukkit.removeRecipe(vanillaKey);

        NamespacedKey key = new NamespacedKey(plugin, "custom_golden_apple_recipe");
        ShapedRecipe recipe = new ShapedRecipe(key, new ItemStack(Material.GOLDEN_APPLE));
        recipe.shape("GGG", "GAG", "GGG");
        recipe.setIngredient('G', Material.GOLD_INGOT);
        recipe.setIngredient('A', Material.APPLE);
        Bukkit.addRecipe(recipe);
    }

    // ---------------- #8 확장: 인게임 커맨드로 레시피 재료 변경 ----------------

    /**
     * 이미 등록된 커스텀 레시피의 특정 슬롯 재료를 변경한다.
     * 사용 가능한 레시피 이름: totem_of_undying_recipe, custom_mace_recipe, custom_golden_apple_recipe
     */
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
}
