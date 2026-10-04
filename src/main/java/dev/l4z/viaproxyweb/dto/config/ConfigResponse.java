package dev.l4z.viaproxyweb.dto.config;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ConfigResponse(

        @JsonProperty("bind_address")
        String bindAddress,

        String proxy,

        @JsonProperty("legacy_skin_loading")
        boolean legacySkinLoading,

        @JsonProperty("proxy_online_mode")
        boolean proxyOnlineMode,

        @JsonProperty("chat_signing")
        boolean chatSigning,

        @JsonProperty("ignore_packet_translation_errors")
        boolean ignorePacketTranslationErrors,

        @JsonProperty("allow_beta_pinging")
        boolean allowBetaPinging,

        @JsonProperty("simple_voice_chat_support")
        boolean simpleVoiceChatSupport,

        @JsonProperty("fake_accept_resource_packs")
        boolean fakeAcceptResourcePacks,

        @JsonProperty("server_address")
        String serverAddress,

        @JsonProperty("server_version")
        String serverVersion,

        @JsonProperty("auth_method")
        String authMethod,

        @JsonProperty("betacraft_auth")
        boolean betacraftAuth
) {
}