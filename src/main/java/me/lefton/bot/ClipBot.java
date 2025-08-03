package me.lefton.bot;

import me.lefton.bot.command.BotCommand;
import me.lefton.bot.event.EventListener;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;

@Mod(
        modid = ClipBot.MODID,
        name = ClipBot.MODNAME,
        version = ClipBot.VERSION)
public class ClipBot {

    public static final String MODID = "clipbot";
    public static final String MODNAME = "ClipBot";
    public static final String VERSION = "1.3";

    @EventHandler
    public void init(FMLInitializationEvent event) {
        ClientCommandHandler.instance.registerCommand(new BotCommand());
        MinecraftForge.EVENT_BUS.register(new EventListener());
    }
}
