package net.oldschoolminecraft.iph;

import net.oldschoolminecraft.iph.handlers.PlayerHandler;
import net.oldschoolminecraft.iph.util.ColorUtil;
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
            sender.sendMessage(ColorUtil.translateAlternateColorCodes('&', "&aIPHub configuration reloaded"));
            return true;
        }
        return false;
    }

    public void onDisable()
    {
        System.out.println("IPHub disabled");
    }
}
