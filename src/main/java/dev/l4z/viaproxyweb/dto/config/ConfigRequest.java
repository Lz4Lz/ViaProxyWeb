package dev.l4z.viaproxyweb.dto.config;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ConfigRequest(

        @JsonProperty("proxy_online_mode")
        Boolean proxyOnlineMode,

        @JsonProperty("chat_signing")
        Boolean chatSigning,

        @JsonProperty("ignore_packet_translation_errors")
        Boolean ignorePacketTranslationErrors,

        @JsonProperty("allow_beta_pinging")
        Boolean allowBetaPinging,

        @JsonProperty("simple_voice_chat_support")
        Boolean simpleVoiceChatSupport,

        @JsonProperty("fake_accept_resource_packs")
        Boolean fakeAcceptResourcePacks,

        @JsonProperty("betacraft_auth")
        Boolean betacraftAuth,

        @JsonProperty("legacy_skin_loading")
        Boolean legacySkinLoading,

        @JsonProperty("server_version")
        String serverVersion,

        @JsonProperty("auth_method")
        String authMethod,

        @JsonProperty("server_address")
        String serverAddress,

        @JsonProperty("bind_address")
        String bindAddress,

        String proxy
) {
}