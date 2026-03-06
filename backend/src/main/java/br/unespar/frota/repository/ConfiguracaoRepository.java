package br.unespar.frota.repository;

import br.unespar.frota.entity.Configuracao;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConfiguracaoRepository extends JpaRepository<Configuracao, String> {
}
