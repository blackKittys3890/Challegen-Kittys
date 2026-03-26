package io.github.black_Kittys22.challengekittys.Challenges

import io.github.black_Kittys22.challengekittys.Main
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.EntityType
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.CreatureSpawnEvent
import java.io.File

class MobRandomizerChallenge(private val plugin: Main) : Listener {

    // Maximale Distanz zum Spieler, damit der Swap ausgeführt wird
    private val MAX_SPAWN_DISTANCE = 64.0

    // Bosse & gefährliche Mobs, die NIE als Ersatz gespawnt werden dürfen
    private val BOSS_BLACKLIST = setOf(
        EntityType.WITHER,
        EntityType.ELDER_GUARDIAN,
        EntityType.ENDER_DRAGON
    )

    // Spawnursachen ignorieren → verhindert Endlos-Loop durch unseren eigenen spawnEntity()-Aufruf
    private val IGNORED_REASONS = setOf(
        CreatureSpawnEvent.SpawnReason.CUSTOM
    )

    // Nur sichere Mobs als mögliche Ersatz-Drops
    private val mobPool: List<EntityType> = EntityType.values().filter { entityType ->
        entityType.isAlive &&
                entityType.isSpawnable &&
                entityType != EntityType.PLAYER &&
                entityType !in BOSS_BLACKLIST
    }

    // Globale feste Zuordnung: Original → Ersatz
    private val mobMappings = mutableMapOf<EntityType, EntityType>()

    private fun getReplacementFor(original: EntityType): EntityType {
        return mobMappings.getOrPut(original) {
            val pool = if (mobPool.size > 1) mobPool.filter { it != original } else mobPool
            pool.random()
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onCreatureSpawn(event: CreatureSpawnEvent) {
        if (!plugin.isMobRandomizerActive) return

        // Unseren eigenen CUSTOM-Spawn ignorieren → kein Endlos-Loop
        if (event.spawnReason in IGNORED_REASONS) return

        val original = event.entityType

        // Bosse als Original nie ersetzen (Wither-Beschwörung bleibt normal)
        if (original in BOSS_BLACKLIST) return

        val location = event.location

        // Nur in der Nähe eines Spielers ersetzen (Distanz-Check)
        val nearestDistSq = location.world?.players
            ?.minOfOrNull { it.location.distanceSquared(location) }
            ?: Double.MAX_VALUE

        if (nearestDistSq > MAX_SPAWN_DISTANCE * MAX_SPAWN_DISTANCE) return

        val replacement = getReplacementFor(original)
        if (replacement == original) return

        event.isCancelled = true

        plugin.server.scheduler.runTask(plugin, Runnable {
            val world = location.world ?: return@Runnable

            // Nochmal prüfen ob noch jemand in der Nähe ist (Spieler könnte weggegangen sein)
            val stillNearby = world.players.any {
                it.location.distanceSquared(location) <= MAX_SPAWN_DISTANCE * MAX_SPAWN_DISTANCE
            }
            if (stillNearby) {
                world.spawnEntity(location, replacement, CreatureSpawnEvent.SpawnReason.CUSTOM)
            }
        })
    }

    // ─── Persistenz ──────────────────────────────────────────────────────────

    fun saveMappings() {
        val file = File(plugin.dataFolder, "mob_randomizer_mappings.yml")
        val yaml = YamlConfiguration()
        for ((original, replacement) in mobMappings) {
            yaml.set(original.name, replacement.name)
        }
        yaml.save(file)
    }

    fun loadMappings() {
        val file = File(plugin.dataFolder, "mob_randomizer_mappings.yml")
        if (!file.exists()) return
        val yaml = YamlConfiguration.loadConfiguration(file)
        mobMappings.clear()
        for (key in yaml.getKeys(false)) {
            val original = try { EntityType.valueOf(key) } catch (e: Exception) { continue }
            val replacementName = yaml.getString(key) ?: continue
            val replacement = try { EntityType.valueOf(replacementName) } catch (e: Exception) { continue }
            // Blacklist-Einträge aus alten Saves ignorieren
            if (replacement in BOSS_BLACKLIST) continue
            mobMappings[original] = replacement
        }
    }

    fun resetAll() {
        mobMappings.clear()
        File(plugin.dataFolder, "mob_randomizer_mappings.yml").delete()
    }
}