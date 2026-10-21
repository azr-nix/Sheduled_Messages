# RizzlersAutoMessages

High-performance, lightweight rotating chat announcement plugin for the **RIZZLERS** Minecraft network, targeting **Purpur 26.3** and **Java 25**.

---

## Features

- **Rotating Announcements**: Automatically broadcasts configured announcements in sequential order, looping indefinitely.
- **Full Adventure MiniMessage Support**: Format messages with modern gradients, hex colors (`<color:#ff4fd8>`), bold, italics, click/hover events, and rainbow effects.
- **Independent Backend Configuration**:
  - **Lobby Backend**: Configured for `server-name: "lobby"` with `interval-seconds: 60`.
  - **Survival Backend**: Configured for `server-name: "survival"` with `interval-seconds: 120`.
- **Dynamic In-Game Reloading**: Update messages and intervals on the fly using `/automessages reload` (permission: `rizzlersautomessages.admin`).
- **Zero Duplicate Tasks**: Cancels existing scheduled timers before rescheduling, completely preventing duplicate broadcast tasks.
- **Local Server Isolation**: Broadcasts only to players connected to the local backend server instance.
- **Predictable Timing**: The first message broadcasts after one full interval, and every subsequent message follows the configured interval.

---

## Configuration (`config.yml`)

```yaml
# Configure each backend separately:
# Lobby:    server-name: "lobby",    interval-seconds: 60
# Survival: server-name: "survival", interval-seconds: 120

server-name: "lobby"
interval-seconds: 60

# Messages rotate in sequence and repeat indefinitely.
messages:
  - "<gradient:#ff4fd8:#7c4dff><bold>RIZZLERS</bold></gradient> <gray>»</gray> <white>Thanks for playing on RIZZLERS!</white>"
  - "<gradient:#ff4fd8:#7c4dff><bold>RIZZLERS</bold></gradient> <gray>»</gray> <white>Join our Discord community for updates and events!</white>"
  - "<gradient:#ff4fd8:#7c4dff><bold>RIZZLERS</bold></gradient> <gray>»</gray> <white>Need help? Contact a staff member.</white>"
  - "<gradient:#ff4fd8:#7c4dff><bold>RIZZLERS</bold></gradient> <gray>»</gray> <white>Invite your friends and enjoy RIZZLERS!</white>"
```

---

## Commands & Permissions

| Command | Description | Permission | Default |
| :--- | :--- | :--- | :--- |
| `/automessages reload` | Reloads `config.yml` and restarts the announcement scheduler | `rizzlersautomessages.admin` | OP |

---

## Installation & Deployment Instructions (Plugged.host / Pelican)

> [!IMPORTANT]
> Install this plugin on **backend servers** (Lobby and Survival).
> Do **NOT** install it on your Velocity proxy.

### 1. Lobby Backend Setup
1. **Back up the Lobby server**: Create a backup snapshot in your control panel.
2. **Upload JAR**: Upload `RizzlersAutoMessages-1.0.0.jar` into the Lobby server's `plugins/` directory.
3. **Restart the server**: Restart the Lobby server to generate the default configuration.
4. **Configure Lobby**: Verify or edit `plugins/RizzlersAutoMessages/config.yml`:
   ```yaml
   server-name: "lobby"
   interval-seconds: 60
   ```
5. **Reload**: Run `/automessages reload` from the console or in-game with `rizzlersautomessages.admin`.

### 2. Survival Backend Setup
1. **Back up the Survival server**: Create a backup snapshot in your control panel.
2. **Upload JAR**: Upload `RizzlersAutoMessages-1.0.0.jar` into the Survival server's `plugins/` directory.
3. **Restart the server**: Restart the Survival server to generate the configuration.
4. **Configure Survival**: Edit `plugins/RizzlersAutoMessages/config.yml`:
   ```yaml
   server-name: "survival"
   interval-seconds: 120
   ```
5. **Reload**: Run `/automessages reload` from the console or in-game with `rizzlersautomessages.admin`.

---

## Building from Source

This project compiles on GitHub Actions using Temurin JDK 25 and Paper/Purpur API 26.3:

```bash
mvn clean verify package
```

All automated JUnit 5 tests execute during `mvn clean verify`. Output JAR: `target/RizzlersAutoMessages-1.0.0.jar`.
