const CACHE = 'frota-v2';
const ASSETS = ['/', '/index.html', '/style.css', '/app.js', '/manifest.json'];

self.addEventListener('install', e => {
  e.waitUntil(caches.open(CACHE).then(c => c.addAll(ASSETS)));
  self.skipWaiting();
});

self.addEventListener('activate', e => {
  e.waitUntil(caches.keys().then(keys =>
    Promise.all(keys.filter(k => k !== CACHE).map(k => caches.delete(k)))
  ));
  self.clients.claim();
});

self.addEventListener('fetch', e => {
  if (e.request.url.includes('/api/')) return;
  e.respondWith(
    caches.match(e.request).then(cached => cached || fetch(e.request))
  );
});

// ── IndexedDB helpers ─────────────────────────────────────────
function abrirDB() {
  return new Promise((resolve, reject) => {
    const req = indexedDB.open('frota-offline', 1);
    req.onupgradeneeded = e =>
      e.target.result.createObjectStore('pendentes', { autoIncrement: true });
    req.onsuccess = e => resolve(e.target.result);
    req.onerror   = e => reject(e.target.error);
  });
}

async function enviarPendentes() {
  const db = await abrirDB();

  const [registros, chaves] = await Promise.all([
    new Promise((res, rej) => {
      const req = db.transaction('pendentes').objectStore('pendentes').getAll();
      req.onsuccess = e => res(e.target.result);
      req.onerror   = e => rej(e.target.error);
    }),
    new Promise((res, rej) => {
      const req = db.transaction('pendentes').objectStore('pendentes').getAllKeys();
      req.onsuccess = e => res(e.target.result);
      req.onerror   = e => rej(e.target.error);
    }),
  ]);

  for (let i = 0; i < registros.length; i++) {
    const resp = await fetch('/api/diario-bordo', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(registros[i]),
    });
    if (resp.ok) {
      await new Promise((res, rej) => {
        const tx  = db.transaction('pendentes', 'readwrite');
        const req = tx.objectStore('pendentes').delete(chaves[i]);
        tx.oncomplete = res;
        req.onerror   = rej;
      });
      // Notifica as abas abertas
      self.clients.matchAll().then(clients =>
        clients.forEach(c => c.postMessage({ tipo: 'SYNC_OK' }))
      );
    }
  }
}

// Background Sync (Chrome/Android)
self.addEventListener('sync', e => {
  if (e.tag === 'sync-diario') e.waitUntil(enviarPendentes());
});
