package io.github.black_Kittys22.challengekittys.ChunkChallenge

import org.bukkit.Material
import org.bukkit.generator.ChunkGenerator
import org.bukkit.generator.WorldInfo
import java.util.Random

/**
 * Generiert eine Welt wo jeder Chunk komplett aus einem einzigen,
 * zufälligen Material besteht – wie in BastiGHGs "Doppel Chunk Randomizer".
 *
 * Registrierung in Main.kt (onEnable):
 *   val creator = WorldCreator("chunk_world")
 *   creator.generator(ChunkMaterialGenerator())
 *   Bukkit.createWorld(creator)
 */
class ChunkMaterialGenerator : ChunkGenerator() {

    // Materialien die als Chunk-Füllung in Frage kommen
    // (keine Flüssigkeiten, kein Luft, keine technischen Blöcke)
    private val possibleMaterials = Material.entries.filter { mat ->
        mat.isBlock &&
                mat.isSolid &&
                !mat.name.contains("AIR") &&
                !mat.name.contains("WATER") &&
                !mat.name.contains("LAVA") &&
                !mat.name.contains("FIRE") &&
                !mat.name.contains("PORTAL") &&
                !mat.name.contains("VOID") &&
                !mat.name.contains("SPAWNER") &&
                !mat.name.contains("COMMAND") &&
                !mat.name.contains("STRUCTURE") &&
                !mat.name.contains("BARRIER") &&
                !mat.name.contains("LIGHT") &&
                mat != Material.BEDROCK
    }

    // Speichert welcher Chunk welches Material bekommt (deterministisch per Seed)
    private val chunkMaterialCache = mutableMapOf<Long, Material>()

    private fun getChunkKey(chunkX: Int, chunkZ: Int): Long {
        return chunkX.toLong().shl(32) or (chunkZ.toLong() and 0xFFFFFFFFL)
    }

    private fun getMaterialForChunk(worldSeed: Long, chunkX: Int, chunkZ: Int): Material {
        val key = getChunkKey(chunkX, chunkZ)
        return chunkMaterialCache.getOrPut(key) {
            // Deterministisch: gleicher Seed + Chunk-Position = immer gleiches Material
            val random = Random(worldSeed xor key)
            possibleMaterials[random.nextInt(possibleMaterials.size)]
        }
    }

    override fun generateSurface(
        worldInfo: WorldInfo,
        random: Random,
        chunkX: Int,
        chunkZ: Int,
        chunkData: ChunkData
    ) {
        val material = getMaterialForChunk(worldInfo.seed, chunkX, chunkZ)

        val minY = worldInfo.minHeight
        val maxY = worldInfo.maxHeight

        // Gesamten Chunk mit dem Material füllen
        for (x in 0 until 16) {
            for (z in 0 until 16) {
                for (y in minY until maxY) {
                    chunkData.setBlock(x, y - minY, z, material)
                }
            }
        }
    }

    override fun shouldGenerateNoise(): Boolean = false
    override fun shouldGenerateSurface(): Boolean = true
    override fun shouldGenerateCaves(): Boolean = false
    override fun shouldGenerateDecorations(): Boolean = false
    override fun shouldGenerateMobs(): Boolean = false
    override fun shouldGenerateStructures(): Boolean = false
}