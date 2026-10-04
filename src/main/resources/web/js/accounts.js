export default () => ({
    async selectAccount(index) {
        this.selectedAccount = index;

        try {
            const res = await fetch(`/api/accounts/select/${index}`, {
                method: 'POST'
            });

            if (!res.ok) {
                throw new Error('Select failed');
            }

            const account = this.accounts.find(a => a.index === index);
            this.status.account = account ? account.name : '';
            
            if (this.config.auth_method === 'NONE') {
                await this.loadConfig();
            }

        } catch (err) {
            console.error(err);
            alert('Failed to select account');
        }
    },

    async loadAccounts() {
        try {
            const res = await fetch('/api/accounts');

            if (!res.ok) {
                throw new Error(`HTTP ${res.status}`);
            }

            const data = await res.json();

            this.accounts = data.accounts.map(account => ({
                ...account,
                type: account.type.replace('Account', ''),
            }));

            this.syncSelectedFromStatus();
        } catch (err) {
            console.error('Failed to load accounts:', err);
        }
    },

    syncSelectedFromStatus() {
        if (!this.status.account) {
            this.selectedAccount = null;
            return;
        }

        this.selectedAccount = this.status.accountIndex ?? null;
    },

    offlineDialog() {
        this.$refs.makeAccountDialog.showModal();
    },

    async createOffline(username) {
        if (username === '') {
            return;
        }

        try {
            const res = await fetch('/api/accounts/offline', {
                method: 'POST',
                body: username,
            });

            this.offlineUsername = '';

            if (!res.ok) {
                throw new Error(`HTTP ${res.status}`);
            }

            const data = await res.json();

            if (data.status !== 'success') {
                alert('Something went wrong.');
                console.log(data);
                return;
            }

            await this.loadAccounts();
            this.$refs.makeAccountDialog.close();
        } catch (err) {
            console.log(err);
            alert(err);
        }
    },

    confirmDelete(index) {
        this.selectedAccount = index;
        this.$refs.deleteDialog.showModal();
    },

    async deleteAccount() {
        if (this.selectedAccount === null) {
            return;
        }

        const index = this.selectedAccount;
        

        try {
            const res = await fetch(
                `/api/accounts/${index}`,
                { method: 'DELETE' }
            );

            if (!res.ok) {
                throw new Error('Delete failed');
            }
            
            await this.loadAccounts();

            if (this.accounts.length > 0) {
                await this.selectAccount(this.accounts.length - 1);
            } else {
                this.selectedAccount = null;
                this.status.account = '';
            }

        } catch (err) {
            console.error(err);
            alert('Failed to delete account');
        } finally {
            this.$refs.deleteDialog.close();
        }
    },
});