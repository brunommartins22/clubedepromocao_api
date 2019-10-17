package br.com.interagese.promocao.service;

import br.com.interagese.postgres.dtos.StatusSincronizadorDto;
import br.com.interagese.promocao.enuns.Envio;
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
    private Envio envio = Envio.NADA;

    @Autowired
    private TabpromocaoService tabpromocaoService;

    @Autowired
    private NotasaiService notasaiService;

    @Autowired
    private FechamentoPromocaoService fechamentoPromocaoService;

    @Autowired
    private SincronizacaoService sincronizacaoService;

    private ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

    public void iniciarTransmissao() {
        executorService.scheduleAtFixedRate(this::executarSincronizacao, 0, 5, TimeUnit.MINUTES);
    }

    public void executarSincronizacao() {
        if (!executando) {
            executando = true;
            try {

                Date dataDaSincronizacaoAtual = new Date();

                //tabpromocaoService.baixarPromocoes();

//                Date dataDaUltimaSincronizacaoDeVenda = sincronizacaoService.getDataDaUltimaSincronizacaoDeVenda();
//                notasaiService.enviarVendas(dataDaUltimaSincronizacaoDeVenda, dataDaSincronizacaoAtual);
//
//                Date dataDoUltimoFechamento = sincronizacaoService.getDataDaUltimaSincronizacaoDeFechamento();
//                fechamentoPromocaoService.enviarFechamento(dataDoUltimoFechamento, dataDaSincronizacaoAtual);
//
//                tabpromocaoService.baixarPromocoes();
                System.out.println("Sincronização finalizada: " + dataDaSincronizacaoAtual);
            } catch (Exception ex) {
                ex.printStackTrace();
            } finally {
                executando = false;
            }
        }
    }

    private boolean foiAMaisDe1DiasAtras(Date dataDoUltimoFechamento, Date dataDaSincronizacaoAtual) {

        return Period
                .between(
                        Instant.ofEpochMilli(dataDoUltimoFechamento.getTime())
                                .atZone(ZoneId.systemDefault()).toLocalDate(),
                        Instant.ofEpochMilli(dataDaSincronizacaoAtual.getTime())
                                .atZone(ZoneId.systemDefault()).toLocalDate())
                .getDays() > 2;

    }

    @EventListener
    public void onApplicationEvent(ContextRefreshedEvent e) {
        iniciarTransmissao();
    }
    
    public StatusSincronizadorDto getStatus(){
        return new StatusSincronizadorDto(executando, envio);
    }

}
