package br.com.interagese.promocao.service;

import br.com.firebird.models.Tabpromocao;
import br.com.firebird.models.Tabpromoitem;
import br.com.interagese.padrao.rest.util.TransformNativeQuery;
import br.com.interagese.postgres.models.ConfiguracaoItem;
import br.com.interagese.postgres.models.FilialScanntech;
import br.com.interagese.postgres.models.SincronizacaoPromocao;
import br.com.interagese.promocao.enuns.EstadoPromocao;
import br.com.interagese.promocao.util.ScanntechRestClient;
import br.com.interagese.util.CalcUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.sun.glass.events.ViewEvent;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;

@Service
public class TabpromocaoService {

    @PersistenceContext(unitName = "integradoPU")
    private EntityManager emFirebird;
    @Autowired
    private ConfiguracaoService configuracaoService;
    @Autowired
    private TabpromoItemService tabpromoItemService;

    @PersistenceContext(unitName = "default")
    private EntityManager em;

    private final SimpleDateFormat dateFormat;
    private final ScanntechRestClient scanntechRestClient;

    public TabpromocaoService() {
        this.dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        this.scanntechRestClient = new ScanntechRestClient();
    }

    @Transactional("integradoTransaction")
    public Map findTabpromocaoByFilters(Map map) throws Exception {

        Integer codigoFilial = (Integer) map.get("codigoFilial");
        Integer tipo = (Integer) map.get("tipo");
        String situacao = (String) map.get("situacao");
        String tituloPromocao = (String) map.get("tituloPromocao");
        String autorPromocao = (String) map.get("autorPromocao");
        List<String> datasValidade = (List<String>) map.get("validade");
        int inicial = (Integer) map.get("inicial");
        int finalR = (Integer) map.get("final");

        String sql = "select distinct tp.* from tabpromocaofilial tpf "
                + "right join tabpromocao tp on tpf.codpromocao = tp.codpromocao "
                + "where tp.rgevento <> 3";

        String countSql = "select count(distinct tp.codpromocao) from tabpromocaofilial tpf "
                + "right join tabpromocao tp on tpf.codpromocao = tp.codpromocao "
                + "where tp.rgevento <> 3";

        String sqlGenerica = "";

        if (codigoFilial != null) {
            sqlGenerica += " and tpf.codfil =" + codigoFilial;
        }

        if (tipo != null) {
            sqlGenerica += " and tp.tipo = " + tipo;
        }

        if (!StringUtils.isEmpty(situacao)) {
            sqlGenerica += " and tp.situacao ='" + situacao + "'";
        }

        if (!StringUtils.isEmpty(tituloPromocao)) {
            sqlGenerica += " and tp.titulo like '" + tituloPromocao + "%'";
        }

        if (!StringUtils.isEmpty(autorPromocao)) {
            sqlGenerica += " and tp.autor like '" + autorPromocao + "%'";
        }

        if (!datasValidade.isEmpty()) {
            SimpleDateFormat dateFormatDate = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

            String date1 = datasValidade.get(0);
            String date2 = datasValidade.get(1);

            if (dateFormatDate.parse(date1).after(new Date())) {
                throw new Exception("Data inicial não pode ser superior a data atual : " + new SimpleDateFormat("dd/MM/yyyy").format(new Date()));
            }

            if (date1 != null && date2 != null) {

                if (dateFormatDate.parse(date2).after(new Date())) {
                    throw new Exception("Data final não pode ser superior a data atual : " + new SimpleDateFormat("dd/MM/yyyy").format(new Date()));
                }

                sqlGenerica += " and tp.datainicio >= '" + dateFormat.format(dateFormatDate.parse(date1)) + "' and tp.datafim <= '" + dateFormat.format(dateFormatDate.parse(date2)) + "'";

            } else {

                sqlGenerica += " and tp.datainicio >='" + dateFormat.format(dateFormatDate.parse(date1)) + "'";

            }
        }

        Long count = ((Number) emFirebird.createNativeQuery(countSql + sqlGenerica).getSingleResult()).longValue();
        Integer countAtivos = 0;
        Integer countInativos = 0;
        Integer countPendentes = 0;
        Integer countRejeitados = 0;
        List<Tabpromocao> result = new ArrayList<>();
        if (count > 0) {
            TypedQuery<Tabpromocao> resp = (TypedQuery<Tabpromocao>) emFirebird.createNativeQuery(sql+sqlGenerica, Tabpromocao.class).setFirstResult(inicial).setMaxResults(finalR);

            for (Tabpromocao tabpromocao : resp.getResultList()) {

                switch (tabpromocao.getTipo()) {
                    case 2: {
                        tabpromocao.setItemList(getItensDaPromocaoLevaPaga(tabpromocao.getCodpromocao()));
                        calcularPercentagemLucroLevaPaga(tabpromocao);
                        tabpromocao.setResumoPromo("Levando a quantidade informada de um dos produto(s) abaixo,"
                                + " é pago apenas " + tabpromocao.getPaga() + " unidade(s)");
                        break;
                    }
                    case 3: {
                        tabpromocao.setItemList(getItensDaPromocaoDescontoVariavel(tabpromocao.getCodpromocao()));
                        calcularPercentagemLucroDescontoVariavel(tabpromocao);
                        tabpromocao.setResumoPromo("Levando a quantidade informada de um dos produtos abaixo. "
                                + "Se aplica um desconto percentual fixo de "
                                + "" + new DecimalFormat("#,##0.00' %'").format(tabpromocao.getDesconto())
                                + " no valor total");
                        break;
                    }
                    case 5: {
                        tabpromocao.setItemList(getItensDaPromocaoPrecoFixo(tabpromocao.getCodpromocao()));
                        calcularPercentagemPrecoFixo(tabpromocao);
                        tabpromocao.setResumoPromo("Levando a quantidade informada de um dos produtos abaixo,"
                                + "paga um total de " + new DecimalFormat("'R$ '#,##0.00").format(tabpromocao.getPreco()) + ":");
                        break;
                    }
                    case 6: {
                        tabpromocao.setItemList(getItensDaPromocaoDescontoFixo(tabpromocao.getCodpromocao()));
                        calcularPercentagemDescontoFixo(tabpromocao);
                        tabpromocao.setResumoPromo("Levando a quantidade informada de um dos produto(s) abaixo,"
                                + "é aplicado um desconto fixo no valor de " + new DecimalFormat("'R$ '#,##0.00").format(tabpromocao.getDesconto()) + " :");
                        break;
                    }
                }

                tabpromocao.setTipoDesc(tabpromocao.getValidarTipo());
                tabpromocao.setSituacaoDesc(tabpromocao.getValidarSituacao());
                tabpromocao.setDataInicioDesc(tabpromocao.getValidarData(tabpromocao.getDatainicio()));
                tabpromocao.setDataFimDesc(tabpromocao.getValidarData(tabpromocao.getDatafim()));

                switch (tabpromocao.getSituacao()) {
                    case "A": {
                        countAtivos++;
                        break;
                    }
                    case "I": {
                        countInativos++;
                        break;
                    }
                    case "P": {
                        countPendentes++;
                        break;
                    }
                    case "R": {
                        countRejeitados++;
                        break;
                    }
                }

            }

            result.addAll(resp.getResultList());

        } else {
            throw new Exception("Nenhum registro encontrado na base de dados.");
        }

        Map resp = new HashMap();

        resp.put("count", count);
        resp.put("countAtivos", countAtivos);
        resp.put("countInativos", countInativos);
        resp.put("countPendentes", countPendentes);
        resp.put("countRejeitados", countRejeitados);
        resp.put("promocoes", result);

        return resp;
    }

//    public static void main(String args[]) {
//        int cont = 1;
//        Tabpromocao resp = new Tabpromocao();
//        resp.setSituacao("A");
//        for (int i = 0; i < 10; i++) {
//            switch (resp.getSituacao()) {
//                case "A": {
//                    resp.setCountAtivos(cont++);
//                    break;
//                }
//            }
//        }
//
//        System.out.println("Count situacao = " + resp.getCountAtivos());
//
//    }
    //************************** Promocao Leva Paga ****************************
    public List<Tabpromoitem> getItensDaPromocaoLevaPaga(Integer codpromocao) {

        String sql = "select\n"
                + "       tpi.codpromocao,\n"
                + "       tpf.codfil,\n"
                + "       tl.nomfil,\n"
                + "       tpi.codpro,\n"
                + "       tpi.codbarun,\n"
                + "       tpi.quantidade,\n"
                + "       tp.paga,\n"
                + "       tpi.descpro,\n"
                + "       tf.prvapro,\n"
                + "       tf.prcustocom\n"
                + "    from\n"
                + "        tabpromocaofilial tpf\n"
                + "            right join tabpromocao tp\n"
                + "                on\n"
                + "                    tp.codpromocao = tpf.codpromocao\n"
                + "            left join tabpromoitem tpi\n"
                + "                on\n"
                + "                    tpi.codpromocao = tp.codpromocao\n"
                + "                    and tpi.tipo = 'I'\n"
                + "            left join tabpro p \n"
                + "                on\n"
                + "                    p.codbarun = tpi.codbarun"
                + "            inner join tabprofil tf\n"
                + "                on\n"
                + "                    p.codpro = tf.codpro and\n"
                + "                    tpf.codfil = tf.codfil\n"
                + "            left join tabfil tl\n"
                + "                on\n"
                + "                    tf.codfil = tl.codfil"
                + "     where "
                + "        tp.codpromocao = :codpromocao";

        TypedQuery<Tabpromoitem> query = (TypedQuery<Tabpromoitem>) emFirebird.createNativeQuery(sql, "produtos-leva-paga");

        query.setParameter("codpromocao", codpromocao);

        return query.getResultList();

    }

    public void calcularPercentagemLucroLevaPaga(Tabpromocao promocaoComItens) {
        for (Tabpromoitem tabpromoitem : promocaoComItens.getItemList()) {
            double percentagem = 100 - CalcUtil.percentagem(promocaoComItens.getPaga(), tabpromoitem.getQuantidade());
            double valorDaPercentagem = CalcUtil.valorDaPercentagem(tabpromoitem.getPrvapro(), percentagem);
            double precoPromocional = tabpromoitem.getPrvapro() - valorDaPercentagem;
            double lucroPercentual = 100.0;
            if (tabpromoitem.getPrcustocom() != null && tabpromoitem.getPrcustocom() > 0.0) {
                lucroPercentual = CalcUtil.lucro(precoPromocional, tabpromoitem.getPrcustocom());
            }
            double lucroReal = precoPromocional - tabpromoitem.getPrcustocom();
            tabpromoitem.setPrecoPromocional(precoPromocional);
            tabpromoitem.setLucroPercentual(lucroPercentual);
            tabpromoitem.setLucroReal(lucroReal);
            //tab
        }
    }

    //********************** Promocao Desconto variavel ************************
    public List<Tabpromoitem> getItensDaPromocaoDescontoVariavel(Integer codpromocao) {

        String sql = "    select\n"
                + "       tpi.codpromocao,\n"
                + "       tpf.codfil,\n"
                + "       tl.nomfil,\n"
                + "       tpi.codpro,\n"
                + "       tpi.codbarun,\n"
                + "       tpi.quantidade,\n"
                + "       tp.desconto,\n"
                + "       tpi.descpro,\n"
                + "       tf.prvapro,\n"
                + "       tf.prcustocom"
                + "    from\n"
                + "        tabpromocaofilial tpf\n"
                + "            right join tabpromocao tp\n"
                + "                on\n"
                + "                    tp.codpromocao = tpf.codpromocao\n"
                + "            left join tabpromoitem tpi\n"
                + "                on\n"
                + "                    tpi.codpromocao = tp.codpromocao\n"
                + "            left join tabpro p \n"
                + "                on\n"
                + "                    p.codbarun = tpi.codbarun"
                + "            inner join tabprofil tf\n"
                + "                on\n"
                + "                    p.codpro = tf.codpro and\n"
                + "                    tpf.codfil = tf.codfil\n"
                + "            left join tabfil tl\n"
                + "                on\n"
                + "                    tf.codfil = tl.codfil"
                + "   where "
                + "       tp.codpromocao = :codpromocao";

        TypedQuery<Tabpromoitem> query = (TypedQuery<Tabpromoitem>) emFirebird.createNativeQuery(sql, "produtos-desconto-variavel");

        query.setParameter("codpromocao", codpromocao);

        return query.getResultList();

    }

    public void calcularPercentagemLucroDescontoVariavel(Tabpromocao promocaoComItens) {
        for (Tabpromoitem tabpromoitem : promocaoComItens.getItemList()) {
            double valorDoDesconto = CalcUtil.valorDaPercentagem(tabpromoitem.getPrvapro(), tabpromoitem.getDesconto());
            double precoPromocional = tabpromoitem.getPrvapro() - valorDoDesconto;
            double lucroPercentual = 100.0;
            if (tabpromoitem.getPrcustocom() != null && tabpromoitem.getPrcustocom() > 0.0) {
                lucroPercentual = CalcUtil.lucro(precoPromocional, tabpromoitem.getPrcustocom());
            }
            double lucroReal = precoPromocional - tabpromoitem.getPrcustocom();
            tabpromoitem.setPrecoPromocional(precoPromocional);
            tabpromoitem.setLucroPercentual(lucroPercentual);
            tabpromoitem.setLucroReal(lucroReal);
        }
    }

    //************************* Promocao Preço Fixo ****************************
    public List<Tabpromoitem> getItensDaPromocaoPrecoFixo(Integer codpromocao) {
        String sql = "select\n"
                + "       tpi.codpromocao,\n"
                + "       tpf.codfil,\n"
                + "       tl.nomfil,\n"
                + "       tpi.codpro,\n"
                + "       tpi.codbarun,\n"
                + "       tpi.quantidade,\n"
                + "       tp.preco,\n"
                + "       tpi.descpro,\n"
                + "       tf.prvapro,\n"
                + "       tf.pratpro,"
                + "       tf.prcustocom"
                + "    from\n"
                + "        tabpromocaofilial tpf\n"
                + "            right join tabpromocao tp\n"
                + "                on\n"
                + "                    tp.codpromocao = tpf.codpromocao\n"
                + "                    and tp.tipo = 5\n"
                + "            left join tabpromoitem tpi\n"
                + "                on\n"
                + "                    tpi.codpromocao = tp.codpromocao\n"
                + "            left join tabpro p \n"
                + "                on\n"
                + "                    p.codbarun = tpi.codbarun"
                + "            inner join tabprofil tf\n"
                + "                on\n"
                + "                    p.codpro = tf.codpro and\n"
                + "                    tpf.codfil = tf.codfil\n"
                + "             left join tabfil tl\n"
                + "                on\n"
                + "                    tf.codfil = tl.codfil"
                + "    where\n"
                + "       tp.codpromocao = :codpromocao";

        TypedQuery<Tabpromoitem> query = (TypedQuery<Tabpromoitem>) emFirebird.createNativeQuery(sql, "produtos-preco-fixo");

        query.setParameter("codpromocao", codpromocao);

        return query.getResultList();
    }

    public void calcularPercentagemPrecoFixo(Tabpromocao promocaoComItens) {
        for (Tabpromoitem tabpromoitem : promocaoComItens.getItemList()) {
            double lucroPercentual = 100.0;
            if (tabpromoitem.getPrcustocom() != null && tabpromoitem.getPrcustocom() > 0.0) {
                lucroPercentual = CalcUtil.lucro(promocaoComItens.getPreco(), tabpromoitem.getPrcustocom());
            }
            double lucroReal = promocaoComItens.getPreco() - tabpromoitem.getPrcustocom();
            tabpromoitem.setPrecoPromocional(promocaoComItens.getPreco());
            tabpromoitem.setLucroPercentual(lucroPercentual);
            tabpromoitem.setLucroReal(lucroReal);
        }
    }

    //************************ Promocao Desconto Fixo **************************
    public List<Tabpromoitem> getItensDaPromocaoDescontoFixo(Integer codpromocao) {
        String sql = "select\n"
                + "       tpi.codpromocao,\n"
                + "       tpf.codfil,\n"
                + "       tl.nomfil,\n"
                + "       tpi.codpro,\n"
                + "       tpi.codbarun,\n"
                + "       tpi.quantidade,\n"
                + "       tp.desconto,\n"
                + "       tpi.descpro,\n"
                + "       tf.prvapro,"
                + "       tf.prcustocom"
                + "    from\n"
                + "        tabpromocaofilial tpf\n"
                + "            right join tabpromocao tp\n"
                + "                on\n"
                + "                    tp.codpromocao = tpf.codpromocao\n"
                + "                    and tp.tipo = 6\n"
                + "            left join tabpromoitem tpi\n"
                + "                on\n"
                + "                    tpi.codpromocao = tp.codpromocao\n"
                + "            left join tabpro p \n"
                + "                on\n"
                + "                    p.codbarun = tpi.codbarun"
                + "            inner join tabprofil tf\n"
                + "                on\n"
                + "                    p.codpro = tf.codpro and\n"
                + "                    tpf.codfil = tf.codfil\n"
                + "            left join tabfil tl\n"
                + "                on\n"
                + "                    tf.codfil = tl.codfil"
                + "    where\n"
                + "       tp.codpromocao = :codpromocao";

        TypedQuery<Tabpromoitem> query = (TypedQuery<Tabpromoitem>) emFirebird.createNativeQuery(sql, "produtos-desconto-variavel");

        query.setParameter("codpromocao", codpromocao);

        return query.getResultList();
    }

    public void calcularPercentagemDescontoFixo(Tabpromocao promocaoComItens) {
        for (Tabpromoitem tabpromoitem : promocaoComItens.getItemList()) {
            double precoTotal = tabpromoitem.getPrvapro();
            double precoPromocional = precoTotal - promocaoComItens.getDesconto();
            double lucroPercentual = 100.0;
            if (tabpromoitem.getPrcustocom() != null && tabpromoitem.getPrcustocom() > 0.0) {
                lucroPercentual = CalcUtil.lucro(precoPromocional, tabpromoitem.getPrcustocom());
            }
            tabpromoitem.setPrecoPromocional(precoPromocional);
            tabpromoitem.setLucroPercentual(lucroPercentual);
            tabpromoitem.setLucroReal(precoPromocional - tabpromoitem.getPrcustocom());
        }
    }

    public Tabpromocao create(Tabpromocao obj) throws Exception {

        int codMax = getCodMax();

        obj.setCodpromocao(codMax);

        int i = 1;
        for (Tabpromoitem tabpromoitem : obj.getItemList()) {
            tabpromoitem.getTabpromoitemPK().setCodpromocao(codMax);
            tabpromoitem.getTabpromoitemPK().setCoditem(i);
            i++;
        }

        for (Tabpromoitem tabpromoitem : obj.getBeneficioList()) {
            tabpromoitem.getTabpromoitemPK().setCodpromocao(codMax);
            tabpromoitem.getTabpromoitemPK().setCoditem(i);
            i++;
        }

        emFirebird.persist(obj);
        for (Tabpromoitem tabpromoitem : obj.getItemList()) {
            emFirebird.persist(tabpromoitem);
        }
        for (Tabpromoitem tabpromoitem : obj.getBeneficioList()) {
            emFirebird.persist(tabpromoitem);
        }

        return obj;
    }

    //**************************************************************************
    private int getCodMax() {

        String hql = "SELECT MAX(t.codpromocao) "
                + "FROM Tabpromocao t ";

        TypedQuery<Number> query = emFirebird.createQuery(hql, Number.class);
        try {
            Number max = query.getSingleResult();
            return max == null ? 1 : max.intValue() + 1;
        } catch (NoResultException e) {
            return 1;
        }

    }

    private int baixarPromocoes(List<ConfiguracaoItem> configuracoes, EstadoPromocao estado) throws Exception {

        for (ConfiguracaoItem configuracao : configuracoes) {
            for (FilialScanntech filialScanntech : configuracao.getListaFilial()) {

                ResponseEntity<JsonNode> response = scanntechRestClient.buscarPromocoes(configuracao, estado, filialScanntech.getCodigoScanntech().intValue());

                if (response.getStatusCodeValue() == 200) {
                    int promocoesInseridas = 0;

                    List<Tabpromocao> promocoes = convertJsonNodeToTabpromocaoList(response.getBody());
                    for (int i = 0; i < promocoes.size(); i++) {
                        Tabpromocao promocao = promocoes.get(i);
                        Tabpromocao promocaoTemp = loadBycodscanntech(promocao.getCodscanntech());
                        if (promocaoTemp != null) {
                            promocaoTemp.setSituacao(estado.getValorInterage());
                            update(promocaoTemp);
                            promocoes.set(i, promocaoTemp);
                        } else {

                            promocao.setRgcodusu(1);
                            promocao.setRgusuario("INTER");
                            promocao.setRgdata(new Date());
                            promocao.setRgevento(1);

                            promocao.setSituacao(estado.getValorInterage());
                            create(promocao);
                            promocoesInseridas++;
                        }
                    }

                    vincularPromocoesAFilial(filialScanntech.getCodigoFilial().intValue(), promocoes);
                    return promocoesInseridas;
                }

            }

        }

        return 0;

    }

    @Transactional("multiTransaction")
    public void baixarPromocoes(List<ConfiguracaoItem> configuracoes) throws Exception {
        try {
            int promocoesBaixadas = 0;

            promocoesBaixadas += baixarPromocoes(configuracoes, EstadoPromocao.ACEITA);
            promocoesBaixadas += baixarPromocoes(configuracoes, EstadoPromocao.PENDENTE);
            promocoesBaixadas += baixarPromocoes(configuracoes, EstadoPromocao.REJEITADA);

            inserirSincronizacao(promocoesBaixadas);
        } catch (Exception e) {
            throw new Exception("Erro ao baixar promoções: ", e);
        }

    }

    private void vincularPromocoesAFilial(int codfil, List<Tabpromocao> promocoes) {

        String selectSql = "select count(*) from tabpromocaofilial where codfil = :codfil and codpromocao = :codpromocao";

        Query selectQuery = emFirebird.createNativeQuery(selectSql);

        for (Tabpromocao promocao : promocoes) {

            selectQuery.setParameter("codpromocao", promocao.getCodpromocao());

            selectQuery.setParameter("codfil", codfil);

            if (((Number) selectQuery.getSingleResult()).longValue() < 1) {
                String insertSql = "insert into tabpromocaofilial (codpromocao, codfil) values(:codpromocao, :codfil)";
                Query insertQuery = emFirebird.createNativeQuery(insertSql);
                insertQuery.setParameter("codpromocao", promocao.getCodpromocao());
                insertQuery.setParameter("codfil", codfil);
                insertQuery.executeUpdate();
            }

        }

    }

    private List<Tabpromocao> convertJsonNodeToTabpromocaoList(JsonNode json) throws Exception {

        JsonNode jsonArray = json.get("results");
        List<Tabpromocao> promocoes = new ArrayList();
        for (JsonNode jsonNode : jsonArray) {
            Tabpromocao tabpromocao = new Tabpromocao();
            tabpromocao.setCodscanntech(jsonNode.get("id").asInt());
            tabpromocao.setTitulo(jsonNode.get("titulo").asText());
            tabpromocao.setDescricao(jsonNode.get("descripcion").asText());
            tabpromocao.setTipo(convertPromocaoConstantToPromocaoCode(jsonNode.get("tipo").asText()));
            tabpromocao.setAutor(jsonNode.get("autor").get("descripcion").asText());
            tabpromocao.setDatainicio(dateFormat.parse(jsonNode.get("vigenciaDesde").asText()));
            tabpromocao.setDatafim(dateFormat.parse(jsonNode.get("vigenciaHasta").asText()));
            if (jsonNode.has("limitePromocionesPorTicket")) {
                tabpromocao.setLimiteporcupom(jsonNode.get("limitePromocionesPorTicket").asInt());
            }

            JsonNode detalheNode = jsonNode.get("detalles");
            if (detalheNode.get("precio") != null) {
                tabpromocao.setPreco(detalheNode.get("precio").asDouble());
            }
            if (detalheNode.get("descuento") != null) {
                tabpromocao.setDesconto(detalheNode.get("descuento").asDouble());
            }
            if (detalheNode.get("paga") != null) {
                tabpromocao.setPaga(detalheNode.get("paga").asInt());
            }

            List<Tabpromoitem> promoitens = new ArrayList();
            JsonNode condicaoNode = detalheNode.get("condiciones");
            JsonNode itens = condicaoNode.get("items");
            for (JsonNode item : itens) {
                for (JsonNode articulo : item.get("articulos")) {
                    Tabpromoitem tabpromoitem = new Tabpromoitem();
                    tabpromoitem.setCodbarun(articulo.get("codigoBarras").asText());
                    Map<String, String> resultMap = (Map<String, String>) load(tabpromoitem.getCodbarun(), "codbarun", Arrays.asList("codpro", "descpro"));
                    if (resultMap != null) {
                        tabpromoitem.setCodpro(resultMap.get("codpro"));
                        tabpromoitem.setDescpro(resultMap.get("descpro"));
                    } else {
                        String descpro = articulo.get("nombre").asText();
                        if (descpro.length() > 60) {
                            tabpromoitem.setDescpro(descpro.substring(0, 60));
                        } else {
                            tabpromoitem.setDescpro(descpro);
                        }
                    }
                    tabpromoitem.setQuantidade(item.get("cantidad").asDouble());
                    tabpromoitem.setTipo("I");
                    promoitens.add(tabpromoitem);

                }
            }
            tabpromocao.setItemList(promoitens);

            List<Tabpromoitem> promobeneficios = new ArrayList();
            JsonNode beneficios = detalheNode.get("beneficios");
            if (!beneficios.isNull()) {
                for (JsonNode beneficioItem : beneficios.get("items")) {
                    for (JsonNode articulo : beneficioItem.get("articulos")) {
                        Tabpromoitem tabpromoitem = new Tabpromoitem();
                        tabpromoitem.setCodbarun(articulo.get("codigoBarras").asText());
                        Map<String, String> resultMap = (Map<String, String>) load(tabpromoitem.getCodbarun(), "codbarun", Arrays.asList("codpro", "descpro"));
                        if (resultMap != null) {
                            tabpromoitem.setCodpro(resultMap.get("codpro"));
                            tabpromoitem.setDescpro(resultMap.get("descpro"));
                        } else {
                            String descpro = articulo.get("nombre").asText();
                            if (descpro.length() > 60) {
                                tabpromoitem.setDescpro(descpro.substring(0, 60));
                            } else {
                                tabpromoitem.setDescpro(descpro);
                            }
                        }
                        tabpromoitem.setQuantidade(beneficioItem.get("cantidad").asDouble());
                        tabpromoitem.setTipo("B");
                        promobeneficios.add(tabpromoitem);
                    }

                }

            }

            tabpromocao.setBeneficioList(promobeneficios);

            promocoes.add(tabpromocao);
        }

        return promocoes;

    }

    public Tabpromocao loadBycodscanntech(Integer codscann) {
        String hql = "SELECT t FROM Tabpromocao t WHERE t.codscanntech = :codscann ";
        TypedQuery<Tabpromocao> query = emFirebird.createQuery(hql, Tabpromocao.class);
        query.setParameter("codscann", codscann);
        try {
            return query.getSingleResult();
        } catch (NoResultException | NonUniqueResultException e) {
            return null;
        }
    }

    private Integer convertPromocaoConstantToPromocaoCode(String constant) {
        switch (constant) {
            case "ADICIONAL_REGALO":
                return 1;
            case "LLEVA_PAGA":
                return 2;
            case "DESCUENTO_VARIABLE":
                return 3;
            case "ADICIONAL_DESCUENTO":
                return 4;
            case "PRECIO_FIJO":
                return 5;
            case "DESCUENTO_FIJO":
                return 6;
            default:
                return null;
        }
    }

    public MultiValueMap<String, String> createHeaders(String usuario, String senha) {
        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        String authorization = usuario + ":" + senha;
        authorization = Base64.getEncoder().encodeToString(authorization.getBytes());
        headers.add("Authorization", "Basic " + authorization);
        return headers;
    }

    public Map<String, ?> load(Object id, String idName, List<String> fields) {

        try {
            StringBuilder hql = new StringBuilder();

            // String columns = String.join(",", fields);
            StringBuilder columns = new StringBuilder();
            String delimiter = "";
            for (String f : fields) {
                String value[] = f.split(",");
                for (int i = 0; i < value.length; i++) {
                    columns.append(delimiter).append(" ");
                    columns.append(value[i]).append(" as ").append(value[i]);
                    delimiter = ",";
                }
            }

            hql.append(" select new Map(").append(columns).append(") ");
            hql.append(" from Tabpromoitem ");
            hql.append(" where ").append(idName).append(" = :id ");
            Map<String, ?> result = (Map<String, ?>) emFirebird.createQuery(hql.toString()).setParameter("id", id)
                    .getSingleResult();

            return result;
        } catch (NoResultException ex) {
            return null;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    private void update(Tabpromocao obj) throws Exception {
        emFirebird.merge(obj);
    }

    private void inserirSincronizacao(int quantidadeDePromocoesInseridas) {
        SincronizacaoPromocao promocao = new SincronizacaoPromocao(quantidadeDePromocoesInseridas);
        em.persist(promocao);
    }

}
