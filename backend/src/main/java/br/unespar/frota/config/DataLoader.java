package br.unespar.frota.config;

import br.unespar.frota.entity.DiarioBordoRecord;
import br.unespar.frota.entity.Veiculo;
import br.unespar.frota.repository.DiarioBordoRepository;
import br.unespar.frota.repository.VeiculoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Configuration
@RequiredArgsConstructor
public class DataLoader {

    private final DiarioBordoRepository repository;
    private final VeiculoRepository veiculoRepository;

    @Bean
    @Profile("!prod")
    CommandLineRunner carregarDadosMock() {
        return args -> {
            // Veículos reais do formulário
            List<String> veiculosReais = List.of(
                "GOL (ADMINISTRATIVO)",
                "HB20",
                "FIAT CROMOS",
                "LOGAN AQT-6D99 (CINEMA)",
                "LOGAN AQY-6384 (ALMOXARIFADO)",
                "LOGAN AQY-6227",
                "KOMBI AQX-5047 (COM BANCO)",
                "KOMBI AQX-5046 (SEM BANCO, PATRIMÔNIO)"
            );
            if (veiculoRepository.count() == 0) {
                veiculosReais.forEach(nome -> {
                    Veiculo v = new Veiculo();
                    v.setNome(nome);
                    veiculoRepository.save(v);
                });
                System.out.println("[DataLoader] " + veiculosReais.size() + " veículos inseridos.");
            }

            // Registros mock
            if (repository.count() > 0) return;

            String[] condutores = {
                "Carlos Silva", "Ana Souza", "Roberto Lima", "Fernanda Costa",
                "Marcos Oliveira", "Juliana Pereira", "Ricardo Alves", "Patrícia Santos"
            };
            String[] destinos = {
                "Reitoria - Paranavaí", "Campus Apucarana", "Campus Curitiba I",
                "SEED - Curitiba", "Secretaria de Estado - Curitiba",
                "Campus Campo Mourão", "Campus Paranaguá", "Banco do Brasil - Centro"
            };
            String[] tanques = { "CHEIO", "3/4", "MEIO 1/2", "1/4" };
            String[] passageiros = {
                "Maria Fernanda, João Pedro", "Luciana Martins",
                "Pedro Henrique, Carlos Augusto", null, null, null
            };

            Random rnd = new Random(42);
            List<DiarioBordoRecord> registros = new ArrayList<>();
            LocalDateTime base = LocalDateTime.now().minusDays(60);

            for (int i = 0; i < 100; i++) {
                DiarioBordoRecord r = new DiarioBordoRecord();
                r.setDataHora(base.plusHours(i * 14L + rnd.nextInt(8)));
                r.setTipoMovimentacao(i % 2 == 0 ? "RETIRADA" : "DEVOLUCAO");
                r.setNomeCondutor(condutores[rnd.nextInt(condutores.length)]);
                r.setVeiculo(veiculosReais.get(rnd.nextInt(veiculosReais.size())));
                r.setKmAtual(50000 + i * 120 + rnd.nextInt(50));
                r.setDestino(destinos[rnd.nextInt(destinos.length)]);
                r.setVolumeTanque(tanques[rnd.nextInt(tanques.length)]);
                String pass = passageiros[rnd.nextInt(passageiros.length)];
                r.setTemPassageiros(pass != null);
                r.setNomePassageiros(pass);
                r.setObservacoes(rnd.nextInt(5) == 0 ? "Veículo com pneu calibrado antes da saída." : null);
                r.setCienteInstrucoes(true);
                registros.add(r);
            }

            repository.saveAll(registros);
            System.out.println("[DataLoader] 100 registros mock inseridos.");
        };
    }
}
