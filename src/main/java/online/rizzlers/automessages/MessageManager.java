package online.rizzlers.automessages;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

public class MessageManager {

    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final AtomicInteger currentIndex = new AtomicInteger(0);

    private String serverName = "lobby";
    private long intervalSeconds = 60;
    private List<String> rawMessages = Collections.emptyList();
    private List<Component> parsedMessages = Collections.emptyList();

    public synchronized boolean load(String serverName, long intervalSeconds, List<String> messages, Logger logger) {
        if (serverName == null || serverName.isBlank()) {
            if (logger != null) {
                logger.severe("Configuration error: 'server-name' must be specified and non-empty.");
            }
            return false;
        }

        if (intervalSeconds < 1) {
            if (logger != null) {
                logger.severe("Configuration error: 'interval-seconds' must be >= 1 (found " + intervalSeconds + ").");
            }
            return false;
        }

        if (messages == null || messages.isEmpty()) {
            if (logger != null) {
                logger.severe("Configuration error: 'messages' list is empty. At least one message is required.");
            }
            return false;
        }

        List<Component> compiled = new ArrayList<>();
        List<String> validRaws = new ArrayList<>();

        for (int i = 0; i < messages.size(); i++) {
            String raw = messages.get(i);
            if (raw == null || raw.isBlank()) {
                if (logger != null) {
                    logger.warning("Skipping empty message entry at index #" + (i + 1));
                }
                continue;
            }
            try {
                Component comp = miniMessage.deserialize(raw);
                compiled.add(comp);
                validRaws.add(raw);
            } catch (Exception ex) {
                if (logger != null) {
                    logger.severe("Could not parse MiniMessage syntax in message #" + (i + 1) + ": " + ex.getMessage());
                }
                return false;
            }
        }

        if (compiled.isEmpty()) {
            if (logger != null) {
                logger.severe("Configuration error: All provided messages were blank or invalid.");
            }
            return false;
        }

        this.serverName = serverName.trim();
        this.intervalSeconds = intervalSeconds;
        this.rawMessages = List.copyOf(validRaws);
        this.parsedMessages = List.copyOf(compiled);
        this.currentIndex.set(0);
        return true;
    }

    public Component getNextMessage() {
        if (parsedMessages.isEmpty()) {
            return null;
        }
        int index = currentIndex.getAndUpdate(i -> (i + 1) % parsedMessages.size());
        return parsedMessages.get(index % parsedMessages.size());
    }

    public Component peekCurrentMessage() {
        if (parsedMessages.isEmpty()) {
            return null;
        }
        return parsedMessages.get(currentIndex.get() % parsedMessages.size());
    }

    public void resetIndex() {
        currentIndex.set(0);
    }

    public int getCurrentIndex() {
        return currentIndex.get();
    }

    public String getServerName() {
        return serverName;
    }

    public long getIntervalSeconds() {
        return intervalSeconds;
    }

    public long getIntervalTicks() {
        return Math.max(20L, intervalSeconds * 20L);
    }

    public List<String> getRawMessages() {
        return rawMessages;
    }

    public List<Component> getParsedMessages() {
        return parsedMessages;
    }

    public int getMessageCount() {
        return parsedMessages.size();
    }
}
