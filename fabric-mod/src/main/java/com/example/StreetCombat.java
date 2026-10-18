package com.example;

import java.util.Comparator;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.memory.*;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.RangedAttackMob;

/** Select Flash combat proxies; movement, attack cadence and damage stay native. */
public final class StreetCombat {
    public static boolean eligible(BridgeActor actor){return actor.combatTarget&&actor.isAlive()&&!actor.isInvulnerable();}
    public static void target(Mob mob){
        if(!(mob.level() instanceof ServerLevel level)||!level.getServer().getWorldData().getLevelName().equals("MC Dad n Me Bridge"))return;
        if(!(mob instanceof Enemy)&&!(mob instanceof RangedAttackMob)&&mob.getAttribute(Attributes.ATTACK_DAMAGE)==null)return;
        var current=mob.getTarget();
        if(current instanceof BridgeActor actor&&!eligible(actor)){
            mob.setTarget(null);mob.getNavigation().stop();
            var brain=mob.getBrain();if(brain.checkMemory(MemoryModuleType.ATTACK_TARGET,MemoryStatus.REGISTERED))brain.eraseMemory(MemoryModuleType.ATTACK_TARGET);
            current=null;
        }
        if(current!=null&&current.isAlive()&&!(current instanceof BridgeActor)&&!BridgeAllies.friendly(mob,current))return;
        double range=mob.getAttribute(Attributes.FOLLOW_RANGE)==null?16:Math.max(8,Math.min(64,mob.getAttributeValue(Attributes.FOLLOW_RANGE)));
        BridgeActor chosen=current instanceof BridgeActor actor&&mob.distanceToSqr(actor)<=range*range?actor:
            level.getEntitiesOfClass(BridgeActor.class,mob.getBoundingBox().inflate(range),a->eligible(a)&&mob.distanceToSqr(a)<=range*range&&mob.hasLineOfSight(a))
                .stream().min(Comparator.comparingDouble(mob::distanceToSqr)).orElse(null);
        if(chosen==null)return;
        mob.setTarget(chosen);
        // Zoglins, piglins and wardens use Brain memories instead of goal selectors.
        var brain=mob.getBrain();if(brain.checkMemory(MemoryModuleType.ATTACK_TARGET,MemoryStatus.REGISTERED))brain.setMemory(MemoryModuleType.ATTACK_TARGET,(LivingEntity)chosen);
    }
}
