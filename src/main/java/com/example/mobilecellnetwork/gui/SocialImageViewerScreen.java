package com.example.mobilecellnetwork.gui;

import com.example.mobilecellnetwork.network.ClientSocialImageCache;
import com.example.mobilecellnetwork.network.ClientSocialCache;
import com.example.mobilecellnetwork.network.SocialImageRequestPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

public final class SocialImageViewerScreen extends Screen implements PhoneNavigableScreen {
    private static final int VISIBLE_COMMENTS = 4;
    private final PhoneMenu menu;
    private final String postId;
    private int commentScroll;

    public SocialImageViewerScreen(PhoneMenu menu, String postId) {
        super(Component.literal("Photo"));
        this.menu = menu;
        this.postId = postId;
    }

    @Override
    public PhoneMenu phoneMenu() {
        return menu;
    }

    @Override
    public void navigateBack() {
        minecraft.setScreen(new SocialsScreen(menu));
    }

    @Override
    protected void init() {
        if (ClientSocialImageCache.imageForDisplay(postId).length == 0) {
            PacketDistributor.sendToServer(new SocialImageRequestPacket(postId));
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        PhoneFrame.render(graphics, this);
        int left = PhoneFrame.getScreenX(this);
        int top = PhoneFrame.getContentTop(this);
        PhoneFrame.renderPanel(graphics, left, top, PhoneFrame.getScreenW(), PhoneFrame.getScreenH() - 24);
        PhoneFrame.renderHeader(graphics, this, "Post details", true);
        PhoneStyle style = com.example.mobilecellnetwork.network.ClientPhoneCache.phoneStyle();
        var post = ClientSocialCache.posts().stream()
                .filter(entry -> entry.id().equals(postId)).findFirst().orElse(null);
        PhoneRenderer.drawCard(graphics, style, left + 8, top + 62, 188, 136);
        byte[] image = ClientSocialImageCache.imageForDisplay(postId);
        if (image.length > 0) {
            PhonePhotoLibrary.drawThumbnail(graphics, "social-view-" + postId, image,
                    left + 14, top + 68, 176, 124);
        } else {
            if (ClientSocialImageCache.markImageRequested(postId)) {
                PacketDistributor.sendToServer(new SocialImageRequestPacket(postId));
            }
            graphics.drawCenteredString(font, "Loading photo...",
                    left + 102, top + 119, PhoneColors.textSecondary(style));
        }
        if (post != null) {
            PhoneRenderer.drawCard(graphics, style, left + 8, top + 204, 188, 48);
            graphics.drawString(font, font.plainSubstrByWidth(post.author(), 174),
                    left + 14, top + 210, PhoneColors.accent(style), true);
            graphics.drawString(font, font.plainSubstrByWidth(post.text(), 174),
                    left + 14, top + 226, PhoneColors.textPrimary(style), true);
            graphics.drawString(font, "Comments (" + post.comments().size() + ")",
                    left + 10, top + 258, PhoneColors.textSecondary(style), true);
            int first = Math.min(commentScroll,
                    Math.max(0, post.comments().size() - VISIBLE_COMMENTS));
            if (post.comments().isEmpty()) {
                PhoneRenderer.drawCard(graphics, style, left + 8, top + 274, 188, 32);
                graphics.drawCenteredString(font, "No comments yet",
                        left + 102, top + 284, PhoneColors.textSecondary(style));
            }
            for (int index = first;
                 index < Math.min(post.comments().size(), first + VISIBLE_COMMENTS); index++) {
                int y = top + 274 + (index - first) * 26;
                PhoneRenderer.drawCard(graphics, style, left + 8, y, 188, 23);
                graphics.drawString(font, font.plainSubstrByWidth(post.comments().get(index), 174),
                        left + 13, y + 7, PhoneColors.textPrimary(style), true);
            }
        } else {
            graphics.drawCenteredString(font, "Post is no longer available",
                    left + 102, top + 224, PhoneColors.textSecondary(style));
        }
        super.render(graphics, PhoneFrame.displayMouseX(this, mouseX),
                PhoneFrame.displayMouseY(this, mouseY), partialTick);
        PhoneFrame.renderStatusBar(graphics, this);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return super.mouseClicked(PhoneFrame.layoutMouseX(this, mouseX),
                PhoneFrame.layoutMouseY(this, mouseY), button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return super.mouseReleased(PhoneFrame.layoutMouseX(this, mouseX),
                PhoneFrame.layoutMouseY(this, mouseY), button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY != 0) {
            int commentCount = ClientSocialCache.posts().stream()
                    .filter(post -> post.id().equals(postId))
                    .mapToInt(post -> post.comments().size()).findFirst().orElse(0);
            commentScroll = Math.clamp(commentScroll + (scrollY < 0 ? 1 : -1),
                    0, Math.max(0, commentCount - VISIBLE_COMMENTS));
            return true;
        }
        return super.mouseScrolled(PhoneFrame.layoutMouseX(this, mouseX),
                PhoneFrame.layoutMouseY(this, mouseY), scrollX, scrollY);
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
