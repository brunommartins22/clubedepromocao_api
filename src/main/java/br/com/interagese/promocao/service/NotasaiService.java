package br.com.interagese.promocao.service;

import br.com.firebird.models.Notasai;
import br.com.firebird.models.Notasaiitens;
import br.com.interagese.postgres.models.ConfiguracaoItem;
import br.com.interagese.postgres.models.FilialScanntech;
import br.com.interagese.promocao.enuns.StatusEnvio;
import br.com.interagese.promocao.util.ScanntechRestClient;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
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

    private SimpleDateFormat dbDateFormat;

    public boolean executando = false;

    public NotasaiService() {
        restClient = new ScanntechRestClient();
        dbDateFormat = new SimpleDateFormat("yyyy-MM-dd");
    }

    @Transactional(value = "integradoTransaction")
    public void enviarVendas(List<ConfiguracaoItem> configItens, Date dataDaPrimeiraSincronizacao) throws Exception {

        if (dataDaPrimeiraSincronizacao == null) {
            throw new Exception("Data da primeira sincronização não informada");
        }

        try {
            for (ConfiguracaoItem configuracao : configItens) {
                for (FilialScanntech filialScanntech1 : configuracao.getListaFilial()) {

                    int codfil = filialScanntech1.getCodigoFilial().intValue();
                    int codscanntech = filialScanntech1.getCodigoScanntech().intValue();

                    int quantidadeDeVendas = getQuantidadeDeVendasParaEnvio(
                            dataDaPrimeiraSincronizacao,
                            codfil
                    );

                    if (quantidadeDeVendas > 0) {
                        int lote = 100;
                        int loops = quantidadeDeVendas / lote;
                        if ((quantidadeDeVendas % lote) != 0) {
                            loops++;
                        }

                        for (int i = 0; (i < loops && executando); i++) {
                            List<Notasai> vendas = getVendasParaEnvio(
                                    dataDaPrimeiraSincronizacao,
                                    codfil,
                                    i == 0 ? i : i * lote,
                                    lote
                            );

                            for (int j = 0; (j < vendas.size() && executando); j++) {

                                Notasai vendaParaEnvio = vendas.get(j);

                                //Vai armazenar as vendas uqe serão enviadas nessa iteração
                                List<Notasai> vendasParaEnvio = new ArrayList();

                                //A venda é um cancelmento normal (1 item)
                                if (vendaParaEnvio.getSituacao().equals("C")
                                        && (vendaParaEnvio.getNrcontr01() != null && !vendaParaEnvio.getNrcontr01().isEmpty())
                                        && (vendaParaEnvio.getNrcontr02() == null || vendaParaEnvio.getNrcontr02().isEmpty())) {

                                    //Pesquisa a venda cancelada e marca como cancelamento para a scanntech
                                    Notasai vendaCancelada = loadVenda(vendaParaEnvio.getNrcontr01());
                                    vendaCancelada.setCancelada(true);

                                    //Não deve ser enviada para a scanntech
                                    vendaParaEnvio.setDeveSerEnviada(false);

                                    vendasParaEnvio.add(vendaCancelada);
                                    vendasParaEnvio.add(vendaParaEnvio);

                                    //A venda é uma alteração ou uma alteração que foi cancelada (2 itens)
                                } else if (vendaParaEnvio.getSituacao().equals("E")
                                        || (vendaParaEnvio.getSituacao().equals("C")
                                        && (vendaParaEnvio.getNrcontr01() != null && !vendaParaEnvio.getNrcontr01().isEmpty())
                                        && (vendaParaEnvio.getNrcontr02() != null && !vendaParaEnvio.getNrcontr02().isEmpty()))) {

                                    //Pesquisa a venda alterada e marca como cancelamento para a scanntech
                                    Notasai vendaAlterada = loadVenda(vendaParaEnvio.getNrcontr01());
                                    vendaAlterada.setCancelada(true);

                                    //Envia as duas para a scanntech
                                    vendasParaEnvio.add(vendaAlterada);
                                    vendasParaEnvio.add(vendaParaEnvio);

                                    //A venda é uma alteração cancelada    
                                } else {
                                    vendasParaEnvio.add(vendaParaEnvio);
                                }

                                for (Notasai venda : vendasParaEnvio) {

                                    if (venda.deveSerEnviada()) {
                                        Double descontoTotal = venda.getVldescnot();
                                        Double acrescimoTotal = venda.getAcrescimentoTotal();

                                        //Adiciona o subsidio
                                        if (venda.getVlsubsidiototal() != null) {
                                            descontoTotal += venda.getVlsubsidiototal();
                                        }

                                        //E o desconto/acrescimo no item se não tiver promocao
                                        for (Notasaiitens notasaiitens : venda.getNotasaiitensList()) {
                                            //Detalhes da lógica de preenchimento estão no construtor
                                            //da classe notasaiitens
                                            if (notasaiitens.getDescontoPromo() == 0.0) {
                                                descontoTotal += notasaiitens.getDescontoNoItem();
                                                acrescimoTotal += notasaiitens.getAcrescimoNoItem();
                                            }

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
                                                logService.logVenda(
                                                        venda.getNumeroCupom(),
                                                        venda.getNrcontr(),
                                                        venda.getNrcaixa(),
                                                        venda.getCodfil(),
                                                        codscanntech,
                                                        venda.isCancelada()
                                                );

                                            } else if (statusCode == 408
                                                    || (statusCode >= 500 && statusCode <= 599)) {

                                                mensagem = response.getBody();

                                                venda.setObsscanntech(mensagem);
                                                venda.setEnvioscanntech(StatusEnvio.PENDENTE.getValor());
                                                self.update(venda);
                                                logService.logVendaComErro(
                                                        venda.getNumeroCupom(),
                                                        venda.getNrcontr(),
                                                        venda.getObsscanntech(),
                                                        venda.getNrcaixa(),
                                                        venda.getCodfil(),
                                                        codscanntech,
                                                        venda.isCancelada()
                                                );

                                            } else {
                                                mensagem = response.getBody();
                                                venda.setEnvioscanntech(StatusEnvio.ERRO.getValor());
                                                venda.setObsscanntech(mensagem);
                                                self.update(venda);
                                                logService.logVendaComErro(
                                                        venda.getNumeroCupom(),
                                                        venda.getNrcontr(),
                                                        venda.getObsscanntech(),
                                                        venda.getNrcaixa(),
                                                        venda.getCodfil(),
                                                        codscanntech,
                                                        venda.isCancelada()
                                                );
                                            }

                                        } catch (Exception e) {
                                            throw e;
                                        }

                                    } else {

                                        //Se teve uma venda de origem, recebe o mesmo status da venda
                                        if (vendasParaEnvio.size() == 2) {
                                            venda.setEnvioscanntech(vendasParaEnvio.get(0).getEnvioscanntech());
                                            self.update(venda);
                                        }

                                    }

                                }

                            }

                            sincronizacaoService.insertSincronizacaoVenda(codfil, new Date());
                        }

                    }

                }
            }
        } catch (Exception e) {
            throw new Exception("Erro ao enviar vendas", e);
        }

    }

    private int getQuantidadeDeVendasParaEnvio(Date dataPrimeiraSincronizacao, int codfil) {

        String hql = "SELECT "
                + " COUNT(n) "
                + "FROM Notasai n "
                + "WHERE "
                + "(n.codfil = :codfil) "
                + "AND (((n.dthrlanc > :fim) "
                + "AND (n.envioscanntech IS NULL)) "
                + "OR (n.envioscanntech = 'P'))";

        TypedQuery<Number> query = emFirebird.createQuery(hql, Number.class)
                .setParameter("codfil", codfil)
                // .setParameter("inicio", dataInicio)
                .setParameter("fim", dataPrimeiraSincronizacao);

        return query.getSingleResult().intValue();

    }

    private List<Notasai> getVendasParaEnvio(Date dataPrimeiraSincronizacao, int codfil, int inicio, int tamanhoDaPagina) {

        String hql = "SELECT "
                + " n "
                + "FROM Notasai n "
                + "WHERE "
                + "(n.codfil = :codfil) "
                + "AND (((n.envioscanntech IS NULL) "
                + "AND (n.dthrlanc > :fim)) "
                + "OR (n.envioscanntech = 'P')) "
                + "ORDER BY n.dthrlanc ";

        TypedQuery<Notasai> query = emFirebird.createQuery(hql, Notasai.class);
        query.setParameter("codfil", codfil);
        query.setParameter("fim", dataPrimeiraSincronizacao);

        //query.setFirstResult(inicio);
        query.setMaxResults(tamanhoDaPagina);

        List<Notasai> notasaiList = query.getResultList();

        setNotasaiitensList(notasaiList);

        return notasaiList;

    }

    private Notasai loadVenda(String nrcontr) throws Exception {

        String hql = "SELECT "
                + " n "
                + "FROM Notasai n "
                + "WHERE "
                + "nrcontr = :nrcontr ";

        TypedQuery<Notasai> query = emFirebird.createQuery(hql, Notasai.class);
        query.setParameter("nrcontr", nrcontr);

        List<Notasai> notasaiList = query.getResultList();

        if (notasaiList.isEmpty()) {
            throw new RuntimeException("Não foi encontrada a venda informada");
        }

        setNotasaiitensList(notasaiList);

        return notasaiList.get(0);

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
                + "    sum(case when n.tpdesacrnot = 'A' then n.vldesacrnot else 0 end) as acrescimonot, "
                + "    sum(n.vlsubsidio) as descontopromo "
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

    @Transactional(value = "integradoTransaction")
    public void desmarcarVendas(Date dataInicial, Date dataFinal) {

        String sql = "UPDATE notasai SET envioscanntech = null ";

        if (dataInicial != null && dataFinal == null) {
            sql += " WHERE dtemissao = '" + dbDateFormat.format(dataInicial) + "' ";
        } else if (dataInicial == null && dataFinal != null) {
            sql += " WHERE dtemissao = '" + dbDateFormat.format(dataFinal) + "' ";
        } else if(dataInicial != null && dataFinal != null) {
            sql += " WHERE dtemissao BETWEEN  '" + dbDateFormat.format(dataInicial) + "' AND '" + dbDateFormat.format(dataFinal) + "' ";
        }else{
            throw new RuntimeException("Não foi informado nenhuma data.");
        }

        emFirebird.createNativeQuery(sql)
                .executeUpdate();

    }

}
