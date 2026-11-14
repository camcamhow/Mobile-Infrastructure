package com.example.mobilecellnetwork.gui;

import com.example.mobilecellnetwork.gui.admin.AdminColors;
import com.example.mobilecellnetwork.gui.admin.AdminPrimitives;
import com.example.mobilecellnetwork.gui.admin.AdminTheme;
import com.example.mobilecellnetwork.gui.admin.AdminToast;
import com.example.mobilecellnetwork.gui.admin.AdminUndoManager;
import com.example.mobilecellnetwork.network.ClientPhoneCache;
import com.example.mobilecellnetwork.network.ClientServerAdminCache;
import com.example.mobilecellnetwork.network.ServerAdminDataPacket;
import com.example.mobilecellnetwork.network.ServerAdminRequestPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import net.minecraft.client.gui.Font;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.fml.common.EventBusSubscriber;
import org.lwjgl.glfw.GLFW;

public final class ServerBlockScreen extends AbstractContainerScreen<ServerMenu> {

    private enum ServerTab {
        NETWORKS, ROAMING, ANTENNAS, PLAYERS, PLANS, BILLING
    }

    private enum RoamingSubTab {
        ACTIVE, PROPOSED, PROPOSE
    }

    private enum ModalType {
        NONE,
        NEW_NETWORK,
        EDIT_NETWORK,
        CHANGE_PASSWORD,
        CONFIRM_DELETE,
        ASSIGN_ANTENNA_TO_NETWORK,
        SEND_ROAMING_PROPOSAL,
        EDIT_PLAN,
        CHANGE_PLAYER_NUMBER,
        CHANGE_PLAYER_PLAN
    }

    private record NetworkEntry(String id, String name, int towerCount, String ownerName,
                                String serverState, boolean passwordSet) {
    }

    private final ServerMenu menu;
    private ServerTab activeTab = ServerTab.NETWORKS;
    private RoamingSubTab activeRoamingSubTab = RoamingSubTab.ACTIVE;
    private ModalType activeModal = ModalType.NONE;
    private String modalInput1 = "";
    private String modalInput2 = "";
    private String modalInput3 = "";
    private String modalInput4 = "";
    private String modalInput5 = "";
    private String modalInput6 = "";
    private String modalContextId = "";
    private String modalContextLabel = "";
    private String modalError = "";
    private String focusedModalField = "input1";
    private String searchQuery = "";
    private String antennaSearchQuery = "";
    private String roamingSearchQuery = "";
    private String playerSearchQuery = "";
    private String planSearchQuery = "";
    private String billingPlayerQuery = "";
    private String focusedField = "";
    private String proposeTargetNetworkId = "";
    private String proposeRateMinute = "";
    private String proposeRateText = "";
    private String proposeRateMb = "";
    private String proposeMessage = "";
    private int scrollOffset = 0;
    private int scrollbarDragging = -1;
    private int contentLeft;
    private int contentTop;
    private int contentWidth;
    private int contentHeight;
    private int panelX;
    private int panelY;
    private int panelWidth;
    private int panelHeight;
    private int sidebarTop;
    private int contentAreaLeft;
    private int contentAreaTop;
    private int contentAreaWidth;
    private int contentAreaHeight;
    private int listTop;
    private int listBottom;
    private int listHeight;
    private int maxScrollOffset;
    private final Set<Integer> selectedAntennaIndices = new HashSet<>();
    private boolean searchFocused = false;
    private int searchCursorPos = 0;
    private int searchSelectionStart = 0;
    private int searchSelectionEnd = 0;

    public ServerBlockScreen(ServerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, Component.literal("Server Block Admin"));
        this.menu = menu;
        this.titleLabelX = 0;
        this.titleLabelY = -1000;
        this.inventoryLabelY = -1000;
    }

    @Override
    public void init() {
        // Register for mouse events to intercept JEI clicks
        NeoForge.EVENT_BUS.register(this);
        requestCurrentTabData();
    }

    @Override
    public void removed() {
        NeoForge.EVENT_BUS.unregister(this);
        super.removed();
    }

    private void requestCurrentTabData() {
        String tab = switch (activeTab) {
            case NETWORKS -> "Networks";
            case ROAMING -> "Roaming";
            case ANTENNAS -> "Antennas";
            case PLAYERS -> "Players";
            case PLANS -> "Plans";
            case BILLING -> "Billing";
        };
        PacketDistributor.sendToServer(new ServerAdminRequestPacket(tab));
    }

        @net.neoforged.bus.api.SubscribeEvent(priority = net.neoforged.bus.api.EventPriority.HIGHEST)
        public void onMouseEvent(net.neoforged.neoforge.client.event.InputEvent.MouseButton.Pre event) {
            if (this.minecraft == null || this.minecraft.screen != this) {
                return;
            }
            if (event.getButton() != 0) { // Only left click
                return;
            }
            if (event.getAction() != GLFW.GLFW_PRESS) { // Only button press, not release
                return;
            }
            // Cancel all left clicks to prevent JEI and other mods from processing them
            // while this screen is open (JEI can open recipes even when its overlay is hidden)
            event.setCanceled(true);
            // Handle our own clicks manually since canceling prevents mouseClicked from being called.
            // Scale mouse coordinates by GUI scale to match what vanilla passes to mouseClicked().
            double scale = this.minecraft.getWindow().getGuiScale();
            this.mouseClicked(
                this.minecraft.mouseHandler.xpos() / scale,
                this.minecraft.mouseHandler.ypos() / scale,
                event.getButton()
            );
        }

    @Override
        protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
            // All rendering is done in render() method
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Calculate panel dimensions
        int panelW = Math.min(AdminTheme.PANEL_PREFERRED_WIDTH, this.width - 20);
        int panelH = Math.min(AdminTheme.PANEL_PREFERRED_HEIGHT, this.height - 20);
        int panelX = (this.width - panelW) / 2;
        int panelY = (this.height - panelH) / 2;

        this.panelX = panelX;
        this.panelY = panelY;
        this.panelWidth = panelW;
        this.panelHeight = panelH;

        int sidebarTop = panelY + AdminTheme.HEADER_HEIGHT;
        int contentAreaLeft = panelX + AdminTheme.SIDEBAR_WIDTH;
        int contentAreaTop = panelY + AdminTheme.HEADER_HEIGHT;
        int contentAreaWidth = panelW - AdminTheme.SIDEBAR_WIDTH;
        int contentAreaHeight = panelH - AdminTheme.HEADER_HEIGHT - AdminTheme.FOOTER_HEIGHT;
        int footerY = panelY + panelH - AdminTheme.FOOTER_HEIGHT;

        this.sidebarTop = sidebarTop;
        this.contentAreaLeft = contentAreaLeft;
        this.contentAreaTop = contentAreaTop;
        this.contentAreaWidth = contentAreaWidth;
        this.contentAreaHeight = contentAreaHeight;

        int contentLeft = contentAreaLeft + AdminTheme.SPACE_L;
        int contentRight = panelX + panelW - AdminTheme.SPACE_L;
        this.contentLeft = contentLeft;
        int contentWidth = contentRight - contentLeft;
        this.contentWidth = contentWidth;
        this.contentTop = contentAreaTop + AdminTheme.SPACE_L;
        this.contentHeight = contentAreaHeight - AdminTheme.SPACE_L * 2;

        int searchBarY = this.contentTop;
        int listTop = searchBarY + AdminTheme.SEARCH_BAR_HEIGHT + AdminTheme.SPACE_S + AdminTheme.SPACE_S;
        int headerRowY = listTop;
        int listBottom = panelY + panelH - AdminTheme.FOOTER_HEIGHT - AdminTheme.SPACE_L;
        int listHeight = listBottom - listTop;

        this.listTop = listTop;
        this.listBottom = listBottom;
        this.listHeight = listHeight;

        // Calculate max scroll offset
        int totalListHeight = 0;
        if (activeTab == ServerTab.NETWORKS) {
            int itemCount = getFilteredNetworkCount();
            totalListHeight = Math.max(0,
                    itemCount * AdminTheme.ROW_HEIGHT_LARGE + (Math.max(0, itemCount - 1)) * AdminTheme.SPACE_XS - listHeight);
        } else if (activeTab == ServerTab.ANTENNAS) {
            int itemCount = getFilteredAntennaCount();
            totalListHeight = Math.max(0,
                    itemCount * AdminTheme.ROW_HEIGHT_LARGE + (Math.max(0, itemCount - 1)) * AdminTheme.SPACE_XS - listHeight);
        } else if (activeTab == ServerTab.PLAYERS) {
            int itemCount = getPlayerCount();
            totalListHeight = Math.max(0,
                    itemCount * AdminTheme.ROW_HEIGHT_LARGE + (Math.max(0, itemCount - 1)) * AdminTheme.SPACE_XS - listHeight);
        } else if (activeTab == ServerTab.PLANS) {
            int itemCount = getPlanCount();
            totalListHeight = Math.max(0,
                    itemCount * AdminTheme.ROW_HEIGHT_LARGE + (Math.max(0, itemCount - 1)) * AdminTheme.SPACE_XS - listHeight);
        }
                this.maxScrollOffset = Math.max(0, totalListHeight);

        this.scrollOffset = Math.clamp(this.scrollOffset, 0, this.maxScrollOffset);

        // Draw panel background
        AdminPrimitives.drawPanel(graphics, panelX, panelY, panelW, panelH);

        // Draw header
        AdminPrimitives.drawHeader(graphics, panelX, panelY, panelW, AdminTheme.HEADER_HEIGHT);
        drawLeftAlignedText(graphics, this.font, "Server Block", panelX + AdminTheme.SPACE_L, panelY,
                AdminTheme.TEXT_TITLE, AdminColors.TEXT_PRIMARY, AdminTheme.HEADER_HEIGHT);

        // Close button
        int closeBtnW = 60;
        int closeBtnH = AdminTheme.BUTTON_HEIGHT_SM;
        int closeBtnX = panelX + panelW - AdminTheme.SPACE_L - closeBtnW;
        int closeBtnY = panelY + (AdminTheme.HEADER_HEIGHT - closeBtnH) / 2;
        AdminPrimitives.drawButton(graphics, this.font, closeBtnX, closeBtnY, closeBtnW, closeBtnH,
                "Close", AdminPrimitives.ButtonVariant.SECONDARY, false, false);

        // Draw sidebar
        AdminPrimitives.drawSidebar(graphics, panelX, sidebarTop, AdminTheme.SIDEBAR_WIDTH,
                panelH - AdminTheme.HEADER_HEIGHT - AdminTheme.FOOTER_HEIGHT);

        // Sidebar items
        String[] tabLabels = {"Networks", "Roaming", "Antennas", "Players", "Plans", "Billing"};
        int[] tabBadges = {
            getFilteredNetworkCount(),
            getIncomingProposalCount(),
            0, // Antennas: unassigned count (stub: 0)
            getPlayerCount(),
            getPlanCount(),
            0 // Billing
        };
        int itemY = sidebarTop + AdminTheme.SPACE_M;
        for (int i = 0; i < tabLabels.length; i++) {
            ServerTab tab = ServerTab.values()[i];
            boolean active = tab == activeTab;
            boolean hovered = mouseX >= panelX && mouseX < panelX + AdminTheme.SIDEBAR_WIDTH &&
                    mouseY >= itemY && mouseY < itemY + AdminTheme.SIDEBAR_ITEM_HEIGHT;
            AdminPrimitives.drawSidebarItem(graphics, this.font, panelX, itemY,
                    AdminTheme.SIDEBAR_WIDTH, AdminTheme.SIDEBAR_ITEM_HEIGHT,
                    tabLabels[i], active, hovered, tabBadges[i]);
            itemY += AdminTheme.SIDEBAR_ITEM_HEIGHT + AdminTheme.SPACE_XXS;
        }

        // Sidebar footer - just a divider line now (text moved to bottom bar)
                                // itemY is now at the position after the last item (including its spacing)
                                int sidebarFooterTop = itemY + AdminTheme.SPACE_M;
                                // Divider line above footer
                                graphics.fill(panelX, sidebarFooterTop, panelX + AdminTheme.SIDEBAR_WIDTH, sidebarFooterTop + 1, AdminColors.SIDEBAR_DIVIDER);
                                // Footer text removed - moved to bottom bar

        // Draw content area background
        AdminPrimitives.drawContentArea(graphics, contentAreaLeft, contentAreaTop, contentAreaWidth, contentAreaHeight);

        // Render active tab content
        if (activeTab == ServerTab.NETWORKS) {
            renderNetworksTab(graphics, mouseX, mouseY);
        } else if (activeTab == ServerTab.ANTENNAS) {
            renderAntennaTab(graphics, mouseX, mouseY);
        } else if (activeTab == ServerTab.ROAMING) {
            renderRoamingTab(graphics, mouseX, mouseY);
        } else if (activeTab == ServerTab.PLAYERS) {
            renderPlayersTab(graphics, mouseX, mouseY);
        } else if (activeTab == ServerTab.PLANS) {
            renderPlansTab(graphics, mouseX, mouseY);
        } else if (activeTab == ServerTab.BILLING) {
            renderBillingTab(graphics, mouseX, mouseY);
        }

        // Draw footer
        AdminPrimitives.drawFooter(graphics, panelX, footerY, panelW, AdminTheme.FOOTER_HEIGHT);

                // Footer left: Revenue/Version info (vertically stacked at far left)
                                String revenueText = "Revenue: $0";
                                String versionText = "Version 1.0.0";
                                int leftX = panelX + AdminTheme.SPACE_S;
                                // Raise text up more: use smaller top margin to prevent version clipping at bottom
                                // First line baseline: footerY + 2 (tight top margin)
                                // Second line baseline: first baseline + line height + spacing
                                int firstLineBaseline = footerY + 2;
                                int secondLineBaseline = firstLineBaseline + AdminTheme.TEXT_CAPTION + AdminTheme.SPACE_XXS;
                                drawLeftAlignedText(graphics, this.font, revenueText, panelX + AdminTheme.SPACE_S, firstLineBaseline,
                                        AdminTheme.TEXT_CAPTION, AdminColors.TEXT_SECONDARY, AdminTheme.TEXT_CAPTION);
                                drawLeftAlignedText(graphics, this.font, versionText, panelX + AdminTheme.SPACE_S, secondLineBaseline,
                                        AdminTheme.TEXT_CAPTION, AdminColors.TEXT_MUTED, AdminTheme.TEXT_CAPTION);

                // Footer buttons - Refresh on the right
                int refreshBtnW = 80;
                int newBtnH = AdminTheme.BUTTON_HEIGHT_SM;
                int refreshBtnX = panelX + panelW - AdminTheme.SPACE_L - refreshBtnW;
                int refreshBtnY = footerY + (AdminTheme.FOOTER_HEIGHT - AdminTheme.BUTTON_HEIGHT_SM) / 2;
                AdminPrimitives.drawButton(graphics, this.font, refreshBtnX, refreshBtnY, refreshBtnW, newBtnH,
                        "Refresh", AdminPrimitives.ButtonVariant.SECONDARY, false, false);

                // Page indicator - centered in footer
                String pageText = "Page 1 of 1";
                int pageTextW = this.font.width(pageText);
                int pageX = panelX + (panelW - pageTextW) / 2;
                int pageY = footerY + (AdminTheme.FOOTER_HEIGHT - AdminTheme.TEXT_CAPTION) / 2;
                drawCenteredText(graphics, this.font, pageText, pageX, pageY, pageTextW, AdminTheme.FOOTER_HEIGHT,
                        AdminTheme.TEXT_CAPTION, AdminColors.TEXT_SECONDARY, true);

        // Draw scrollbar for list
        if ((activeTab == ServerTab.NETWORKS || activeTab == ServerTab.ANTENNAS) && maxScrollOffset > 0) {
            int scrollbarX = contentAreaLeft + contentAreaWidth - AdminTheme.SPACE_XS - 4;
            int scrollbarY = listTop;
            int scrollbarH = listHeight;
            AdminPrimitives.drawScrollbar(graphics, scrollbarX, scrollbarY, 4, scrollbarH,
                    (float) scrollOffset / maxScrollOffset, (float) listHeight / (listHeight + maxScrollOffset));
        }

        // Undo banner
        if (AdminUndoManager.canUndo()) {
            int bannerY = footerY - AdminTheme.SPACE_L - 30;
            int bannerW = panelW - AdminTheme.SPACE_L * 2;
            int bannerX = panelX + AdminTheme.SPACE_L;
            AdminPrimitives.drawCard(graphics, bannerX, bannerY, bannerW, 30);
            String desc = AdminUndoManager.getDescription();
            long remaining = AdminUndoManager.getRemainingMillis();
            String text = "Undo: " + desc + " (" + (remaining / 1000) + "s remaining)";
            drawLeftAlignedText(graphics, this.font, text, bannerX + AdminTheme.SPACE_M, bannerY,
                    AdminTheme.TEXT_CAPTION, AdminColors.TEXT_PRIMARY, 30);

            int undoBtnW = 60;
            int undoBtnH = 22;
            int undoBtnX = bannerX + bannerW - AdminTheme.SPACE_M - undoBtnW;
            int undoBtnY = bannerY + 4;
            AdminPrimitives.drawButton(graphics, this.font, undoBtnX, undoBtnY, undoBtnW, undoBtnH,
                    "Undo", AdminPrimitives.ButtonVariant.PRIMARY, false, false);
        }

        // Render toasts
        AdminToast.renderAll(graphics, this.font, this.width, this.height);

        // Update undo manager
        AdminUndoManager.tick();

        // Render modal overlay
        renderModal(graphics, mouseX, mouseY);
    }

    private void renderNetworksTab(GuiGraphics graphics, int mouseX, int mouseY) {
            // Search bar with New Network button on the right
            // Calculate space for New Network button on the right
            int newBtnW = 120;
            int newBtnH = AdminTheme.BUTTON_HEIGHT_SM;
            int newBtnX = contentLeft + contentWidth - AdminTheme.SPACE_L - newBtnW;
            int newBtnY = contentTop + (AdminTheme.SEARCH_BAR_HEIGHT - AdminTheme.BUTTON_HEIGHT_SM) / 2;
        
            // Adjust search bar width to make room for the button
            int searchBarW = contentWidth - newBtnW - AdminTheme.SPACE_L * 2;
            int searchBarX = contentLeft + AdminTheme.SPACE_L;
            int searchBarY = contentTop;
            int searchBarH = AdminTheme.SEARCH_BAR_HEIGHT;
        
            AdminPrimitives.drawSearchBar(graphics, this.font, searchBarX, searchBarY, searchBarW, searchBarH,
                    searchQuery, "Search networks", searchFocused,
                    searchFocused ? searchSelectionStart : 0, searchFocused ? searchSelectionEnd : 0);
        
            // Draw New Network button to the right of search bar
                        AdminPrimitives.drawButton(graphics, this.font, newBtnX, newBtnY, newBtnW, newBtnH,
                                "+ New Network", AdminPrimitives.ButtonVariant.PRIMARY, false, false);

            // Header row
            int rowY = listTop;
            List<NetworkEntry> networks = getFilteredNetworks();
            if (activeModal == ModalType.NONE) {
                int networkCount = networks.size();
                String networkLabel = networkCount + " network" + (networkCount == 1 ? "" : "s");
                drawLeftAlignedText(graphics, this.font, networkLabel, contentLeft, rowY,
                        AdminTheme.TEXT_CAPTION, AdminColors.TEXT_SECONDARY, 20);
            }

        String sortText = "Sort: Name";
        int sortW = this.font.width(sortText) + AdminTheme.SPACE_L * 2;
        int sortX = contentLeft + contentWidth - AdminTheme.SPACE_L - sortW;
        drawLeftAlignedText(graphics, this.font, sortText, sortX, rowY,
                AdminTheme.TEXT_CAPTION, AdminColors.TEXT_MUTED, 20);

        // Network list
        int cardY = listTop + AdminTheme.SPACE_L + AdminTheme.SPACE_S - scrollOffset;
        // Render empty state only when no networks exist and no modal is open
        if (activeModal == ModalType.NONE && networks.isEmpty()) {
                    String line1 = searchQuery.isBlank() ? "No networks yet." : "No networks found.";
                    String line2 = "Click + New Network to create one.";
                    int line1W = this.font.width(line1);
                    int line2W = this.font.width(line2);
                    int maxW = Math.max(line1W, line2W);
                    int textX = contentLeft + (contentWidth - maxW) / 2;
                    int textY = listTop + (listHeight - this.font.lineHeight * 2 - AdminTheme.SPACE_XXS) / 2;
                    drawLeftAlignedText(graphics, this.font, line1, contentLeft + (contentWidth - line1W) / 2, textY,
                            AdminTheme.TEXT_BODY, AdminColors.TEXT_MUTED, this.font.lineHeight);
                    drawLeftAlignedText(graphics, this.font, line2, contentLeft + (contentWidth - line2W) / 2, textY + this.font.lineHeight + AdminTheme.SPACE_XXS,
                            AdminTheme.TEXT_BODY, AdminColors.TEXT_MUTED, this.font.lineHeight);
                }

        for (NetworkEntry network : networks) {
            AdminPrimitives.drawCard(graphics, contentLeft, cardY, contentWidth, AdminTheme.ROW_HEIGHT_LARGE);
            int textX = contentLeft + AdminTheme.SPACE_L;
            drawLeftAlignedText(graphics, this.font, network.name(),
                    textX, cardY + 4, AdminTheme.TEXT_BODY, AdminColors.TEXT_PRIMARY,
                    AdminTheme.ROW_HEIGHT_LARGE / 2);
            drawLeftAlignedText(graphics, this.font,
                    "Owner: " + network.ownerName() + "  |  Towers: " + network.towerCount(),
                    textX, cardY + 4 + AdminTheme.TEXT_BODY,
                    AdminTheme.TEXT_CAPTION, AdminColors.TEXT_SECONDARY, AdminTheme.ROW_HEIGHT_LARGE / 2);
            drawLeftAlignedText(graphics, this.font,
                    "Server: " + network.serverState() + "  |  Password: "
                            + (network.passwordSet() ? "Set" : "Not set"),
                    textX, cardY + 4 + AdminTheme.TEXT_BODY * 2,
                    AdminTheme.TEXT_CAPTION, AdminColors.TEXT_SECONDARY, AdminTheme.ROW_HEIGHT_LARGE / 2);
            cardY += AdminTheme.ROW_HEIGHT_LARGE + AdminTheme.SPACE_XS;
        }
    }

    private List<NetworkEntry> getFilteredNetworks() {
        ServerAdminDataPacket snapshot = ClientServerAdminCache.snapshot();
        if (!"Networks".equals(snapshot.tab())) {
            return List.of();
        }

        String query = searchQuery.trim().toLowerCase(Locale.ROOT);
        List<NetworkEntry> networks = new ArrayList<>();
        for (String row : snapshot.entries()) {
            String[] fields = row.split("\\|", -1);
            if (fields.length != 12 || !"network".equals(fields[0])) {
                continue;
            }
            int towerCount;
            try {
                towerCount = Integer.parseInt(fields[3]);
            } catch (NumberFormatException exception) {
                continue;
            }

            NetworkEntry network = new NetworkEntry(fields[1], fields[2], towerCount, fields[5],
                    fields[7], Boolean.parseBoolean(fields[8]));
            if (query.isEmpty() || network.name().toLowerCase(Locale.ROOT).contains(query)
                    || network.id().toLowerCase(Locale.ROOT).contains(query)
                    || network.ownerName().toLowerCase(Locale.ROOT).contains(query)) {
                networks.add(network);
            }
        }
        return networks;
    }

    private int getFilteredNetworkCount() {
        return getFilteredNetworks().size();
    }

    // === ANTENNAS TAB ===

    private void renderAntennaTab(GuiGraphics graphics, int mouseX, int mouseY) {
        // Search bar
        int searchBarX = contentLeft + AdminTheme.SPACE_L;
        int searchBarY = contentTop;
        int searchBarW = contentWidth - AdminTheme.SPACE_L * 2;
        int searchBarH = AdminTheme.SEARCH_BAR_HEIGHT;
        boolean antSearchFocused = focusedField != null && focusedField.equals("antennaSearch");
        AdminPrimitives.drawSearchBar(graphics, this.font, searchBarX, searchBarY, searchBarW, searchBarH,
                antennaSearchQuery, "Search antennas", antSearchFocused,
                antSearchFocused ? searchSelectionStart : 0, antSearchFocused ? searchSelectionEnd : 0);

        // Header row
        int rowY = listTop;
        int antennaCount = getFilteredAntennaCount();
        String antennaLabel = antennaCount + " antenna" + (antennaCount == 1 ? "" : "s");
        drawLeftAlignedText(graphics, this.font, antennaLabel, contentLeft, rowY,
                AdminTheme.TEXT_CAPTION, AdminColors.TEXT_SECONDARY, 20);

        String sortText = "Sort: Position";
        int sortW = this.font.width(sortText) + AdminTheme.SPACE_L * 2;
        int sortX = contentLeft + contentWidth - AdminTheme.SPACE_L - sortW;
        drawLeftAlignedText(graphics, this.font, sortText, sortX, rowY,
                AdminTheme.TEXT_CAPTION, AdminColors.TEXT_MUTED, 20);

        // Bulk action bar (visible only if 1+ row selected)
        if (!selectedAntennaIndices.isEmpty()) {
            int bulkY = rowY + AdminTheme.ROW_HEIGHT_LARGE + AdminTheme.SPACE_S;
            int btnW = 100;
            int btnH = AdminTheme.BUTTON_HEIGHT_SM;
            int btnX = contentLeft;
            AdminPrimitives.drawButton(graphics, this.font, btnX, bulkY, btnW, btnH,
                    "Assign to Network", AdminPrimitives.ButtonVariant.PRIMARY, false, false);
            btnX += btnW + AdminTheme.SPACE_XS;
            AdminPrimitives.drawButton(graphics, this.font, btnX, bulkY, btnW, btnH,
                    "Unclaim", AdminPrimitives.ButtonVariant.SECONDARY, false, false);
            btnX += btnW + AdminTheme.SPACE_XS;
            AdminPrimitives.drawButton(graphics, this.font, btnX, bulkY, btnW, btnH,
                    "Delete", AdminPrimitives.ButtonVariant.DESTRUCTIVE, false, false);
            btnX += btnW + AdminTheme.SPACE_XS;
            AdminPrimitives.drawButton(graphics, this.font, btnX, bulkY, btnW, btnH,
                    "Clear", AdminPrimitives.ButtonVariant.GHOST, false, false);
        }

        // Antenna list
        int cardY = listTop + AdminTheme.SPACE_L + AdminTheme.SPACE_S - scrollOffset;
        if (antennaCount == 0) {
            String line1 = "No antennas registered";
            int line1W = this.font.width(line1);
            int textX = contentLeft + (contentWidth - line1W) / 2;
            int textY = listTop + (listHeight - this.font.lineHeight) / 2;
            drawLeftAlignedText(graphics, this.font, line1, textX, textY,
                    AdminTheme.TEXT_BODY, AdminColors.TEXT_MUTED, this.font.lineHeight);
        } else {
            for (int i = 0; i < antennaCount; i++) {
                int rowY2 = cardY;
                boolean selected = selectedAntennaIndices.contains(i);
                boolean hovered = mouseX >= contentLeft && mouseX < contentLeft + contentWidth &&
                        mouseY >= rowY2 && mouseY < rowY2 + AdminTheme.ROW_HEIGHT_LARGE;

                AdminPrimitives.drawCard(graphics, contentLeft, rowY2, contentWidth, AdminTheme.ROW_HEIGHT_LARGE);

                int cbSize = 18;
                int cbX = contentLeft + AdminTheme.SPACE_S;
                int cbY = rowY2 + (AdminTheme.ROW_HEIGHT_LARGE - cbSize) / 2;
                AdminPrimitives.drawButton(graphics, this.font, cbX, cbY, cbSize, cbSize,
                        selected ? "X" : "", AdminPrimitives.ButtonVariant.SECONDARY, selected, hovered);

                int textX = cbX + cbSize + AdminTheme.SPACE_S;
                drawLeftAlignedText(graphics, this.font, "Tier 1 Antenna @ 100, 64, -200",
                        textX, rowY2 + 4, AdminTheme.TEXT_BODY, AdminColors.TEXT_PRIMARY, AdminTheme.ROW_HEIGHT_LARGE / 2);
                drawLeftAlignedText(graphics, this.font, "Network: Unassigned - Owner: Player1",
                        textX, rowY2 + 4 + AdminTheme.TEXT_BODY, AdminTheme.TEXT_CAPTION, AdminColors.TEXT_SECONDARY, AdminTheme.ROW_HEIGHT_LARGE / 2);
                drawLeftAlignedText(graphics, this.font, "Power: ACTIVE - Loaded",
                        textX, rowY2 + 4 + AdminTheme.TEXT_BODY * 2, AdminTheme.TEXT_CAPTION, AdminColors.TEXT_SECONDARY, AdminTheme.ROW_HEIGHT_LARGE / 2);

                int btnW = 70;
                int btnH = AdminTheme.BUTTON_HEIGHT_SM;
                int btnX2 = contentLeft + contentWidth - AdminTheme.SPACE_L - btnW;
                int btnY2 = rowY2 + (AdminTheme.ROW_HEIGHT_LARGE - btnH) / 2;
                AdminPrimitives.drawButton(graphics, this.font, btnX2, btnY2, btnW, btnH,
                        "Claim", AdminPrimitives.ButtonVariant.PRIMARY, false, false);

                cardY += AdminTheme.ROW_HEIGHT_LARGE + AdminTheme.SPACE_XS;
            }
        }
    }

    private int getFilteredAntennaCount() {
        return 0;
    }

    // === ROAMING TAB ===

    private void renderRoamingTab(GuiGraphics graphics, int mouseX, int mouseY) {
        // Sub-tabs at top of content area
        int subTabY = contentTop;
        int subTabW = 100;
        int subTabH = AdminTheme.BUTTON_HEIGHT_SM;
        int subTabX = contentLeft;

        String[] subTabLabels = {"Active", "Proposed", "Propose"};
        RoamingSubTab[] subTabs = {RoamingSubTab.ACTIVE, RoamingSubTab.PROPOSED, RoamingSubTab.PROPOSE};

        for (int i = 0; i < subTabs.length; i++) {
            boolean active = subTabs[i] == activeRoamingSubTab;
            boolean hovered = mouseX >= subTabX && mouseX < subTabX + subTabW &&
                    mouseY >= subTabY && mouseY < subTabY + subTabH;
            AdminPrimitives.drawTab(graphics, this.font, subTabX, subTabY, subTabW, subTabH,
                    subTabLabels[i], active, hovered);
            subTabX += subTabW + AdminTheme.SPACE_XS;
        }

        int contentStartY = subTabY + subTabH + AdminTheme.SPACE_M;

        if (activeRoamingSubTab == RoamingSubTab.ACTIVE) {
            renderRoamingActive(graphics, mouseX, mouseY, contentStartY);
        } else if (activeRoamingSubTab == RoamingSubTab.PROPOSED) {
            renderRoamingProposed(graphics, mouseX, mouseY, contentStartY);
        } else if (activeRoamingSubTab == RoamingSubTab.PROPOSE) {
            renderRoamingPropose(graphics, mouseX, mouseY, contentStartY);
        }
    }

    private void renderRoamingActive(GuiGraphics graphics, int mouseX, int mouseY, int startY) {
        // Search bar
        int searchBarX = contentLeft + AdminTheme.SPACE_L;
        int searchBarY = startY;
        int searchBarW = contentWidth - AdminTheme.SPACE_L * 2;
        int searchBarH = AdminTheme.SEARCH_BAR_HEIGHT;
        boolean roamSearchFocused = focusedField != null && focusedField.equals("roamingSearch");
        AdminPrimitives.drawSearchBar(graphics, this.font, searchBarX, searchBarY, searchBarW, searchBarH,
                roamingSearchQuery, "Search agreements", roamSearchFocused,
                roamSearchFocused ? searchSelectionStart : 0, roamSearchFocused ? searchSelectionEnd : 0);

        int headerY = startY + AdminTheme.SEARCH_BAR_HEIGHT + AdminTheme.SPACE_S;

        // Header
        int agreementCount = getActiveAgreementCount();
        String label = agreementCount + " active agreement" + (agreementCount == 1 ? "" : "s");
        drawLeftAlignedText(graphics, this.font, label, contentLeft, headerY,
                AdminTheme.TEXT_CAPTION, AdminColors.TEXT_SECONDARY, 20);

        // List
        int cardY = headerY + AdminTheme.TEXT_CAPTION + AdminTheme.SPACE_S - scrollOffset;
        if (agreementCount == 0) {
            String line1 = "No active roaming agreements";
            int line1W = this.font.width(line1);
            int textX = contentLeft + (contentWidth - line1W) / 2;
            int textY = headerY + (listHeight - this.font.lineHeight) / 2;
            drawLeftAlignedText(graphics, this.font, line1, textX, textY,
                    AdminTheme.TEXT_BODY, AdminColors.TEXT_MUTED, this.font.lineHeight);
        } else {
            for (int i = 0; i < agreementCount; i++) {
                AdminPrimitives.drawCard(graphics, contentLeft, cardY, contentWidth, AdminTheme.ROW_HEIGHT_LARGE);
                int textX = contentLeft + AdminTheme.SPACE_L;
                drawLeftAlignedText(graphics, this.font, "Network: Net-01",
                        textX, cardY + 4, AdminTheme.TEXT_BODY, AdminColors.TEXT_PRIMARY, AdminTheme.ROW_HEIGHT_LARGE / 2);
                drawLeftAlignedText(graphics, this.font, "Rates: $0.10/min - $0.05/text - $0.01/MB",
                        textX, cardY + 4 + AdminTheme.TEXT_BODY, AdminTheme.TEXT_CAPTION, AdminColors.TEXT_SECONDARY, AdminTheme.ROW_HEIGHT_LARGE / 2);
                drawLeftAlignedText(graphics, this.font, "Agreed: 2d ago",
                        textX, cardY + 4 + AdminTheme.TEXT_BODY * 2, AdminTheme.TEXT_CAPTION, AdminColors.TEXT_SECONDARY, AdminTheme.ROW_HEIGHT_LARGE / 2);

                int btnW = 70;
                int btnH = AdminTheme.BUTTON_HEIGHT_SM;
                int btnX = contentLeft + contentWidth - AdminTheme.SPACE_L - btnW;
                int btnY = cardY + (AdminTheme.ROW_HEIGHT_LARGE - btnH) / 2;
                AdminPrimitives.drawButton(graphics, this.font, btnX, btnY, btnW, btnH,
                        "Revoke", AdminPrimitives.ButtonVariant.DESTRUCTIVE, false, false);

                cardY += AdminTheme.ROW_HEIGHT_LARGE + AdminTheme.SPACE_XS;
            }
        }
    }

    private void renderRoamingProposed(GuiGraphics graphics, int mouseX, int mouseY, int startY) {
        // Outgoing proposals section
        int sectionY = startY;
        drawLeftAlignedText(graphics, this.font, "OUTGOING PROPOSALS", contentLeft, sectionY,
                AdminTheme.TEXT_CAPTION, AdminColors.TEXT_SECONDARY, 20);

        int cardY = sectionY + AdminTheme.TEXT_CAPTION + AdminTheme.SPACE_S - scrollOffset;
        int outgoingCount = getOutgoingProposalCount();
        if (outgoingCount == 0) {
            drawLeftAlignedText(graphics, this.font, "No outgoing proposals", contentLeft, cardY,
                    AdminTheme.TEXT_BODY, AdminColors.TEXT_MUTED, this.font.lineHeight);
        } else {
            for (int i = 0; i < outgoingCount; i++) {
                AdminPrimitives.drawCard(graphics, contentLeft, cardY, contentWidth, AdminTheme.ROW_HEIGHT_LARGE);
                int textX = contentLeft + AdminTheme.SPACE_L;
                drawLeftAlignedText(graphics, this.font, "To: Net-01",
                        textX, cardY + 4, AdminTheme.TEXT_BODY, AdminColors.TEXT_PRIMARY, AdminTheme.ROW_HEIGHT_LARGE / 2);
                drawLeftAlignedText(graphics, this.font, "Rates: $0.10/min - $0.05/text - $0.01/MB",
                        textX, cardY + 4 + AdminTheme.TEXT_BODY, AdminTheme.TEXT_CAPTION, AdminColors.TEXT_SECONDARY, AdminTheme.ROW_HEIGHT_LARGE / 2);
                drawLeftAlignedText(graphics, this.font, "Status: PENDING",
                        textX, cardY + 4 + AdminTheme.TEXT_BODY * 2, AdminTheme.TEXT_CAPTION, AdminColors.TEXT_SECONDARY, AdminTheme.ROW_HEIGHT_LARGE / 2);

                int btnW = 70;
                int btnH = AdminTheme.BUTTON_HEIGHT_SM;
                int btnX = contentLeft + contentWidth - AdminTheme.SPACE_L - btnW;
                int btnY = cardY + (AdminTheme.ROW_HEIGHT_LARGE - btnH) / 2;
                AdminPrimitives.drawButton(graphics, this.font, btnX, btnY, btnW, btnH,
                        "Cancel", AdminPrimitives.ButtonVariant.DESTRUCTIVE, false, false);

                cardY += AdminTheme.ROW_HEIGHT_LARGE + AdminTheme.SPACE_XS;
            }
        }

        // Incoming proposals section
        sectionY = cardY + AdminTheme.SPACE_M;
        drawLeftAlignedText(graphics, this.font, "INCOMING PROPOSALS", contentLeft, sectionY,
                AdminTheme.TEXT_CAPTION, AdminColors.TEXT_SECONDARY, 20);

        cardY = sectionY + AdminTheme.TEXT_CAPTION + AdminTheme.SPACE_S - scrollOffset;
        int incomingCount = getIncomingProposalCount();
        if (incomingCount == 0) {
            drawLeftAlignedText(graphics, this.font, "No incoming proposals", contentLeft, cardY,
                    AdminTheme.TEXT_BODY, AdminColors.TEXT_MUTED, this.font.lineHeight);
        } else {
            for (int i = 0; i < incomingCount; i++) {
                AdminPrimitives.drawCard(graphics, contentLeft, cardY, contentWidth, AdminTheme.ROW_HEIGHT_LARGE);
                int textX = contentLeft + AdminTheme.SPACE_L;
                drawLeftAlignedText(graphics, this.font, "From: Net-01",
                        textX, cardY + 4, AdminTheme.TEXT_BODY, AdminColors.TEXT_PRIMARY, AdminTheme.ROW_HEIGHT_LARGE / 2);
                drawLeftAlignedText(graphics, this.font, "Rates: $0.10/min - $0.05/text - $0.01/MB",
                        textX, cardY + 4 + AdminTheme.TEXT_BODY, AdminTheme.TEXT_CAPTION, AdminColors.TEXT_SECONDARY, AdminTheme.ROW_HEIGHT_LARGE / 2);
                drawLeftAlignedText(graphics, this.font, "Message: -",
                        textX, cardY + 4 + AdminTheme.TEXT_BODY * 2, AdminTheme.TEXT_CAPTION, AdminColors.TEXT_SECONDARY, AdminTheme.ROW_HEIGHT_LARGE / 2);

                int btnW = 70;
                int btnH = AdminTheme.BUTTON_HEIGHT_SM;
                int btnX = contentLeft + contentWidth - AdminTheme.SPACE_L - btnW * 3 - AdminTheme.SPACE_XS * 2;
                int btnY = cardY + (AdminTheme.ROW_HEIGHT_LARGE - btnH) / 2;
                AdminPrimitives.drawButton(graphics, this.font, btnX, btnY, btnW, btnH,
                        "Accept", AdminPrimitives.ButtonVariant.PRIMARY, false, false);
                btnX += btnW + AdminTheme.SPACE_XS;
                AdminPrimitives.drawButton(graphics, this.font, btnX, btnY, btnW, btnH,
                        "Counter", AdminPrimitives.ButtonVariant.SECONDARY, false, false);
                btnX += btnW + AdminTheme.SPACE_XS;
                AdminPrimitives.drawButton(graphics, this.font, btnX, btnY, btnW, btnH,
                        "Decline", AdminPrimitives.ButtonVariant.DESTRUCTIVE, false, false);

                cardY += AdminTheme.ROW_HEIGHT_LARGE + AdminTheme.SPACE_XS;
            }
        }
    }

    private void renderRoamingPropose(GuiGraphics graphics, int mouseX, int mouseY, int startY) {
        drawLeftAlignedText(graphics, this.font, "SEND A ROAMING PROPOSAL", contentLeft, startY,
                AdminTheme.TEXT_CAPTION, AdminColors.TEXT_SECONDARY, 20);

        int labelW = 140;
        int fieldH = AdminTheme.TEXT_BODY + 8;
        int rowSpacing = AdminTheme.SPACE_L;

        // Compact mode if form exceeds content area height
        int formHeight = AdminTheme.TEXT_CAPTION + AdminTheme.SPACE_M
                + 5 * (fieldH + rowSpacing)
                + AdminTheme.BUTTON_HEIGHT_SM + AdminTheme.SPACE_L;
        boolean compact = (startY + formHeight > contentTop + contentHeight);
        if (compact) {
            fieldH = AdminTheme.BUTTON_HEIGHT_SM;
            rowSpacing = AdminTheme.SPACE_M;
        }

        int fieldW = contentWidth - AdminTheme.SPACE_L * 2 - labelW;
        int fieldY = startY + AdminTheme.TEXT_CAPTION + AdminTheme.SPACE_M;

        // Target network
        drawLeftAlignedText(graphics, this.font, "Target network:", contentLeft, fieldY,
                AdminTheme.TEXT_BODY, AdminColors.TEXT_PRIMARY, AdminTheme.TEXT_BODY);
        boolean tfTargetFocused = focusedField != null && focusedField.equals("proposeTarget");
        AdminPrimitives.drawTextField(graphics, this.font, contentLeft + labelW, fieldY - 4, fieldW, fieldH,
                proposeTargetNetworkId, "Target network", tfTargetFocused ? AdminPrimitives.InputState.FOCUSED : AdminPrimitives.InputState.NORMAL, searchCursorPos, tfTargetFocused, fieldSelStart("proposeTarget"), fieldSelEnd("proposeTarget"));
        fieldY += fieldH + rowSpacing;

        // Rate per minute
        drawLeftAlignedText(graphics, this.font, "Rate per minute:", contentLeft, fieldY,
                AdminTheme.TEXT_BODY, AdminColors.TEXT_PRIMARY, AdminTheme.TEXT_BODY);
        boolean tfRateMinFocused = focusedField != null && focusedField.equals("proposeRateMinute");
        AdminPrimitives.drawTextField(graphics, this.font, contentLeft + labelW, fieldY - 4, fieldW, fieldH,
                proposeRateMinute, "Rate/min", tfRateMinFocused ? AdminPrimitives.InputState.FOCUSED : AdminPrimitives.InputState.NORMAL, searchCursorPos, tfRateMinFocused, fieldSelStart("proposeRateMinute"), fieldSelEnd("proposeRateMinute"));
        fieldY += fieldH + rowSpacing;

        // Rate per text
        drawLeftAlignedText(graphics, this.font, "Rate per text:", contentLeft, fieldY,
                AdminTheme.TEXT_BODY, AdminColors.TEXT_PRIMARY, AdminTheme.TEXT_BODY);
        boolean tfRateTextFocused = focusedField != null && focusedField.equals("proposeRateText");
        AdminPrimitives.drawTextField(graphics, this.font, contentLeft + labelW, fieldY - 4, fieldW, fieldH,
                proposeRateText, "Rate/text", tfRateTextFocused ? AdminPrimitives.InputState.FOCUSED : AdminPrimitives.InputState.NORMAL, searchCursorPos, tfRateTextFocused, fieldSelStart("proposeRateText"), fieldSelEnd("proposeRateText"));
        fieldY += fieldH + rowSpacing;

        // Rate per MB
        drawLeftAlignedText(graphics, this.font, "Rate per MB:", contentLeft, fieldY,
                AdminTheme.TEXT_BODY, AdminColors.TEXT_PRIMARY, AdminTheme.TEXT_BODY);
        boolean tfRateMbFocused = focusedField != null && focusedField.equals("proposeRateMb");
        AdminPrimitives.drawTextField(graphics, this.font, contentLeft + labelW, fieldY - 4, fieldW, fieldH,
                proposeRateMb, "Rate/MB", tfRateMbFocused ? AdminPrimitives.InputState.FOCUSED : AdminPrimitives.InputState.NORMAL, searchCursorPos, tfRateMbFocused, fieldSelStart("proposeRateMb"), fieldSelEnd("proposeRateMb"));
        fieldY += fieldH + rowSpacing;

        // Optional message
        drawLeftAlignedText(graphics, this.font, "Optional message:", contentLeft, fieldY,
                AdminTheme.TEXT_BODY, AdminColors.TEXT_PRIMARY, AdminTheme.TEXT_BODY);
        boolean tfMessageFocused = focusedField != null && focusedField.equals("proposeMessage");
        AdminPrimitives.drawTextField(graphics, this.font, contentLeft + labelW, fieldY - 4, fieldW, fieldH,
                proposeMessage, "Message", tfMessageFocused ? AdminPrimitives.InputState.FOCUSED : AdminPrimitives.InputState.NORMAL, searchCursorPos, tfMessageFocused, fieldSelStart("proposeMessage"), fieldSelEnd("proposeMessage"));
        fieldY += fieldH + rowSpacing;

        // Send Proposal button at bottom of content area with SPACE_L clearance above it
        int btnW = 140;
        int btnH = AdminTheme.BUTTON_HEIGHT_SM;
        int btnX = contentLeft + (contentWidth - btnW) / 2;
        int btnY = contentTop + contentHeight - AdminTheme.SPACE_L - btnH;
        boolean btnHovered = mouseX >= btnX && mouseX < btnX + btnW && mouseY >= btnY && mouseY < btnY + btnH;
        AdminPrimitives.drawButton(graphics, this.font, btnX, btnY, btnW, btnH,
                "Send Proposal", AdminPrimitives.ButtonVariant.PRIMARY, false, btnHovered);
    }

    // === PLAYERS TAB ===

    private void renderPlayersTab(GuiGraphics graphics, int mouseX, int mouseY) {
        // Search bar
        int searchBarX = contentLeft + AdminTheme.SPACE_L;
        int searchBarY = contentTop;
        int searchBarW = contentWidth - AdminTheme.SPACE_L * 2;
        int searchBarH = AdminTheme.SEARCH_BAR_HEIGHT;
        boolean playerSearchFocused = focusedField != null && focusedField.equals("playerSearch");
        AdminPrimitives.drawSearchBar(graphics, this.font, searchBarX, searchBarY, searchBarW, searchBarH,
                playerSearchQuery, "Search players", playerSearchFocused,
                playerSearchFocused ? searchSelectionStart : 0, playerSearchFocused ? searchSelectionEnd : 0);

        // Header row
        int rowY = listTop;
        int playerCount = getPlayerCount();
        String playerLabel = playerCount + " online";
        drawLeftAlignedText(graphics, this.font, playerLabel, contentLeft, rowY,
                AdminTheme.TEXT_CAPTION, AdminColors.TEXT_SECONDARY, 20);

        String sortText = "Sort: Name";
        int sortW = this.font.width(sortText) + AdminTheme.SPACE_L * 2;
        int sortX = contentLeft + contentWidth - AdminTheme.SPACE_L - sortW;
        drawLeftAlignedText(graphics, this.font, sortText, sortX, rowY,
                AdminTheme.TEXT_CAPTION, AdminColors.TEXT_MUTED, 20);

        // Player list
        int cardY = listTop + AdminTheme.SPACE_L + AdminTheme.SPACE_S - scrollOffset;
        if (playerCount == 0) {
            String line1 = "No players tracked yet";
            int line1W = this.font.width(line1);
            int textX = contentLeft + (contentWidth - line1W) / 2;
            int textY = listTop + (listHeight - this.font.lineHeight) / 2;
            drawLeftAlignedText(graphics, this.font, line1, textX, textY,
                    AdminTheme.TEXT_BODY, AdminColors.TEXT_MUTED, this.font.lineHeight);
        } else {
            for (int i = 0; i < playerCount; i++) {
                AdminPrimitives.drawCard(graphics, contentLeft, cardY, contentWidth, AdminTheme.ROW_HEIGHT_LARGE);
                int textX = contentLeft + AdminTheme.SPACE_L;
                drawLeftAlignedText(graphics, this.font, "PlayerName",
                        textX, cardY + 4, AdminTheme.TEXT_BODY, AdminColors.TEXT_PRIMARY, AdminTheme.ROW_HEIGHT_LARGE / 2);
                drawLeftAlignedText(graphics, this.font, "Plan: None - #555-1234",
                        textX, cardY + 4 + AdminTheme.TEXT_BODY, AdminTheme.TEXT_CAPTION, AdminColors.TEXT_SECONDARY, AdminTheme.ROW_HEIGHT_LARGE / 2);
                drawLeftAlignedText(graphics, this.font, "Balance: $0.00",
                        textX, cardY + 4 + AdminTheme.TEXT_BODY * 2, AdminTheme.TEXT_CAPTION, AdminColors.TEXT_SECONDARY, AdminTheme.ROW_HEIGHT_LARGE / 2);

                int btnW = 70;
                int btnH = AdminTheme.BUTTON_HEIGHT_SM;
                int btnX = contentLeft + contentWidth - AdminTheme.SPACE_L - btnW * 3 - AdminTheme.SPACE_XS * 2;
                int btnY = cardY + (AdminTheme.ROW_HEIGHT_LARGE - btnH) / 2;
                AdminPrimitives.drawButton(graphics, this.font, btnX, btnY, btnW, btnH,
                        "Plan", AdminPrimitives.ButtonVariant.SECONDARY, false, false);
                btnX += btnW + AdminTheme.SPACE_XS;
                AdminPrimitives.drawButton(graphics, this.font, btnX, btnY, btnW, btnH,
                        "Number", AdminPrimitives.ButtonVariant.SECONDARY, false, false);
                btnX += btnW + AdminTheme.SPACE_XS;
                AdminPrimitives.drawButton(graphics, this.font, btnX, btnY, btnW, btnH,
                        "Balance", AdminPrimitives.ButtonVariant.SECONDARY, false, false);

                cardY += AdminTheme.ROW_HEIGHT_LARGE + AdminTheme.SPACE_XS;
            }
        }
    }

    // === PLANS TAB ===

    private void renderPlansTab(GuiGraphics graphics, int mouseX, int mouseY) {
        // Search bar
        int searchBarX = contentLeft + AdminTheme.SPACE_L;
        int searchBarY = contentTop;
        int searchBarW = contentWidth - AdminTheme.SPACE_L * 2;
        int searchBarH = AdminTheme.SEARCH_BAR_HEIGHT;
        boolean planSearchFocused = focusedField != null && focusedField.equals("planSearch");
        AdminPrimitives.drawSearchBar(graphics, this.font, searchBarX, searchBarY, searchBarW, searchBarH,
                planSearchQuery, "Search plans", planSearchFocused,
                planSearchFocused ? searchSelectionStart : 0, planSearchFocused ? searchSelectionEnd : 0);

        // Header row
        int rowY = listTop;
        int planCount = getPlanCount();
        String planLabel = planCount + " plan" + (planCount == 1 ? "" : "s");
        drawLeftAlignedText(graphics, this.font, planLabel, contentLeft, rowY,
                AdminTheme.TEXT_CAPTION, AdminColors.TEXT_SECONDARY, 20);

        String sortText = "Sort: Price";
        int sortW = this.font.width(sortText) + AdminTheme.SPACE_L * 2;
        int sortX = contentLeft + contentWidth - AdminTheme.SPACE_L - sortW;
        drawLeftAlignedText(graphics, this.font, sortText, sortX, rowY,
                AdminTheme.TEXT_CAPTION, AdminColors.TEXT_MUTED, 20);

        // Plan list
        int cardY = listTop + AdminTheme.SPACE_L + AdminTheme.SPACE_S - scrollOffset;
        if (planCount == 0) {
            String line1 = "No plans defined";
            int line1W = this.font.width(line1);
            int textX = contentLeft + (contentWidth - line1W) / 2;
            int textY = listTop + (listHeight - this.font.lineHeight) / 2;
            drawLeftAlignedText(graphics, this.font, line1, textX, textY,
                    AdminTheme.TEXT_BODY, AdminColors.TEXT_MUTED, this.font.lineHeight);
        } else {
            for (int i = 0; i < planCount; i++) {
                AdminPrimitives.drawCard(graphics, contentLeft, cardY, contentWidth, AdminTheme.ROW_HEIGHT_LARGE);
                int textX = contentLeft + AdminTheme.SPACE_L;
                drawLeftAlignedText(graphics, this.font, "Basic Plan",
                        textX, cardY + 4, AdminTheme.TEXT_BODY, AdminColors.TEXT_PRIMARY, AdminTheme.ROW_HEIGHT_LARGE / 2);
                drawLeftAlignedText(graphics, this.font, "$29.99",
                        contentLeft + contentWidth - AdminTheme.SPACE_L - this.font.width("$29.99"), cardY + 4,
                        AdminTheme.TEXT_BODY, AdminColors.TEXT_PRIMARY, AdminTheme.ROW_HEIGHT_LARGE / 2);
                drawLeftAlignedText(graphics, this.font, "1 tier - 30d cycle - 500min - 1000texts - 5000MB",
                        textX, cardY + 4 + AdminTheme.TEXT_BODY, AdminTheme.TEXT_CAPTION, AdminColors.TEXT_SECONDARY, AdminTheme.ROW_HEIGHT_LARGE / 2);
                drawLeftAlignedText(graphics, this.font, "Carrier: Any",
                        textX, cardY + 4 + AdminTheme.TEXT_BODY * 2, AdminTheme.TEXT_CAPTION, AdminColors.TEXT_SECONDARY, AdminTheme.ROW_HEIGHT_LARGE / 2);
                drawLeftAlignedText(graphics, this.font, "Roaming: CHARGED - Overage: yes",
                        textX, cardY + 4 + AdminTheme.TEXT_BODY * 3, AdminTheme.TEXT_CAPTION, AdminColors.TEXT_SECONDARY, AdminTheme.ROW_HEIGHT_LARGE / 2);

                int btnW = 70;
                int btnH = AdminTheme.BUTTON_HEIGHT_SM;
                int btnX = contentLeft + contentWidth - AdminTheme.SPACE_L - btnW * 2 - AdminTheme.SPACE_XS;
                int btnY = cardY + (AdminTheme.ROW_HEIGHT_LARGE - btnH) / 2;
                AdminPrimitives.drawButton(graphics, this.font, btnX, btnY, btnW, btnH,
                        "Edit", AdminPrimitives.ButtonVariant.SECONDARY, false, false);
                btnX += btnW + AdminTheme.SPACE_XS;
                AdminPrimitives.drawButton(graphics, this.font, btnX, btnY, btnW, btnH,
                        "Delete", AdminPrimitives.ButtonVariant.DESTRUCTIVE, false, false);

                cardY += AdminTheme.ROW_HEIGHT_LARGE + AdminTheme.SPACE_XS;
            }
        }
    }

    // === BILLING TAB ===

    private void renderBillingTab(GuiGraphics graphics, int mouseX, int mouseY) {
        // Header section
        drawLeftAlignedText(graphics, this.font, "PLAYER BILLING", contentLeft, contentTop,
                AdminTheme.TEXT_CAPTION, AdminColors.TEXT_SECONDARY, 20);

        // Player query field
        int fieldY = contentTop + AdminTheme.TEXT_CAPTION + AdminTheme.SPACE_M;
        int labelW = 120;
        int fieldW = contentWidth - AdminTheme.SPACE_L * 2 - labelW;

        drawLeftAlignedText(graphics, this.font, "Player:", contentLeft, fieldY,
                AdminTheme.TEXT_BODY, AdminColors.TEXT_PRIMARY, AdminTheme.TEXT_BODY);
        boolean billingFocused = focusedField != null && focusedField.equals("billingPlayer");
        AdminPrimitives.drawTextField(graphics, this.font, contentLeft + labelW, fieldY - 4, fieldW, AdminTheme.TEXT_BODY + 8,
                billingPlayerQuery, "Player name", billingFocused ? AdminPrimitives.InputState.FOCUSED : AdminPrimitives.InputState.NORMAL, searchCursorPos, billingFocused, fieldSelStart("billingPlayer"), fieldSelEnd("billingPlayer"));

        fieldY += AdminTheme.TEXT_BODY + AdminTheme.SPACE_S + AdminTheme.TEXT_BODY + 8 + AdminTheme.SPACE_S;

        // Force Renewal / View History buttons
        int btnW = 120;
        int btnH = AdminTheme.BUTTON_HEIGHT_SM;
        int btnX = contentLeft;
        int btnY = fieldY;
        boolean forceHovered = mouseX >= btnX && mouseX < btnX + btnW && mouseY >= btnY && mouseY < btnY + btnH;
        AdminPrimitives.drawButton(graphics, this.font, btnX, btnY, btnW, btnH,
                "Force Renewal", AdminPrimitives.ButtonVariant.PRIMARY, false, forceHovered);
        btnX += btnW + AdminTheme.SPACE_XS;
        boolean viewHovered = mouseX >= btnX && mouseX < btnX + btnW && mouseY >= btnY && mouseY < btnY + btnH;
        AdminPrimitives.drawButton(graphics, this.font, btnX, btnY, btnW, btnH,
                "View History", AdminPrimitives.ButtonVariant.SECONDARY, false, viewHovered);

        fieldY += btnH + AdminTheme.SPACE_M;

        // Recent Transactions section header
        drawLeftAlignedText(graphics, this.font, "RECENT TRANSACTIONS", contentLeft, fieldY,
                AdminTheme.TEXT_CAPTION, AdminColors.TEXT_SECONDARY, 20);

        // Transaction list
        int cardY = fieldY + AdminTheme.TEXT_CAPTION + AdminTheme.SPACE_S - scrollOffset;
        int transactionCount = getTransactionCount();
        if (transactionCount == 0) {
            String line1 = "No transactions recorded";
            int line1W = this.font.width(line1);
            int textX = contentLeft + (contentWidth - line1W) / 2;
            int textY = fieldY + (listHeight - this.font.lineHeight) / 2;
            drawLeftAlignedText(graphics, this.font, line1, textX, textY,
                    AdminTheme.TEXT_BODY, AdminColors.TEXT_MUTED, this.font.lineHeight);
        } else {
            for (int i = 0; i < transactionCount; i++) {
                AdminPrimitives.drawCard(graphics, contentLeft, cardY, contentWidth, AdminTheme.ROW_HEIGHT_LARGE);
                int textX = contentLeft + AdminTheme.SPACE_L;
                drawLeftAlignedText(graphics, this.font, "2d ago",
                        textX, cardY + 4, AdminTheme.TEXT_BODY, AdminColors.TEXT_PRIMARY, AdminTheme.ROW_HEIGHT_LARGE / 2);
                drawLeftAlignedText(graphics, this.font, "Player1",
                        contentLeft + AdminTheme.SPACE_L + 80, cardY + 4,
                        AdminTheme.TEXT_BODY, AdminColors.TEXT_PRIMARY, AdminTheme.ROW_HEIGHT_LARGE / 2);
                drawLeftAlignedText(graphics, this.font, "Monthly renewal",
                        contentLeft + AdminTheme.SPACE_L + 200, cardY + 4,
                        AdminTheme.TEXT_BODY, AdminColors.TEXT_PRIMARY, AdminTheme.ROW_HEIGHT_LARGE / 2);
                drawLeftAlignedText(graphics, this.font, "-$29.99",
                        contentLeft + contentWidth - AdminTheme.SPACE_L - this.font.width("-$29.99"), cardY + 4,
                        AdminTheme.TEXT_BODY, AdminColors.TEXT_PRIMARY, AdminTheme.ROW_HEIGHT_LARGE / 2);

                cardY += AdminTheme.ROW_HEIGHT_LARGE + AdminTheme.SPACE_XS;
            }
        }
    }

    // === SERVER BLOCK MODAL ===

    private void renderModal(GuiGraphics graphics, int mouseX, int mouseY) {
        if (activeModal == ModalType.NONE) return;

        int panelW = Math.min(AdminTheme.PANEL_PREFERRED_WIDTH, this.width - 20);
        int panelH = Math.min(AdminTheme.PANEL_PREFERRED_HEIGHT, this.height - 20);
        int panelX = (this.width - panelW) / 2;
        int panelY = (this.height - panelH) / 2;

        // Modal panel
        int modalW = Math.min(360, panelW - 40);
        int modalX = panelX + (panelW - modalW) / 2;
        int modalY = panelY + (panelH - 200) / 2;
        int modalH = switch (activeModal) {
            case NEW_NETWORK, CHANGE_PASSWORD -> 240;
            case EDIT_NETWORK -> 220;
            case CONFIRM_DELETE -> 200;
            case ASSIGN_ANTENNA_TO_NETWORK -> 240;
            case SEND_ROAMING_PROPOSAL -> 260;
            case EDIT_PLAN -> 420;
            case CHANGE_PLAYER_NUMBER -> 200;
            case CHANGE_PLAYER_PLAN -> 280;
            default -> 200;
        };

        AdminPrimitives.drawPanel(graphics, modalX, modalY, modalW, modalH);

        // Title
        String title = switch (activeModal) {
            case NEW_NETWORK -> "New Network";
            case EDIT_NETWORK -> "Edit Network";
            case CHANGE_PASSWORD -> "Change Password";
            case CONFIRM_DELETE -> "Confirm Delete";
            case ASSIGN_ANTENNA_TO_NETWORK -> "Assign Antenna";
            case SEND_ROAMING_PROPOSAL -> "Send Roaming Proposal";
            case EDIT_PLAN -> "Edit Plan";
            case CHANGE_PLAYER_NUMBER -> "Change Phone Number";
            case CHANGE_PLAYER_PLAN -> "Change Player Plan";
            default -> "Modal";
        };
        drawLeftAlignedText(graphics, this.font, title, modalX + AdminTheme.SPACE_L, modalY + AdminTheme.SPACE_L,
                AdminTheme.TEXT_TITLE, AdminColors.TEXT_PRIMARY, AdminTheme.TEXT_TITLE);

        // Subtitle
        drawLeftAlignedText(graphics, this.font, modalContextLabel, modalX + AdminTheme.SPACE_L,
                modalY + AdminTheme.SPACE_L + AdminTheme.TEXT_TITLE, AdminTheme.TEXT_CAPTION,
                AdminColors.TEXT_SECONDARY, AdminTheme.TEXT_CAPTION);

        int fieldY = modalY + AdminTheme.SPACE_L + AdminTheme.TEXT_TITLE + AdminTheme.TEXT_CAPTION + AdminTheme.SPACE_M;
        int labelW = 180;
        int fieldW = modalW - AdminTheme.SPACE_L * 2 - labelW;
        int fieldH = AdminTheme.TEXT_BODY + 8;

        switch (activeModal) {
            case NEW_NETWORK -> {
                drawLeftAlignedText(graphics, this.font, "Network name:", modalX + AdminTheme.SPACE_L, fieldY,
                        AdminTheme.TEXT_BODY, AdminColors.TEXT_PRIMARY, fieldH);
                boolean f1 = focusedModalField.equals("input1");
                AdminPrimitives.drawTextField(graphics, this.font, modalX + labelW, fieldY - 4, fieldW,
                        AdminTheme.TEXT_BODY + 8, modalInput1, "Network name",
                        f1 ? AdminPrimitives.InputState.FOCUSED : AdminPrimitives.InputState.NORMAL,
                searchCursorPos, f1, selStartFor("input1"), selEndFor("input1"));
                fieldY += AdminTheme.TEXT_BODY + AdminTheme.SPACE_S + AdminTheme.TEXT_BODY + 8 + AdminTheme.SPACE_S;

                drawLeftAlignedText(graphics, this.font, "Password:", modalX + AdminTheme.SPACE_L, fieldY,
                        AdminTheme.TEXT_BODY, AdminColors.TEXT_PRIMARY, fieldH);
                boolean f2 = focusedModalField.equals("input2");
                String masked = mask(modalInput2);
                AdminPrimitives.drawTextField(graphics, this.font, modalX + labelW, fieldY - 4, fieldW,
                        AdminTheme.TEXT_BODY + 8, masked, "Password",
                        f2 ? AdminPrimitives.InputState.FOCUSED : AdminPrimitives.InputState.NORMAL,
                searchCursorPos, f2, selStartFor("input2"), selEndFor("input2"));
                drawLeftAlignedText(graphics, this.font, "(min 4 chars)", modalX + AdminTheme.SPACE_L,
                        fieldY + AdminTheme.TEXT_BODY + 8 + AdminTheme.SPACE_S,
                        AdminTheme.TEXT_CAPTION, AdminColors.TEXT_MUTED, AdminTheme.TEXT_CAPTION);
                fieldY += AdminTheme.TEXT_BODY + AdminTheme.SPACE_S + AdminTheme.TEXT_BODY + 8 + AdminTheme.SPACE_S
                        + AdminTheme.TEXT_CAPTION + AdminTheme.SPACE_S;
            }
            case EDIT_NETWORK -> {
                drawLeftAlignedText(graphics, this.font, "Display name:", modalX + AdminTheme.SPACE_L, fieldY,
                        AdminTheme.TEXT_BODY, AdminColors.TEXT_PRIMARY, fieldH);
                boolean f1 = focusedModalField.equals("input1");
                AdminPrimitives.drawTextField(graphics, this.font, modalX + labelW, fieldY - 4, fieldW,
                        AdminTheme.TEXT_BODY + 8, modalInput1, "Display name",
                        f1 ? AdminPrimitives.InputState.FOCUSED : AdminPrimitives.InputState.NORMAL,
                searchCursorPos, f1, selStartFor("input1"), selEndFor("input1"));
                fieldY += AdminTheme.TEXT_BODY + AdminTheme.SPACE_S + AdminTheme.TEXT_BODY + 8 + AdminTheme.SPACE_S;

                drawLeftAlignedText(graphics, this.font, "Allow roaming:", modalX + AdminTheme.SPACE_L, fieldY,
                        AdminTheme.TEXT_BODY, AdminColors.TEXT_PRIMARY, fieldH);
                boolean f2 = focusedModalField.equals("input2");
                AdminPrimitives.drawTextField(graphics, this.font, modalX + labelW, fieldY - 4, fieldW,
                        AdminTheme.TEXT_BODY + 8, modalInput2, "true/false",
                        f2 ? AdminPrimitives.InputState.FOCUSED : AdminPrimitives.InputState.NORMAL,
                searchCursorPos, f2, selStartFor("input2"), selEndFor("input2"));
            }
            case CHANGE_PASSWORD -> {
                drawLeftAlignedText(graphics, this.font, "New password:", modalX + AdminTheme.SPACE_L, fieldY,
                        AdminTheme.TEXT_BODY, AdminColors.TEXT_PRIMARY, fieldH);
                boolean f1 = focusedModalField.equals("input1");
                AdminPrimitives.drawTextField(graphics, this.font, modalX + labelW, fieldY - 4, fieldW,
                        AdminTheme.TEXT_BODY + 8, modalInput1, "New password",
                        f1 ? AdminPrimitives.InputState.FOCUSED : AdminPrimitives.InputState.NORMAL,
                searchCursorPos, f1, selStartFor("input1"), selEndFor("input1"));
                fieldY += AdminTheme.TEXT_BODY + AdminTheme.SPACE_S + AdminTheme.TEXT_BODY + 8 + AdminTheme.SPACE_S;

                drawLeftAlignedText(graphics, this.font, "Confirm password:", modalX + AdminTheme.SPACE_L, fieldY,
                        AdminTheme.TEXT_BODY, AdminColors.TEXT_PRIMARY, fieldH);
                boolean f2 = focusedModalField.equals("input2");
                String masked = mask(modalInput2);
                AdminPrimitives.drawTextField(graphics, this.font, modalX + labelW, fieldY - 4, fieldW,
                        AdminTheme.TEXT_BODY + 8, masked, "Confirm password",
                        f2 ? AdminPrimitives.InputState.FOCUSED : AdminPrimitives.InputState.NORMAL,
                searchCursorPos, f2, selStartFor("input2"), selEndFor("input2"));
            }
            case CONFIRM_DELETE -> {
                drawLeftAlignedText(graphics, this.font, "This cannot be undone.", modalX + AdminTheme.SPACE_L, fieldY,
                        AdminTheme.TEXT_BODY, AdminColors.TEXT_PRIMARY, fieldH);
                fieldY += fieldH + AdminTheme.SPACE_S;
                drawLeftAlignedText(graphics, this.font, "Type DELETE below to confirm.", modalX + AdminTheme.SPACE_L,
                        fieldY, AdminTheme.TEXT_CAPTION, AdminColors.TEXT_SECONDARY, AdminTheme.TEXT_CAPTION);
                fieldY += AdminTheme.TEXT_CAPTION + AdminTheme.SPACE_S + AdminTheme.SPACE_M;
                drawLeftAlignedText(graphics, this.font, "Confirm:", modalX + AdminTheme.SPACE_L, fieldY,
                        AdminTheme.TEXT_BODY, AdminColors.TEXT_PRIMARY, fieldH);
                boolean f1 = focusedModalField.equals("input1");
                AdminPrimitives.drawTextField(graphics, this.font, modalX + labelW, fieldY - 4, fieldW,
                        AdminTheme.TEXT_BODY + 8, modalInput1, "Type DELETE",
                        f1 ? AdminPrimitives.InputState.FOCUSED : AdminPrimitives.InputState.NORMAL,
                        searchCursorPos, f1, selStartFor("input1"), selEndFor("input1"));
            }
            case ASSIGN_ANTENNA_TO_NETWORK -> {
                drawLeftAlignedText(graphics, this.font, "Select a network:", modalX + AdminTheme.SPACE_L, fieldY,
                        AdminTheme.TEXT_BODY, AdminColors.TEXT_PRIMARY, fieldH);
                fieldY += fieldH + AdminTheme.SPACE_M;
                drawLeftAlignedText(graphics, this.font, "(no networks available)", modalX + AdminTheme.SPACE_L, fieldY,
                        AdminTheme.TEXT_BODY, AdminColors.TEXT_MUTED, fieldH);
            }
            case SEND_ROAMING_PROPOSAL -> {
                int propLabelW = 160;
                int propFieldW = modalW - AdminTheme.SPACE_L * 2 - propLabelW;
                int propFieldH = AdminTheme.TEXT_BODY + 8;

                drawLeftAlignedText(graphics, this.font, "Target network:", modalX + AdminTheme.SPACE_L, fieldY,
                        AdminTheme.TEXT_BODY, AdminColors.TEXT_PRIMARY, propFieldH);
                boolean pf1 = focusedModalField.equals("input1");
                AdminPrimitives.drawTextField(graphics, this.font, modalX + propLabelW, fieldY - 4, propFieldW,
                        propFieldH, modalInput1, "Target network ID",
                        pf1 ? AdminPrimitives.InputState.FOCUSED : AdminPrimitives.InputState.NORMAL,
                        searchCursorPos, pf1, selStartFor("input1"), selEndFor("input1"));
                fieldY += propFieldH + AdminTheme.SPACE_M;

                drawLeftAlignedText(graphics, this.font, "Rate per minute:", modalX + AdminTheme.SPACE_L, fieldY,
                        AdminTheme.TEXT_BODY, AdminColors.TEXT_PRIMARY, propFieldH);
                boolean pf2 = focusedModalField.equals("input2");
                AdminPrimitives.drawTextField(graphics, this.font, modalX + propLabelW, fieldY - 4, propFieldW,
                        propFieldH, modalInput2, "Rate/min",
                        pf2 ? AdminPrimitives.InputState.FOCUSED : AdminPrimitives.InputState.NORMAL,
                        searchCursorPos, pf2, selStartFor("input2"), selEndFor("input2"));
                fieldY += propFieldH + AdminTheme.SPACE_M;

                drawLeftAlignedText(graphics, this.font, "Rate per text:", modalX + AdminTheme.SPACE_L, fieldY,
                        AdminTheme.TEXT_BODY, AdminColors.TEXT_PRIMARY, propFieldH);
                boolean pf3 = focusedModalField.equals("input3");
                AdminPrimitives.drawTextField(graphics, this.font, modalX + propLabelW, fieldY - 4, propFieldW,
                        propFieldH, modalInput3, "Rate/text",
                        pf3 ? AdminPrimitives.InputState.FOCUSED : AdminPrimitives.InputState.NORMAL,
                        searchCursorPos, pf3, selStartFor("input3"), selEndFor("input3"));
                fieldY += propFieldH + AdminTheme.SPACE_M;

                drawLeftAlignedText(graphics, this.font, "Rate per MB:", modalX + AdminTheme.SPACE_L, fieldY,
                        AdminTheme.TEXT_BODY, AdminColors.TEXT_PRIMARY, propFieldH);
                boolean pf4 = focusedModalField.equals("input4");
                AdminPrimitives.drawTextField(graphics, this.font, modalX + propLabelW, fieldY - 4, propFieldW,
                        propFieldH, modalInput4, "Rate/MB",
                        pf4 ? AdminPrimitives.InputState.FOCUSED : AdminPrimitives.InputState.NORMAL,
                        searchCursorPos, pf4, selStartFor("input4"), selEndFor("input4"));
                fieldY += propFieldH + AdminTheme.SPACE_M;

                drawLeftAlignedText(graphics, this.font, "Optional message:", modalX + AdminTheme.SPACE_L, fieldY,
                        AdminTheme.TEXT_BODY, AdminColors.TEXT_PRIMARY, propFieldH);
                boolean pf5 = focusedModalField.equals("input5");
                AdminPrimitives.drawTextField(graphics, this.font, modalX + propLabelW, fieldY - 4, propFieldW,
                        propFieldH, modalInput5, "Message",
                        pf5 ? AdminPrimitives.InputState.FOCUSED : AdminPrimitives.InputState.NORMAL,
                        searchCursorPos, pf5, selStartFor("input5"), selEndFor("input5"));
            }
            case EDIT_PLAN -> {
                int fieldStartY = modalY + 60;
                int rowHeight = 32;
                int planLabelW = 160;
                int planFieldW = modalW - AdminTheme.SPACE_L * 2 - planLabelW;
                int planFieldH = AdminTheme.TEXT_BODY + 8;

                drawLeftAlignedText(graphics, this.font, "Display name:", modalX + AdminTheme.SPACE_L, fieldStartY,
                        AdminTheme.TEXT_BODY, AdminColors.TEXT_PRIMARY, planFieldH);
                boolean ep1 = focusedModalField.equals("input1");
                AdminPrimitives.drawTextField(graphics, this.font, modalX + planLabelW, fieldStartY - 4, planFieldW,
                        planFieldH, modalInput1, "Display name",
                        ep1 ? AdminPrimitives.InputState.FOCUSED : AdminPrimitives.InputState.NORMAL,
                        searchCursorPos, ep1, selStartFor("input1"), selEndFor("input1"));

                drawLeftAlignedText(graphics, this.font, "Price:", modalX + AdminTheme.SPACE_L, fieldStartY + rowHeight,
                        AdminTheme.TEXT_BODY, AdminColors.TEXT_PRIMARY, planFieldH);
                boolean ep2 = focusedModalField.equals("input2");
                AdminPrimitives.drawTextField(graphics, this.font, modalX + planLabelW, fieldStartY + rowHeight - 4, planFieldW,
                        planFieldH, modalInput2, "Price",
                        ep2 ? AdminPrimitives.InputState.FOCUSED : AdminPrimitives.InputState.NORMAL,
                        searchCursorPos, ep2, selStartFor("input2"), selEndFor("input2"));

                drawLeftAlignedText(graphics, this.font, "Cycle days:", modalX + AdminTheme.SPACE_L, fieldStartY + rowHeight * 2,
                        AdminTheme.TEXT_BODY, AdminColors.TEXT_PRIMARY, planFieldH);
                boolean ep3 = focusedModalField.equals("input3");
                AdminPrimitives.drawTextField(graphics, this.font, modalX + planLabelW, fieldStartY + rowHeight * 2 - 4, planFieldW,
                        planFieldH, modalInput3, "Days",
                        ep3 ? AdminPrimitives.InputState.FOCUSED : AdminPrimitives.InputState.NORMAL,
                        searchCursorPos, ep3, selStartFor("input3"), selEndFor("input3"));

                drawLeftAlignedText(graphics, this.font, "Call minutes (-1):", modalX + AdminTheme.SPACE_L, fieldStartY + rowHeight * 3,
                        AdminTheme.TEXT_BODY, AdminColors.TEXT_PRIMARY, planFieldH);
                boolean ep4 = focusedModalField.equals("input4");
                AdminPrimitives.drawTextField(graphics, this.font, modalX + planLabelW, fieldStartY + rowHeight * 3 - 4, planFieldW,
                        planFieldH, modalInput4, "-1 for unlimited",
                        ep4 ? AdminPrimitives.InputState.FOCUSED : AdminPrimitives.InputState.NORMAL,
                        searchCursorPos, ep4, selStartFor("input4"), selEndFor("input4"));

                drawLeftAlignedText(graphics, this.font, "Texts (-1):", modalX + AdminTheme.SPACE_L, fieldStartY + rowHeight * 4,
                        AdminTheme.TEXT_BODY, AdminColors.TEXT_PRIMARY, planFieldH);
                boolean ep5 = focusedModalField.equals("input5");
                AdminPrimitives.drawTextField(graphics, this.font, modalX + planLabelW, fieldStartY + rowHeight * 4 - 4, planFieldW,
                        planFieldH, modalInput5, "-1 for unlimited",
                        ep5 ? AdminPrimitives.InputState.FOCUSED : AdminPrimitives.InputState.NORMAL,
                        searchCursorPos, ep5, selStartFor("input5"), selEndFor("input5"));

                drawLeftAlignedText(graphics, this.font, "Data MB (-1):", modalX + AdminTheme.SPACE_L, fieldStartY + rowHeight * 5,
                        AdminTheme.TEXT_BODY, AdminColors.TEXT_PRIMARY, planFieldH);
                boolean ep6 = focusedModalField.equals("input6");
                AdminPrimitives.drawTextField(graphics, this.font, modalX + planLabelW, fieldStartY + rowHeight * 5 - 4, planFieldW,
                        planFieldH, modalInput6, "-1 for unlimited",
                        ep6 ? AdminPrimitives.InputState.FOCUSED : AdminPrimitives.InputState.NORMAL,
                        searchCursorPos, ep6, selStartFor("input6"), selEndFor("input6"));
            }
            case CHANGE_PLAYER_NUMBER -> {
                drawLeftAlignedText(graphics, this.font, "New number:", modalX + AdminTheme.SPACE_L, fieldY,
                        AdminTheme.TEXT_BODY, AdminColors.TEXT_PRIMARY, fieldH);
                boolean cpn1 = focusedModalField.equals("input1");
                AdminPrimitives.drawTextField(graphics, this.font, modalX + labelW, fieldY - 4, fieldW,
                        fieldH, modalInput1, "1-9 digits",
                        cpn1 ? AdminPrimitives.InputState.FOCUSED : AdminPrimitives.InputState.NORMAL,
                        searchCursorPos, cpn1, selStartFor("input1"), selEndFor("input1"));
            }
            case CHANGE_PLAYER_PLAN -> {
                drawLeftAlignedText(graphics, this.font, "Select a plan:", modalX + AdminTheme.SPACE_L, fieldY,
                        AdminTheme.TEXT_BODY, AdminColors.TEXT_PRIMARY, fieldH);
                fieldY += fieldH + AdminTheme.SPACE_M;
                drawLeftAlignedText(graphics, this.font, "(no plans available)", modalX + AdminTheme.SPACE_L, fieldY,
                        AdminTheme.TEXT_BODY, AdminColors.TEXT_MUTED, fieldH);
            }
            default -> {
            }
        }

        // Error message
        if (modalError != null && !modalError.isEmpty()) {
            drawLeftAlignedText(graphics, this.font, modalError, modalX + AdminTheme.SPACE_L, fieldY + AdminTheme.SPACE_S,
                    AdminTheme.TEXT_CAPTION, AdminColors.ERROR, AdminTheme.TEXT_CAPTION);
            fieldY += AdminTheme.TEXT_CAPTION + AdminTheme.SPACE_S;
        }

        // Buttons
        int btnW = 100;
        int btnH = AdminTheme.BUTTON_HEIGHT_SM;
        int btnY = modalY + modalH - AdminTheme.SPACE_L - btnH;
        int btnX1 = modalX + AdminTheme.SPACE_L;
        int btnX2 = modalX + modalW - AdminTheme.SPACE_L - btnW;

        boolean cancelHovered = mouseX >= btnX1 && mouseX < btnX1 + btnW && mouseY >= btnY && mouseY < btnY + btnH;
        AdminPrimitives.drawButton(graphics, this.font, btnX1, btnY, btnW, btnH,
                "Cancel", AdminPrimitives.ButtonVariant.SECONDARY, false, cancelHovered);

        String confirmText = switch (activeModal) {
            case NEW_NETWORK -> "Create";
            case EDIT_NETWORK -> "Save";
            case CHANGE_PASSWORD -> "Change";
            case CONFIRM_DELETE -> "Delete";
            case ASSIGN_ANTENNA_TO_NETWORK -> "Assign";
            case SEND_ROAMING_PROPOSAL -> "Send";
            default -> "OK";
        };
        AdminPrimitives.ButtonVariant confirmVariant = switch (activeModal) {
            case CONFIRM_DELETE -> AdminPrimitives.ButtonVariant.DESTRUCTIVE;
            default -> AdminPrimitives.ButtonVariant.PRIMARY;
        };
        boolean confirmHovered = mouseX >= btnX2 && mouseX < btnX2 + btnW && mouseY >= btnY && mouseY < btnY + btnH;
        AdminPrimitives.drawButton(graphics, this.font, btnX2, btnY, btnW, btnH,
                confirmText, confirmVariant, false, confirmHovered);
    }

    private int getActiveAgreementCount() {
        return 0;
    }

    private int getOutgoingProposalCount() {
        return 0;
    }

    private int getIncomingProposalCount() {
        return 0;
    }

    private int getPlayerCount() {
        return 0;
    }

    private int getPlanCount() {
        return 0;
    }

    private int getTransactionCount() {
        return 0;
    }

    // Helper methods for text rendering
    private void drawLeftAlignedText(GuiGraphics graphics, Font font, String text, int x, int y,
                                     int textSize, int color, int height) {
        if (text == null || text.isEmpty()) return;
        float scale = textSize / (float) font.lineHeight;
        int textY = y + (height - (int) (font.lineHeight * scale)) / 2;
        graphics.pose().pushPose();
        graphics.pose().translate(x, textY, 0);
        graphics.pose().scale(scale, scale, 1);
        graphics.drawString(font, text, 0, 0, color, true);
        graphics.pose().popPose();
    }

    private void drawCenteredText(GuiGraphics graphics, Font font, String text, int x, int y, int width, int height,
                                  int textSize, int color, boolean shadow) {
        if (text == null || text.isEmpty() || width <= 0 || height <= 0) return;
        float scale = textSize / (float) font.lineHeight;
        graphics.pose().pushPose();
        graphics.pose().translate(x + width / 2.0, y + height / 2.0, 0);
        graphics.pose().scale(scale, scale, 1);
        graphics.drawString(font, text, -font.width(text) / 2, -font.lineHeight / 2, color, shadow);
        graphics.pose().popPose();
    }

    private String getFocusedFieldValue() {
        switch (focusedField) {
            case "antennaSearch": return antennaSearchQuery;
            case "roamingSearch": return roamingSearchQuery;
            case "playerSearch": return playerSearchQuery;
            case "planSearch": return planSearchQuery;
            case "billingPlayer": return billingPlayerQuery;
            case "proposeTarget": return proposeTargetNetworkId;
            case "proposeRateMinute": return proposeRateMinute;
            case "proposeRateText": return proposeRateText;
            case "proposeRateMb": return proposeRateMb;
            case "proposeMessage": return proposeMessage;
            default: return "";
        }
    }

    private void setFocusedFieldValue(String value) {
        switch (focusedField) {
            case "antennaSearch": antennaSearchQuery = value; break;
            case "roamingSearch": roamingSearchQuery = value; break;
            case "playerSearch": playerSearchQuery = value; break;
            case "planSearch": planSearchQuery = value; break;
            case "billingPlayer": billingPlayerQuery = value; break;
            case "proposeTarget": proposeTargetNetworkId = value; break;
            case "proposeRateMinute": proposeRateMinute = value; break;
            case "proposeRateText": proposeRateText = value; break;
            case "proposeRateMb": proposeRateMb = value; break;
            case "proposeMessage": proposeMessage = value; break;
        }
    }

    private String getModalFieldValue() {
        return switch (focusedModalField) {
            case "input1" -> modalInput1;
            case "input2" -> modalInput2;
            case "input3" -> modalInput3;
            case "input4" -> modalInput4;
            case "input5" -> modalInput5;
            case "input6" -> modalInput6;
            default -> "";
        };
    }

    private void setModalFieldValue(String field, String value) {
        switch (field) {
            case "input1" -> modalInput1 = value;
            case "input2" -> modalInput2 = value;
            case "input3" -> modalInput3 = value;
            case "input4" -> modalInput4 = value;
            case "input5" -> modalInput5 = value;
            case "input6" -> modalInput6 = value;
        }
    }

    // === Text Selection Helpers ===

    private int getSelStart() {
        return Math.min(searchSelectionStart, searchSelectionEnd);
    }

    private int getSelEnd() {
        return Math.max(searchSelectionStart, searchSelectionEnd);
    }

    private boolean hasSelection() {
        return searchSelectionStart != searchSelectionEnd;
    }

    private String deleteSelectedText(String val) {
        if (!hasSelection()) return val;
        int start = Math.min(getSelStart(), val.length());
        int end = Math.min(Math.max(getSelEnd(), start), val.length());
        return val.substring(0, start) + val.substring(end);
    }

    private int selStartFor(String fieldName) {
        return focusedModalField != null && focusedModalField.equals(fieldName) ? searchSelectionStart : 0;
    }

    private int selEndFor(String fieldName) {
        return focusedModalField != null && focusedModalField.equals(fieldName) ? searchSelectionEnd : 0;
    }

    private int fieldSelStart(String fieldName) {
        return focusedField != null && focusedField.equals(fieldName) ? searchSelectionStart : 0;
    }

    private int fieldSelEnd(String fieldName) {
        return focusedField != null && focusedField.equals(fieldName) ? searchSelectionEnd : 0;
    }

    private String insertTextAtCursor(String val, String text) {
        if (hasSelection()) {
            val = deleteSelectedText(val);
            searchCursorPos = getSelStart();
            searchSelectionStart = searchCursorPos;
            searchSelectionEnd = searchCursorPos;
        }
        if (searchCursorPos < 0) searchCursorPos = 0;
        if (searchCursorPos > val.length()) searchCursorPos = val.length();
        return val.substring(0, searchCursorPos) + text + val.substring(searchCursorPos);
    }

    private void openModal(ModalType type, String contextId, String contextLabel) {
        activeModal = type;
        modalContextId = contextId;
        modalContextLabel = contextLabel;
        modalInput1 = "";
        modalInput2 = "";
        modalInput3 = "";
        modalInput4 = "";
        modalInput5 = "";
        modalInput6 = "";
        modalError = "";
        focusedModalField = switch (type) {
            case NEW_NETWORK, EDIT_NETWORK, CHANGE_PASSWORD -> "input1";
            case CONFIRM_DELETE -> "input1";
            case ASSIGN_ANTENNA_TO_NETWORK -> "input1";
            case SEND_ROAMING_PROPOSAL -> "input1";
            case EDIT_PLAN -> "input1";
            case CHANGE_PLAYER_NUMBER -> "input1";
            case CHANGE_PLAYER_PLAN -> "input1";
            default -> "input1";
        };
        searchCursorPos = 0;
        searchSelectionStart = 0;
        searchSelectionEnd = 0;
    }

    private void closeModal() {
        activeModal = ModalType.NONE;
        modalInput1 = "";
        modalInput2 = "";
        modalInput3 = "";
        modalInput4 = "";
        modalInput5 = "";
        modalInput6 = "";
        modalContextId = "";
        modalContextLabel = "";
        modalError = "";
        focusedModalField = "input1";
        searchCursorPos = 0;
        searchSelectionStart = 0;
        searchSelectionEnd = 0;
    }

    private void showModalError(String message) {
        modalError = message;
    }

    private String mask(String value) {
        return value.isEmpty() ? "" : "*".repeat(value.length());
    }

    @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (button != 0) return super.mouseClicked(mouseX, mouseY, button);

            // Modal clicks consume all input when modal is open
            if (activeModal != ModalType.NONE) {
                return handleModalClick(mouseX, mouseY, button);
            }

                // Calculate panel dimensions (same as in render method)
                int panelW = Math.min(AdminTheme.PANEL_PREFERRED_WIDTH, this.width - 20);
                int panelH = Math.min(AdminTheme.PANEL_PREFERRED_HEIGHT, this.height - 20);
                int panelX = (this.width - panelW) / 2;
                int panelY = (this.height - panelH) / 2;
                int sidebarTop = panelY + AdminTheme.HEADER_HEIGHT;
                int contentAreaLeft = panelX + AdminTheme.SIDEBAR_WIDTH;
                int contentAreaTop = panelY + AdminTheme.HEADER_HEIGHT;
                int contentAreaWidth = panelW - AdminTheme.SIDEBAR_WIDTH;
                int contentAreaHeight = panelH - AdminTheme.HEADER_HEIGHT - AdminTheme.FOOTER_HEIGHT;
                int footerY = panelY + panelH - AdminTheme.FOOTER_HEIGHT;
                int contentLeft = contentAreaLeft + AdminTheme.SPACE_L;
                int contentRight = panelX + panelW - AdminTheme.SPACE_L;
                int contentWidth = contentRight - contentLeft;
                int contentTop = contentAreaTop + AdminTheme.SPACE_L;
                int contentHeight = contentAreaHeight - AdminTheme.SPACE_L * 2;

                // Close button
                int closeBtnW = 60;
                int closeBtnH = AdminTheme.BUTTON_HEIGHT_SM;
                int closeBtnX = panelX + panelWidth - AdminTheme.SPACE_L - closeBtnW;
                int closeBtnY = panelY + (AdminTheme.HEADER_HEIGHT - closeBtnH) / 2;
                if (mouseX >= closeBtnX && mouseX < closeBtnX + closeBtnW &&
                        mouseY >= closeBtnY && mouseY < closeBtnY + closeBtnH) {
                    this.minecraft.setScreen(null);
                    return true;
                }

            // Sidebar tabs
            int itemY = sidebarTop + AdminTheme.SPACE_M;
            for (int i = 0; i < ServerTab.values().length; i++) {
                if (mouseX >= panelX && mouseX < panelX + AdminTheme.SIDEBAR_WIDTH &&
                        mouseY >= itemY && mouseY < itemY + AdminTheme.SIDEBAR_ITEM_HEIGHT) {
                    activeTab = ServerTab.values()[i];
                    scrollOffset = 0;
                    activeRoamingSubTab = RoamingSubTab.ACTIVE;
                    focusedField = "";
                        searchQuery = "";
                        searchCursorPos = 0;
                        searchFocused = false;
                        requestCurrentTabData();
                        return true;
                    }
                    itemY += AdminTheme.SIDEBAR_ITEM_HEIGHT + AdminTheme.SPACE_XXS;
                }

            // Roaming sub-tab clicks (check before search bar so sub-tabs aren't consumed)
            if (activeTab == ServerTab.ROAMING) {
                int subTabY = contentTop;
                int subTabW = 100;
                int subTabH = AdminTheme.BUTTON_HEIGHT_SM;
                int subTabX = contentLeft;
                RoamingSubTab[] subTabs = {RoamingSubTab.ACTIVE, RoamingSubTab.PROPOSED, RoamingSubTab.PROPOSE};
                for (int i = 0; i < subTabs.length; i++) {
                    if (mouseX >= subTabX && mouseX < subTabX + subTabW &&
                            mouseY >= subTabY && mouseY < subTabY + subTabH) {
                        activeRoamingSubTab = subTabs[i];
                        return true;
                    }
                    subTabX += subTabW + AdminTheme.SPACE_XS;
                }
            }

            // Antenna search bar focus
            if (activeTab == ServerTab.ANTENNAS) {
                if (mouseX >= contentLeft + AdminTheme.SPACE_L &&
                        mouseX < contentLeft + AdminTheme.SPACE_L + (contentWidth - AdminTheme.SPACE_L * 2) &&
                        mouseY >= contentTop &&
                        mouseY < contentTop + AdminTheme.SEARCH_BAR_HEIGHT) {
                    focusedField = "antennaSearch";
                    return true;
                }
            }

            // Roaming search bar focus
            if (activeTab == ServerTab.ROAMING && activeRoamingSubTab == RoamingSubTab.ACTIVE) {
                int rSearchBarY = contentTop + AdminTheme.BUTTON_HEIGHT_SM + AdminTheme.SPACE_M;
                int rSearchBarX = contentLeft + AdminTheme.SPACE_L;
                int rSearchBarW = contentWidth - AdminTheme.SPACE_L * 2;
                int rSearchBarH = AdminTheme.SEARCH_BAR_HEIGHT;
                if (mouseX >= rSearchBarX && mouseX < rSearchBarX + rSearchBarW &&
                        mouseY >= rSearchBarY && mouseY < rSearchBarY + rSearchBarH) {
                    focusedField = "roamingSearch";
                    return true;
                }
            }

            // Player search bar focus
            if (activeTab == ServerTab.PLAYERS) {
                if (mouseX >= contentLeft + AdminTheme.SPACE_L &&
                        mouseX < contentLeft + AdminTheme.SPACE_L + (contentWidth - AdminTheme.SPACE_L * 2) &&
                        mouseY >= contentTop &&
                        mouseY < contentTop + AdminTheme.SEARCH_BAR_HEIGHT) {
                    focusedField = "playerSearch";
                    return true;
                }
            }

            // Plan search bar focus
            if (activeTab == ServerTab.PLANS) {
                if (mouseX >= contentLeft + AdminTheme.SPACE_L &&
                        mouseX < contentLeft + AdminTheme.SPACE_L + (contentWidth - AdminTheme.SPACE_L * 2) &&
                        mouseY >= contentTop &&
                        mouseY < contentTop + AdminTheme.SEARCH_BAR_HEIGHT) {
                    focusedField = "planSearch";
                    return true;
                }
            }

            // Billing player field focus
            if (activeTab == ServerTab.BILLING) {
                int bFieldY = contentTop + AdminTheme.TEXT_CAPTION + AdminTheme.SPACE_M;
                int bLabelW = 120;
                int bFieldW = contentWidth - AdminTheme.SPACE_L * 2 - bLabelW;
                int bFieldH = AdminTheme.TEXT_BODY + 8;
                if (mouseX >= contentLeft + bLabelW && mouseX < contentLeft + bLabelW + bFieldW &&
                        mouseY >= bFieldY - 4 && mouseY < bFieldY - 4 + bFieldH) {
                    focusedField = "billingPlayer";
                    return true;
                }
            }

            // Search bar focus
                            int newBtnW = 120;
                            int newBtnH = AdminTheme.BUTTON_HEIGHT_SM;
                            int searchBarX = contentLeft + AdminTheme.SPACE_L;
                            int searchBarY = contentTop;
                            int searchBarW = contentWidth - AdminTheme.SPACE_L * 2 - 120;
                            int searchBarH = AdminTheme.SEARCH_BAR_HEIGHT;
                            boolean searchHit = mouseX >= contentLeft + AdminTheme.SPACE_L && mouseX < contentLeft + AdminTheme.SPACE_L + (contentWidth - AdminTheme.SPACE_L * 2 - 120) &&
                                    mouseY >= contentTop && mouseY < contentTop + AdminTheme.SEARCH_BAR_HEIGHT;
                            if (searchHit) {
                                searchFocused = true;
                                searchCursorPos = searchQuery.length();
                                return true;
                            } else {
                                searchFocused = false;
                            }

                                // New Network button -> open modal
                                if (activeTab == ServerTab.NETWORKS) {
                                        int newBtnX = contentLeft + contentWidth - AdminTheme.SPACE_L - 120;
                                        int newBtnY = contentTop + (AdminTheme.SEARCH_BAR_HEIGHT - AdminTheme.BUTTON_HEIGHT_SM) / 2;
                                        if (mouseX >= newBtnX && mouseX < newBtnX + 120 &&
                                                mouseY >= newBtnY && mouseY < newBtnY + AdminTheme.BUTTON_HEIGHT_SM) {
                                            openModal(ModalType.NEW_NETWORK, "", "");
                                            return true;
                                        }
                                }

                // Refresh button (in footer)
                int refreshBtnW = 80;
                int refreshBtnX = panelX + panelWidth - AdminTheme.SPACE_L - refreshBtnW;
                int refreshBtnY = footerY + (AdminTheme.FOOTER_HEIGHT - AdminTheme.BUTTON_HEIGHT_SM) / 2;
                if (mouseX >= refreshBtnX && mouseX < refreshBtnX + refreshBtnW &&
                        mouseY >= refreshBtnY && mouseY < refreshBtnY + AdminTheme.BUTTON_HEIGHT_SM) {
                    requestCurrentTabData();
                    AdminToast.info("Refresh requested");
                    return true;
                }

                // Undo button
                if (AdminUndoManager.canUndo()) {
                    int bannerY = panelY + panelHeight - AdminTheme.FOOTER_HEIGHT - AdminTheme.SPACE_L - 30;
                    int bannerX = panelX + AdminTheme.SPACE_L;
                    int bannerW = panelWidth - AdminTheme.SPACE_L * 2;
                    int undoBtnW = 60;
                    int undoBtnH = 22;
                    int undoBtnX = bannerX + bannerW - AdminTheme.SPACE_M - undoBtnW;
                    int undoBtnY = bannerY + 4;
                    if (mouseX >= undoBtnX && mouseX < undoBtnX + undoBtnW &&
                            mouseY >= undoBtnY && mouseY < undoBtnY + undoBtnH) {
                        AdminUndoManager.undo();
                        return true;
                    }
                }

                // Network card buttons (placeholder)
            // Would check card button hits here

            // Antenna tab interactions
            if (activeTab == ServerTab.ANTENNAS) {
                int antennaCount = getFilteredAntennaCount();
                int cardY = listTop + AdminTheme.SPACE_L + AdminTheme.SPACE_S - scrollOffset;
                for (int i = 0; i < antennaCount; i++) {
                    int rowY2 = cardY;
                    if (mouseX >= contentLeft && mouseX < contentLeft + contentWidth &&
                            mouseY >= rowY2 && mouseY < rowY2 + AdminTheme.ROW_HEIGHT_LARGE) {
                        // Checkbox click
                        int cbSize = 18;
                        int cbX = contentLeft + AdminTheme.SPACE_S;
                        int cbY = rowY2 + (AdminTheme.ROW_HEIGHT_LARGE - cbSize) / 2;
                        if (mouseX >= cbX && mouseX < cbX + cbSize &&
                                mouseY >= cbY && mouseY < cbY + cbSize) {
                            if (selectedAntennaIndices.contains(i)) {
                                selectedAntennaIndices.remove(i);
                            } else {
                                selectedAntennaIndices.add(i);
                            }
                            return true;
                        }
                        // Right-aligned button clicks
                        int btnW = 70;
                        int btnH = AdminTheme.BUTTON_HEIGHT_SM;
                        int btnX2 = contentLeft + contentWidth - AdminTheme.SPACE_L - btnW;
                        int btnY2 = rowY2 + (AdminTheme.ROW_HEIGHT_LARGE - btnH) / 2;
                        if (mouseX >= btnX2 && mouseX < btnX2 + btnW &&
                                mouseY >= btnY2 && mouseY < btnY2 + btnH) {
                            AdminToast.success("Claim request sent");
                            return true;
                        }
                        // Row click toggles checkbox
                        if (selectedAntennaIndices.contains(i)) {
                            selectedAntennaIndices.remove(i);
                        } else {
                            selectedAntennaIndices.add(i);
                        }
                        return true;
                    }
                    cardY += AdminTheme.ROW_HEIGHT_LARGE + AdminTheme.SPACE_XS;
                }
            }

            // Roaming Active: Revoke button
            if (activeTab == ServerTab.ROAMING && activeRoamingSubTab == RoamingSubTab.ACTIVE) {
                int agreementCount = getActiveAgreementCount();
                int headerY = contentTop + AdminTheme.BUTTON_HEIGHT_SM + AdminTheme.SPACE_M +
                        AdminTheme.SEARCH_BAR_HEIGHT + AdminTheme.SPACE_S;
                int cardY = headerY + AdminTheme.TEXT_CAPTION + AdminTheme.SPACE_S - scrollOffset;
                for (int i = 0; i < agreementCount; i++) {
                    int btnW = 70;
                    int btnH = AdminTheme.BUTTON_HEIGHT_SM;
                    int btnX = contentLeft + contentWidth - AdminTheme.SPACE_L - btnW;
                    int btnY = cardY + (AdminTheme.ROW_HEIGHT_LARGE - btnH) / 2;
                    if (mouseX >= btnX && mouseX < btnX + btnW &&
                            mouseY >= btnY && mouseY < btnY + btnH) {
                        AdminToast.info("Revoked");
                        return true;
                    }
                    cardY += AdminTheme.ROW_HEIGHT_LARGE + AdminTheme.SPACE_XS;
                }
            }

            // Roaming Proposed: Cancel/Accept/Counter/Decline buttons
            if (activeTab == ServerTab.ROAMING && activeRoamingSubTab == RoamingSubTab.PROPOSED) {
                int sectionY = contentTop + AdminTheme.BUTTON_HEIGHT_SM + AdminTheme.SPACE_M;
                int cardY = sectionY + AdminTheme.TEXT_CAPTION + AdminTheme.SPACE_S - scrollOffset;
                int outgoingCount = getOutgoingProposalCount();
                for (int i = 0; i < outgoingCount; i++) {
                    int btnW = 70;
                    int btnH = AdminTheme.BUTTON_HEIGHT_SM;
                    int btnX = contentLeft + contentWidth - AdminTheme.SPACE_L - btnW;
                    int btnY = cardY + (AdminTheme.ROW_HEIGHT_LARGE - btnH) / 2;
                    if (mouseX >= btnX && mouseX < btnX + btnW &&
                            mouseY >= btnY && mouseY < btnY + btnH) {
                        AdminToast.info("Cancelled");
                        return true;
                    }
                    cardY += AdminTheme.ROW_HEIGHT_LARGE + AdminTheme.SPACE_XS;
                }
                // Incoming proposals
                sectionY = cardY + AdminTheme.SPACE_M;
                cardY = sectionY + AdminTheme.TEXT_CAPTION + AdminTheme.SPACE_S - scrollOffset;
                int incomingCount = getIncomingProposalCount();
                int btnX3 = contentLeft + contentWidth - AdminTheme.SPACE_L - 70 * 3 - AdminTheme.SPACE_XS * 2;
                for (int i = 0; i < incomingCount; i++) {
                    int btnY3 = cardY + (AdminTheme.ROW_HEIGHT_LARGE - AdminTheme.BUTTON_HEIGHT_SM) / 2;
                    if (mouseX >= btnX3 && mouseX < btnX3 + 70 &&
                            mouseY >= btnY3 && mouseY < btnY3 + AdminTheme.BUTTON_HEIGHT_SM) {
                        AdminToast.info("Accepted");
                        return true;
                    }
                    btnX3 += 70 + AdminTheme.SPACE_XS;
                    if (mouseX >= btnX3 && mouseX < btnX3 + 70 &&
                            mouseY >= btnY3 && mouseY < btnY3 + AdminTheme.BUTTON_HEIGHT_SM) {
                        AdminToast.info("Countered");
                        return true;
                    }
                    btnX3 += 70 + AdminTheme.SPACE_XS;
                    if (mouseX >= btnX3 && mouseX < btnX3 + 70 &&
                            mouseY >= btnY3 && mouseY < btnY3 + AdminTheme.BUTTON_HEIGHT_SM) {
                        AdminToast.info("Declined");
                        return true;
                    }
                    cardY += AdminTheme.ROW_HEIGHT_LARGE + AdminTheme.SPACE_XS;
                }
            }

            // Roaming Propose: Send Proposal button and text field focus
            if (activeTab == ServerTab.ROAMING && activeRoamingSubTab == RoamingSubTab.PROPOSE) {
                int labelW = 140;
                int fieldH = AdminTheme.TEXT_BODY + 8;
                int rowSpacing = AdminTheme.SPACE_L;

                // Compact mode if form exceeds content area height
                int formHeight = AdminTheme.TEXT_CAPTION + AdminTheme.SPACE_M
                        + 5 * (fieldH + rowSpacing)
                        + AdminTheme.BUTTON_HEIGHT_SM + AdminTheme.SPACE_L;
                // startY = contentTop + BUTTON_HEIGHT_SM + SPACE_M (sub-tabs + margin)
                boolean compact = (contentTop + AdminTheme.BUTTON_HEIGHT_SM + AdminTheme.SPACE_M + formHeight > contentTop + contentHeight);
                if (compact) {
                    fieldH = AdminTheme.BUTTON_HEIGHT_SM;
                    rowSpacing = AdminTheme.SPACE_M;
                }

                int fieldW = contentWidth - AdminTheme.SPACE_L * 2 - labelW;
                int fieldY = contentTop + AdminTheme.BUTTON_HEIGHT_SM + AdminTheme.SPACE_M
                        + AdminTheme.TEXT_CAPTION + AdminTheme.SPACE_M;

                // Target network field
                if (mouseX >= contentLeft + labelW && mouseX < contentLeft + labelW + fieldW &&
                        mouseY >= fieldY - 4 && mouseY < fieldY - 4 + fieldH) {
                    focusedField = "proposeTarget";
                    return true;
                }
                fieldY += fieldH + rowSpacing;

                // Rate per minute field
                if (mouseX >= contentLeft + labelW && mouseX < contentLeft + labelW + fieldW &&
                        mouseY >= fieldY - 4 && mouseY < fieldY - 4 + fieldH) {
                    focusedField = "proposeRateMinute";
                    return true;
                }
                fieldY += fieldH + rowSpacing;

                // Rate per text field
                if (mouseX >= contentLeft + labelW && mouseX < contentLeft + labelW + fieldW &&
                        mouseY >= fieldY - 4 && mouseY < fieldY - 4 + fieldH) {
                    focusedField = "proposeRateText";
                    return true;
                }
                fieldY += fieldH + rowSpacing;

                // Rate per MB field
                if (mouseX >= contentLeft + labelW && mouseX < contentLeft + labelW + fieldW &&
                        mouseY >= fieldY - 4 && mouseY < fieldY - 4 + fieldH) {
                    focusedField = "proposeRateMb";
                    return true;
                }
                fieldY += fieldH + rowSpacing;

                // Optional message field
                if (mouseX >= contentLeft + labelW && mouseX < contentLeft + labelW + fieldW &&
                        mouseY >= fieldY - 4 && mouseY < fieldY - 4 + fieldH) {
                    focusedField = "proposeMessage";
                    return true;
                }
                fieldY += fieldH + rowSpacing;

                // Send Proposal button at bottom of content area
                int btnW = 140;
                int btnH = AdminTheme.BUTTON_HEIGHT_SM;
                int btnX = contentLeft + (contentWidth - btnW) / 2;
                int btnY = contentTop + contentHeight - AdminTheme.SPACE_L - btnH;
                if (mouseX >= btnX && mouseX < btnX + btnW &&
                        mouseY >= btnY && mouseY < btnY + btnH) {
                    AdminToast.info("Proposal sent (packet in Batch 5)");
                    return true;
                }
            }

            // Players tab: row buttons (Plan/Number/Balance)
            if (activeTab == ServerTab.PLAYERS) {
                int playerCount = getPlayerCount();
                int cardY = listTop + AdminTheme.SPACE_L + AdminTheme.SPACE_S - scrollOffset;
                for (int i = 0; i < playerCount; i++) {
                    int rowY2 = cardY;
                    if (mouseX >= contentLeft && mouseX < contentLeft + contentWidth &&
                            mouseY >= rowY2 && mouseY < rowY2 + AdminTheme.ROW_HEIGHT_LARGE) {
                        int btnW = 70;
                        int btnH = AdminTheme.BUTTON_HEIGHT_SM;
                        int btnX = contentLeft + contentWidth - AdminTheme.SPACE_L - btnW * 3 - AdminTheme.SPACE_XS * 2;
                        int btnY = rowY2 + (AdminTheme.ROW_HEIGHT_LARGE - btnH) / 2;
                        if (mouseX >= btnX && mouseX < btnX + btnW &&
                                mouseY >= btnY && mouseY < btnY + btnH) {
                            openModal(ModalType.CHANGE_PLAYER_PLAN, "", "");
                            return true;
                        }
                        btnX += btnW + AdminTheme.SPACE_XS;
                        if (mouseX >= btnX && mouseX < btnX + btnW &&
                                mouseY >= btnY && mouseY < btnY + btnH) {
                            openModal(ModalType.CHANGE_PLAYER_NUMBER, "", "");
                            return true;
                        }
                        btnX += btnW + AdminTheme.SPACE_XS;
                        if (mouseX >= btnX && mouseX < btnX + btnW &&
                                mouseY >= btnY && mouseY < btnY + btnH) {
                            AdminToast.info("Balance modal coming in Batch 5");
                            return true;
                        }
                    }
                    cardY += AdminTheme.ROW_HEIGHT_LARGE + AdminTheme.SPACE_XS;
                }
            }

            // Plans tab: row buttons (Edit/Delete)
            if (activeTab == ServerTab.PLANS) {
                int planCount = getPlanCount();
                int cardY = listTop + AdminTheme.SPACE_L + AdminTheme.SPACE_S - scrollOffset;
                for (int i = 0; i < planCount; i++) {
                    int rowY2 = cardY;
                    if (mouseX >= contentLeft && mouseX < contentLeft + contentWidth &&
                            mouseY >= rowY2 && mouseY < rowY2 + AdminTheme.ROW_HEIGHT_LARGE) {
                        int btnW = 70;
                        int btnH = AdminTheme.BUTTON_HEIGHT_SM;
                        int btnX = contentLeft + contentWidth - AdminTheme.SPACE_L - btnW * 2 - AdminTheme.SPACE_XS;
                        int btnY = rowY2 + (AdminTheme.ROW_HEIGHT_LARGE - btnH) / 2;
                        if (mouseX >= btnX && mouseX < btnX + btnW &&
                                mouseY >= btnY && mouseY < btnY + btnH) {
                            openModal(ModalType.EDIT_PLAN, "", "");
                            return true;
                        }
                        btnX += btnW + AdminTheme.SPACE_XS;
                        if (mouseX >= btnX && mouseX < btnX + btnW &&
                                mouseY >= btnY && mouseY < btnY + btnH) {
                            openModal(ModalType.CONFIRM_DELETE, "", "");
                            return true;
                        }
                    }
                    cardY += AdminTheme.ROW_HEIGHT_LARGE + AdminTheme.SPACE_XS;
                }
            }

            // Billing tab: Force Renewal / View History buttons
            if (activeTab == ServerTab.BILLING) {
                int fieldY = contentTop + AdminTheme.TEXT_CAPTION + AdminTheme.SPACE_M;
                fieldY += AdminTheme.TEXT_BODY + AdminTheme.SPACE_S + AdminTheme.TEXT_BODY + 8 + AdminTheme.SPACE_S;
                int btnW = 120;
                int btnH = AdminTheme.BUTTON_HEIGHT_SM;
                int btnX = contentLeft;
                int btnY = fieldY;
                if (mouseX >= btnX && mouseX < btnX + btnW &&
                        mouseY >= btnY && mouseY < btnY + btnH) {
                    AdminToast.info("Force renewal requested");
                    return true;
                }
                btnX += btnW + AdminTheme.SPACE_XS;
                if (mouseX >= btnX && mouseX < btnX + btnW &&
                        mouseY >= btnY && mouseY < btnY + btnH) {
                    AdminToast.info("View history requested");
                    return true;
                }
            }

            return super.mouseClicked(mouseX, mouseY, button);
        }

    private boolean handleModalClick(double mouseX, double mouseY, int button) {
        int panelW = Math.min(AdminTheme.PANEL_PREFERRED_WIDTH, this.width - 20);
        int panelH = Math.min(AdminTheme.PANEL_PREFERRED_HEIGHT, this.height - 20);
        int panelX = (this.width - panelW) / 2;
        int panelY = (this.height - panelH) / 2;

        int modalW = Math.min(360, panelW - 40);
        int modalX = panelX + (panelW - modalW) / 2;
        int modalY = panelY + (panelH - 200) / 2;
        int modalH = switch (activeModal) {
            case NEW_NETWORK, CHANGE_PASSWORD -> 240;
            case EDIT_NETWORK -> 220;
            case CONFIRM_DELETE -> 200;
            case ASSIGN_ANTENNA_TO_NETWORK -> 240;
            case SEND_ROAMING_PROPOSAL -> 260;
            case EDIT_PLAN -> 420;
            case CHANGE_PLAYER_NUMBER -> 200;
            case CHANGE_PLAYER_PLAN -> 280;
            default -> 200;
        };

        // Click outside modal panel: consume but don't close
        if (mouseX < modalX || mouseX >= modalX + modalW || mouseY < modalY || mouseY >= modalY + modalH) {
            return true;
        }

        int btnW = 100;
        int btnH = AdminTheme.BUTTON_HEIGHT_SM;
        int btnY = modalY + modalH - AdminTheme.SPACE_L - btnH;
        int btnX1 = modalX + AdminTheme.SPACE_L;
        int btnX2 = modalX + modalW - AdminTheme.SPACE_L - btnW;

        boolean cancelHovered = mouseX >= btnX1 && mouseX < btnX1 + btnW && mouseY >= btnY && mouseY < btnY + btnH;
        boolean confirmHovered = mouseX >= btnX2 && mouseX < btnX2 + btnW && mouseY >= btnY && mouseY < btnY + btnH;

        if (cancelHovered) {
            closeModal();
            return true;
        }

        if (confirmHovered) {
            handleModalConfirm();
            return true;
        }

        // Field focus - check each field area based on active modal
        int fieldY = modalY + AdminTheme.SPACE_L + AdminTheme.TEXT_TITLE + AdminTheme.TEXT_CAPTION + AdminTheme.SPACE_M;
        int labelW = 180;
        int fieldW = modalW - AdminTheme.SPACE_L * 2 - labelW;
        int fieldH = AdminTheme.TEXT_BODY + 8;

        switch (activeModal) {
            case NEW_NETWORK -> {
                if (mouseX >= modalX + labelW && mouseX < modalX + labelW + fieldW
                        && mouseY >= fieldY - 4 && mouseY < fieldY - 4 + fieldH) {
                    focusedModalField = "input1";
                    return true;
                }
                fieldY += fieldH + AdminTheme.SPACE_L;
                if (mouseX >= modalX + labelW && mouseX < modalX + labelW + fieldW
                        && mouseY >= fieldY - 4 && mouseY < fieldY - 4 + fieldH) {
                    focusedModalField = "input2";
                    return true;
                }
            }
            case EDIT_NETWORK -> {
                if (mouseX >= modalX + labelW && mouseX < modalX + labelW + fieldW
                        && mouseY >= fieldY - 4 && mouseY < fieldY - 4 + fieldH) {
                    focusedModalField = "input1";
                    return true;
                }
                fieldY += fieldH + AdminTheme.SPACE_L;
                if (mouseX >= modalX + labelW && mouseX < modalX + labelW + fieldW
                        && mouseY >= fieldY - 4 && mouseY < fieldY - 4 + fieldH) {
                    focusedModalField = "input2";
                    return true;
                }
            }
            case CHANGE_PASSWORD -> {
                if (mouseX >= modalX + labelW && mouseX < modalX + labelW + fieldW
                        && mouseY >= fieldY - 4 && mouseY < fieldY - 4 + fieldH) {
                    focusedModalField = "input1";
                    return true;
                }
                fieldY += fieldH + AdminTheme.SPACE_L;
                if (mouseX >= modalX + labelW && mouseX < modalX + labelW + fieldW
                        && mouseY >= fieldY - 4 && mouseY < fieldY - 4 + fieldH) {
                    focusedModalField = "input2";
                    return true;
                }
            }
            case CONFIRM_DELETE -> {
                if (mouseX >= modalX + labelW && mouseX < modalX + labelW + fieldW
                        && mouseY >= fieldY - 4 && mouseY < fieldY - 4 + fieldH) {
                    focusedModalField = "input1";
                    return true;
                }
            }
            case ASSIGN_ANTENNA_TO_NETWORK -> {
                // No input fields in this modal (placeholder list only)
            }
            case SEND_ROAMING_PROPOSAL -> {
                int propLabelW = 160;
                int propFieldW = modalW - AdminTheme.SPACE_L * 2 - propLabelW;
                int propFieldH = AdminTheme.TEXT_BODY + 8;
                String[] propFields = {"input1", "input2", "input3", "input4", "input5"};
                for (int i = 0; i < propFields.length; i++) {
                    int fY = fieldY + i * (propFieldH + AdminTheme.SPACE_M);
                    if (mouseX >= modalX + propLabelW && mouseX < modalX + propLabelW + propFieldW
                            && mouseY >= fY - 4 && mouseY < fY - 4 + propFieldH) {
                        focusedModalField = propFields[i];
                        return true;
                    }
                }
            }
            case EDIT_PLAN -> {
                int planFieldStartY = modalY + 60;
                int planRowHeight = 32;
                int planLabelW = 160;
                int planFieldW = modalW - AdminTheme.SPACE_L * 2 - planLabelW;
                String[] planFields = {"input1", "input2", "input3", "input4", "input5", "input6"};
                for (int i = 0; i < planFields.length; i++) {
                    int fY = planFieldStartY + i * planRowHeight - 4;
                    if (mouseX >= modalX + planLabelW && mouseX < modalX + planLabelW + planFieldW
                            && mouseY >= fY && mouseY < fY + planRowHeight) {
                        focusedModalField = planFields[i];
                        return true;
                    }
                }
            }
            case CHANGE_PLAYER_NUMBER -> {
                if (mouseX >= modalX + labelW && mouseX < modalX + labelW + fieldW
                        && mouseY >= fieldY - 4 && mouseY < fieldY - 4 + fieldH) {
                    focusedModalField = "input1";
                    return true;
                }
            }
            case CHANGE_PLAYER_PLAN -> {
                // No input fields in this modal (placeholder list only)
            }
            default -> {
            }
        }

        return true;
    }

    private void handleModalConfirm() {
        switch (activeModal) {
            case NEW_NETWORK -> handleNewNetworkConfirm();
            case EDIT_NETWORK -> handleEditNetworkConfirm();
            case CHANGE_PASSWORD -> handleChangePasswordConfirm();
            case CONFIRM_DELETE -> handleConfirmDeleteConfirm();
            case ASSIGN_ANTENNA_TO_NETWORK -> handleAssignAntennaConfirm();
            case SEND_ROAMING_PROPOSAL -> handleSendRoamingProposalConfirm();
            case EDIT_PLAN -> handleEditPlanConfirm();
            case CHANGE_PLAYER_NUMBER -> handleChangePlayerNumberConfirm();
            case CHANGE_PLAYER_PLAN -> handleChangePlayerPlanConfirm();
            default -> {
            }
        }
    }

    private void handleNewNetworkConfirm() {
        String name = modalInput1.trim();
        String password = modalInput2.trim();
        if (name.isEmpty()) {
            showModalError("Network name is required.");
            return;
        }
        int minimumPasswordLength =
                com.example.mobilecellnetwork.config.MobileCellNetworkConfig.MINIMUM_NETWORK_PASSWORD_LENGTH.get();
        if (password.length() < minimumPasswordLength) {
            showModalError("Password must be at least " + minimumPasswordLength + " characters.");
            return;
        }
        PacketDistributor.sendToServer(new com.example.mobilecellnetwork.network.packet.CreateNetworkPacket(name, password));
        closeModal();
    }

    private void handleEditNetworkConfirm() {
        String name = modalInput1.trim();
        if (name.isEmpty()) {
            showModalError("Name is required.");
            return;
        }
        PacketDistributor.sendToServer(new com.example.mobilecellnetwork.network.packet.UpdateNetworkPacket(modalContextId, name, false));
        AdminToast.success("Network updated");
        closeModal();
    }

    private void handleChangePasswordConfirm() {
        String p1 = modalInput1.trim();
        String p2 = modalInput2.trim();
        if (p1.length() < 4) {
            showModalError("Password must be at least 4 characters.");
            return;
        }
        if (!p1.equals(p2)) {
            showModalError("Passwords do not match.");
            return;
        }
        PacketDistributor.sendToServer(new com.example.mobilecellnetwork.network.packet.ChangePasswordPacket(modalContextId, p1));
        AdminToast.success("Password changed");
        closeModal();
    }

    private void handleConfirmDeleteConfirm() {
        String confirm = modalInput1.trim();
        if (!confirm.equals("DELETE")) {
            showModalError("Type DELETE exactly.");
            return;
        }
        PacketDistributor.sendToServer(new com.example.mobilecellnetwork.network.packet.DeleteEntityPacket(modalContextId, modalContextLabel));
        AdminUndoManager.push("Deleted " + modalContextLabel, () -> {});
        AdminToast.success(modalContextLabel + " deleted");
        closeModal();
    }

    private void handleAssignAntennaConfirm() {
        if (modalContextId == null || modalContextId.isEmpty()) {
            showModalError("Antenna position is required.");
            return;
        }
        showModalError("Antenna registry not yet loaded.");
    }

    private void handleSendRoamingProposalConfirm() {
        String target = modalInput1.trim();
        if (target.isEmpty()) {
            showModalError("Target network is required.");
            return;
        }
        try {
            int rateMin = Integer.parseInt(modalInput2.trim());
            int rateText = Integer.parseInt(modalInput3.trim());
            int rateMb = Integer.parseInt(modalInput4.trim());
            if (rateMin < 0 || rateText < 0 || rateMb < 0) {
                showModalError("Rates must be non-negative numbers.");
                return;
            }
        } catch (NumberFormatException e) {
            showModalError("Rates must be non-negative numbers.");
            return;
        }
        AdminToast.success("Proposal sent");
        closeModal();
    }

    private void handleEditPlanConfirm() {
        String name = modalInput1.trim();
        if (name.isEmpty()) {
            showModalError("Plan name is required.");
            return;
        }
        try {
            int price = Integer.parseInt(modalInput2.trim());
            int cycle = Integer.parseInt(modalInput3.trim());
            if (price < 0 || cycle < 0) {
                showModalError("Check numeric fields.");
                return;
            }
        } catch (NumberFormatException e) {
            showModalError("Check numeric fields.");
            return;
        }
        try {
            int callMin = Integer.parseInt(modalInput4.trim());
            int texts = Integer.parseInt(modalInput5.trim());
            int dataMb = Integer.parseInt(modalInput6.trim());
        } catch (NumberFormatException e) {
            showModalError("Check numeric fields.");
            return;
        }
        AdminToast.success("Plan updated");
        closeModal();
    }

    private void handleChangePlayerNumberConfirm() {
        String number = modalInput1.trim();
        if (number.isEmpty() || number.length() > 9 || !number.matches("\\d+")) {
            showModalError("Number must be 1-9 digits.");
            return;
        }
        AdminToast.success("Number updated");
        closeModal();
    }

    private void handleChangePlayerPlanConfirm() {
        String planId = modalInput1.trim();
        if (planId.isEmpty()) {
            showModalError("Plan registry not yet loaded.");
            return;
        }
        showModalError("Plan registry not yet loaded.");
    }

    private void cycleModalFocus() {
        String[] fields = switch (activeModal) {
            case NEW_NETWORK, EDIT_NETWORK, CHANGE_PASSWORD -> new String[]{"input1", "input2"};
            case CONFIRM_DELETE -> new String[]{"input1"};
            case ASSIGN_ANTENNA_TO_NETWORK -> new String[]{"input1"};
            case SEND_ROAMING_PROPOSAL -> new String[]{"input1", "input2", "input3", "input4", "input5"};
            case EDIT_PLAN -> new String[]{"input1", "input2", "input3", "input4", "input5", "input6"};
            case CHANGE_PLAYER_NUMBER -> new String[]{"input1"};
            case CHANGE_PLAYER_PLAN -> new String[]{"input1"};
            default -> new String[]{};
        };
        if (fields.length == 0) return;
        int idx = 0;
        for (int i = 0; i < fields.length; i++) {
            if (fields[i].equals(focusedModalField)) {
                idx = i;
                break;
            }
        }
        focusedModalField = fields[(idx + 1) % fields.length];
    }

    @Override
    public boolean keyPressed(int key, int scanCode, int modifiers) {
        boolean shift = (modifiers & 0x1) != 0;

        // Modal key handling
        if (activeModal != ModalType.NONE) {
            if (key == 256) { // Escape
                closeModal();
                return true;
            }
            if (key == 257) { // Enter
                handleModalConfirm();
                return true;
            }
            if (key == 258) { // Tab
                cycleModalFocus();
                return true;
            }
            if (key == 259) { // Backspace
                String val = getModalFieldValue();
                if (hasSelection()) {
                    setModalFieldValue(focusedModalField, deleteSelectedText(val));
                    searchCursorPos = getSelStart();
                    searchSelectionStart = searchCursorPos;
                    searchSelectionEnd = searchCursorPos;
                } else if (searchCursorPos > 0) {
                    setModalFieldValue(focusedModalField,
                            val.substring(0, searchCursorPos - 1) +
                                    val.substring(searchCursorPos));
                    searchCursorPos--;
                }
                return true;
            }
            if (key == 261) { // Delete
                String val = getModalFieldValue();
                if (hasSelection()) {
                    setModalFieldValue(focusedModalField, deleteSelectedText(val));
                    searchCursorPos = getSelStart();
                    searchSelectionStart = searchCursorPos;
                    searchSelectionEnd = searchCursorPos;
                } else if (searchCursorPos < val.length()) {
                    setModalFieldValue(focusedModalField,
                            val.substring(0, searchCursorPos) +
                                    val.substring(searchCursorPos + 1));
                }
                return true;
            }
            if (key == 263) { // Left arrow
                if (shift) {
                    // Extend selection left
                    if (searchSelectionStart == searchSelectionEnd) {
                        searchSelectionStart = searchCursorPos;
                        searchSelectionEnd = searchCursorPos;
                    }
                    searchSelectionStart = Math.max(0, searchSelectionStart - 1);
                    searchCursorPos = searchSelectionStart;
                } else {
                    searchCursorPos = Math.max(0, searchCursorPos - 1);
                    searchSelectionStart = searchCursorPos;
                    searchSelectionEnd = searchCursorPos;
                }
                return true;
            }
            if (key == 262) { // Right arrow
                if (shift) {
                    // Extend selection right
                    if (searchSelectionStart == searchSelectionEnd) {
                        searchSelectionStart = searchCursorPos;
                        searchSelectionEnd = searchCursorPos;
                    }
                    searchSelectionEnd = Math.min(getModalFieldValue().length(), searchSelectionEnd + 1);
                    searchCursorPos = searchSelectionEnd;
                } else {
                    searchCursorPos = Math.min(getModalFieldValue().length(), searchCursorPos + 1);
                    searchSelectionStart = searchCursorPos;
                    searchSelectionEnd = searchCursorPos;
                }
                return true;
            }
            if (key == 268) { // Home
                if (shift) {
                    searchSelectionStart = 0;
                    searchCursorPos = 0;
                } else {
                    searchCursorPos = 0;
                    searchSelectionStart = 0;
                    searchSelectionEnd = 0;
                }
                return true;
            }
            if (key == 269) { // End
                String val = getModalFieldValue();
                if (shift) {
                    searchSelectionEnd = val.length();
                    searchCursorPos = val.length();
                } else {
                    searchCursorPos = val.length();
                    searchSelectionStart = val.length();
                    searchSelectionEnd = val.length();
                }
                return true;
            }
            if (key == 65 && (modifiers & 0x2) != 0) { // Ctrl+A - Select all
                String val = getModalFieldValue();
                searchSelectionStart = 0;
                searchSelectionEnd = val.length();
                searchCursorPos = val.length();
                return true;
            }
            if (key == 67 && (modifiers & 0x2) != 0) { // Ctrl+C - Copy
                if (hasSelection()) {
                    String val = getModalFieldValue();
                    this.minecraft.keyboardHandler.setClipboard(val.substring(getSelStart(), getSelEnd()));
                }
                return true;
            }
            if (key == 86 && (modifiers & 0x2) != 0) { // Ctrl+V - Paste
                String val = getModalFieldValue();
                String clip = this.minecraft.keyboardHandler.getClipboard();
                if (clip != null && !clip.isEmpty()) {
                    if (hasSelection()) {
                        val = deleteSelectedText(val);
                        searchCursorPos = getSelStart();
                        searchSelectionStart = searchCursorPos;
                        searchSelectionEnd = searchCursorPos;
                    }
                    if (searchCursorPos < 0) searchCursorPos = 0;
                    if (searchCursorPos > val.length()) searchCursorPos = val.length();
                    StringBuilder filtered = new StringBuilder();
                    for (int i = 0; i < clip.length(); i++) {
                        char c = clip.charAt(i);
                        if (focusedModalField.equals("proposeRateMinute")
                                || focusedModalField.equals("proposeRateText")
                                || focusedModalField.equals("proposeRateMb")) {
                            if (Character.isDigit(c)) filtered.append(c);
                        } else if (focusedModalField.equals("proposeTarget")) {
                            if (Character.isDigit(c) || c == '-') filtered.append(c);
                        } else if (activeModal == ModalType.EDIT_PLAN
                                && (focusedModalField.equals("input2")
                                || focusedModalField.equals("input3"))) {
                            if (Character.isDigit(c)) filtered.append(c);
                        } else if (activeModal == ModalType.EDIT_PLAN
                                && (focusedModalField.equals("input4")
                                || focusedModalField.equals("input5")
                                || focusedModalField.equals("input6"))) {
                            if (Character.isDigit(c) || c == '-') filtered.append(c);
                        } else if (activeModal == ModalType.CHANGE_PLAYER_NUMBER
                                && focusedModalField.equals("input1")) {
                            if (Character.isDigit(c)) filtered.append(c);
                        } else {
                            filtered.append(c);
                        }
                    }
                    clip = filtered.toString();
                    val = val.substring(0, searchCursorPos) + clip + val.substring(searchCursorPos);
                    searchCursorPos += clip.length();
                    setModalFieldValue(focusedModalField, val);
                }
                return true;
            }
            return true; // Consume all keys when modal is open
        }

        // Handle focused field input (antenna search, roaming search, propose fields)
        if (focusedField != null && !focusedField.isEmpty()) {
            String val = getFocusedFieldValue();
            if (searchCursorPos < 0) searchCursorPos = 0;
            if (searchCursorPos > val.length()) searchCursorPos = val.length();
            if (key == 259) { // Backspace
                if (hasSelection()) {
                    setFocusedFieldValue(deleteSelectedText(val));
                    searchCursorPos = getSelStart();
                    searchSelectionStart = searchCursorPos;
                    searchSelectionEnd = searchCursorPos;
                } else if (searchCursorPos > 0) {
                    setFocusedFieldValue(val.substring(0, searchCursorPos - 1) +
                            val.substring(searchCursorPos));
                    searchCursorPos--;
                }
                return true;
            } else if (key == 261) { // Delete
                if (hasSelection()) {
                    setFocusedFieldValue(deleteSelectedText(val));
                    searchCursorPos = getSelStart();
                    searchSelectionStart = searchCursorPos;
                    searchSelectionEnd = searchCursorPos;
                } else if (searchCursorPos < val.length()) {
                    setFocusedFieldValue(val.substring(0, searchCursorPos) +
                            val.substring(searchCursorPos + 1));
                }
                return true;
            } else if (key == 257) { // Enter
                focusedField = "";
                return true;
            } else if (key == 263) { // Left arrow
                if (shift) {
                    if (searchSelectionStart == searchSelectionEnd) {
                        searchSelectionStart = searchCursorPos;
                        searchSelectionEnd = searchCursorPos;
                    }
                    searchSelectionStart = Math.max(0, searchSelectionStart - 1);
                    searchCursorPos = searchSelectionStart;
                } else {
                    searchCursorPos = Math.max(0, searchCursorPos - 1);
                    searchSelectionStart = searchCursorPos;
                    searchSelectionEnd = searchCursorPos;
                }
                return true;
            } else if (key == 262) { // Right arrow
                if (shift) {
                    if (searchSelectionStart == searchSelectionEnd) {
                        searchSelectionStart = searchCursorPos;
                        searchSelectionEnd = searchCursorPos;
                    }
                    searchSelectionEnd = Math.min(getFocusedFieldValue().length(), searchSelectionEnd + 1);
                    searchCursorPos = searchSelectionEnd;
                } else {
                    searchCursorPos = Math.min(getFocusedFieldValue().length(), searchCursorPos + 1);
                    searchSelectionStart = searchCursorPos;
                    searchSelectionEnd = searchCursorPos;
                }
                return true;
            } else if (key == 268) { // Home
                if (shift) {
                    searchSelectionStart = 0;
                    searchCursorPos = 0;
                } else {
                    searchCursorPos = 0;
                    searchSelectionStart = 0;
                    searchSelectionEnd = 0;
                }
                return true;
            } else if (key == 269) { // End
                val = getFocusedFieldValue();
                if (shift) {
                    searchSelectionEnd = val.length();
                    searchCursorPos = val.length();
                } else {
                    searchCursorPos = val.length();
                    searchSelectionStart = val.length();
                    searchSelectionEnd = val.length();
                }
                return true;
            } else if (key == 65 && (modifiers & 0x2) != 0) { // Ctrl+A - Select all
                val = getFocusedFieldValue();
                searchSelectionStart = 0;
                searchSelectionEnd = val.length();
                searchCursorPos = val.length();
                return true;
            } else if (key == 67 && (modifiers & 0x2) != 0) { // Ctrl+C - Copy
                if (hasSelection()) {
                    val = getFocusedFieldValue();
                    this.minecraft.keyboardHandler.setClipboard(val.substring(getSelStart(), getSelEnd()));
                }
                return true;
            } else if (key == 86 && (modifiers & 0x2) != 0) { // Ctrl+V - Paste
                val = getFocusedFieldValue();
                String clip = this.minecraft.keyboardHandler.getClipboard();
                if (clip != null && !clip.isEmpty()) {
                    if (hasSelection()) {
                        val = deleteSelectedText(val);
                        searchCursorPos = getSelStart();
                        searchSelectionStart = searchCursorPos;
                        searchSelectionEnd = searchCursorPos;
                    }
                    if (searchCursorPos < 0) searchCursorPos = 0;
                    if (searchCursorPos > val.length()) searchCursorPos = val.length();
                    StringBuilder filtered = new StringBuilder();
                    for (int i = 0; i < clip.length(); i++) {
                        char c = clip.charAt(i);
                        if (focusedField.equals("proposeRateMinute")
                                || focusedField.equals("proposeRateText")
                                || focusedField.equals("proposeRateMb")) {
                            if (Character.isDigit(c)) filtered.append(c);
                        } else if (focusedField.equals("proposeTarget")) {
                            if (Character.isDigit(c) || c == '-') filtered.append(c);
                        } else {
                            filtered.append(c);
                        }
                    }
                    clip = filtered.toString();
                    val = val.substring(0, searchCursorPos) + clip + val.substring(searchCursorPos);
                    searchCursorPos += clip.length();
                    setFocusedFieldValue(val);
                }
                return true;
            } else if (key == 69) { // 'e' key - prevent closing inventory
                return true;
            }
        }

        if (searchFocused) {
            if (key == 259) { // Backspace
                if (hasSelection()) {
                    searchQuery = deleteSelectedText(searchQuery);
                    searchCursorPos = getSelStart();
                    searchSelectionStart = searchCursorPos;
                    searchSelectionEnd = searchCursorPos;
                } else if (searchCursorPos > 0) {
                    searchQuery = searchQuery.substring(0, searchCursorPos - 1) +
                            searchQuery.substring(searchCursorPos);
                    searchCursorPos--;
                }
                return true;
            } else if (key == 261) { // Delete
                if (hasSelection()) {
                    searchQuery = deleteSelectedText(searchQuery);
                    searchCursorPos = getSelStart();
                    searchSelectionStart = searchCursorPos;
                    searchSelectionEnd = searchCursorPos;
                } else if (searchCursorPos < searchQuery.length()) {
                    searchQuery = searchQuery.substring(0, searchCursorPos) +
                            searchQuery.substring(searchCursorPos + 1);
                }
                return true;
            } else if (key == 257) { // Enter
                searchFocused = false;
                return true;
            } else if (key == 263) { // Left arrow
                if (shift) {
                    if (searchSelectionStart == searchSelectionEnd) {
                        searchSelectionStart = searchCursorPos;
                        searchSelectionEnd = searchCursorPos;
                    }
                    searchSelectionStart = Math.max(0, searchSelectionStart - 1);
                    searchCursorPos = searchSelectionStart;
                } else {
                    searchCursorPos = Math.max(0, searchCursorPos - 1);
                    searchSelectionStart = searchCursorPos;
                    searchSelectionEnd = searchCursorPos;
                }
                return true;
            } else if (key == 262) { // Right arrow
                if (shift) {
                    if (searchSelectionStart == searchSelectionEnd) {
                        searchSelectionStart = searchCursorPos;
                        searchSelectionEnd = searchCursorPos;
                    }
                    searchSelectionEnd = Math.min(searchQuery.length(), searchSelectionEnd + 1);
                    searchCursorPos = searchSelectionEnd;
                } else {
                    searchCursorPos = Math.min(searchQuery.length(), searchCursorPos + 1);
                    searchSelectionStart = searchCursorPos;
                    searchSelectionEnd = searchCursorPos;
                }
                return true;
            } else if (key == 268) { // Home
                if (shift) {
                    searchSelectionStart = 0;
                    searchCursorPos = 0;
                } else {
                    searchCursorPos = 0;
                    searchSelectionStart = 0;
                    searchSelectionEnd = 0;
                }
                return true;
            } else if (key == 269) { // End
                if (shift) {
                    searchSelectionEnd = searchQuery.length();
                    searchCursorPos = searchQuery.length();
                } else {
                    searchCursorPos = searchQuery.length();
                    searchSelectionStart = searchQuery.length();
                    searchSelectionEnd = searchQuery.length();
                }
                return true;
            } else if (key == 65 && (modifiers & 0x2) != 0) { // Ctrl+A - Select all
                searchSelectionStart = 0;
                searchSelectionEnd = searchQuery.length();
                searchCursorPos = searchQuery.length();
                return true;
            } else if (key == 67 && (modifiers & 0x2) != 0) { // Ctrl+C - Copy
                if (hasSelection()) {
                    this.minecraft.keyboardHandler.setClipboard(searchQuery.substring(getSelStart(), getSelEnd()));
                }
                return true;
            } else if (key == 86 && (modifiers & 0x2) != 0) { // Ctrl+V - Paste
                String clip = this.minecraft.keyboardHandler.getClipboard();
                if (clip != null && !clip.isEmpty()) {
                    if (hasSelection()) {
                        searchQuery = deleteSelectedText(searchQuery);
                        searchCursorPos = getSelStart();
                        searchSelectionStart = searchCursorPos;
                        searchSelectionEnd = searchCursorPos;
                    }
                    if (searchCursorPos < 0) searchCursorPos = 0;
                    if (searchCursorPos > searchQuery.length()) searchCursorPos = searchQuery.length();
                    searchQuery = searchQuery.substring(0, searchCursorPos) + clip + searchQuery.substring(searchCursorPos);
                    searchCursorPos += clip.length();
                }
                return true;
            } else if (key == 69) { // 'e' key - prevent closing inventory
                return true;
            }
        }
        return super.keyPressed(key, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        // Modal char input
        if (activeModal != ModalType.NONE && focusedModalField != null && !focusedModalField.isEmpty()) {
            if (codePoint >= 32 && codePoint < 127) {
                String val = getModalFieldValue();
                if (searchCursorPos < 0) searchCursorPos = 0;
                if (searchCursorPos > val.length()) searchCursorPos = val.length();
                
                // If text is selected, delete it first
                if (hasSelection()) {
                    val = deleteSelectedText(val);
                    searchCursorPos = getSelStart();
                    searchSelectionStart = searchCursorPos;
                    searchSelectionEnd = searchCursorPos;
                }
                
                // Numeric-only fields (rates)
                if (focusedModalField.equals("proposeRateMinute")
                        || focusedModalField.equals("proposeRateText")
                        || focusedModalField.equals("proposeRateMb")) {
                    if (!Character.isDigit(codePoint)) {
                        return true;
                    }
                }
                // Phone number: digits and dashes only
                if (focusedModalField.equals("proposeTarget")) {
                    if (!Character.isDigit(codePoint) && codePoint != '-') {
                        return true;
                    }
                }
                // Plan edit: price and cycle days - digits only
                if (activeModal == ModalType.EDIT_PLAN
                        && (focusedModalField.equals("input2") || focusedModalField.equals("input3"))
                        && !Character.isDigit(codePoint)) {
                    return true;
                }
                // Plan edit: call minutes, texts, data MB - digits and minus
                if (activeModal == ModalType.EDIT_PLAN
                        && (focusedModalField.equals("input4")
                        || focusedModalField.equals("input5")
                        || focusedModalField.equals("input6"))
                        && !Character.isDigit(codePoint) && codePoint != '-') {
                    return true;
                }
                // Change player number - digits only
                if (activeModal == ModalType.CHANGE_PLAYER_NUMBER
                        && focusedModalField.equals("input1")
                        && !Character.isDigit(codePoint)) {
                    return true;
                }
                
                if (searchCursorPos < 0) searchCursorPos = 0;
                if (searchCursorPos > val.length()) searchCursorPos = val.length();
                setModalFieldValue(focusedModalField, val.substring(0, searchCursorPos)
                        + codePoint + val.substring(searchCursorPos));
                searchCursorPos++;
                searchSelectionStart = searchCursorPos;
                searchSelectionEnd = searchCursorPos;
                return true;
            }
        }
        if (focusedField != null && !focusedField.isEmpty()) {
            if (codePoint >= 32 && codePoint < 127) {
                String val = getFocusedFieldValue();
                if (searchCursorPos < 0) searchCursorPos = 0;
                if (searchCursorPos > val.length()) searchCursorPos = val.length();
                // If text is selected, delete it first
                if (hasSelection()) {
                    val = deleteSelectedText(val);
                    searchCursorPos = getSelStart();
                    searchSelectionStart = searchCursorPos;
                    searchSelectionEnd = searchCursorPos;
                }
                setFocusedFieldValue(val.substring(0, searchCursorPos) +
                        codePoint + val.substring(searchCursorPos));
                searchCursorPos++;
                searchSelectionStart = searchCursorPos;
                searchSelectionEnd = searchCursorPos;
                return true;
            }
        }
        if (searchFocused) {
            if (codePoint >= 32 && codePoint < 127) {
                // If text is selected, delete it first
                if (hasSelection()) {
                    searchQuery = deleteSelectedText(searchQuery);
                    searchCursorPos = getSelStart();
                    searchSelectionStart = searchCursorPos;
                    searchSelectionEnd = searchCursorPos;
                }
                if (searchCursorPos < 0) searchCursorPos = 0;
                if (searchCursorPos > searchQuery.length()) searchCursorPos = searchQuery.length();
                searchQuery = searchQuery.substring(0, searchCursorPos) +
                        codePoint + searchQuery.substring(searchCursorPos);
                searchCursorPos++;
                searchSelectionStart = searchCursorPos;
                searchSelectionEnd = searchCursorPos;
                return true;
            }
        }
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if ((activeTab == ServerTab.NETWORKS || activeTab == ServerTab.ANTENNAS || activeTab == ServerTab.ROAMING || activeTab == ServerTab.PLAYERS || activeTab == ServerTab.PLANS) && maxScrollOffset > 0) {
            scrollOffset = Math.clamp((int) (scrollOffset - scrollY * 40), 0, maxScrollOffset);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        // Scrollbar drag
        if ((activeTab == ServerTab.NETWORKS || activeTab == ServerTab.ANTENNAS || activeTab == ServerTab.ROAMING || activeTab == ServerTab.PLAYERS || activeTab == ServerTab.PLANS) && maxScrollOffset > 0) {
            int scrollbarX = contentAreaLeft + contentAreaWidth - AdminTheme.SPACE_XS - 4;
            int scrollbarY = listTop;
            int scrollbarH = listHeight;
            if (mouseX >= scrollbarX && mouseX < scrollbarX + 4 &&
                    mouseY >= scrollbarY && mouseY < scrollbarY + scrollbarH) {
                float visibleFraction = (float) listHeight / (listHeight + maxScrollOffset);
                int thumbHeight = Math.max(20, (int) (listHeight * visibleFraction));
                    float relativeY = (float) (mouseY - scrollbarY) / (listHeight - thumbHeight);
                scrollOffset = Math.clamp((int) (relativeY * maxScrollOffset), 0, maxScrollOffset);
                return true;
            }
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // No background - the panel covers everything
    }

    @Override
    public boolean isPauseScreen() {
            return true;
    }

        // NetworkHandler integration methods
        public boolean isRoamingTabSelected() {
            return activeTab == ServerTab.ROAMING;
        }

        public void refreshRoamingTab() {
            // Placeholder - will be implemented in later batches
            // For now, just force a re-render by requesting fresh data
            PacketDistributor.sendToServer(new com.example.mobilecellnetwork.network.RequestPhoneDataPacket());
        }
    }