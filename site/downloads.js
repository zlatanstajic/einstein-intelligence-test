/* Resolve only published release assets; fallback links also work without JavaScript. */
(() => {
  const repository = 'zlatanstajic/einstein-intelligence-test';
  const status = document.getElementById('release-status');
  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), 8000);

  async function loadDownloads() {
    try {
      const response = await fetch(`https://api.github.com/repos/${repository}/releases/latest`, {
        headers: { Accept: 'application/vnd.github+json' },
        signal: controller.signal,
      });
      if (response.status === 404) {
        status.textContent = 'The first release is on its way. Build artifacts and source are available below.';
        return;
      }
      if (!response.ok) throw new Error('Release lookup failed');
      const release = await response.json();
      const assets = Array.isArray(release.assets) ? release.assets : [];
      const choices = [
        ['windows-download', asset => /\.msi$/i.test(asset.name), 'Download for Windows'],
        ['jar-download', asset => asset.name === 'einstein-intelligence-test.jar', 'Download portable JAR'],
      ];
      let available = 0;
      for (const [id, matches, label] of choices) {
        const asset = assets.find(matches);
        if (!asset) continue;
        const url = new URL(asset.browser_download_url);
        if (url.origin !== 'https://github.com' || !url.pathname.startsWith(`/${repository}/releases/download/`)) continue;
        const link = document.getElementById(id);
        link.href = url.href;
        link.textContent = `${label} ↓`;
        available++;
      }
      status.textContent = available
        ? `Latest release: ${release.tag_name}. Available packages are linked below.`
        : 'No matching packages in the latest release. Browse releases or build artifacts below.';
    } catch {
      status.textContent = 'Release information is unavailable. Use the release links or build artifacts below.';
    } finally {
      clearTimeout(timeout);
    }
  }
  loadDownloads();
})();
