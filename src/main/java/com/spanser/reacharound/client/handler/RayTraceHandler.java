package com.spanser.reacharound.client.handler;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import org.apache.commons.lang3.tuple.Pair;

public class RayTraceHandler {
    public static HitResult rayTrace(Entity entity, World world, Vec3d startPos, Vec3d ray, RaycastContext.ShapeType blockMode, RaycastContext.FluidHandling fluidMode) {
        Vec3d end = startPos.add(ray);
        RaycastContext context = new RaycastContext(startPos, end, blockMode, fluidMode, entity);
        return world.raycast(context);
    }

    public static Pair<Vec3d, Vec3d> getEntityParams(Entity player) {
        return Pair.of(eyePosition(player), aimDirection(player));
    }

    private static Vec3d eyePosition(Entity player) {
        Vec3d pos = player.getEntityPos();
        double eyeHeight = player instanceof PlayerEntity ? player.getEyeHeight(player.getPose()) : 0;
        return new Vec3d(pos.x, pos.y + eyeHeight, pos.z);
    }

    private static Vec3d lookFromRotation(float pitch, float yaw) {
        float yawRad = yaw * (float) Math.PI / 180;
        float pitchRad = pitch * (float) Math.PI / 180;

        float horizontalFactor = -MathHelper.cos(pitchRad);
        float x = MathHelper.sin(yawRad) * horizontalFactor;
        float y = -MathHelper.sin(pitchRad);
        float z = -MathHelper.cos(yawRad) * horizontalFactor;

        return new Vec3d(x, y, z).normalize();
    }

    /**
     * Direction from the player's eyes towards what the crosshair is aiming at.
     * <p>
     * In first person (and vanilla third person) this is the same as the player's look direction.
     * Third-person camera mods such as Leawind's Third Person aim the crosshair from a detached camera
     * and update {@code MinecraftClient.crosshairTarget} accordingly, while the player itself may face
     * elsewhere. Aiming at the hit location keeps the reacharound target in line with the crosshair.
     */
    public static Vec3d aimDirection(Entity player) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (player == client.player && client.crosshairTarget != null) {
            Vec3d toTarget = client.crosshairTarget.getPos().subtract(eyePosition(player));
            if (toTarget.lengthSquared() > 1.0e-6) {
                return toTarget.normalize();
            }
        }
        return lookFromRotation(player.getPitch(), player.getYaw());
    }

    /** Pitch (degrees, positive = down) of {@link #aimDirection(Entity)}. */
    public static float aimXRot(Entity player) {
        Vec3d dir = aimDirection(player);
        return (float) -Math.toDegrees(Math.asin(MathHelper.clamp(dir.y, -1.0, 1.0)));
    }

    /** Yaw (degrees, Minecraft convention) of {@link #aimDirection(Entity)}. */
    public static float aimYRot(Entity player) {
        Vec3d dir = aimDirection(player);
        return (float) Math.toDegrees(MathHelper.atan2(-dir.x, dir.z));
    }
}
