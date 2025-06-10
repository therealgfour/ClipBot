package me.drvzs.bot.events;

import me.drvzs.bot.commands.Command;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovementInput;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;

public class EventListener {

    private final KeyBinding toggleKeybind = new KeyBinding("Toggle ClipBot", Keyboard.KEY_K, "ClipBot");
    private final Minecraft mc = Minecraft.getMinecraft();
    private float targetYaw = 0;
    private float targetPitch = 0;

    public EventListener() {
        ClientRegistry.registerKeyBinding(toggleKeybind);
    }

    @SubscribeEvent
    public void onKeyPress(InputEvent.KeyInputEvent event) {
        if (toggleKeybind.isPressed()) {
            Command.isClipBotEnabled = !Command.isClipBotEnabled;
            mc.thePlayer.addChatMessage(new ChatComponentText("ClipBot " + (Command.isClipBotEnabled ? "enabled" : "disabled")));
            if (Command.isClipBotEnabled) {
                EntityPlayerSP player = mc.thePlayer;
                if (player != null) {
                    player.movementInput.moveForward = 1.0F;
                    player.setSprinting(true);
                }
            }
        }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START || mc.thePlayer == null || !Command.isClipBotEnabled) {
            return;
        }

        EntityPlayerSP player = mc.thePlayer;

        MovementInput movementInput = player.movementInput;
        movementInput.moveForward = 1.0F;
        player.setSprinting(true);
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindSprint.getKeyCode(), true); // Simulate sprint key
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindForward.getKeyCode(), true); // Simulate forward key

        float yawRad = player.rotationYaw * (float) (Math.PI / 180);
        player.motionX = -MathHelper.sin(yawRad) * 0.2F;
        player.motionZ = MathHelper.cos(yawRad) * 0.2F;

        EntityPlayer target = findNearestPlayer();
        if (target != null) {
            calculateTargetAngles(target);
            aim(player);
        }
    }

    private EntityPlayer findNearestPlayer() {
        EntityPlayerSP player = mc.thePlayer;
        EntityPlayer closestPlayer = null;
        double closestDistance = Double.MAX_VALUE;

        for (EntityPlayer obj : mc.theWorld.playerEntities) {
            if (obj == null || obj == player) {
                continue;
            }

            double distance = player.getDistanceToEntity(obj);
            if (distance < closestDistance && obj.isEntityAlive()) {
                closestDistance = distance;
                closestPlayer = obj;
            }
        }

        return closestPlayer;
    }

    private void calculateTargetAngles(EntityPlayer target) {
        EntityPlayerSP player = mc.thePlayer;
        double dx = target.posX - player.posX;
        double dy = (target.posY + target.getEyeHeight() / 2) - (player.posY + player.getEyeHeight());
        double dz = target.posZ - player.posZ;

        double distance = MathHelper.sqrt_double(dx * dx + dz * dz);
        targetYaw = (float) (Math.atan2(dz, dx) * 180 / Math.PI) - 90;
        targetPitch = (float) -(Math.atan2(dy, distance) * 180 / Math.PI);

        targetYaw = normalizeAngle(targetYaw);
        targetPitch = MathHelper.clamp_float(targetPitch, -90, 90);
    }

    private void aim(EntityPlayerSP player) {
        float currentYaw = normalizeAngle(player.rotationYaw);
        float currentPitch = player.rotationPitch;

        float yawDiff = normalizeAngle(targetYaw - currentYaw);
        float pitchDiff = targetPitch - currentPitch;
        // 0.7 is the med value, lower = smoother and slower, higher = faster
        player.rotationYaw = currentYaw + yawDiff * 0.7F;
        player.rotationPitch = currentPitch + pitchDiff * 0.7F;

        player.rotationYaw = normalizeAngle(player.rotationYaw);
        player.rotationPitch = MathHelper.clamp_float(player.rotationPitch, -90, 90);
    }

    private float normalizeAngle(float angle) {
        angle = angle % 360;
        if (angle > 180) {
            angle -= 360;
        } else if (angle < -180) {
            angle += 360;
        }
        return angle;
    }
}