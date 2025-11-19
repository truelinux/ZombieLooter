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
- Economy saves now batch updates with a dirty flag and periodic flush task, cutting disk churn while still forcing a full snapshot on plugin shutdown. `src/main/java/com/zombielooter/economy/EconomyManager.java`
- Quest progress access is synchronized around the shared `Config`; async load/save copies data with validation to avoid concurrent writes and corrupt counters. `src/main/java/com/zombielooter/quests/QuestManager.java`
- Container-open packet logging is gated behind `debug.log-container-open-packets` (default false) to keep production logs clean. `src/main/java/com/zombielooter/events/PlayerEvents.java` `src/main/resources/config.yml`
- Boss bars are destroyed on player quit to avoid orphaned marquee instances. `src/main/java/com/zombielooter/events/PlayerEvents.java`
- Economy and quest loaders emit warnings for malformed UUIDs/rewards/counts so bad data can be fixed quickly. `src/main/java/com/zombielooter/economy/EconomyManager.java` `src/main/java/com/zombielooter/quests/QuestManager.java`

