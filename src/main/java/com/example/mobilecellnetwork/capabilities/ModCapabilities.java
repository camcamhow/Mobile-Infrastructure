package com.example.mobilecellnetwork.capabilities;

import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.minecraft.world.entity.EntityType;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import com.example.mobilecellnetwork.MobileCellNetwork;
import com.example.mobilecellnetwork.blockentity.AntennaBlockEntity;
import com.example.mobilecellnetwork.blockentity.PhoneChargerBlockEntity;
import com.example.mobilecellnetwork.registry.ModBlockEntities;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.ItemStackHandler;

public final class ModCapabilities {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, MobileCellNetwork.MOD_ID);

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<PhoneData>> PHONE_DATA =
            ATTACHMENTS.register("phone_data", () -> AttachmentType.serializable(PhoneData::new).copyOnDeath().build());
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<BankAccount>> BANK_ACCOUNT =
            ATTACHMENTS.register("bank_account", () -> AttachmentType.serializable(BankAccount::new).copyOnDeath().build());
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<PlanUsage>> PLAN_USAGE =
            ATTACHMENTS.register("plan_usage", () -> AttachmentType.serializable(PlanUsage::new).copyOnDeath().build());

    private ModCapabilities() {
    }

    public static void register(RegisterCapabilitiesEvent event) {
        event.registerEntity(PhoneData.CAPABILITY, EntityType.PLAYER,
                (player, context) -> player.getData(PHONE_DATA));
        event.registerEntity(BankAccount.CAPABILITY, EntityType.PLAYER,
                (player, context) -> player.getData(BANK_ACCOUNT));
        event.registerEntity(PlanUsage.CAPABILITY, EntityType.PLAYER,
                (player, context) -> player.getData(PLAN_USAGE));
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, ModBlockEntities.COPPER_ANTENNA.get(),
                (antenna, side) -> (AntennaBlockEntity) antenna);
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, ModBlockEntities.IRON_ANTENNA.get(),
                (antenna, side) -> (AntennaBlockEntity) antenna);
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, ModBlockEntities.DIAMOND_ANTENNA.get(),
                (antenna, side) -> (AntennaBlockEntity) antenna);
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, ModBlockEntities.TEST_ANTENNA.get(),
                (antenna, side) -> antenna);
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, ModBlockEntities.SERVER.get(),
                (server, side) -> server);
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, ModBlockEntities.PHONE_CHARGER.get(),
                (charger, side) -> charger);
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, ModBlockEntities.SIGNAL_BOOSTER.get(),
                (booster, side) -> booster);
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.PHONE_CHARGER.get(),
                (charger, side) -> charger.getItemHandler());
    }
}
