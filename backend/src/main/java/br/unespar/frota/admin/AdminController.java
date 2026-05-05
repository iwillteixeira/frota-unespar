package br.unespar.frota.admin;

import br.unespar.frota.entity.AdminUser;
import br.unespar.frota.entity.Configuracao;
import br.unespar.frota.entity.DiarioBordoFoto;
import br.unespar.frota.entity.DiarioBordoRecord;
import br.unespar.frota.entity.Veiculo;
import br.unespar.frota.repository.AdminUserRepository;
import br.unespar.frota.repository.ConfiguracaoRepository;
import br.unespar.frota.repository.DiarioBordoFotoRepository;
import br.unespar.frota.repository.DiarioBordoRepository;
import br.unespar.frota.repository.VeiculoRepository;
import br.unespar.frota.security.JwtService;
import br.unespar.frota.security.MicrosoftTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final MicrosoftTokenService microsoftTokenService;
    private final JwtService jwtService;
    private final DiarioBordoRepository repository;
    private final DiarioBordoFotoRepository fotoRepository;
    private final VeiculoRepository veiculoRepository;
    private final AdminUserRepository adminUserRepository;
    private final ConfiguracaoRepository configuracaoRepository;

    @Value("${frota.email.destino}")
    private String emailDestinoDefault;

    @Value("${frota.admin.email}")
    private String adminEmail;

    @Value("${frota.admin.email2:}")
    private String adminEmail2;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
        String idToken = body.get("idToken");
        if (idToken == null || idToken.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("erro", "idToken ausente"));
        }
        try {
            String email = microsoftTokenService.validarEObterEmail(idToken);
            if (!adminEmail.equalsIgnoreCase(email) && !adminEmail2.equalsIgnoreCase(email)
                    && !adminUserRepository.existsByEmailIgnoreCase(email)) {
                return ResponseEntity.status(403).body(Map.of("erro", "Acesso não autorizado para " + email));
            }
            String token = jwtService.gerarToken(email);
            return ResponseEntity.ok(Map.of("token", token, "email", email));
        } catch (SecurityException e) {
            return ResponseEntity.status(401).body(Map.of("erro", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("erro", "Erro ao validar token: " + e.getMessage()));
        }
    }

    @GetMapping("/registros")
    public ResponseEntity<List<DiarioBordoRecord>> registros(
            @RequestParam(required = false) String condutor,
            @RequestParam(required = false) String veiculo,
            @RequestParam(required = false) String tipo,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim
    ) {
        // Datas sempre preenchidas para evitar inferência de tipo NULL no PostgreSQL
        LocalDateTime dtInicio = inicio != null ? inicio.atStartOfDay()   : LocalDateTime.of(2000, 1, 1, 0, 0);
        LocalDateTime dtFim    = fim    != null ? fim.atTime(23, 59, 59)  : LocalDateTime.of(2100, 1, 1, 0, 0);
        String condutorParam = (condutor != null && !condutor.isBlank()) ? condutor : null;
        String veiculoParam  = (veiculo  != null && !veiculo.isBlank())  ? veiculo  : null;
        String tipoParam     = (tipo     != null && !tipo.isBlank())     ? tipo     : null;

        return ResponseEntity.ok(repository.filtrar(condutorParam, veiculoParam, tipoParam, dtInicio, dtFim));
    }

    @GetMapping("/registros/{id}/fotos")
    public ResponseEntity<?> getFotos(@PathVariable Long id) {
        List<DiarioBordoFoto> fotos = fotoRepository.findByRegistroId(id);
        List<Map<String, String>> response = fotos.stream().map(f -> Map.of(
                "id", String.valueOf(f.getId()),
                "nomeArquivo", f.getNomeArquivo(),
                "tipoConteudo", f.getTipoConteudo(),
                "dadosBase64", f.getDadosBase64()
        )).toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/veiculos")
    public ResponseEntity<List<Veiculo>> veiculos() {
        return ResponseEntity.ok(veiculoRepository.findAll()
                .stream().sorted((a, b) -> a.getNome().compareToIgnoreCase(b.getNome())).toList());
    }

    @PostMapping("/veiculos")
    public ResponseEntity<?> addVeiculo(@RequestBody Map<String, String> body) {
        String nome = body.getOrDefault("nome", "").trim();
        if (nome.isBlank()) return ResponseEntity.badRequest().body(Map.of("erro", "Nome obrigatório"));
        if (veiculoRepository.existsByNomeIgnoreCase(nome))
            return ResponseEntity.badRequest().body(Map.of("erro", "Veículo já existe"));
        Veiculo v = new Veiculo();
        v.setNome(nome);
        return ResponseEntity.ok(veiculoRepository.save(v));
    }

    @DeleteMapping("/veiculos/{id}")
    public ResponseEntity<?> removeVeiculo(@PathVariable Long id) {
        if (!veiculoRepository.existsById(id))
            return ResponseEntity.notFound().build();
        veiculoRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("ok", true));
    }

    @PatchMapping("/veiculos/{id}/manutencao")
    public ResponseEntity<?> toggleManutencao(@PathVariable Long id) {
        return veiculoRepository.findById(id).map(v -> {
            v.setEmManutencao(!v.isEmManutencao());
            return ResponseEntity.ok((Object) veiculoRepository.save(v));
        }).orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/veiculos/{id}")
    public ResponseEntity<?> editarVeiculo(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String nome = body.getOrDefault("nome", "").trim();
        if (nome.isBlank()) return ResponseEntity.badRequest().body(Map.of("erro", "Nome obrigatório"));
        return veiculoRepository.findById(id).map(v -> {
            if (!v.getNome().equalsIgnoreCase(nome) && veiculoRepository.existsByNomeIgnoreCase(nome))
                return ResponseEntity.badRequest().body((Object) Map.of("erro", "Veículo já existe"));
            v.setNome(nome);
            return ResponseEntity.ok((Object) veiculoRepository.save(v));
        }).orElse(ResponseEntity.notFound().build());
    }

    // ── Usuários Admin ──────────────────────────────────────────────────────────

    @GetMapping("/usuarios")
    public ResponseEntity<List<AdminUser>> listarUsuarios() {
        return ResponseEntity.ok(adminUserRepository.findAll()
                .stream().sorted((a, b) -> a.getEmail().compareToIgnoreCase(b.getEmail())).toList());
    }

    @PostMapping("/usuarios")
    public ResponseEntity<?> addUsuario(@RequestBody Map<String, String> body) {
        String email = body.getOrDefault("email", "").trim().toLowerCase();
        if (email.isBlank()) return ResponseEntity.badRequest().body(Map.of("erro", "E-mail obrigatório"));
        if (adminUserRepository.existsByEmailIgnoreCase(email))
            return ResponseEntity.badRequest().body(Map.of("erro", "E-mail já cadastrado"));
        AdminUser u = new AdminUser();
        u.setEmail(email);
        return ResponseEntity.ok(adminUserRepository.save(u));
    }

    @DeleteMapping("/usuarios/{id}")
    public ResponseEntity<?> removeUsuario(@PathVariable Long id) {
        if (!adminUserRepository.existsById(id)) return ResponseEntity.notFound().build();
        adminUserRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("ok", true));
    }

    // ── Configurações ──────────────────────────────────────────────────────────

    @GetMapping("/config/notificacao")
    public ResponseEntity<?> getEmailNotificacao() {
        String email = configuracaoRepository.findById("email.notificacao")
                .map(c -> c.getValor())
                .filter(v -> !v.isBlank())
                .orElse(emailDestinoDefault);
        return ResponseEntity.ok(Map.of("email", email));
    }

    @PutMapping("/config/notificacao")
    public ResponseEntity<?> setEmailNotificacao(@RequestBody Map<String, String> body) {
        String email = body.getOrDefault("email", "").trim();
        if (email.isBlank()) return ResponseEntity.badRequest().body(Map.of("erro", "E-mail obrigatório"));
        Configuracao c = configuracaoRepository.findById("email.notificacao").orElse(new Configuracao());
        c.setChave("email.notificacao");
        c.setValor(email);
        configuracaoRepository.save(c);
        return ResponseEntity.ok(Map.of("ok", true, "email", email));
    }

    @DeleteMapping("/config/notificacao")
    public ResponseEntity<?> removeEmailNotificacao() {
        configuracaoRepository.deleteById("email.notificacao");
        return ResponseEntity.ok(Map.of("ok", true));
    }
}
