package fr.citizen.fonderie.client;

import fr.citizen.fonderie.menu.ForgeMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/** GUI dessiné en code (pas de texture) : s'adapte à 2, 4 ou 8 colonnes. Thème noir / or ancien / braise. */
public class ForgeScreen extends AbstractContainerScreen<ForgeMenu> {
    private static final int BG = 0xFF1C1814;
    private static final int BG_INNER = 0xFF26201A;
    private static final int GOLD = 0xFFB08D3C;
    private static final int GOLD_DARK = 0xFF6B5222;
    private static final int SLOT_BG = 0xFF0E0C0A;
    private static final int SLOT_EDGE = 0xFF4A3D2C;
    private static final int ARROW_BG = 0xFF3A3128;
    private static final int EMBER = 0xFFE0701C;
    private static final int EMBER_HOT = 0xFFFFC040;
    private static final int TEXT = 0xFFE8C66A;

    public ForgeScreen(ForgeMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 196;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;

        // cadre
        g.fill(x, y, x + imageWidth, y + imageHeight, GOLD_DARK);
        g.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, GOLD);
        g.fill(x + 2, y + 2, x + imageWidth - 2, y + imageHeight - 2, BG);
        g.fill(x + 5, y + 14, x + imageWidth - 5, y + 99, BG_INNER);

        // emplacements
        for (Slot slot : menu.slots) {
            int sx = x + slot.x - 1;
            int sy = y + slot.y - 1;
            g.fill(sx, sy, sx + 18, sy + 18, SLOT_EDGE);
            g.fill(sx + 1, sy + 1, sx + 17, sy + 17, SLOT_BG);
        }

        // flèches de fonte, une par colonne
        for (int i = 0; i < menu.slotCount(); i++) {
            int cx = x + menu.columnX(i) + 8; // centre de la colonne
            int top = y + ForgeMenu.INPUT_Y + 19;
            drawArrow(g, cx, top, ARROW_BG, 1.0F);
            float p = menu.progress(i);
            if (p > 0) {
                drawArrow(g, cx, top, EMBER, p);
            }
        }

        // flamme du combustible
        int fx = x + ForgeMenu.FUEL_X - 20;
        int fy = y + ForgeMenu.FUEL_Y + 1;
        drawFlame(g, fx, fy, ARROW_BG, 1.0F);
        float burn = menu.burn();
        if (burn > 0) {
            drawFlame(g, fx, fy, EMBER_HOT, burn);
        }
    }

    /** Flèche verticale de 16 px vers le bas, remplie depuis le haut selon {@code fill}. */
    private static void drawArrow(GuiGraphics g, int cx, int top, int color, float fill) {
        int limit = top + Math.round(16 * fill);
        for (int row = 0; row < 16; row++) {
            int yy = top + row;
            if (yy >= limit) {
                break;
            }
            int half = row < 11 ? 1 : 5 - (row - 11); // corps puis pointe
            g.fill(cx - half - 1, yy, cx + half + 1, yy + 1, color);
        }
    }

    /** Flamme 14×14 remplie depuis le bas. */
    private static void drawFlame(GuiGraphics g, int x, int y, int color, float fill) {
        int[] widths = {2, 4, 4, 6, 6, 8, 8, 10, 10, 12, 12, 12, 10, 8};
        int start = 14 - Math.round(14 * fill);
        for (int row = start; row < 14; row++) {
            int w = widths[row];
            int left = x + 7 - w / 2;
            g.fill(left, y + row, left + w, y + row + 1, color);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, titleLabelX, titleLabelY, TEXT, false);
        g.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, TEXT, false);
        Component info = Component.translatable("gui.citizenfonderie.info", menu.slotCount());
        g.drawString(font, info, imageWidth - 8 - font.width(info), ForgeMenu.FUEL_Y + 4, 0xFF9C8A6A, false);
    }
}
