package br.com.interagese.promocao.service;

import java.time.Instant;
import java.time.Period;
import java.time.ZoneId;
import java.util.Date;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class SincronizadorService {

    private boolean executando = false;

    @Autowired
    private TabpromocaoService tabpromocaoService;

    @Autowired
    private NotasaiService notasaiService;

    @Autowired
    private FechamentoPromocaoService fechamentoPromocaoService;

    @Autowired
    private SincronizacaoService sincronizacaoService;

    public SincronizadorService() {

    }

    @Scheduled(initialDelay = 2000, fixedDelay = 999999999)
    public void executarTransmissao() {
        if (!executando) {
            executando = true;
            try {

//                Date dataDaSincronizacaoAtual = new Date();
//                Date dataDoUltimoFechamento = sincronizacaoService.getDataDaUltimaSincronizacaoDeFechamento();
//                if (foiAMaisDe1DiasAtras(dataDoUltimoFechamento)) {
//
//                    //fechamentoPromocaoService.enviarFechamento(dataDoUltimoFechamento, dataDaSincronizacaoAtual);
//
//                }
//                System.out.println("Teste: " + dataDoUltimoFechamento);
                // notasaiService.enviarVendas();
                //tabpromocaoService.baixarPromocoes();
                //System.out.println("Promoção baixadas");
            } catch (Exception ex) {
                ex.printStackTrace();
            } finally {
                executando = false;
            }
        }
    }

    private boolean foiAMaisDe1DiasAtras(Date dataDoUltimoFechamento) {

        return Period
                .between(
                        Instant.ofEpochMilli(dataDoUltimoFechamento.getTime())
                                .atZone(ZoneId.systemDefault()).toLocalDate(),
                        Instant.now().atZone(ZoneId.systemDefault()).toLocalDate())
                .getDays() > 2;

    }

}
