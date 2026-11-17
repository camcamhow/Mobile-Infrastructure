package com.example.mobilecellnetwork.gui.map;

import java.util.HashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;

public final class MapPalette {
    private static final int FALLBACK_COLOR = 0xFF6E6E6E;
    private static final HashMap<Block, Integer> BASE_COLORS = createBaseColors();

    private MapPalette() {
    }

    public static int colorFor(BlockState state, int worldX, int worldY, int worldZ,
                               int minY, int maxY) {
        Integer mappedColor = BASE_COLORS.get(state.getBlock());
        int color = mappedColor == null ? FALLBACK_COLOR : mappedColor;
        boolean water = state.getFluidState().is(Fluids.WATER);
        boolean lava = state.getFluidState().is(Fluids.LAVA);

        if (water) {
            color = waterColor(state, worldX, worldY, worldZ);
        } else if (lava) {
            return 0xFFEE7821;
        } else if (state.is(net.minecraft.tags.BlockTags.LEAVES) && hasSolidBlockAbove(worldX, worldY, worldZ)) {
            color = multiply(color, 0.88F);
        }

        color = multiply(color, 0.9F + 0.2F * hash2D(worldX, worldZ));
        if (water) {
            color = clampWaterColor(color);
        } else {
            float t = maxY > minY ? (worldY - minY) / (float) (maxY - minY) : 0.5F;
            color = multiply(color, 0.75F + 0.5F * t);
        }
        return color;
    }

    private static int waterColor(BlockState state, int worldX, int worldY, int worldZ) {
        var level = Minecraft.getInstance().level;
        int depth = 1;
        if (level != null) {
            BlockPos pos = new BlockPos(worldX, worldY, worldZ);
            if (level.getBlockState(pos.above()).isAir()) {
                for (int y = worldY - 1; y >= level.getMinBuildHeight(); y--) {
                    if (!level.getBlockState(new BlockPos(worldX, y, worldZ))
                            .getFluidState().is(Fluids.WATER)) {
                        break;
                    }
                    depth++;
                }
            }
        } else if (!state.getFluidState().isSource()) {
            depth = 2;
        }

        int color = 0xFF4A8FDB;
        float factor = Math.max(0.0F, 1.0F - (depth - 1) * 0.05F);
        color = multiply(color, factor);
        int red = Math.max(0x1A, (color >> 16) & 0xFF);
        int green = Math.max(0x3A, (color >> 8) & 0xFF);
        int blue = Math.max(0x6A, color & 0xFF);
        return 0xFF000000 | red << 16 | green << 8 | blue;
    }

    private static int clampWaterColor(int color) {
        int red = Math.max(0x1A, (color >> 16) & 0xFF);
        int green = Math.max(0x3A, (color >> 8) & 0xFF);
        int blue = Math.max(0x6A, color & 0xFF);
        return 0xFF000000 | red << 16 | green << 8 | blue;
    }

    private static boolean hasSolidBlockAbove(int worldX, int worldY, int worldZ) {
        var level = Minecraft.getInstance().level;
        return level != null && level.getBlockState(new BlockPos(worldX, worldY + 1, worldZ)).blocksMotion();
    }

    private static float hash2D(int worldX, int worldZ) {
        int hash = worldX * 374761393 + worldZ * 668265263;
        hash = (hash ^ (hash >> 13)) * 1274126177;
        hash ^= hash >> 16;
        return (hash & 0xFFFFFF) / (float) 0x1000000;
    }

    private static int multiply(int color, float factor) {
        int red = Math.clamp(Math.round(((color >> 16) & 0xFF) * factor), 0, 255);
        int green = Math.clamp(Math.round(((color >> 8) & 0xFF) * factor), 0, 255);
        int blue = Math.clamp(Math.round((color & 0xFF) * factor), 0, 255);
        return color & 0xFF000000 | red << 16 | green << 8 | blue;
    }

    private static HashMap<Block, Integer> createBaseColors() {
        HashMap<Block, Integer> colors = new HashMap<>();
        colors.put(Blocks.GRASS_BLOCK, 0xFF7CBD6B);
        colors.put(Blocks.DIRT, 0xFF8B6B4A);
        colors.put(Blocks.COARSE_DIRT, 0xFF7A5E40);
        colors.put(Blocks.PODZOL, 0xFF5A3A22);
        colors.put(Blocks.FARMLAND, 0xFF6B4A2E);
        colors.put(Blocks.SAND, 0xFFE8D8A0);
        colors.put(Blocks.RED_SAND, 0xFFC66A2E);
        colors.put(Blocks.GRAVEL, 0xFF9A9390);
        colors.put(Blocks.STONE, 0xFF9A9A9A);
        colors.put(Blocks.COBBLESTONE, 0xFF7A7A7A);
        colors.put(Blocks.DEEPSLATE, 0xFF5A5A5E);
        colors.put(Blocks.BEDROCK, 0xFF2A2A2E);
        colors.put(Blocks.WATER, 0xFF3B6FC4);
        colors.put(Blocks.LAVA, 0xFFEE7821);
        colors.put(Blocks.SNOW, 0xFFF0F0F0);
        colors.put(Blocks.SNOW_BLOCK, 0xFFF0F0F0);
        colors.put(Blocks.ICE, 0xFFA0D0F0);
        colors.put(Blocks.PACKED_ICE, 0xFF7AB0E0);
        colors.put(Blocks.BLUE_ICE, 0xFF5A8FC0);
        colors.put(Blocks.OAK_LOG, 0xFF5A4020);
        colors.put(Blocks.SPRUCE_LOG, 0xFF3A2818);
        colors.put(Blocks.BIRCH_LOG, 0xFFD8C8A0);
        colors.put(Blocks.JUNGLE_LOG, 0xFF7A5A30);
        colors.put(Blocks.ACACIA_LOG, 0xFF8A5A30);
        colors.put(Blocks.DARK_OAK_LOG, 0xFF3A2818);
        colors.put(Blocks.OAK_LEAVES, 0xFF3E7A2E);
        colors.put(Blocks.SPRUCE_LEAVES, 0xFF2A5A28);
        colors.put(Blocks.BIRCH_LEAVES, 0xFF6FA850);
        colors.put(Blocks.JUNGLE_LEAVES, 0xFF2A6A2A);
        colors.put(Blocks.ACACIA_LEAVES, 0xFF5A8A30);
        colors.put(Blocks.DARK_OAK_LEAVES, 0xFF2A5A28);
        colors.put(Blocks.AZALEA_LEAVES, 0xFF4A8A3A);
        colors.put(Blocks.FLOWERING_AZALEA_LEAVES, 0xFF5A9A4A);
        colors.put(Blocks.MOSS_BLOCK, 0xFF4A7A3A);
        colors.put(Blocks.MUD, 0xFF3A2A2A);
        colors.put(Blocks.CLAY, 0xFFA0A8B0);
        colors.put(Blocks.TERRACOTTA, 0xFF9A5A40);
        colors.put(Blocks.NETHERRACK, 0xFF6A2A2A);
        colors.put(Blocks.SOUL_SAND, 0xFF4A3A2A);
        colors.put(Blocks.END_STONE, 0xFFD8E0B0);
        return colors;
    }
}
