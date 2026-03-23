/*
 * Copyright (c) 2019-2024 GeyserMC. http://geysermc.org
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 * @author GeyserMC
 * @link https://github.com/GeyserMC/Geyser
 */

package org.geysermc.geyser.command.defaults;

import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.api.util.TriState;
import org.geysermc.geyser.command.GeyserCommand;
import org.geysermc.geyser.command.GeyserCommandSource;
import org.geysermc.geyser.network.EducationAuthManager;
import org.geysermc.geyser.network.EducationTenancyMode;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.text.ChatColor;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.context.CommandContext;

import java.time.Instant;
import java.util.List;

public class EduCommand extends GeyserCommand {

    private final GeyserImpl geyser;

    public EduCommand(GeyserImpl geyser, String name, String description, String permission) {
        super(name, description, permission, TriState.NOT_SET);
        this.geyser = geyser;
    }

    @Override
    public void register(CommandManager<GeyserCommandSource> manager) {
        // /geyser edu (no args = status)
        manager.command(baseBuilder(manager).handler(this::execute));

        // /geyser edu status
        manager.command(baseBuilder(manager)
                .literal("status")
                .handler(this::executeStatus));

        // /geyser edu players
        manager.command(baseBuilder(manager)
                .literal("players")
                .handler(this::executePlayers));

        // /geyser edu reset
        manager.command(baseBuilder(manager)
                .literal("reset")
                .handler(this::executeReset));

        // /geyser edu token
        manager.command(baseBuilder(manager)
                .literal("token")
                .handler(this::executeToken));
    }

    @Override
    public void execute(CommandContext<GeyserCommandSource> context) {
        executeStatus(context);
    }

    private void executeStatus(CommandContext<GeyserCommandSource> context) {
        GeyserCommandSource source = context.sender();
        EducationAuthManager eduAuth = geyser.getEducationAuthManager();
        EducationTenancyMode mode = geyser.config().education().tenancyMode();

        source.sendMessage(ChatColor.AQUA + "=== Education Edition Status ===");

        if (eduAuth == null || !eduAuth.isActive()) {
            source.sendMessage(ChatColor.YELLOW + "Education system: " + ChatColor.RED + "NOT INITIALIZED");
            source.sendMessage(ChatColor.GRAY + "Set tenancy-mode in the education section of config.yml to enable.");
            return;
        }

        source.sendMessage(ChatColor.YELLOW + "Education system: " + ChatColor.GREEN + "ACTIVE");
        source.sendMessage(ChatColor.YELLOW + "Tenancy mode: " + ChatColor.WHITE + mode);

        // Official/hybrid: show MESS registration info
        if (mode == EducationTenancyMode.OFFICIAL || mode == EducationTenancyMode.HYBRID) {
            source.sendMessage(ChatColor.YELLOW + "Server ID: " + ChatColor.WHITE + eduAuth.getServerId());
            source.sendMessage(ChatColor.YELLOW + "Server IP: " + ChatColor.WHITE + eduAuth.getServerIp());

            long expires = eduAuth.getServerTokenExpires();
            long now = Instant.now().getEpochSecond();
            String expiryStr = eduAuth.formatExpiry(expires);
            if (expires > now) {
                source.sendMessage(ChatColor.YELLOW + "MESS token: " + ChatColor.WHITE + expiryStr);
            } else {
                source.sendMessage(ChatColor.YELLOW + "MESS token: " + ChatColor.RED + "EXPIRED (" + expiryStr + ")");
            }
        }

        // Education player count
        int eduCount = 0;
        int totalCount = 0;
        for (GeyserSession session : geyser.onlineConnections()) {
            totalCount++;
            if (session.isEducationClient()) {
                eduCount++;
            }
        }
        source.sendMessage(ChatColor.YELLOW + "Education players: " + ChatColor.WHITE + eduCount + "/" + totalCount + " online");

        // Tenant table
        List<EducationAuthManager.TenantStatusInfo> tenants = eduAuth.getTenantStatusList();
        if (tenants.isEmpty()) {
            source.sendMessage(ChatColor.YELLOW + "Tenants: " + ChatColor.GRAY + "none registered");
        } else {
            source.sendMessage(ChatColor.YELLOW + "Tenants (" + tenants.size() + "):");
            for (EducationAuthManager.TenantStatusInfo info : tenants) {
                String shortId = info.tenantId().length() > 8
                        ? info.tenantId().substring(0, 8) + "..."
                        : info.tenantId();
                String statusColor = switch (info.status()) {
                    case "VALID" -> ChatColor.GREEN;
                    case "EXPIRING" -> ChatColor.GOLD;
                    case "EXPIRED" -> ChatColor.RED;
                    default -> ChatColor.GRAY;
                };
                String expiryDisplay = info.expiry() != Instant.EPOCH
                        ? eduAuth.formatExpiry(info.expiry().getEpochSecond())
                        : "unknown";
                String line = ChatColor.WHITE + "  " + shortId
                        + ChatColor.GRAY + " [" + info.source() + "] "
                        + statusColor + info.status()
                        + ChatColor.GRAY + " (expires: " + expiryDisplay + ")";
                if (info.tokenCount() > 1) {
                    line += ChatColor.GRAY + " [" + info.tokenCount() + " tokens]";
                }
                source.sendMessage(line);
            }
        }

        // Device-code token count
        int dcCount = eduAuth.getStandaloneTokenCount();
        if (dcCount > 0) {
            source.sendMessage(ChatColor.YELLOW + "Device-code tokens: " + ChatColor.WHITE + dcCount + " (auto-refreshing)");
        }
    }

    private void executePlayers(CommandContext<GeyserCommandSource> context) {
        GeyserCommandSource source = context.sender();

        source.sendMessage(ChatColor.AQUA + "=== Education Edition Players ===");

        int count = 0;
        for (GeyserSession session : geyser.onlineConnections()) {
            if (session.isEducationClient()) {
                count++;
                String name = session.bedrockUsername();
                String tenantId = session.getEducationTenantId() != null
                        ? session.getEducationTenantId() : "unknown";
                String roleName = session.getClientData() != null
                        ? session.getClientData().adRoleName() : "unknown";
                source.sendMessage(ChatColor.WHITE + "  " + name + ChatColor.GRAY
                        + " (tenant: " + tenantId + ", " + roleName + ")");
            }
        }

        if (count == 0) {
            source.sendMessage(ChatColor.GRAY + "No Education Edition players connected.");
        } else {
            source.sendMessage(ChatColor.YELLOW + "Total: " + count + " education player(s)");
        }
    }

    private void executeReset(CommandContext<GeyserCommandSource> context) {
        GeyserCommandSource source = context.sender();
        EducationAuthManager eduAuth = geyser.getEducationAuthManager();

        if (eduAuth == null || geyser.config().education().tenancyMode() == EducationTenancyMode.OFF) {
            source.sendMessage(ChatColor.RED + "Education system is not initialized.");
            return;
        }

        if (!eduAuth.resetAndReauthenticate()) {
            source.sendMessage(ChatColor.RED + "Education is not configured. Set server-name in edu_official.yml.");
            return;
        }
        source.sendMessage(ChatColor.YELLOW + "Resetting education session and re-authenticating...");
        source.sendMessage(ChatColor.GRAY + "This will delete the current session and start a new device code flow.");
        source.sendMessage(ChatColor.GRAY + "Check the console for the authentication code.");
    }

    private void executeToken(CommandContext<GeyserCommandSource> context) {
        GeyserCommandSource source = context.sender();
        EducationAuthManager eduAuth = geyser.getEducationAuthManager();

        EducationTenancyMode mode = geyser.config().education().tenancyMode();
        if (eduAuth == null || mode == EducationTenancyMode.OFF) {
            source.sendMessage(ChatColor.RED + "Education system is not initialized.");
            return;
        }

        if (mode == EducationTenancyMode.OFFICIAL) {
            source.sendMessage(ChatColor.RED + "Token command is not available in official mode.");
            source.sendMessage(ChatColor.GRAY + "Official mode obtains tokens automatically via MESS registration.");
            source.sendMessage(ChatColor.GRAY + "Use hybrid or standalone mode to add tokens manually.");
            return;
        }

        if (!eduAuth.startStandaloneTokenFlow()) {
            source.sendMessage(ChatColor.RED + "A token flow is already in progress. Wait for it to complete.");
            return;
        }
        source.sendMessage(ChatColor.YELLOW + "Starting device code flow to obtain a server token...");
        source.sendMessage(ChatColor.GRAY + "Sign in with any M365 Education account from the school you want to add.");
        source.sendMessage(ChatColor.GRAY + "Check the console for the authentication code.");
    }
}
