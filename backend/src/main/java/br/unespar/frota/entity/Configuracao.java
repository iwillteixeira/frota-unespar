package br.unespar.frota.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "configuracoes")
@Getter @Setter
public class Configuracao {
    @Id
    @Column(length = 100)
    private String chave;

    @Column(nullable = false, length = 500)
    private String valor;
}
