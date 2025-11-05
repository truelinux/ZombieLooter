package com.zombielooter;

import cn.nukkit.command.PluginCommand;
import cn.nukkit.event.Listener;
import cn.nukkit.plugin.PluginBase;

import com.zombielooter.boss.BossEventManager;
import com.zombielooter.commands.*;
import com.zombielooter.economy.EconomyManager;
import com.zombielooter.events.GlobalEventManager;
import com.zombielooter.events.InfectionManager;
import com.zombielooter.factions.ClaimManager;
import com.zombielooter.factions.FactionManager;
import com.zombielooter.gui.GUIFormListener;
import com.zombielooter.gui.GUITextManager;
import com.zombielooter.market.MarketManager;
import com.zombielooter.placeholder.ZombielooterPlaceholderExtension;
import com.zombielooter.quests.QuestManager;
import com.zombielooter.security.AntiCheatBasic;
import com.zombielooter.security.AntiExploitListener;
import com.zombielooter.gui.HUDManager;
import com.zombielooter.gui.UIManager;
import com.zombielooter.xp.XPManager;
import com.zombielooter.events.WorldEventManager;
import com.zombielooter.pvp.KillStreakManager;
import com.zombielooter.leaderboard.LeaderboardManager;
import com.zombielooter.zones.PvPListener;
import com.zombielooter.zones.ZoneManager;
import me.skh6075.pnx.graphicscore.placeholder.PlaceholderAPI;

public class ZombieLooterX extends PluginBase implements Listener {

    public static ZombieLooterX instance;
    public static String PREFIX = "ZOMBIELOOTER.NET";

    // Core gameplay managers
    private LootManager lootManager;
    private ZombieSpawner zombieSpawner;
    private ZoneManager zoneManager;

    // Systems / features
    private EconomyManager economyManager;
    private FactionManager factionManager;
    private ClaimManager claimManager;          // ✅ added back (chunk/claim handling)
    private QuestManager questManager;
    private BossEventManager bossEventManager;
    private GlobalEventManager globalEventManager;
    private InfectionManager infectionManager;
    private MarketManager marketManager;
    private HUDManager hudManager;

    // Additional systems added for a more immersive experience
    private GUITextManager guiTextManager;
    private UIManager uiManager;
    private XPManager xpManager;
    private WorldEventManager worldEventManager;
    private KillStreakManager killStreakManager;
    private LeaderboardManager leaderboardManager;

    @Override
    public void onEnable() {
        instance = this;
        getLogger().info("🧟 ZombieLooterX enabling...");
        getDataFolder().mkdirs();

    try {
        // ---- Initialize managers (order matters where there are deps) ----
        uiManager = new UIManager(this);
        worldEventManager = new WorldEventManager(this);
        killStreakManager = new KillStreakManager(this);
        leaderboardManager = new LeaderboardManager(this);
        guiTextManager     = new GUITextManager(this);
        lootManager        = new LootManager(this);
        zombieSpawner      = new ZombieSpawner(this, lootManager);
        zoneManager        = new ZoneManager(this);
        xpManager          = new XPManager(this);


        economyManager     = new EconomyManager(this);
        factionManager     = new FactionManager(this);
        claimManager       = new ClaimManager(this);
        questManager = new QuestManager(this);


    } catch (Exception e) {
        getLogger().error("Failed to initialize ZombieLooterX!", e);
        getServer().getPluginManager().disablePlugin(this);
        return;
    }


        // ---- Register listeners ----
        getServer().getPluginManager().registerEvents(this, this);
        getServer().getPluginManager().registerEvents(new GUIFormListener(this), this);
        getServer().getPluginManager().registerEvents(zombieSpawner, this);
        getServer().getPluginManager().registerEvents(new PvPListener(this, zoneManager), this);
        getServer().getPluginManager().registerEvents(new AntiCheatBasic(this), this);
        getServer().getPluginManager().registerEvents(new AntiExploitListener(this, getZoneManager()), this);

        // ---- Register commands via setExecutor() ----
        tryRegisterCommand("zlx",     new ZombieCommand(this));
        tryRegisterCommand("f",       new FactionCommand(this));
        tryRegisterCommand("zmarket", new MarketCommand(this));
        tryRegisterCommand("quest",   new QuestCommand(this));
        tryRegisterCommand("boss",    new BossCommand(this));
        tryRegisterCommand("economy", new EconomyCommand(this));

        // ---- Save default resources if missing ----
        saveResource("config.yml", false);
        saveResource("gui_text.yml", false);
        saveResource("zones.yml", false);
        saveResource("loot.yml", false);
        saveResource("quests.yml", false);
        saveResource("bosses.yml", false);
        saveResource("market.yml", false);
        saveResource("factions.yml", false);
        saveResource("claims.yml", false);
        saveResource("economy.yml", false);
        saveResource("power.yml", false);
        saveResource("raid.yml", false);


        PlaceholderAPI.INSTANCE.register(new ZombielooterPlaceholderExtension());

        getLogger().info("✅ ZombieLooterX enabled. Commands: /zlx, /f, /zmarket, /quest, /boss, /economy");
    }

    @Override
    public void onDisable() {
        getLogger().info("💀 ZombieLooterX disabling...");
    
    // Save all data before shutdown
    if (questManager != null) {
        questManager.saveProgress();
        getLogger().info("✅ Saved quest progress");
    }
    
    if (factionManager != null) {
        factionManager.save();
        getLogger().info("✅ Saved factions");
    }
    
    if (xpManager != null) {
        xpManager.save();
        getLogger().info("✅ Saved XP data");
    }
    
    getLogger().info("💀 ZombieLooterX disabled.");
    }

    // Helper to wire commands safely
    private void tryRegisterCommand(String name, Object executor) {
        @SuppressWarnings("unchecked")
        PluginCommand<ZombieLooterX> cmd = (PluginCommand<ZombieLooterX>) getCommand(name);
        if (cmd != null) {
            if (executor instanceof cn.nukkit.command.CommandExecutor) {
                cmd.setExecutor((cn.nukkit.command.CommandExecutor) executor);
            } else {
                getLogger().warning("⚠ Command " + name + " executor is not a CommandExecutor.");
            }
        } else {
            getLogger().warning("⚠ Command '/" + name + "' not found in plugin.yml. Please verify.");
        }
    }

    // ---- Getters for other classes/commands to access managers ----
    public LootManager getLootManager()               { return lootManager; }
    public ZombieSpawner getZombieSpawner()           { return zombieSpawner; }
    public ZoneManager getZoneManager()               { return zoneManager; }
    public EconomyManager getEconomyManager()         { return economyManager; }
    public FactionManager getFactionManager()         { return factionManager; }
    public ClaimManager getClaimManager()             { return claimManager; }     // ✅ added getter
    public QuestManager getQuestManager()             { return questManager; }
    public BossEventManager getBossEventManager()     { return bossEventManager; }
    public GlobalEventManager getGlobalEventManager() { return globalEventManager; }
    public InfectionManager getInfectionManager()     { return infectionManager; }
    public MarketManager getMarketManager()           { return marketManager; }
    public HUDManager getHudManager()                 { return hudManager; }

    // Getters for newly added managers
    public UIManager getUIManager()                  { return uiManager; }
    public XPManager getXPManager()                  { return xpManager; }
    public WorldEventManager getWorldEventManager()  { return worldEventManager; }
    public KillStreakManager getKillStreakManager()  { return killStreakManager; }
    public LeaderboardManager getLeaderboardManager(){ return leaderboardManager; }

    public GUITextManager getGUITextManager() {return guiTextManager;}
}
