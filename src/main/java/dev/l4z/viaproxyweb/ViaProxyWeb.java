package dev.l4z.viaproxyweb;

import dev.l4z.viaproxyweb.logging.ViaWebAppender;
import dev.l4z.viaproxyweb.logging.ViaWebLogger;
import dev.l4z.viaproxyweb.web.WebServer;

import net.lenni0451.optconfig.ConfigLoader;
import net.lenni0451.optconfig.provider.ConfigProvider;
import net.raphimc.viaproxy.plugins.ViaProxyPlugin;

import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;

public class ViaProxyWeb extends ViaProxyPlugin {

    private WebServer webServer;

    org.apache.logging.log4j.core.Logger viaCore = (org.apache.logging.log4j.core.Logger) net.raphimc.viaproxy.util.logging.Logger.LOGGER;

    private static final Logger LOGGER = ViaWebLogger.getLogger(ViaProxyWeb.class);

    @Override
    public void onEnable() {

        ConfigLoader<WebConfig> configLoader = new ConfigLoader<>(WebConfig.class);

        try {
            configLoader.loadStatic(ConfigProvider.file(new File(getDataFolder(), "config.yml")));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        ViaWebAppender.install(viaCore);

        webServer = new WebServer(this, WebConfig.address, WebConfig.port);
        webServer.start();
        LOGGER.info("ViaProxyWeb listening on {}:{}", WebConfig.address, WebConfig.port);
    }

    @Override
    public void onDisable() {
        if (webServer != null) {
            webServer.stop();
        }

        ViaWebAppender.uninstall(viaCore);
    }
}
