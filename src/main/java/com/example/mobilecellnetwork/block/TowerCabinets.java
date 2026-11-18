package com.example.mobilecellnetwork.block;

import com.example.mobilecellnetwork.blockentity.TestAntennaBlockEntity;
import com.example.mobilecellnetwork.gui.AntennaMenu;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public final class TowerCabinets {
    public static final Vec3 MONOPOLE_OFFSET = new Vec3(0, 0, -0.6);
    public static final List<AABB> BOUNDS = List.of(
            new AABB(0.15, TestTowerGeometry.FOUNDATION_TOP, 0.15, 0.85, 1.4, 0.85),
            platform(14), platform(17));
    public static final int ALL_OPEN = (1 << BOUNDS.size()) - 1;
    public static final List<AABB> TERMINALS = terminals();

    private TowerCabinets() {}

    public static Vec3 offset(TowerVariant variant) {
        return variant.monopole() ? MONOPOLE_OFFSET : Vec3.ZERO;
    }

    public static AABB bounds(TowerVariant variant, int index, boolean terminal) {
        return (terminal ? TERMINALS : BOUNDS).get(index).move(offset(variant));
    }

    private static AABB platform(int deck) {
        return new AABB(1.7, deck + TestTowerGeometry.DECK_THICKNESS, -1.25, 2.3, deck + 1.1, -0.65);
    }

    private static List<AABB> terminals() {
        List<AABB> terminals = new ArrayList<>();
        for (int i = 0; i < BOUNDS.size(); i++) {
            AABB cabinet = BOUNDS.get(i);
            terminals.add(localBounds(i, new AABB(0.06, cabinet.getYsize() + 0.1, 0.02,
                    cabinet.getXsize() - 0.06, cabinet.getYsize() + 0.58, 0.12)));
        }
        return List.copyOf(terminals);
    }

    public static Vec3 localPoint(int index, double x, double y, double z) {
        AABB cabinet = BOUNDS.get(index);
        return index == 0 ? new Vec3(cabinet.minX + x, cabinet.minY + y, cabinet.minZ + z)
                : new Vec3(cabinet.minX + z, cabinet.minY + y, cabinet.maxZ - x);
    }

    public static AABB localBounds(int index, AABB bounds) {
        Vec3 a = localPoint(index, bounds.minX, bounds.minY, bounds.minZ);
        Vec3 b = localPoint(index, bounds.maxX, bounds.maxY, bounds.maxZ);
        return new AABB(a, b);
    }

    public static Vec3 frontCenter(int index, boolean terminal) {
        AABB bounds = terminal ? TERMINALS.get(index) : BOUNDS.get(index);
        return index == 0 ? new Vec3(bounds.getCenter().x, bounds.getCenter().y, bounds.minZ)
                : new Vec3(bounds.minX, bounds.getCenter().y, bounds.getCenter().z);
    }

    public static int terminalAt(Vec3 localHit) {
        for (int i = 0; i < TERMINALS.size(); i++) {
            if (TERMINALS.get(i).inflate(0.015).contains(localHit)) return i;
        }
        return -1;
    }

    public static int terminalAt(TowerVariant variant, Vec3 localHit) {
        int index = terminalAt(localHit.subtract(offset(variant)));
        return variant.hasTerminal(index) ? index : -1;
    }

    public static int at(TowerVariant variant, Vec3 localHit) {
        int index = at(localHit.subtract(offset(variant)));
        return variant.hasCabinet(index) ? index : -1;
    }

    public static boolean withinTerminalRange(Vec3 player, Vec3 terminal) {
        return player.distanceToSqr(terminal) <= 64;
    }

    public static int at(Vec3 localHit) {
        for (int i = 0; i < BOUNDS.size(); i++) {
            if (BOUNDS.get(i).inflate(0.015).contains(localHit)) return i;
        }
        return -1;
    }

    public static InteractionResult use(Level level, BlockPos base, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(base) instanceof TestAntennaBlockEntity tower) || !tower.isWideTower()) {
            return InteractionResult.PASS;
        }
        Vec3 localHit = tower.variant().toModel(hit.getLocation().subtract(Vec3.atLowerCornerOf(base)));
        int cabinet = at(tower.variant(), localHit);
        int terminal = terminalAt(tower.variant(), localHit);
        if (cabinet < 0 && terminal < 0) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!player.mayBuild() || !level.mayInteract(player, base)
                || !level.mayInteract(player, hit.getBlockPos())
                || !player.mayUseItemAt(hit.getBlockPos(), hit.getDirection(), player.getMainHandItem())) {
            player.displayClientMessage(Component.translatable("message.mobilecellnetwork.technician.denied"), true);
            return InteractionResult.CONSUME;
        }
        if (tower.getOwnerUUID() != null && !tower.getOwnerUUID().equals(player.getUUID())
                && !player.hasPermissions(2)) {
            player.displayClientMessage(Component.translatable("message.mobilecellnetwork.technician.owner"), true);
            return InteractionResult.CONSUME;
        }
        if (terminal >= 0) {
            if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                serverPlayer.openMenu(new net.minecraft.world.SimpleMenuProvider(
                        (id, inventory, user) -> new AntennaMenu(id, inventory, tower, terminal),
                        Component.translatable("gui.mobilecellnetwork.tower_terminal")),
                        buffer -> buffer.writeBlockPos(base));
            }
            return InteractionResult.CONSUME;
        }
        tower.toggleCabinet(cabinet);
        String key = !tower.isCabinetOpen(cabinet) ? "cabinet_closed"
                : tower.variant().monopole() ? "monopole_cabinet_open"
                : cabinet == 0 ? "power_cabinet_open" : "radio_cabinet_open";
        player.displayClientMessage(Component.translatable("message.mobilecellnetwork.technician." + key), true);
        level.playSound(null, hit.getBlockPos(), tower.isCabinetOpen(cabinet)
                        ? net.minecraft.sounds.SoundEvents.IRON_TRAPDOOR_OPEN
                        : net.minecraft.sounds.SoundEvents.IRON_TRAPDOOR_CLOSE,
                net.minecraft.sounds.SoundSource.BLOCKS, 0.6F, 1.0F);
        return InteractionResult.CONSUME;
    }
}
