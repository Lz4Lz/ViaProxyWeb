package dev.l4z.viaproxyweb.logging;

import org.apache.logging.log4j.Logger;

public final class ViaWebLogger {
    private ViaWebLogger() {
    }

    public static Logger getLogger(Class<?> clazz) {
        org.apache.logging.log4j.core.Logger viaLogger = (org.apache.logging.log4j.core.Logger)
                net.raphimc.viaproxy.util.logging.Logger.LOGGER;

        return viaLogger.getContext().getLogger(clazz.getName());
    }
}
