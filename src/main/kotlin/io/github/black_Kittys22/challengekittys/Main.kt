package io.github.black_Kittys22.challengekittys

import io.github.black_Kittys22.challengekittys.AllItems.AllItemsListener
import io.github.black_Kittys22.challengekittys.AllMobs.AllMobsListener
import io.github.black_Kittys22.challengekittys.Challenges.BedrockChallenge
import io.github.black_Kittys22.challengekittys.Challenges.CraftingRandomizer
import io.github.black_Kittys22.challengekittys.Challenges.HalfHeartChallenge
import io.github.black_Kittys22.challengekittys.Challenges.InfiniteLoopChallenge
import io.github.black_Kittys22.challengekittys.Challenges.MobDropChallenge
import io.github.black_Kittys22.challengekittys.Challenges.MobRandomizerChallenge
import io.github.black_Kittys22.challengekittys.Challenges.RandomizerChallenge
import io.github.black_Kittys22.challengekittys.Challenges.SharedInvCommand
import io.github.black_Kittys22.challengekittys.Timer.Timer
import io.github.black_Kittys22.challengekittys.Timer.TimerCommand
import io.github.black_Kittys22.challengekittys.Timer.TimerListener
import io.github.black_Kittys22.challengekittys.ChunkChallenge.*
import io.github.black_Kittys22.challengekittys.Commands.BCCommand
import io.github.black_Kittys22.challengekittys.Commands.BackpackCommand
import io.github.black_Kittys22.challengekittys.Commands.ExemptCommand
import io.github.black_Kittys22.challengekittys.MonsterBattle.*
import io.github.black_Kittys22.challengekittys.Commands.LBCommand
import io.github.black_Kittys22.challengekittys.Commands.SettingsCommand
import io.github.black_Kittys22.challengekittys.Commands.SpectatorManager
import io.github.black_Kittys22.challengekittys.DamageInvClear.DamageListener
import io.github.black_Kittys22.challengekittys.LuegenBattle.BattleProtectionListener
import io.github.black_Kittys22.challengekittys.LuegenBattle.StructureBattleManager
import io.github.black_Kittys22.challengekittys.LuegenBattle.StructureDeathListener
import io.github.black_Kittys22.challengekittys.SharedInventoryChallenge.SharedInvListener
import io.github.black_Kittys22.challengekittys.AllAchievments.AllAchievments
import io.github.black_Kittys22.challengekittys.ChainedTogether.ChainedTogetherChallenge
import io.github.black_Kittys22.challengekittys.Challenges.SwapKeysChallenge
import io.github.black_Kittys22.challengekittys.Commands.WarnCommand
import io.github.black_Kittys22.challengekittys.MobForceBattle.MobForceBattleCommand
import io.github.black_Kittys22.challengekittys.MobForceBattle.MobForceBattleListener
import io.github.black_Kittys22.challengekittys.MobForceBattle.MobForceBattleManager
import io.github.black_Kittys22.challengekittys.MobForceBattle.MobForceRankingGUI
import io.github.black_Kittys22.challengekittys.Timer.TimerColorGUI
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.*
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.util.*

class Main : JavaPlugin(), Listener {

    lateinit var timer: Timer
    lateinit var mobDropChallenge: MobDropChallenge
    var isMobDropChallengeActive = false
    lateinit var manager: ChallengeManager
    lateinit var structureBattleManager: StructureBattleManager
    lateinit var allItemsListener: AllItemsListener
    lateinit var craftingRandomizer: CraftingRandomizer
    var isCraftingRandomizerActive = false
    lateinit var monsterBattleChallenge: MonsterBattleChallenge
    lateinit var swapKeysChallenge: SwapKeysChallenge
    var isSwapKeysChallengeActive = false
    lateinit var allMobsListener: AllMobsListener
    var isAllMobsChallengeActive = false
    lateinit var allAchievments: AllAchievments
    var isSharedAdvancementsActive = false
    lateinit var infiniteLoopChallenge: InfiniteLoopChallenge
    var isInfiniteLoopActive = false
    lateinit var chainedTogetherChallenge: ChainedTogetherChallenge
    var isChainedTogetherActive = false
    lateinit var randomizerChallenge: RandomizerChallenge
    var isRandomizerActive = false
    lateinit var backpackInventory: Inventory
    var isTimerAutoStartEnabled = true
    var isChunkChallengeSelected = false
    val exemptPlayers = mutableSetOf<UUID>()
    var isDamageClearInventoryActive = false
    var isDeadSyncActive = false
    val timerColorGUI = TimerColorGUI(this)
    var isSharedInventoryActive = false
    lateinit var mobRandomizerChallenge: MobRandomizerChallenge
    var isMobRandomizerActive = false
    var isKeepInventoryActive = false
    var isAllItemsChallengeActive = false
    val sharedInvGroups = mutableMapOf<UUID, String>()
    val blacklistedMaterials = mutableListOf<Material>()
    val transformedChunks = mutableSetOf<String>()
    val chunkEntityMap = mutableMapOf<String, UUID>()
    val playerActiveChunk = mutableMapOf<UUID, String>()
    var isHalfHeartChallengeActive = false
    lateinit var halfHeartChallenge: HalfHeartChallenge
    lateinit var bedrockChallenge: BedrockChallenge
    lateinit var mobForceBattleManager: MobForceBattleManager

    @EventHandler
    fun onJoin(event: PlayerJoinEvent) {
        updateTablist()
    }

    override fun onEnable() {
        saveDefaultConfig()

        mobDropChallenge = MobDropChallenge(this)
        isMobDropChallengeActive = config.getBoolean("challenges.mobDrop.active", false)
        server.pluginManager.registerEvents(mobDropChallenge, this)

        server.pluginManager.registerEvents(timerColorGUI, this)

        craftingRandomizer = CraftingRandomizer(this)
        isCraftingRandomizerActive = config.getBoolean("challenges.craftingRandomizer.active", false)
        craftingRandomizer.loadMappings()
        server.pluginManager.registerEvents(craftingRandomizer, this)

        swapKeysChallenge = SwapKeysChallenge(this)
        isSwapKeysChallengeActive = config.getBoolean("challenges.swapKeys.active", false)
        server.pluginManager.registerEvents(swapKeysChallenge, this)
        if (isSwapKeysChallengeActive) swapKeysChallenge.enable()
        mobRandomizerChallenge = MobRandomizerChallenge(this)
        isMobRandomizerActive = config.getBoolean("challenges.mobRandomizer.active", false)
        mobRandomizerChallenge.loadMappings()
        server.pluginManager.registerEvents(mobRandomizerChallenge, this)
        bedrockChallenge = BedrockChallenge(this)
        timer = Timer(this)
        manager = ChallengeManager(this)
        structureBattleManager = StructureBattleManager(this)
        allItemsListener = AllItemsListener(this)
        monsterBattleChallenge = MonsterBattleChallenge(this)
        mobForceBattleManager = MobForceBattleManager(this)
        server.pluginManager.registerEvents(MobForceBattleListener(this), this)
        server.pluginManager.registerEvents(MobForceRankingGUI, this)
        halfHeartChallenge = HalfHeartChallenge(this)
        server.pluginManager.registerEvents(halfHeartChallenge, this)
        isHalfHeartChallengeActive = config.getBoolean("challenges.halfHeart.active", false)
        loadPluginConfig()
        infiniteLoopChallenge = InfiniteLoopChallenge(this)
        isInfiniteLoopActive = config.getBoolean("challenges.infiniteLoop.active", false)
        server.pluginManager.registerEvents(infiniteLoopChallenge, this)
        randomizerChallenge = RandomizerChallenge(this)
        isRandomizerActive = config.getBoolean("challenges.randomizer.active", false)
        server.pluginManager.registerEvents(randomizerChallenge, this)
        timer.timeSeconds = config.getInt("timer.time", 0)
        isTimerAutoStartEnabled = config.getBoolean("timer.autoStart", true)
        isAllItemsChallengeActive = config.getBoolean("challenges.allItems.active", false)
        backpackInventory = Bukkit.createInventory(null, 54, Component.text("Globales Backpack", NamedTextColor.GOLD))
        loadBackpack()
        allMobsListener = AllMobsListener(this)
        isAllMobsChallengeActive = config.getBoolean("challenges.allMobs.active", false)
        server.pluginManager.registerEvents(allMobsListener, this)
        chainedTogetherChallenge = ChainedTogetherChallenge(this)
        server.pluginManager.registerEvents(chainedTogetherChallenge, this)
        allAchievments = AllAchievments(this)
        isSharedAdvancementsActive = config.getBoolean("challenges.sharedAdvancements.active", false)
        server.pluginManager.registerEvents(allAchievments, this)

        val exemptList = config.getStringList("exemptPlayers")
        exemptList.forEach { uuidStr ->
            runCatching { exemptPlayers.add(UUID.fromString(uuidStr)) }
        }

        setupArenaWorld()
        registerCommands()
        registerListeners()

        logger.info("§a[ChallengeSystem] Geladen!")
    }

    override fun onDisable() {
        config.set("exemptPlayers", exemptPlayers.map { it.toString() })
        config.set("challenges.craftingRandomizer.active", isCraftingRandomizerActive)
        craftingRandomizer.saveMappings()
        config.set("challenges.halfHeart.active", isHalfHeartChallengeActive)
        config.set("timer.time", timer.timeSeconds)
        config.set("challenges.mobRandomizer.active", isMobRandomizerActive)
        mobRandomizerChallenge.saveMappings()
        config.set("challenges.mobDrop.active", isMobDropChallengeActive)
        config.set("timer.autoStart", isTimerAutoStartEnabled)
        config.set("challenges.allItems.active", isAllItemsChallengeActive)
        config.set("challenges.allMobs.active", isAllMobsChallengeActive)
        config.set("challenges.sharedAdvancements.active", isSharedAdvancementsActive)
        config.set("challenges.infiniteLoop.active", isInfiniteLoopActive)
        config.set("challenges.randomizer.active", isRandomizerActive)
        config.set("challenges.swapKeys.active", isSwapKeysChallengeActive)
        // Alle laufenden Loop-Tasks stoppen
        if (::infiniteLoopChallenge.isInitialized) infiniteLoopChallenge.stopAllTasks()

        if (::allMobsListener.isInitialized) allMobsListener.saveProgress()

        saveBackpack()

        if (::allItemsListener.isInitialized) allItemsListener.saveProgress()
        saveConfig()
    }

    fun updateTablist() {
        val header = Component.text("\n")
            .append(Component.text("★ CHALLENGES ★", NamedTextColor.GOLD, TextDecoration.BOLD))
            .append(Component.text("\n", NamedTextColor.GRAY))

        val footer = Component.text("\n", NamedTextColor.GRAY)
            .append(Component.text("Items: ", NamedTextColor.AQUA))
            .append(Component.text(allItemsListener.getProgressString(), NamedTextColor.WHITE))
            .append(Component.text("  |  ", NamedTextColor.DARK_GRAY))
            .append(Component.text("Mobs: ", NamedTextColor.RED))
            .append(Component.text(allMobsListener.getProgressString(), NamedTextColor.WHITE))
            .append(Component.text("\n", NamedTextColor.GRAY))

        for (player in Bukkit.getOnlinePlayers()) {
            player.sendPlayerListHeaderAndFooter(header, footer)
        }
    }

    private fun registerCommands() {
        val bpCmd = BackpackCommand(this)
        val specManager = SpectatorManager(this)
        getCommand("spec")?.setExecutor(specManager)
        server.pluginManager.registerEvents(specManager, this)
        getCommand("backpack")?.setExecutor(bpCmd)
        getCommand("warn")?.setExecutor(WarnCommand(this))
        getCommand("bp")?.setExecutor(bpCmd)
        val mfbCmd = MobForceBattleCommand(this)
        getCommand("mobforce")?.setExecutor(mfbCmd)
        getCommand("mobforce")?.tabCompleter = mfbCmd
        getCommand("settings")?.setExecutor(SettingsCommand(this))
        val chExec = org.bukkit.command.CommandExecutor { s, _, _, _ -> if (s is Player) manager.openChallengeGUI(s); true }
        getCommand("ch")?.setExecutor(chExec)
        getCommand("challenges")?.setExecutor(chExec)
        getCommand("lb")?.setExecutor(LBCommand(this))

        val tCmd = TimerCommand(this)
        getCommand("timer")?.setExecutor(tCmd)
        getCommand("timer")?.tabCompleter = tCmd

        val mbCmd = MonsterBattleCommand(this)
        getCommand("monsterbattle")?.setExecutor(mbCmd)
        getCommand("monsterbattle")?.tabCompleter = mbCmd
        getCommand("exempt")?.setExecutor(ExemptCommand(this))
        getCommand("exempt")?.tabCompleter = ExemptCommand(this)
        getCommand("reset")?.setExecutor(ResetCommand(this))
        getCommand("chunkblacklist")?.setExecutor(GuiCommand(this))
        getCommand("block")?.setExecutor(BlockCommand(this))
        getCommand("block")?.tabCompleter = BlockTabCompleter()
        getCommand("bc")?.setExecutor(BCCommand(this))

        val sInv = SharedInvCommand(this)
        getCommand("shareinv")?.setExecutor(sInv)
        getCommand("shareinv")?.tabCompleter = sInv

        getCommand("skipitem")?.setExecutor { sender, _, _, _ ->
            if (sender.hasPermission("challenge.skip")) {
                allItemsListener.selectNextItem()
                sender.sendMessage("§aNächstes Item gewählt.")
            }
            true
        }
    }

    private fun registerListeners() {
        val pm = server.pluginManager
        pm.registerEvents(manager, this)
        pm.registerEvents(TimerListener(this), this)
        pm.registerEvents(GuiListener(this), this)
        pm.registerEvents(ChunkListener(this), this)
        pm.registerEvents(DamageListener(this), this)
        pm.registerEvents(DeathListener(this), this)
        pm.registerEvents(SharedInvListener(this), this)
        pm.registerEvents(allItemsListener, this)
        pm.registerEvents(monsterBattleChallenge, this)
        pm.registerEvents(StructureDeathListener(this), this)
        pm.registerEvents(BattleProtectionListener(this), this)
        pm.registerEvents(EntityDeathListener(this), this)
        pm.registerEvents(PlayerDeathListener(this), this)
        pm.registerEvents(MobProtectionListener(this), this)
        pm.registerEvents(ProtectionListener(this), this)
        pm.registerEvents(this, this)
    }

    @EventHandler
    fun onPlayerJoin(e: PlayerJoinEvent) {
        if (isAllItemsChallengeActive) allItemsListener.showBar(e.player)
    }

    fun saveBackpack() {
        val file = File(dataFolder, "backpack.yml")
        val yaml = YamlConfiguration.loadConfiguration(file)
        yaml.set("items", backpackInventory.contents.filterNotNull())
        yaml.save(file)
    }

    fun loadBackpack() {
        val file = File(dataFolder, "backpack.yml")
        if (!file.exists()) return
        val yaml = YamlConfiguration.loadConfiguration(file)
        val items = yaml.getList("items") as? List<ItemStack> ?: return
        backpackInventory.contents = items.toTypedArray()
    }

    fun makeChunkKey(x: Int, z: Int): String = "$x,$z"

    fun isLocationInChunk(loc: Location, chunkKey: String): Boolean {
        return makeChunkKey(loc.chunk.x, loc.chunk.z) == chunkKey
    }

    fun resetPlayerBorder(player: Player) {
        player.worldBorder.size = 60000000.0
    }

    private fun setupArenaWorld() {
        val worldName = config.getString("monsterbattle.arena_world_name") ?: "arena"
        if (Bukkit.getWorld(worldName) == null) {
            Bukkit.createWorld(WorldCreator(worldName))
        }
    }

    fun getPlayerSymbol(player: Player): String {
        val file = File(dataFolder, "configplayer.yml")
        if (!file.exists()) return "👤"
        val playerConfig = YamlConfiguration.loadConfiguration(file)
        return playerConfig.getString("player-symbols.${player.uniqueId}") ?: "👤"
    }

    fun savePluginConfig() {
        config.set("blacklistedBlocks", blacklistedMaterials.map { it.name })
        saveConfig()
    }

    fun loadPluginConfig() {
        val savedBlocks = config.getStringList("blacklistedBlocks")
        blacklistedMaterials.clear()
        savedBlocks.forEach { name ->
            Material.getMaterial(name)?.let { blacklistedMaterials.add(it) }
        }
    }
}