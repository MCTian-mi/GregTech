package gregtech.client.renderer.scene;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexBuffer;
import net.minecraft.client.renderer.vertex.VertexFormatElement;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import java.nio.ByteBuffer;
import java.util.Collection;

import static gregtech.api.metatileentity.IFastRenderMetaTileEntity.RENDER_PASS_NORMAL;
import static gregtech.api.metatileentity.IFastRenderMetaTileEntity.RENDER_PASS_TRANSLUCENT;

@SideOnly(Side.CLIENT)
public class VBOWorldSceneRenderer extends WorldSceneRenderer {

//    protected int[] vaos;
    protected VertexBuffer[] vbos;
    protected boolean isDirty = true;

    public VBOWorldSceneRenderer(World world) {
        super(world);
        int layers = BlockRenderLayer.values().length;
//        this.vaos = new int[layers];
        this.vbos = new VertexBuffer[layers];
        for (int layer = 0; layer < layers; layer++) {
//            this.vaos[layer] = GL30.glGenVertexArrays();
            this.vbos[layer] = new VertexBuffer(DefaultVertexFormats.BLOCK);
        }
    }

    private void uploadVBO() {
        Minecraft mc = Minecraft.getMinecraft();
//        GlStateManager.enableCull();
//        GlStateManager.enableRescaleNormal();
//        RenderHelper.disableStandardItemLighting();
//        mc.entityRenderer.disableLightmap();
//        mc.renderEngine.bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
//        GlStateManager.disableLighting();
//        GlStateManager.enableTexture2D();
//        GlStateManager.enableAlpha();
        BlockRenderLayer oldRenderLayer = MinecraftForgeClient.getRenderLayer();

        try { // render block in each layer
            for (BlockRenderLayer layer : BlockRenderLayer.values()) {
                ForgeHooksClient.setRenderLayer(layer);
                int pass = layer == BlockRenderLayer.TRANSLUCENT ? 1 : 0;
                setDefaultPassRenderState(pass);

                BufferBuilder buffer = Tessellator.getInstance().getBuffer();
                buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
                BlockRendererDispatcher brd = mc.getBlockRendererDispatcher();

                for (BlockPos pos : renderedBlocks) {
                    IBlockState state = world.getBlockState(pos);
                    Block block = state.getBlock();
                    if (block == Blocks.AIR) continue;
                    state = state.getActualState(world, pos);
                    if (block.canRenderInLayer(state, layer)) {
                        brd.renderBlock(state, pos, world, buffer);
                    }
                }

                buffer.finishDrawing();
//                buffer.reset();

                var vbo = this.vbos[layer.ordinal()];
//                int vao = this.vaos[layer.ordinal()];

                ByteBuffer data = buffer.getByteBuffer();
                vbo.bufferData(data);

//                GL30.glBindVertexArray(vao);
//                vbo.bindBuffer();
//                setupArrayPointers();
//                GL30.glBindVertexArray(0);
//                vbo.unbindBuffer();

//                Tessellator.getInstance().getBuffer().setTranslation(0, 0, 0);
            }
        } finally {
            ForgeHooksClient.setRenderLayer(oldRenderLayer);
        }
//        RenderHelper.enableStandardItemLighting();
//        GlStateManager.enableLighting();
//
//        GlStateManager.enableDepth();
//        GlStateManager.disableBlend();
//        GlStateManager.depthMask(true);

        this.isDirty = false;
    }

    @Override
    public void render(float x, float y, float width, float height, int mouseX, int mouseY) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        super.render(x, y, width, height, mouseX, mouseY);
    }

    protected float x, y, width, height, mouseX, mouseY;


    @Override
    protected void drawWorld() {
        if (this.isDirty) {
            uploadVBO();
        }

        Minecraft mc = Minecraft.getMinecraft();
        GlStateManager.enableCull();
        GlStateManager.enableRescaleNormal();
        RenderHelper.disableStandardItemLighting();
        mc.entityRenderer.disableLightmap();
        mc.renderEngine.bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
        GlStateManager.disableLighting();
        GlStateManager.enableTexture2D();
        GlStateManager.enableAlpha();

        var oldRenderLayer = MinecraftForgeClient.getRenderLayer();
        for (var layer : BlockRenderLayer.values()) {

            ForgeHooksClient.setRenderLayer(layer);
            setDefaultPassRenderState(layer == BlockRenderLayer.TRANSLUCENT ? RENDER_PASS_TRANSLUCENT : RENDER_PASS_NORMAL);

            GlStateManager.glEnableClientState(GL11.GL_VERTEX_ARRAY);
            OpenGlHelper.setClientActiveTexture(OpenGlHelper.defaultTexUnit);
            GlStateManager.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
            OpenGlHelper.setClientActiveTexture(OpenGlHelper.lightmapTexUnit);
            GlStateManager.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
            OpenGlHelper.setClientActiveTexture(OpenGlHelper.defaultTexUnit);
            GlStateManager.glEnableClientState(GL11.GL_COLOR_ARRAY);

            GlStateManager.pushMatrix();
            {
                var sr = new ScaledResolution(mc);
//                int vao = this.vaos[layer.ordinal()];
                var vbo = this.vbos[layer.ordinal()];
                vbo.bindBuffer();
                setupArrayPointers();
//                GL30.glBindVertexArray(vao);
                vbo.drawArrays(GL11.GL_QUADS);
//                GL30.glBindVertexArray(0);
                vbo.unbindBuffer();
            }
            GlStateManager.popMatrix();

//            for (VertexFormatElement vertexformatelement : DefaultVertexFormats.BLOCK.getElements()) {
//                VertexFormatElement.EnumUsage enumUsage = vertexformatelement.getUsage();
//                int k1 = vertexformatelement.getIndex();
//
//                switch (enumUsage) {
//                    case POSITION -> GlStateManager.glDisableClientState(GL11.GL_VERTEX_ARRAY);
//                    case UV -> {
//                        OpenGlHelper.setClientActiveTexture(OpenGlHelper.defaultTexUnit + k1);
//                        GlStateManager.glDisableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
//                        OpenGlHelper.setClientActiveTexture(OpenGlHelper.defaultTexUnit);
//                    }
//                    case COLOR -> {
//                        GlStateManager.glDisableClientState(GL11.GL_COLOR_ARRAY);
//                        GlStateManager.resetColor();
//                    }
//                }
//            }
        }
        ForgeHooksClient.setRenderLayer(oldRenderLayer);

        renderTESR();

        GlStateManager.shadeModel(GL11.GL_SMOOTH);
        RenderHelper.enableStandardItemLighting();
        GlStateManager.enableDepth();
        GlStateManager.disableBlend();
        GlStateManager.depthMask(true);
    }

    @Override
    public WorldSceneRenderer addRenderedBlocks(@Nullable Collection<BlockPos> blocks) {
        this.isDirty = true;
        return super.addRenderedBlocks(blocks);
    }

    protected static void setupArrayPointers() {
        GlStateManager.glVertexPointer(3, GL11.GL_FLOAT, 28, 0);
        GlStateManager.glColorPointer(4, GL11.GL_UNSIGNED_BYTE, 28, 12);
        GlStateManager.glTexCoordPointer(2, GL11.GL_FLOAT, 28, 16);
        OpenGlHelper.setClientActiveTexture(OpenGlHelper.lightmapTexUnit);
        GlStateManager.glTexCoordPointer(2, GL11.GL_SHORT, 28, 24);
        OpenGlHelper.setClientActiveTexture(OpenGlHelper.defaultTexUnit);
    }

    protected void renderTESR() {
        RenderHelper.enableStandardItemLighting();
        for (int pass = 0; pass < 2; pass++) {
            ForgeHooksClient.setRenderPass(pass);
            setDefaultPassRenderState(pass);
            for (BlockPos pos : renderedBlocks) {
                TileEntity tile = world.getTileEntity(pos);
                if (tile != null) {
                    if (tile.shouldRenderInPass(pass)) {
                        TileEntityRendererDispatcher.instance.render(tile, pos.getX(), pos.getY(), pos.getZ(), 0);
                    }
                }
            }
        }
        ForgeHooksClient.setRenderPass(-1);
        RenderHelper.disableStandardItemLighting();
    }
}
