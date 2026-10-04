export default () => ({
    async loadConfig() {
        const [cfgRes, verRes, authRes] = await Promise.all([
            fetch('/api/config'),
            fetch('/api/config/versions'),
            fetch('/api/config/auth-methods'),
        ]);

        this.config = await cfgRes.json();
        this.versions = (await verRes.json()).versions;
        this.authMethods = (await authRes.json()).authMethods;
    },

    async saveConfig() {
        
        try {
            const res = await fetch('/api/config', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(this.config),
            });

            if (!res.ok) {
                throw new Error(`HTTP ${res.status}`);
            }

            const data = await res.json();

            if (data.status !== 'success') {
                alert("hmm that didn't succeed.");
                return;
            }

            await this.loadConfig();
            await this.loadProxyStatus();
        } catch (err) {
            console.log(err);
            alert('Something went wrong.');
        }
    },
});