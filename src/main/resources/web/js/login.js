export default () => ({
    async startDeviceLogin(type) {
        try {
            const res = await fetch(
                `/api/accounts/login/start?type=${encodeURIComponent(type)}`,
                { method: 'POST' }
            );

            if (!res.ok) {
                throw new Error('Failed to start login');
            }

            const data = await res.json();

            this.loginModal = {
                open: true,
                userCode: data.userCode,
                verificationUri: data.verificationUri,
                directUri: data.directVerificationUri || data.verificationUri,
                loginId: data.loginId,
                status: 'pending',
                error: null,
                pollTimer: null,
            };

            this.pollLoginStatus();
        } catch (err) {
            console.error(err);
            alert('Failed to start device login');
        }
    },

    pollLoginStatus() {
        if (!this.loginModal.loginId) {
            return;
        }

        const poll = async () => {
            if (!this.loginModal.open) {
                return;
            }

            try {
                const res = await fetch(
                    `/api/accounts/login/status?loginId=${encodeURIComponent(this.loginModal.loginId)}`
                );

                if (!res.ok) {
                    throw new Error(`HTTP ${res.status}`);
                }

                const data = await res.json();


                if (data.status === 'SUCCESS') {
                    this.loginModal.status = 'success';

                    await this.loadAccounts();

                    setTimeout(() => {
                        this.closeLoginModal();
                    }, 1500);

                    return;

                } else if (data.status === 'ERROR') {
                    this.loginModal.status = 'error';
                    this.loginModal.error = data.error || 'Login failed';
                    return;

                } else if (data.status === 'UNKNOWN') {
                    this.loginModal.status = 'error';
                    this.loginModal.error = 'Login session expired or was not found';
                    return;
                }
                
                
                this.loginModal.pollTimer = setTimeout(
                    poll,
                    2000
                );

            } catch (err) {
                console.error('Poll error', err);

                if (!this.loginModal.open) {
                    return;
                }

                this.loginModal.pollTimer = setTimeout(
                    poll,
                    2000
                );
            }
        };

        poll();
    },

    closeLoginModal() {
        if (this.loginModal.pollTimer) {
            clearTimeout(this.loginModal.pollTimer);
        }

        this.loginModal.pollTimer = null;
        this.loginModal.open = false;
    },
});