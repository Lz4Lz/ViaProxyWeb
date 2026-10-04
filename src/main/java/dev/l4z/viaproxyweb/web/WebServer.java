package dev.l4z.viaproxyweb.web;

import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;

import dev.l4z.viaproxyweb.ViaProxyWeb;
import dev.l4z.viaproxyweb.service.AccountService;
import dev.l4z.viaproxyweb.web.routes.AccountRoutes;
import dev.l4z.viaproxyweb.web.routes.ConfigRoutes;
import dev.l4z.viaproxyweb.web.routes.DashboardRoutes;
import dev.l4z.viaproxyweb.web.routes.RealmsRoutes;

public class WebServer {

    private final ViaProxyWeb plugin;

    private final int port;
    private final String address;

    private Javalin app;

    public WebServer(ViaProxyWeb plugin, String address, int port) {
        this.plugin = plugin;
        this.port = port;
        this.address = address;
    }

    public void start() {
        ClassLoader originalLoader = Thread.currentThread().getContextClassLoader();

        try {
            Thread.currentThread().setContextClassLoader(plugin.getClass().getClassLoader());

            app = Javalin.create(config -> {

                config.startup.showJavalinBanner = false;
                config.events.serverStopping(AccountService::shutdown);

                config.staticFiles.add(staticFiles -> {
                    staticFiles.hostedPath = "/";
                    staticFiles.directory = "/web";
                    staticFiles.location = Location.CLASSPATH;
                });

                new DashboardRoutes().register(config);
                new AccountRoutes().register(config);
                new ConfigRoutes().register(config);
                new RealmsRoutes().register(config);

            }).start(address, port);

        } finally {
            Thread.currentThread().setContextClassLoader(originalLoader);
        }
    }

    public void stop() {
        if (app != null) {
            app.stop();
        }
    }
}
