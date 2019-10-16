package br.com.interagese.promocao.service;

import br.com.firebird.models.Notasai;
import br.com.firebird.models.Notasaiitens;
import br.com.interagese.postgres.models.Configuracao;
import br.com.interagese.postgres.models.FilialScanntech;
import br.com.interagese.postgres.models.Url;
import br.com.interagese.promocao.enuns.StatusEnvio;
import br.com.interagese.promocao.util.ScanntechRestClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotasaiService {

    @PersistenceContext(unitName = "integradoPU")
    private EntityManager emFirebird;

    @Autowired
    private SincronizacaoService scanntechsincService;

    private final ObjectMapper mapper;
    private final ScanntechRestClient restClient;

    public NotasaiService() {
        mapper = new ObjectMapper();
        restClient = new ScanntechRestClient();
    }

    @Transactional("integradoTransaction")
    public void enviarVendas() throws Exception {

        Date dataDaUltimaSincronizacao = scanntechsincService.getDataDaUltimaSincronizacaoDeVenda();
        Date dataDaSincronizacaoAtual = new Date();

        //Configuracao de teste
        Configuracao configuracao = new Configuracao();
        configuracao.setCodigoEmpresa("31672");

        Url url = new Url();
        url.setValor("http://br.homo.apipdv.scanntech.com");
        configuracao.setListaUrl(Arrays.asList(url));

        FilialScanntech f = new FilialScanntech();
        f.setCodigoFilial(1L);
        f.setCodigoScanntech(1L);
        configuracao.setListaFilial(Arrays.asList(f));

        configuracao.setUsuario("integrador_test@interagese.com.br");
        configuracao.setSenha("integrador");

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

                    for (Notasaiitens notasaiitens : venda.getNotasaiitensList()) {
                        
                        venda.setDescontoTotal(notasaiitens.getDesconto() + venda.getDescontoTotal());
                        venda.setAcrescimentoTotal(notasaiitens.getAcrescimo() + venda.getAcrescimentoTotal());
                        
                    }
                    
                    ResponseEntity<String> response = restClient.enviarVenda(
                            configuracao,
                            venda,
                            codscanntech,
                            venda.getNrcaixa()
                    );

                    if (response.getStatusCode() == HttpStatus.OK
                            || response.getStatusCode() == HttpStatus.ALREADY_REPORTED) {

                        venda.setEnvioscanntech(StatusEnvio.ENVIADO.getValor());
                        
                    } else if (response.getStatusCode() == HttpStatus.REQUEST_TIMEOUT
                            || (response.getStatusCodeValue() >= 500 && response.getStatusCodeValue() <= 599)) {

                        venda.setObsscanntech(response.getBody());
                        venda.setEnvioscanntech(StatusEnvio.PENDENTE.getValor());
                        
                    }else{
                        venda.setEnvioscanntech(StatusEnvio.ERRO.getValor());
                    }
                    
                    update(venda);
                    
                }

            }

            scanntechsincService.insertSincronizacaoVenda(dataDaSincronizacaoAtual);
        }

    }

    private int getQuantidadeDeVendasParaEnvio(Date dataInicio, Date dataFim, int codfil) {

        String hql = "SELECT "
                + " COUNT(n) "
                + "FROM Notasai n "
                + "WHERE "
                + "((n.codfil = :codfil) "
                + "AND (n.dthrlanc BETWEEN :inicio AND :fim) "
                + "AND (n.envioscanntech IS NULL OR n.envioscanntech = 'P') "
                + "AND (n.nrnotaf IS NOT NULL AND n.nrnotaf <> ''))";

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
                + "((n.codfil = :codfil) "
                + "AND (n.dthrlanc BETWEEN :inicio AND :fim) "
                + "AND (n.envioscanntech IS NULL OR n.envioscanntech = 'P') "
                + "AND (n.nrnotaf IS NOT NULL AND n.nrnotaf <> ''))";

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
    
    private void update(Notasai notasai){
        emFirebird.merge(notasai);
    }

}
