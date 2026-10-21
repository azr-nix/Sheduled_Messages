package online.rizzlers.automessages;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

public class AutoMessagesTest {

    private static final Logger LOGGER = Logger.getLogger(AutoMessagesTest.class.getName());

    private final List<String> defaultMessages = List.of(
            "<gradient:#ff4fd8:#7c4dff><bold>RIZZLERS</bold></gradient> <gray>»</gray> <white>Thanks for playing on RIZZLERS!</white>",
            "<gradient:#ff4fd8:#7c4dff><bold>RIZZLERS</bold></gradient> <gray>»</gray> <white>Join our Discord community for updates and events!</white>",
            "<gradient:#ff4fd8:#7c4dff><bold>RIZZLERS</bold></gradient> <gray>»</gray> <white>Need help? Contact a staff member.</white>",
            "<gradient:#ff4fd8:#7c4dff><bold>RIZZLERS</bold></gradient> <gray>»</gray> <white>Invite your friends and enjoy RIZZLERS!</white>"
    );

    private MessageManager manager;

    @BeforeEach
    void setUp() {
        manager = new MessageManager();
    }

    @Test
    @DisplayName("1. Four messages rotate in the correct order and repeat indefinitely")
    void testFourMessagesRotationOrder() {
        boolean loaded = manager.load("lobby", 60, defaultMessages, LOGGER);
        assertTrue(loaded, "Manager should successfully load valid configuration.");
        assertEquals(4, manager.getMessageCount(), "Must have exactly 4 messages in rotation.");

        // First cycle: 0, 1, 2, 3
        Component m0 = manager.getNextMessage();
        Component m1 = manager.getNextMessage();
        Component m2 = manager.getNextMessage();
        Component m3 = manager.getNextMessage();

        assertNotNull(m0);
        assertNotNull(m1);
        assertNotNull(m2);
        assertNotNull(m3);

        String text0 = PlainTextComponentSerializer.plainText().serialize(m0);
        String text1 = PlainTextComponentSerializer.plainText().serialize(m1);
        String text2 = PlainTextComponentSerializer.plainText().serialize(m2);
        String text3 = PlainTextComponentSerializer.plainText().serialize(m3);

        assertTrue(text0.contains("Thanks for playing"));
        assertTrue(text1.contains("Discord community"));
        assertTrue(text2.contains("Need help"));
        assertTrue(text3.contains("Invite your friends"));

        // Second cycle: must repeat indefinitely starting from 0 again
        Component repeat0 = manager.getNextMessage();
        String repeatText0 = PlainTextComponentSerializer.plainText().serialize(repeat0);
        assertEquals(text0, repeatText0, "Cycle must repeat starting with message index 0.");

        Component repeat1 = manager.getNextMessage();
        String repeatText1 = PlainTextComponentSerializer.plainText().serialize(repeat1);
        assertEquals(text1, repeatText1, "Cycle must continue to index 1.");
    }

    @Test
    @DisplayName("2. Lobby 60-second and Survival 120-second schedules calculate correct ticks")
    void testScheduleIntervals() {
        // Lobby schedule: 60 seconds = 1200 ticks
        manager.load("lobby", 60, defaultMessages, LOGGER);
        assertEquals("lobby", manager.getServerName());
        assertEquals(60, manager.getIntervalSeconds());
        assertEquals(1200L, manager.getIntervalTicks(), "60 seconds must equate to 1200 ticks.");

        // Survival schedule: 120 seconds = 2400 ticks
        MessageManager survivalManager = new MessageManager();
        survivalManager.load("survival", 120, defaultMessages, LOGGER);
        assertEquals("survival", survivalManager.getServerName());
        assertEquals(120, survivalManager.getIntervalSeconds());
        assertEquals(2400L, survivalManager.getIntervalTicks(), "120 seconds must equate to 2400 ticks.");
    }

    @Test
    @DisplayName("3. Independent configuration on Lobby and Survival")
    void testIndependentLobbyAndSurvivalConfigurations() {
        MessageManager lobby = new MessageManager();
        List<String> lobbyMsgs = List.of("<yellow>Lobby Announcement 1</yellow>", "<yellow>Lobby Announcement 2</yellow>");
        lobby.load("lobby", 60, lobbyMsgs, LOGGER);

        MessageManager survival = new MessageManager();
        List<String> survivalMsgs = List.of("<green>Survival Tip 1</green>", "<green>Survival Tip 2</green>", "<green>Survival Tip 3</green>");
        survival.load("survival", 120, survivalMsgs, LOGGER);

        // Verify independent state
        assertEquals("lobby", lobby.getServerName());
        assertEquals(60, lobby.getIntervalSeconds());
        assertEquals(2, lobby.getMessageCount());

        assertEquals("survival", survival.getServerName());
        assertEquals(120, survival.getIntervalSeconds());
        assertEquals(3, survival.getMessageCount());

        // Verify independent rotation
        lobby.getNextMessage();
        assertEquals(1, lobby.getCurrentIndex());
        assertEquals(0, survival.getCurrentIndex(), "Advancing Lobby must not affect Survival's rotation index.");
    }

    @Test
    @DisplayName("4. MiniMessage formatting: colors, gradients, and formatting are parsed correctly")
    void testMiniMessageFormatting() {
        List<String> richMessages = List.of(
                "<gradient:#ff4fd8:#7c4dff><bold>RIZZLERS</bold></gradient> <gray>»</gray> <white>Formatted message</white>",
                "<rainbow>Colorful Announcement</rainbow>",
                "<color:#00ff00>Green text</color> with <click:open_url:'https://rizzlers.online'>clickable url</click>"
        );

        boolean ok = manager.load("lobby", 60, richMessages, LOGGER);
        assertTrue(ok, "Valid MiniMessage tags must be accepted.");
        assertEquals(3, manager.getMessageCount());

        Component c = manager.getNextMessage();
        assertNotNull(c);
        String plain = PlainTextComponentSerializer.plainText().serialize(c);
        assertTrue(plain.contains("RIZZLERS » Formatted message"));
    }

    @Test
    @DisplayName("5. Invalid or empty message lists and invalid intervals are safely rejected")
    void testInvalidConfigurationRejection() {
        // Blank server name
        assertFalse(manager.load("", 60, defaultMessages, LOGGER), "Blank server name must be rejected.");
        assertFalse(manager.load(null, 60, defaultMessages, LOGGER), "Null server name must be rejected.");

        // Invalid interval (less than 1 second)
        assertFalse(manager.load("lobby", 0, defaultMessages, LOGGER), "Zero interval must be rejected.");
        assertFalse(manager.load("lobby", -15, defaultMessages, LOGGER), "Negative interval must be rejected.");

        // Empty message list
        assertFalse(manager.load("lobby", 60, Collections.emptyList(), LOGGER), "Empty messages list must be rejected.");
        assertFalse(manager.load("lobby", 60, null, LOGGER), "Null messages list must be rejected.");

        // All blank entries
        assertFalse(manager.load("lobby", 60, List.of("   ", "\t", ""), LOGGER), "All-blank messages must be rejected.");

        // Malformed MiniMessage syntax (e.g. invalid gradient definition)
        assertFalse(manager.load("lobby", 60, List.of("<gradient:invalid>broken"), LOGGER),
                "Malformed MiniMessage syntax must be rejected safely without crashing.");
    }

    @Test
    @DisplayName("6. Permission checks for /automessages reload command")
    void testReloadPermissionChecks() {
        RizzlersAutoMessages plugin = new RizzlersAutoMessages();
        Command mockCommand = (Command) Proxy.newProxyInstance(
                Command.class.getClassLoader(),
                new Class<?>[]{Command.class},
                (proxy, method, args) -> null
        );

        // 1. Unprivileged sender
        final boolean[] deniedMsgSent = {false};
        CommandSender unprivilegedSender = (CommandSender) Proxy.newProxyInstance(
                CommandSender.class.getClassLoader(),
                new Class<?>[]{CommandSender.class},
                (proxy, method, args) -> {
                    if ("hasPermission".equals(method.getName())) {
                        return false;
                    }
                    if ("sendMessage".equals(method.getName())) {
                        deniedMsgSent[0] = true;
                    }
                    return null;
                }
        );

        plugin.onCommand(unprivilegedSender, mockCommand, "automessages", new String[]{"reload"});
        assertTrue(deniedMsgSent[0], "Sender without permission must receive a denial message.");

        // 2. Privileged admin sender
        final boolean[] adminPermissionChecked = {false};
        CommandSender adminSender = (CommandSender) Proxy.newProxyInstance(
                CommandSender.class.getClassLoader(),
                new Class<?>[]{CommandSender.class},
                (proxy, method, args) -> {
                    if ("hasPermission".equals(method.getName())) {
                        adminPermissionChecked[0] = true;
                        return RizzlersAutoMessages.ADMIN_PERMISSION.equals(args[0]);
                    }
                    return null;
                }
        );

        // Should check permission and proceed
        plugin.onCommand(adminSender, mockCommand, "automessages", new String[]{"reload"});
        assertTrue(adminPermissionChecked[0], "Admin sender permission must be verified.");
    }
}
