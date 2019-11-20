package br.com.interagese.promocao.service;

import br.com.interagese.postgres.dtos.StatusSincronizadorDto;
import br.com.interagese.postgres.models.Configuracao;
import br.com.interagese.postgres.models.ConfiguracaoItem;
import br.com.interagese.promocao.enuns.Envio;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
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
    private String log = "";
    private ScheduledFuture sincronizacaoAtual;

    public void iniciarSincronizacao() {

        finalizarSincronizacao();

        Integer intervalo = configuracaoService.getIntervalo();
        if (intervalo != null) {
            sincronizacaoAtual = taskScheduler.scheduleAtFixedRate(this::executarSincronizacao, intervalo * (60000));
        }

    }

    public void executarSincronizacao() {
        if (!executando) {
            log = "";
            executando = true;
            notasaiService.setExecutando(true);
            try {
                Date dataDaSincronizacaoAtual = new Date();
                System.out.println("Executando: " + dataDaSincronizacaoAtual);

                LOGGER.info("Iniciando sincronizacao: " + dataDaSincronizacaoAtual);

                Configuracao configuracao = configuracaoService.findById(1L);
                List<ConfiguracaoItem> configuracaoItems = configuracao.getConfiguracaoItem();

                if (!configuracaoItems.isEmpty()) {

                    envio = Envio.PROMOCAO;
                    if (executando) {
                       tabpromocaoService.baixarPromocoes(configuracaoItems);
                    }

                    envio = Envio.VENDA;
                    if (executando) {
                        notasaiService.setExecutando(true);
                        notasaiService.enviarVendas(configuracaoItems, configuracao.getPrimeiraSincronizacao());
                    }

                    envio = Envio.FECHAMENTO;
                    if (executando) {
                        fechamentoPromocaoService.enviarFechamento(configuracaoItems, dataDaSincronizacaoAtual);

                    }

                } else {
                    LOGGER.warn("Configurações scanntech não informadas!");
                }
//             
                envio = Envio.NADA;
                LOGGER.info("Sincronização finalizada");
            } catch (Exception ex) {
                LOGGER.error("Erro ao realizar sincronização: ", ex);
                envio = Envio.ERRO;
                StringWriter writer = new StringWriter();
                PrintWriter pw = new PrintWriter(writer);
                ex.printStackTrace(pw);
                log = writer.toString();

            } finally {
                executando = false;
            }

        }
    }

    public void sincronizarVendas() {
        if (!executando) {
            log = "";
            executando = true;
            Configuracao configuracao = configuracaoService.findById(1L);
            List<ConfiguracaoItem> configuracaoItems = configuracao.getConfiguracaoItem();

            try {
                envio = Envio.VENDA;
                if (!configuracaoItems.isEmpty()) {
                    this.notasaiService.setExecutando(true);
                    this.notasaiService.enviarVendas(configuracaoItems, configuracao.getPrimeiraSincronizacao());
                }

                envio = Envio.NADA;

                LOGGER.info("Sincronização de vendas finalizada");

            } catch (Exception ex) {
                envio = Envio.ERRO;
                StringWriter writer = new StringWriter();
                PrintWriter pw = new PrintWriter(writer);
                ex.printStackTrace(pw);
                log = writer.toString();
                LOGGER.error("Erro ao realizar sincronização: ", ex);
            } finally {
                executando = false;
            }

        }
    }

    public void sincronizarPromocao() {
        if (!executando) {
            log = "";
            executando = true;
            Configuracao configuracao = configuracaoService.findById(1L);
            List<ConfiguracaoItem> configuracaoItems = configuracao.getConfiguracaoItem();

            try {
                envio = Envio.PROMOCAO;
                if (!configuracaoItems.isEmpty()) {
                    this.tabpromocaoService.baixarPromocoes(configuracaoItems);
                }

                envio = Envio.NADA;

                LOGGER.info("Sincronização de promoção finalizada");

            } catch (Exception ex) {
                envio = Envio.ERRO;
                StringWriter writer = new StringWriter();
                PrintWriter pw = new PrintWriter(writer);
                ex.printStackTrace(pw);
                log = writer.toString();
                LOGGER.error("Erro ao realizar sincronização: ", ex);
            } finally {
                executando = false;
            }

        }
    }

    public void reenviarFechamento(Date dataInicio, Date dataFim, Integer nrcaixa) {
        if (!executando) {
            executando = true;
            Configuracao configuracao = configuracaoService.findById(1L);
            List<ConfiguracaoItem> configuracaoItems = configuracao.getConfiguracaoItem();

            try {
                envio = Envio.FECHAMENTO;
                if (!configuracaoItems.isEmpty()) {

                    fechamentoPromocaoService.reenviarFechamento(configuracaoItems, dataInicio, dataFim, nrcaixa);

                }

                envio = Envio.NADA;

                LOGGER.info("Reenvio de fechamento finalizado");

            } catch (Exception ex) {
                envio = Envio.ERRO;
                ex.printStackTrace();
                LOGGER.error("Erro ao reenviar fechamento: ", ex);
            } finally {
                executando = false;
            }

        }
    }

    public void desmarcarVendas(Date dataInicio, Date dataFim) throws Exception {

        try {
            notasaiService.desmarcarVendas(dataInicio, dataFim);
        } catch (Exception ex) {
            ex.printStackTrace();
            LOGGER.error("Erro ao desmarcar vendas: ", ex);
            throw ex;
        }
    }

    @EventListener
    public void onApplicationEvent(ContextRefreshedEvent e) {
        iniciarSincronizacao();
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
        return new StatusSincronizadorDto(executando, envio, log);
    }

}
