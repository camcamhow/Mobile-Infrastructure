package com.example.mobilecellnetwork.block;

import java.util.ArrayList;
import java.util.List;
import java.lang.reflect.Proxy;
import java.util.function.Function;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.resources.ResourceLocation;
import org.joml.Vector3f;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class TestTowerGeometryChecks {
    public static void main(String[] args) throws Exception {
        if (args.length > 0 && args[0].equals("sections-first")) {
            net.neoforged.fml.loading.LoadingModList.of(List.of(), List.of(), List.of(), List.of(), java.util.Map.of());
            net.minecraft.SharedConstants.tryDetectVersion();
            net.minecraft.server.Bootstrap.bootStrap();
            ((net.minecraft.core.MappedRegistry<?>) net.minecraft.core.registries.BuiltInRegistries.BLOCK).unfreeze();
            for (TowerVariant variant : TowerVariant.values()) {
                var section = new TestTowerSectionBlock(
                        net.minecraft.world.level.block.state.BlockBehaviour.Properties.of(), variant);
                for (var state : section.getStateDefinition().getPossibleStates()) {
                    int expected = TowerWorkLights.emission(variant, state.getValue(TestTowerBlock.WIDE),
                            state.getValue(TestTowerSectionBlock.LIT),
                            state.getValue(TestTowerSectionBlock.X_OFFSET) - TestTowerGeometry.RADIUS,
                            state.getValue(TestTowerSectionBlock.OFFSET),
                            state.getValue(TestTowerSectionBlock.Z_OFFSET) - TestTowerGeometry.RADIUS);
                    require(state.getLightEmission() == expected,
                            "Section construction must cache correct light before subclass fields are initialized: " + variant);
                }
            }
            System.out.println("PASS: actual tower section construction and cached lighting for every tier/state");
            return;
        }
        Shapes.empty();
        long geometryStart = System.nanoTime();
        if (args.length > 0 && args[0].equals("lights-first")) {
            require(TowerWorkLights.FIXTURES.size() == 8,
                    "Lighting must initialize before collision geometry during block registration");
        }
        TestTowerGeometry.prepare();
        System.out.printf("Tower geometry preparation (mod loading): %.1f ms%n",
                (System.nanoTime() - geometryStart) / 1_000_000.0);
        if (args.length > 0 && args[0].equals("outlines")) {
            checkOutlineCaching();
            return;
        }
        geometryStart = System.nanoTime();
        var prepared = TestTowerGeometry.shape(0, 0, 0);
        TestTowerGeometry.prepare();
        require(TestTowerGeometry.shape(0, 0, 0) == prepared,
                "Repeated preparation must reuse existing collision shapes");
        List<VoxelShape> world = new ArrayList<>();
        for (int y = 0; y < 20; y++) {
            for (int x = -2; x <= 2; x++) {
                for (int z = -2; z <= 2; z++) {
                    world.add(TestTowerGeometry.shape(x, y, z).move(x, y, z));
                    require(TestTowerGeometry.shape(x, y, z) == TestTowerGeometry.shape(x, y, z),
                            "Collision query rebuilt its shape");
                    for (AABB box : TestTowerGeometry.shape(x, y, z).toAabbs()) {
                        require(box.minX >= 0 && box.minY >= 0 && box.minZ >= 0
                                        && box.maxX <= 1 && box.maxY <= 1 && box.maxZ <= 1,
                                "Section collision extends into neighboring block");
                    }
                }
            }
        }
        System.out.printf("Prepared tower's first 500 collision queries and validation: %.1f ms%n",
                (System.nanoTime() - geometryStart) / 1_000_000.0);
        require(TestTowerGeometry.onLadder(0.5, 1, -1.8), "Valid ladder position rejected");
        require(!TestTowerGeometry.onLadder(0.5, 19.7, -1.8), "Climbing above ladder");
        require(!TestTowerGeometry.onLadder(0.5, -0.1, -1.8), "Climbing below foundation");
        require(!TestTowerGeometry.onLadder(1.2, 1, -1.8), "Climbing beside ladder");
        require(!TestTowerGeometry.onLadder(0.5, 1, -1.2), "Climbing inside mast");
        for (double x : new double[] {0.35, 0.5, 0.65}) {
            for (double y = 0.5; y < 19.4; y += 0.15) {
                AABB player = player(y, -1.801).move(x - 0.5, 0, 0);
                require(close(Shapes.collide(Direction.Axis.Y, player, world, -0.15), -0.15),
                        "Descending ladder snag at " + x + ", " + y);
            }
            for (int deck : TestTowerGeometry.PLATFORM_LEVELS) {
                AABB entry = player(deck + 0.2, -1.801).move(x - 0.5, 0, 0);
                double sideways = 1.3 - x;
                AABB landing = walk(entry, world, Direction.Axis.X, sideways, "Off-center ladder side exit");
                AABB exit = walk(landing, world, Direction.Axis.Z, 0.65, "Off-center platform entrance");
                walk(exit, world, Direction.Axis.Z, -0.65, "Return to transfer landing");
                walk(landing, world, Direction.Axis.X, -sideways, "Return to ladder");
            }
        }
        for (double y = TestTowerGeometry.FOUNDATION_TOP; y < 19.4; y += 0.05) {
            AABB player = player(y, -1.801);
            require(close(Shapes.collide(Direction.Axis.Y, player, world, 0.2), 0.2),
                    "Ladder blocked at " + y);
        }
        for (int deck : TestTowerGeometry.PLATFORM_LEVELS) {
            double feet = deck - 0.3;
            double velocity = 0.2;
            boolean entered = false;
            // Start below the landing, rather than placing the player on top of the deck.
            for (int tick = 0; tick < 60; tick++) {
                AABB player = player(feet, -1.801);
                double rise = Shapes.collide(Direction.Axis.Y, player, world, velocity);
                feet += rise;
                player = player(feet, -1.801);
                double forward = Shapes.collide(Direction.Axis.Z, player, world, 0.1);
                if (feet >= deck + TestTowerGeometry.DECK_THICKNESS) {
                    AABB landing = walk(player, world, Direction.Axis.X, 0.8, "Climbing side exit");
                    AABB landed = walk(landing, world, Direction.Axis.Z, 0.65, "Climbing platform entry");
                    double down = Shapes.collide(Direction.Axis.Y, landed, world, -1);
                    require(down > -0.5, "Platform floor missing at " + deck);
                    entered = true;
                    break;
                }
                boolean wall = forward < 0.002;
                velocity = wall ? 0.2 : (velocity - 0.08) * 0.98;
            }
            require(entered, "Cannot climb onto platform at " + deck);
            AABB railProbe = new AABB(1.9, deck + 0.125, -2.101, 2.5, deck + 1.925, -1.501);
            require(Shapes.collide(Direction.Axis.Z, railProbe, world, 0.8) < 0.01,
                    "North safety railing missing");
        }
        List<AABB> boxes = world.stream().flatMap(shape -> shape.toAabbs().stream()).toList();
        for (double wireX : new double[] {0.92, 1.02, 1.12}) {
            for (double x = 0.82; x < wireX + 0.045; x += 0.01) {
                AABB probe = new AABB(x, 0.96, 0.66, x + 0.001, 0.961, 0.661);
                require(boxes.stream().anyMatch(box -> box.intersects(probe)),
                        "Cable bend disconnected from base cabinet");
            }
            for (double y = 0.96; y < 19.15; y += 0.1) {
                AABB probe = new AABB(wireX + 0.02, y, 0.66, wireX + 0.021, y + 0.001, 0.661);
                require(boxes.stream().anyMatch(box -> box.intersects(probe)),
                        "Cable run disconnected from upper junction");
            }
        }
        for (int deck : TestTowerGeometry.PLATFORM_LEVELS) {
            AABB approachingMount = new AABB(0.2, deck + 0.125, 1.8, 0.8, deck + 1.925, 2.4);
            require(Shapes.collide(Direction.Axis.Z, approachingMount, world, -0.8) > -0.2,
                    "Can walk through balcony antenna mount at " + deck);
        }
        for (TestTowerGeometry.Beam beam : TestTowerGeometry.BEAMS) {
            for (double fraction : new double[] {0.1, 0.5, 0.9}) {
                double x = beam.x1() + (beam.x2() - beam.x1()) * fraction;
                double y = beam.y1() + (beam.y2() - beam.y1()) * fraction;
                double z = beam.z1() + (beam.z2() - beam.z1()) * fraction;
                if (y < 0 || y >= 20) continue;
                AABB probe = new AABB(x - 0.001, y - 0.001, z - 0.001, x + 0.001, y + 0.001, z + 0.001);
                boolean solid = boxes.stream().anyMatch(box -> box.intersects(probe));
                require(solid, "Rendered cross-bar has no collision at " + probe);
            }
        }
        long ladderSupports = TestTowerGeometry.BEAMS.stream().filter(beam ->
                beam.y1() == beam.y2() && beam.x1() == beam.x2()
                        && (Math.abs(beam.x1() - 0.25F) < 0.001F || Math.abs(beam.x1() - 0.75F) < 0.001F)
                        && Math.abs(beam.z1() - (TestTowerGeometry.LADDER_Z + 0.05F)) < 0.001F
                        && beam.z2() > beam.z1()).count();
        require(ladderSupports >= 8, "Ladder needs paired standoffs tied into the tower frame");
        for (var support : TestTowerGeometry.BEAMS) {
            if (Math.abs(support.z1() - (TestTowerGeometry.LADDER_Z + 0.05F)) > 0.001F
                    || support.x1() != support.x2() || support.y1() != support.y2()) continue;
            float half = TestTowerGeometry.mastHalfWidth(support.y1());
            require(TestTowerGeometry.BEAMS.stream().anyMatch(tie ->
                            Math.abs(tie.y1() - support.y1()) < 0.001F && tie.y1() == tie.y2()
                                    && Math.abs(tie.z1() - support.z2()) < 0.001F && tie.z1() == tie.z2()
                                    && Math.abs(tie.x1() - (0.5F - half)) < 0.001F
                                    && Math.abs(tie.x2() - (0.5F + half)) < 0.001F),
                    "Ladder standoff must meet a crossmember that reaches both mast legs");
            require(TestTowerGeometry.PARTS.stream().anyMatch(part ->
                            part.material() == TestTowerGeometry.Material.LADDER
                                    && part.bounds().inflate(0.001).contains(
                                            support.x1(), support.y1(), support.z1())),
                    "Ladder standoff must overlap the actual ladder rail");
        }
        for (float y : TestTowerGeometry.CAGE_LEVELS) {
            float half = TestTowerGeometry.mastHalfWidth(y);
            for (int side : new int[] {-1, 1}) {
                require(TestTowerGeometry.BEAMS.stream().anyMatch(beam ->
                                Math.abs(beam.x1() - (0.5F + side * 0.6F)) < 0.001F
                                        && beam.y1() == y && beam.y2() == y
                                        && Math.abs(beam.z1() + 1.4F) < 0.001F
                                        && Math.abs(beam.x2() - (0.5F + side * half)) < 0.001F
                                        && Math.abs(beam.z2() - (0.5F - half)) < 0.001F),
                        "Every safety cage hoop must attach to a mast leg at both ends");
            }
        }
        for (int cabinet = 0; cabinet < TowerCabinets.BOUNDS.size(); cabinet++) {
            AABB bounds = TowerCabinets.BOUNDS.get(cabinet);
            var front = new net.minecraft.world.phys.Vec3((bounds.minX + bounds.maxX) / 2,
                    (bounds.minY + bounds.maxY) / 2, bounds.minZ);
            require(TowerCabinets.at(front) == cabinet, "Cabinet door is not an interaction target");
            require(TowerCabinets.at(front.add(0, 0, 0.1)) == cabinet, "Cabinet interior is not accessible");
            require(boxes.stream().anyMatch(box -> box.contains(front.add(0, 0, 0.01))),
                    "Cabinet has no ray-pickable collision");
        }
        require(TowerCabinets.at(new net.minecraft.world.phys.Vec3(0.5, 19.8, 0.5)) == -1,
                "Beacon must not open a cabinet");
        require(TowerCabinets.at(new net.minecraft.world.phys.Vec3(0.5, 17.5, -1.5)) == -1,
                "Ladder must not open a cabinet");
        checkMaintenanceAccess(world);
        checkPanelClearance(world);
        checkCabinetFacing();
        checkWorkLights();
        checkVariants();
        checkRenderer();
        checkCableRenderer();
        checkSocketRendering();
        checkExposedSurfaces();
        checkMeterGaugeBuffer();
        checkDoorwayAndMounts();
        System.out.println("PASS: four tower heights/ranges/radio caps, connected monopole, Tier 2 computer deck, fixed socket meshes, "
                + "climb/descent, ladder limits, off-center landing entry/return, "
                + "base/cabinet access, inward rendered doors/screens, separated front braces, terminal sightlines, "
                + "corner lighting, solid braces/rails, "
                + "cached local shapes, tower/cable render bounds, meter gauge buffer reacquisition");
    }

    private static void checkOutlineCaching() throws Exception {
        var renderer = Class.forName("com.example.mobilecellnetwork.client.TowerOutlineRenderer");
        var cache = renderer.getDeclaredMethod("edges", VoxelShape.class);
        cache.setAccessible(true);
        var render = renderer.getDeclaredMethod("render", VoxelShape.class, PoseStack.class, VertexConsumer.class);
        render.setAccessible(true);
        for (TowerVariant variant : TowerVariant.values()) {
            double worst = 0;
            for (int[] cell : new int[][] {{-1, Math.min(8, variant.height() - 1), -1},
                    {1, Math.min(8, variant.height() - 1), 1},
                    {-1, Math.min(8, variant.height() - 1), 1}, {1, Math.min(8, variant.height() - 1), -1},
                    {-1, 3, -1}, {1, 3, 1}, {-1, 3, 1}, {1, 3, -1},
                    {0, 0, 0}, {variant.radius(), variant.height() - 6, -1}, {0, variant.height() - 1, 0}}) {
                var shape = TestTowerGeometry.shape(variant, cell[0], cell[1], cell[2]);
                List<?> cached = (List<?>) cache.invoke(null, shape);
                var expected = new ArrayList<Vector3f>();
                shape.forAllEdges((a, b, c, d, e, f) -> {
                    expected.add(new Vector3f((float) a, (float) b, (float) c));
                    expected.add(new Vector3f((float) d, (float) e, (float) f));
                });
                require(expected.size() == cached.size() * 2, "Cached outline must preserve every vanilla edge");
                var actual = new ArrayList<Vector3f>();
                VertexConsumer out = (VertexConsumer) Proxy.newProxyInstance(VertexConsumer.class.getClassLoader(),
                        new Class<?>[] {VertexConsumer.class}, (proxy, method, values) -> {
                            if (method.getName().equals("addVertex") && values.length == 4) {
                                var point = new Vector3f((float) values[1], (float) values[2], (float) values[3]);
                                ((PoseStack.Pose) values[0]).pose().transformPosition(point);
                                actual.add(point);
                            }
                            return method.getReturnType() == VertexConsumer.class ? proxy : null;
                        });
                var pose = new PoseStack();
                pose.translate(3, 5, -2);
                render.invoke(null, shape, pose, out);
                require(actual.size() == expected.size(), "Outline render must emit both vertices of each edge");
                for (int i = 0; i < expected.size(); i++) {
                    require(actual.get(i).distance(expected.get(i).add(3, 5, -2)) < 0.00001,
                            "Cached highlight must match vanilla geometry and camera-relative transform");
                }
                long start = System.nanoTime();
                for (int i = 0; i < 10000; i++) {
                    require(cache.invoke(null, shape) == cached,
                            "Repeated highlighting must reuse edges without scanning the voxel grid");
                }
                double perFrame = (System.nanoTime() - start) / 10_000_000_000.0;
                worst = Math.max(worst, perFrame);
                require(perFrame < 0.1, "Cached edge lookup must average under 0.1 ms");
            }
            System.out.printf("%s cached highlight lookup: worst %.6f ms/frame; exact vanilla edges preserved%n",
                    variant, worst);
        }
    }

    private static void checkMaintenanceAccess(List<VoxelShape> world) {
        AABB ground = new AABB(2.7, TestTowerGeometry.FOUNDATION_TOP + 0.03, -0.7,
                3.3, TestTowerGeometry.FOUNDATION_TOP + 1.83, -0.1);
        ground = walk(ground, world, Direction.Axis.X, -2.5, "East-side framed service entrance");
        require(TowerCabinets.BOUNDS.get(0).minZ - ground.maxZ < 0.6, "Base cabinet is not within working reach");
        require(TowerCabinets.terminalAt(TowerCabinets.TERMINALS.get(0).getCenter()) == 0,
                "Base terminal cannot be selected");
        var eye = new net.minecraft.world.phys.Vec3(ground.getCenter().x, ground.minY + 1.62, ground.getCenter().z);
        AABB screenBounds = TowerCabinets.TERMINALS.get(0);
        require(clearRay(world, eye, new net.minecraft.world.phys.Vec3(
                screenBounds.getCenter().x, screenBounds.getCenter().y, screenBounds.minZ)),
                "Ground computer screen is obstructed");
        AABB baseCabinet = TowerCabinets.BOUNDS.get(0);
        require(clearRay(world, eye, new net.minecraft.world.phys.Vec3(
                baseCabinet.getCenter().x, baseCabinet.getCenter().y, baseCabinet.minZ)),
                "Ground power cabinet door is obstructed");
        walk(ground, world, Direction.Axis.X, 2.5, "East-side service exit");
        for (int i = 1; i < TowerCabinets.BOUNDS.size(); i++) {
            int deck = TestTowerGeometry.PLATFORM_LEVELS.get(i - 1);
            AABB route = player(deck + TestTowerGeometry.DECK_THICKNESS, -1.801);
            route = walk(route, world, Direction.Axis.X, 0.8, "Side exit from ladder at " + deck);
            route = walk(route, world, Direction.Axis.Z, 0.85, "Reach inward-facing cabinet at " + deck);
            AABB terminal = TowerCabinets.TERMINALS.get(i);
            require(TowerCabinets.withinTerminalRange(route.getCenter(), terminal.getCenter()),
                    "Platform terminal is out of menu reach");
            require(TowerCabinets.terminalAt(terminal.getCenter()) == i, "Terminal cannot be selected");
            eye = new net.minecraft.world.phys.Vec3(route.getCenter().x, route.minY + 1.62, route.getCenter().z);
            var screen = TowerCabinets.frontCenter(i, true);
            var door = TowerCabinets.frontCenter(i, false);
            require(eye.x < door.x && Math.abs(eye.z - door.z) < 0.05,
                    "Technician must stand in front of inward-facing cabinet, not behind the railing");
            require(clearRay(world, eye, screen), "Mast obstructs platform computer screen at " + deck);
            require(clearRay(world, eye, door), "Mast obstructs radio cabinet door at " + deck);
            route = walk(route, world, Direction.Axis.Z, -0.85, "Return to ladder landing at " + deck);
            walk(route, world, Direction.Axis.X, -0.8, "Side entry onto ladder at " + deck);
        }
        var terminal = TowerCabinets.TERMINALS.get(1).getCenter();
        require(!TowerCabinets.withinTerminalRange(terminal.add(0, -14, 0), terminal),
                "Platform terminal should close when player returns to ground");
    }

    private static void checkCabinetFacing() throws Exception {
        List<Vector3f> labels = new ArrayList<>();
        List<Vector3f> screens = new ArrayList<>();
        Vector3f[] vertex = {null};
        VertexConsumer out = (VertexConsumer) Proxy.newProxyInstance(VertexConsumer.class.getClassLoader(),
                new Class<?>[] {VertexConsumer.class}, (proxy, method, values) -> {
                    if (method.getName().equals("addVertex") && values.length == 4) {
                        vertex[0] = new Vector3f((float) values[1], (float) values[2], (float) values[3]);
                        ((PoseStack.Pose) values[0]).pose().transformPosition(vertex[0]);
                    } else if (method.getName().equals("setColor") && values.length == 1) {
                        int color = (int) values[0];
                        if (color == 0xFF4499DD) labels.add(vertex[0]);
                        if (color == 0xFF14344D) screens.add(vertex[0]);
                    }
                    return method.getReturnType() == VertexConsumer.class ? proxy : null;
                });
        Class<?> renderer = Class.forName("com.example.mobilecellnetwork.client.TestAntennaRenderer");
        var cabinets = renderer.getDeclaredMethod("renderCabinets", VertexConsumer.class, PoseStack.class,
                int.class, int.class, int.class, boolean.class, boolean.class, boolean.class);
        cabinets.setAccessible(true);
        cabinets.invoke(null, out, new PoseStack(), 15728880, 0, 0, false, false, false);
        var terminals = renderer.getDeclaredMethod("renderTerminals", VertexConsumer.class, PoseStack.class,
                int.class, int.class);
        terminals.setAccessible(true);
        terminals.invoke(null, out, new PoseStack(), 15728880, 0);
        require(labels.size() == 48 && screens.size() == 72, "Facing test missed cabinet labels or LCD surfaces");
        for (int i = 1; i < TowerCabinets.BOUNDS.size(); i++) {
            AABB cabinet = TowerCabinets.BOUNDS.get(i);
            for (Vector3f point : labels.subList((i - 1) * 24, i * 24)) {
                require(point.x < cabinet.minX && point.z > cabinet.minZ && point.z < cabinet.maxZ,
                        "Rendered cabinet front must face the mast-side walkway");
            }
            AABB terminal = TowerCabinets.TERMINALS.get(i);
            for (Vector3f point : screens.subList(i * 24, (i + 1) * 24)) {
                require(point.x < terminal.minX && point.z > terminal.minZ && point.z < terminal.maxZ,
                        "Rendered screen must face inward along with its interaction bounds");
            }
            require(TowerCabinets.terminalAt(TowerCabinets.frontCenter(i, true)) == i,
                    "Rotated computer front must remain clickable");
        }
        List<TestTowerGeometry.Beam> front = TestTowerGeometry.BEAMS.stream()
                .filter(beam -> Math.abs(beam.x2() - beam.x1()) > 2
                        && beam.y1() < 0.4 && beam.y2() > 1.8 && beam.z1() < -1).toList();
        require(front.size() == 2, "Front lattice check must inspect both lower crossing diagonals");
        require(Math.abs(front.get(0).z1() - front.get(1).z1()) > 0.06,
                "Crossing front braces must not share a surface depth");
    }

    private static AABB walk(AABB player, List<VoxelShape> world, Direction.Axis axis, double distance, String message) {
        double actual = Shapes.collide(axis, player, world, distance);
        require(close(actual, distance), message + " blocked: moved " + actual + " instead of " + distance);
        return switch (axis) {
            case X -> player.move(actual, 0, 0);
            case Y -> player.move(0, actual, 0);
            case Z -> player.move(0, 0, actual);
        };
    }

    private static boolean clearRay(List<VoxelShape> world, net.minecraft.world.phys.Vec3 from,
                                    net.minecraft.world.phys.Vec3 target) {
        var end = from.lerp(target, 0.98);
        return world.stream().flatMap(shape -> shape.toAabbs().stream()).noneMatch(box -> box.clip(from, end).isPresent());
    }

    private static void checkWorkLights() {
        require(TowerWorkLights.FIXTURES.size() == 8, "Keep only the original eight under-deck lamps");
        var cells = new java.util.HashSet<net.minecraft.core.BlockPos>();
        for (TowerWorkLights.Fixture fixture : TowerWorkLights.FIXTURES) {
            var cell = fixture.cell();
            require(cells.add(cell), "Lamp cells must be distinct");
            require(fixture.lens().maxY <= fixture.housing().minY + 0.00001, "Lens must face down");
            require(TowerWorkLights.emission(true, true, cell.getX(), cell.getY(), cell.getZ()) == 15,
                    "Powered fixture must emit real world light");
            require(TowerWorkLights.emission(true, false, cell.getX(), cell.getY(), cell.getZ()) == 0,
                    "Unpowered fixture must not emit light");
            require(TowerWorkLights.emission(false, true, cell.getX(), cell.getY(), cell.getZ()) == 0,
                    "Legacy narrow sections must not emit platform light");
            require(TestTowerGeometry.PARTS.stream().anyMatch(part ->
                            (part.material() == TestTowerGeometry.Material.STEEL
                                    || part.material() == TestTowerGeometry.Material.DARK)
                                    && part.bounds().inflate(0.001).intersects(fixture.housing()))
                            || TestTowerGeometry.BEAMS.stream().anyMatch(beam ->
                                    fixture.housing().clip(
                                            new net.minecraft.world.phys.Vec3(beam.x1(), beam.y1(), beam.z1()),
                                            new net.minecraft.world.phys.Vec3(beam.x2(), beam.y2(), beam.z2())).isPresent()),
                    "Work light must mount to a deck, railing, or mast rather than float");
        }

        require(TowerWorkLights.emission(true, true, 0, 13, 0) == 0, "Non-lamp sections must not emit light");
        require(TowerWorkLights.UPDATE_CELLS.size() == 24
                        && new java.util.HashSet<>(TowerWorkLights.UPDATE_CELLS).size() == 24,
                "Update original lamps and clear all sixteen former floodlight cells");
        int retired = 0;
        for (var cell : TowerWorkLights.UPDATE_CELLS) {
            if (cells.contains(cell)) continue;
            retired++;
            require(TowerWorkLights.emission(true, true, cell.getX(), cell.getY(), cell.getZ()) == 0,
                    "Removed floodlights must not emit even with a saved LIT state");
        }
        require(retired == 16, "Must clear all removed floodlights on existing towers");
    }

    private static void checkVariants() {
            int[] heights = {10, 15, 20, 6};
            int[] ranges = {256, 512, 1024, 128};
            var caps = new com.example.mobilecellnetwork.network.SignalTier[] {
                    com.example.mobilecellnetwork.network.SignalTier.THREE_G,
                    com.example.mobilecellnetwork.network.SignalTier.FOUR_G_PLUS,
                    com.example.mobilecellnetwork.network.SignalTier.FIVE_G_PLUS,
                    com.example.mobilecellnetwork.network.SignalTier.TWO_G};
            for (TowerVariant variant : TowerVariant.values()) {
                require(variant.height() == heights[variant.ordinal()] && variant.range() == ranges[variant.ordinal()]
                                && variant.maxSignalTier() == caps[variant.ordinal()],
                        "Tower variant height, range, or radio tier cap is wrong: " + variant);
                if (variant.monopole()) {
                    checkMonopole();
                    continue;
                }
                require(variant.y(20) == variant.height() && variant.y(1.4) == 1.4
                                && close(variant.y(17) - variant.y(14), 3),
                        "Variant must preserve ground cabinet size and full-height platform clearance");
                for (double y = 0; y <= 20; y += 0.1) {
                    require(close(variant.modelY(variant.y(y)), y), "Cabinet/ladder coordinate transform is not reversible");
                }
                List<VoxelShape> world = new ArrayList<>();
                for (int y = 0; y < variant.height(); y++) {
                    for (int x = -2; x <= 2; x++) {
                        for (int z = -2; z <= 2; z++) {
                            VoxelShape shape = TestTowerGeometry.shape(variant, x, y, z);
                            require(shape == TestTowerGeometry.shape(variant, x, y, z),
                                    "Variant collision must be cached, not rebuilt while climbing");
                            world.add(shape.move(x, y, z));
                            for (AABB box : shape.toAabbs()) {
                                require(box.minX >= 0 && box.minY >= 0 && box.minZ >= 0
                                                && box.maxX <= 1 && box.maxY <= 1 && box.maxZ <= 1,
                                        "Variant section collision extends beyond its block");
                            }
                        }
                    }
                }
                AABB ground = new AABB(2.7, TestTowerGeometry.FOUNDATION_TOP + 0.03, -0.7,
                        3.3, TestTowerGeometry.FOUNDATION_TOP + 1.83, -0.1);
                ground = walk(ground, world, Direction.Axis.X, -2.5, "Variant ground service entrance");
                walk(ground, world, Direction.Axis.X, 2.5, "Variant ground service exit");
                for (double y = TestTowerGeometry.FOUNDATION_TOP; y < variant.height() - 0.6; y += 0.1) {
                    require(close(Shapes.collide(Direction.Axis.Y, player(y, -1.801), world, 0.1), 0.1),
                            "Variant ladder ascent blocked: " + variant + " at " + y);
                    require(close(Shapes.collide(Direction.Axis.Y, player(y, -1.801), world, -0.1), -0.1)
                                    || y < TestTowerGeometry.FOUNDATION_TOP + 0.1,
                            "Variant ladder descent blocked: " + variant + " at " + y);
                }
                for (int originalDeck : TestTowerGeometry.PLATFORM_LEVELS) {
                    double deck = variant.y(originalDeck);
                    AABB entry = player(deck + 0.2, -1.801);
                    AABB landing = walk(entry, world, Direction.Axis.X, 0.8, "Variant ladder side exit");
                    walk(landing, world, Direction.Axis.Z, 0.65, "Variant platform entry");
                    require(Shapes.collide(Direction.Axis.Y, landing, world, -0.5) > -0.2,
                            "Variant transfer landing must retain floor support");
                }
                for (int cabinet = 0; cabinet < TowerCabinets.BOUNDS.size(); cabinet++) {
                    var canonical = TowerCabinets.BOUNDS.get(cabinet).getCenter();
                    require(TowerCabinets.at(variant, variant.toModel(variant.toWorld(canonical)))
                                    == (variant.hasCabinet(cabinet) ? cabinet : -1),
                            "Variant cabinet hit must resolve to the displayed cabinet");
                    var terminal = TowerCabinets.TERMINALS.get(cabinet).getCenter();
                    require(TowerCabinets.terminalAt(variant, variant.toModel(variant.toWorld(terminal)))
                                    == (variant.hasTerminal(cabinet) ? cabinet : -1),
                            "Variant terminal hit must resolve to the displayed terminal");
                }
                if (variant.separateComputerDeck()) {
                    var eye = new net.minecraft.world.phys.Vec3(1.05, variant.y(14.125) + 1.62, -0.95);
                    require(clearRay(world, eye, variant.toWorld(TowerCabinets.frontCenter(1, true))),
                            "Tier 2 lower computer must remain reachable from the lower deck");
                    require(TestTowerGeometry.parts(variant).stream().anyMatch(
                                    part -> part.material() == TestTowerGeometry.Material.CABINET
                                            && part.bounds().equals(TowerCabinets.BOUNDS.get(1))),
                            "Lower computer must retain its cabinet box");
                    require(world.stream().anyMatch(shape -> shape.toAabbs().stream()
                                    .anyMatch(box -> box.contains(2.1, variant.y(14.6), -0.95))),
                            "Restored lower cabinet must have matching physical collision");
                }
                var lights = new java.util.HashSet<net.minecraft.core.BlockPos>();
                for (var fixture : TowerWorkLights.FIXTURES) {
                    var cell = TowerWorkLights.cell(variant, fixture);
                    require(lights.add(cell) && cell.getY() < variant.height(),
                            "Variant work lights must occupy eight distinct in-bounds cells");
                    require(TowerWorkLights.emission(variant, true, true, cell.getX(), cell.getY(), cell.getZ()) == 15
                                    && TowerWorkLights.emission(variant, true, false, cell.getX(), cell.getY(), cell.getZ()) == 0,
                            "Variant lamp emission must match the relocated fixture");
                }
            }
        }

    private static void checkMonopole() {
        require(TowerVariant.MONOPOLE.displayTier() == 1 && TowerVariant.TIER_1.displayTier() == 2
                        && TowerVariant.TIER_2.displayTier() == 3 && TowerVariant.TIER_3.displayTier() == 4,
                "Display tier shift must preserve internal profiles and existing save IDs");
        var world = new ArrayList<VoxelShape>();
        for (int y = 0; y < 6; y++) {
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    var shape = MonopoleGeometry.shape(x, y, z);
                    require(shape == TestTowerGeometry.shape(TowerVariant.MONOPOLE, x, y, z),
                            "Monopole collision must be shared and cached");
                    for (var box : shape.toAabbs()) {
                        require(box.minX >= 0 && box.minY >= 0 && box.minZ >= 0
                                        && box.maxX <= 1 && box.maxY <= 1 && box.maxZ <= 1,
                                "Monopole section collision must fit its block");
                    }
                    world.add(shape.move(x, y, z));
                }
            }
        }
        for (double y = 0.4; y < 5.6; y += 0.1) {
            AABB player = player(y, -0.251).move(-0.751, 0, 0.751);
            require(TowerVariant.MONOPOLE.onLadder(-0.251, y, 0.5),
                    "Monopole climbing lane must register as ladder");
            require(close(Shapes.collide(Direction.Axis.Y, player, world, 0.1), 0.1)
                            && close(Shapes.collide(Direction.Axis.Y, player, world, -0.05), -0.05),
                    "Monopole climbing lane blocked by pole, cabinet, or antenna arms at " + y);
        }
        require(!TowerVariant.MONOPOLE.onLadder(-0.251, 5.7, 0.5)
                        && !TowerVariant.MONOPOLE.onLadder(-0.251, 3, 1.0)
                        && !TowerVariant.MONOPOLE.onLadder(0.5, 3, -0.251),
                "Monopole ladder limits must exclude the tip and sides");
        var cabinet = TowerCabinets.bounds(TowerVariant.MONOPOLE, 0, false);
        var computer = TowerCabinets.bounds(TowerVariant.MONOPOLE, 0, true);
        require(MonopoleGeometry.PARTS.stream()
                        .filter(part -> part.material() == MonopoleGeometry.Material.POLE)
                        .noneMatch(part -> part.bounds().intersects(cabinet)
                                || part.bounds().intersects(computer)),
                "Monopole pole must stay behind the cabinet interior and computer");
        require(TowerCabinets.at(TowerVariant.MONOPOLE, cabinet.getCenter()) == 0
                        && TowerCabinets.terminalAt(TowerVariant.MONOPOLE, computer.getCenter()) == 0,
                "Moved cabinet and screen hit targets must match their geometry");
        var mount = com.example.mobilecellnetwork.infrastructure.CablePorts.Socket.MONOPOLE.mount();
        require(close(mount.x, cabinet.maxX) && mount.z > cabinet.minZ && mount.z < cabinet.maxZ,
                "Monopole cable socket must remain attached to the moved cabinet side");
        require(MonopoleGeometry.PARTS.stream()
                        .filter(part -> part.material() == MonopoleGeometry.Material.STEEL)
                        .noneMatch(part -> part.bounds().intersects(cabinet)
                                && part.bounds().minX < cabinet.minX
                                || part.bounds().intersects(computer)
                                        && part.bounds().maxY > computer.minY + 0.001),
                "Monopole ladder rails, rungs, and standoffs must clear the cabinet and computer");
        require(MonopoleGeometry.PARTS.stream()
                        .filter(part -> part.material() == MonopoleGeometry.Material.POLE)
                        .allMatch(part -> part.bounds().maxY < MonopoleGeometry.BEACON.minY),
                "Monopole pole must end below the beacon rather than draw through its top lens");
        require(MonopoleGeometry.PARTS.stream().allMatch(part -> part.bounds().maxY < MonopoleGeometry.BEACON.maxY),
                "No static pole cap or support may share the beacon's top surface");
        require(MonopoleGeometry.PARTS.stream().filter(p -> p.material() == MonopoleGeometry.Material.PANEL).count() == 6,
                "Monopole must have two three-sector antenna arrays");
        var connected = new ArrayList<AABB>();
        connected.add(MonopoleGeometry.PARTS.getFirst().bounds());
        var remaining = new ArrayList<>(MonopoleGeometry.PARTS.stream().map(MonopoleGeometry.Part::bounds).toList());
        remaining.add(cabinet);
        remaining.add(computer);
        for (var beam : MonopoleGeometry.ARMS) {
            remaining.add(new AABB(Math.min(beam.x1(), beam.x2()), Math.min(beam.y1(), beam.y2()),
                    Math.min(beam.z1(), beam.z2()), Math.max(beam.x1(), beam.x2()),
                    Math.max(beam.y1(), beam.y2()), Math.max(beam.z1(), beam.z2())).inflate(beam.thickness()));
        }
        boolean changed;
        do {
            changed = remaining.removeIf(box -> {
                if (connected.stream().noneMatch(other -> other.inflate(0.001).intersects(box))) return false;
                connected.add(box);
                return true;
            });
        } while (changed);
        require(remaining.isEmpty(), "Monopole contains floating pole sections, arms, panels, or ladder supports");
        var eye = new net.minecraft.world.phys.Vec3(0.5, 1.93, -1.0);
        require(clearRay(world, eye, TowerCabinets.frontCenter(0, true).add(TowerCabinets.MONOPOLE_OFFSET))
                        && clearRay(world, eye, TowerCabinets.frontCenter(0, false).add(TowerCabinets.MONOPOLE_OFFSET)),
                "Monopole base computer and combined cabinet must be reachable from the ground");
        require(TowerWorkLights.updateCells(TowerVariant.MONOPOLE).isEmpty(),
                "Monopole must not inherit floating platform lamps");
        require(TowerVariant.TIER_1.hasCabinet(1) && TowerVariant.TIER_1.hasCabinet(2)
                        && TowerVariant.TIER_1.hasTerminal(1) && !TowerVariant.TIER_1.hasTerminal(2),
                "Tier 2 must put the radio cabinet above and the computer alone below");
        require(TestTowerGeometry.parts(TowerVariant.TIER_1).stream().anyMatch(
                        p -> p.material() == TestTowerGeometry.Material.CABINET
                                && p.bounds().equals(TowerCabinets.BOUNDS.get(1))),
                "Tier 2 lower computer must retain its cabinet box");
        require(TestTowerGeometry.beams(TowerVariant.TIER_1).stream().noneMatch(
                        b -> TestTowerGeometry.PANEL_MOUNTS.contains(b) && b.y1() < 17),
                "Tier 2 must not retain floating lower antenna mounts");
    }

    private static void checkDoorwayAndMounts() {
        var steel = TestTowerGeometry.PARTS.stream()
                .filter(part -> part.material() == TestTowerGeometry.Material.STEEL)
                .map(TestTowerGeometry.Part::bounds).toList();
        double foundation = TestTowerGeometry.FOUNDATION_TOP;
        require(steel.stream().anyMatch(box -> box.minY <= foundation && box.maxY > foundation
                        && box.minZ <= -1.1 && box.maxZ >= 0.55 && box.minX <= 2 && box.maxX >= 2.1),
                "Doorway threshold must extend under both jambs");
        for (double z : new double[] {-1.05, 0.5}) {
            require(steel.stream().anyMatch(box -> box.contains(2.05, foundation + 0.02, z)
                            && box.getXsize() >= 0.3 && box.getZsize() >= 0.19),
                    "Each jamb needs a foundation-mounted base shoe");
            require(TestTowerGeometry.BEAMS.stream().anyMatch(beam ->
                            Math.abs(beam.x1() - 2.05) < 0.001 && Math.abs(beam.z1() - z) < 0.001
                                    && Math.abs(beam.y1() - foundation - 2.45) < 0.001
                                    && Math.abs(beam.x2() - 0.5 - TestTowerGeometry.mastHalfWidth(beam.y2())) < 0.001),
                    "Doorway header must tie into a real east mast leg");
        }
        for (var beam : TestTowerGeometry.BEAMS) {
            if (Math.abs(beam.thickness() - 0.045F) > 0.0001) continue;
            double bottom = Math.min(beam.y1(), beam.y2());
            double top = Math.max(beam.y1(), beam.y2());
            require(Math.abs((bottom - foundation) / 1.5 - Math.rint((bottom - foundation) / 1.5)) < 0.001,
                    "Access opening left a diagonal stub with a floating lower end");
            require(Math.abs(top - Math.min(bottom + 1.5, 19.4)) < 0.001,
                    "Access opening left a diagonal stub with a floating upper end");
        }
        require(TestTowerGeometry.PANEL_MOUNTS.size() == 24, "Missing antenna rear mounting assemblies");
        for (int sector = 0; sector < 3; sector++) {
            double angle = Math.toRadians(sector * 120);
            for (int index = sector * 8; index < sector * 8 + 8; index++) {
                var beam = TestTowerGeometry.PANEL_MOUNTS.get(index);
                for (var point : List.of(new net.minecraft.world.phys.Vec3(beam.x1(), beam.y1(), beam.z1()),
                        new net.minecraft.world.phys.Vec3(beam.x2(), beam.y2(), beam.z2()))) {
                    double x = (point.x - 0.5) * Math.cos(angle) - (point.z - 0.5) * Math.sin(angle);
                    double z = (point.x - 0.5) * Math.sin(angle) + (point.z - 0.5) * Math.cos(angle);
                    require(Math.abs(x) <= 0.351 && z >= -0.001 && z <= 1.151,
                            "Panel mounting bar protrudes past the panel or through its front");
                    int deck = point.y < 17 ? 14 : 17;
                    require(point.y >= deck + 0.6 && point.y <= deck + 1.2,
                            "Panel mounting bar misses the rear bracket vertically");
                }
            }
        }
    }

    private static void checkPanelClearance(List<VoxelShape> world) throws Exception {
        List<AABB> panels = new ArrayList<>();
        List<Vector3f> vertices = new ArrayList<>();
        VertexConsumer out = (VertexConsumer) Proxy.newProxyInstance(VertexConsumer.class.getClassLoader(),
                new Class<?>[] {VertexConsumer.class}, (proxy, method, values) -> {
                    if (method.getName().equals("addVertex") && values.length == 4) {
                        var pose = (PoseStack.Pose) values[0];
                        Vector3f point = new Vector3f((float) values[1], (float) values[2], (float) values[3]);
                        pose.pose().transformPosition(point);
                        vertices.add(point);
                        if (vertices.size() == 24) {
                            double minX = vertices.stream().mapToDouble(v -> v.x).min().orElseThrow();
                            double minY = vertices.stream().mapToDouble(v -> v.y).min().orElseThrow();
                            double minZ = vertices.stream().mapToDouble(v -> v.z).min().orElseThrow();
                            double maxX = vertices.stream().mapToDouble(v -> v.x).max().orElseThrow();
                            double maxY = vertices.stream().mapToDouble(v -> v.y).max().orElseThrow();
                            double maxZ = vertices.stream().mapToDouble(v -> v.z).max().orElseThrow();
                            panels.add(new AABB(minX, minY, minZ, maxX, maxY, maxZ));
                            vertices.clear();
                        }
                    }
                    return method.getReturnType() == VertexConsumer.class ? proxy : null;
                });
        var render = Class.forName("com.example.mobilecellnetwork.client.TestAntennaRenderer")
                .getDeclaredMethod("widePanels", VertexConsumer.class, PoseStack.class, int.class, int.class,
                        float.class, boolean.class);
        render.setAccessible(true);
        for (boolean front : new boolean[] {false, true}) {
            render.invoke(null, out, new PoseStack(), 15728880, 0, 20F, front);
        }
        require(panels.size() == 24, "Panel overlap check did not inspect every rendered panel and backplate");
        for (AABB panel : panels) {
            for (AABB cabinet : TowerCabinets.BOUNDS) {
                require(!panel.intersects(cabinet.inflate(0.04)), "Rendered antenna panel overlaps cabinet: " + panel);
            }
            for (AABB terminal : TowerCabinets.TERMINALS) {
                require(!panel.intersects(terminal.inflate(0.04)), "Rendered antenna panel overlaps computer: " + panel);
            }
        }
        List<VoxelShape> complete = new ArrayList<>(world);
        panels.forEach(panel -> complete.add(Shapes.create(panel)));
        checkMaintenanceAccess(complete);
        for (int deck : TestTowerGeometry.PLATFORM_LEVELS) {
            AABB ladder = player(deck + TestTowerGeometry.DECK_THICKNESS, -1.801);
            require(Shapes.collide(Direction.Axis.Z, ladder, complete, 0.6) < 0.01,
                    "Ladder should be solid: platform access must use the side landing, not pass through rungs");
            AABB landing = walk(ladder, complete, Direction.Axis.X, 0.8, "Rendered-panel ladder exit");
            require(Shapes.collide(Direction.Axis.Y, landing, complete, -0.5) > -0.01,
                    "Transfer landing has no supporting floor");
            walk(landing, complete, Direction.Axis.Z, 0.65, "Rendered-panel platform entrance");
        }
        checkTierTwoPanels(out, panels);
    }

    private static void checkTierTwoPanels(VertexConsumer out, List<AABB> panels) throws Exception {
        var variantRender = Class.forName("com.example.mobilecellnetwork.client.TestAntennaRenderer")
                .getDeclaredMethod("widePanels", VertexConsumer.class, PoseStack.class, int.class, int.class,
                        float.class, boolean.class, TowerVariant.class);
        variantRender.setAccessible(true);
        for (var variant : List.of(TowerVariant.TIER_1, TowerVariant.TIER_2)) {
            panels.clear();
            for (boolean front : new boolean[] {false, true}) {
                variantRender.invoke(null, out, new PoseStack(), 15728880, 0, 20F, front, variant);
            }
            require(panels.size() == 12 && panels.stream().allMatch(panel -> panel.minY >= 17),
                    "Tiers 2 and 3 must render all six panels and six back radios on the top platform only");
            require(variant.hasCabinet(1) && variant.hasCabinet(2)
                            && variant.hasTerminal(1) && !variant.hasTerminal(2),
                    "Tiers 2 and 3 must retain only the computer below and the radio cabinet above");
            require(TestTowerGeometry.parts(variant).stream().anyMatch(
                            p -> p.material() == TestTowerGeometry.Material.CABINET
                                    && p.bounds().equals(TowerCabinets.BOUNDS.get(1))),
                    "Separated computer deck must retain the cabinet beneath its screen");
            require(TestTowerGeometry.beams(variant).stream().noneMatch(
                            b -> TestTowerGeometry.PANEL_MOUNTS.contains(b) && b.y1() < 17),
                    "Separated computer deck must have no leftover lower antenna mounts");
        }
    }

    private static void checkRenderer() throws Exception {
        ResourceLocation[] activeTexture = new ResourceLocation[1];
        int[] vertices = {0};
        Function<ResourceLocation, VertexConsumer> textures = texture -> {
            activeTexture[0] = texture;
            return (VertexConsumer) Proxy.newProxyInstance(VertexConsumer.class.getClassLoader(),
                    new Class<?>[] {VertexConsumer.class}, (proxy, method, values) -> {
                        require(activeTexture[0] == texture, "Renderer reused a flushed buffer");
                        if (method.getName().equals("addVertex") && values.length == 4) {
                            PoseStack.Pose transform = (PoseStack.Pose) values[0];
                            Vector3f point = new Vector3f((float) values[1], (float) values[2], (float) values[3]);
                            transform.pose().transformPosition(point);
                            require(Float.isFinite(point.x) && Float.isFinite(point.y) && Float.isFinite(point.z),
                                    "Invalid rendered vertex");
                            require(point.x >= -2.001 && point.x <= 3.001 && point.z >= -2.001
                                            && point.z <= 3.001 && point.y >= -0.001 && point.y <= 20.001,
                                    "Tower detail outside checked footprint: " + point);
                            if (texture.getPath().equals("textures/block/gray_concrete.png")
                                    && Math.abs(point.x - 0.5F) < 0.05F && Math.abs(point.z - 0.5F) < 0.05F) {
                                require(point.y <= 19.4001F, "Feed mast overlaps beacon support");
                            }
                            vertices[0]++;
                        }
                        return method.getReturnType() == VertexConsumer.class ? proxy : null;
                    });
        };
        var render = Class.forName("com.example.mobilecellnetwork.client.TestAntennaRenderer")
                .getDeclaredMethod("renderWideTower", float.class, boolean.class, PoseStack.class,
                        Function.class, int.class, int.class, int.class, boolean.class, boolean.class, boolean.class,
                        boolean.class);
        render.setAccessible(true);
        PoseStack pose = new PoseStack();
        for (int mask = 0; mask <= TowerCabinets.ALL_OPEN; mask++) {
            for (boolean populated : new boolean[] {false, true}) {
                render.invoke(null, 20F, populated, pose, textures, 15728880, 0, mask,
                        populated, populated, populated, populated);
                require(pose.clear(), "Unbalanced render pose");
            }
        }
        require(vertices[0] > 10000, "Detail model unexpectedly incomplete");
        var renderer = Class.forName("com.example.mobilecellnetwork.client.TestAntennaRenderer");
        var dynamic = renderer.getDeclaredMethod("renderWideDynamic", float.class, boolean.class, PoseStack.class,
                Function.class, int.class, int.class, int.class, boolean.class, boolean.class, boolean.class,
                boolean.class);
        dynamic.setAccessible(true);
        var fixed = renderer.getDeclaredMethod("renderWideStatic", float.class, PoseStack.class,
                Function.class, int.class, int.class);
        fixed.setAccessible(true);
        vertices[0] = 0;
        fixed.invoke(null, 20F, pose, textures, 15728880, 0);
        int cachedVertices = vertices[0];
        vertices[0] = 0;
        dynamic.invoke(null, 20F, true, pose, textures, 15728880, 0, TowerCabinets.ALL_OPEN,
                true, true, true, true);
        int dynamicVertices = vertices[0];
        require(cachedVertices > dynamicVertices * 4,
                "GPU mesh caching must eliminate at least 80% of per-frame tower vertex construction");
        vertices[0] = 0;
        render.invoke(null, 20F, true, pose, textures, 15728880, 0, TowerCabinets.ALL_OPEN,
                true, true, true, true);
        require(vertices[0] == cachedVertices + dynamicVertices,
                "Cached and dynamic passes must preserve every original vertex");
        require(pose.clear(), "Cached/dynamic rendering leaked transforms");
        System.out.printf("Tower mesh cache: %,d static vertices reused; %,d dynamic vertices per frame%n",
                cachedVertices, dynamicVertices);
        var adapter = Class.forName("com.example.mobilecellnetwork.client.TowerVertexConsumer")
                .getDeclaredConstructor(VertexConsumer.class, TowerVariant.class, PoseStack.Pose.class);
        adapter.setAccessible(true);
        var variantStatic = renderer.getDeclaredMethod("renderVariantStatic", TowerVariant.class, float.class,
                PoseStack.class, Function.class, int.class, int.class);
        variantStatic.setAccessible(true);
        var variantDynamic = renderer.getDeclaredMethod("renderVariantDynamic", TowerVariant.class, boolean.class,
                PoseStack.class, Function.class, int.class, int.class, int.class,
                boolean.class, boolean.class, boolean.class, boolean.class);
        variantDynamic.setAccessible(true);
        for (TowerVariant variant : TowerVariant.values()) {
            double[] bounds = {Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY};
            int[] count = {0};
            var parent = new PoseStack();
            parent.translate(3, 5, -2);
            Function<ResourceLocation, VertexConsumer> mapped = texture -> {
                VertexConsumer target = (VertexConsumer) Proxy.newProxyInstance(VertexConsumer.class.getClassLoader(),
                        new Class<?>[] {VertexConsumer.class}, (proxy, method, values) -> {
                            if (method.getName().equals("addVertex") && values.length == 4) {
                                var point = new Vector3f((float) values[1], (float) values[2], (float) values[3]);
                                ((PoseStack.Pose) values[0]).pose().transformPosition(point);
                                require(Float.isFinite(point.x) && Float.isFinite(point.y) && Float.isFinite(point.z)
                                                && point.x >= 0.999 && point.x <= 6.001
                                                && point.z >= -4.001 && point.z <= 1.001,
                                        "Variant render must preserve the tower footprint and parent transform");
                                bounds[0] = Math.min(bounds[0], point.y);
                                bounds[1] = Math.max(bounds[1], point.y);
                                count[0]++;
                            }
                            return method.getReturnType() == VertexConsumer.class ? proxy : null;
                        });
                try {
                    return (VertexConsumer) adapter.newInstance(target, variant, parent.last());
                } catch (ReflectiveOperationException exception) {
                    throw new AssertionError(exception);
                }
            };
            variantStatic.invoke(null, variant, 20F, new PoseStack(), mapped, 15728880, 0);
            int staticCount = count[0];
            variantDynamic.invoke(null, variant, true, new PoseStack(), mapped, 15728880, 0,
                    TowerCabinets.ALL_OPEN, true, true, true, true);
            require(count[0] > staticCount && staticCount > 1000
                            && close(bounds[0], 5) && close(bounds[1], variant.height() + 5),
                    "Static and dynamic render mapping must retain all geometry at the exact variant height");
            if (variant == TowerVariant.TIER_3) {
                require(count[0] == cachedVertices + dynamicVertices,
                        "Existing medium/tall tower geometry must remain unchanged after tier renumbering");
            }
        }
    }

    private static void checkCableRenderer() throws Exception {
        var render = Class.forName("com.example.mobilecellnetwork.client.PowerWireRenderer")
                .getDeclaredMethod("renderPath", VertexConsumer.class, PoseStack.class,
                        net.minecraft.world.phys.Vec3.class, List.class, net.minecraft.world.phys.Vec3.class,
                        int.class, int.class);
        render.setAccessible(true);
        for (var end : List.of(new net.minecraft.world.phys.Vec3(32, 0, 0),
                new net.minecraft.world.phys.Vec3(-12, 8, 3),
                new net.minecraft.world.phys.Vec3(0, 20, 0),
                new net.minecraft.world.phys.Vec3(0, -20, 0),
                new net.minecraft.world.phys.Vec3(0.5, 0, 0))) {
            AABB bounds = new AABB(Math.min(0, end.x) - 0.1, Math.min(0, end.y) - 0.1,
                    Math.min(0, end.z) - 0.1, Math.max(0, end.x) + 0.1,
                    Math.max(0, end.y) + 0.1, Math.max(0, end.z) + 0.1);
            int[] vertices = {0};
            VertexConsumer out = (VertexConsumer) Proxy.newProxyInstance(VertexConsumer.class.getClassLoader(),
                    new Class<?>[] {VertexConsumer.class}, (proxy, method, values) -> {
                        if (method.getName().equals("addVertex") && values.length == 4) {
                            var transform = (PoseStack.Pose) values[0];
                            var point = new Vector3f((float) values[1], (float) values[2], (float) values[3]);
                            transform.pose().transformPosition(point);
                            require(Float.isFinite(point.x) && Float.isFinite(point.y) && Float.isFinite(point.z),
                                    "Cable has non-finite vertex");
                            require(bounds.contains(point.x, point.y, point.z), "Cable leaves straight route bounds");
                            vertices[0]++;
                        }
                        return method.getReturnType() == VertexConsumer.class ? proxy : null;
                    });
            PoseStack pose = new PoseStack();
            render.invoke(null, out, pose, net.minecraft.world.phys.Vec3.ZERO, List.of(), end, 15728880, 0xFFFFFFFF);
            require(vertices[0] == 3 * 24, "Direct cable must have a solid span and two connector collars");
            vertices[0] = 0;
            render.invoke(null, out, pose, net.minecraft.world.phys.Vec3.ZERO,
                    List.of(end.scale(0.5)), end, 15728880, 0xFFFFFFFF);
            require(vertices[0] == 5 * 24, "Snapped cable must render both spans and all three attachments");
            require(pose.clear(), "Cable renderer leaked a transform");
        }
    }

    private static void checkSocketRendering() throws Exception {
        var render = Class.forName("com.example.mobilecellnetwork.client.PowerWireRenderer")
                .getDeclaredMethod("socket", VertexConsumer.class, PoseStack.class, net.minecraft.core.BlockPos.class,
                        com.example.mobilecellnetwork.infrastructure.CablePorts.Socket.class, int.class);
        render.setAccessible(true);
        var origin = new net.minecraft.core.BlockPos(-7, 64, 13);
        for (var socket : com.example.mobilecellnetwork.infrastructure.CablePorts.Socket.values()) {
            int[] vertices = {0};
            boolean[] reachesCable = {false};
            var mount = socket.mount().add(net.minecraft.world.phys.Vec3.atLowerCornerOf(origin));
            var normal = net.minecraft.world.phys.Vec3.atLowerCornerOf(socket.facing().getNormal());
            VertexConsumer out = (VertexConsumer) Proxy.newProxyInstance(VertexConsumer.class.getClassLoader(),
                    new Class<?>[] {VertexConsumer.class}, (proxy, method, values) -> {
                        if (method.getName().equals("addVertex") && values.length == 4) {
                            var point = new Vector3f((float) values[1], (float) values[2], (float) values[3]);
                            ((PoseStack.Pose) values[0]).pose().transformPosition(point);
                            var relative = new net.minecraft.world.phys.Vec3(point).subtract(mount);
                            double along = relative.dot(normal);
                            var sideways = relative.subtract(normal.scale(along));
                            require(along >= -0.0081 && along <= 0.0651
                                            && Math.abs(sideways.x) <= 0.0501 && Math.abs(sideways.y) <= 0.0501
                                            && Math.abs(sideways.z) <= 0.0501,
                                    "Cable socket floats or points into equipment: " + socket);
                            reachesCable[0] |= Math.abs(along - 0.065) < 0.0001;
                            vertices[0]++;
                        }
                        return method.getReturnType() == VertexConsumer.class ? proxy : null;
                    });
            PoseStack pose = new PoseStack();
            render.invoke(null, out, pose, origin, socket, 15728880);
            require(vertices[0] == 72 && reachesCable[0] && pose.clear(),
                    "Socket must retain its mount, collar, and exact cable-end contact");
        }
    }

    private static void checkExposedSurfaces() {
        var surfaces = TestTowerGeometry.SURFACES;
        require(!surfaces.isEmpty(), "Static tower shell has no exposed surfaces");
        for (int i = 0; i < surfaces.size(); i++) {
            var a = surfaces.get(i);
            var axis = a.direction().getAxis();
            var box = a.bounds();
            require(box.getSize() > 0, "Degenerate tower surface");
            for (int j = i + 1; j < surfaces.size(); j++) {
                var b = surfaces.get(j);
                if (b.direction().getAxis() != axis) continue;
                var other = b.bounds();
                double planeA = axis.choose(box.minX, box.minY, box.minZ);
                double planeB = axis.choose(other.minX, other.minY, other.minZ);
                if (Math.abs(planeA - planeB) > 0.000001) continue;
                double overlapX = Math.min(box.maxX, other.maxX) - Math.max(box.minX, other.minX);
                double overlapY = Math.min(box.maxY, other.maxY) - Math.max(box.minY, other.minY);
                double overlapZ = Math.min(box.maxZ, other.maxZ) - Math.max(box.minZ, other.minZ);
                boolean overlap = switch (axis) {
                    case X -> overlapY > 0.000001 && overlapZ > 0.000001;
                    case Y -> overlapX > 0.000001 && overlapZ > 0.000001;
                    case Z -> overlapX > 0.000001 && overlapY > 0.000001;
                };
                require(!overlap, "Coplanar tower surfaces cause z-fighting: " + a + " / " + b);
            }
        }
        for (int deck : TestTowerGeometry.PLATFORM_LEVELS) {
            for (double z : new double[] {-1.8, -1.475, -1.3, 0, 2.4}) {
                double x = z < -1.5 ? 1.3 : 1.1;
                var point = new net.minecraft.world.phys.Vec3(x, deck + TestTowerGeometry.DECK_THICKNESS, z);
                require(surfaces.stream().anyMatch(surface -> surface.direction() == Direction.UP
                                && surface.material() == TestTowerGeometry.Material.DARK
                                && surface.bounds().inflate(0.000001).contains(point)),
                        "Removing overlap opened a hole in the platform or transfer landing");
            }
        }
    }

    private static void checkMeterGaugeBuffer() throws Exception {
        var render = Class.forName("com.example.mobilecellnetwork.client.HeldTesterRenderer")
                .getDeclaredMethod("renderChargeGauge", java.util.function.Supplier.class,
                        PoseStack.class, int.class, int.class);
        render.setAccessible(true);
        int[] generation = {0};
        int[] requests = {0};
        int[] vertices = {0};
        java.util.function.Supplier<VertexConsumer> buffer = () -> {
            requests[0]++;
            int current = ++generation[0];
            return (VertexConsumer) Proxy.newProxyInstance(VertexConsumer.class.getClassLoader(),
                    new Class<?>[] {VertexConsumer.class}, (proxy, method, values) -> {
                        require(current == generation[0], "Meter wrote to a buffer flushed by text rendering");
                        if (method.getName().equals("addVertex")) vertices[0]++;
                        return method.getReturnType() == VertexConsumer.class ? proxy : null;
                    });
        };
        for (int energy : new int[] {0, 25, 100}) {
            buffer.get();
            generation[0]++;
            int before = requests[0];
            vertices[0] = 0;
            PoseStack pose = new PoseStack();
            render.invoke(null, buffer, pose, energy, 100);
            require(requests[0] == before + 1, "Gauge must acquire a fresh buffer after the LCD text pass");
            int active = (int) Math.ceil(energy / 10.0);
            require(vertices[0] == (10 + active) * 24, "Charge gauge lost segments while fixing buffer lifetime");
            require(pose.clear(), "Gauge leaked a pose");
        }
        int before = requests[0];
        vertices[0] = 0;
        render.invoke(null, buffer, new PoseStack(), 0, 0);
        require(requests[0] == before && vertices[0] == 0, "Unknown capacity must not render a plausible charge");
    }

    private static AABB player(double y, double z) {
        return new AABB(0.2, y, z - 0.3, 0.8, y + 1.8, z + 0.3);
    }

    private static boolean close(double actual, double expected) {
        return Math.abs(actual - expected) < 0.00001;
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
