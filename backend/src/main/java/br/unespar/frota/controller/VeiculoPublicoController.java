package br.unespar.frota.controller;

import br.unespar.frota.entity.Veiculo;
import br.unespar.frota.repository.VeiculoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class VeiculoPublicoController {

    private final VeiculoRepository veiculoRepository;

    @GetMapping("/veiculos")
    public ResponseEntity<List<String>> listar() {
        return ResponseEntity.ok(
            veiculoRepository.findAll().stream()
                .filter(v -> !v.isEmManutencao())
                .sorted((a, b) -> a.getNome().compareToIgnoreCase(b.getNome()))
                .map(Veiculo::getNome)
                .toList()
        );
    }
}
