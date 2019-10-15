package br.com.interagese.promocao.service;

import br.com.firebird.models.Notasai;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Date;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotasaiService {

    @PersistenceContext(unitName = "integradoPU")
    private EntityManager emFirebird;

    @Autowired
    private ScanntechsincService scanntechsincService;

    private ObjectMapper mapper = new ObjectMapper();

    @Transactional("integradoTransaction")
    public void enviarVendas() throws Exception {

        Date dataDaUltimaSincronizacao = scanntechsincService.getDataDaUltimaSincronizacao();
        Date dataDaSincronizacaoAtual = new Date();

        int vendas = getQuantidadeDeVendasParaEnvio(dataDaUltimaSincronizacao, dataDaSincronizacaoAtual);

        int lote = 100;
        int loops = vendas / lote;
        if ((vendas % lote) != 0) {
            loops++;
        }

        for (int i = 0; i < loops; i++) {
            List<Notasai> vendasParaEnvio = getVendasParaEnvio(
                    dataDaUltimaSincronizacao,
                    dataDaSincronizacaoAtual,
                    i == 0 ? i : i * lote,
                    lote
            );

            for (Notasai venda : vendasParaEnvio) {
                String json = mapper.writeValueAsString(venda);
                System.out.println("Vendas: " + json);
            }

        }

        scanntechsincService.insertSincronizacao(dataDaSincronizacaoAtual);

    }

    private int getQuantidadeDeVendasParaEnvio(Date dataInicio, Date dataFim) {

        String hql = "SELECT "
                + " COUNT(n) "
                + "FROM Notasai "
                + "WHERE "
                + "(n.dthrlanc BETWEEN :inicio AND :fim) "
                + "AND (n.codscanntech IS NOT NULL "
                + "AND (n.envioscanntech IS NULL OR n.envioscanntech = 'P') )";

        TypedQuery<Number> query = emFirebird.createQuery(hql, Number.class)
                .setParameter("inicio", dataInicio)
                .setParameter("fim", dataFim);

        return query.getSingleResult().intValue();

    }

    private List<Notasai> getVendasParaEnvio(Date dataInicio, Date dataFim, int inicio, int tamanhoDaPagina) {

        String hql = "SELECT "
                + " n "
                + "FROM Notasai "
                + "WHERE "
                + "(n.dthrlanc BETWEEN :inicio AND :fim) "
                + "AND (n.codscanntech IS NOT NULL "
                + "AND (n.envioscanntech IS NULL OR n.envioscanntech = 'P') )";

        TypedQuery<Notasai> query = emFirebird.createQuery(hql, Notasai.class);
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

        String hql = "SELECT n FROM Notasaiitens WHERE n.notasaiitensPK.nrcontr = :nrcontr ";

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

        String hql = "SELECT n FROM Regcaixa WHERE n.regcaixaPK.nrcontr = :nrcontr ";

        Query query = emFirebird.createQuery(hql);

        for (Notasai notasai : list) {
            notasai.setRegcaixaList(
                    query
                            .setParameter("nrcontr", notasai.getNrcontr())
                            .getResultList()
            );
        }

    }

}
