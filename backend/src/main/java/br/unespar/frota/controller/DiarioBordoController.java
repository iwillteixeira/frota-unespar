package br.unespar.frota.controller;

import br.unespar.frota.dto.DiarioBordoDTO;
import br.unespar.frota.service.DiarioBordoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/diario-bordo")
@RequiredArgsConstructor
public class DiarioBordoController {

    private final DiarioBordoService service;

    @PostMapping
    public ResponseEntity<Map<String, String>> registrar(@Valid @RequestBody DiarioBordoDTO dto) {
        service.registrar(dto);
        return ResponseEntity.ok(Map.of("mensagem", "Registro enviado com sucesso!"));
    }
}
