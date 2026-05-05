package br.unespar.frota.repository;

import br.unespar.frota.entity.DiarioBordoFoto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DiarioBordoFotoRepository extends JpaRepository<DiarioBordoFoto, Long> {
    List<DiarioBordoFoto> findByRegistroId(Long registroId);
}
