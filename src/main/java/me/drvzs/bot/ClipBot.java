package me.drvzs.bot;

import me.drvzs.bot.commands.Command;
import me.drvzs.bot.events.EventListener;
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
    public static final String VERSION = "1.1";

    @EventHandler
    public void init(FMLInitializationEvent event) {
        ClientCommandHandler.instance.registerCommand(new Command());
        MinecraftForge.EVENT_BUS.register(new EventListener());
    }
}
