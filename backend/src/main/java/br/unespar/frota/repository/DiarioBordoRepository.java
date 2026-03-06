package br.unespar.frota.repository;

import br.unespar.frota.entity.DiarioBordoRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface DiarioBordoRepository extends JpaRepository<DiarioBordoRecord, Long> {

    @Query("SELECT r FROM DiarioBordoRecord r WHERE " +
           "(:condutor IS NULL OR LOWER(r.nomeCondutor) LIKE LOWER(CONCAT('%', CAST(:condutor AS string), '%'))) AND " +
           "(:veiculo  IS NULL OR r.veiculo = :veiculo) AND " +
           "(:tipo     IS NULL OR r.tipoMovimentacao = :tipo) AND " +
           "r.dataHora >= :inicio AND r.dataHora <= :fim " +
           "ORDER BY r.dataHora DESC")
    List<DiarioBordoRecord> filtrar(
            @Param("condutor") String condutor,
            @Param("veiculo")  String veiculo,
            @Param("tipo")     String tipo,
            @Param("inicio")   LocalDateTime inicio,
            @Param("fim")      LocalDateTime fim
    );
}
