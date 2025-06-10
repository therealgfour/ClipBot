package me.drvzs.bot.events;

import me.drvzs.bot.commands.Command;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.client.C0APacketAnimation;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.MathHelper;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;
import java.util.Random;

public class EventListener {

    private final KeyBinding toggleKeybind = new KeyBinding("Toggle ClipBot", Keyboard.KEY_K, "ClipBot");
    private final Minecraft mc = Minecraft.getMinecraft();
    private float targetYaw = 0;
    private float targetPitch = 0;
    private int tickCounter = 0;
    private boolean isSwingPending = false;
    private final Random random = new Random();

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
            } else {
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindSprint.getKeyCode(), false);
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindForward.getKeyCode(), false);
                tickCounter = 0;
                isSwingPending = false;
            }
        }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START || mc.thePlayer == null || !Command.isClipBotEnabled || mc.currentScreen != null || mc.thePlayer.isDead) {
            return;
        }

        EntityPlayerSP player = mc.thePlayer;

        KeyBinding.setKeyBindState(mc.gameSettings.keyBindSprint.getKeyCode(), true);
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindForward.getKeyCode(), true);

        tickCounter++;
        int ticksPerClick = Math.round(20.0f / 7.0f); // ~2.857 ticks per click
        if (tickCounter >= ticksPerClick) {
            if (!isSwingPending) {
                player.sendQueue.addToSendQueue(new C0APacketAnimation());
                player.swingItem();
                isSwingPending = true;

                EntityPlayer target = findNearestPlayer();
                if (target != null && player.getDistanceToEntity(target) <= 3.0) {
                    player.sendQueue.addToSendQueue(new C02PacketUseEntity(target, C02PacketUseEntity.Action.ATTACK));
                }
            } else {
                isSwingPending = false;
                tickCounter = 0;
            }
        }

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
            if (obj == null || obj == player || !obj.isEntityAlive()) {
                continue;
            }

            double distance = player.getDistanceToEntity(obj);
            if (distance < closestDistance) {
                closestDistance = distance;
                closestPlayer = obj;
            }
        }

        return closestPlayer;
    }

    private void calculateTargetAngles(EntityPlayer target) {
        EntityPlayerSP player = mc.thePlayer;
        double dx = target.posX - player.posX;
        double dy = (target.posY + target.getEyeHeight() / 2) - (player.posY + player.getEyeHeight()) + 0.5;
        double dz = target.posZ - player.posZ;

        double distance = MathHelper.sqrt_double(dx * dx + dz * dz);
        targetYaw = (float) (Math.atan2(dz, dx) * 180 / Math.PI) - 90;
        targetPitch = (float) -(Math.atan2(dy, distance) * 180 / Math.PI);

        // 0.5 is the med value, lower = smoother and slower, higher = faster
        targetYaw += random.nextFloat() * 0.5f - 0.25f;
        targetPitch += random.nextFloat() * 0.5f - 0.25f;

        targetYaw = normalizeAngle(targetYaw);
        targetPitch = MathHelper.clamp_float(targetPitch, -90, 90);
    }

    private void aim(EntityPlayerSP player) {
        float currentYaw = normalizeAngle(player.rotationYaw);
        float currentPitch = player.rotationPitch;

        float yawDiff = normalizeAngle(targetYaw - currentYaw);
        float pitchDiff = targetPitch - currentPitch;

        player.rotationYaw = currentYaw + yawDiff * 0.5F;
        player.rotationPitch = currentPitch + pitchDiff * 0.5F;

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