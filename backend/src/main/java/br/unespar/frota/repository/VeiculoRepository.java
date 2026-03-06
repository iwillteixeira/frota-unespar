package br.unespar.frota.repository;

import br.unespar.frota.entity.Veiculo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VeiculoRepository extends JpaRepository<Veiculo, Long> {
    boolean existsByNomeIgnoreCase(String nome);
}
