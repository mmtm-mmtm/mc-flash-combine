package com.example;

import java.util.List;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** The Flash partner's hitbox, without zombie AI, armor, sounds or sun damage. */
public final class BridgeActor extends LivingEntity implements net.minecraft.world.entity.monster.Enemy {
    public boolean combatTarget;
    public BridgeActor(EntityType<? extends BridgeActor> type,Level level){
        super(type,level);setNoGravity(true);noPhysics=true;setSilent(true);
    }
    @Override public Iterable<ItemStack> getArmorSlots(){return List.of();}
    @Override public ItemStack getItemBySlot(EquipmentSlot slot){return ItemStack.EMPTY;}
    @Override public void setItemSlot(EquipmentSlot slot,ItemStack stack){}
    @Override public HumanoidArm getMainArm(){return HumanoidArm.RIGHT;}
    @Override public boolean isPickable(){return isAlive();}
}
