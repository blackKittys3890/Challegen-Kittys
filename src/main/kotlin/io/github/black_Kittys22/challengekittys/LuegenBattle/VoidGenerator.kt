package io.github.black_Kittys22.challengekittys.LuegenBattle

import org.bukkit.Material
import org.bukkit.generator.ChunkGenerator
import org.bukkit.generator.WorldInfo
import java.util.Random

class VoidGenerator : ChunkGenerator() {
    override fun generateSurface(worldInfo: WorldInfo, random: Random, x: Int, z: Int, chunkData: ChunkData) {
        // Erstelle eine Flatworld mit Gras
        // Y 0-63: Bedrock und Stein
        // Y 64-66: Dirt
        // Y 67: Grass Block

        for (blockX in 0..15) {
            for (blockZ in 0..15) {
                // Bedrock am Boden
                chunkData.setBlock(blockX, worldInfo.minHeight, blockZ, Material.BEDROCK)

                // Stein von Y 1 bis Y 63
                for (y in (worldInfo.minHeight + 1)..63) {
                    chunkData.setBlock(blockX, y, blockZ, Material.STONE)
                }

                // Dirt von Y 64 bis Y 66
                for (y in 64..66) {
                    chunkData.setBlock(blockX, y, blockZ, Material.DIRT)
                }

                // Grasblock auf Y 67
                chunkData.setBlock(blockX, 67, blockZ, Material.GRASS_BLOCK)
            }
        }
    }
}