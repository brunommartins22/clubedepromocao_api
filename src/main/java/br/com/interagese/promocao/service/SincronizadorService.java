package br.com.interagese.promocao.service;

import br.com.interagese.postgres.dtos.StatusSincronizadorDto;
import br.com.interagese.postgres.models.Configuracao;
import br.com.interagese.postgres.models.ConfiguracaoItem;
import br.com.interagese.promocao.enuns.Envio;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;

@Service
public class SincronizadorService {
    
    private final static Logger LOGGER = LogManager.getFormatterLogger(SincronizadorService.class);

    @Autowired
    private TabpromocaoService tabpromocaoService;
    @Autowired
    private NotasaiService notasaiService;
    @Autowired
    private FechamentoPromocaoService fechamentoPromocaoService;
    @Autowired
    private ConfiguracaoService configuracaoService;
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
            notasaiService.setExecutando(true);
            try {
                Date dataDaSincronizacaoAtual = new Date();
                System.out.println("Executando: " + dataDaSincronizacaoAtual);
                
                LOGGER.info("Iniciando sincronizacao: " + dataDaSincronizacaoAtual);
                
                Configuracao configuracao = configuracaoService.findById(1L);
                List<ConfiguracaoItem> configuracaoItems = configuracao.getConfiguracaoItem();
//              
                envio = Envio.PROMOCAO;
                if (executando) {
                    //Thread.sleep(5000);
                 //   tabpromocaoService.baixarPromocoes(configuracaoItems);
                }

                envio = Envio.VENDA;
                if (executando) {
                    //Thread.sleep(15000);
                    notasaiService.enviarVendas(configuracaoItems, dataDaSincronizacaoAtual);
                   // sincronizacaoService.insertSincronizacaoVenda(dataDaSincronizacaoAtual);
                }

                envio = Envio.FECHAMENTO;
                if (executando) {
                    Thread.sleep(5000);
                    //fechamentoPromocaoService.enviarFechamento(configuracaoItems, dataDaSincronizacaoAtual);

                }

                LOGGER.info("Sincronização finalizada");
            } catch (Exception ex) {
                LOGGER.error("Erro ao realizar sincronização: ", ex);
                ex.printStackTrace();
            } finally {
                envio = Envio.NADA;
                executando = false;
            }
        }
    }

    @EventListener
    public void onApplicationEvent(ContextRefreshedEvent e) {
        iniciarTransmissao();
    }

    public void finalizarSincronizacao() {
        if (sincronizacaoAtual != null) {
            sincronizacaoAtual.cancel(false);
            sincronizacaoAtual = null;
            executando = false;
            notasaiService.setExecutando(false);
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
