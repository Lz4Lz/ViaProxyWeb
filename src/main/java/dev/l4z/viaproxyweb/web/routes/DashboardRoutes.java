package dev.l4z.viaproxyweb.web.routes;

import io.javalin.config.JavalinConfig;
import io.javalin.websocket.WsConfig;
import dev.l4z.viaproxyweb.dto.GenericResponse;
import dev.l4z.viaproxyweb.logging.LogBroadcast;
import dev.l4z.viaproxyweb.logging.ViaWebLogger;
import dev.l4z.viaproxyweb.service.ProxyService;
import org.apache.logging.log4j.Logger;

import java.util.function.Consumer;

public class DashboardRoutes {

    private static final Logger LOGGER = ViaWebLogger.getLogger(DashboardRoutes.class);

    public void register(JavalinConfig config) {
        config.routes.post("/api/proxy/start", ctx -> {
            try {
                ctx.json(ProxyService.start());
            } catch (Exception e) {
                ctx.status(500).json(new GenericResponse("error", "failed to start proxy"));
                LOGGER.error("failed to start proxy", e);
            }
        });

        config.routes.post("/api/proxy/stop", ctx -> {
            try {
                ctx.json(ProxyService.stop());
            } catch (Exception e) {
                ctx.status(500).json(new GenericResponse("error", "failed to stop proxy"));
                LOGGER.error("failed to stop proxy: ", e);
            }
        });

        config.routes.get("/api/proxy/status", ctx -> {
            try {
                ctx.json(ProxyService.status());
            } catch (Exception e) {
                ctx.status(500).json(new GenericResponse("error", "failed to get proxy status."));
                LOGGER.info("failed to get proxy status: ", e);
            }
        });

        config.routes.ws("/ws/logs", this::LogWebSocket);

        config.routes.get("/", ctx -> ctx.redirect("/index.html"));
    }

    private void LogWebSocket(WsConfig ws) {
        ws.onConnect(ctx -> {
            ctx.enableAutomaticPings();

            for (String line : LogBroadcast.recent()) {
                ctx.send(line);
            }

            Consumer<String> listener = line -> {
                try {
                    if (ctx.session.isOpen()) {
                        ctx.send(line);
                    }
                } catch (Exception e) {
                    LOGGER.debug("Failed to send log update to WebSocket client", e);
                }
            };

            LogBroadcast.subscribe(listener);
            ctx.attribute("logListener", listener);

        });

        ws.onClose(ctx -> {
            Consumer<String> listener = ctx.attribute("logListener");
            if (listener != null) {
                LogBroadcast.unsubscribe(listener);
            }

        });

        ws.onError(ctx -> LOGGER.error("WebSocket error", ctx.error()));
    }
}