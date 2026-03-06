package br.unespar.frota.service;

import br.unespar.frota.dto.DiarioBordoDTO;
import br.unespar.frota.entity.DiarioBordoRecord;
import br.unespar.frota.repository.DiarioBordoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class DiarioBordoService {

    private final JavaMailSender mailSender;
    private final DiarioBordoRepository repository;

    @Value("${frota.email.destino}")
    private String emailDestino;

    @Value("${spring.mail.username}")
    private String emailUsername;

    public void registrar(DiarioBordoDTO dto) {
        LocalDateTime agora = LocalDateTime.now(ZoneId.of("America/Sao_Paulo"));

        // Salva no banco
        DiarioBordoRecord record = new DiarioBordoRecord();
        record.setDataHora(agora);
        record.setTipoMovimentacao(dto.getTipoMovimentacao().name());
        record.setNomeCondutor(dto.getNomeCondutor());
        record.setKmAtual(dto.getKmAtual());
        record.setVeiculo(dto.getVeiculo());
        record.setDestino(dto.getDestino());
        record.setTemPassageiros(dto.getTemPassageiros());
        record.setNomePassageiros(dto.getNomePassageiros());
        record.setVolumeTanque(dto.getVolumeTanque());
        record.setObservacoes(dto.getObservacoes());
        record.setCienteInstrucoes(dto.getCienteInstrucoes());
        repository.save(record);

        // Envia por e-mail
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(emailUsername);
        message.setTo(emailDestino);
        message.setSubject(buildSubject(dto));
        message.setText(buildBody(dto, agora));
        mailSender.send(message);
    }

    private String buildSubject(DiarioBordoDTO dto) {
        String tipo = dto.getTipoMovimentacao() == DiarioBordoDTO.TipoMovimentacao.RETIRADA
                ? "RETIRADA" : "DEVOLUÇÃO";
        return "[FROTA UNESPAR] " + tipo + " - " + dto.getVeiculo() + " - " + dto.getNomeCondutor();
    }

    private String buildBody(DiarioBordoDTO dto, LocalDateTime dataHora) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        String tipo = dto.getTipoMovimentacao() == DiarioBordoDTO.TipoMovimentacao.RETIRADA
                ? "RETIRADA / SAÍDA" : "DEVOLUÇÃO / CHEGADA";

        StringBuilder sb = new StringBuilder();
        sb.append("=== DIÁRIO DE BORDO - UNESPAR ===\n\n");
        sb.append("Data/Hora: ").append(dataHora.format(fmt)).append("\n");
        sb.append("Tipo: ").append(tipo).append("\n\n");
        sb.append("Condutor: ").append(dto.getNomeCondutor()).append("\n");
        sb.append("Veículo/Placa: ").append(dto.getVeiculo()).append("\n");
        sb.append("KM Atual: ").append(dto.getKmAtual()).append("\n");
        sb.append("Destino: ").append(dto.getDestino()).append("\n");
        sb.append("Volume do tanque: ").append(dto.getVolumeTanque()).append("\n");
        sb.append("Passageiros: ").append(dto.getTemPassageiros() ? "Sim" : "Não").append("\n");

        if (dto.getTemPassageiros() && dto.getNomePassageiros() != null) {
            sb.append("Nome(s) passageiro(s): ").append(dto.getNomePassageiros()).append("\n");
        }

        if (dto.getObservacoes() != null && !dto.getObservacoes().isBlank()) {
            sb.append("\nObservações:\n").append(dto.getObservacoes()).append("\n");
        }

        sb.append("\nCiente das instruções normativas 001/2025 e 004/2024: ")
          .append(dto.getCienteInstrucoes() ? "SIM" : "NÃO").append("\n");

        return sb.toString();
    }
}

