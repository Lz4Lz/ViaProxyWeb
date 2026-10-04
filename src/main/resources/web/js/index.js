import Alpine from 'https://cdn.jsdelivr.net/npm/alpinejs@3.14.8/dist/module.esm.js';

import state from './state.js';
import status from './status.js';
import accounts from './accounts.js';
import configuration from './configuration.js';
import login from './login.js';
import realms from './realms.js';

import { loadViews } from './view.js';

const index = () => ({
    ...state(),
    ...status(),
    ...accounts(),
    ...configuration(),
    ...login(),
    ...realms(),
});

window.Alpine = Alpine;

Alpine.data('index', index);

await loadViews();

Alpine.start();
