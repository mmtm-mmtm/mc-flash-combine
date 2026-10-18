package com.example;

import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.material.*;
import net.minecraft.world.phys.shapes.*;

/** The 2D fluid boundary is neither a physical wall nor an explosion shield. */
public final class BridgeContainment extends Block implements LiquidBlockContainer {
    public static final Block PLANE=Registry.register(BuiltInRegistries.BLOCK,new ResourceLocation("mcflash","containment_plane"),new BridgeContainment());
    private BridgeContainment(){super(BlockBehaviour.Properties.of().noCollission().noOcclusion().strength(-1,3600000).noLootTable());}
    public static void initialize(){}
    @Override public RenderShape getRenderShape(BlockState state){return RenderShape.INVISIBLE;}
    @Override public VoxelShape getShape(BlockState s,BlockGetter level,BlockPos pos,CollisionContext context){return Shapes.empty();}
    @Override public boolean canPlaceLiquid(BlockGetter level,BlockPos pos,BlockState state,Fluid fluid){return false;}
    @Override public boolean placeLiquid(LevelAccessor level,BlockPos pos,BlockState state,FluidState fluid){return false;}
}
