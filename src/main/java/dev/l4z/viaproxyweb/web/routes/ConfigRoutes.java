package dev.l4z.viaproxyweb.web.routes;

import io.javalin.config.JavalinConfig;

import dev.l4z.viaproxyweb.dto.GenericResponse;
import dev.l4z.viaproxyweb.dto.config.ConfigRequest;
import dev.l4z.viaproxyweb.logging.ViaWebLogger;
import dev.l4z.viaproxyweb.service.ConfigService;

import org.apache.logging.log4j.Logger;

import java.util.Map;

public class ConfigRoutes {

    private static final Logger LOGGER = ViaWebLogger.getLogger(ConfigRoutes.class);

    public void register(JavalinConfig config) {

        config.routes.get(
                "/api/config",
                ctx -> ctx.json(ConfigService.getConfig())
        );

        config.routes.post("/api/config", ctx -> {
            try {
                ConfigRequest request = ctx.bodyAsClass(ConfigRequest.class);

                ConfigService.applyConfig(request);

                ctx.json(Map.of("status", "success"));

            } catch (IllegalArgumentException e) {

                ctx.status(400).json(new GenericResponse(
                        "error", e.getMessage()
                ));

            } catch (Exception e) {

                ctx.status(500).json(new GenericResponse(
                        "error",
                        "failed to save config"
                ));

                LOGGER.error("failed to save config: ", e);
            }
        });

        config.routes.get(
                "/api/config/versions",
                ctx -> ctx.json(Map.of(
                        "versions",
                        ConfigService.getAvailableVersions()
                ))
        );

        config.routes.get(
                "/api/config/auth-methods",
                ctx -> ctx.json(Map.of(
                        "authMethods",
                        ConfigService.getAuthMethods()
                ))
        );
    }
}