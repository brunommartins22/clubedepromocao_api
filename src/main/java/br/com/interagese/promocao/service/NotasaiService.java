package br.com.interagese.promocao.service;

import br.com.firebird.models.Notasai;
import br.com.firebird.models.Notasaiitens;
import br.com.interagese.postgres.models.ConfiguracaoItem;
import br.com.interagese.postgres.models.FilialScanntech;
import br.com.interagese.promocao.enuns.StatusEnvio;
import br.com.interagese.promocao.util.ScanntechRestClient;
import java.util.Date;
import java.util.List;
import java.util.Map;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotasaiService {

    @PersistenceContext(unitName = "integradoPU")
    private EntityManager emFirebird;
    @Autowired
    private LogService logService;
    @Autowired
    private NotasaiService self;
    @Autowired
    private SincronizacaoService sincronizacaoService;

    private final ScanntechRestClient restClient;

    public boolean executando = false;

    public NotasaiService() {
        restClient = new ScanntechRestClient();
    }

    @Transactional(value = "integradoTransaction")
    public void enviarVendas(List<ConfiguracaoItem> configItens, Date dataDaSincronizacaoAtual) throws Exception {

        for (ConfiguracaoItem configuracao : configItens) {
            for (FilialScanntech filialScanntech1 : configuracao.getListaFilial()) {

                int codfil = filialScanntech1.getCodigoFilial().intValue();
                int codscanntech = filialScanntech1.getCodigoScanntech().intValue();

                Date dataDaUltimaSincronizacao = sincronizacaoService.getDataDaUltimaSincronizacaoDeVenda(codfil);

                int vendas = getQuantidadeDeVendasParaEnvio(
                        dataDaUltimaSincronizacao,
                        dataDaSincronizacaoAtual,
                        codfil
                );

                if (vendas > 0) {
                    int lote = 100;
                    int loops = vendas / lote;
                    if ((vendas % lote) != 0) {
                        loops++;
                    }

                    for (int i = 0; (i < loops && executando); i++) {
                        List<Notasai> vendasParaEnvio = getVendasParaEnvio(
                                dataDaUltimaSincronizacao,
                                dataDaSincronizacaoAtual,
                                codfil,
                                i == 0 ? i : i * lote,
                                lote
                        );

                        for (int j = 0; (j < vendasParaEnvio.size() && executando); j++) {

                            Notasai venda = vendasParaEnvio.get(j);

                            Double descontoTotal = venda.getVldescnot();
                            Double acrescimoTotal = venda.getAcrescimentoTotal();

                            //Adiciona o subsidio
                            if (venda.getVlsubsidiototal() != null) {
                                descontoTotal += venda.getVlsubsidiototal();
                            }

                            //E o desconto/acrescimo no item
                            //Detalhes da lógica de preenchimento estão no construtor
                            //da classe notasaiitens
                            for (Notasaiitens notasaiitens : venda.getNotasaiitensList()) {
                                descontoTotal += notasaiitens.getDescontoNoItem();
                                acrescimoTotal += notasaiitens.getAcrescimoNoItem();
                            }

                            venda.setDescontoTotal(descontoTotal);
                            venda.setAcrescimentoTotal(acrescimoTotal);

                            try {
                                ResponseEntity<String> response = restClient.enviarVenda(
                                        configuracao,
                                        venda,
                                        codscanntech,
                                        venda.getNrcaixa()
                                );

                                int statusCode = response.getStatusCodeValue();
                                String mensagem = "";

                                if (statusCode == 200
                                        || statusCode == 208) {

                                    venda.setEnvioscanntech(StatusEnvio.ENVIADO.getValor());
                                    self.update(venda);
                                    logService.logVenda(venda.getNumeroCupom(), venda.getNrcontr(), venda.getNrcaixa(), venda.getCodfil());

                                } else if (statusCode == 408
                                        || (statusCode >= 500 && statusCode <= 599)) {

                                    mensagem = response.getBody();

                                    venda.setObsscanntech(mensagem);
                                    venda.setEnvioscanntech(StatusEnvio.PENDENTE.getValor());
                                    self.update(venda);
                                    logService.logVendaComErro(venda.getNumeroCupom(), venda.getNrcontr(), venda.getObsscanntech(), venda.getNrcaixa(), venda.getCodfil());

                                } else {
                                    mensagem = response.getBody();
                                    venda.setEnvioscanntech(StatusEnvio.ERRO.getValor());
                                    venda.setObsscanntech(mensagem);
                                    self.update(venda);
                                    logService.logVendaComErro(venda.getNumeroCupom(), venda.getNrcontr(), venda.getObsscanntech(), venda.getNrcaixa(), venda.getCodfil());
                                }

                            } catch (Exception e) {
                                throw e;
                            }

                        }

                    }
                    
                    sincronizacaoService.insertSincronizacaoVenda(codfil, dataDaSincronizacaoAtual);
                    
                }

            }
        }

    }

    private int getQuantidadeDeVendasParaEnvio(Date dataInicio, Date dataFim, int codfil) {

        String hql = "SELECT "
                + " COUNT(n) "
                + "FROM Notasai n "
                + "WHERE "
                + "(n.codfil = :codfil) "
                + "AND (((n.dthrlanc BETWEEN :inicio AND :fim) "
                + "AND (n.envioscanntech IS NULL) "
                + "AND (n.situacao IN ('N', 'E')) "
                + "AND (n.envioscanntech IS NULL)) "
                + "OR (n.envioscanntech = 'P'))";

        TypedQuery<Number> query = emFirebird.createQuery(hql, Number.class)
                .setParameter("codfil", codfil)
                .setParameter("inicio", dataInicio)
                .setParameter("fim", dataFim);

        return query.getSingleResult().intValue();

    }

    private List<Notasai> getVendasParaEnvio(Date dataInicio, Date dataFim, int codfil, int inicio, int tamanhoDaPagina) {

        String hql = "SELECT "
                + " n "
                + "FROM Notasai n "
                + "WHERE "
                + "(n.codfil = :codfil) "
                + "AND (((n.dthrlanc BETWEEN :inicio AND :fim) "
                + "AND (n.envioscanntech IS NULL) "
                + "AND (n.situacao IN ('N', 'E')) "
                + "AND (n.envioscanntech IS NULL)) "
                + "OR (n.envioscanntech = 'P')) "
                + "ORDER BY n.dthrlanc ";

        TypedQuery<Notasai> query = emFirebird.createQuery(hql, Notasai.class);
        query.setParameter("codfil", codfil);
        query.setParameter("inicio", dataInicio);
        query.setParameter("fim", dataFim);

        //query.setFirstResult(inicio);
        query.setMaxResults(tamanhoDaPagina);

        List<Notasai> notasaiList = query.getResultList();

        setNotasaiitensList(notasaiList);

        return notasaiList;

    }

    private void setNotasaiitensList(List<Notasai> list) {

        String sql = "select  "
                + "    n.codpro,"
                + "    t.codbarun,"
                + "    n.descpro,"
                + "    sum(n.qtdvend) as qtdvend,"
                + "    n.vlunit,"
                + "    sum(n.vltotal) as vltotal,"
                + "    sum(case when n.tpdesacrite = 'D' then n.vldesacrite * n.qtdvend else 0 end) as descontoite,"
                + "    sum(case when n.tpdesacrite = 'A' then n.vldesacrite * n.qtdvend else 0 end) as acrescimoite,"
                + "    sum(case when n.tpdesacrnot = 'D' then (n.vldesacrnot + n.vlsubsidio) else 0 end) as descontonot,"
                + "    sum(case when n.tpdesacrnot = 'A' then n.vldesacrnot else 0 end) as acrescimonot "
                + "from"
                + "    notasaiitens n"
                + "        left join"
                + "            tabpro t on"
                + "                t.codpro = n.codpro "
                + "where"
                + "    n.nrcontr = :nrcontr "
                + "group by"
                + "    n.codpro,"
                + "    t.codbarun,"
                + "    n.descpro,"
                + "    n.vlunit";

        Query query = emFirebird.createNativeQuery(sql, "detalhes");

        for (Notasai notasai : list) {
            notasai.setNotasaiitensList(
                    query
                            .setParameter("nrcontr", notasai.getNrcontr())
                            .getResultList()
            );
        }

    }

    private void setRegcaixaList(List<Notasai> list) {

        String hql = "SELECT r FROM Regcaixa r WHERE r.regcaixaPK.nrcontr = :nrcontr ";

        Query query = emFirebird.createQuery(hql);

        for (Notasai notasai : list) {
            notasai.setRegcaixaList(
                    query
                            .setParameter("nrcontr", notasai.getNrcontr())
                            .getResultList()
            );
        }

    }

    private List<Map<String, Object>> getCodigoFilialAndCodigoScanntech() {
        String hql = "SELECT Map(t.codfil AS codfil, t.codscanntech AS codscanntech) "
                + "FROM Tabfil t "
                + "WHERE t.codscanntech IS NOT NULL ";

        Query query = emFirebird.createQuery(hql);
        return query.getResultList();
    }

    public void setExecutando(boolean executando) {
        this.executando = executando;
    }

    @Transactional(value = "integradoTransaction", propagation = Propagation.REQUIRES_NEW)
    public void update(Notasai notasai) {
        emFirebird.merge(notasai);
    }

}
