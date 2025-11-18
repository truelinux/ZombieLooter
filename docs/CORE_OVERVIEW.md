# ZombieLooterX Core Overview

## High-level Loop & Player Flow
- **Startup**: `ZombieLooterX` wires all feature managers (loot, quests, bosses, factions, kit PvP, market, economy, zones, raids, vendors, world events) and registers listeners/commands before showing players scrolling action bar + boss bar marquees. 【F:src/main/java/com/zombielooter/ZombieLooterX.java†L34-L148】
- **Join**: Players receive a boss bar on login and are tracked by global listeners for anti-cheat/exploit, PvP zone rules, NPC interactions, quest kills, infection level, and loot drops. 【F:src/main/java/com/zombielooter/events/PlayerEvents.java†L21-L35】【F:src/main/java/com/zombielooter/zones/PvPListener.java†L1-L120】【F:src/main/java/com/zombielooter/LootManager.java†L22-L106】
- **Progression**: Quests advance on zombie kills with rewards paid through the economy manager; XP, kill-streaks, power/raid, and leaderboard systems track ongoing performance. 【F:src/main/java/com/zombielooter/quests/QuestManager.java†L1-L121】【F:src/main/java/com/zombielooter/pvp/KillStreakManager.java†L1-L120】
- **Economy & Trading**: The economy manager stores balances in `economy.yml`; vendor/market commands let players trade while faction balances back raids and claims. 【F:src/main/java/com/zombielooter/economy/EconomyManager.java†L1-L76】【F:src/main/java/com/zombielooter/market/MarketManager.java†L1-L200】
- **Looping Events**: Boss events, infections, and world/global events run via scheduled managers, keeping the world dynamic. 【F:src/main/java/com/zombielooter/boss/BossEventManager.java†L1-L160】【F:src/main/java/com/zombielooter/events/GlobalEventManager.java†L1-L140】
- **Shutdown**: Core managers persist state (quests, factions, claims, market, XP, economy) before disabling marquees to avoid data loss. 【F:src/main/java/com/zombielooter/ZombieLooterX.java†L214-L244】

## Key Systems Snapshot
- **Loot**: Config-driven weighted drops with custom names, lore, enchantments, and NBT when entities die. 【F:src/main/java/com/zombielooter/LootManager.java†L32-L170】
- **Quests**: YAML-defined objectives (primarily kill counts) with async persistence to `quest_progress.yml` and periodic autosave. 【F:src/main/java/com/zombielooter/quests/QuestManager.java†L34-L121】【F:src/main/java/com/zombielooter/quests/QuestManager.java†L121-L205】
- **Factions & Territory**: Claim, power, raid, and territory buff managers coordinate faction state and raids. 【F:src/main/java/com/zombielooter/ZombieLooterX.java†L50-L87】【F:src/main/resources/raid.yml†L1-L80】
- **Kit PvP**: Dedicated kit PvP manager/listener/command provide arena progression separate from the main survival loop. 【F:src/main/java/com/zombielooter/kitpvp/KitPvpManager.java†L1-L200】【F:src/main/java/com/zombielooter/kitpvp/KitPvpListener.java†L1-L180】
- **UI Layer**: GUI text, HUD, and menu managers power in-game forms/menus for factions, vendors, bosses, and markets. 【F:src/main/java/com/zombielooter/gui/GUITextManager.java†L1-L140】【F:src/main/java/com/zombielooter/gui/UIManager.java†L1-L200】

## Immediate Improvement Opportunities
1) **Reduce disk churn in economy saves**: `EconomyManager` writes the config file on every balance change; batching saves (tick task or dirty flag) would lower I/O without risking loss because a flush helper already exists. 【F:src/main/java/com/zombielooter/economy/EconomyManager.java†L39-L75】
2) **Quest persistence threading**: Quest progress loading/saving runs on async threads while sharing a single `Config` instance; wrap access or isolate per-thread configs to avoid concurrent writes from autosave and manual triggers. 【F:src/main/java/com/zombielooter/quests/QuestManager.java†L74-L121】【F:src/main/java/com/zombielooter/quests/QuestManager.java†L121-L205】
3) **Join logging noise**: `PlayerEvents` logs every `ContainerOpenPacket`, which will spam logs in normal play; gate behind debug config or remove for production. 【F:src/main/java/com/zombielooter/events/PlayerEvents.java†L18-L27】
4) **Boss bar lifecycle**: Marquee task recreates/updates bars each tick but boss bars are only destroyed on plugin disable; cleaning up on player quit avoids orphaned bars. 【F:src/main/java/com/zombielooter/ZombieLooterX.java†L161-L213】【F:src/main/java/com/zombielooter/events/PlayerEvents.java†L21-L35】
5) **Data validation**: Several loaders swallow malformed data (e.g., economy UUID parse, quest objectives) without surfacing actionable warnings; emit structured warnings per key to simplify live debugging. 【F:src/main/java/com/zombielooter/economy/EconomyManager.java†L23-L41】【F:src/main/java/com/zombielooter/quests/QuestManager.java†L52-L101】

