package dev.l4z.viaproxyweb.service;

import com.google.common.collect.Iterables;

import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import com.viaversion.viaversion.api.protocol.version.VersionType;

import dev.l4z.viaproxyweb.dto.proxy.ProxyResponse;
import dev.l4z.viaproxyweb.dto.realms.JoinRealmResponse;
import dev.l4z.viaproxyweb.dto.realms.RealmDto;
import dev.l4z.viaproxyweb.dto.realms.RealmsListResponse;

import dev.l4z.viaproxyweb.exception.NoAccountSelectedException;
import dev.l4z.viaproxyweb.exception.UnsupportedAccountException;

import net.raphimc.minecraftauth.MinecraftAuth;
import net.raphimc.minecraftauth.extra.realms.exception.RealmsRequestException;
import net.raphimc.minecraftauth.extra.realms.model.RealmsJoinInformation;
import net.raphimc.minecraftauth.extra.realms.model.RealmsServer;
import net.raphimc.minecraftauth.extra.realms.service.impl.BedrockRealmsService;
import net.raphimc.minecraftauth.extra.realms.service.impl.JavaRealmsService;
import net.raphimc.viabedrock.api.BedrockProtocolVersion;
import net.raphimc.viabedrock.protocol.data.ProtocolConstants;
import net.raphimc.viaproxy.ViaProxy;
import net.raphimc.viaproxy.saves.impl.accounts.Account;
import net.raphimc.viaproxy.saves.impl.accounts.BedrockAccount;
import net.raphimc.viaproxy.saves.impl.accounts.MicrosoftAccount;
import net.raphimc.viaproxy.util.AddressUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public final class RealmsService {

    // https://github.com/ViaVersion/ViaProxy/blob/main/src/main/java/net/raphimc/viaproxy/ui/impl/RealmsTab.java

    private static final ProtocolVersion LATEST_JAVA_RELEASE;
    private static final ProtocolVersion LATEST_JAVA_SNAPSHOT;

    static {
        ProtocolVersion latestRelease = null;
        ProtocolVersion latestSnapshot = null;

        List<ProtocolVersion> versions = ProtocolVersion.getProtocols();

        for (int i = versions.size() - 1; i >= 0; i--) {
            ProtocolVersion version = versions.get(i);

            if (version.getVersionType() != VersionType.RELEASE) {
                continue;
            }

            if (version.isSnapshot() && latestSnapshot == null) {
                latestSnapshot = version;
            } else if (!version.isSnapshot()) {
                latestRelease = version;
                break;
            }
        }

        if (latestRelease == null) {
            throw new IllegalStateException("No Java release version found");
        }

        LATEST_JAVA_RELEASE = latestRelease;
        LATEST_JAVA_SNAPSHOT = latestSnapshot;
    }

    private RealmsService() {
    }

    public static CompletableFuture<RealmsListResponse> listRealms(boolean snapshot) {
        Account account = ViaProxy.getConfig().getAccount();

        if (account == null) {
            return CompletableFuture.failedFuture(
                    new NoAccountSelectedException()
            );
        }

        switch (account) {
            case MicrosoftAccount ms -> {
                ProtocolVersion version = snapshot && LATEST_JAVA_SNAPSHOT != null ? LATEST_JAVA_SNAPSHOT : LATEST_JAVA_RELEASE;

                JavaRealmsService service = new JavaRealmsService(
                        MinecraftAuth.createHttpClient(),
                        Iterables.getLast(version.getIncludedVersions()),
                        ms.getAuthManager().getMinecraftToken(),
                        ms.getAuthManager().getMinecraftProfile()
                );

                return service.isCompatibleAsync().thenCompose(compatible -> {
                    if (!compatible) {
                        return CompletableFuture.completedFuture(
                                new RealmsListResponse(
                                        false,
                                        "java",
                                        version.getName(),
                                        account.getName(),
                                        List.of()
                                )
                        );
                    }

                    return service.getWorldsAsync().thenApply(worlds ->
                            new RealmsListResponse(
                                    true,
                                    "java",
                                    version.getName(),
                                    account.getName(),
                                    mapServers(worlds)
                            ));
                });

            }

            case BedrockAccount bedrock -> {
                BedrockRealmsService service = new BedrockRealmsService(
                        MinecraftAuth.createHttpClient(),
                        ProtocolConstants.BEDROCK_VERSION_NAME,
                        bedrock.getAuthManager().getRealmsXstsToken()
                );

                return service.isCompatibleAsync()
                        .thenCompose(compatible -> {
                            if (!compatible) {
                                return CompletableFuture.completedFuture(
                                        new RealmsListResponse(
                                                false,
                                                "bedrock",
                                                null,
                                                account.getName(),
                                                List.of()
                                        )
                                );
                            }

                            return service.getWorldsAsync()
                                    .thenApply(servers ->
                                            new RealmsListResponse(
                                                    true,
                                                    "bedrock",
                                                    null,
                                                    account.getName(),
                                                    mapServers(servers)
                                            )
                                    );
                        });
            }

            default -> {
                return CompletableFuture.failedFuture(
                        new UnsupportedAccountException(
                                "Unsupported account type: " + account.getClass().getSimpleName()
                        )
                );
            }
        }
    }

    public static CompletableFuture<JoinRealmResponse> joinRealm(String realmId, boolean snapshot, boolean autoStart) {

        Account account = ViaProxy.getConfig().getAccount();

        if (account == null) {
            return CompletableFuture.failedFuture(
                    new NoAccountSelectedException()
            );
        }

        ProtocolVersion version;

        if (account instanceof MicrosoftAccount ms) {
            version = snapshot && LATEST_JAVA_SNAPSHOT != null
                    ? LATEST_JAVA_SNAPSHOT
                    : LATEST_JAVA_RELEASE;

            JavaRealmsService service = new JavaRealmsService(
                    MinecraftAuth.createHttpClient(),
                    Iterables.getLast(version.getIncludedVersions()),
                    ms.getAuthManager().getMinecraftToken(),
                    ms.getAuthManager().getMinecraftProfile()
            );

            return service.getWorldsAsync().thenCompose(worlds -> {
                RealmsServer server = findServer(worlds, realmId);
                return service.joinWorldAsync(server);
            }).thenApply(joinInfo ->
                    finishJoin(joinInfo, version, autoStart)
            ).exceptionallyCompose(throwable -> {
                Throwable cause = throwable.getCause() != null ? throwable.getCause() : throwable;
                if (cause instanceof RealmsRequestException e && e.getErrorCode() == RealmsRequestException.ERROR_TOS_NOT_ACCEPTED) {
                    return CompletableFuture.completedFuture(
                            new JoinRealmResponse(
                                    "tos_required",
                                    null,
                                    version.getName(),
                                    null,
                                    null
                            )
                    );
                }
                return CompletableFuture.failedFuture(cause);
            });

        } else if (account instanceof BedrockAccount bedrock) {
            version = BedrockProtocolVersion.bedrockLatest;

            BedrockRealmsService service = new BedrockRealmsService(
                    MinecraftAuth.createHttpClient(),
                    ProtocolConstants.BEDROCK_VERSION_NAME,
                    bedrock.getAuthManager().getRealmsXstsToken()
            );

            return service.getWorldsAsync().thenCompose(worlds -> {
                RealmsServer server = findServer(worlds, realmId);
                return service.joinWorldAsync(server);
            }).thenApply(joinInfo ->
                    finishJoin(joinInfo, version, autoStart));
        }

        return CompletableFuture.failedFuture(
                new UnsupportedAccountException("Unsupported account type")
        );
    }

    public static CompletableFuture<Map<String, Object>> acceptTos(boolean snapshot) {
        Account account = ViaProxy.getConfig().getAccount();

        if (!(account instanceof MicrosoftAccount microsoftAccount)) {
            return CompletableFuture.failedFuture(
                    new IllegalStateException(
                            "TOS only applies to Java Microsoft accounts"
                    )
            );
        }

        ProtocolVersion version = snapshot && LATEST_JAVA_SNAPSHOT != null
                ? LATEST_JAVA_SNAPSHOT
                : LATEST_JAVA_RELEASE;

        JavaRealmsService service = new JavaRealmsService(
                MinecraftAuth.createHttpClient(),
                Iterables.getLast(version.getIncludedVersions()),
                microsoftAccount.getAuthManager().getMinecraftToken(),
                microsoftAccount.getAuthManager().getMinecraftProfile()
        );

        return service.acceptTosAsync().thenApply(_ -> Map.of("status", "accepted"));

    }

    private static JoinRealmResponse finishJoin(
            RealmsJoinInformation joinInfo,
            ProtocolVersion version,
            boolean autoStart
    ) {
        String address = formatJoinAddress(joinInfo);

        ViaProxy.getConfig().setTargetVersion(version);
        ViaProxy.getConfig().setTargetAddress(AddressUtil.parse(address, version));

        ViaProxy.getConfig().save();
        ViaProxy.getSaveManager().save();

        ProxyResponse proxy = null;

        if (autoStart) {
            if (ProxyService.isRunning()) {
                ProxyService.stop();
            }

            proxy = ProxyService.start();
        }

        return new JoinRealmResponse(
                "ok",
                address,
                version.getName(),
                String.valueOf(joinInfo.getNetworkProtocol()),
                proxy
        );
    }

    private static List<RealmDto> mapServers(List<RealmsServer> servers) {
        List<RealmsServer> sorted = new ArrayList<>(servers);

        sorted.sort((a, b) -> {
            boolean aGood = a.isCompatible() && !a.isExpired();
            boolean bGood = b.isCompatible() && !b.isExpired();

            return Boolean.compare(bGood, aGood);
        });

        return sorted.stream()
                .map(server -> new RealmDto(
                        String.valueOf(server.getId()),
                        server.getNameOr(""),
                        server.getOwnerName() != null
                                ? server.getOwnerName()
                                : "",
                        server.getState(),
                        server.getMotdOr(""),
                        server.getWorldType(),
                        server.getActiveVersion() != null
                                ? server.getActiveVersion()
                                : "",
                        server.isCompatible(),
                        server.isExpired()
                ))
                .toList();
    }

    private static RealmsServer findServer(List<RealmsServer> servers, String id) {
        return servers.stream()
                .filter(server -> String.valueOf(server.getId()).equals(id))
                .findFirst()
                .orElseThrow(() ->
                        new IllegalArgumentException("Realm not found: " + id)
                );
    }

    private static String formatJoinAddress(RealmsJoinInformation info) {
        return switch (info.getNetworkProtocol()) {
            case RealmsJoinInformation.PROTOCOL_DEFAULT -> info.getAddress();

            case RealmsJoinInformation.PROTOCOL_NETHERNET -> "nethernet://" + info.getAddress();

            case RealmsJoinInformation.PROTOCOL_NETHERNET_JSONRPC -> "nethernet-rpc://" + info.getAddress();

            default -> throw new IllegalArgumentException(
                    "Unknown realms network protocol: "
                            + info.getNetworkProtocol()
            );
        };
    }
}