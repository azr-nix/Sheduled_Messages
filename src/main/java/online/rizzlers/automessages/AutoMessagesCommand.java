package online.rizzlers.automessages;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Supplier;

public class AutoMessagesCommand implements CommandExecutor, TabCompleter {

    public static final String ADMIN_PERMISSION = "rizzlersautomessages.admin";

    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final MessageManager messageManager;
    private final Supplier<Boolean> reloadAction;

    public AutoMessagesCommand(MessageManager messageManager, Supplier<Boolean> reloadAction) {
        this.messageManager = Objects.requireNonNull(messageManager, "messageManager cannot be null");
        this.reloadAction = Objects.requireNonNull(reloadAction, "reloadAction cannot be null");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission(ADMIN_PERMISSION)) {
                sender.sendMessage(miniMessage.deserialize("<red>You do not have permission to execute this command."));
                return true;
            }

            boolean success = reloadAction.get();
            if (!success) {
                sender.sendMessage(miniMessage.deserialize("<red>Invalid configuration in config.yml! Existing schedule remains active. Check console."));
                return true;
            }

            sender.sendMessage(miniMessage.deserialize("<green>RizzlersAutoMessages configuration reloaded successfully for server '<yellow>"
                    + messageManager.getServerName() + "</yellow>' (" + messageManager.getIntervalSeconds() + "s interval, "
                    + messageManager.getMessageCount() + " messages)."));
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
}
