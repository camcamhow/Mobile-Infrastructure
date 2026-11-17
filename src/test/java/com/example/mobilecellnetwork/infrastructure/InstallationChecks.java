package com.example.mobilecellnetwork.infrastructure;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.energy.EnergyStorage;

public final class InstallationChecks {
    public static void main(String[] args) throws java.io.IOException {
        Installation installation = new Installation();
        installation.load(new CompoundTag());
        require(installation.ready() && !installation.required(), "Old equipment must remain operational");
        UUID owner = UUID.randomUUID();
        installation.begin(owner);
        require(!installation.ready(), "New equipment must start offline");
        require(installation.status() == Installation.Status.MISSING_MODULE, "Must explain missing electronics");
        require(!installation.commission(true), "Cannot commission missing electronics and wire");
        installation.installModule();
        require(installation.status() == Installation.Status.MISSING_WIRE, "Must explain missing wire");
        require(!installation.commission(true), "Cannot commission without wire");
        installation.connect(new BlockPos(10, 64, -8), Direction.WEST);
        require(installation.status() == Installation.Status.AWAITING_TEST, "Must explain missing commissioning");
        require(!installation.commission(false), "Cannot commission without power");
        require(!installation.ready(), "Wiring alone must not bring equipment online");
        require(installation.commission(true) && installation.ready(), "Complete installation must work");
        require(installation.status() == Installation.Status.READY, "Commissioned equipment must show ready");

        Installation restored = new Installation();
        restored.load(installation.save());
        require(restored.ready() && restored.required() && restored.hasModule() && restored.commissioned(),
                "Installation state must survive reload and synchronization");
        require(owner.equals(restored.owner()) && restored.source().equals(installation.source())
                && restored.sourceFace() == Direction.WEST, "Owner and wire endpoint must survive reload");
        restored.disconnect();
        require(!restored.ready() && !restored.commissioned(), "Cutting wire must disable installation");
        restored.connect(new BlockPos(11, 64, -8), Direction.UP);
        require(!restored.ready(), "Rewiring must require another test");
        require(restored.commission(true), "Rewired installation must be commissionable");
        restored.connect(new BlockPos(12, 64, -8), Direction.NORTH);
        require(!restored.commissioned(), "Replacing source must invalidate commissioning");

        BlockPos origin = BlockPos.ZERO;
        require(PowerWire.canConnect(origin, new BlockPos(32, 0, 0)), "Exactly 32 blocks must be allowed");
        require(!PowerWire.canConnect(origin, new BlockPos(33, 0, 0)), "Over 32 blocks must be rejected");
        require(!PowerWire.canConnect(origin, origin), "Cannot wire a block to itself");
        require(!PowerWire.canConnect(origin, new BlockPos(32, 1, 0)), "Must check 3D length, not axis lengths");
        require(PowerWire.cableUnits(origin, new BlockPos(3, 4, 0)) == 5, "Exact distances must not overcharge");
        require(PowerWire.cableUnits(origin, new BlockPos(1, 1, 1)) == 2, "Fractional length must round up");
        require(PowerWire.cableUnits(origin, new BlockPos(-32, 0, 0)) == 32, "Negative coordinates must work");
        checkWireRoutes();
        checkFixedCablePorts();
        checkBoosterTuning();
        checkBoosterReceptionAndLayout();

        EnergyStorage source = new EnergyStorage(1000, 1000, 1000, 1000);
        EnergyStorage target = new EnergyStorage(1000);
        require(PowerWire.transfer(source, target) == 256, "Wire must transfer exactly 256 FE per tick");
        require(source.getEnergyStored() == 744 && target.getEnergyStored() == 256, "Energy must be conserved");
        EnergyStorage almostFull = new EnergyStorage(1000, 1000, 0, 990);
        require(PowerWire.transfer(source, almostFull) == 10, "Wire must respect remaining destination capacity");
        require(source.getEnergyStored() == 734 && almostFull.getEnergyStored() == 1000, "Full target lost energy");
        require(PowerWire.transfer(source, almostFull) == 0 && source.getEnergyStored() == 734,
                "A full target must not drain power");
        EnergyStorage limitedSource = new EnergyStorage(1000, 1000, 7, 100);
        EnergyStorage limitedTarget = new EnergyStorage(1000, 3, 0);
        require(PowerWire.transfer(limitedSource, limitedTarget) == 3, "Must respect destination input limit");
        require(limitedSource.getEnergyStored() == 97 && limitedTarget.getEnergyStored() == 3,
                "Limited transfer lost energy");
        require(PowerWire.transfer(limitedSource, target) == 7, "Must respect source output limit");
        require(PowerWire.transfer(new EnergyStorage(1000), target) == 0, "Empty source must not create energy");
        require(PowerWire.transfer(new EnergyStorage(1000, 1000, 0, 1000), target) == 0,
                "Receive-only source must not create energy");
        checkPowerTap();
        checkPushOnlyConverter();
        checkMeterReading();
        require(TechnicianDiagnostics.percent(0) == 0 && TechnicianDiagnostics.percent(1) == 100
                        && TechnicianDiagnostics.percent(0.456) == 46
                        && TechnicianDiagnostics.percent(-0.2) == 0 && TechnicianDiagnostics.percent(1.2) == 100,
                "Signal tester must report a clamped, rounded percentage");
        checkItemModels();
        checkTapResources();
        System.out.println("Installation, power wire, fixed equipment sockets/charger wiring/persistence, "
                + "directional tap buffer/simulation, push-only FE converter, "
                + "booster carrier tuning/persistence/strongest-source selection/responsive layout/"
                + "regenerated reception/HUD cache, "
                + "meter sample persistence/staleness/LCD layout, item facing, and 3D model/resource checks passed.");
    }

    private static void checkBoosterTuning() {
        record Source(String network, double strength) {}
        var weak = new Source("alpha", 0.3);
        var strong = new Source("alpha", 0.8);
        var other = new Source("beta", 0.95);
        var absent = new Source("alpha", 0);
        var candidates = java.util.List.of(weak, other, absent, strong);
        var legacy = BoosterTuning.load(new CompoundTag());
        require(legacy.automatic() && legacy.matches("alpha") && legacy.matches("beta"),
                "Existing boosters must retain automatic all-carrier behavior");
        require(legacy.strongest(candidates, Source::network, Source::strength) == other,
                "Automatic diagnostics must find the strongest direct upstream");
        var tuned = new BoosterTuning("alpha");
        require(tuned.matches("alpha") && !tuned.matches("beta") && !tuned.automatic(),
                "Tuned boosters must reject other carriers");
        require(tuned.strongest(candidates, Source::network, Source::strength) == strong,
                "Selected carrier must use its strongest source, not a stronger rival carrier");
        require(tuned.strongest(java.util.List.of(other, absent), Source::network, Source::strength) == null,
                "Losing the selected carrier must not fall back to another carrier or an absent signal");
        require(tuned.strongest(java.util.List.of(weak), Source::network, Source::strength) == weak,
                "Losing one tower must automatically switch to another receivable tower of that carrier");
        var saved = new CompoundTag();
        saved.putInt("energy", 1234);
        tuned.save(saved);
        require(BoosterTuning.load(saved).equals(tuned), "Carrier selection must persist across reloads");
        require(saved.getInt("energy") == 1234, "Tuning must not overwrite equipment power data");
        new BoosterTuning("").save(saved);
        require(BoosterTuning.load(saved).automatic(), "Clearing carrier selection must persist too");
        var buffer = new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
        try {
            var options = java.util.List.of(
                    new com.example.mobilecellnetwork.gui.SignalBoosterMenu.NetworkOption("", "", 0),
                    new com.example.mobilecellnetwork.gui.SignalBoosterMenu.NetworkOption("alpha", "Carrier A", 80),
                    new com.example.mobilecellnetwork.gui.SignalBoosterMenu.NetworkOption("beta", "Carrier A", 0));
            BlockPos position = new BlockPos(-12, 64, 34);
            buffer.writeBlockPos(position);
            buffer.writeCollection(options, (buf, option) -> option.write(buf));
            require(buffer.readBlockPos().equals(position)
                            && buffer.readList(com.example.mobilecellnetwork.gui.SignalBoosterMenu.NetworkOption::read)
                            .equals(options) && buffer.readableBytes() == 0,
                    "Booster opening data must preserve position, automatic entry, distinct carrier IDs, "
                            + "names, percentages, and unavailable selections");
        } finally {
            buffer.release();
        }
    }

    private static void checkBoosterReceptionAndLayout() {
        require(BoosterReception.strength(0.75, 0, 32, 0) == 1,
                "A powered booster must regenerate full local reception, not cap strength at incoming 75%");
        require(BoosterReception.strength(0.1, 8, 32, 0) == 0.75,
                "Weak but receivable upstream must support stronger local reception");
        require(BoosterReception.strength(0.75, 16, 32, 0.2) == 0.3,
                "Boosted reception must still lose strength with distance and obstructions");
        require(BoosterReception.strength(0, 0, 32, 0) == 0
                        && BoosterReception.strength(0.75, 32, 32, 0) == 0
                        && BoosterReception.strength(0.75, 33, 32, 0) == 0
                        && BoosterReception.strength(0.75, 0, 32, 1) == 0,
                "No upstream, out-of-range, or fully obstructed boosters must not create reception");
        for (int width : new int[] {320, 360, 427, 640, 960, 1920, 3840}) {
            for (int height : new int[] {240, 256, 360, 480, 1080, 2160}) {
                var layout = com.example.mobilecellnetwork.gui.BoosterMenuLayout.fit(width, height);
                require(layout.width() <= width - 12 && layout.height() <= height - 12,
                        "Booster panel must remain inside every GUI-scaled viewport");
                require(layout.width() <= 420 && layout.height() <= 260,
                        "Large windows must not enlarge the booster into oversized controls");
                require(layout.rowHeight() >= 18, "Small GUI viewports must retain usable carrier buttons");
                for (int row = 0; row < com.example.mobilecellnetwork.gui.SignalBoosterMenu.PAGE_SIZE; row++) {
                    int bottom = layout.rowTop(row) + layout.rowHeight();
                    require(layout.rowTop(row) >= 100 && bottom < layout.footerY(),
                            "Carrier buttons must not overlap information or footer controls");
                    if (row < 3) require(bottom < layout.rowTop(row + 1), "Carrier rows must remain separated");
                }
                int navigationEnd = 16 + 2 * layout.navigationWidth();
                int refreshStart = layout.width() - layout.navigationWidth() - 12;
                require(navigationEnd + 32 <= refreshStart && layout.footerY() + 20 < layout.height(),
                        "Navigation, page indicator, and rescan must fit without overlapping");
            }
        }
        var option = new com.example.mobilecellnetwork.gui.SignalBoosterMenu.NetworkOption(
                "6786784f-7141-4229-9a57-684e", "Carrier Name", 75);
        require(!option.label().getString().contains(option.id())
                        && option.label().getString().contains("Carrier Name"),
                "Carrier rows must show human-readable names, not oversized UUIDs");
        var packet = new com.example.mobilecellnetwork.network.SignalUpdatePacket(
                com.example.mobilecellnetwork.network.SignalTier.THREE_G, "Carrier Name", "alpha", 1,
                4, BoosterReception.strength(0.75, 0, 32, 0), false,
                0, 32, 1, 0, 0, false, 0);
        com.example.mobilecellnetwork.network.ClientSignalCache.update(packet);
        require(com.example.mobilecellnetwork.network.ClientSignalCache.strength() == 1
                        && com.example.mobilecellnetwork.network.ClientSignalCache.tier()
                        == com.example.mobilecellnetwork.network.SignalTier.THREE_G,
                "HUD cache must show regenerated reception while preserving the separately capped speed tier");
        com.example.mobilecellnetwork.network.ClientSignalCache.clear();
    }

    private static void checkPowerTap() {
        int[] changes = {0};
        PowerTapStorage tap = new PowerTapStorage(() -> changes[0]++);
        var input = tap.port(false);
        var output = tap.port(true);
        require(input.canReceive() && !input.canExtract() && output.canExtract() && !output.canReceive(),
                "Power tap ports must be directional");
        require(output.extractEnergy(100, false) == 0, "Empty tap must not generate energy");
        require(input.receiveEnergy(5000, true) == 1024 && tap.getEnergyStored() == 0 && changes[0] == 0,
                "Tap input simulation must not mutate energy or mark dirty");
        require(input.receiveEnergy(5000, false) == 1024 && changes[0] == 1, "Tap input transfer limit failed");
        require(input.extractEnergy(100, false) == 0 && output.receiveEnergy(100, false) == 0,
                "Tap ports must reject wrong-way transfers");
        require(output.extractEnergy(5000, true) == 1024 && tap.getEnergyStored() == 1024,
                "Tap output simulation changed the buffer");
        EnergyStorage antenna = new EnergyStorage(5000);
        require(PowerWire.transfer(output, antenna) == 256 && tap.getEnergyStored() == 768
                        && antenna.getEnergyStored() == 256, "Tap-to-tower wire transfer must conserve energy");
        tap.load(PowerTapStorage.CAPACITY - 5);
        require(input.receiveEnergy(100, false) == 5 && tap.getEnergyStored() == PowerTapStorage.CAPACITY,
                "Tap must not overfill");
        tap.load(Integer.MAX_VALUE);
        require(tap.getEnergyStored() == PowerTapStorage.CAPACITY, "Tap loaded above capacity");
        tap.load(-1);
        require(tap.getEnergyStored() == 0, "Tap loaded negative energy");
        require(input.receiveEnergy(-100, false) == 0 && output.extractEnergy(-100, false) == 0,
                "Negative transfer must not create or remove energy");
    }

    private static void checkItemModels() throws java.io.IOException {
        for (String item : java.util.List.of("power_cable_spool", "radio_module", "technician_tester")) {
            String path = "/assets/mobilecellnetwork/models/item/" + item + ".json";
            try (var stream = InstallationChecks.class.getResourceAsStream(path)) {
                require(stream != null, "Missing model resource " + path);
                var model = com.google.gson.JsonParser.parseReader(
                        new java.io.InputStreamReader(stream, java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
                if (item.equals("technician_tester")) {
                    checkMeterMesh(model);
                    continue;
                }
                require(model.get("parent").getAsString().equals("minecraft:block/block"),
                        "Technician item still uses a flat placeholder model");
                var elements = model.getAsJsonArray("elements");
                require(elements.size() >= 8, "Technician item lacks modeled details");
                var textures = model.getAsJsonObject("textures");
                for (var value : elements) {
                    var element = value.getAsJsonObject();
                    var from = element.getAsJsonArray("from");
                    var to = element.getAsJsonArray("to");
                    for (int axis = 0; axis < 3; axis++) {
                        double min = from.get(axis).getAsDouble();
                        double max = to.get(axis).getAsDouble();
                        require(min >= 0 && max <= 16 && min < max, "Invalid 3D model element bounds");
                    }
                    var faces = element.getAsJsonObject("faces");
                    require(faces.size() == 6, "3D model element is missing a face");
                    for (var face : faces.entrySet()) {
                        String texture = face.getValue().getAsJsonObject().get("texture").getAsString();
                        require(texture.startsWith("#") && textures.has(texture.substring(1)),
                                "Unresolved model texture: " + texture);
                    }
                }
                for (String view : java.util.List.of("gui", "ground", "fixed", "firstperson_righthand",
                        "firstperson_lefthand", "thirdperson_righthand", "thirdperson_lefthand")) {
                    require(model.getAsJsonObject("display").has(view), "Missing model display context " + view);
                }
                if (item.equals("radio_module")) {
                    var board = elements.get(0).getAsJsonObject();
                    var chip = elements.get(1).getAsJsonObject();
                    require(chip.getAsJsonArray("from").get(2).getAsFloat()
                                    >= board.getAsJsonArray("to").get(2).getAsFloat(),
                            "Radio components must protrude from the south/front face of the board");
                    for (String view : java.util.List.of("gui", "firstperson_righthand", "firstperson_lefthand")) {
                        var rotation = model.getAsJsonObject("display").getAsJsonObject(view).getAsJsonArray("rotation");
                        float mirror = view.endsWith("lefthand") ? -1 : 1;
                        var front = new org.joml.Vector3f(0, 0, 1).rotate(
                                new org.joml.Quaternionf().rotationXYZ(
                                        (float) Math.toRadians(rotation.get(0).getAsFloat()),
                                        mirror * (float) Math.toRadians(rotation.get(1).getAsFloat()),
                                        mirror * (float) Math.toRadians(rotation.get(2).getAsFloat())));
                        require(front.z > 0.9F, "Component face must point toward the camera in " + view);
                    }
                    var fixed = model.getAsJsonObject("display").getAsJsonObject("fixed").getAsJsonArray("rotation");
                    var frameFront = new org.joml.Vector3f(0, 0, 1)
                            .rotateY((float) Math.toRadians(fixed.get(1).getAsDouble()))
                            .rotateY((float) Math.PI);
                    require(frameFront.z > 0.99F, "Item-frame transform must expose electronics, not the PCB back");
                }
            }
        }
        checkTowerItemModel();
    }

    private static void checkTowerItemModel() throws java.io.IOException {
        var monopole = resource("/assets/mobilecellnetwork/models/block/monopole_tower.json");
        var monopoleItem = resource("/assets/mobilecellnetwork/models/item/monopole_tower.json");
        require(monopoleItem.get("parent").getAsString().equals("mobilecellnetwork:block/monopole_tower"),
                "Monopole must have its own slim-pole miniature instead of inheriting the lattice tower");
        var monopoleBoxes = new java.util.ArrayList<net.minecraft.world.phys.AABB>();
        var gui = monopoleItem.getAsJsonObject("display").getAsJsonObject("gui");
        var rotation = gui.getAsJsonArray("rotation");
        var guiRotation = new org.joml.Quaternionf().rotationXYZ(
                (float) Math.toRadians(rotation.get(0).getAsDouble()),
                (float) Math.toRadians(rotation.get(1).getAsDouble()),
                (float) Math.toRadians(rotation.get(2).getAsDouble()));
        for (var value : monopole.getAsJsonArray("elements")) {
            var element = value.getAsJsonObject();
            for (var face : element.getAsJsonObject("faces").entrySet()) {
                var uv = face.getValue().getAsJsonObject().getAsJsonArray("uv");
                require(uv != null && uv.size() == 4,
                        "Tall monopole miniature needs explicit UVs to avoid sampling neighboring atlas textures");
                for (var coordinate : uv) {
                    require(coordinate.getAsDouble() >= 0 && coordinate.getAsDouble() <= 16,
                            "Monopole miniature UVs must stay inside their texture sprite");
                }
            }
            var from = element.getAsJsonArray("from");
            var to = element.getAsJsonArray("to");
            for (int corner = 0; corner < 8; corner++) {
                var point = new org.joml.Vector3f(
                        ((corner & 1) == 0 ? from : to).get(0).getAsFloat() - 8,
                        ((corner & 2) == 0 ? from : to).get(1).getAsFloat() - 8,
                        ((corner & 4) == 0 ? from : to).get(2).getAsFloat() - 8);
                point.mul(gui.getAsJsonArray("scale").get(0).getAsFloat()).rotate(guiRotation);
                point.add(gui.getAsJsonArray("translation").get(0).getAsFloat() + 8,
                        gui.getAsJsonArray("translation").get(1).getAsFloat() + 8, 0);
                require(point.x >= 0 && point.x <= 16 && point.y >= 0 && point.y <= 16,
                        "Entire monopole miniature must fit inside the hotbar/inventory icon");
            }
            monopoleBoxes.add(new net.minecraft.world.phys.AABB(from.get(0).getAsDouble(),
                    from.get(1).getAsDouble(), from.get(2).getAsDouble(), to.get(0).getAsDouble(),
                    to.get(1).getAsDouble(), to.get(2).getAsDouble()));
        }
        var connected = new java.util.ArrayList<net.minecraft.world.phys.AABB>();
        connected.add(monopoleBoxes.getFirst());
        boolean found;
        do {
            found = monopoleBoxes.removeIf(box -> {
                if (connected.stream().noneMatch(other -> other.inflate(0.001).intersects(box))) return false;
                connected.add(box);
                return true;
            });
        } while (found);
        require(monopoleBoxes.isEmpty(), "Monopole miniature contains floating antennas or arms");
        for (String hand : java.util.List.of("firstperson_righthand", "firstperson_lefthand")) {
            var scale = monopoleItem.getAsJsonObject("display").getAsJsonObject(hand).getAsJsonArray("scale");
            require(scale.get(0).getAsDouble() <= 0.4 && scale.get(1).equals(scale.get(0))
                            && scale.get(2).equals(scale.get(0)),
                    "Held monopole must use a compact uniform scale in either hand");
        }
        require(resource("/assets/mobilecellnetwork/blockstates/monopole_tower.json")
                        .getAsJsonObject("variants").has("wide=true"),
                "Monopole base must use the block-entity renderer");
        resource("/assets/mobilecellnetwork/blockstates/monopole_tower_section.json");
        require(resource("/data/mobilecellnetwork/loot_table/blocks/monopole_tower.json").toString()
                        .contains("mobilecellnetwork:monopole_tower"),
                "Breaking a monopole must return its own item");
        var language = resource("/assets/mobilecellnetwork/lang/en_us.json");
        for (var entry : java.util.Map.of("monopole_tower", 1, "tower_tier_1", 2,
                "tower_tier_2", 3, "test_antenna", 4).entrySet()) {
            require(language.get("block.mobilecellnetwork." + entry.getKey()).getAsString()
                            .contains("Tier " + entry.getValue()),
                    "Tower item names must show the four tiers without renaming saved registry IDs");
        }
        var model = resource("/assets/mobilecellnetwork/models/block/test_antenna.json");
        var textures = model.getAsJsonObject("textures");
        require(textures.get("structure").getAsString().equals("minecraft:block/iron_block")
                        && textures.get("dark").getAsString().equals("minecraft:block/gray_concrete")
                        && textures.get("light").getAsString().equals("minecraft:block/light_gray_concrete")
                        && textures.get("panel").getAsString().equals("minecraft:block/white_concrete"),
                "Held tower must match placed tower steel, concrete, and white panels, not legacy diamond textures");
        for (int tier : new int[] {1, 2}) {
            var item = resource("/assets/mobilecellnetwork/models/item/tower_tier_" + tier + ".json");
            require(item.get("parent").getAsString().equals("mobilecellnetwork:item/test_antenna"),
                    "Tier items must inherit the attached miniature tower geometry");
            var palette = item.getAsJsonObject("textures");
            require(palette.get("structure").getAsString().equals(tier == 1
                            ? "minecraft:block/copper_block" : "minecraft:block/iron_block")
                            && palette.get("panel").getAsString().equals(tier == 1
                            ? "minecraft:block/orange_concrete" : "minecraft:block/light_blue_concrete"),
                    "Tier miniature palette must match placed copper/orange or silver/blue materials");
            for (var entry : item.getAsJsonObject("display").entrySet()) {
                var scale = entry.getValue().getAsJsonObject().getAsJsonArray("scale");
                require(Math.abs(scale.get(1).getAsDouble() / scale.get(0).getAsDouble()
                                - (tier == 1 ? 0.5 : 0.75)) < 0.00001,
                        "Tier miniature height must match the small or medium tower");
            }
            require(resource("/assets/mobilecellnetwork/blockstates/tower_tier_" + tier + ".json")
                            .getAsJsonObject("variants").has("wide=true"),
                    "Tier base is missing its wide blockstate model");
            resource("/assets/mobilecellnetwork/blockstates/tower_tier_" + tier + "_section.json");
            var loot = resource("/data/mobilecellnetwork/loot_table/blocks/tower_tier_" + tier + ".json");
            require(loot.toString().contains("mobilecellnetwork:tower_tier_" + tier),
                    "Breaking a tier tower must return its own variant item");
        }
        var elements = model.getAsJsonArray("elements");
        var boxes = new java.util.ArrayList<net.minecraft.world.phys.AABB>();
        for (var value : elements) {
            var element = value.getAsJsonObject();
            var from = element.getAsJsonArray("from");
            var to = element.getAsJsonArray("to");
            boxes.add(new net.minecraft.world.phys.AABB(from.get(0).getAsDouble(), from.get(1).getAsDouble(),
                    from.get(2).getAsDouble(), to.get(0).getAsDouble(), to.get(1).getAsDouble(), to.get(2).getAsDouble()));
        }
        java.util.Set<Integer> attached = new java.util.HashSet<>();
        attached.add(0);
        boolean changed;
        do {
            changed = false;
            for (int i = 0; i < boxes.size(); i++) {
                if (attached.contains(i)) continue;
                var box = boxes.get(i);
                if (attached.stream().anyMatch(j -> boxes.get(j).inflate(0.001).intersects(box))) {
                    attached.add(i);
                    changed = true;
                }
            }
        } while (changed);
        require(attached.size() == boxes.size(), "Tower item contains floating components");
        var cap = boxes.stream().filter(box -> box.minY == 21 && box.maxY == 24).findFirst().orElseThrow();
        require(boxes.stream().filter(box -> box.minY < 5 && box.maxY >= 22
                        && box.getXsize() <= 1.1 && box.getZsize() <= 1.1
                        && box.intersects(cap)).count() == 4,
                "Held tower top needs four visible supports reaching from the base to the cap");
    }

    private static void checkMeterReading() throws java.io.IOException {
        var reading = new MeterReading(MeterReading.Mode.TOWER, 9876, 100000, 73, "THREE_G",
                "MISSING_WIRE", 256, 200);
        require(MeterReading.load(reading.save()).equals(reading), "Meter sample must survive stack synchronization");
        var rf = (net.minecraft.network.chat.contents.TranslatableContents) reading.lines(200).get(2).getContents();
        require(rf.getArgs()[0].equals(73) && rf.getArgs()[1].equals("3G"),
                "LCD must show the measured percentage and human-readable radio tier");
        require(!reading.stale(220) && reading.stale(241) && reading.stale(100),
                "Meter must distinguish fresh samples from stale or previous-world data");
        require(MeterReading.load(new CompoundTag()).mode() == MeterReading.Mode.IDLE,
                "Existing testers without sample data must show an explicit idle display");
        CompoundTag invalid = reading.save();
        invalid.putString("mode", "BROKEN");
        require(MeterReading.load(invalid).status().equals("INVALID_READING"),
                "Malformed samples must display an error, not plausible readings");
        CompoundTag bounded = reading.save();
        bounded.putInt("energy", Integer.MAX_VALUE);
        bounded.putInt("signal", -5);
        require(MeterReading.load(bounded).energy() == 100000 && MeterReading.load(bounded).signal() == -1,
                "Meter must clamp corrupt stored values");
        var language = resource("/assets/mobilecellnetwork/lang/en_us.json");
        for (var mode : MeterReading.Mode.values()) {
            require(language.has("meter.mobilecellnetwork.mode." + mode.name().toLowerCase(java.util.Locale.ROOT)),
                    "Missing meter mode translation");
        }
        for (String status : java.util.List.of("READY", "MISSING_MODULE", "MISSING_WIRE", "AWAITING_TEST",
                "POWERED", "NO_POWER", "RADIO_ONLY", "NO_SERVICE", "DENIED", "AIM_CABINET",
                "OPEN_CABINET", "BYPASS", "DISABLED", "INVALID_READING")) {
            var sample = new MeterReading(MeterReading.Mode.TOWER, 1, 100, 50, "3G", status, 1, 100);
            require(sample.lines(100).size() == 6, "Meter screen layout must remain six lines");
            for (var line : sample.lines(100)) {
                var content = (net.minecraft.network.chat.contents.TranslatableContents) line.getContents();
                require(language.has(content.getKey()), "Untranslated meter line: " + content.getKey());
            }
        }
    }

    private static void checkFixedCablePorts() throws java.io.IOException {
        require(WiredDevice.class.isAssignableFrom(
                        com.example.mobilecellnetwork.blockentity.PhoneChargerBlockEntity.class)
                        && !Serviceable.class.isAssignableFrom(
                        com.example.mobilecellnetwork.blockentity.PhoneChargerBlockEntity.class),
                "Spool-powered chargers must not acquire radio-module or commissioning requirements");
        var models = java.util.Map.of(
                "copper_antenna", CablePorts.Socket.ANTENNA,
                "iron_antenna", CablePorts.Socket.ANTENNA,
                "diamond_antenna", CablePorts.Socket.ANTENNA,
                "signal_booster", CablePorts.Socket.BOOSTER,
                "phone_charger", CablePorts.Socket.CHARGER,
                "power_tap", CablePorts.Socket.POWER_TAP);
        for (var entry : models.entrySet()) {
            var socket = entry.getValue();
            var point = socket.mount().scale(16);
            boolean mounted = false;
            for (var value : resource("/assets/mobilecellnetwork/models/block/" + entry.getKey() + ".json")
                    .getAsJsonArray("elements")) {
                var part = value.getAsJsonObject();
                var a = part.getAsJsonArray("from");
                var b = part.getAsJsonArray("to");
                var box = new net.minecraft.world.phys.AABB(a.get(0).getAsDouble(), a.get(1).getAsDouble(),
                        a.get(2).getAsDouble(), b.get(0).getAsDouble(), b.get(1).getAsDouble(), b.get(2).getAsDouble());
                if (!box.inflate(0.00001).contains(point)) continue;
                var axis = socket.facing().getAxis();
                double face = socket.facing().getAxisDirection() == Direction.AxisDirection.POSITIVE
                        ? axis.choose(box.maxX, box.maxY, box.maxZ) : axis.choose(box.minX, box.minY, box.minZ);
                mounted |= Math.abs(axis.choose(point.x, point.y, point.z) - face) < 0.00001;
            }
            require(mounted, "Cable socket must attach to a model surface, not float: " + entry.getKey());
        }
        var cabinet = com.example.mobilecellnetwork.block.TowerCabinets.BOUNDS.getFirst();
        var mount = CablePorts.Socket.TOWER.mount();
        require(mount.x == cabinet.maxX && cabinet.inflate(0.00001).contains(mount),
                "Tower cable gland must attach to the fixed cabinet side, clear of the moving front door");
        var source = new BlockPos(-8, 64, 3);
        var destination = new BlockPos(-5, 65, 7);
        for (var socket : CablePorts.Socket.values()) {
            var start = CablePorts.Socket.POWER_TAP.at(source);
            var finish = socket.at(destination);
            require(Math.abs(socket.point().distanceTo(socket.mount()) - 0.065) < 0.000001,
                    "Cable end must coincide with the outer end of its socket");
            require(WirePath.valid(start, java.util.List.of(), finish), "Direct socket route rejected");
            var clips = java.util.List.of(start.add(0, 1, 0), finish.add(0, 1, 0));
            var wired = new Installation();
            wired.connect(source, Direction.UP, start, clips, finish);
            var saved = new Installation();
            saved.load(wired.save());
            require(saved.sourcePoint().equals(start) && saved.targetPoint().equals(finish)
                            && saved.snaps().equals(clips) && PowerWire.canConnect(saved, destination),
                    "Fixed sockets and manual routing must survive synchronization/reload");
        }
        require(resource("/assets/mobilecellnetwork/lang/en_us.json")
                        .has("message.mobilecellnetwork.technician.charger_connected"),
                "Charger wiring must explain that commissioning is not needed");
    }

    private static void checkWireRoutes() {
        var from = new net.minecraft.world.phys.Vec3(-4, 64, 0);
        var snap = new net.minecraft.world.phys.Vec3(-4, 68, 0);
        var to = new net.minecraft.world.phys.Vec3(-1, 68, 0);
        var points = new java.util.ArrayList<>(java.util.List.of(snap));
        require(WirePath.length(from, points, to) == 7, "Charge full routed length, not endpoint distance");
        require(WirePath.valid(from, points, to), "Valid snapped route rejected");
        require(WirePath.valid(from, java.util.List.of(), from.add(32, 0, 0)), "Exact 32-block route rejected");
        require(!WirePath.valid(from, points, from.add(32, 0, 0)), "Detours must not bypass length limit");
        require(!WirePath.valid(from, java.util.List.of(), from), "Zero-length cable accepted");
        require(!WirePath.valid(from, java.util.List.of(), new net.minecraft.world.phys.Vec3(Double.NaN, 0, 0)),
                "Nonfinite route accepted");
        require(!WirePath.valid(from, java.util.Collections.nCopies(WirePath.MAX_SNAPS + 1, snap), to),
                "Snap point count is not bounded");
        Installation routed = new Installation();
        routed.begin(UUID.randomUUID());
        routed.installModule();
        routed.connect(BlockPos.containing(from), Direction.UP, from, points, to);
        points.clear();
        require(routed.snaps().size() == 1, "Route must own an immutable copy of snap points");
        require(routed.commission(true), "Snapped installation must be commissionable");
        Installation copy = new Installation();
        copy.load(routed.save());
        require(copy.ready() && copy.sourcePoint().equals(from) && copy.targetPoint().equals(to)
                        && copy.snaps().equals(java.util.List.of(snap)),
                "Exact cable attachments and routing must survive save/client sync");
        require(PowerWire.canConnect(copy, BlockPos.containing(to)), "Routed cable must deliver power");
        copy.disconnect();
        require(copy.sourcePoint() == null && copy.targetPoint() == null && copy.snaps().isEmpty(),
                "Cutting cable must clear all geometry");
        copy.connect(BlockPos.ZERO, Direction.UP);
        copy.load(new CompoundTag());
        require(copy.sourcePoint() == null && copy.targetPoint() == null && copy.snaps().isEmpty(),
                "Loading old equipment must not retain a previous cable route");
        var languagePath = "/assets/mobilecellnetwork/lang/en_us.json";
        try {
            var language = resource(languagePath);
            for (String key : java.util.List.of("snapped", "snap_solid", "snap_limit", "snap_duplicate")) {
                require(language.has("message.mobilecellnetwork.technician." + key), "Missing snap feedback " + key);
            }
        } catch (java.io.IOException exception) {
            throw new java.io.UncheckedIOException(exception);
        }
    }

    private static void checkMeterMesh(com.google.gson.JsonObject model) throws java.io.IOException {
        require(model.get("loader").getAsString().equals("neoforge:obj"), "Meter must use a rounded mesh like phones");
        require(!model.get("automatic_culling").getAsBoolean(), "Rounded meter must not use block-face culling");
        for (String view : java.util.List.of("gui", "ground", "fixed", "firstperson_righthand",
                "firstperson_lefthand", "thirdperson_righthand", "thirdperson_lefthand")) {
            require(model.getAsJsonObject("display").has(view), "Missing meter display context " + view);
        }
        String base = "/assets/mobilecellnetwork/";
        String obj;
        try (var stream = InstallationChecks.class.getResourceAsStream(base + "models/item/technician_tester.obj")) {
            require(stream != null, "Missing meter mesh");
            obj = new String(stream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        }
        require(obj.lines().filter(line -> line.startsWith("f ")).count() > 2000, "Meter lacks rounded detail");
        for (String button : java.util.List.of("hold", "range", "light")) {
            require(obj.contains("usemtl " + button + "\n"), "Raised meter button is missing its visible legend");
        }
        java.util.List<double[]> vertices = new java.util.ArrayList<>();
        java.util.List<double[]> lcdFront = new java.util.ArrayList<>();
        java.util.Map<String, java.util.List<double[]>> groups = new java.util.HashMap<>();
        String group = "body";
        String material = "";
        int lcdFaces = 0;
        int vertexCount = 0;
        for (String line : obj.lines().toList()) {
            if (line.startsWith("v ")) {
                var coordinates = line.substring(2).split(" ");
                double[] vertex = new double[3];
                for (int axis = 0; axis < 3; axis++) {
                    vertex[axis] = Double.parseDouble(coordinates[axis]);
                    require(Double.isFinite(vertex[axis]), "Invalid meter vertex");
                }
                vertices.add(vertex);
                groups.computeIfAbsent(group, key -> new java.util.ArrayList<>()).add(vertex);
                vertexCount++;
            } else if (line.startsWith("g ")) {
                group = line.substring(2);
            } else if (line.startsWith("usemtl ")) {
                material = line.substring(7);
            } else if (line.startsWith("f ") && material.equals("lcd")) {
                lcdFaces++;
                for (String reference : line.substring(2).split(" ")) {
                    int index = Integer.parseInt(reference.split("/")[0]) - 1;
                    require(index >= 0 && index < vertexCount, "Invalid meter face index");
                    if (Math.abs(vertices.get(index)[2] * 16 - 10.75) < 0.001) {
                        lcdFront.add(vertices.get(index));
                    }
                }
            }
        }
        require(lcdFaces > 32, "Missing recessed LCD mesh");
        require(lcdFront.size() > 32, "LCD front surface is missing");
        double minX = lcdFront.stream().mapToDouble(v -> v[0] * 16).min().orElseThrow();
        double maxX = lcdFront.stream().mapToDouble(v -> v[0] * 16).max().orElseThrow();
        double minY = lcdFront.stream().mapToDouble(v -> v[1] * 16).min().orElseThrow();
        double maxY = lcdFront.stream().mapToDouble(v -> v[1] * 16).max().orElseThrow();
        require(minX < 4.6 && maxX > 4.6 + 105 * 0.004 * 16
                        && minY < 13.1 - 66 * 0.004 * 16 && maxY > 13.1 && 10.75 < 10.78,
                "Live digits and the full charge gauge must fit inside the rounded LCD");
        for (String color : java.util.List.of("rubber", "red")) {
            double probeX = color.equals("rubber") ? 2.25 : 13.75;
            double jackX = color.equals("rubber") ? 9 : 11;
            var handle = meshBounds(groups.get(color + "_probe_handle"));
            for (double y : new double[] {4.3, 8.5}) {
                var clip = meshBounds(groups.get(color + "_clip_" + y));
                require(clip.intersects(handle), color + " probe is floating outside its retaining clips");
                require(clip.intersects(new net.minecraft.world.phys.AABB(3, 3, 6.7, 13, 12, 10.36)),
                        color + " retaining clip does not reach the meter boot");
                require(clip.contains(probeX, y, 8.5), "Clip does not enclose the probe handle");
            }
            var lead = meshBounds(groups.get(color + "_lead"));
            var plug = meshBounds(groups.get(color + "_plug"));
            require(lead.intersects(handle) && lead.intersects(plug), color + " lead has disconnected ends");
            require(plug.minZ < 10.81 && plug.maxZ > 10.81
                            && plug.contains(jackX, 2.25, 10.81),
                    color + " plug must seat inside its input jack, not hover in front of it");
        }
        String mtl;
        try (var stream = InstallationChecks.class.getResourceAsStream(base + "models/item/technician_tester.mtl")) {
            require(stream != null, "Missing meter material library");
            mtl = new String(stream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        }
        for (String line : mtl.lines().filter(line -> line.startsWith("map_Kd ")).toList()) {
            String texture = line.substring(7).replace("mobilecellnetwork:", "");
            try (var stream = InstallationChecks.class.getResourceAsStream(base + "textures/" + texture + ".png")) {
                require(stream != null, "Missing meter material " + texture);
                var image = javax.imageio.ImageIO.read(stream);
                require(image != null && image.getWidth() >= 16, "Invalid meter texture");
            }
        }
    }

    private static net.minecraft.world.phys.AABB meshBounds(java.util.List<double[]> vertices) {
        require(vertices != null && !vertices.isEmpty(), "Missing meter component geometry");
        return new net.minecraft.world.phys.AABB(
                vertices.stream().mapToDouble(v -> v[0] * 16).min().orElseThrow(),
                vertices.stream().mapToDouble(v -> v[1] * 16).min().orElseThrow(),
                vertices.stream().mapToDouble(v -> v[2] * 16).min().orElseThrow(),
                vertices.stream().mapToDouble(v -> v[0] * 16).max().orElseThrow(),
                vertices.stream().mapToDouble(v -> v[1] * 16).max().orElseThrow(),
                vertices.stream().mapToDouble(v -> v[2] * 16).max().orElseThrow());
    }

    private static void checkPushOnlyConverter() {
        PowerTapStorage tap = new PowerTapStorage(() -> {});
        EnergyStorage equipment = new EnergyStorage(1000);
        // Electro Energetics pushes to an adjacent FE input; its capability cannot be drained by a wire.
        EnergyStorage converterPort = new EnergyStorage(1000, 1000, 1000, 1000) {
            @Override
            public int extractEnergy(int amount, boolean simulate) { return 0; }

            @Override
            public int receiveEnergy(int amount, boolean simulate) { return 0; }
        };
        require(converterPort.canExtract() && PowerWire.transfer(converterPort, equipment) == 0,
                "A push-only converter must not be treated as an extractable wire supply");
        int converterBuffer = 1000;
        int accepted = tap.port(false).receiveEnergy(converterBuffer, false);
        converterBuffer -= accepted;
        require(accepted == 1000 && converterBuffer == 0 && tap.getEnergyStored() == 1000,
                "Converter-style adjacent FE push must fill the power tap");
        require(PowerWire.transfer(tap.port(true), equipment) == 256,
                "Buffered converter power must reach equipment through a spool wire");
        require(converterBuffer + tap.getEnergyStored() + equipment.getEnergyStored() == 1000,
                "Converter-to-tap-to-equipment transfers must conserve energy");
    }

    private static void checkTapResources() throws java.io.IOException {
        var state = resource("/assets/mobilecellnetwork/blockstates/power_tap.json");
        require(state.getAsJsonObject("variants").getAsJsonObject("").get("model").getAsString()
                        .equals("mobilecellnetwork:block/power_tap"), "Tap blockstate points at wrong model");
        var item = resource("/assets/mobilecellnetwork/models/item/power_tap.json");
        require(item.get("parent").getAsString().equals("mobilecellnetwork:block/power_tap"),
                "Tap item must use distribution cabinet model");
        var model = resource("/assets/mobilecellnetwork/models/block/power_tap.json");
        require(model.getAsJsonArray("elements").size() >= 6, "Tap model lacks cabinet/terminal details");
        var recipe = resource("/data/mobilecellnetwork/recipe/power_tap.json");
        require(recipe.getAsJsonObject("result").get("id").getAsString().equals("mobilecellnetwork:power_tap"),
                "Tap recipe must produce registered tap");
        var loot = resource("/data/mobilecellnetwork/loot_table/blocks/power_tap.json");
        require(loot.getAsJsonArray("pools").get(0).getAsJsonObject().getAsJsonArray("entries")
                        .get(0).getAsJsonObject().get("name").getAsString().equals("mobilecellnetwork:power_tap"),
                "Breaking tap must drop the tap item");
        var language = resource("/assets/mobilecellnetwork/lang/en_us.json");
        require(language.has("block.mobilecellnetwork.power_tap") && language.has("diagnostic.mobilecellnetwork.survey")
                        && language.has("diagnostic.mobilecellnetwork.source_missing"),
                "Missing tap/tester translations");
    }

    private static com.google.gson.JsonObject resource(String path) throws java.io.IOException {
        try (var stream = InstallationChecks.class.getResourceAsStream(path)) {
            require(stream != null, "Missing packaged resource: " + path);
            return com.google.gson.JsonParser.parseReader(
                    new java.io.InputStreamReader(stream, java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
