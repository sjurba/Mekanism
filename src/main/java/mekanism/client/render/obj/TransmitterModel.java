package mekanism.client.render.obj;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.vecmath.Matrix4f;
import javax.vecmath.Vector3f;
import mekanism.api.EnumColor;
import mekanism.api.IColor;
import mekanism.common.Mekanism;
import mekanism.common.block.states.BlockStateHelper;
import mekanism.common.block.states.IStateColor;
import mekanism.common.block.transmitter.BlockDiversionTransporter;
import mekanism.common.config.MekanismConfig;
import mekanism.common.tile.transmitter.TileEntitySidedPipe.ConnectionType;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType;
import net.minecraft.client.renderer.block.model.ItemOverrideList;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumFacing.Axis;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraftforge.client.model.obj.OBJModel;
import net.minecraftforge.client.model.obj.OBJModel.Face;
import net.minecraftforge.client.model.obj.OBJModel.OBJState;
import net.minecraftforge.common.model.IModelState;
import net.minecraftforge.common.model.TRSRTransformation;
import org.apache.commons.lang3.tuple.Pair;

public class TransmitterModel extends OBJBakedModelBase {

    // Copy from old CTM
    public static Map<TransformType, TRSRTransformation> transforms = ImmutableMap.<TransformType, TRSRTransformation>builder()
          .put(TransformType.GUI, get(0, 0, 0, 30, 225, 0, 0.625f))
          .put(TransformType.THIRD_PERSON_RIGHT_HAND, get(0, 2.5f, 0, 75, 45, 0, 0.375f))
          .put(TransformType.THIRD_PERSON_LEFT_HAND, get(0, 2.5f, 0, 75, 45, 0, 0.375f))
          .put(TransformType.FIRST_PERSON_RIGHT_HAND, get(0, 0, 0, 0, 45, 0, 0.4f))
          .put(TransformType.FIRST_PERSON_LEFT_HAND, get(0, 0, 0, 0, 225, 0, 0.4f))
          .put(TransformType.GROUND, get(0, 2, 0, 0, 0, 0, 0.25f))
          .put(TransformType.HEAD, get(0, 0, 0, 0, 0, 0, 1))
          .put(TransformType.FIXED, get(0, 0, 0, 0, 0, 0, 1))
          .put(TransformType.NONE, get(0, 0, 0, 0, 0, 0, 0))
          .build();
    private static Set<TransmitterModel> modelInstances = new HashSet<>();
    private static TextureAtlasSprite[] transporter_center = new TextureAtlasSprite[2];
    private static TextureAtlasSprite[] transporter_center_color = new TextureAtlasSprite[2];
    private static TextureAtlasSprite[] transporter_side = new TextureAtlasSprite[2];
    private static TextureAtlasSprite[] transporter_side_color = new TextureAtlasSprite[2];
    private Map<Integer, List<BakedQuad>> modelCache = new HashMap<>();
    private TransmitterModel itemCache;
    private IBlockState tempState;
    @Nullable
    private EnumColor color;
    private TextureAtlasSprite particle;
    private TransmitterOverride override = new TransmitterOverride();

    public TransmitterModel(IBakedModel base, OBJModel model, IModelState state, VertexFormat format, ImmutableMap<String, TextureAtlasSprite> textures,
          Map<TransformType, Matrix4f> transform) {
        super(base, model, state, format, textures, transform);
        particle = textureMap.getOrDefault("None_Center", textureMap.getOrDefault("CentreMaterial", tempSprite));
        modelInstances.add(this);
    }

    private static TRSRTransformation get(float tx, float ty, float tz, float ax, float ay, float az, float s) {
        return new TRSRTransformation(new Vector3f(tx / 16, ty / 16, tz / 16), TRSRTransformation.quatFromXYZDegrees(new Vector3f(ax, ay, az)),
              new Vector3f(s, s, s), null);
    }

    public static void registerIcons(TextureMap map) {
        transporter_center[0] = map.registerSprite(new ResourceLocation(Mekanism.MODID, "blocks/models/multipart/LogisticalTransporterGlass"));
        transporter_center_color[0] = map.registerSprite(new ResourceLocation(Mekanism.MODID, "blocks/models/multipart/LogisticalTransporterGlassColored"));
        transporter_side[0] = map.registerSprite(new ResourceLocation(Mekanism.MODID, "blocks/models/multipart/LogisticalTransporterVerticalGlass"));
        transporter_side_color[0] = map.registerSprite(new ResourceLocation(Mekanism.MODID, "blocks/models/multipart/LogisticalTransporterVerticalGlassColored"));

        transporter_center[1] = map.registerSprite(new ResourceLocation(Mekanism.MODID, "blocks/models/multipart/opaque/LogisticalTransporterGlass"));
        transporter_center_color[1] = map.registerSprite(new ResourceLocation(Mekanism.MODID, "blocks/models/multipart/opaque/LogisticalTransporterGlassColored"));
        transporter_side[1] = map.registerSprite(new ResourceLocation(Mekanism.MODID, "blocks/models/multipart/opaque/LogisticalTransporterVerticalGlass"));
        transporter_side_color[1] = map.registerSprite(new ResourceLocation(Mekanism.MODID, "blocks/models/multipart/opaque/LogisticalTransporterVerticalGlassColored"));
    }

    public static void clearCache() {
        for (TransmitterModel model : modelInstances) {
            model.modelCache.clear();
            model.itemCache = null;
        }
    }

    @Nonnull
    @Override
    public ItemOverrideList getOverrides() {
        return override;
    }

    @Nonnull
    @Override
    public List<BakedQuad> getQuads(IBlockState state, EnumFacing side, long rand) {
        if (side != null) {
            return ImmutableList.of();
        }

        if (state != null && tempState == null) {
            BlockRenderLayer layer = MinecraftForgeClient.getRenderLayer();
            EnumColor color = null;

            if (state.getBlock() instanceof IStateColor && layer == BlockRenderLayer.TRANSLUCENT) {
                //Only try getting the color property for ones that will have a color

                IColor iColor = (IColor) state.getValue(BlockStateHelper.colorProperty);
                if (iColor != EnumColor.NONE) {
                    color = (EnumColor) iColor;
                }
            }

            try {
                ConnectionType down = state.getValue(BlockStateHelper.downConnectionProperty);
                ConnectionType up = state.getValue(BlockStateHelper.upConnectionProperty);
                ConnectionType north = state.getValue(BlockStateHelper.northConnectionProperty);
                ConnectionType south = state.getValue(BlockStateHelper.southConnectionProperty);
                ConnectionType west = state.getValue(BlockStateHelper.westConnectionProperty);
                ConnectionType east = state.getValue(BlockStateHelper.eastConnectionProperty);

                int hash = 1;
                hash = hash * 31 + layer.ordinal();
                hash = hash * 31 + down.ordinal();
                hash = hash * 31 + up.ordinal();
                hash = hash * 31 + north.ordinal();
                hash = hash * 31 + south.ordinal();
                hash = hash * 31 + west.ordinal();
                hash = hash * 31 + east.ordinal();
                if (color != null) {
                    hash = hash * 31 + color.ordinal();
                }

                if (!modelCache.containsKey(hash)) {
                    TransmitterModel model = new TransmitterModel(baseModel, getModel(), new OBJState(getVisibleGroups(down, up, north, south, west, east), true),
                          vertexFormat, textureMap, transformationMap);
                    model.tempState = state;
                    model.color = color;
                    modelCache.put(hash, model.getQuads(state, side, rand));
                }

                return modelCache.get(hash);
            } catch (Exception ignored) {
            }
        }

        return super.getQuads(state, side, rand);
    }

    public List<String> getVisibleGroups(ConnectionType down, ConnectionType up, ConnectionType north, ConnectionType south, ConnectionType west, ConnectionType east) {
        List<String> visible = new ArrayList<>();
        visible.add(EnumFacing.DOWN.getName() + down.getName().toUpperCase());
        visible.add(EnumFacing.UP.getName() + up.getName().toUpperCase());
        visible.add(EnumFacing.NORTH.getName() + north.getName().toUpperCase());
        visible.add(EnumFacing.SOUTH.getName() + south.getName().toUpperCase());
        visible.add(EnumFacing.WEST.getName() + west.getName().toUpperCase());
        visible.add(EnumFacing.EAST.getName() + east.getName().toUpperCase());
        return visible;
    }

    @Override
    public float[] getOverrideColor(Face f, String groupName) {
        if (tempState != null && color != null && MinecraftForgeClient.getRenderLayer() == BlockRenderLayer.TRANSLUCENT) {
            return new float[]{color.getColor(0), color.getColor(1), color.getColor(2), 1};
        }
        return null;
    }

    @Nonnull
    @Override
    public TextureAtlasSprite getParticleTexture() {
        return particle;
    }

    @Override
    public TextureAtlasSprite getOverrideTexture(Face f, String groupName) {
        if (tempState != null) {
            boolean sideIconOverride = getIconStatus(f, tempState) > 0;

            if (MinecraftForgeClient.getRenderLayer() == BlockRenderLayer.TRANSLUCENT) {
                int opaqueVal = MekanismConfig.current().client.opaqueTransmitters.val() ? 1 : 0;
                if (color != null) {
                    return (!sideIconOverride && f.getMaterialName().contains("Center")) ? transporter_center_color[opaqueVal] : transporter_side_color[opaqueVal];
                }
                return (!sideIconOverride && f.getMaterialName().contains("Center")) ? transporter_center[opaqueVal] : transporter_side[opaqueVal];
            } else if (groupName.endsWith("NONE") && sideIconOverride) {
                for (String s : getModel().getMatLib().getMaterialNames()) {
                    if (s.contains("Texture.Name")) {
                        continue;
                    }
                    if (!s.contains("Center") && !s.contains("Centre") && (MekanismConfig.current().client.opaqueTransmitters.val() == s.contains("Opaque"))) {
                        return textureMap.get(s);
                    }
                }
            } else if (MekanismConfig.current().client.opaqueTransmitters.val()) {
                return textureMap.get(f.getMaterialName() + "_Opaque");
            }
        }

        return null;
    }

    @Override
    public boolean shouldRotate(Face f, String groupName) {
        if (tempState != null) {
            return groupName.endsWith("NONE") && getIconStatus(f, tempState) == 2;
        }
        return false;
    }

    public byte getIconStatus(Face f, @Nonnull IBlockState state) {
        return getIconStatus(EnumFacing.getFacingFromVector(f.getNormal().x, f.getNormal().y, f.getNormal().z), state);
    }

    public byte getIconStatus(EnumFacing side, @Nonnull IBlockState state) {
        boolean hasDown = state.getValue(BlockStateHelper.downConnectionProperty) != ConnectionType.NONE;
        boolean hasUp = state.getValue(BlockStateHelper.upConnectionProperty) != ConnectionType.NONE;
        boolean hasNorth = state.getValue(BlockStateHelper.northConnectionProperty) != ConnectionType.NONE;
        boolean hasSouth = state.getValue(BlockStateHelper.southConnectionProperty) != ConnectionType.NONE;
        boolean hasWest = state.getValue(BlockStateHelper.westConnectionProperty) != ConnectionType.NONE;
        boolean hasEast = state.getValue(BlockStateHelper.eastConnectionProperty) != ConnectionType.NONE;
        boolean hasConnection = false;
        if (side == EnumFacing.DOWN) {
            hasConnection = hasDown;
        } else if (side == EnumFacing.UP) {
            hasConnection = hasUp;
        } else if (side == EnumFacing.NORTH) {
            hasConnection = hasNorth;
        } else if (side == EnumFacing.SOUTH) {
            hasConnection = hasSouth;
        } else if (side == EnumFacing.WEST) {
            hasConnection = hasWest;
        } else if (side == EnumFacing.EAST) {
            hasConnection = hasEast;
        }
        if (!hasConnection && !(state.getBlock() instanceof BlockDiversionTransporter)) {
            if (hasDown && hasUp && side != EnumFacing.DOWN && side != EnumFacing.UP) {
                return (byte) 1;
            } else if (hasNorth && hasSouth && (side == EnumFacing.DOWN || side == EnumFacing.UP)) {
                return (byte) 1;
            } else if (hasNorth && hasSouth && (side == EnumFacing.EAST || side == EnumFacing.WEST)) {
                return (byte) 2;
            } else if (hasWest && hasEast && side != EnumFacing.EAST && side != EnumFacing.WEST) {
                return (byte) 2;
            }
        }
        return (byte) 0;
    }

    @Nonnull
    @Override
    public Pair<? extends IBakedModel, Matrix4f> handlePerspective(ItemCameraTransforms.TransformType cameraTransformType) {
        return Pair.of(this, transforms.get(cameraTransformType).getMatrix());
    }

    private class TransmitterOverride extends ItemOverrideList {

        public TransmitterOverride() {
            super(new ArrayList<>());
        }

        @Nonnull
        @Override
        public IBakedModel handleItemState(@Nonnull IBakedModel originalModel, ItemStack stack, World world, EntityLivingBase entity) {
            if (itemCache == null) {
                List<String> visible = new ArrayList<>();
                for (EnumFacing side : EnumFacing.VALUES) {
                    visible.add(side.getName() + (side.getAxis() == Axis.Y ? "NORMAL" : "NONE"));
                }
                itemCache = new TransmitterModel(baseModel, getModel(), new OBJState(visible, true), vertexFormat, textureMap, transformationMap);
            }
            return itemCache;
        }
    }
}