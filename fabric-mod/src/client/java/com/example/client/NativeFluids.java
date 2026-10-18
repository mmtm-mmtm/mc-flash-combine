package com.example.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/** Keep vanilla fluid geometry, flow, animated atlas textures, colors and lighting. */
final class NativeFluids {
    static boolean render(Minecraft mc,BlockPos pos,BlockState state,Matrix4f street,MultiBufferSource buffer){
        var fluid=state.getFluidState();
        if(fluid.isEmpty())return false;
        // LiquidBlockRenderer emits vertices relative to the containing 16-block section.
        var transform=new Matrix4f(street).translate(pos.getX()&~15,pos.getY()&~15,pos.getZ()&~15);
        var sink=buffer.getBuffer(ItemBlockRenderTypes.getRenderLayer(fluid));
        mc.getBlockRenderer().renderLiquid(pos,mc.level,new Projected(sink,transform),state,fluid);
        return true;
    }
    private static final class Projected implements VertexConsumer {
        private final VertexConsumer sink;
        private final Matrix4f matrix;
        private final Matrix3f normals;
        private final Vector3f vector=new Vector3f();
        Projected(VertexConsumer sink,Matrix4f matrix){this.sink=sink;this.matrix=matrix;normals=new Matrix3f(matrix).invert().transpose();}
        public VertexConsumer vertex(double x,double y,double z){matrix.transformPosition(vector.set((float)x,(float)y,(float)z));sink.vertex(vector.x,vector.y,vector.z);return this;}
        public VertexConsumer color(int r,int g,int b,int a){sink.color(r,g,b,a);return this;}
        public VertexConsumer uv(float u,float v){sink.uv(u,v);return this;}
        public VertexConsumer overlayCoords(int u,int v){sink.overlayCoords(u,v);return this;}
        public VertexConsumer uv2(int u,int v){sink.uv2(u,v);return this;}
        public VertexConsumer normal(float x,float y,float z){normals.transform(vector.set(x,y,z)).normalize();sink.normal(vector.x,vector.y,vector.z);return this;}
        public void endVertex(){sink.endVertex();}
        public void defaultColor(int r,int g,int b,int a){sink.defaultColor(r,g,b,a);}
        public void unsetDefaultColor(){sink.unsetDefaultColor();}
    }
}
