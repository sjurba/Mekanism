package mekanism.client.gui.element.gauge;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import mekanism.api.text.EnumColor;
import mekanism.api.transmitters.TransmissionType;
import mekanism.client.gui.GuiMekanismTile;
import mekanism.client.gui.IGuiWrapper;
import mekanism.client.gui.element.GuiTexturedElement;
import mekanism.client.render.MekanismRenderer;
import mekanism.common.MekanismLang;
import mekanism.common.base.ISideConfiguration;
import mekanism.common.item.ItemConfigurator;
import mekanism.common.tile.base.TileEntityMekanism;
import mekanism.common.tile.component.config.ConfigInfo;
import mekanism.common.tile.component.config.DataType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.ITextComponent;

public abstract class GuiGauge<T> extends GuiTexturedElement {

    private final GaugeType gaugeType;
    protected boolean dummy;
    protected T dummyType;

    public GuiGauge(GaugeType gaugeType, IGuiWrapper gui, int x, int y) {
        this(gaugeType, gui, x, y, gaugeType.getGaugeOverlay().getWidth() + 2, gaugeType.getGaugeOverlay().getHeight() + 2);
    }

    public GuiGauge(GaugeType gaugeType, IGuiWrapper gui, int x, int y, int sizeX, int sizeY) {
        super(gaugeType.getGaugeOverlay().getBarOverlay(), gui, x, y, sizeX, sizeY);
        this.gaugeType = gaugeType;
    }

    public abstract int getScaledLevel();

    public abstract TextureAtlasSprite getIcon();

    public abstract ITextComponent getLabel();

    public abstract ITextComponent getTooltipText();

    protected GaugeInfo getGaugeColor() {
        return GaugeInfo.STANDARD;
    }

    protected void applyRenderColor() {
    }

    @Override
    public void renderButton(int mouseX, int mouseY, float partialTicks) {
        GaugeInfo color = getGaugeColor();
        renderExtendedTexture(color.getResourceLocation(), color.getSideWidth(), color.getSideHeight());
        if (!dummy) {
            int scale = getScaledLevel();
            TextureAtlasSprite icon = getIcon();
            if (scale > 0 && icon != null) {
                applyRenderColor();
                drawTiledSprite(x + 1, y + 1, height - 2, width - 2, scale, icon);
                MekanismRenderer.resetColor();
            }
            //Draw the bar overlay
            minecraft.textureManager.bindTexture(getResource());
            GaugeOverlay gaugeOverlay = gaugeType.getGaugeOverlay();

            blit(x + 1, y + 1, getWidth() - 2, getHeight() - 2, 0, 0, gaugeOverlay.getWidth(), gaugeOverlay.getHeight(), gaugeOverlay.getWidth(), gaugeOverlay.getHeight());
        }
    }

    @Override
    public void renderToolTip(int mouseX, int mouseY) {
        ItemStack stack = minecraft.player.inventory.getItemStack();
        EnumColor color = gaugeType.getGaugeInfo().getColor();
        if (!stack.isEmpty() && stack.getItem() instanceof ItemConfigurator && color != null) {
            if (guiObj instanceof GuiMekanismTile) {
                TileEntityMekanism tile = ((GuiMekanismTile<?, ?>) guiObj).getTileEntity();
                if (tile instanceof ISideConfiguration && getTransmission() != null) {
                    DataType dataType = null;
                    ConfigInfo config = ((ISideConfiguration) tile).getConfig().getConfig(getTransmission());
                    if (config != null) {
                        Set<DataType> supportedDataTypes = config.getSupportedDataTypes();
                        for (DataType type : supportedDataTypes) {
                            if (type.getColor() == color) {
                                dataType = type;
                                break;
                            }
                        }
                    }
                    if (dataType == null) {
                        guiObj.displayTooltip(MekanismLang.GENERIC_PARENTHESIS.translateColored(color, color.getName()), mouseX, mouseY);
                    } else {
                        guiObj.displayTooltip(MekanismLang.GENERIC_WITH_PARENTHESIS.translateColored(color, dataType, color.getName()), mouseX, mouseY);
                    }
                }
            }
        } else {
            List<ITextComponent> list = new ArrayList<>();
            if (getLabel() != null) {
                list.add(getLabel());
            }
            list.add(getTooltipText());
            guiObj.displayTooltips(list, mouseX, mouseY);
        }
    }

    public abstract TransmissionType getTransmission();

    public void setDummyType(T type) {
        dummyType = type;
    }
}