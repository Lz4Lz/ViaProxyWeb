import { AnsiUp } from 'https://cdn.jsdelivr.net/npm/ansi_up@6.0.2/+esm';

export default () => ({
    
    async loadProxyStatus() {
        const res = await fetch('/api/proxy/status');

        if (!res.ok) {
            throw new Error(`HTTP ${res.status}`);
        }

        this.status = await res.json();
        this.syncSelectedFromStatus();
    },

    async toggleProxy() {
        await this.saveConfig();
        
        try {
            const endpoint = this.status.running ? '/api/proxy/stop' : '/api/proxy/start';

            const res = await fetch(endpoint, {
                method: 'POST',
            });

            if (!res.ok) {
                throw new Error(`HTTP ${res.status}`);
            }

            await this.loadConfig();
            await this.loadProxyStatus();
        } catch (err) {
            console.error(err);
            alert('Something went wrong.');
        }
    },

    connectLogs() {
        if (this._reconnecting) return;

        const protocol = location.protocol === 'https:' ? 'wss:' : 'ws:';

        this.ws = new WebSocket(
            `${protocol}//${location.host}/ws/logs`
        );

        this.ws.onmessage = (event) => {
            this.appendLog(event.data);
        };

        this.ws.onopen = () => {
            console.log('[ViaProxy] Log WebSocket connected');
            this._reconnecting = false;
        };

        this.ws.onclose = (event) => {
            console.log('[ViaProxy] Log WebSocket closed', event.code);

            if (!this._reconnecting) {
                this._reconnecting = true;

                setTimeout(() => {
                    this.connectLogs();
                }, 2000);
            }
        };
    },
    
    appendLog(line) {
        if (!this._ansiUp) {
            this._ansiUp = new AnsiUp();
            this._ansiUp.use_classes = false;
        }

        const html = this._ansiUp.ansi_to_html(String(line));
        this.logs.push(html);

        if (this.logs.length > this.maxLogLines) {
            this.logs.shift();
        }

        this.$nextTick(() => {
            const el = document.getElementById('logs');
            if (el) el.scrollTop = el.scrollHeight;
        });
    },
});