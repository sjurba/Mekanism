package mekanism.client.gui.element;

import java.util.List;
import java.util.function.Supplier;
import mekanism.client.gui.IGuiWrapper;
import mekanism.common.util.MekanismUtils;
import mekanism.common.util.MekanismUtils.ResourceType;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;

public class GuiInnerScreen extends GuiScalableElement {

    public static final ResourceLocation SCREEN = MekanismUtils.getResource(ResourceType.GUI, "inner_screen.png");

    private Supplier<List<ITextComponent>> renderStrings;

    private boolean centerY;
    private int spacing = 1;
    private int padding = 3;
    private float textScale = 1.0F;

    public GuiInnerScreen(IGuiWrapper gui, int x, int y, int width, int height) {
        super(SCREEN, gui, x, y, width, height, 32, 32);
    }

    public GuiInnerScreen(IGuiWrapper gui, int x, int y, int width, int height, Supplier<List<ITextComponent>> renderStrings) {
        this(gui, x, y, width, height);
        this.renderStrings = renderStrings;
        defaultFormat();
    }

    public GuiInnerScreen spacing(int spacing) {
        this.spacing = spacing;
        return this;
    }

    public GuiInnerScreen padding(int padding) {
        this.padding = padding;
        return this;
    }

    public GuiInnerScreen textScale(float textScale) {
        this.textScale = textScale;
        return this;
    }

    public GuiInnerScreen centerY() {
        this.centerY = true;
        return this;
    }

    public GuiInnerScreen defaultFormat() {
        return padding(5).spacing(3).textScale(0.8F).centerY();
    }

    @Override
    public void renderForeground(int mouseX, int mouseY, int xAxis, int yAxis) {
        super.renderForeground(mouseX, mouseY, xAxis, yAxis);

        if (renderStrings != null) {
            List<ITextComponent> list = renderStrings.get();
            int startY = relativeY + padding;
            if (centerY) {
                int totalHeight = list.size() * 8 + spacing * list.size() - 1;
                startY = relativeY + Math.round(getHeight() / 2F - totalHeight / 2F);
            }
            for (ITextComponent text : renderStrings.get()) {
                drawText(text, relativeX + padding, startY);
                startY += 8 + spacing;
            }
        }
    }

    private void drawText(ITextComponent text, int x, int y) {
        renderDynamicText(text, x, y, screenTextColor(), getWidth() - padding * 2, textScale);
    }
}