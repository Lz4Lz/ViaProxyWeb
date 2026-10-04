export async function loadViews() {
    const views = document.querySelectorAll('[data-view]');

    await Promise.all(
        [...views].map(async (element) => {
            const name = element.dataset.view;

            const res = await fetch(`/views/${name}.html`);

            if (!res.ok) {
                throw new Error(
                    `Failed to load view "${name}": HTTP ${response.status}`
                );
            }
            element.innerHTML = await res.text();
        })
    )
}