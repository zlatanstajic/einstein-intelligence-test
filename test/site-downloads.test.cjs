const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const path = require('node:path');
const source = fs.readFileSync(path.join(__dirname, '../site/downloads.js'), 'utf8');
const repo = 'https://github.com/zlatanstajic/einstein-intelligence-test';

async function render(response) {
  const nodes = Object.fromEntries(['release-status', 'windows-download', 'jar-download']
    .map(id => [id, { textContent: '', href: `${repo}/releases` }]));
  vm.runInNewContext(source, {
    document: { getElementById: id => nodes[id] },
    fetch: async () => {
      if (response instanceof Error) throw response;
      return response;
    },
    AbortController, URL, setTimeout, clearTimeout,
  });
  await new Promise(resolve => setImmediate(resolve));
  return nodes;
}
const release = assets => ({ ok: true, status: 200, json: async () => ({ tag_name: 'v1.0.0', assets }) });
const asset = name => ({ name, browser_download_url: `${repo}/releases/download/v1.0.0/${encodeURIComponent(name)}` });

test('published MSI and JAR become direct download links', async () => {
  const nodes = await render(release([asset('Einstein Intelligence Test.msi'), asset('einstein-intelligence-test.jar')]));
  assert.match(nodes['windows-download'].href, /releases\/download\/v1.0.0\//);
  assert.equal(nodes['jar-download'].href, asset('einstein-intelligence-test.jar').browser_download_url);
  assert.match(nodes['release-status'].textContent, /v1.0.0/);
});
test('an absent platform asset keeps its release-page fallback', async () => {
  const nodes = await render(release([asset('einstein-intelligence-test.jar')]));
  assert.equal(nodes['windows-download'].href, `${repo}/releases`);
  assert.match(nodes['jar-download'].textContent, /Download portable/);
});
test('no published release is explained without inventing a download', async () => {
  const nodes = await render({ status: 404, ok: false });
  assert.match(nodes['release-status'].textContent, /first release/);
  assert.equal(nodes['jar-download'].href, `${repo}/releases`);
});
test('network and rate-limit failures retain usable fallback links', async () => {
  for (const response of [new Error('offline'), { status: 403, ok: false }]) {
    const nodes = await render(response);
    assert.match(nodes['release-status'].textContent, /unavailable/);
    assert.equal(nodes['windows-download'].href, `${repo}/releases`);
  }
});
test('untrusted or missing asset URLs are not offered as downloads', async () => {
  for (const url of ['https://example.com/file.msi', `${repo}/issues/file.msi`]) {
    const nodes = await render(release([{ name: 'file.msi', browser_download_url: url }]));
    assert.equal(nodes['windows-download'].href, `${repo}/releases`);
    assert.match(nodes['release-status'].textContent, /No matching packages/);
  }
});
test('malformed release data leaves fallback links intact', async () => {
  const nodes = await render({ ok: true, json: async () => null });
  assert.match(nodes['release-status'].textContent, /unavailable/);
  assert.equal(nodes['jar-download'].href, `${repo}/releases`);
});
