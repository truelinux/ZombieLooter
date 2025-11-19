package com.zombielooter;

import cn.nukkit.Player;
import cn.nukkit.command.PluginCommand;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.level.ChunkLoadEvent;
import cn.nukkit.event.level.ChunkUnloadEvent;
import cn.nukkit.plugin.PluginBase;
import cn.nukkit.registry.Registries;
import cn.nukkit.registry.RegisterException;

import cn.nukkit.utils.DummyBossBar;
import com.zombielooter.boss.BossCommand;
import com.zombielooter.boss.BossEventManager;
import com.zombielooter.commands.*;
import com.zombielooter.economy.EconomyCommand;
import com.zombielooter.economy.EconomyManager;
import com.zombielooter.economy.VendorManager;
import com.zombielooter.events.GlobalEventManager;
import com.zombielooter.events.InfectionManager;
import com.zombielooter.events.PlayerDeathListener;
import com.zombielooter.events.PlayerEvents;
import com.zombielooter.factions.*;
import com.zombielooter.gui.GUIFormListener;
import com.zombielooter.gui.GUITextManager;
import com.zombielooter.market.MarketCommand;
import com.zombielooter.market.MarketManager;
import com.zombielooter.mail.MailCommand;
import com.zombielooter.mail.MailManager;
import com.zombielooter.npc.NPCListener;
import com.zombielooter.npc.NPCManager;
import com.zombielooter.npc.VendorNPC;
import com.zombielooter.placeholder.ZombielooterPlaceholderExtension;
import com.zombielooter.quests.QuestCommand;
import com.zombielooter.quests.QuestManager;
import com.zombielooter.rewards.DailyRewardManager;
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
import com.zombielooter.kitpvp.KitPvpManager;
import com.zombielooter.kitpvp.KitPvpListener;
import me.skh6075.pnx.graphicscore.placeholder.PlaceholderAPI;

import cn.nukkit.utils.TextFormat;
import cn.nukkit.scheduler.Task;

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
    private ClaimManager claimManager;
    private QuestManager questManager;
    private BossEventManager bossEventManager;
    private GlobalEventManager globalEventManager;
    private InfectionManager infectionManager;
    private MarketManager marketManager;
    private HUDManager hudManager;
    private VendorManager vendorManager;
    private NPCManager npcManager;
    private PowerManager powerManager;
    private RaidManager raidManager;
    private TerritoryBuffManager territoryBuffManager;

    // Additional systems
    private GUITextManager guiTextManager;
    private UIManager uiManager;
    private XPManager xpManager;
    private WorldEventManager worldEventManager;
    private KillStreakManager killStreakManager;
    private LeaderboardManager leaderboardManager;
    private KitPvpManager kitPvpManager;
    private DailyRewardManager dailyRewardManager;
    private MailManager mailManager;

    private int marqueeTaskId = -1;

    // =======================
    //     BOSSBAR MARQUEE
    // =======================
    private String marqueeText = "";
    private int bossbarMarqueeTaskId = -1;


    @Override
    public void onLoad() {
        try {
            Registries.ENTITY.registerCustomEntity(this, VendorNPC.class);
        } catch (RegisterException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onEnable() {
        instance = this;
        getLogger().info("🧟 ZombieLooterX enabling...");
        getDataFolder().mkdirs();

        try {
            uiManager        = new UIManager(this);
            guiTextManager   = new GUITextManager(this);
            hudManager       = new HUDManager();

            lootManager      = new LootManager(this);
            zombieSpawner    = new ZombieSpawner(this, lootManager);
            zoneManager      = new ZoneManager(this);

            xpManager        = new XPManager(this);
            economyManager   = new EconomyManager(this);
            mailManager      = new MailManager(this);
            vendorManager    = new VendorManager(this, economyManager);
            npcManager       = new NPCManager(this);
            powerManager     = new PowerManager(this);
            raidManager      = new RaidManager(this);
            territoryBuffManager = new TerritoryBuffManager(this);
            factionManager   = new FactionManager(this);
            claimManager     = new ClaimManager(this);
            questManager     = new QuestManager(this);
            marketManager    = new MarketManager(this, mailManager, economyManager);

            bossEventManager     = new BossEventManager(this);
            infectionManager     = new InfectionManager(this);
            globalEventManager   = new GlobalEventManager(this);
            worldEventManager    = new WorldEventManager(this);
            killStreakManager    = new KillStreakManager(this);
            leaderboardManager   = new LeaderboardManager(this);
            kitPvpManager        = new KitPvpManager(this);
            dailyRewardManager   = new DailyRewardManager(this);

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
        getServer().getPluginManager().registerEvents(new PlayerEvents(this), this);
        getServer().getPluginManager().registerEvents(new PlayerDeathListener(this), this);
        getServer().getPluginManager().registerEvents(new NPCListener(this, npcManager), this);
        getServer().getPluginManager().registerEvents(new RaidListener(this), this);
        getServer().getPluginManager().registerEvents(new KitPvpListener(kitPvpManager), this);

        // ---- Register commands via setExecutor() ----
        tryRegisterCommand("zlx",     new ZombieCommand(this));
        tryRegisterCommand("lootpreview", new LootPreviewCommand(this, lootManager));
        tryRegisterCommand("f",       new FactionCommand(this));
        tryRegisterCommand("zmarket", new MarketCommand(this));
        tryRegisterCommand("mail",    new MailCommand(this, mailManager));
        tryRegisterCommand("quest",   new QuestCommand(this));
        tryRegisterCommand("boss",    new BossCommand(this));
        tryRegisterCommand("economy", new EconomyCommand(this));
        tryRegisterCommand("vendor",  new VendorCommand(this));
        tryRegisterCommand("kitpvp",  new com.zombielooter.kitpvp.KitPvpCommand(kitPvpManager));
        tryRegisterCommand("dailyrewards", new DailyRewardsCommand(dailyRewardManager));

        // ---- Save default resources if missing ----
        saveResource("config.yml", false);
        saveResource("gui_text.yml", false);
        saveResource("zones.yml", false);
        saveResource("loot.yml", false);
        saveResource("quests.yml", false);
        saveResource("bosses.yml", false);
        saveResource("events.yml", false);
        saveResource("xp.yml", false);
        saveResource("market.yml", false);
        saveResource("factions.yml", false);
        saveResource("claims.yml", false);
        saveResource("economy.yml", false);
        saveResource("power.yml", false);
        saveResource("raid.yml", false);
        saveResource("vendors.yml", false);
        saveResource("territory_buffs.yml", false);
        saveResource("kitpvp.yml", false);
        saveResource("daily_rewards.yml", false);
        saveResource("mail.yml", false);

        PlaceholderAPI.INSTANCE.register(new ZombielooterPlaceholderExtension());

        // Start existing hotbar stats popup
        startHotbarMarquee();

        // Start new bossbar marquee
        startBossBarMarquee();

        getLogger().info("✅ ZombieLooterX enabled.");
    }

    // ======================================================
    //               BOSSBAR MARQUEE LOGIC
    // ======================================================

    private void startBossBarMarquee() {
        stopBossBarMarquee();
        String raw = getConfig().getString("marquee", "&aZombie &bLooter &cServer");
        scrollingBuffer = TextFormat.colorize('&', raw) + "     ";


        bossbarMarqueeTaskId = getServer().getScheduler().scheduleRepeatingTask(this, new Task() {
            @Override
            public void onRun(int tick) {
                rotateBuffer();
                String frame = buildMarqueeFrame();
                for (Player p : getServer().getOnlinePlayers().values()) {
                    for(DummyBossBar bar : p.getDummyBossBars().values()) {
                        p.updateBossBar(frame, 100, bar.getBossBarId());
                    }
                }
            }
        }, 3).getTaskId();
    }

    private void stopBossBarMarquee() {
        if (bossbarMarqueeTaskId != -1) {
            getServer().getScheduler().cancelTask(bossbarMarqueeTaskId);
            bossbarMarqueeTaskId = -1;
        }
        for (Player p : getServer().getOnlinePlayers().values()) {
            DummyBossBar bossBar = p.getDummyBossBar(0);
            bossBar.destroy();
        }
    }

    private String scrollingBuffer;

    private void rotateBuffer() {
        if (scrollingBuffer.startsWith("§") && scrollingBuffer.length() > 2) {
            scrollingBuffer = scrollingBuffer.substring(2) + scrollingBuffer.substring(0, 2);
        } else {
            scrollingBuffer = scrollingBuffer.substring(1) + scrollingBuffer.charAt(0);
        }
    }


    private String buildMarqueeFrame() {
        String active = getActiveColors(scrollingBuffer);
        return active + scrollingBuffer;
    }

    private String getActiveColors(String text) {
        String active = "";
        String lastColor = "";

        for (int i = 0; i < text.length() - 1; i++) {
            if (text.charAt(i) == '§') {
                String code = text.substring(i, i + 2);
                char type = code.charAt(1);

                // Color code resets formatting
                if ("0123456789abcdef".indexOf(type) != -1) {
                    lastColor = code;
                    active = code;  // reset to this color
                }
                else if ("lnokm".indexOf(type) != -1) {
                    active += code; // stacked formatting
                }
                else if (type == 'r') {
                    active = "";
                }
                i++;
            }
        }

        return active;
    }


    /**
     * Tracks persistent formatting:
     * - Last color wins
     * - Formatting codes (§l, §n, §o, §k) stack
     */
    private String updateActive(String existing, String code) {
        char type = code.charAt(1);

        // COLOR CODES replace everything
        if ((type >= '0' && type <= '9') || (type >= 'a' && type <= 'f')) {
            return code;
        }

        // FORMATTING CODES stack AFTER color
        if ("lnokm".contains(String.valueOf(type))) {
            return existing + code;
        }

        // Reset wipe all
        if (type == 'r') {
            return "";
        }

        return existing;
    }


    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        npcManager.onChunkLoad(event.getChunk());
    }

    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent event) {
        npcManager.onChunkUnload(event.getChunk());
    }

    @Override
    public void onDisable() {
        getLogger().info("💀 ZombieLooterX disabling...");

        stopHotbarMarquee();
        stopBossBarMarquee();

        if (questManager != null) questManager.saveProgress();
        if (factionManager != null) factionManager.save();
        if (claimManager != null) claimManager.save();
        if (marketManager != null) marketManager.save();
        if (xpManager != null) xpManager.save();
        if (economyManager != null) economyManager.save();
        if (mailManager != null) mailManager.save();

        getLogger().info("💀 ZombieLooterX disabled.");
    }

    // ======================================================
    //                EXISTING ACTIONBAR POPUP
    // ======================================================

    private void startHotbarMarquee() {
        stopHotbarMarquee();

        marqueeTaskId = getServer().getScheduler().scheduleRepeatingTask(this, new cn.nukkit.scheduler.Task() {

            @Override
            public void onRun(int currentTick) {
                int playerCount = getServer().getOnlinePlayers().size();
                int listingCount = marketManager != null ? marketManager.getListings().size() : 0;
                int infectionLevel = infectionManager != null ? infectionManager.getInfectionLevel() : 0;

                String message = "§l§k==§r§l§fPlayers \ue130: §a" + playerCount
                        + " §7| §fListings: §b" + listingCount
                        + " §7| §fInfection: §c" + infectionLevel + "%";

                for (Player player : getServer().getOnlinePlayers().values()) {
                    player.sendActionBar(message);
                }
            }
        }, 1).getTaskId();
    }

    private void stopHotbarMarquee() {
        if (marqueeTaskId != -1) {
            getServer().getScheduler().cancelTask(marqueeTaskId);
            marqueeTaskId = -1;
        }
    }

    // ======================================================
    //  SAFE COMMAND REGISTRATION
    // ======================================================

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
    public ClaimManager getClaimManager()             { return claimManager; }
    public QuestManager getQuestManager()             { return questManager; }
    public BossEventManager getBossEventManager()     { return bossEventManager; }
    public GlobalEventManager getGlobalEventManager() { return globalEventManager; }
    public InfectionManager getInfectionManager()     { return infectionManager; }
    public MarketManager getMarketManager()           { return marketManager; }
    public MailManager getMailManager()               { return mailManager; }
    public HUDManager getHudManager()                 { return hudManager; }
    public VendorManager getVendorManager()           { return vendorManager; }
    public NPCManager getNpcManager()                 { return npcManager; }
    public PowerManager getPowerManager()             { return powerManager; }
    public RaidManager getRaidManager()               { return raidManager; }
    public TerritoryBuffManager getTerritoryBuffManager() { return territoryBuffManager; }

    // Getters for newly added managers
    public UIManager getUIManager()                  { return uiManager; }
    public XPManager getXPManager()                  { return xpManager; }
    public WorldEventManager getWorldEventManager()  { return worldEventManager; }
    public KillStreakManager getKillStreakManager()  { return killStreakManager; }
    public LeaderboardManager getLeaderboardManager(){ return leaderboardManager; }
    public KitPvpManager getKitPvpManager()          { return kitPvpManager; }
    public DailyRewardManager getDailyRewardManager(){ return dailyRewardManager; }

    public GUITextManager getGUITextManager() {return guiTextManager;}
}
