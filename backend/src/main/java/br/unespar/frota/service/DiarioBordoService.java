package br.unespar.frota.service;

import br.unespar.frota.dto.DiarioBordoDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class DiarioBordoService {

    private final JavaMailSender mailSender;

    @Value("${frota.email.destino}")
    private String emailDestino;

    @Value("${spring.mail.username}")
    private String emailUsername;

    public void registrar(DiarioBordoDTO dto) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(emailUsername);
        message.setTo(emailDestino);
        message.setSubject(buildSubject(dto));
        message.setText(buildBody(dto));
        mailSender.send(message);
    }

    private String buildSubject(DiarioBordoDTO dto) {
        String tipo = dto.getTipoMovimentacao() == DiarioBordoDTO.TipoMovimentacao.RETIRADA
                ? "RETIRADA" : "DEVOLUÇÃO";
        return "[FROTA UNESPAR] " + tipo + " - " + dto.getVeiculo() + " - " + dto.getNomeCondutor();
    }

    private String buildBody(DiarioBordoDTO dto) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        String tipo = dto.getTipoMovimentacao() == DiarioBordoDTO.TipoMovimentacao.RETIRADA
                ? "RETIRADA / SAÍDA" : "DEVOLUÇÃO / CHEGADA";

        StringBuilder sb = new StringBuilder();
        sb.append("=== DIÁRIO DE BORDO - UNESPAR ===\n\n");
        sb.append("Data/Hora: ").append(LocalDateTime.now().format(fmt)).append("\n");
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
