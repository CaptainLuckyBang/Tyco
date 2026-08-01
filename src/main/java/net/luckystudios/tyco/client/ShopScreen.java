package net.luckystudios.tyco.client;

import net.luckystudios.tyco.network.BuyItemPayload;
import net.luckystudios.tyco.network.UnlockCategoryPayload;
import net.luckystudios.tyco.shop.CategoryDisplay;
import net.luckystudios.tyco.shop.ShopEntry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ShopScreen extends Screen {
    private static final ResourceLocation BACKGROUND =
            ResourceLocation.fromNamespaceAndPath("tyco", "textures/gui/shop_background.png");

    private static final int PANEL_WIDTH = 200;
    private static final int PANEL_HEIGHT = 200;

    private List<ShopEntry> allEntries;
    private List<CategoryDisplay> categoryDisplays;
    private String selectedCategory;

    private final List<Integer> visibleGlobalIndices = new ArrayList<>();
    private final Map<Integer, Integer> quantities = new HashMap<>();

    private final Map<Button, ResourceLocation> iconTabButtons = new HashMap<>();

    private static final int SLOT_SIZE = 18;
    private static final int COLUMN_GAP = 10;
    private static final int COLUMNS = 5;
    private static final int ROWS_PER_PAGE = 4;
    private static final int ITEMS_PER_PAGE = COLUMNS * ROWS_PER_PAGE;
    private static final int TAB_HEIGHT = 20;
    private static final int TABS_PER_PAGE = 4;
    private static final int TAB_BUTTON_WIDTH = 46;
    private static final int TAB_BUTTON_HEIGHT = 20;
    private static final int ARROW_BUTTON_WIDTH = 18;

    private int panelLeft;
    private int panelTop;
    private int gridLeft;
    private int gridTop;
    private int currentPage = 0;
    private int tabPage = 0;

    public ShopScreen(List<ShopEntry> entries, List<CategoryDisplay> categoryDisplays) {
        super(Component.literal("Tyco Shop"));
        this.allEntries = entries;
        this.categoryDisplays = categoryDisplays;

        if (!categoryDisplays.isEmpty()) {
            selectedCategory = categoryDisplays.get(0).category();
        }
    }

    @Override
    protected void init() {
        super.init();
        panelLeft = (this.width - PANEL_WIDTH) / 2;
        panelTop = (this.height - PANEL_HEIGHT) / 2;
        gridLeft = panelLeft + (PANEL_WIDTH - COLUMNS * (SLOT_SIZE + COLUMN_GAP)) / 2 + 4;
        gridTop = panelTop + 40;
        rebuildVisibleList();
        rebuildShopWidgets();
    }

    private void rebuildVisibleList() {
        visibleGlobalIndices.clear();
        for (int i = 0; i < allEntries.size(); i++) {
            if (allEntries.get(i).category().equals(selectedCategory)) {
                visibleGlobalIndices.add(i);
            }
        }
    }

    private int getMaxPage() {
        return Math.max(0, (visibleGlobalIndices.size() - 1) / ITEMS_PER_PAGE);
    }

    private int getMaxTabPage() {
        return Math.max(0, (categoryDisplays.size() - 1) / TABS_PER_PAGE);
    }

    private int quantityFor(int globalIndex) {
        return quantities.getOrDefault(globalIndex, 1);
    }

    // Finds the CategoryDisplay matching whichever category is currently selected
    private CategoryDisplay getSelectedCategoryDisplay() {
        for (CategoryDisplay cat : categoryDisplays) {
            if (cat.category().equals(selectedCategory)) return cat;
        }
        return null;
    }

    private boolean isSelectedCategoryLocked() {
        CategoryDisplay cat = getSelectedCategoryDisplay();
        return cat != null && cat.locked();
    }

    private int findHoveredGlobalIndex(double mouseX, double mouseY) {
        if (isSelectedCategoryLocked()) return -1;

        int startIndex = currentPage * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, visibleGlobalIndices.size());

        for (int i = startIndex; i < endIndex; i++) {
            int slotOnPage = i - startIndex;
            int col = slotOnPage % COLUMNS;
            int row = slotOnPage / COLUMNS;
            int x = gridLeft + col * (SLOT_SIZE + COLUMN_GAP);
            int y = gridTop + TAB_HEIGHT + row * (SLOT_SIZE + 12);

            if (mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16) {
                return visibleGlobalIndices.get(i);
            }
        }
        return -1;
    }

    private void rebuildShopWidgets() {
        this.clearWidgets();
        iconTabButtons.clear();

        int maxTabPage = getMaxTabPage();
        boolean needsTabPaging = maxTabPage > 0;

        int tabStart = tabPage * TABS_PER_PAGE;
        int tabEnd = Math.min(tabStart + TABS_PER_PAGE, categoryDisplays.size());

        int tabX = panelLeft + (needsTabPaging ? ARROW_BUTTON_WIDTH + 10 : 10);
        int tabY = panelTop + 20;

        for (int i = tabStart; i < tabEnd; i++) {
            CategoryDisplay cat = categoryDisplays.get(i);
            String categoryName = cat.category();
            boolean hasIcon = cat.iconItemId().isPresent();
            boolean locked = cat.locked();

            int width = hasIcon ? TAB_BUTTON_HEIGHT : TAB_BUTTON_WIDTH;
            Component label = hasIcon ? Component.literal("") : Component.literal(categoryName);

            // Tabs are always selectable now, locked or not - selecting a locked tab
            // just shows the unlock prompt instead of the item grid.
            Button tabButton = Button.builder(label, button -> {
                selectedCategory = categoryName;
                currentPage = 0;
                rebuildVisibleList();
                rebuildShopWidgets();
            }).bounds(tabX, tabY, width, TAB_BUTTON_HEIGHT).build();

            tabButton.active = !categoryName.equals(selectedCategory);

            Component tooltipText = locked
                    ? Component.literal(categoryName + " - Locked")
                    : Component.literal(categoryName);
            tabButton.setTooltip(Tooltip.create(tooltipText));

            this.addRenderableWidget(tabButton);
            if (hasIcon) {
                iconTabButtons.put(tabButton, cat.iconItemId().get());
            }

            tabX += width + 2;
        }

        if (needsTabPaging) {
            int finalTabPage = tabPage;

            Button prevTabButton = Button.builder(Component.literal("<"), button -> {
                tabPage--;
                rebuildShopWidgets();
            }).bounds(panelLeft + 6, tabY, ARROW_BUTTON_WIDTH, TAB_BUTTON_HEIGHT).build();
            prevTabButton.active = finalTabPage > 0;
            this.addRenderableWidget(prevTabButton);

            Button nextTabButton = Button.builder(Component.literal(">"), button -> {
                tabPage++;
                rebuildShopWidgets();
            }).bounds(panelLeft + PANEL_WIDTH - ARROW_BUTTON_WIDTH - 6, tabY, ARROW_BUTTON_WIDTH, TAB_BUTTON_HEIGHT).build();
            nextTabButton.active = finalTabPage < maxTabPage;
            this.addRenderableWidget(nextTabButton);
        }

        CategoryDisplay selected = getSelectedCategoryDisplay();

        if (selected != null && selected.locked()) {
            // Locked category selected - show a single Unlock button instead of item grid/pagination
            int unlockButtonY = gridTop + 60;
            Button unlockButton = Button.builder(Component.literal("Unlock"), button -> {
                PacketDistributor.sendToServer(new UnlockCategoryPayload(selectedCategory));
            }).bounds(panelLeft + PANEL_WIDTH / 2 - 40, unlockButtonY, 80, 20).build();
            this.addRenderableWidget(unlockButton);
        } else {
            int maxPage = getMaxPage();
            if (maxPage > 0) {
                int pageButtonY = gridTop + TAB_HEIGHT + ROWS_PER_PAGE * (SLOT_SIZE + 12) + 3;
                int finalCurrentPage = currentPage;

                Button prevPageButton = Button.builder(Component.literal("< Prev"), button -> {
                    currentPage--;
                    rebuildShopWidgets();
                }).bounds(gridLeft, pageButtonY, 60, 20).build();
                prevPageButton.active = finalCurrentPage > 0;
                this.addRenderableWidget(prevPageButton);

                Button nextPageButton = Button.builder(Component.literal("Next >"), button -> {
                    currentPage++;
                    rebuildShopWidgets();
                }).bounds(gridLeft + COLUMNS * (SLOT_SIZE + COLUMN_GAP) - 60, pageButtonY, 60, 20).build();
                nextPageButton.active = finalCurrentPage < maxPage;
                this.addRenderableWidget(nextPageButton);
            }
        }
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        guiGraphics.blit(BACKGROUND, panelLeft, panelTop, 0, 0, PANEL_WIDTH, PANEL_HEIGHT, PANEL_WIDTH, PANEL_HEIGHT);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        String title = "Tyco Shop";
        int titleWidth = this.font.width(title);
        guiGraphics.drawString(this.font, title, panelLeft + PANEL_WIDTH / 2 - titleWidth / 2, panelTop + 8, 0x404040, false);

        for (var entry : iconTabButtons.entrySet()) {
            Button button = entry.getKey();
            Item item = BuiltInRegistries.ITEM.get(entry.getValue());
            guiGraphics.renderItem(new ItemStack(item), button.getX() + 2, button.getY() + 2);
        }

        CategoryDisplay selected = getSelectedCategoryDisplay();

        if (selected != null && selected.locked()) {
            String costLine;
            if (selected.unlockPrice().isPresent()) {
                costLine = "Unlock for " + selected.unlockPrice().get() + "c";
            } else if (selected.unlockItemId().isPresent()) {
                Item unlockItem = BuiltInRegistries.ITEM.get(selected.unlockItemId().get());
                costLine = "Requires " + selected.unlockItemCount() + "x " + unlockItem.getDescription().getString();
            } else {
                costLine = "Locked";
            }

            String lockedText = selectedCategory + " is locked";
            int lockedTextWidth = this.font.width(lockedText);
            guiGraphics.drawString(this.font, lockedText, panelLeft + PANEL_WIDTH / 2 - lockedTextWidth / 2, gridTop + 20, 0x404040, false);

            int costLineWidth = this.font.width(costLine);
            guiGraphics.drawString(this.font, costLine, panelLeft + PANEL_WIDTH / 2 - costLineWidth / 2, gridTop + 36, 0xCC7A00, false);
        } else {
            int startIndex = currentPage * ITEMS_PER_PAGE;
            int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, visibleGlobalIndices.size());

            for (int i = startIndex; i < endIndex; i++) {
                int slotOnPage = i - startIndex;
                int globalIndex = visibleGlobalIndices.get(i);
                ShopEntry entry = allEntries.get(globalIndex);

                int col = slotOnPage % COLUMNS;
                int row = slotOnPage / COLUMNS;
                int x = gridLeft + col * (SLOT_SIZE + COLUMN_GAP);
                int y = gridTop + TAB_HEIGHT + row * (SLOT_SIZE + 12);

                Item item = BuiltInRegistries.ITEM.get(entry.itemId());
                ItemStack stack = new ItemStack(item);

                guiGraphics.renderItem(stack, x, y);

                int qty = quantityFor(globalIndex);
                long totalPrice = (long) entry.price() * qty;
                int centerX = x + 8;

                if (qty > 1) {
                    String qtyText = "x" + qty;
                    int qtyWidth = this.font.width(qtyText);
                    guiGraphics.drawString(this.font, qtyText, centerX - qtyWidth / 2, y - 8, 0xAAAAAA, false);
                }

                String priceText = formatPrice(totalPrice) + "c";
                int priceWidth = this.font.width(priceText);
                guiGraphics.drawString(this.font, priceText, centerX - priceWidth / 2, y + 18, 0xCC7A00, false);
            }

            int maxPage = getMaxPage();
            if (maxPage > 0) {
                int pageTextY = gridTop + TAB_HEIGHT + ROWS_PER_PAGE * (SLOT_SIZE + 12) + 26;
                String pageLabel = "Page " + (currentPage + 1) + " / " + (maxPage + 1);
                int pageLabelWidth = this.font.width(pageLabel);
                guiGraphics.drawString(this.font, pageLabel, panelLeft + PANEL_WIDTH / 2 - pageLabelWidth / 2, pageTextY, 0x555555, false);
            }

            int hoveredGlobalIndex = findHoveredGlobalIndex(mouseX, mouseY);
            if (hoveredGlobalIndex != -1) {
                ShopEntry hoveredEntry = allEntries.get(hoveredGlobalIndex);
                Item hoveredItem = BuiltInRegistries.ITEM.get(hoveredEntry.itemId());
                ItemStack hoveredStack = new ItemStack(hoveredItem);

                int qty = quantityFor(hoveredGlobalIndex);
                long exactPrice = (long) hoveredEntry.price() * qty;

                List<Component> tooltipLines = new ArrayList<>();
                tooltipLines.add(hoveredStack.getHoverName());
                tooltipLines.add(Component.literal("Price: " + exactPrice + "c").withStyle(style -> style.withColor(0xFFD700)));
                if (qty > 1) {
                    tooltipLines.add(Component.literal("(x" + qty + " @ " + hoveredEntry.price() + "c each)").withStyle(style -> style.withColor(0xAAAAAA)));
                }

                List<net.minecraft.util.FormattedCharSequence> formattedLines = tooltipLines.stream()
                        .map(Component::getVisualOrderText)
                        .toList();
                guiGraphics.renderTooltip(this.font, formattedLines, mouseX, mouseY);
            }
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int globalIndex = findHoveredGlobalIndex(mouseX, mouseY);
        if (globalIndex == -1) return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);

        int current = quantityFor(globalIndex);
        int changed = current + (scrollY > 0 ? 1 : -1);
        changed = Math.max(1, Math.min(64, changed));
        quantities.put(globalIndex, changed);
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }

        int globalIndex = findHoveredGlobalIndex(mouseX, mouseY);
        if (globalIndex != -1) {
            int qty = quantityFor(globalIndex);
            PacketDistributor.sendToServer(new BuyItemPayload(globalIndex, qty));
            return true;
        }

        return false;
    }

    private static String formatPrice(long value) {
        if (value >= 1_000_000) {
            return String.format("%.1fm", value / 1_000_000.0);
        } else if (value >= 100_000) {
            return String.format("%.1fh", value / 100_000.0);
        } else if (value >= 1_000) {
            return String.format("%.1fk", value / 1_000.0);
        } else {
            return String.valueOf(value);
        }
    }

    // Called when fresh data arrives (e.g. after a successful unlock) while this screen is already open -
    // updates the data in place instead of resetting selectedCategory/currentPage/tabPage
    public void updateData(List<ShopEntry> newEntries, List<CategoryDisplay> newCategories) {
        this.allEntries = newEntries;
        this.categoryDisplays = newCategories;
        rebuildVisibleList();
        rebuildShopWidgets();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}