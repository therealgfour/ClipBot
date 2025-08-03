package me.fayne.bot.event;

import me.fayne.bot.command.BotCommand;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.client.C0APacketAnimation;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.MathHelper;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;

public class EventListener {
    private final KeyBinding toggleKeybind = new KeyBinding("Toggle ClipBot", Keyboard.KEY_K, "ClipBot");
    private static final Minecraft mc = Minecraft.getMinecraft();
    private int tickCounter = 0;
    private boolean isSwingPending = false;

    public EventListener() {
        ClientRegistry.registerKeyBinding(toggleKeybind);
    }

    @SubscribeEvent
    public void onKeyPress(InputEvent.KeyInputEvent event) {
        if (toggleKeybind.isPressed()) {
            BotCommand.isClipBotEnabled = !BotCommand.isClipBotEnabled;
            mc.thePlayer.addChatMessage(new ChatComponentText("ClipBot " + (BotCommand.isClipBotEnabled ? "enabled" : "disabled")));
            if (BotCommand.isClipBotEnabled) {
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
        if (event.phase != TickEvent.Phase.START || mc.thePlayer == null || !BotCommand.isClipBotEnabled || mc.currentScreen != null || mc.thePlayer.isDead) {
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
            } else {
                isSwingPending = false;
                tickCounter = 0;
            }
        }

        EntityPlayer target = findNearestPlayer();
        if (target != null) {
            aim(target, 0.0F, false);
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

    // Credits: Blowsy
    public void aim(EntityPlayer player, float ps, boolean pc) {
        if (isInFOV(player, 90.0F)) {
            float[] t = getTargetRotations(player);
            if (t != null) {
                float yaw = t[0];
                float pitch = MathHelper.clamp_float(t[1] + 4.0F + ps, -90.0F, 90.0F);
                if (pc) {
                    mc.getNetHandler().addToSendQueue(new C03PacketPlayer.C05PacketPlayerLook(yaw, pitch, mc.thePlayer.onGround));
                } else {
                    mc.thePlayer.rotationYaw = yaw;
                    mc.thePlayer.rotationPitch = pitch;
                }
            }

        }
    }

    public boolean isInFOV(Entity target, float maxFov) {
        if (target == null) return false;

        float[] rotations = getTargetRotations(target);
        float yawDifference = MathHelper.wrapAngleTo180_float(rotations[0] - mc.thePlayer.rotationYaw);
        return Math.abs(yawDifference) <= maxFov;
    }

    public float[] getTargetRotations(Entity q) {
        if (q == null) {
            return null;
        } else {
            double diffX = q.posX - mc.thePlayer.posX;
            double diffY;
            if (q instanceof EntityLivingBase) {
                EntityLivingBase en = (EntityLivingBase) q;
                diffY = en.posY + (double) en.getEyeHeight() * 0.9D - (mc.thePlayer.posY + (double) mc.thePlayer.getEyeHeight());
            } else {
                diffY = (q.getEntityBoundingBox().minY + q.getEntityBoundingBox().maxY) / 2.0D - (mc.thePlayer.posY + (double) mc.thePlayer.getEyeHeight());
            }

            double diffZ = q.posZ - mc.thePlayer.posZ;
            double dist = MathHelper.sqrt_double(diffX * diffX + diffZ * diffZ);
            float yaw = (float) (Math.atan2(diffZ, diffX) * 180.0D / 3.141592653589793D) - 90.0F;
            float pitch = (float) (-(Math.atan2(diffY, dist) * 180.0D / 3.141592653589793D));
            return new float[]{mc.thePlayer.rotationYaw + MathHelper.wrapAngleTo180_float(yaw - mc.thePlayer.rotationYaw), mc.thePlayer.rotationPitch + MathHelper.wrapAngleTo180_float(pitch - mc.thePlayer.rotationPitch)};
        }
    }
}