package br.unespar.frota.admin;

import br.unespar.frota.entity.DiarioBordoRecord;
import br.unespar.frota.entity.Veiculo;
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
    private final VeiculoRepository veiculoRepository;

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
            if (!adminEmail.equalsIgnoreCase(email) && !adminEmail2.equalsIgnoreCase(email)) {
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
        LocalDateTime dtInicio = inicio != null ? inicio.atStartOfDay() : null;
        LocalDateTime dtFim    = fim    != null ? fim.atTime(23, 59, 59) : null;
        String condutorParam = (condutor != null && !condutor.isBlank()) ? condutor : null;
        String veiculoParam  = (veiculo  != null && !veiculo.isBlank())  ? veiculo  : null;
        String tipoParam     = (tipo     != null && !tipo.isBlank())     ? tipo     : null;

        return ResponseEntity.ok(repository.filtrar(condutorParam, veiculoParam, tipoParam, dtInicio, dtFim));
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
}
