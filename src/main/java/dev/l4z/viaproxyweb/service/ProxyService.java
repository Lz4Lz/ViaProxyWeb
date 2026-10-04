package dev.l4z.viaproxyweb.service;

import dev.l4z.viaproxyweb.dto.proxy.ProxyResponse;
import dev.l4z.viaproxyweb.dto.proxy.ProxyStatusResponse;

import net.raphimc.viaproxy.ViaProxy;

public class ProxyService {

    public static synchronized ProxyResponse start() {
        if (isRunning()) {
            return new ProxyResponse(
                    "already_running",
                    null
            );
        }

        ViaProxy.getConfig().save();
        ViaProxy.getSaveManager().save();

        ViaProxy.startProxy();

        return new ProxyResponse(
                "started",
                ViaProxy.getConfig().getBindAddress().toString()
        );

    }

    public static synchronized ProxyResponse stop() {
        if (!isRunning()) {
            return new ProxyResponse(
                    "already_stopped",
                    null
            );
        }

        ViaProxy.stopProxy();

        return new ProxyResponse(
                "stopped",
                null
        );
    }

    public static boolean isRunning() {
        return ViaProxy.getCurrentProxyServer() != null;
    }

    public static ProxyStatusResponse status() {
        var config = ViaProxy.getConfig();

        var account = config != null ? config.getAccount() : null;
        var target = config != null ? config.getTargetAddress() : null;
        var clients = ViaProxy.getConnectedClients();

        int accountIndex = -1;

        if (account != null) {
            var accounts = ViaProxy.getSaveManager().accountsSave.getAccounts();
            accountIndex = accounts.indexOf(account);
        }

        return new ProxyStatusResponse(
                isRunning(),
                clients != null ? clients.size() : 0,
                account != null ? account.getName() : "",
                accountIndex,
                target != null ? target.toString() : ""
        );
    }

}
