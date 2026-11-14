package com.example.mobilecellnetwork.blockentity;

import com.example.mobilecellnetwork.config.MobileCellNetworkConfig;
import com.example.mobilecellnetwork.registry.ModBlockEntities;
import com.example.mobilecellnetwork.registry.ModDataComponents;
import com.example.mobilecellnetwork.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.ItemStackHandler;

public class PhoneChargerBlockEntity extends BlockEntity implements IEnergyStorage {
    private final ItemStackHandler itemHandler = new ItemStackHandler(1) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot == 0 && stack.is(ModItems.CELLPHONE.get());
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    private int energy;

    public PhoneChargerBlockEntity(BlockPos position, BlockState state) {
        super(ModBlockEntities.PHONE_CHARGER.get(), position, state);
    }

    public ItemStackHandler getItemHandler() {
        return itemHandler;
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
        if (level == null || level.isClientSide || level.getGameTime() % 5 != 0) {
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
        if (tag.contains("items")) {
            itemHandler.deserializeNBT(registries, tag.getCompound("items"));
        }
    }

    @Override
    public void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("energy", energy);
        tag.put("items", itemHandler.serializeNBT(registries));
    }
}
