package br.com.interagese.promocao.service;

import br.com.interagese.postgres.dtos.StatusSincronizadorDto;
import br.com.interagese.postgres.models.Configuracao;
import br.com.interagese.postgres.models.ConfiguracaoItem;
import br.com.interagese.promocao.enuns.Envio;
import java.time.Instant;
import java.time.Period;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;

@Service
public class SincronizadorService {

    @Autowired
    private TabpromocaoService tabpromocaoService;
    @Autowired
    private NotasaiService notasaiService;
    @Autowired
    private FechamentoPromocaoService fechamentoPromocaoService;
    @Autowired
    private ConfiguracaoService configuracaoService;
    @Autowired
    private SincronizacaoService sincronizacaoService;
    @Autowired
    private ThreadPoolTaskScheduler taskScheduler;

    private boolean executando = false;
    private Envio envio = Envio.NADA;
    private ScheduledFuture sincronizacaoAtual;

    public void iniciarTransmissao() {

        finalizarSincronizacao();

        Integer intervalo = configuracaoService.getIntervalo();
        if (intervalo != null) {
            sincronizacaoAtual = taskScheduler.scheduleAtFixedRate(this::executarSincronizacao, intervalo * (60000));
        }

    }

    public void executarSincronizacao() {
        if (!executando) {
            executando = true;
            try {
                Date dataDaSincronizacaoAtual = new Date();
                System.out.println("Executando: " + dataDaSincronizacaoAtual);
//                Configuracao configuracao = configuracaoService.findById(1L);
//                List<ConfiguracaoItem> configuracaoItems = configuracao.getConfiguracaoItem();
////              
//                envio = Envio.PROMOCAO;
//                tabpromocaoService.baixarPromocoes(configuracaoItems);
//                
//                envio = Envio.VENDA;
//                Date dataDaUltimaSincronizacaoDeVenda = sincronizacaoService.getDataDaUltimaSincronizacaoDeVenda();
//                notasaiService.enviarVendas(configuracaoItems, dataDaUltimaSincronizacaoDeVenda, dataDaSincronizacaoAtual);
//                sincronizacaoService.insertSincronizacaoVenda(dataDaSincronizacaoAtual);
//
//                envio = Envio.FECHAMENTO;
//                Date dataDoUltimoFechamento = sincronizacaoService.getDataDaUltimaSincronizacaoDeFechamento();
//                fechamentoPromocaoService.enviarFechamento(configuracaoItems, dataDoUltimoFechamento, dataDaSincronizacaoAtual);
//                sincronizacaoService.insertSincronizacaoFechamento(dataDaSincronizacaoAtual);
//
//                tabpromocaoService.baixarPromocoes();
                //       System.out.println("Sincronização finalizada: " + dataDaSincronizacaoAtual);
            } catch (Exception ex) {
                ex.printStackTrace();
            } finally {
                envio = Envio.NADA;
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

    public void finalizarSincronizacao() {
        if (sincronizacaoAtual != null) {
            sincronizacaoAtual.cancel(false);
            sincronizacaoAtual = null;
        }
    }

    @Bean
    public ThreadPoolTaskScheduler threadPoolTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setWaitForTasksToCompleteOnShutdown(false);
        scheduler.setRemoveOnCancelPolicy(true);
        return scheduler;
    }

    public StatusSincronizadorDto getStatus() {
        return new StatusSincronizadorDto(executando, envio);
    }

}
