package com.example.mobilecellnetwork.blockentity;

import com.example.mobilecellnetwork.config.MobileCellNetworkConfig;
import com.example.mobilecellnetwork.registry.ModBlockEntities;
import com.example.mobilecellnetwork.registry.ModDataComponents;
import com.example.mobilecellnetwork.infrastructure.Installation;
import com.example.mobilecellnetwork.infrastructure.PowerWire;
import com.example.mobilecellnetwork.infrastructure.WiredDevice;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.ItemStackHandler;

public class PhoneChargerBlockEntity extends BlockEntity implements IEnergyStorage, WiredDevice {
    private static final java.util.Set<PhoneChargerBlockEntity> LOADED = new java.util.HashSet<>();
    private final Installation installation = new Installation();

    @Override
    public Installation installation() { return installation; }

    public static java.util.List<PhoneChargerBlockEntity> loaded(net.minecraft.world.level.Level level) {
        return LOADED.stream().filter(charger -> !charger.isRemoved() && charger.level == level).toList();
    }

    @Override
    public void onLoad() {
        super.onLoad();
        LOADED.add(this);
    }

    @Override
    public void setRemoved() {
        LOADED.remove(this);
        super.setRemoved();
    }

    @Override
    public void onChunkUnloaded() {
        LOADED.remove(this);
        super.onChunkUnloaded();
    }
    private ItemStack displayPhone = ItemStack.EMPTY;
    private String lastDisplayItem = "";
    private int displayChargePercent = -1;
    private int lastDisplayChargePercent = -1;
    private boolean displayPowered;
    private boolean lastDisplayPowered;
    private final ItemStackHandler itemHandler = new ItemStackHandler(1) {
        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot == 0 && com.example.mobilecellnetwork.item.CellphoneItem.isPhone(stack);
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            syncDisplay();
        }
    };
    private int energy;

    public PhoneChargerBlockEntity(BlockPos position, BlockState state) {
        super(ModBlockEntities.PHONE_CHARGER.get(), position, state);
    }

    public ItemStackHandler getItemHandler() {
        return itemHandler;
    }

    private String displayItemId() {
        ItemStack stack = itemHandler.getStackInSlot(0);
        return stack.isEmpty() ? "" : BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
    }

    public ItemStack getDisplayPhone() {
        return level != null && level.isClientSide ? displayPhone : itemHandler.getStackInSlot(0);
    }

    private int calculateChargePercent() {
        ItemStack phone = itemHandler.getStackInSlot(0);
        if (phone.isEmpty()) return -1;
        int maxCharge = MobileCellNetworkConfig.PHONE_MAX_CHARGE.get();
        return (int) Math.clamp((long) phone.getOrDefault(ModDataComponents.PHONE_CHARGE.get(), 0)
                * 100L / maxCharge, 0L, 100L);
    }

    public int getDisplayChargePercent() {
        return level != null && level.isClientSide ? displayChargePercent : calculateChargePercent();
    }

    public boolean isDisplayPowered() {
        return level != null && level.isClientSide ? displayPowered : energy > 0;
    }

    private void syncDisplay() {
        if (level == null || level.isClientSide) return;
        String item = displayItemId();
        int percent = calculateChargePercent();
        boolean powered = energy > 0;
        if (!item.equals(lastDisplayItem) || percent != lastDisplayChargePercent || powered != lastDisplayPowered) {
            lastDisplayItem = item;
            lastDisplayChargePercent = percent;
            lastDisplayPowered = powered;
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putString("displayItem", displayItemId());
        tag.putInt("displayChargePercent", calculateChargePercent());
        tag.putBoolean("displayPowered", energy > 0);
        tag.put("installation", installation.save());
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        installation.load(tag.getCompound("installation"));
        ResourceLocation id = ResourceLocation.tryParse(tag.getString("displayItem"));
        var item = id == null ? null : BuiltInRegistries.ITEM.get(id);
        ItemStack stack = item == null ? ItemStack.EMPTY : new ItemStack(item);
        displayPhone = com.example.mobilecellnetwork.item.CellphoneItem.isPhone(stack) ? stack : ItemStack.EMPTY;
        displayChargePercent = displayPhone.isEmpty() ? -1
                : Math.clamp(tag.getInt("displayChargePercent"), 0, 100);
        displayPowered = tag.getBoolean("displayPowered");
    }

    @Override
    public void onDataPacket(Connection connection, ClientboundBlockEntityDataPacket packet,
                             HolderLookup.Provider registries) {
        handleUpdateTag(packet.getTag(), registries);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    public String getChargeStatus() {
        ItemStack phone = itemHandler.getStackInSlot(0);
        if (phone.isEmpty() || phone.getOrDefault(ModDataComponents.PHONE_CHARGE.get(), 0)
                >= MobileCellNetworkConfig.PHONE_MAX_CHARGE.get()) {
            return "Idle";
        }
        return energy >= MobileCellNetworkConfig.CHARGER_FE_DRAW_PER_TICK.get() ? "Charging" : "No FE";
    }

    public int getPhoneCharge() {
        ItemStack phone = itemHandler.getStackInSlot(0);
        return phone.isEmpty() ? 0 : phone.getOrDefault(ModDataComponents.PHONE_CHARGE.get(), 0);
    }

    public void tick() {
        if (level == null || level.isClientSide) return;
        PowerWire.tick(this, installation, this);
        if (level.getGameTime() % 5 != 0) {
            return;
        }
        ItemStack phone = itemHandler.getStackInSlot(0);
        if (phone.isEmpty()) {
            return;
        }
        int currentCharge = phone.getOrDefault(ModDataComponents.PHONE_CHARGE.get(), 0);
        int maxCharge = MobileCellNetworkConfig.PHONE_MAX_CHARGE.get();
        int energyCost = MobileCellNetworkConfig.CHARGER_FE_DRAW_PER_TICK.get();
        if (currentCharge >= maxCharge || energy < energyCost) {
            return;
        }
        energy -= energyCost;
        phone.set(ModDataComponents.PHONE_CHARGE.get(),
                Math.min(maxCharge, currentCharge + MobileCellNetworkConfig.CHARGER_CHARGE_RATE_PER_TICK.get()));
        itemHandler.setStackInSlot(0, phone);
        setChanged();
    }

    public void openChargerMenu(Player player) {
        player.displayClientMessage(Component.literal(
                "Phone Charger: " + energy + "/" + getMaxEnergyStored() + " FE | " + getChargeStatus()), true);
    }

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        if (maxReceive <= 0) return 0;
        int stored = Math.min(energy, getMaxEnergyStored());
        int accepted = Math.min(Math.min(maxReceive,
                MobileCellNetworkConfig.CHARGER_FE_MAX_RECEIVE_PER_TICK.get()), getMaxEnergyStored() - stored);
        accepted = Math.max(0, accepted);
        if (!simulate && accepted > 0) {
            energy = stored + accepted;
            setChanged();
            syncDisplay();
        }
        return accepted;
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        if (maxExtract <= 0) return 0;
        int extracted = Math.min(maxExtract, energy);
        if (!simulate && extracted > 0) {
            energy -= extracted;
            setChanged();
            syncDisplay();
        }
        return extracted;
    }

    @Override
    public int getEnergyStored() { return Math.min(energy, getMaxEnergyStored()); }
    @Override
    public int getMaxEnergyStored() { return MobileCellNetworkConfig.CHARGER_FE_CAPACITY.get(); }
    @Override
    public boolean canExtract() { return true; }
    @Override
    public boolean canReceive() { return energy < getMaxEnergyStored(); }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        energy = Math.clamp(tag.getInt("energy"), 0, getMaxEnergyStored());
        installation.load(tag.getCompound("installation"));
        if (tag.contains("items")) {
            itemHandler.deserializeNBT(registries, tag.getCompound("items"));
        }
        lastDisplayItem = displayItemId();
        lastDisplayChargePercent = calculateChargePercent();
        lastDisplayPowered = energy > 0;
    }

    @Override
    public void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("energy", energy);
        tag.put("installation", installation.save());
        tag.put("items", itemHandler.serializeNBT(registries));
    }
}
