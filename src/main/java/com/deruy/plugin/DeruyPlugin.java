package com.deruy.plugin;

import com.deruy.plugin.bingo.BingoListener;
import com.deruy.plugin.bingo.BingoManager;
import com.deruy.plugin.bingo.commands.BingoCommand;
import com.deruy.plugin.bounty.BountyManager;
import com.deruy.plugin.bounty.commands.BountyCommand;
import com.deruy.plugin.combatzone.CombatZoneListener;
import com.deruy.plugin.dragon.DragonListener;
import com.deruy.plugin.events.EventManager;
import com.deruy.plugin.events.commands.DeruyEventCommand;
import com.deruy.plugin.events.commands.KothCommand;
import com.deruy.plugin.events.koth.KothManager;
import com.deruy.plugin.lifesteal.LifeStealManager;
import com.deruy.plugin.lifesteal.RecipeManager;
import com.deruy.plugin.lifesteal.commands.CombatCommand;
import com.deruy.plugin.lifesteal.commands.LifeStealCommand;
import com.deruy.plugin.lifesteal.listeners.DeathListener;
import com.deruy.plugin.lifesteal.listeners.EnchantRestrictionListener;
import com.deruy.plugin.lifesteal.listeners.EnderPearlListener;
import com.deruy.plugin.lifesteal.listeners.NetheriteRecipeBlockListener;
import com.deruy.plugin.lifesteal.listeners.RestrictionListener;
import com.deruy.plugin.lifesteal.listeners.TridentListener;
import com.deruy.plugin.locatorbar.LocatorBarManager;
import com.deruy.plugin.misc.MiscRuleListener;
import com.deruy.plugin.misc.commands.DeruyTimeCommand;
import com.deruy.plugin.roleeffect.RoleEffectScheduler;
import com.deruy.plugin.supplydrop.SupplyDropManager;
import com.deruy.plugin.supplydrop.SuperSupplyDropManager;
import com.deruy.plugin.supplydrop.commands.SupplyDropCommand;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class DeruyPlugin extends JavaPlugin {

    private LifeStealManager lifeStealManager;
    private RecipeManager recipeManager;
    private EventManager eventManager;
    private KothManager kothManager;
    private LocatorBarManager locatorBarManager;
    private BingoManager bingoManager;
    private SupplyDropManager supplyDropManager;
    private SuperSupplyDropManager superSupplyDropManager;
    private BountyManager bountyManager;
    private RoleEffectScheduler roleEffectScheduler;

    private boolean worldGuardPresent;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        worldGuardPresent = Bukkit.getPluginManager().getPlugin("WorldGuard") != null;

        // ---------------- 매니저 초기화 ----------------
        this.lifeStealManager = new LifeStealManager(this);
        this.recipeManager = new RecipeManager(this);
        this.eventManager = new EventManager();
        this.kothManager = new KothManager(this);
        this.locatorBarManager = new LocatorBarManager(this);
        this.bingoManager = new BingoManager(this);
        this.supplyDropManager = new SupplyDropManager(this);
        this.superSupplyDropManager = new SuperSupplyDropManager(this);
        this.bountyManager = new BountyManager(this);
        this.roleEffectScheduler = new RoleEffectScheduler(this);

        eventManager.register(kothManager);
        eventManager.register(locatorBarManager);
        eventManager.register(bingoManager);
        eventManager.register(supplyDropManager);
        eventManager.register(superSupplyDropManager);
        eventManager.register(bountyManager);

        // 커스텀 레시피 등록 (#6, #7, #8, #11)
        recipeManager.registerAll();

        // ---------------- 리스너 등록 ----------------
        var pm = getServer().getPluginManager();
        pm.registerEvents(new DeathListener(this), this);
        pm.registerEvents(new TridentListener(this), this);            // #4
        pm.registerEvents(new RestrictionListener(this), this);        // #5
        pm.registerEvents(new EnchantRestrictionListener(this), this); // #10
        pm.registerEvents(new NetheriteRecipeBlockListener(this), this); // #13
        pm.registerEvents(new EnderPearlListener(this), this);          // 엔더진주 금지
        pm.registerEvents(new DragonListener(this), this);             // 드래곤 체력/알
        pm.registerEvents(new MiscRuleListener(this), this);           // 토템/사망이펙트/화살
        pm.registerEvents(new BingoListener(this), this);

        if (worldGuardPresent) {
            pm.registerEvents(new CombatZoneListener(this), this);
            getLogger().info("WorldGuard 감지됨: 컴벳존 차단 기능 활성화.");
        } else {
            getLogger().warning("WorldGuard가 없어 컴벳존 차단 기능이 비활성화됩니다.");
        }

        // pending #1,#2,#3 (토템부활금지/엔더진주금지/XRay탐지) 은
        // 기존에 이런 제한 코드가 없으므로 별도 리스너 없이 "제한 없음" 상태 그대로 둠.
        // pending #9 (무제한 주민거래) 는 요청에 따라 구현하지 않음(삭제됨).

        // ---------------- 커맨드 등록 (실행기 + 탭완성) ----------------
        var lifeStealCmd = new LifeStealCommand(this);
        getCommand("lifesteal").setExecutor(lifeStealCmd);
        getCommand("lifesteal").setTabCompleter(lifeStealCmd);

        var eventCmd = new DeruyEventCommand(this);
        getCommand("devent").setExecutor(eventCmd);
        getCommand("devent").setTabCompleter(eventCmd);

        var kothCmd = new KothCommand(this);
        getCommand("koth").setExecutor(kothCmd);
        getCommand("koth").setTabCompleter(kothCmd);

        var bingoCmd = new BingoCommand(this);
        getCommand("bingo").setExecutor(bingoCmd);
        getCommand("bingo").setTabCompleter(bingoCmd);

        var bountyCmd = new BountyCommand(this);
        getCommand("bounty").setExecutor(bountyCmd);
        getCommand("bounty").setTabCompleter(bountyCmd);

        var supplyDropCmd = new SupplyDropCommand(this, false);
        getCommand("supplydrop").setExecutor(supplyDropCmd);
        getCommand("supplydrop").setTabCompleter(supplyDropCmd);

        var superSupplyDropCmd = new SupplyDropCommand(this, true);
        getCommand("supersupplydrop").setExecutor(superSupplyDropCmd);
        getCommand("supersupplydrop").setTabCompleter(superSupplyDropCmd);

        var combatCmd = new CombatCommand(this);
        getCommand("combat").setExecutor(combatCmd);
        getCommand("combat").setTabCompleter(combatCmd);

        getCommand("deruytime").setExecutor(new DeruyTimeCommand());

        // ---------------- 스케줄러 시작 ----------------
        roleEffectScheduler.start();

        getLogger().info("DeruyPlugin 활성화 완료.");
    }

    @Override
    public void onDisable() {
        if (kothManager != null && kothManager.isRunning()) kothManager.stop();
        if (locatorBarManager != null && locatorBarManager.isRunning()) locatorBarManager.stop();
        if (bingoManager != null && bingoManager.isRunning()) bingoManager.stop();
        if (supplyDropManager != null && supplyDropManager.isRunning()) supplyDropManager.stop();
        if (superSupplyDropManager != null && superSupplyDropManager.isRunning()) superSupplyDropManager.stop();
        if (bountyManager != null && bountyManager.isRunning()) bountyManager.stop();
        if (roleEffectScheduler != null) roleEffectScheduler.stop();
    }

    /**
     * LuckPerms 연동: 플레이어가 특정 role(그룹)에 속하는지 확인.
     * LuckPerms가 없으면 항상 false.
     */
    public boolean hasRole(Player player, String role) {
        if (Bukkit.getPluginManager().getPlugin("LuckPerms") == null) return false;
        try {
            LuckPerms api = LuckPermsProvider.get();
            User user = api.getUserManager().getUser(player.getUniqueId());
            if (user == null) return false;
            if (user.getPrimaryGroup().equalsIgnoreCase(role)) return true;
            return user.getNodes().stream()
                    .anyMatch(node -> node.getKey().equalsIgnoreCase("group." + role));
        } catch (IllegalStateException e) {
            return false; // LuckPerms 아직 준비 안 됨
        }
    }

    public boolean isWorldGuardPresent() {
        return worldGuardPresent;
    }

    public LifeStealManager getLifeStealManager() {
        return lifeStealManager;
    }

    public RecipeManager getRecipeManager() {
        return recipeManager;
    }

    public EventManager getEventManager() {
        return eventManager;
    }

    public KothManager getKothManager() {
        return kothManager;
    }

    public LocatorBarManager getLocatorBarManager() {
        return locatorBarManager;
    }

    public BingoManager getBingoManager() {
        return bingoManager;
    }

    public SupplyDropManager getSupplyDropManager() {
        return supplyDropManager;
    }

    public SuperSupplyDropManager getSuperSupplyDropManager() {
        return superSupplyDropManager;
    }

    public BountyManager getBountyManager() {
        return bountyManager;
    }

    public RoleEffectScheduler getRoleEffectScheduler() {
        return roleEffectScheduler;
    }
}
