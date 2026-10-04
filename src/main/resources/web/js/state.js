export default () => ({
    tab: 'status',

    selectedAccount: null,
    offlineUsername: '',

    accounts: [],

    versions: [],
    authMethods: [],

    logs: [],
    maxLogLines: 1000,
    ws: null,

    loginModal: {
        open: false,
        userCode: '',
        verificationUri: '',
        directUri: '',
        loginId: null,
        status: 'pending',
        error: null,
        pollTimer: null,
    },

    config: {
        bind_address: '0.0.0.0:25568',
        proxy: '',
        legacy_skin_loading: false,
        proxy_online_mode: false,
        chat_signing: true,
        ignore_packet_translation_errors: false,
        allow_beta_pinging: false,
        simple_voice_chat_support: false,
        fake_accept_resource_packs: false,

        server_address: '',
        server_version: '',
        auth_method: 'NONE',
        betacraft_auth: false,
    },

    status: {
        running: false,
        clients: 0,
        account: '',
        accountIndex: '',
        target: '',
    },

    realms: [],
    realmsMeta: {},      // { type, compatible, version, account }
    realmsLoading: false,
    realmsError: null,
    realmsSnapshot: false,
    pendingJoinRealmId: null,

    init() {
        this.connectLogs();
        this.loadAccounts();
        this.loadConfig();
        this.loadProxyStatus();
        setInterval(() => {
            this.loadProxyStatus();
        }, 10000);
    },
});