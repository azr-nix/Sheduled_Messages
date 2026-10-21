package online.rizzlers.automessages;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class RizzlersAutoMessages extends JavaPlugin implements CommandExecutor, TabCompleter {

    public static final String ADMIN_PERMISSION = "rizzlersautomessages.admin";

    private final MiniMessage miniMessage = MiniMessage.miniMessage();
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
            command.setExecutor(this);
            command.setTabCompleter(this);
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
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission(ADMIN_PERMISSION)) {
                sender.sendMessage(miniMessage.deserialize("<red>You do not have permission to execute this command."));
                return true;
            }

            reloadConfig();
            if (!loadSettings()) {
                sender.sendMessage(miniMessage.deserialize("<red>Invalid configuration in config.yml! Existing schedule remains active. Check console."));
                return true;
            }

            startScheduler();
            sender.sendMessage(miniMessage.deserialize("<green>RizzlersAutoMessages configuration reloaded successfully for server '<yellow>"
                    + messageManager.getServerName() + "</yellow>' (" + messageManager.getIntervalSeconds() + "s interval, "
                    + messageManager.getMessageCount() + " messages)."));
            getLogger().info("Configuration reloaded by " + sender.getName() + ".");
            return true;
        }

        sender.sendMessage(miniMessage.deserialize("<gold>=== RizzlersAutoMessages ===</gold>"));
        sender.sendMessage(miniMessage.deserialize("<yellow>/automessages reload</yellow> <gray>- Reload configuration and restart timer</gray>"));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1 && sender.hasPermission(ADMIN_PERMISSION)) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            if ("reload".startsWith(prefix)) {
                return List.of("reload");
            }
        }
        return Collections.emptyList();
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
