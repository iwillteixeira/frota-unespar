package br.unespar.frota.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "diario_bordo")
@Data
public class DiarioBordoRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime dataHora;

    @Column(nullable = false, length = 20)
    private String tipoMovimentacao;

    @Column(nullable = false)
    private String nomeCondutor;

    @Column(nullable = false)
    private Integer kmAtual;

    @Column(nullable = false)
    private String veiculo;

    @Column(nullable = false)
    private String destino;

    @Column(nullable = false)
    private Boolean temPassageiros;

    private String nomePassageiros;

    @Column(nullable = false, length = 20)
    private String volumeTanque;

    @Column(columnDefinition = "TEXT")
    private String observacoes;

    @Column(nullable = false)
    private Boolean cienteInstrucoes;
}
