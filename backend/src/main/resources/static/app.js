const API_URL = '/api/diario-bordo';

// ── IndexedDB offline ─────────────────────────────────────────
function abrirDB() {
  return new Promise((resolve, reject) => {
    const req = indexedDB.open('frota-offline', 1);
    req.onupgradeneeded = e =>
      e.target.result.createObjectStore('pendentes', { autoIncrement: true });
    req.onsuccess = e => resolve(e.target.result);
    req.onerror   = e => reject(e.target.error);
  });
}

async function salvarOffline(payload) {
  const db = await abrirDB();
  return new Promise((resolve, reject) => {
    const tx  = db.transaction('pendentes', 'readwrite');
    const req = tx.objectStore('pendentes').add(payload);
    tx.oncomplete = resolve;
    req.onerror   = reject;
  });
}

async function contarPendentes() {
  const db = await abrirDB();
  return new Promise((resolve, reject) => {
    const req = db.transaction('pendentes').objectStore('pendentes').count();
    req.onsuccess = e => resolve(e.target.result);
    req.onerror   = e => reject(e.target.error);
  });
}

async function tentarEnviarPendentes() {
  if (!navigator.onLine) return;
  const n = await contarPendentes();
  if (n === 0) return;
  if ('serviceWorker' in navigator && 'SyncManager' in window) {
    const reg = await navigator.serviceWorker.ready;
    await reg.sync.register('sync-diario');
  }
}

// ── Detecção de ambiente ──────────────────────────────────────
let deferredPrompt = null;
const isSafari = /^((?!chrome|android).)*safari/i.test(navigator.userAgent);
const isIOS    = /iphone|ipad|ipod/i.test(navigator.userAgent);
const isInStandalone = window.matchMedia('(display-mode: standalone)').matches
  || window.navigator.standalone;

// Captura prompt de instalação (Android/Chrome)
window.addEventListener('beforeinstallprompt', e => {
  e.preventDefault();
  deferredPrompt = e;
});
window.addEventListener('appinstalled', () => { deferredPrompt = null; });

// ── Drawer ────────────────────────────────────────────────────
function abrirMenu() {
  // Esconde botão "Instalar" se já está como app
  const item = document.getElementById('drawer-install');
  if (isInStandalone) {
    item.classList.add('hidden');
  } else if (isIOS && !isSafari) {
    // iOS mas não Safari — não consegue instalar PWA
    item.classList.add('hidden');
  }
  document.getElementById('drawer').classList.remove('hidden');
  document.getElementById('drawer-overlay').classList.remove('hidden');
}
function fecharMenu() {
  document.getElementById('drawer').classList.add('hidden');
  document.getElementById('drawer-overlay').classList.add('hidden');
}

// ── Instalar app ─────────────────────────────────────────────
async function instalarApp() {
  fecharMenu();
  if (isIOS && isSafari && !isInStandalone) {
    document.getElementById('modal-ios').classList.remove('hidden');
    return;
  }
  if (deferredPrompt) {
    deferredPrompt.prompt();
    await deferredPrompt.userChoice;
    deferredPrompt = null;
  } else {
    showToast('Abra no Chrome → menu ⋮ → "Adicionar à tela inicial"', '');
  }
}

function fecharModal() {
  document.getElementById('modal-ios').classList.add('hidden');
}



// ── Fotos ─────────────────────────────────────────────────────
const fotosState = []; // { nomeArquivo, tipoConteudo, dadosBase64 }

function onFotosSelecionadas(input) {
  const files = Array.from(input.files);
  input.value = ''; // permite re-selecionar o mesmo arquivo
  files.forEach(file => {
    if (!file.type.startsWith('image/')) return;
    const reader = new FileReader();
    reader.onload = e => {
      const base64 = e.target.result.split(',')[1];
      fotosState.push({ nomeArquivo: file.name, tipoConteudo: file.type, dadosBase64: base64 });
      renderFotosPreview();
    };
    reader.readAsDataURL(file);
  });
}

function renderFotosPreview() {
  const container = document.getElementById('fotos-preview');
  container.innerHTML = fotosState.map((f, i) => `
    <div class="foto-thumb">
      <img src="data:${f.tipoConteudo};base64,${f.dadosBase64}" alt="${f.nomeArquivo}" />
      <button class="foto-thumb-rm" onclick="removerFoto(${i})" type="button">✕</button>
    </div>`).join('');
}

function removerFoto(i) {
  fotosState.splice(i, 1);
  renderFotosPreview();
}

// ── Estado do formulário ──────────────────────────────────────
const state = {
  cienteInstrucoes: null,
  tipoMovimentacao: null,
  temPassageiros: null,
};

// ── Helpers ──────────────────────────────────────────────────
function setOpcao(grupo, valor) {
  state[grupo] = valor;
  document.querySelectorAll(`[data-grupo="${grupo}"]`).forEach(el => {
    el.classList.toggle('selecionada', el.dataset.valor === String(valor));
  });
  if (grupo === 'temPassageiros') {
    document.getElementById('campo-passageiros').classList.toggle('hidden', !valor);
  }
}

function showToast(msg, tipo = '') {
  const t = document.getElementById('toast');
  t.textContent = msg;
  t.className = `toast show ${tipo}`;
  setTimeout(() => t.classList.remove('show'), 3500);
}

function validar() {
  if (state.cienteInstrucoes === null) return 'Confirme se está ciente das instruções normativas.';
  if (!state.cienteInstrucoes)         return 'Você precisa estar ciente das instruções para prosseguir.';
  if (state.tipoMovimentacao === null) return 'Informe se está retirando ou devolvendo o veículo.';
  if (!document.getElementById('nomeCondutor').value.trim()) return 'Nome completo é obrigatório.';
  const km = document.getElementById('kmAtual').value;
  if (!km || isNaN(km) || Number(km) <= 0) return 'KM atual inválido.';
  if (!document.getElementById('veiculo').value) return 'Selecione o veículo.';
  if (!document.getElementById('destino').value.trim()) return 'Destino é obrigatório.';
  if (state.temPassageiros === null) return 'Informe se há passageiros.';
  if (!document.getElementById('volumeTanque').value) return 'Selecione o volume do tanque.';
  return null;
}

async function enviar() {
  const erro = validar();
  if (erro) { showToast(erro, 'erro'); return; }

  const btn = document.getElementById('btn-enviar');
  btn.disabled = true;
  btn.textContent = 'Enviando…';

  const payload = {
    cienteInstrucoes: state.cienteInstrucoes,
    tipoMovimentacao: state.tipoMovimentacao,
    nomeCondutor: document.getElementById('nomeCondutor').value.trim(),
    kmAtual: Number(document.getElementById('kmAtual').value),
    veiculo: document.getElementById('veiculo').value,
    destino: document.getElementById('destino').value.trim(),
    temPassageiros: state.temPassageiros,
    nomePassageiros: state.temPassageiros
      ? document.getElementById('nomePassageiros').value.trim()
      : undefined,
    volumeTanque: document.getElementById('volumeTanque').value,
    observacoes: document.getElementById('observacoes').value.trim() || undefined,
    fotos: fotosState.length > 0 ? fotosState.map(f => ({ ...f })) : undefined,
  };

  let enviado = false;
  if (navigator.onLine) {
    try {
      const res = await fetch(API_URL, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload),
      });
      if (!res.ok) throw new Error(`HTTP ${res.status}`);
      enviado = true;
    } catch (_) {}
  }

  if (enviado) {
    showToast('✅ Registro enviado com sucesso!', 'sucesso');
  } else {
    await salvarOffline(payload);
    // Tenta Background Sync (Chrome/Android)
    if ('serviceWorker' in navigator && 'SyncManager' in window) {
      const reg = await navigator.serviceWorker.ready;
      await reg.sync.register('sync-diario');
    }
    showToast('📶 Sem conexão — será enviado quando você ficar online.', 'aviso');
  }

  limpar();
  btn.disabled = false;
  btn.textContent = 'Enviar registro';
}

function limpar() {
  state.cienteInstrucoes = null;
  state.tipoMovimentacao = null;
  state.temPassageiros = null;
  document.querySelectorAll('.opcao').forEach(el => el.classList.remove('selecionada'));
  document.getElementById('nomeCondutor').value = '';
  document.getElementById('kmAtual').value = '';
  document.getElementById('veiculo').value = '';
  document.getElementById('destino').value = '';
  document.getElementById('nomePassageiros').value = '';
  document.getElementById('volumeTanque').value = '';
  document.getElementById('observacoes').value = '';
  document.getElementById('campo-passageiros').classList.add('hidden');
  fotosState.length = 0;
  renderFotosPreview();
  window.scrollTo({ top: 0, behavior: 'smooth' });
}

// ── Service Worker ────────────────────────────────────────────
if ('serviceWorker' in navigator) {
  navigator.serviceWorker.register('/sw.js');

  // Recebe confirmação de envio feito em background pelo SW
  navigator.serviceWorker.addEventListener('message', e => {
    if (e.data?.tipo === 'SYNC_OK') {
      showToast('✅ Registro offline enviado com sucesso!', 'sucesso');
    }
  });
}

// Fallback para Safari/Firefox (sem Background Sync API)
window.addEventListener('online', tentarEnviarPendentes);

// ── Carrega veículos da API ───────────────────────────────────
async function carregarListaVeiculos() {
  try {
    const resp = await fetch('/api/veiculos');
    if (!resp.ok) return;
    const nomes = await resp.json();
    const sel = document.getElementById('veiculo');
    sel.innerHTML = '<option value="">Selecione o veículo...</option>';
    nomes.forEach(n => { const o = document.createElement('option'); o.value = n; o.textContent = n; sel.appendChild(o); });
  } catch (_) {}
}
document.addEventListener('DOMContentLoaded', carregarListaVeiculos);
