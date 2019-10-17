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
import javax.annotation.Resource;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import org.hibernate.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;

@Service
public class NotasaiService {

    @PersistenceContext(unitName = "integradoPU")
    private EntityManager emFirebird;
    @Autowired
    private ConfiguracaoService configuracaoService;
    @Autowired
    private LogService logService;
    @Autowired
    private NotasaiService self;

    private final ScanntechRestClient restClient;

    public NotasaiService() {
        restClient = new ScanntechRestClient();
    }

    @Transactional(value = "integradoTransaction")
    public void enviarVendas(List<ConfiguracaoItem> configItens, Date dataDaUltimaSincronizacao, Date dataDaSincronizacaoAtual) throws Exception {

        for (ConfiguracaoItem configuracao : configItens) {
            for (FilialScanntech filialScanntech1 : configuracao.getListaFilial()) {

                int codfil = filialScanntech1.getCodigoFilial().intValue();
                int codscanntech = filialScanntech1.getCodigoScanntech().intValue();

                int vendas = getQuantidadeDeVendasParaEnvio(
                        dataDaUltimaSincronizacao,
                        dataDaSincronizacaoAtual,
                        codfil
                );

                int lote = 100;
                int loops = vendas / lote;
                if ((vendas % lote) != 0) {
                    loops++;
                }

                for (int i = 0; i < loops; i++) {
                    List<Notasai> vendasParaEnvio = getVendasParaEnvio(
                            dataDaUltimaSincronizacao,
                            dataDaSincronizacaoAtual,
                            codfil,
                            i == 0 ? i : i * lote,
                            lote
                    );

                    for (Notasai venda : vendasParaEnvio) {

                        venda.setDescontoTotal(venda.getVldescnot());
                        
                        for (Notasaiitens notasaiitens : venda.getNotasaiitensList()) {

                            venda.setDescontoTotal(notasaiitens.getDesconto() + venda.getDescontoTotal());
                            venda.setAcrescimentoTotal(notasaiitens.getAcrescimo() + venda.getAcrescimentoTotal());

                        }

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
                                logService.logVenda(venda.getNrnotaf(), venda.getNrcaixa(), venda.getCodfil());

                            } else if (statusCode == 408
                                    || (statusCode >= 500 && statusCode <= 599)) {

                                mensagem = response.getBody();
                                
                                venda.setObsscanntech(mensagem);
                                venda.setEnvioscanntech(StatusEnvio.PENDENTE.getValor());
                                self.update(venda);
                                logService.logVendaComErro(venda.getNrnotaf(), venda.getObsscanntech(), venda.getNrcaixa(), venda.getCodfil());

                            } else {
                                mensagem = response.getBody();
                                venda.setEnvioscanntech(StatusEnvio.ERRO.getValor());
                                venda.setObsscanntech(mensagem);
                                self.update(venda);
                                logService.logVendaComErro(venda.getNrnotaf(), venda.getObsscanntech(), venda.getNrcaixa(), venda.getCodfil());
                            }

                        } catch (Exception e) {
                            throw e;
                        }

                    }

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
                + "AND (n.situacao IN ('N', 'E'))"
                + "AND (n.nrnotaf IS NOT NULL AND n.nrnotaf <> ''))"
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
                + "AND (n.situacao IN ('N', 'E'))"
                + "AND (n.nrnotaf IS NOT NULL AND n.nrnotaf <> ''))"
                + "OR (n.envioscanntech = 'P'))";

        TypedQuery<Notasai> query = emFirebird.createQuery(hql, Notasai.class);
        query.setParameter("codfil", codfil);
        query.setParameter("inicio", dataInicio);
        query.setParameter("fim", dataFim);

        query.setFirstResult(inicio);
        query.setMaxResults(tamanhoDaPagina);

        List<Notasai> notasaiList = query.getResultList();

        setNotasaiitensList(notasaiList);
        setRegcaixaList(notasaiList);

        return notasaiList;

    }

    private void setNotasaiitensList(List<Notasai> list) {

        String hql = "SELECT n FROM Notasaiitens n WHERE n.notasaiitensPK.nrcontr = :nrcontr ";

        Query query = emFirebird.createQuery(hql);

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

    @Transactional(value = "integradoTransaction", propagation = Propagation.REQUIRES_NEW)
    public void update(Notasai notasai) {
        emFirebird.unwrap(Session.class).update(notasai);
    }

}
