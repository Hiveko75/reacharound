package com.spanser.reacharound.client.handler;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.tuple.Pair;

public class RayTraceHandler {
    public static HitResult rayTrace(Entity entity, Level world, Vec3 startPos, Vec3 ray, ClipContext.Block blockMode, ClipContext.Fluid fluidMode) {
        Vec3 end = startPos.add(ray);
        ClipContext context = new ClipContext(startPos, end, blockMode, fluidMode, entity);
        return world.clip(context);
    }

    public static Pair<Vec3, Vec3> getEntityParams(Entity player) {
        return Pair.of(eyePosition(player), aimDirection(player));
    }

    private static Vec3 eyePosition(Entity player) {
        Vec3 pos = player.position();
        double eyeHeight = player instanceof Player ? player.getEyeHeight(player.getPose()) : 0;
        return new Vec3(pos.x, pos.y + eyeHeight, pos.z);
    }

    private static Vec3 lookFromRotation(float pitch, float yaw) {
        float yawRad = yaw * (float) Math.PI / 180;
        float pitchRad = pitch * (float) Math.PI / 180;

        float horizontalFactor = -Mth.cos(pitchRad);
        float x = Mth.sin(yawRad) * horizontalFactor;
        float y = -Mth.sin(pitchRad);
        float z = -Mth.cos(yawRad) * horizontalFactor;

        return new Vec3(x, y, z).normalize();
    }

    /**
     * Direction from the player's eyes towards what the crosshair is aiming at.
     * <p>
     * In first person (and vanilla third person) this is the same as the player's look direction.
     * Third-person camera mods such as Leawind's Third Person aim the crosshair from a detached camera
     * and update {@code Minecraft.hitResult} accordingly, while the player itself may face elsewhere.
     * Aiming at the hit location keeps the reacharound target in line with the crosshair.
     */
    public static Vec3 aimDirection(Entity player) {
        Minecraft client = Minecraft.getInstance();
        if (player == client.player && client.hitResult != null) {
            Vec3 toTarget = client.hitResult.getLocation().subtract(eyePosition(player));
            if (toTarget.lengthSqr() > 1.0e-6) {
                return toTarget.normalize();
            }
        }
        return lookFromRotation(player.getXRot(), player.getYRot());
    }

    /** Pitch (degrees, positive = down) of {@link #aimDirection(Entity)}. */
    public static float aimXRot(Entity player) {
        Vec3 dir = aimDirection(player);
        return (float) -Math.toDegrees(Math.asin(Mth.clamp(dir.y, -1.0, 1.0)));
    }

    /** Yaw (degrees, Minecraft convention) of {@link #aimDirection(Entity)}. */
    public static float aimYRot(Entity player) {
        Vec3 dir = aimDirection(player);
        return (float) Math.toDegrees(Mth.atan2(-dir.x, dir.z));
    }
}
