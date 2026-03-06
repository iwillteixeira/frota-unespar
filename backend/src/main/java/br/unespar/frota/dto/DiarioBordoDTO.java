package br.unespar.frota.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class DiarioBordoDTO {

    @NotNull(message = "Declaração de ciência é obrigatória")
    private Boolean cienteInstrucoes;

    @NotNull(message = "Informe se está retirando ou devolvendo o veículo")
    private TipoMovimentacao tipoMovimentacao;

    @NotBlank(message = "Nome completo é obrigatório")
    private String nomeCondutor;

    @NotNull(message = "KM atual é obrigatório")
    @Positive(message = "KM deve ser positivo")
    private Integer kmAtual;

    @NotBlank(message = "Veículo/Placa é obrigatório")
    private String veiculo;

    @NotBlank(message = "Destino é obrigatório")
    private String destino;

    @NotNull(message = "Informe se há passageiros")
    private Boolean temPassageiros;

    private String nomePassageiros;

    @NotBlank(message = "Volume do tanque é obrigatório")
    private String volumeTanque;

    private String observacoes;

    public enum TipoMovimentacao {
        RETIRADA, DEVOLUCAO
    }
}
