package br.com.interagese.promocao.service;

import br.com.interagese.padrao.rest.util.PadraoService;
import br.com.interagese.postgres.models.ConfiguracaoItem;
import br.com.interagese.postgres.models.FechamentoPromocao;
import br.com.interagese.postgres.models.FilialScanntech;
import br.com.interagese.promocao.enuns.StatusEnvio;
import br.com.interagese.promocao.util.ScanntechRestClient;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import static java.util.stream.Collectors.*;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

@Service
public class FechamentoPromocaoService extends PadraoService<FechamentoPromocao> {

    @PersistenceContext(unitName = "integradoPU")
    private EntityManager emFirebird;
    @Autowired
    private ConfiguracaoService configuracaoService;

    private final ScanntechRestClient restClient;
    private final SimpleDateFormat dbDateFormat;

    public FechamentoPromocaoService() {
        restClient = new ScanntechRestClient();
        dbDateFormat = new SimpleDateFormat("yyyy-MM-dd");
    }

    public void enviarFechamento(Date dataDaUltimaSincronizacao, Date dataDaSincronizacaoAtual) throws Exception {
        //Configuracao de teste
      
        for (ConfiguracaoItem configuracao : configuracaoService.findById(((Integer)1).longValue()).getConfiguracaoItem()) {
            for (FilialScanntech filialScanntech1 : configuracao.getListaFilial()) {

                int codfil = filialScanntech1.getCodigoFilial().intValue();
                int codscanntech = filialScanntech1.getCodigoScanntech().intValue();

                List<FechamentoPromocao> fechamentos = getFechamentos(
                        dataDaUltimaSincronizacao,
                        dataDaSincronizacaoAtual,
                        codfil,
                        null
                );

                for (FechamentoPromocao fechamento : fechamentos) {

                    fechamento.setCodigoScanntech(Integer.valueOf(codscanntech).longValue());

                    int statusCode = 0;
                    String message = "";

                    try {
                        ResponseEntity<String> response = restClient.enviarFechamento(
                                configuracao,
                                fechamento,
                                codscanntech,
                                fechamento.getNumeroCaixa().intValue()
                        );

                        statusCode = response.getStatusCodeValue();
                        message = response.getBody();

                    } catch (HttpClientErrorException e) {

                        statusCode = e.getRawStatusCode();
                        message = e.getResponseBodyAsString();

                    }

                    if (statusCode == 200
                            || statusCode == 208) {

                        fechamento.setEnvioScanntech(StatusEnvio.ENVIADO.getValor());

                    } else if (statusCode == 408
                            || (statusCode >= 500 && statusCode <= 599)) {

                        fechamento.setObsScanntech(message);
                        fechamento.setEnvioScanntech(StatusEnvio.PENDENTE.getValor());

                    } else {
                        fechamento.setEnvioScanntech(StatusEnvio.ERRO.getValor());
                    }

                    create(fechamento);
                }

            }
        }

    }

    private List<FechamentoPromocao> getFechamentos(Date dataInicio, Date dataFim, Integer codfil, Integer nrcaixa) {

        StringBuilder hql = new StringBuilder("select "
                + "    codfil as codfil, "
                + "    nrcaixa as nrcaixa, "
                + "    dtemissao as data_fechamento, "
                + "    sum(case when n.situacao in ('N', 'E') then n.totnota else 0 end) as valor_total_vendas, "
                + "    sum(case when n.situacao in ('N', 'E') then 1 else 0 end) as quantidade_vendas, "
                + "    sum(case when n.situacao in ('C', 'A') then n.totnota else 0 end) as valor_total_cancelamentos, "
                + "    sum(case when n.situacao in ('C', 'A') then 1 else 0 end) as quantidade_cancelamentos "
                + "        from "
                + "    notasai n "
                + "        where ");

        if (dataFim == null && dataInicio != null) {
            hql.append(" n.dtemissao = '").append(dbDateFormat.format(dataInicio)).append("' ");
        } else {
            hql.append(" (n.dtemissao >= '").append(dbDateFormat.format(dataInicio))
                    .append("' AND n.dtemissao < '").append(dbDateFormat.format(dataFim)).append("' ");
        }

        if (nrcaixa != null) {
            hql.append(" AND (n.nrcaixa = :nrcaixa) "
                    + "AND (n.envioscanntech = 'E') ");
        }

        hql.append(" AND (n.codfil = :codfil ) ")
                .append("        group by "
                        + "            n.dtemissao, "
                        + "            n.codfil, "
                        + "            n.nrcaixa");

        Query query = emFirebird.createNativeQuery(hql.toString());
        query.setParameter("codfil", codfil);

        if (nrcaixa != null) {
            query.setParameter("nrcaixa", nrcaixa);
        }

        List<Object[]> results = query.getResultList();

        return results.stream()
                .map((result) -> {
                    return new FechamentoPromocao(
                            (Number) result[0], //codfil
                            (Number) result[1], //nrcaixa
                            (Date) result[2], //dtfechamento
                            (Number) result[3], //valor total de vendas
                            (Number) result[4], //quantidade de vendas
                            (Number) result[5], //valor total de cancelamentos
                            (Number) result[6] //quantiade de cancelamento
                    );
                })
                .collect(toList());

    }

}
