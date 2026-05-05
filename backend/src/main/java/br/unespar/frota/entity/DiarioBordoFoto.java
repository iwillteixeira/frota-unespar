package br.unespar.frota.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "diario_bordo_foto")
@Data
public class DiarioBordoFoto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "registro_id", nullable = false)
    private DiarioBordoRecord registro;

    @Column(nullable = false)
    private String nomeArquivo;

    @Column(nullable = false)
    private String tipoConteudo;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String dadosBase64;
}
