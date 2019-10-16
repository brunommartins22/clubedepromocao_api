package br.com.interagese.promocao.service;

import java.time.Instant;
import java.time.Period;
import java.time.ZoneId;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
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

    private ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

    public SincronizadorService() {

    }
    
    public void iniciarTransmissao() {
        executorService.scheduleAtFixedRate(this::executarSincronizacao, 0, 5, TimeUnit.MINUTES);
    }

    public void executarSincronizacao() {
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
                tabpromocaoService.baixarPromocoes();
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

    @EventListener
    public void onApplicationEvent(ContextRefreshedEvent e) {
        iniciarTransmissao();
    }

}
