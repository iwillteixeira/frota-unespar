const API_URL = '/api/diario-bordo';

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
  };

  try {
    const res = await fetch(API_URL, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload),
    });
    if (!res.ok) throw new Error('Erro no servidor.');
    showToast('✅ Registro enviado com sucesso!', 'sucesso');
    limpar();
  } catch (e) {
    showToast('❌ ' + e.message, 'erro');
  } finally {
    btn.disabled = false;
    btn.textContent = 'ENVIAR REGISTRO';
  }
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
  window.scrollTo({ top: 0, behavior: 'smooth' });
}

// ── Service Worker ────────────────────────────────────────────
if ('serviceWorker' in navigator) {
  navigator.serviceWorker.register('/sw.js');
}
