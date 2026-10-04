package dev.l4z.viaproxyweb.service;

import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import dev.l4z.viaproxyweb.dto.config.ConfigRequest;
import dev.l4z.viaproxyweb.dto.config.ConfigResponse;
import net.raphimc.viaproxy.ViaProxy;
import net.raphimc.viaproxy.protocoltranslator.viaproxy.ViaProxyConfig;
import net.raphimc.viaproxy.util.AddressUtil;
import net.raphimc.viaproxy.util.Proxy;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URI;
import java.util.List;

public class ConfigService {

    public static ConfigResponse getConfig() {
        var cfg = ViaProxy.getConfig();
        var ui = ViaProxy.getSaveManager().uiSave;
        var proxy = cfg.getBackendProxy();

        return new ConfigResponse(
                formatAddress(cfg.getBindAddress()),
                proxy != null ? proxy.toString() : "",
                Boolean.parseBoolean(ui.get("legacy_skin_loading")),
                cfg.isProxyOnlineMode(),
                cfg.shouldSignChat(),
                cfg.shouldIgnoreProtocolTranslationErrors(),
                cfg.shouldAllowBetaPinging(),
                cfg.shouldSupportSimpleVoiceChat(),
                cfg.shouldFakeAcceptResourcePacks(),
                formatAddress(cfg.getTargetAddress()),
                cfg.getTargetVersion().getName(),
                cfg.getAuthMethod().name(),
                cfg.useBetacraftAuth()
        );
    }

    public static void applyConfig(ConfigRequest data) {
        var cfg = ViaProxy.getConfig();
        var ui = ViaProxy.getSaveManager().uiSave;

        if (data.proxyOnlineMode() != null) {
            cfg.setProxyOnlineMode(data.proxyOnlineMode());
        }

        if (data.chatSigning() != null) {
            cfg.setChatSigning(data.chatSigning());
        }

        if (data.ignorePacketTranslationErrors() != null) {
            cfg.setIgnoreProtocolTranslationErrors(
                    data.ignorePacketTranslationErrors()
            );
        }

        if (data.allowBetaPinging() != null) {
            cfg.setAllowBetaPinging(data.allowBetaPinging());
        }

        if (data.simpleVoiceChatSupport() != null) {
            cfg.setSimpleVoiceChatSupport(data.simpleVoiceChatSupport());
        }

        if (data.fakeAcceptResourcePacks() != null) {
            cfg.setFakeAcceptResourcePacks(data.fakeAcceptResourcePacks());
        }

        if (data.betacraftAuth() != null) {
            cfg.setBetacraftAuth(data.betacraftAuth());
        }

        if (data.legacySkinLoading() != null) {
            ui.put(
                    "legacy_skin_loading",
                    String.valueOf(data.legacySkinLoading())
            );
        }

        if (data.serverVersion() != null) {
            ProtocolVersion version = ProtocolVersion.getClosest(data.serverVersion());

            if (version == null) {
                throw new IllegalArgumentException(
                        "Invalid server version: " + data.serverVersion()
                );
            }

            cfg.setTargetVersion(version);
        }

        if (data.authMethod() != null) {
            try {
                cfg.setAuthMethod(
                        ViaProxyConfig.AuthMethod.valueOf(data.authMethod())
                );

            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(
                        "Invalid auth method: " + data.authMethod()
                );
            }
            if (cfg.getAuthMethod() == ViaProxyConfig.AuthMethod.NONE) {
                ViaProxy.getConfig().setAccount(null);
            }
        }

        ProtocolVersion version = cfg.getTargetVersion();

        if (data.serverAddress() != null) {
            String serverAddress = data.serverAddress().trim();

            if (!serverAddress.isEmpty()) {
                if (version == null) {
                    throw new IllegalArgumentException(
                            "Select a server version before setting the address"
                    );
                }

                try {
                    cfg.setTargetAddress(AddressUtil.parse(serverAddress, version));
                } catch (Throwable t) {
                    throw new IllegalArgumentException(
                            "Invalid server address: " + serverAddress
                    );
                }
            }
        }

        if (data.bindAddress() != null) {
            String bindAddress = data.bindAddress().trim();

            if (bindAddress.isEmpty()) {
                bindAddress = "0.0.0.0:25568";
            }

            try {
                cfg.setBindAddress(
                        AddressUtil.parse(bindAddress, null)
                );
            } catch (Throwable t) {
                throw new IllegalArgumentException(
                        "Invalid bind address: " + bindAddress
                );
            }
        }

        if (data.proxy() != null) {
            String proxyUrl = data.proxy().trim();

            if (!proxyUrl.isEmpty()) {
                try {
                    cfg.setBackendProxy(new Proxy(new URI(proxyUrl)));
                } catch (Exception e) {
                    throw new IllegalArgumentException(
                            "Invalid proxy URL: " + proxyUrl
                    );
                }
            } else {
                cfg.setBackendProxy(null);
            }
        }

        ViaProxy.getSaveManager().save();
        cfg.save();
    }

    public static List<String> getAvailableVersions() {
        return ProtocolVersion.getReversedProtocols().stream()
                .map(ProtocolVersion::getName)
                .toList();
    }

    public static List<String> getAuthMethods() {
        return java.util.Arrays.stream(ViaProxyConfig.AuthMethod.values())
                .map(Enum::name)
                .toList();
    }


    private static String formatAddress(SocketAddress addr) {
        if (addr instanceof InetSocketAddress isa) {
            return isa.getHostString() + ":" + isa.getPort();
        }
        return String.valueOf(addr);
    }
}