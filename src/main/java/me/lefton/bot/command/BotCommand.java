package me.lefton.bot.command;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentText;

import java.util.List;

public class BotCommand extends CommandBase {

    public static boolean isClipBotEnabled = false;

    @Override
    public String getCommandName() {
        return "clipbot";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/" + getCommandName() + "toggle";
    }

    @Override
    public boolean canCommandSenderUseCommand(ICommandSender sender) {
        return true;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("toggle")) {
            isClipBotEnabled = !isClipBotEnabled;
            sender.addChatMessage(new ChatComponentText("ClipBot " + (isClipBotEnabled ? "enabled" : "disabled")));
        } else {
            sender.addChatMessage(new ChatComponentText("Wrong usage: " + getCommandUsage(sender)));
        }
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args, BlockPos pos) {
        return args.length == 1 ? getListOfStringsMatchingLastWord(args, "toggle") : null;
    }
}