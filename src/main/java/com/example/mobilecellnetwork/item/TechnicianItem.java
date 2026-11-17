package com.example.mobilecellnetwork.item;

import com.example.mobilecellnetwork.block.TestTowerSectionBlock;
import com.example.mobilecellnetwork.block.TowerCabinets;
import com.example.mobilecellnetwork.blockentity.AntennaBlockEntity;
import com.example.mobilecellnetwork.blockentity.InfrastructurePower;
import com.example.mobilecellnetwork.blockentity.TestAntennaBlockEntity;
import com.example.mobilecellnetwork.infrastructure.Installation;
import com.example.mobilecellnetwork.infrastructure.PowerWire;
import com.example.mobilecellnetwork.infrastructure.Serviceable;
import com.example.mobilecellnetwork.infrastructure.WirePath;
import com.example.mobilecellnetwork.infrastructure.WiredDevice;
import com.example.mobilecellnetwork.infrastructure.CablePorts;
import java.util.ArrayList;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;

public final class TechnicianItem extends Item {
    public enum Tool { MODULE, SPOOL, TESTER }
    private final Tool tool;

    public TechnicianItem(Properties properties, Tool tool) {
        super(properties);
        this.tool = tool;
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return tool == Tool.TESTER ? slotChanged || oldStack.getItem() != newStack.getItem()
                : super.shouldCauseReequipAnimation(oldStack, newStack, slotChanged);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, java.util.List<Component> tooltip,
                                net.minecraft.world.item.TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        if (tool == Tool.TESTER) {
            tooltip.add(Component.translatable("tooltip.mobilecellnetwork.technician_tester"));
        }
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, net.minecraft.world.entity.Entity entity,
                              int slot, boolean selected) {
        if (tool == Tool.TESTER && level.getGameTime() % 20 == 0
                && entity instanceof net.minecraft.server.level.ServerPlayer player
                && (selected || player.getOffhandItem() == stack)) {
            com.example.mobilecellnetwork.infrastructure.TechnicianDiagnostics.updateMeter(player, stack,
                    getPlayerPOVHitResult(level, player, net.minecraft.world.level.ClipContext.Fluid.NONE));
        }
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;
        Level level = context.getLevel();
        if (level.isClientSide) return InteractionResult.SUCCESS;
        BlockPos clicked = context.getClickedPos();
        var state = level.getBlockState(clicked);
        BlockPos pos = state.getBlock() instanceof TestTowerSectionBlock
                ? TestTowerSectionBlock.basePosition(clicked, state) : clicked;
        if (!player.mayBuild() || !level.mayInteract(player, clicked)
                || !player.mayUseItemAt(clicked, context.getClickedFace(), stack)
                || !level.mayInteract(player, pos)) {
            return message(player, "denied");
        }
        BlockEntity block = level.getBlockEntity(pos);
        if (tool == Tool.TESTER && player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            com.example.mobilecellnetwork.infrastructure.TechnicianDiagnostics.updateMeter(serverPlayer, stack,
                    new net.minecraft.world.phys.BlockHitResult(context.getClickLocation(), context.getClickedFace(),
                            context.getClickedPos(), context.isInside()));
        }
        if (!(block instanceof WiredDevice device) || !(block instanceof Serviceable) && tool != Tool.SPOOL) {
            if (tool == Tool.SPOOL && !player.isShiftKeyDown()) return routeOrSelect(context);
            if (tool == Tool.TESTER && player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                if (block instanceof com.example.mobilecellnetwork.blockentity.PowerTapBlockEntity tap) {
                    com.example.mobilecellnetwork.infrastructure.TechnicianDiagnostics.tap(player, tap);
                } else {
                    IEnergyStorage storage = level.getCapability(Capabilities.EnergyStorage.BLOCK, clicked,
                            context.getClickedFace());
                    if (storage != null) {
                        com.example.mobilecellnetwork.infrastructure.TechnicianDiagnostics.power(
                                player, state.getBlock().getName(), clicked, storage);
                    } else {
                        com.example.mobilecellnetwork.infrastructure.TechnicianDiagnostics.survey(serverPlayer);
                    }
                }
                return InteractionResult.CONSUME;
            }
            return message(player, "target");
        }
        Installation installation = device.installation();
        var owner = block instanceof AntennaBlockEntity antenna ? antenna.getOwnerUUID() : installation.owner();
        if (owner != null && !owner.equals(player.getUUID()) && !player.hasPermissions(2)) {
            return message(player, "owner");
        }
        if (block instanceof TestAntennaBlockEntity tower && tower.isWideTower()) {
            int cabinet = TowerCabinets.at(tower.variant(), tower.variant().toModel(context.getClickLocation()
                    .subtract(net.minecraft.world.phys.Vec3.atLowerCornerOf(pos))));
            if (cabinet < 0) {
                if (tool == Tool.SPOOL && !player.isShiftKeyDown()) return routeOrSelect(context);
                return message(player, "aim_cabinet");
            }
            if (!tower.isCabinetOpen(cabinet)) return message(player, "open_cabinet");
            if (tool == Tool.MODULE && cabinet == 0 && !tower.variant().monopole()) return message(player, "climb");
            if (tool == Tool.MODULE && cabinet == 1 && tower.variant().separateComputerDeck()) {
                return message(player, "climb");
            }
            if (tool == Tool.SPOOL && cabinet != 0) return message(player, "base");
        }
        switch (tool) {
            case MODULE -> {
                if (installation.hasModule()) return message(player, "module_present");
                if (block instanceof TestAntennaBlockEntity tower
                        && clicked.getY() - pos.getY() < tower.getTowerHeight() - 6) {
                    return message(player, "climb");
                }
                installation.installModule();
                if (!player.isCreative()) stack.shrink(1);
                message(player, "module_installed");
            }
            case SPOOL -> {
                if (!(block instanceof TestAntennaBlockEntity tower && tower.isWideTower())
                        && clicked.getY() != pos.getY()) return message(player, "base");
                if (player.isShiftKeyDown()) {
                    if (installation.source() == null) return message(player, "no_wire");
                    installation.disconnect();
                    clearSelection(stack);
                    message(player, "disconnected");
                } else {
                    if (installation.source() != null) return message(player, "wire_present");
                    CompoundTag selection = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
                    if (!selection.contains("wireSource")) return message(player, "select_source");
                    if (!level.dimension().location().toString().equals(selection.getString("wireDimension"))) {
                        clearSelection(stack);
                        return message(player, "dimension");
                    }
                    BlockPos source = BlockPos.of(selection.getLong("wireSource"));
                    Direction face = Direction.from3DDataValue(selection.getInt("wireFace"));
                    if (!level.hasChunkAt(source)) return message(player, "unloaded");
                    if (!level.mayInteract(player, source) || !player.mayUseItemAt(source, face, stack)) {
                        return message(player, "denied");
                    }
                    if (!PowerWire.canConnect(source, pos)) {
                        return message(player, "length", Installation.MAX_WIRE_LENGTH);
                    }
                    IEnergyStorage supply = level.getCapability(Capabilities.EnergyStorage.BLOCK, source, face);
                    if (level.getBlockEntity(source) instanceof WiredDevice || supply == null || !supply.canExtract()) {
                        clearSelection(stack);
                        return message(player, "source");
                    }
                    Vec3 from = selection.contains("wireStart")
                            ? WirePath.loadPoint(selection.getCompound("wireStart"))
                            : Vec3.atCenterOf(source).add(Vec3.atLowerCornerOf(face.getNormal()).scale(0.5));
                    Vec3 to = CablePorts.point(block);
                    var snaps = WirePath.load(selection, "wireSnaps");
                    if (!WirePath.valid(from, snaps, to)) {
                        return message(player, "length", Installation.MAX_WIRE_LENGTH);
                    }
                    int cost = (int) Math.ceil(WirePath.length(from, snaps, to));
                    if (!player.isCreative() && stack.getMaxDamage() - stack.getDamageValue() < cost) {
                        return message(player, "spool_empty", cost);
                    }
                    installation.connect(source, face, from, snaps, to);
                    clearSelection(stack);
                    if (!player.isCreative()) stack.hurtAndBreak(cost, player,
                            context.getHand() == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
                    message(player, block instanceof Serviceable ? "connected" : "charger_connected", cost);
                }
            }
            case TESTER -> {
                if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                    com.example.mobilecellnetwork.infrastructure.TechnicianDiagnostics.equipment(
                            serverPlayer, block, installation);
                }
                if (player.isShiftKeyDown()) return InteractionResult.CONSUME;
                if (!installation.required()) return message(player, "legacy");
                if (!installation.hasModule()) return message(player, "missing_module");
                if (installation.source() == null) return message(player, "select_source");
                if (!installation.commission(InfrastructurePower.bypassed(level)
                        || block instanceof IEnergyStorage storage && storage.getEnergyStored() > 0)) {
                    return message(player, "no_power");
                }
                message(player, "commissioned");
            }
        }
        block.setChanged();
        level.sendBlockUpdated(pos, block.getBlockState(), block.getBlockState(), 3);
        if (tool == Tool.TESTER && player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            com.example.mobilecellnetwork.infrastructure.TechnicianDiagnostics.updateMeter(serverPlayer, stack,
                    new net.minecraft.world.phys.BlockHitResult(context.getClickLocation(), context.getClickedFace(),
                            context.getClickedPos(), context.isInside()));
        }
        return InteractionResult.CONSUME;
    }

    private InteractionResult routeOrSelect(UseOnContext context) {
        var level = context.getLevel();
        var pos = context.getClickedPos();
        var stack = context.getItemInHand();
        var selection = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (selection.contains("wireSource")) {
            if (!level.dimension().location().toString().equals(selection.getString("wireDimension"))) {
                clearSelection(stack);
                return message(context.getPlayer(), "dimension");
            }
            if (level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()) {
                return message(context.getPlayer(), "snap_solid");
            }
            var snaps = new ArrayList<>(WirePath.load(selection, "wireSnaps"));
            if (snaps.size() >= WirePath.MAX_SNAPS) return message(context.getPlayer(), "snap_limit", WirePath.MAX_SNAPS);
            Vec3 from = selection.contains("wireStart") ? WirePath.loadPoint(selection.getCompound("wireStart"))
                    : Vec3.atCenterOf(BlockPos.of(selection.getLong("wireSource"))).add(
                            Vec3.atLowerCornerOf(Direction.from3DDataValue(selection.getInt("wireFace"))
                                    .getNormal()).scale(0.5));
            Vec3 point = context.getClickLocation().add(
                    Vec3.atLowerCornerOf(context.getClickedFace().getNormal()).scale(0.025));
            Vec3 previous = snaps.isEmpty() ? from : snaps.getLast();
            if (previous.distanceToSqr(point) < 0.01) return message(context.getPlayer(), "snap_duplicate");
            if (!WirePath.valid(from, snaps, point)) {
                return message(context.getPlayer(), "length", Installation.MAX_WIRE_LENGTH);
            }
            snaps.add(point);
            CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
                tag.put("wireStart", WirePath.savePoint(from));
                tag.put("wireSnaps", WirePath.save(snaps));
            });
            return message(context.getPlayer(), "snapped", snaps.size());
        }
        if (level.getBlockEntity(pos) instanceof com.example.mobilecellnetwork.blockentity.PowerTapBlockEntity
                && context.getClickedFace() != Direction.UP) {
            return message(context.getPlayer(), "tap_output");
        }
        IEnergyStorage supply = level.getCapability(Capabilities.EnergyStorage.BLOCK, pos, context.getClickedFace());
        if (supply == null || !supply.canExtract()) return message(context.getPlayer(), "source");
        CustomData.update(DataComponents.CUSTOM_DATA, context.getItemInHand(), tag -> {
            tag.putLong("wireSource", pos.asLong());
            tag.putInt("wireFace", context.getClickedFace().get3DDataValue());
            tag.putString("wireDimension", level.dimension().location().toString());
            tag.put("wireStart", WirePath.savePoint(CablePorts.sourcePoint(level, pos, context.getClickLocation())));
            tag.remove("wireSnaps");
        });
        return message(context.getPlayer(), "source_selected");
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (tool == Tool.TESTER) {
            if (!level.isClientSide && player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                com.example.mobilecellnetwork.infrastructure.TechnicianDiagnostics.updateMeter(serverPlayer, stack,
                        getPlayerPOVHitResult(level, player, net.minecraft.world.level.ClipContext.Fluid.NONE));
                com.example.mobilecellnetwork.infrastructure.TechnicianDiagnostics.survey(serverPlayer);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        if (tool == Tool.SPOOL && player.isShiftKeyDown()) {
            if (!level.isClientSide) {
                clearSelection(stack);
                message(player, "cleared");
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        return InteractionResultHolder.pass(stack);
    }

    private static void clearSelection(ItemStack stack) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.remove("wireSource");
            tag.remove("wireFace");
            tag.remove("wireDimension");
            tag.remove("wireStart");
            tag.remove("wireSnaps");
        });
    }

    private static InteractionResult message(Player player, String key, Object... args) {
        player.displayClientMessage(Component.translatable("message.mobilecellnetwork.technician." + key, args), true);
        return InteractionResult.CONSUME;
    }
}
