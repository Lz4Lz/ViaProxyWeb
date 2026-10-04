package dev.l4z.viaproxyweb.service;

import dev.l4z.viaproxyweb.dto.account.AccountInfo;
import dev.l4z.viaproxyweb.dto.account.LoginStartResponse;
import dev.l4z.viaproxyweb.dto.account.LoginStatusResponse;

import net.raphimc.minecraftauth.MinecraftAuth;
import net.raphimc.minecraftauth.bedrock.BedrockAuthManager;
import net.raphimc.minecraftauth.java.JavaAuthManager;
import net.raphimc.minecraftauth.msa.model.MsaDeviceCode;
import net.raphimc.minecraftauth.msa.service.impl.DeviceCodeMsaAuthService;
import net.raphimc.viaproxy.ViaProxy;
import net.raphimc.viaproxy.protocoltranslator.viaproxy.ViaProxyConfig;
import net.raphimc.viaproxy.saves.impl.accounts.BedrockAccount;
import net.raphimc.viaproxy.saves.impl.accounts.MicrosoftAccount;

import static net.raphimc.viabedrock.protocol.data.ProtocolConstants.BEDROCK_VERSION_NAME;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.stream.IntStream;

public class AccountService {

    private static final Logger LOGGER = LogManager.getLogger(AccountService.class);

    private static final ExecutorService LOGIN_EXECUTOR = Executors.newVirtualThreadPerTaskExecutor();
    private static final Map<String, PendingLogin> pendingLogins = new ConcurrentHashMap<>();

    public enum AccountType {
        MICROSOFT,
        BEDROCK
    }

    public enum LoginState {
        PENDING,
        SUCCESS,
        ERROR
    }

    public static void shutdown() {
        LOGIN_EXECUTOR.shutdownNow();
    }

    public static List<AccountInfo> getAccounts() {
        var accounts = ViaProxy.getSaveManager().accountsSave.getAccounts();

        return IntStream.range(0, accounts.size())
                .mapToObj(i -> {
                    var account = accounts.get(i);

                    return new AccountInfo(
                            i,
                            account.getName(),
                            account.getClass().getSimpleName()
                    );
                })
                .toList();
    }


    public static CompletableFuture<LoginStartResponse> startDeviceLogin(AccountType type) {
        String loginId = UUID.randomUUID().toString();

        PendingLogin pending = new PendingLogin();
        pendingLogins.put(loginId, pending);

        CompletableFuture<LoginStartResponse> response = new CompletableFuture<>();

        LOGIN_EXECUTOR.submit(() -> {
            try {
                if (type == AccountType.MICROSOFT) {
                    loginMicrosoft(pending, loginId, response);
                } else {
                    loginBedrock(pending, loginId, response);
                }

                ViaProxy.getSaveManager().save();

                pending.status = LoginState.SUCCESS;
            } catch (Exception e) {
                pending.status = LoginState.ERROR;

                if (e instanceof TimeoutException) {
                    pending.error = "Timed out.";
                } else {
                    pending.error = "Login failed.";
                    LOGGER.error("failed to login account", e);
                }

                response.completeExceptionally(e);
            } finally {
                CompletableFuture.delayedExecutor(
                        3,
                        TimeUnit.MINUTES
                ).execute(() -> pendingLogins.remove(loginId, pending));
            }
        });

        return response;
    }

    /**
     * Checks the status of a pending login.
     */
    public static LoginStatusResponse getLoginStatus(String loginId) {
        PendingLogin pending = pendingLogins.get(loginId);

        if (pending == null) {
            return LoginStatusResponse.unknown();
        }

        return switch (pending.status) {

            case PENDING -> {
                if (pending.deviceCode == null) {
                    yield LoginStatusResponse.unknown();
                }

                yield LoginStatusResponse.pending(
                        pending.deviceCode.getUserCode(),
                        pending.deviceCode.getVerificationUri(),
                        pending.deviceCode.getDirectVerificationUri()
                );
            }

            case SUCCESS -> LoginStatusResponse.success(pending.accountName);

            case ERROR -> LoginStatusResponse.error(pending.error);
        };
    }

    public static void removeAccount(int index) {
        var accounts = ViaProxy.getSaveManager().accountsSave.getAccounts();

        if (index < 0 || index >= accounts.size()) {
            throw new IllegalArgumentException("Invalid account index");
        }

        ViaProxy.getSaveManager().accountsSave.removeAccount(accounts.get(index));

        ViaProxy.getSaveManager().save();
    }

    public static boolean createOfflineAccount(String name) {
        if (name != null && !name.trim().isEmpty()) {

            ViaProxy.getSaveManager().accountsSave.addAccount(name);

            ViaProxy.getSaveManager().save();

            return true;
        }

        return false;
    }

    public static void selectAccount(int index) {
        var accounts = ViaProxy.getSaveManager().accountsSave.getAccounts();

        if (index < 0 || index >= accounts.size()) {
            throw new IllegalArgumentException("Invalid account index");
        }
        var cfg = ViaProxy.getConfig();

        if (cfg.getAuthMethod() == ViaProxyConfig.AuthMethod.NONE) {
            cfg.setAuthMethod(ViaProxyConfig.AuthMethod.ACCOUNT);
        }

        ViaProxy.getConfig().setAccount(accounts.get(index));
        ViaProxy.getConfig().save();
    }

    private static void loginMicrosoft(
            PendingLogin pending,
            String loginId,
            CompletableFuture<LoginStartResponse> response
    ) throws Exception {

        JavaAuthManager authManager = JavaAuthManager.create(MinecraftAuth.createHttpClient())
                .login(DeviceCodeMsaAuthService::new, (Consumer<MsaDeviceCode>) code -> {
                            pending.deviceCode = code;
                            pending.status = LoginState.PENDING;

                            response.complete(
                                    new LoginStartResponse(
                                            loginId,
                                            code.getUserCode(),
                                            code.getVerificationUri(),
                                            code.getDirectVerificationUri()
                                    )
                            );
                        }
                );

        authManager.getMinecraftProfile().getUpToDate();
        authManager.getMinecraftToken().getUpToDate();

        MicrosoftAccount account = new MicrosoftAccount(authManager);

        ViaProxy.getSaveManager().accountsSave.addAccount(account);

        pending.accountName = account.getName();
    }

    private static void loginBedrock(
            PendingLogin pending,
            String loginId,
            CompletableFuture<LoginStartResponse> response
    ) throws Exception {

        BedrockAuthManager authManager = BedrockAuthManager.create(
                MinecraftAuth.createHttpClient(),
                BEDROCK_VERSION_NAME
        ).login(
                DeviceCodeMsaAuthService::new,
                (Consumer<MsaDeviceCode>) code -> {
                    pending.deviceCode = code;
                    pending.status = LoginState.PENDING;

                    response.complete(
                            new LoginStartResponse(
                                    loginId,
                                    code.getUserCode(),
                                    code.getVerificationUri(),
                                    code.getDirectVerificationUri()
                            )
                    );
                }
        );

        BedrockAccount account = new BedrockAccount(authManager);

        ViaProxy.getSaveManager().accountsSave.addAccount(account);

        pending.accountName = account.getName();
    }

    private static final class PendingLogin {
        volatile LoginState status = LoginState.PENDING;
        volatile MsaDeviceCode deviceCode;
        volatile String accountName;
        volatile String error;
    }
}