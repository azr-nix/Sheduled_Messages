package online.rizzlers.automessages;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;

public final class RizzlersAutoMessages extends JavaPlugin {

    private final MessageManager messageManager = new MessageManager();
    private BukkitTask messageTask;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        if (!loadSettings()) {
            getLogger().severe("==================================================");
            getLogger().severe("RizzlersAutoMessages failed to load valid configuration.");
            getLogger().severe("Please inspect plugins/RizzlersAutoMessages/config.yml");
            getLogger().severe("==================================================");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        startScheduler();

        PluginCommand command = getCommand("automessages");
        if (command != null) {
            AutoMessagesCommand handler = new AutoMessagesCommand(messageManager, () -> {
                reloadConfig();
                if (!loadSettings()) {
                    return false;
                }
                startScheduler();
                return true;
            });
            command.setExecutor(handler);
            command.setTabCompleter(handler);
        }

        getLogger().info("==================================================");
        getLogger().info("RizzlersAutoMessages v" + getDescription().getVersion() + " initialized successfully.");
        getLogger().info("Active Server: '" + messageManager.getServerName() + "'");
        getLogger().info("Message Interval: " + messageManager.getIntervalSeconds() + " seconds (" + messageManager.getIntervalTicks() + " ticks)");
        getLogger().info("Loaded Messages: " + messageManager.getMessageCount() + " announcements in rotation");
        getLogger().info("First announcement will broadcast in " + messageManager.getIntervalSeconds() + " seconds.");
        getLogger().info("==================================================");
    }

    public boolean loadSettings() {
        FileConfiguration config = getConfig();
        String name = config.getString("server-name", "lobby");
        long interval = config.getLong("interval-seconds", 60);
        List<String> list = config.getStringList("messages");

        return messageManager.load(name, interval, list, getLogger());
    }

    public synchronized void startScheduler() {
        stopScheduler();
        long intervalTicks = messageManager.getIntervalTicks();
        // Delay by one full interval; thereafter repeat every intervalTicks
        messageTask = Bukkit.getScheduler().runTaskTimer(this, this::broadcastNextMessage, intervalTicks, intervalTicks);
    }

    public synchronized void stopScheduler() {
        if (messageTask != null) {
            if (!messageTask.isCancelled()) {
                messageTask.cancel();
            }
            messageTask = null;
        }
    }

    public void broadcastNextMessage() {
        Component message = messageManager.getNextMessage();
        if (message == null) {
            return;
        }

        // Broadcast to players currently online on this local backend server only
        int count = 0;
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(message);
            count++;
        }
        getLogger().fine("Dispatched scheduled message to " + count + " player(s) on " + messageManager.getServerName() + ".");
    }

    @Override
    public void onDisable() {
        stopScheduler();
        getLogger().info("RizzlersAutoMessages disabled.");
    }

    public MessageManager getMessageManager() {
        return messageManager;
    }

    public BukkitTask getMessageTask() {
        return messageTask;
    }
}
