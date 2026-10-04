package dev.l4z.viaproxyweb.web.routes;

import dev.l4z.viaproxyweb.exception.NoAccountSelectedException;
import dev.l4z.viaproxyweb.exception.UnsupportedAccountException;
import dev.l4z.viaproxyweb.logging.ViaWebLogger;
import dev.l4z.viaproxyweb.service.RealmsService;

import io.javalin.config.JavalinConfig;

import org.apache.logging.log4j.Logger;

import java.util.Map;

public class RealmsRoutes {

    private static final Logger LOGGER = ViaWebLogger.getLogger(RealmsRoutes.class);

    public void register(JavalinConfig config) {

        config.routes.get(
                "/api/realms",
                ctx -> {
                    boolean snapshot = "true".equalsIgnoreCase(ctx.queryParam("snapshot"));

                    ctx.future(() ->
                            RealmsService.listRealms(snapshot)
                                    .thenAccept(ctx::json)
                                    .exceptionally(e -> {
                                        Throwable cause = e.getCause() != null ? e.getCause() : e;

                                        if (cause instanceof NoAccountSelectedException) {
                                            ctx.status(400).json(Map.of("error", "no_account"));
                                            return null;
                                        }

                                        if (cause instanceof UnsupportedAccountException) {
                                            ctx.status(400).json(Map.of("error", "unsupported_account"));
                                            return null;
                                        }

                                        ctx.status(500).json(Map.of("error", "failed_to_list_realms"));
                                        LOGGER.error("could not list realms", cause);
                                        return null;
                                    })
                    );
                }
        );

        config.routes.post(
                "/api/realms/{id}/join",
                ctx -> {
                    boolean snapshot = "true".equalsIgnoreCase(ctx.queryParam("snapshot"));
                    boolean autoStart = !"false".equalsIgnoreCase(ctx.queryParam("autoStart"));
                    ctx.future(() ->
                            RealmsService.joinRealm(ctx.pathParam("id"), snapshot, autoStart)
                                    .thenAccept(ctx::json)
                                    .exceptionally(e -> {
                                        Throwable cause = e.getCause() != null ? e.getCause() : e;

                                        if (cause instanceof NoAccountSelectedException) {
                                            ctx.status(400).json(Map.of("error", "no_account"));
                                            return null;
                                        }

                                        if (cause instanceof UnsupportedAccountException) {
                                            ctx.status(400).json(Map.of("error", "unsupported_account"));
                                            return null;
                                        }
                                        
                                        LOGGER.error("Failed to join realm", cause);
                                        ctx.status(500).json(Map.of("error", "could not join realm"));
                                        return null;

                                    })
                    );
                });

        config.routes.post(
                "/api/realms/tos/accept",
                ctx -> {

                    boolean snapshot = Boolean.parseBoolean(ctx.queryParam("snapshot"));

                    ctx.future(() ->
                            RealmsService.acceptTos(snapshot)
                                    .thenAccept(ctx::json)
                                    .exceptionally(e -> {
                                        Throwable cause = e.getCause() != null ? e.getCause() : e;

                                        if (cause instanceof IllegalStateException) {
                                            ctx.status(400).json(
                                                    Map.of("error", cause.getMessage())
                                            );
                                            return null;
                                        }

                                        LOGGER.error("failed to accept realms TOS", cause);

                                        ctx.status(400).json(
                                                Map.of("error", "failed to accept realms TOS")
                                        );

                                        return null;
                                    })
                    );

                });
    }
}
