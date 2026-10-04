package dev.l4z.viaproxyweb.web.routes;

import io.javalin.config.JavalinConfig;
import io.javalin.http.Context;

import dev.l4z.viaproxyweb.dto.GenericResponse;
import dev.l4z.viaproxyweb.logging.ViaWebLogger;
import dev.l4z.viaproxyweb.service.AccountService;

import org.apache.logging.log4j.Logger;

import java.util.Map;

public class AccountRoutes {

    private static final Logger LOGGER = ViaWebLogger.getLogger(AccountRoutes.class);

    public void register(JavalinConfig config) {

        config.routes.get("/api/accounts", this::getAccounts); // lists all accounts
        config.routes.post(
                "/api/accounts/login/start", this::postDeviceLogin); // Start device code login
        config.routes.get("/api/accounts/login/status", this::getLoginStatus); // Check login status (unknown, pending, success, error)
        config.routes.delete("/api/accounts/{index}", this::deleteAccount); // delete an account
        config.routes.post("/api/accounts/offline", this::addOffline); // Add an offline account
        config.routes.post("/api/accounts/select/{index}", this::selectAccount);
    }

    private void getAccounts(Context ctx) {
        ctx.json(Map.of("accounts", AccountService.getAccounts()));
    }

    private void postDeviceLogin(Context ctx) {
        try {
            String typeParam = ctx.queryParam("type"); // "microsoft" or "bedrock"
            if (typeParam == null || typeParam.isBlank()) {
                ctx.status(400).json(new GenericResponse("error", "Missing type (microsoft or bedrock)"));
                return;
            }

            AccountService.AccountType type =
                    switch (typeParam.toLowerCase()) {
                        case "microsoft" -> AccountService.AccountType.MICROSOFT;
                        case "bedrock" -> AccountService.AccountType.BEDROCK;
                        default -> throw new IllegalArgumentException("Invalid type: " + typeParam);
                    };

            ctx.future(() ->
                    AccountService.startDeviceLogin(type)
                            .thenAccept(ctx::json)
                            .exceptionally(e -> {
                                Throwable cause = e.getCause() != null ? e.getCause() : e;

                                LOGGER.error("failed to start login", cause);

                                ctx.status(500).json(
                                        new GenericResponse(
                                                "error",
                                                "Failed to start login"
                                        )
                                );

                                return null;
                            })
            );

        } catch (IllegalArgumentException e) {
            ctx.status(400).json(new GenericResponse("error", e.getMessage()));
        } catch (Exception e) {
            ctx.status(400).json(new GenericResponse("error", "Failed to start login"));
            LOGGER.error("failed to start login: ", e);
        }
    }

    private void getLoginStatus(Context ctx) {
        String loginId = ctx.queryParam("loginId");
        if (loginId == null || loginId.isBlank()) {
            ctx.status(400).json(new GenericResponse("error", "missing loginId"));
            return;
        }
        ctx.json(AccountService.getLoginStatus(loginId));
    }

    private void addOffline(Context ctx) {
        if (!AccountService.createOfflineAccount(ctx.body())) {
            ctx.json(new GenericResponse("error", "missing or invalid username"));
        }
        ctx.json(new GenericResponse("success", "added offline account"));
    }

    private void selectAccount(Context ctx) {
        try {
            int id = Integer.parseInt(ctx.pathParam("index"));
            AccountService.selectAccount(id);
            ctx.json(new GenericResponse("success", "selected account"));
        } catch (IllegalArgumentException e) {
            ctx.status(400).json(new GenericResponse("error", e.getMessage()));
        } catch (Exception e) {
            ctx.status(400).json(new GenericResponse("error", "failed to select an account."));
            LOGGER.error("failed to select account:", e);
        }
    }

    private void deleteAccount(Context ctx) {
        try {
            int id = Integer.parseInt(ctx.pathParam("index"));
            AccountService.removeAccount(id);
            ctx.json(new GenericResponse("success", "deleted account."));
        } catch (IllegalArgumentException e) {
            ctx.status(400).json(new GenericResponse("error", e.getMessage()));
        } catch (Exception e) {
            ctx.status(400).json(Map.of("error", "could not delete account."));
            LOGGER.error("failed to delete account:", e);
        }
    }
}