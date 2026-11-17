package com.example.mobilecellnetwork.client;

import com.example.mobilecellnetwork.blockentity.TestAntennaBlockEntity;
import com.example.mobilecellnetwork.block.TestTowerGeometry;
import com.example.mobilecellnetwork.block.TowerCabinets;
import com.example.mobilecellnetwork.block.TowerWorkLights;
import com.example.mobilecellnetwork.block.TowerVariant;
import com.example.mobilecellnetwork.block.MonopoleGeometry;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.function.Function;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class TestAntennaRenderer implements BlockEntityRenderer<TestAntennaBlockEntity> {
    private static final ResourceLocation STEEL =
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/iron_block.png");
    private static final ResourceLocation CONCRETE =
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/light_gray_concrete.png");
    private static final ResourceLocation DARK =
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/gray_concrete.png");
    private static final ResourceLocation PANEL =
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/white_concrete.png");
    private static final ResourceLocation LIGHT =
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/white_concrete.png");

    public TestAntennaRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public AABB getRenderBoundingBox(TestAntennaBlockEntity antenna) {
        int radius = antenna.isWideTower() ? antenna.variant().radius() : 0;
        return new AABB(antenna.getBlockPos()).inflate(radius, 0, radius)
                .expandTowards(0, antenna.getTowerHeight() - 1, 0);
    }

    @Override
    public void render(TestAntennaBlockEntity antenna, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int light, int overlay) {
        boolean on = antenna.isPowered() && antenna.getLevel() != null
                && Math.floorMod(antenna.getLevel().getGameTime() + antenna.getBlockPos().asLong(), 40) < 20;
        Function<ResourceLocation, VertexConsumer> textures =
                texture -> buffers.getBuffer(RenderType.entityCutoutNoCull(texture));
        if (antenna.isWideTower()) {
            TowerMeshCache.render(antenna.variant(), pose, light, overlay);
            Function<ResourceLocation, VertexConsumer> variantTextures = texture ->
                    new TowerVertexConsumer(textures.apply(textureFor(antenna.variant(), texture)),
                            antenna.variant(), pose.last());
            renderVariantDynamic(antenna.variant(), on, new PoseStack(), variantTextures, light, overlay,
                    antenna.getOpenCabinets(), antenna.installation().hasModule() || !antenna.installation().required(),
                    antenna.installation().source() != null, antenna.isPowered(), antenna.areWorkLightsPowered());
        } else {
            renderTower(antenna.getTowerHeight(), on, pose, textures, light, overlay);
        }
    }

    private static void renderMonopoleStatic(PoseStack pose, Function<ResourceLocation, VertexConsumer> buffers,
                                             int light, int overlay) {
        for (MonopoleGeometry.Material material : MonopoleGeometry.Material.values()) {
            ResourceLocation texture = switch (material) {
                case POLE, STEEL -> STEEL;
                case DARK -> DARK;
                case PANEL -> PANEL;
                case CONCRETE -> CONCRETE;
            };
            VertexConsumer out = buffers.apply(texture);
            for (var part : MonopoleGeometry.PARTS) {
                if (part.material() != material) continue;
                AABB b = part.bounds();
                if (material == MonopoleGeometry.Material.POLE) {
                    float radius = (float) b.getXsize() / 2;
                    for (int side = 0; side < 12; side++) {
                        double a = Math.PI * 2 * side / 12;
                        double c = Math.PI * 2 * (side + 1) / 12;
                        float x1 = 0.5F + (float) Math.cos(a) * radius;
                        float z1 = 0.5F + (float) Math.sin(a) * radius;
                        float x2 = 0.5F + (float) Math.cos(c) * radius;
                        float z2 = 0.5F + (float) Math.sin(c) * radius;
                        float y1 = (float) b.minY, y2 = (float) b.maxY;
                        face(out, pose, light, overlay, x1, y1, z1, x1, y2, z1,
                                x2, y2, z2, x2, y1, z2,
                                (float) Math.cos((a + c) / 2), 0, (float) Math.sin((a + c) / 2), 0xFF858578);
                        face(out, pose, light, overlay, 0.5F, y2, 0.5F, x2, y2, z2,
                                x1, y2, z1, 0.5F, y2, 0.5F, 0, 1, 0, 0xFF858578);
                    }
                } else {
                    box(out, pose, light, overlay, (float) b.minX, (float) b.minY, (float) b.minZ,
                            (float) b.maxX, (float) b.maxY, (float) b.maxZ,
                            material == MonopoleGeometry.Material.PANEL ? 0xFFE5E1CE : 0xFFFFFFFF);
                }
            }
        }
        VertexConsumer steel = buffers.apply(STEEL);
        for (var b : MonopoleGeometry.ARMS) {
            beam(steel, pose, light, overlay, b.x1(), b.y1(), b.z1(), b.x2(), b.y2(), b.z2(), b.thickness());
        }
        renderTerminals(buffers.apply(PANEL), pose, light, overlay, TowerVariant.MONOPOLE);
    }

    static ResourceLocation textureFor(TowerVariant variant, ResourceLocation texture) {
        if (variant == TowerVariant.TIER_1 && texture.equals(STEEL)) {
            return ResourceLocation.withDefaultNamespace("textures/block/copper_block.png");
        }
        if (texture.equals(PANEL)) {
            if (variant == TowerVariant.TIER_1) return ResourceLocation.withDefaultNamespace("textures/block/orange_concrete.png");
            if (variant == TowerVariant.TIER_2) return ResourceLocation.withDefaultNamespace("textures/block/light_blue_concrete.png");
        }
        return texture;
    }

    private static void renderWideTower(float height, boolean on, PoseStack pose,
                                        Function<ResourceLocation, VertexConsumer> buffers, int light, int overlay,
                                        int openCabinets, boolean module, boolean wired, boolean powered, boolean workLights) {
        renderWideStatic(height, pose, buffers, light, overlay);
        renderWideDynamic(height, on, pose, buffers, light, overlay, openCabinets, module, wired, powered, workLights);
    }

    static void renderWideStatic(float height, PoseStack pose,
                                 Function<ResourceLocation, VertexConsumer> buffers, int light, int overlay) {
        renderVariantStatic(TowerVariant.TIER_3, height, pose, buffers, light, overlay);
    }

    static void renderVariantStatic(TowerVariant variant, float height, PoseStack pose,
                                    Function<ResourceLocation, VertexConsumer> buffers, int light, int overlay) {
        if (variant.monopole()) {
            renderMonopoleStatic(pose, buffers, light, overlay);
            return;
        }
        renderParts(buffers.apply(CONCRETE), pose, light, overlay, TestTowerGeometry.Material.CONCRETE, variant);
        VertexConsumer steel = buffers.apply(STEEL);
        renderParts(steel, pose, light, overlay, TestTowerGeometry.Material.STEEL, variant);
        float top = height - 0.6F;
        for (TestTowerGeometry.Beam b : TestTowerGeometry.beams(variant)) {
            beam(steel, pose, light, overlay, b.x1(), b.y1(), b.z1(), b.x2(), b.y2(), b.z2(), b.thickness());
        }

        renderParts(steel, pose, light, overlay, TestTowerGeometry.Material.LADDER, variant);
        for (float y : TestTowerGeometry.CAGE_LEVELS) {
            for (int segment = 0; segment < 8; segment++) {
                double a = Math.PI * segment / 8;
                double b = Math.PI * (segment + 1) / 8;
                beam(steel, pose, light, overlay, 0.5F + (float) Math.cos(a) * 0.6F, y,
                        -1.4F - (float) Math.sin(a) * 0.55F,
                        0.5F + (float) Math.cos(b) * 0.6F, y,
                        -1.4F - (float) Math.sin(b) * 0.55F, 0.025F);
            }
        }
        for (float x : new float[] {-0.1F, 1.1F}) {
            for (float[] span : new float[][] {{3, 14.1F}, {16.3F, 17.1F}}) {
                beam(steel, pose, light, overlay, x, span[0], -1.4F, x, span[1], -1.4F, 0.025F);
            }
        }
        for (int deck : TestTowerGeometry.PLATFORM_LEVELS) {
            for (float x = -1.4F; x < 2.5F; x += 0.2F) {
                box(steel, pose, light, overlay, x, deck + TestTowerGeometry.DECK_THICKNESS - 0.003F,
                        -1.35F, x + 0.012F, deck + TestTowerGeometry.DECK_THICKNESS + 0.006F, 2.4F);
            }
        }
        box(steel, pose, light, overlay, 0.46F, top, 0.46F, 0.54F, height - 0.18F, 0.54F);

        VertexConsumer dark = buffers.apply(DARK);
        renderParts(dark, pose, light, overlay, TestTowerGeometry.Material.DARK, variant);
        // The feed stops below the beacon support; coincident faces here caused z-fighting.
        box(dark, pose, light, overlay, 0.46F, 1.4F, 0.46F, 0.54F, top, 0.54F);
        widePanels(dark, pose, light, overlay, height, false, variant);
        widePanels(buffers.apply(PANEL), pose, light, overlay, height, true, variant);
        renderTerminals(buffers.apply(PANEL), pose, light, overlay, variant);
    }

    private static void renderWideDynamic(float height, boolean on, PoseStack pose,
                                          Function<ResourceLocation, VertexConsumer> buffers, int light, int overlay,
                                          int openCabinets, boolean module, boolean wired, boolean powered,
                                          boolean workLights) {
        renderVariantDynamic(TowerVariant.TIER_3, on, pose, buffers, light, overlay,
                openCabinets, module, wired, powered, workLights);
    }

    static void renderVariantDynamic(TowerVariant variant, boolean on, PoseStack pose,
                                     Function<ResourceLocation, VertexConsumer> buffers, int light, int overlay,
                                     int openCabinets, boolean module, boolean wired, boolean powered, boolean workLights) {
        renderCabinets(buffers.apply(PANEL), pose, light, overlay, openCabinets, module, wired, powered, variant);
        if (!variant.monopole()) renderWorkLights(buffers.apply(PANEL), pose, light, overlay, workLights);
        AABB beacon = variant.monopole() ? MonopoleGeometry.BEACON
                : new AABB(0.4, 20 - 0.18F, 0.4, 0.6, 20, 0.6);
        box(buffers.apply(LIGHT), pose, net.minecraft.client.renderer.LightTexture.FULL_BRIGHT,
                overlay, (float) beacon.minX, (float) beacon.minY, (float) beacon.minZ,
                (float) beacon.maxX, (float) beacon.maxY, (float) beacon.maxZ, on ? 0xFFFF4444 : 0xFF882222);
    }

    private static void renderTerminals(VertexConsumer out, PoseStack pose, int light, int overlay) {
        renderTerminals(out, pose, light, overlay, TowerVariant.TIER_3);
    }

    private static void renderTerminals(VertexConsumer out, PoseStack pose, int light, int overlay, TowerVariant variant) {
        for (int i = 0; i < TowerCabinets.BOUNDS.size(); i++) {
            if (!variant.hasTerminal(i)) continue;
            AABB cabinet = TowerCabinets.BOUNDS.get(i);
            float w = (float) cabinet.getXsize() - 0.12F;
            float h = 0.48F;
            pose.pushPose();
            Vec3 offset = TowerCabinets.offset(variant);
            pose.translate(offset.x, offset.y, offset.z);
            cabinetPose(pose, i);
            pose.translate(0.06, cabinet.getYsize() + 0.1, 0.02);
            box(out, pose, light, overlay, 0, 0, 0, w, h, 0.1F, 0xFF252C33);
            int bright = net.minecraft.client.renderer.LightTexture.FULL_BRIGHT;
            box(out, pose, bright, overlay, 0.025F, 0.04F, -0.003F, w - 0.025F, h - 0.025F, -0.001F, 0xFF14344D);
            box(out, pose, bright, overlay, 0.05F, h - 0.09F, -0.006F, w - 0.05F, h - 0.055F, -0.004F, 0xFF66CCEE);
            for (float y = 0.1F; y < h - 0.14F; y += 0.055F) {
                box(out, pose, bright, overlay, 0.05F, y, -0.006F, w - 0.09F, y + 0.013F, -0.004F, 0xFF9DD9B8);
            }
            pose.popPose();
        }
    }

    private static void renderWorkLights(VertexConsumer out, PoseStack pose, int light, int overlay, boolean powered) {
        for (TowerWorkLights.Fixture fixture : TowerWorkLights.FIXTURES) {
            AABB b = fixture.housing();
            box(out, pose, light, overlay, (float) b.minX, (float) b.minY, (float) b.minZ,
                    (float) b.maxX, (float) b.maxY, (float) b.maxZ, 0xFF303840);
            b = fixture.lens();
            box(out, pose, powered ? net.minecraft.client.renderer.LightTexture.FULL_BRIGHT : light, overlay,
                    (float) b.minX, (float) b.minY, (float) b.minZ,
                    (float) b.maxX, (float) b.maxY, (float) b.maxZ, powered ? 0xFFFFF2CB : 0xFF77776F);
        }
    }

    private static void renderCabinets(VertexConsumer out, PoseStack pose, int light, int overlay,
                                       int openCabinets, boolean module, boolean wired, boolean powered) {
        renderCabinets(out, pose, light, overlay, openCabinets, module, wired, powered, TowerVariant.TIER_3);
    }

    private static void renderCabinets(VertexConsumer out, PoseStack pose, int light, int overlay,
                                       int openCabinets, boolean module, boolean wired, boolean powered, TowerVariant variant) {
        for (int i = 0; i < TowerCabinets.BOUNDS.size(); i++) {
            if (!variant.hasCabinet(i)) continue;
            AABB cabinet = TowerCabinets.BOUNDS.get(i);
            float w = (float) cabinet.getXsize();
            float h = (float) cabinet.getYsize();
            float d = (float) cabinet.getZsize();
            boolean open = (openCabinets & (1 << i)) != 0;
            pose.pushPose();
            Vec3 offset = TowerCabinets.offset(variant);
            pose.translate(offset.x, offset.y, offset.z);
            cabinetPose(pose, i);
            int shell = 0xFF686F75;
            box(out, pose, light, overlay, 0, 0, 0, 0.03F, h, d, shell);
            box(out, pose, light, overlay, w - 0.03F, 0, 0, w, h, d, shell);
            box(out, pose, light, overlay, 0.03F, 0, 0, w - 0.03F, 0.03F, d, shell);
            box(out, pose, light, overlay, 0.03F, h - 0.03F, 0, w - 0.03F, h, d, shell);
            box(out, pose, light, overlay, 0.03F, 0.03F, d - 0.03F, w - 0.03F, h - 0.03F, d, shell);
            if (open) {
                // Inward-opening door stays inside the cabinet's existing collision envelope.
                box(out, pose, light, overlay, 0.04F, 0.04F, 0.04F, 0.065F, h - 0.04F, d - 0.04F, 0xFFBBC2C7);
                box(out, pose, light, overlay, 0.09F, 0.1F, d - 0.07F, w - 0.07F, h - 0.08F, d - 0.04F, 0xFF292D32);
                boolean populated = i == 0 ? (wired || variant.monopole() && module) : module;
                box(out, pose, light, overlay, 0.13F, 0.32F, d - 0.1F, w - 0.11F, h - 0.14F, d - 0.075F,
                        populated ? 0xFF267344 : 0xFF60636A);
                for (float x = 0.15F; x < w - 0.12F; x += 0.085F) {
                    box(out, pose, light, overlay, x, 0.17F, d - 0.16F, x + 0.045F, 0.26F, d - 0.08F, 0xFFD8A64B);
                    if (populated) {
                        box(out, pose, light, overlay, x, 0.27F, d - 0.13F, x + 0.018F, 0.4F, d - 0.105F,
                                i == 0 ? 0xFFCC4939 : 0xFF4388CC);
                    }
                }
                if (populated && i != 0) {
                    box(out, pose, light, overlay, 0.2F, 0.48F, d - 0.14F, w - 0.18F, 0.61F, d - 0.105F, 0xFF202329);
                    for (float x = 0.205F; x < w - 0.19F; x += 0.035F) {
                        box(out, pose, light, overlay, x, 0.49F, d - 0.165F,
                                x + 0.012F, 0.6F, d - 0.14F, 0xFFBBC2C7);
                    }
                    box(out, pose, light, overlay, 0.14F, 0.65F, d - 0.13F, 0.2F, 0.73F, d - 0.105F, 0xFF202329);
                    box(out, pose, light, overlay, 0.14F, 0.75F, d - 0.13F, 0.2F, 0.83F, d - 0.105F, 0xFF202329);
                    box(out, pose, light, overlay, 0.15F, 0.42F, d - 0.105F, 0.16F, 0.64F, d - 0.1F, 0xFFD8A64B);
                    box(out, pose, light, overlay, 0.16F, 0.63F, d - 0.105F, w - 0.14F, 0.64F, d - 0.1F, 0xFFD8A64B);
                } else if (populated) {
                    for (float x = 0.15F; x < w - 0.15F; x += 0.1F) {
                        box(out, pose, light, overlay, x, 0.52F, d - 0.17F, x + 0.055F, 0.72F, d - 0.1F, 0xFFEEEEDE);
                        box(out, pose, light, overlay, x + 0.01F, 0.57F, d - 0.19F,
                                x + 0.045F, 0.64F, d - 0.17F, 0xFF30343A);
                    }
                }
                box(out, pose, net.minecraft.client.renderer.LightTexture.FULL_BRIGHT, overlay,
                        w - 0.17F, h - 0.2F, d - 0.125F, w - 0.12F, h - 0.15F, d - 0.1F,
                        powered ? 0xFF44EE66 : 0xFFDD7733);
            } else {
                box(out, pose, light, overlay, 0.035F, 0.035F, 0, w - 0.035F, h - 0.035F, 0.025F, 0xFFBBC2C7);
                box(out, pose, light, overlay, w - 0.14F, h * 0.4F, -0.012F,
                        w - 0.1F, h * 0.65F, -0.002F, 0xFF30343A);
                box(out, pose, light, overlay, 0.1F, h - 0.21F, -0.005F, w - 0.1F, h - 0.11F, -0.001F,
                        i == 0 ? 0xFFFFCC33 : 0xFF4499DD);
            }
            pose.popPose();
        }
    }

    private static void cabinetPose(PoseStack pose, int index) {
        var origin = TowerCabinets.localPoint(index, 0, 0, 0);
        pose.translate(origin.x, origin.y, origin.z);
        if (index != 0) pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(90));
    }

    private static void renderParts(VertexConsumer out, PoseStack pose, int light, int overlay,
                                    TestTowerGeometry.Material material, TowerVariant variant) {
        for (TestTowerGeometry.Surface surface : TestTowerGeometry.surfaces(variant)) {
            if (surface.material() != material) continue;
            AABB b = surface.bounds();
            float x1 = (float) b.minX, y1 = (float) b.minY, z1 = (float) b.minZ;
            float x2 = (float) b.maxX, y2 = (float) b.maxY, z2 = (float) b.maxZ;
            switch (surface.direction()) {
                case NORTH -> face(out, pose, light, overlay, x1, y1, z1, x1, y2, z1, x2, y2, z1, x2, y1, z1, 0, 0, -1, 0xFFFFFFFF);
                case SOUTH -> face(out, pose, light, overlay, x2, y1, z2, x2, y2, z2, x1, y2, z2, x1, y1, z2, 0, 0, 1, 0xFFFFFFFF);
                case WEST -> face(out, pose, light, overlay, x1, y1, z2, x1, y2, z2, x1, y2, z1, x1, y1, z1, -1, 0, 0, 0xFFFFFFFF);
                case EAST -> face(out, pose, light, overlay, x2, y1, z1, x2, y2, z1, x2, y2, z2, x2, y1, z2, 1, 0, 0, 0xFFFFFFFF);
                case UP -> face(out, pose, light, overlay, x1, y2, z1, x1, y2, z2, x2, y2, z2, x2, y2, z1, 0, 1, 0, 0xFFFFFFFF);
                case DOWN -> face(out, pose, light, overlay, x1, y1, z2, x1, y1, z1, x2, y1, z1, x2, y1, z2, 0, -1, 0, 0xFFFFFFFF);
            }
        }
    }

    private static void widePanels(VertexConsumer out, PoseStack pose, int light, int overlay,
                                    float height, boolean front) {
        widePanels(out, pose, light, overlay, height, front, TowerVariant.TIER_3);
    }

    private static void widePanels(VertexConsumer out, PoseStack pose, int light, int overlay,
                                    float height, boolean front, TowerVariant variant) {
        for (int sector = 0; sector < 3; sector++) {
            pose.pushPose();
            pose.translate(0.5F, 0, 0.5F);
            pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(sector * 120.0F));
            for (float y : new float[] {height - 5.8F, height - 2.8F}) {
                if (variant.separateComputerDeck() && y < height - 3) continue;
                for (float x : new float[] {-0.4F, 0.1F}) {
                    box(out, pose, light, overlay, front ? x : x + 0.025F,
                            front ? y : y + 0.4F, front ? 1.2F : 1.05F,
                            front ? x + 0.3F : x + 0.275F,
                            front ? y + 2.0F : y + 1.0F, front ? 1.35F : 1.21F);
                }
            }
            pose.popPose();
        }
    }

    private static void renderTower(float height, boolean on, PoseStack pose,
                                    Function<ResourceLocation, VertexConsumer> buffers, int light, int overlay) {
        VertexConsumer steel = buffers.apply(STEEL);
        float mastTop = height - 0.6F;
        for (float y = 0.32F; y < mastTop; y += 1.5F) {
            float nextY = Math.min(y + 1.5F, mastTop);
            float lower = mastHalfWidth(y, mastTop);
            float upper = mastHalfWidth(nextY, mastTop);
            for (int x = -1; x <= 1; x += 2) {
                for (int z = -1; z <= 1; z += 2) {
                    beam(steel, pose, light, overlay, 0.5F + x * lower, y, 0.5F + z * lower,
                            0.5F + x * upper, nextY, 0.5F + z * upper, 0.055F);
                }
            }
            for (int side = 0; side < 4; side++) {
                pose.pushPose();
                pose.translate(0.5F, 0, 0.5F);
                pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(side * 90.0F));
                beam(steel, pose, light, overlay, -lower, y, -lower, upper, nextY, -upper, 0.025F);
                beam(steel, pose, light, overlay, lower, y, -lower, -upper, nextY, -upper, 0.025F);
                beam(steel, pose, light, overlay, -upper, nextY, -upper,
                        upper, nextY, -upper, 0.035F);
                pose.popPose();
            }
        }

        float ladderBottom = 0.32F;
        float ladderTop = height - 1.0F;
        float ladderZBottom = 0.5F - mastHalfWidth(ladderBottom, mastTop) - 0.035F;
        float ladderZTop = 0.5F - mastHalfWidth(ladderTop, mastTop) - 0.035F;
        for (float x : new float[] {0.40F, 0.60F}) {
            beam(steel, pose, light, overlay, x, ladderBottom, ladderZBottom,
                    x, ladderTop, ladderZTop, 0.022F);
        }
        for (float y = 0.5F; y < ladderTop; y += 0.28F) {
            float z = 0.5F - mastHalfWidth(y, mastTop) - 0.035F;
            beam(steel, pose, light, overlay, 0.40F, y, z, 0.60F, y, z, 0.018F);
        }

        for (float platformY : new float[] {height - 6.0F, height - 3.0F}) {
            platform(steel, pose, light, overlay, platformY);
        }

        for (int sector = 0; sector < 3; sector++) {
            pose.pushPose();
            pose.translate(0.5F, 0, 0.5F);
            pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(sector * 120.0F));
            for (float y : new float[] {height - 5.8F, height - 2.8F}) {
                beam(steel, pose, light, overlay, 0, y + 0.3F, 0,
                        0, y + 0.3F, 0.28F, 0.045F);
                beam(steel, pose, light, overlay, 0, y + 1.4F, 0,
                        0, y + 1.4F, 0.28F, 0.045F);
            }
            pose.popPose();
        }
        box(steel, pose, light, overlay, 0.475F, mastTop, 0.475F,
                0.525F, height - 0.18F, 0.525F);

        VertexConsumer dark = buffers.apply(DARK);
        box(dark, pose, light, overlay, 0.48F, 0.32F, 0.48F, 0.52F, height - 1.0F, 0.52F);
        for (float y : new float[] {height - 6.0F, height - 3.0F}) {
            box(dark, pose, light, overlay, 0.14F, y, 0.14F, 0.86F, y + 0.06F, 0.86F);
        }
        sectorPanels(dark, pose, light, overlay, height, false);
        VertexConsumer panel = buffers.apply(PANEL);
        sectorPanels(panel, pose, light, overlay, height, true);
        VertexConsumer beacon = buffers.apply(LIGHT);
        int beaconLight = net.minecraft.client.renderer.LightTexture.FULL_BRIGHT;
        int beaconColor = on ? 0xFFFF4444 : 0xFF882222;
        box(beacon, pose, beaconLight, overlay, 0.43F, height - 0.18F, 0.43F,
                0.57F, height, 0.57F, beaconColor);
    }

    private static float mastHalfWidth(float y, float top) {
        return 0.27F - 0.12F * (y - 0.32F) / (top - 0.32F);
    }

    private static void sectorPanels(VertexConsumer out, PoseStack pose, int light, int overlay,
                                     float height, boolean front) {
        for (int sector = 0; sector < 3; sector++) {
            pose.pushPose();
            pose.translate(0.5F, 0, 0.5F);
            pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(sector * 120.0F));
            for (float y : new float[] {height - 5.8F, height - 2.8F}) {
                for (float x : new float[] {-0.13F, 0.03F}) {
                    if (front) {
                        box(out, pose, light, overlay, x, y, 0.27F, x + 0.10F, y + 1.8F, 0.34F);
                    } else {
                        box(out, pose, light, overlay, x, y + 0.4F, 0.20F,
                                x + 0.10F, y + 0.9F, 0.26F);
                    }
                }
            }
            pose.popPose();
        }
    }

    private static void platform(VertexConsumer steel, PoseStack pose,
                                 int light, int overlay, float y) {
        for (int side = 0; side < 4; side++) {
            pose.pushPose();
            pose.translate(0.5F, 0, 0.5F);
            pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(side * 90.0F));
            for (float x : new float[] {-0.34F, 0.34F}) {
                beam(steel, pose, light, overlay, x, y, -0.34F, x, y + 0.42F, -0.34F, 0.022F);
            }
            beam(steel, pose, light, overlay, -0.34F, y + 0.42F, -0.34F,
                    0.34F, y + 0.42F, -0.34F, 0.022F);
            pose.popPose();
        }
    }

    private static void beam(VertexConsumer out, PoseStack pose, int light, int overlay,
                             float x1, float y1, float z1, float x2, float y2, float z2, float thickness) {
        Vector3f direction = new Vector3f(x2 - x1, y2 - y1, z2 - z1);
        float length = direction.length();
        pose.pushPose();
        pose.translate(x1, y1, z1);
        pose.mulPose(new Quaternionf().rotationTo(new Vector3f(0, 1, 0), direction.normalize()));
        float half = thickness / 2;
        box(out, pose, light, overlay, -half, 0, -half, half, length, half);
        pose.popPose();
    }

    private static void box(VertexConsumer out, PoseStack pose, int light, int overlay,
                            float x1, float y1, float z1, float x2, float y2, float z2) {
        box(out, pose, light, overlay, x1, y1, z1, x2, y2, z2, 0xFFFFFFFF);
    }

    static void box(VertexConsumer out, PoseStack pose, int light, int overlay,
                            float x1, float y1, float z1, float x2, float y2, float z2, int color) {
        face(out, pose, light, overlay, x1, y1, z1, x1, y2, z1, x2, y2, z1, x2, y1, z1, 0, 0, -1, color);
        face(out, pose, light, overlay, x2, y1, z2, x2, y2, z2, x1, y2, z2, x1, y1, z2, 0, 0, 1, color);
        face(out, pose, light, overlay, x1, y1, z2, x1, y2, z2, x1, y2, z1, x1, y1, z1, -1, 0, 0, color);
        face(out, pose, light, overlay, x2, y1, z1, x2, y2, z1, x2, y2, z2, x2, y1, z2, 1, 0, 0, color);
        face(out, pose, light, overlay, x1, y2, z1, x1, y2, z2, x2, y2, z2, x2, y2, z1, 0, 1, 0, color);
        face(out, pose, light, overlay, x1, y1, z2, x1, y1, z1, x2, y1, z1, x2, y1, z2, 0, -1, 0, color);
    }

    private static void face(VertexConsumer out, PoseStack pose, int light, int overlay,
                             float ax, float ay, float az, float bx, float by, float bz,
                             float cx, float cy, float cz, float dx, float dy, float dz,
                             float nx, float ny, float nz, int color) {
        vertex(out, pose, light, overlay, ax, ay, az, 0, 1, nx, ny, nz, color);
        vertex(out, pose, light, overlay, bx, by, bz, 0, 0, nx, ny, nz, color);
        vertex(out, pose, light, overlay, cx, cy, cz, 1, 0, nx, ny, nz, color);
        vertex(out, pose, light, overlay, dx, dy, dz, 1, 1, nx, ny, nz, color);
    }

    private static void vertex(VertexConsumer out, PoseStack pose, int light, int overlay,
                               float x, float y, float z, float u, float v, float nx, float ny, float nz, int color) {
        out.addVertex(pose.last(), x, y, z).setColor(color)
                .setUv(u, v).setOverlay(overlay).setLight(light).setNormal(pose.last(), nx, ny, nz);
    }
}
