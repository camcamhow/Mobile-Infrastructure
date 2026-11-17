package com.example.mobilecellnetwork.gui;

import com.example.mobilecellnetwork.network.ClientPhoneCache;
import com.example.mobilecellnetwork.network.ClientSocialImageCache;
import com.example.mobilecellnetwork.network.ClientSocialProfileCache;
import com.example.mobilecellnetwork.network.ClientSocialCache;
import com.example.mobilecellnetwork.network.RequestPhoneDataPacket;
import com.example.mobilecellnetwork.network.SocialFeedRequestPacket;
import com.example.mobilecellnetwork.network.SocialImageRequestPacket;
import com.example.mobilecellnetwork.network.SocialLikePacket;
import com.example.mobilecellnetwork.network.SocialPostPacket;
import com.example.mobilecellnetwork.gui.theme.McnPrimitives;
import com.example.mobilecellnetwork.gui.theme.McnTheme;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
import com.example.mobilecellnetwork.gui.NoServiceOverlay;

public final class SocialsScreen extends Screen implements PhoneNavigableScreen {
    private static final int VISIBLE_POSTS = 2;
    private final PhoneMenu menu;
    private EditBox composer;
    private int scroll;
    private String filter = "ALL";
    private String commentPostId = "";
    private String replyAuthor = "";
    private byte[] profileAvatar;
    private byte[] pendingPhoto;

    public SocialsScreen(PhoneMenu menu) {
        super(Component.literal("Socials"));
        this.menu = menu;
    }

    @Override
    public PhoneMenu phoneMenu() {
        return menu;
    }

    @Override
    protected void init() {
        ClientSocialProfileCache.beginLoad();
        pendingPhoto = ClientSocialImageCache.pendingImage();
        PacketDistributor.sendToServer(new RequestPhoneDataPacket());
        PacketDistributor.sendToServer(new SocialFeedRequestPacket(filter));

        int screenX = McnPrimitives.getScreenX(width);
        int screenY = McnPrimitives.getScreenY(height);
        int contentLeft = McnPrimitives.getContentLeft(screenX);
        int contentTop = McnPrimitives.getContentTop(screenY);

        int filterY = contentTop + McnTheme.SPACE_S;
        int filterHeight = McnTheme.BUTTON_HEIGHT_SM + McnTheme.SPACE_XS;
        int composerY = filterY + filterHeight + McnTheme.SPACE_S;
        int composerInnerY = composerY + McnTheme.SPACE_XS;

        composer = PhoneTextField.create(font,
                        contentLeft + McnTheme.SPACE_S - 6, composerInnerY + McnTheme.SPACE_XS - 3,
                112, McnTheme.TEXT_FIELD_HEIGHT,
                "Write a post", 280, ClientPhoneCache.phoneStyle());
        addRenderableWidget(composer);
    }

    private void submitPost() {
        byte[] image = pendingPhoto;
        boolean reply = !commentPostId.isBlank();
        if (composer.getValue().isBlank() || reply && image.length > 0) {
            return;
        }
        String action = reply ? "comment" : "post";
        PacketDistributor.sendToServer(new SocialPostPacket(action, commentPostId, composer.getValue(),
                "", reply ? new byte[0] : image));
        composer.setValue("");
        if (!reply) {
            ClientSocialImageCache.clearPendingImage();
            pendingPhoto = new byte[0];
        } else {
            composer.setHint(Component.literal("Write a post"));
            composer.setMaxLength(280);
        }
        commentPostId = "";
        replyAuthor = "";
        PacketDistributor.sendToServer(new SocialFeedRequestPacket(filter));
    }

    private void selectFilter(String next) {
        if (!filter.equals(next)) {
            filter = next;
            scroll = 0;
            PacketDistributor.sendToServer(new SocialFeedRequestPacket(filter));
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // No service gating: show overlay if no active plan and screen is not exempt
        if (ClientPhoneCache.planId().isBlank()) {
            PhoneFrame.render(graphics, this);
            NoServiceOverlay.render(graphics, ClientPhoneCache.phoneStyle(), PhoneFrame.getScreenX(this),
                    PhoneFrame.getContentTop(this), PhoneFrame.getScreenW(), PhoneFrame.getScreenH() - 24, "Socials", font);
            PhoneFrame.renderStatusBar(graphics, this);
            return;
        }
        PhoneFrame.render(graphics, this);
        PhoneStyle style = ClientPhoneCache.phoneStyle();

        // Compute content bounds once
        int screenX = McnPrimitives.getScreenX(width);
        int screenY = McnPrimitives.getScreenY(height);
        int contentLeft = McnPrimitives.getContentLeft(screenX);
        int contentRight = McnPrimitives.getContentRight(screenX, McnPrimitives.getScreenWidth());
        int contentTop = McnPrimitives.getContentTop(screenY);
        int contentBottom = McnPrimitives.getContentBottom(screenY, McnPrimitives.getScreenHeight());
        int contentWidth = contentRight - contentLeft;

        PhoneFrame.renderHeader(graphics, this, "Socials", true);

        if (!ClientSocialProfileCache.loaded()) {
            int statusY = contentTop + McnTheme.SPACE_L;
            McnPrimitives.drawCard(graphics, style, contentLeft, statusY,
                    contentWidth, McnTheme.BUTTON_HEIGHT_MD);
            graphics.drawCenteredString(font, "Connecting to Socials...",
                    contentLeft + contentWidth / McnTheme.SPACE_XXS,
                    statusY + (McnTheme.BUTTON_HEIGHT_MD - font.lineHeight) / McnTheme.SPACE_XXS,
                    PhoneColors.textSecondary(style));
            super.render(graphics, PhoneFrame.displayMouseX(this, mouseX),
                    PhoneFrame.displayMouseY(this, mouseY), partialTick);
            PhoneFrame.renderStatusBar(graphics, this);
            return;
        }
        if (!ClientSocialProfileCache.configured()) {
            PhoneFrame.renderStatusBar(graphics, this);
            minecraft.setScreen(new SocialProfileSetupScreen(menu));
            return;
        }
        if (profileAvatar == null) {
            profileAvatar = ClientSocialProfileCache.avatar();
        }

        // ------------------------------------------------------------------
        // Filter row
        // ------------------------------------------------------------------
        int filterY = contentTop + McnTheme.SPACE_S;
        int filterHeight = McnTheme.BUTTON_HEIGHT_SM + McnTheme.SPACE_XS;
        McnPrimitives.drawCard(graphics, style, contentLeft, filterY, contentWidth, filterHeight);

        int filterInnerY = filterY + McnTheme.SPACE_XXS;
        int filterCursor = contentLeft + McnTheme.SPACE_XXS;
        drawFilter(graphics, style, filterCursor, filterInnerY, 58, "All", "ALL");
        filterCursor += 58 + McnTheme.SPACE_XXS;
        drawFilter(graphics, style, filterCursor, filterInnerY, 62, "Following", "FOLLOWING");
        filterCursor += 62 + McnTheme.SPACE_XXS;
        drawFilter(graphics, style, filterCursor, filterInnerY, 52, "My posts", "MINE");

        // ------------------------------------------------------------------
        // Composer card
        // ------------------------------------------------------------------
        int composerY = filterY + filterHeight + McnTheme.SPACE_S;
        int composerHeight = McnTheme.TEXT_FIELD_HEIGHT + McnTheme.SPACE_M;
        McnPrimitives.drawCard(graphics, style, contentLeft, composerY, contentWidth, composerHeight);

        int composerInnerY = composerY + McnTheme.SPACE_XS;
        int composerLeft = contentLeft + McnTheme.SPACE_S;

        // Composer text field
        McnPrimitives.drawTextField(graphics, style, font,
                composerLeft, composerInnerY + McnTheme.SPACE_XS,
                112, McnTheme.TEXT_FIELD_HEIGHT,
                "", commentPostId.isBlank() ? "Write a post" : "Write a reply",
                McnPrimitives.TextFieldState.NORMAL, 0, false, 0, 0);

        // Avatar (right side of composer)
        int avatarSize = 22;
        int avatarX = contentRight - McnTheme.SPACE_S - 23 - avatarSize - McnTheme.SPACE_XS;
        int avatarY = composerInnerY;

        if (profileAvatar.length > 0) {
            PhonePhotoLibrary.drawThumbnail(graphics, "social-profile-avatar", profileAvatar,
                    avatarX, avatarY, avatarSize, avatarSize);
        } else {
            McnPrimitives.fillRoundedRect(graphics, avatarX, avatarY,
                    avatarSize, avatarSize, avatarSize / 2, ClientSocialProfileCache.color());
            String initial = ClientSocialProfileCache.username().substring(0, 1)
                    .toUpperCase(java.util.Locale.ROOT);
            graphics.drawCenteredString(font, initial, avatarX + avatarSize / 2,
                    avatarY + (avatarSize - font.lineHeight) / 2, 0xFFFFFFFF);
        }

        // Gallery/Photo button + Send button on the right of the composer
        int sendButtonWidth = 23;
        int sendX = contentRight - McnTheme.SPACE_S - sendButtonWidth;
        int sendY = composerInnerY;

        int galleryWidth = 42;
        int galleryX = sendX - galleryWidth - McnTheme.SPACE_XXS;

        McnPrimitives.drawButton(graphics, style, font, galleryX, sendY,
                galleryWidth, McnTheme.BUTTON_HEIGHT_SM,
                pendingPhoto.length == 0 ? "Gallery" : "Photo",
                McnPrimitives.ButtonVariant.SECONDARY, false, false);
        McnPrimitives.drawButton(graphics, style, font, sendX, sendY,
                sendButtonWidth, McnTheme.BUTTON_HEIGHT_SM,
                ">", McnPrimitives.ButtonVariant.PRIMARY, false, false);

        // Composer label above the text field
        String composerLabel = commentPostId.isBlank()
                ? ClientSocialProfileCache.username() + " - Share a moment"
                : "Replying to " + replyAuthor;
        graphics.drawString(font, font.plainSubstrByWidth(composerLabel, 150),
                composerLeft, composerY + 2, PhoneColors.textSecondary(style), true);

        // ------------------------------------------------------------------
        // Find people card
        // ------------------------------------------------------------------
        int findY = composerY + composerHeight + McnTheme.SPACE_S;
        int findHeight = McnTheme.BUTTON_HEIGHT_SM + McnTheme.SPACE_XS;
        McnPrimitives.drawCard(graphics, style, contentLeft, findY, contentWidth, findHeight);

        String peopleLabel = "Find people - " + ClientPhoneCache.followedPlayers().size() + " following";
        graphics.drawString(font, font.plainSubstrByWidth(peopleLabel, 152),
                contentLeft + McnTheme.SPACE_S, findY + McnTheme.SPACE_XS + 2,
                PhoneColors.textPrimary(style), true);

        int viewButtonWidth = 34;
        McnPrimitives.drawButton(graphics, style, font,
                contentRight - McnTheme.SPACE_S - viewButtonWidth, findY + McnTheme.SPACE_XXS,
                viewButtonWidth, McnTheme.BUTTON_HEIGHT_SM - McnTheme.SPACE_XS,
                "View", McnPrimitives.ButtonVariant.SECONDARY, false, false);

        // ------------------------------------------------------------------
        // Filter label
        // ------------------------------------------------------------------
        int filterLabelY = findY + findHeight + McnTheme.SPACE_S;
        graphics.drawString(font, filterLabel(),
                contentLeft, filterLabelY, PhoneColors.textSecondary(style), true);

        // ------------------------------------------------------------------
        // Posts
        // ------------------------------------------------------------------
        var posts = ClientSocialCache.posts();
        int first = Math.min(scroll, Math.max(0, posts.size() - VISIBLE_POSTS));
        int postsStartY = filterLabelY + McnTheme.TEXT_CAPTION + McnTheme.SPACE_XS;
        int cardHeight = 68;

        if (posts.isEmpty()) {
            McnPrimitives.drawCard(graphics, style, contentLeft, postsStartY,
                    contentWidth, cardHeight);
            graphics.drawCenteredString(font, emptyMessage(),
                    contentLeft + contentWidth / McnTheme.SPACE_XXS,
                    postsStartY + (cardHeight - font.lineHeight) / McnTheme.SPACE_XXS,
                    PhoneColors.textSecondary(style));
        }

        for (int index = first; index < Math.min(posts.size(), first + VISIBLE_POSTS); index++) {
            var post = posts.get(index);
            int y = postsStartY + (index - first) * (cardHeight + McnTheme.SPACE_XS);

            McnPrimitives.drawCard(graphics, style, contentLeft, y, contentWidth, cardHeight);

            // Avatar
            int postAvatarSize = 22;
            int postAvatarX = contentLeft + McnTheme.SPACE_XS;
            int postAvatarY = y + McnTheme.SPACE_XS;

            int avatarColor = 0xFF000000 | (post.authorId().hashCode() & 0x00FFFFFF);
            McnPrimitives.fillRoundedRect(graphics, postAvatarX, postAvatarY,
                    postAvatarSize, postAvatarSize, postAvatarSize / 2, avatarColor);

            String initial = post.author().isBlank() ? "?" : post.author().substring(0, 1)
                    .toUpperCase(java.util.Locale.ROOT);
            graphics.drawCenteredString(font, initial,
                    postAvatarX + postAvatarSize / 2,
                    postAvatarY + (postAvatarSize - font.lineHeight) / 2, 0xFFFFFFFF);

            // Author name
            graphics.drawString(font, font.plainSubstrByWidth(post.author(), 110),
                    postAvatarX + postAvatarSize + McnTheme.SPACE_XS, postAvatarY + 2,
                    PhoneColors.accent(style), true);

            // Post text
            int textMaxWidth = post.hasImage() ? 94 : (contentWidth - McnTheme.SPACE_M);
            graphics.drawString(font, font.plainSubstrByWidth(post.text(), textMaxWidth),
                    contentLeft + McnTheme.SPACE_S, y + 32,
                    PhoneColors.textPrimary(style), true);

            // Like button
            int likeY = y + cardHeight - McnTheme.BUTTON_HEIGHT_SM - McnTheme.SPACE_XXS;
            String likes = (post.liked() ? "* " : "- ") + post.likes();
            McnPrimitives.drawButton(graphics, style, font,
                    contentLeft + McnTheme.SPACE_XS, likeY,
                    48, McnTheme.BUTTON_HEIGHT_SM,
                    likes, McnPrimitives.ButtonVariant.SECONDARY, false, false);

            // Reply button
            McnPrimitives.drawButton(graphics, style, font,
                    contentLeft + McnTheme.SPACE_XS + 48 + McnTheme.SPACE_XXS, likeY,
                    62, McnTheme.BUTTON_HEIGHT_SM,
                    "Reply " + post.comments().size(),
                    McnPrimitives.ButtonVariant.SECONDARY, false, false);

            // Image thumbnail on the right if present
            if (post.hasImage()) {
                if (ClientSocialImageCache.markImageRequested(post.id())) {
                    PacketDistributor.sendToServer(new SocialImageRequestPacket(post.id()));
                }
                byte[] image = ClientSocialImageCache.imageForDisplay(post.id());
                int thumbWidth = 44;
                int thumbHeight = 40;
                int thumbX = contentRight - McnTheme.SPACE_S - thumbWidth;
                int thumbY = y + McnTheme.SPACE_XS;

                if (image.length > 0) {
                    PhonePhotoLibrary.drawThumbnail(graphics, "social-post-" + post.id(), image,
                            thumbX, thumbY, thumbWidth, thumbHeight);
                } else {
                    McnPrimitives.drawButton(graphics, style, font,
                            thumbX, thumbY + McnTheme.SPACE_S, thumbWidth, McnTheme.BUTTON_HEIGHT_MD,
                            "Photo", McnPrimitives.ButtonVariant.SECONDARY, false, false);
                }
            }
        }

        super.render(graphics, PhoneFrame.displayMouseX(this, mouseX),
                PhoneFrame.displayMouseY(this, mouseY), partialTick);
        PhoneFrame.renderStatusBar(graphics, this);
    }

    private void drawFilter(GuiGraphics graphics, PhoneStyle style, int x, int y, int width,
                            String label, String value) {
        if (filter.equals(value)) {
            PhoneRenderer.drawPrimaryButton(graphics, style, x, y, width, 20, label, font);
        } else {
            PhoneRenderer.drawSecondaryButton(graphics, style, x, y, width, 20, label, font);
        }
    }

    private String filterLabel() {
        return switch (filter) {
            case "FOLLOWING" -> "Posts from people you follow";
            case "MINE" -> "Your posts";
            default -> "All posts";
        };
    }

    private String emptyMessage() {
        return switch (filter) {
            case "FOLLOWING" -> "No posts from followed people";
            case "MINE" -> "You have not posted yet";
            default -> "No posts yet";
        };
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        double x = PhoneFrame.layoutMouseX(this, mouseX);
        double y = PhoneFrame.layoutMouseY(this, mouseY);

        if (!ClientSocialProfileCache.configured()) {
            return super.mouseClicked(x, y, button);
        }

        // Compute content bounds once (must match the render() method)
        int screenX = McnPrimitives.getScreenX(width);
        int screenY = McnPrimitives.getScreenY(height);
        int contentLeft = McnPrimitives.getContentLeft(screenX);
        int contentRight = McnPrimitives.getContentRight(screenX, McnPrimitives.getScreenWidth());
        int contentTop = McnPrimitives.getContentTop(screenY);

        // ------------------------------------------------------------------
        // Filter row
        // ------------------------------------------------------------------
        int filterY = contentTop + McnTheme.SPACE_S;
        int filterHeight = McnTheme.BUTTON_HEIGHT_SM + McnTheme.SPACE_XS;
        int filterInnerY = filterY + McnTheme.SPACE_XXS;
        int filterCursor = contentLeft + McnTheme.SPACE_XXS;

        if (button == 0) {
            if (inside(x, y, filterCursor, filterInnerY, 58, 20)) {
                selectFilter("ALL");
                return true;
            }
            filterCursor += 58 + McnTheme.SPACE_XXS;
            if (inside(x, y, filterCursor, filterInnerY, 62, 20)) {
                selectFilter("FOLLOWING");
                return true;
            }
            filterCursor += 62 + McnTheme.SPACE_XXS;
            if (inside(x, y, filterCursor, filterInnerY, 52, 20)) {
                selectFilter("MINE");
                return true;
            }

            // ----------------------------------------------------------------
            // Composer card
            // ----------------------------------------------------------------
            int composerY = filterY + filterHeight + McnTheme.SPACE_S;
            int composerHeight = McnTheme.TEXT_FIELD_HEIGHT + McnTheme.SPACE_M;
            int composerInnerY = composerY + McnTheme.SPACE_XS;

            // Send button
            int sendButtonWidth = 23;
            int sendX = contentRight - McnTheme.SPACE_S - sendButtonWidth;
            int sendY = composerInnerY;
            if (inside(x, y, sendX, sendY, sendButtonWidth, McnTheme.BUTTON_HEIGHT_SM)) {
                submitPost();
                return true;
            }

            // Gallery button
            int galleryWidth = 42;
            int galleryX = sendX - galleryWidth - McnTheme.SPACE_XXS;
            if (inside(x, y, galleryX, sendY, galleryWidth, McnTheme.BUTTON_HEIGHT_SM)) {
                minecraft.setScreen(new GalleryScreen(menu, this, null));
                return true;
            }

            // ----------------------------------------------------------------
            // Find people card
            // ----------------------------------------------------------------
            int findY = composerY + composerHeight + McnTheme.SPACE_S;
            int findHeight = McnTheme.BUTTON_HEIGHT_SM + McnTheme.SPACE_XS;
            if (inside(x, y, contentLeft, findY, contentRight - contentLeft, findHeight)) {
                // Only the "View" button area should open the people screen.
                int viewButtonWidth = 34;
                int viewX = contentRight - McnTheme.SPACE_S - viewButtonWidth;
                if (inside(x, y, viewX, findY, viewButtonWidth, findHeight)) {
                    minecraft.setScreen(new SocialPeopleScreen(menu));
                    return true;
                }
            }

            // ----------------------------------------------------------------
            // Posts
            // ----------------------------------------------------------------
            int filterLabelY = findY + findHeight + McnTheme.SPACE_S;
            int postsStartY = filterLabelY + McnTheme.TEXT_CAPTION + McnTheme.SPACE_XS;
            int cardHeight = 68;

            var posts = ClientSocialCache.posts();
            int first = Math.min(scroll, Math.max(0, posts.size() - VISIBLE_POSTS));

            for (int index = first; index < Math.min(posts.size(), first + VISIBLE_POSTS); index++) {
                int rowY = postsStartY + (index - first) * (cardHeight + McnTheme.SPACE_XS);
                var post = posts.get(index);

                if (!inside(x, y, contentLeft, rowY, contentRight - contentLeft, cardHeight)) {
                    continue;
                }

                // Like button
                int likeY = rowY + cardHeight - McnTheme.BUTTON_HEIGHT_SM - McnTheme.SPACE_XXS;
                if (inside(x, y, contentLeft + McnTheme.SPACE_XS, likeY, 48, McnTheme.BUTTON_HEIGHT_SM)) {
                    PacketDistributor.sendToServer(new SocialLikePacket(post.id(), filter));
                    return true;
                }

                // Reply button
                int replyX = contentLeft + McnTheme.SPACE_XS + 48 + McnTheme.SPACE_XXS;
                if (inside(x, y, replyX, likeY, 62, McnTheme.BUTTON_HEIGHT_SM)) {
                    commentPostId = post.id();
                    replyAuthor = post.author();
                    ClientSocialImageCache.clearPendingImage();
                    pendingPhoto = new byte[0];
                    composer.setHint(Component.literal("Write a reply"));
                    composer.setMaxLength(140);
                    setFocused(composer);
                    return true;
                }

                // Image thumbnail
                if (post.hasImage()) {
                    int thumbWidth = 44;
                    int thumbHeight = 40;
                    int thumbX = contentRight - McnTheme.SPACE_S - thumbWidth;
                    int thumbY = rowY + McnTheme.SPACE_XS;
                    if (inside(x, y, thumbX, thumbY, thumbWidth, thumbHeight)) {
                        if (ClientSocialImageCache.markImageRequested(post.id())) {
                            PacketDistributor.sendToServer(new SocialImageRequestPacket(post.id()));
                        }
                        minecraft.setScreen(new SocialImageViewerScreen(menu, post.id()));
                        return true;
                    }
                }
            }
        }

        return super.mouseClicked(x, y, button);
    }

    private static boolean inside(double x, double y, int left, int top, int width, int height) {
        return x >= left && x < left + width && y >= top && y < top + height;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY != 0) {
            scroll = Math.clamp(scroll + (scrollY < 0 ? 1 : -1), 0,
                    Math.max(0, ClientSocialCache.posts().size() - VISIBLE_POSTS));
            return true;
        }
        return super.mouseScrolled(PhoneFrame.layoutMouseX(this, mouseX),
                PhoneFrame.layoutMouseY(this, mouseY), scrollX, scrollY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return super.mouseReleased(PhoneFrame.layoutMouseX(this, mouseX),
                PhoneFrame.layoutMouseY(this, mouseY), button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return super.mouseDragged(PhoneFrame.layoutMouseX(this, mouseX),
                PhoneFrame.layoutMouseY(this, mouseY), button,
                PhoneFrame.layoutMouseDeltaX(this, dragX), PhoneFrame.layoutMouseDeltaY(this, dragY));
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        super.mouseMoved(PhoneFrame.layoutMouseX(this, mouseX), PhoneFrame.layoutMouseY(this, mouseY));
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
