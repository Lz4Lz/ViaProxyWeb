export default () => ({

    async loadRealms() {
        if (!this.status.account) {
            this.realms = [];
            this.realmsMeta = {};
            return;
        }

        this.realmsLoading = true;
        this.realmsError = null;

        try {
            const q = this.realmsSnapshot ? '?snapshot=true' : '';
            const res = await fetch('/api/realms' + q);
            const data = await res.json();

            if (!res.ok) {
                if (data.error === 'unsupported_account') {
                    this.realmsError = 'Selected account does not support Realms (need Microsoft or Bedrock).';
                } else if (data.error === 'no_account') {
                    this.realmsError = 'No account selected.';
                } else {
                    this.realmsError = 'Failed to load Realms.';
                }

                this.realms = [];
                this.realmsMeta = {};
                return;
            }

            this.realmsMeta = data;
            this.realms = (data.realms || []).map(r => ({
                ...r,
                joining: false
            }));
        } catch (err) {
            console.error(err);
            this.realmsError = 'Failed to load Realms.';
            this.realms = [];
            this.realmsMeta = {};
        } finally {
            this.realmsLoading = false;
        }
    },

    async joinRealm(realm) {
        
        if (!realm || realm.joining) return;

        realm.joining = true;
        this.realmsError = null;

        try {
            const q = new URLSearchParams({
                autoStart: 'true',
                snapshot: this.realmsSnapshot ? 'true' : 'false',
            });
            
            const res = await fetch(`/api/realms/${encodeURIComponent(realm.id)}/join?${q}`, {
                method: 'POST',
            });

            const data = await res.json();

            if (data.status === 'tos_required') {
                this.pendingJoinRealmId = realm.id;
                this.$refs.realmsTosDialog.showModal();
                return;
            }

            if (!res.ok || data.error) {
                throw new Error(data.error || 'Join failed');
            }

            await this.loadConfig();
            await this.loadProxyStatus();
            this.tab = 'status';

        } catch (err) {
            console.error(err);
            this.realmsError = err.message || 'Failed to join Realm';
            alert(this.realmsError);
            
        } finally {
            realm.joining = false;
        }
    },

    async acceptRealmsTos(snapshot) {
        try {
            const res = await fetch(`/api/realms/tos/accept?snapshot=${snapshot}`, { method: 'POST' });
            const data = await res.json();
            if (!res.ok) throw new Error(data.error || 'TOS accept failed');

            this.$refs.realmsTosDialog.close();

            const id = this.pendingJoinRealmId;
            this.pendingJoinRealmId = null;
            const realm = this.realms.find(r => r.id === id);
            if (realm) await this.joinRealm(realm);
            
        } catch (err) {
            console.error(err);
            alert(err.message || 'Failed to accept TOS');
        }
    },
});