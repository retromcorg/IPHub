package net.oldschoolminecraft.iph.handlers;

import com.google.gson.Gson;
import com.projectposeidon.johnymuffin.ConnectionPause;
import net.oldschoolminecraft.iph.IPHub;
import net.oldschoolminecraft.iph.util.*;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerListener;
import org.bukkit.event.player.PlayerPreLoginEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public class PlayerHandler extends PlayerListener
{
    private static final Gson gson = new Gson();

    private final MemoryCache<String, IPHubResponse> cache = new MemoryCache<>(60 * 5, 3, 10000);
    private final PLConfig config = IPHub.instance.config;
    private int lastStatusCode = 200;
    private boolean needBackupKey = false;
    private JavaPlugin plugin;
    private BetaEvolutionsUtils betaEvolutionsUtils;

    public PlayerHandler(IPHub plugin)
    {
        this.plugin = plugin;
        this.betaEvolutionsUtils = new BetaEvolutionsUtils(false); //Disable debug for now
    }

    @EventHandler
    public void onPlayerPreLogin(PlayerPreLoginEvent event)
    {
        ConnectionPause pause = event.addConnectionPause(IPHub.instance, "IPHub-Main");
        Bukkit.getScheduler().scheduleAsyncDelayedTask(plugin, () -> {
            List<String> passthroughName = config.getConfigList("settings.passthrough.nameList");
            List<String> passthroughIP = config.getConfigList("settings.passthrough.ipList");

            //Beta Evolutions check
            if(config.getBoolean("settings.beta-evolutions-bypass.enabled", true)) {
                final BetaEvolutionsUtils.VerificationResults verificationResult = betaEvolutionsUtils.verifyUser(event.getName(), event.getAddress().getHostAddress());
                if(verificationResult.getSuccessful() > 0) {
                    pause.removeConnectionPause();
                    System.out.println(String.format("[IPHub Log] %s has bypassed VPN check as they are Beta Evolutions authenticated.", event.getName()));
                    return;
                }
            }

            if (passthroughName.contains(event.getName()))
            {
                pause.removeConnectionPause();
                return;
            }

            if (passthroughIP.contains(event.getAddress().getHostAddress()))
            {
                pause.removeConnectionPause();
                return;
            }

            IPHubResponse iphr = cache.get(event.getName());
            if (iphr != null && iphr.block != 1)
            {
                if (iphr.block == 2)
                    adminBroadcast(formatString(String.valueOf(config.getConfigOption("settings.messages.vpnPossible")), event.getName(), iphr), "iphub.warnblock2");
                pause.removeConnectionPause();
                return;
            }

            String ip = event.getAddress().getHostAddress();
            if (ip.equals("127.0.0.1"))
            {
                pause.removeConnectionPause();
                return;
            }

            if (lastStatusCode == 429) needBackupKey = !needBackupKey; // if rate limit is hit, switch keys.
            CloseableHttpClient httpclient = HttpClients.createDefault();
            HttpGet httpGet = new HttpGet("http://v2.api.iphub.info/ip/" + ip);
            httpGet.setHeader("X-Key", String.valueOf(config.getConfigOption(needBackupKey ? "settings.api.backupKey" : "settings.api.key")));
            try (CloseableHttpResponse res = httpclient.execute(httpGet))
            {
                HttpEntity ent = res.getEntity();
                int resCode = res.getStatusLine().getStatusCode();
                lastStatusCode = resCode;
                String rawResponse = EntityUtils.toString(ent);
                if ((Boolean) config.getConfigOption("settings.developer.debug"))
                    System.out.println(rawResponse);
                IPHubResponse response = gson.fromJson(rawResponse, IPHubResponse.class);
                if (resCode != 200 && resCode != 429)
                {
                    adminBroadcast(formatString(String.valueOf(config.getConfigOption("settings.messages.notChecked")), event.getName(), response), "iphub.warnblock2");
//                    event.cancelPlayerLogin(ColorUtil.translateAlternateColorCodes('&', ""));
                    event.cancelPlayerLogin(config.getMessage("settings.messages.notChecked"));
                    pause.removeConnectionPause();
                    return;
                }
                if ((Boolean) config.getConfigOption("settings.logging.enabled"))
                    System.out.println(String.format("[IPHub Log] %s: %s %s, %s (%s)", event.getName(), response.countryCode, ip, response.isp, response.asn));
                if (response.block == 1)
                {
//                    event.cancelPlayerLogin(ColorUtil.translateAlternateColorCodes('&', String.valueOf(config.getConfigOption("settings.messages.vpnDetected"))));
                    event.cancelPlayerLogin(config.getMessage("settings.messages.vpnDetected"));
                    adminBroadcast(formatString(String.valueOf(config.getConfigOption("settings.messages.vpnDetectedNotif")), event.getName(), response), "iphub.ipalert");
                    pause.removeConnectionPause();
                    return;
                }
                if (response.block == 2)
                    adminBroadcast(formatString(String.valueOf(config.getConfigOption("settings.messages.vpnPossible")), event.getName(), response), "iphub.warnblock2");
                EntityUtils.consume(ent);
                pause.removeConnectionPause();
                cache.put(event.getName(), response);
            } catch (Exception e) {
                e.printStackTrace();
//                event.cancelPlayerLogin(ColorUtil.translateAlternateColorCodes('&', String.valueOf(config.getConfigOption("settings.messages.notChecked"))));
                event.cancelPlayerLogin(config.getMessage("settings.messages.notChecked"));
                pause.removeConnectionPause();
            }
        });
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event)
    {
        IPHubResponse iphr = cache.get(event.getPlayer().getName());
        if (iphr == null || iphr.hasNullData()) return;
        for (Player p : Bukkit.getOnlinePlayers())
            if (p.hasPermission("iphub.ipalert") || p.isOp())
//                p.sendMessage(ColorUtil.translateAlternateColorCodes('&', formatString(String.valueOf(config.getConfigOption("settings.logging.msgFormat")), event.getPlayer().getName(), iphr)));
                p.sendMessage(IPHub.formatChat(formatString(String.valueOf(config.getConfigOption("settings.logging.msgFormat")), event.getPlayer().getName(), iphr)));
    }

    private void adminBroadcast(String msg, String permissionRequired)
    {
        for (Player p : Bukkit.getOnlinePlayers())
            if (p.hasPermission(permissionRequired) || p.isOp())
//                p.sendMessage(ColorUtil.translateAlternateColorCodes('&', msg));
                p.sendMessage(IPHub.formatChat(msg));
    }

    private String formatString(String input, String player, IPHubResponse iphData)
    {
        if (iphData == null || iphData.hasNullData()) return input;
        String temp = input.replace("{player}", player);
        temp = temp.replace("{ip}", iphData.ip);
        temp = temp.replace("{cnCode}", iphData.countryCode);
        temp = temp.replace("{cnName}", iphData.countryName);
        temp = temp.replace("{asn}", String.valueOf(iphData.asn));
        temp = temp.replace("{isp}", iphData.isp);
        temp = temp.replace("{block}", String.valueOf(iphData.block));
        return temp;
    }
}
