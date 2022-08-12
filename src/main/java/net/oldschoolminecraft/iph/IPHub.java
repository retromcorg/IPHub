package net.oldschoolminecraft.iph;

import net.oldschoolminecraft.iph.handlers.PlayerHandler;
import net.oldschoolminecraft.iph.util.PLConfig;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public class IPHub extends JavaPlugin
{
    public static IPHub instance;

    public PLConfig config;

    public void onEnable()
    {
        instance = this;
        config = new PLConfig();

        getServer().getPluginManager().registerEvents(new PlayerHandler(this), this);

        System.out.println("IPHub enabled");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
    {
        if (label.equalsIgnoreCase("iphr") && (sender.hasPermission("iphub.reload") || sender.isOp()))
        {
            config.reload();
            sender.sendMessage(formatChat("&aIPHub configuration reloaded"));
            return true;
        }
        return false;
    }

    public static String formatChat(String message)
    {
        return message.replace("&", "\u00a7");
    }

    public void onDisable()
    {
        System.out.println("IPHub disabled");
    }
}
