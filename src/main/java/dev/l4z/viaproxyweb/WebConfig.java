package dev.l4z.viaproxyweb;

import net.lenni0451.optconfig.annotations.Description;
import net.lenni0451.optconfig.annotations.OptConfig;
import net.lenni0451.optconfig.annotations.Option;

@OptConfig
public class WebConfig {

    @Option("address")
    @Description("Address the web backend binds to")
    public static String address = "127.0.0.1";

    @Option("port")
    @Description("Port the web backend binds to")
    public static int port = 8080;
}
