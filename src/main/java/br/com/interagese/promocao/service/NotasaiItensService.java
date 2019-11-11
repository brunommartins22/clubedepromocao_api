/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package br.com.interagese.promocao.service;

import br.com.interagese.erplibrary.Utils;
import br.com.interagese.padrao.rest.util.TransformNativeQuery;
import br.com.interagese.postgres.dtos.ConsultaVendas;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 *
 * @author Bruno Martins
 */
@Service
public class NotasaiItensService {

    @PersistenceContext(unitName = "integradoPU")
    private EntityManager emFirebird;

    public Map findItensByFilters(Map map) throws Exception {

        Integer codigoFilial = (Integer) map.get("codigoFilial");
        String descricaoPromocao = (String) map.get("descricaoPromocao");
        List<String> datas = (List<String>) map.get("datas");
        int inicial = (Integer) map.get("inicial");
        int finalR = (Integer) map.get("final");

        String sql = "Select tf.nomfil as filial, cast(ns.dtemissao as date) as dtVenda, tp.descricao as promocao, nsi.coddig as ean, nsi.qtdvend as quantidade, nsi.vlsubsidio as desconto from notasaiitens nsi\n"
                + "right join tabpromocao tp on tp.codpromocao = nsi.codpromo and tp.tipo = nsi.tppromo\n"
                + "join notasai ns on nsi.nrcontr = ns.nrcontr\n"
                + "left join tabfil tf on tf.codfil = ns.codfil\n"
                + "where nsi.nritem is not null and nsi.nrcontr is not null";

        String countSql = "Select count(*) from notasaiitens nsi\n"
                + "right join tabpromocao tp on tp.codpromocao = nsi.codpromo and tp.tipo = nsi.tppromo\n"
                + "join notasai ns on nsi.nrcontr = ns.nrcontr\n"
                + "left join tabfil tf on tf.codfil = ns.codfil\n"
                + "where nsi.nritem is not null and nsi.nrcontr is not null";
        String sqlGenerica = "";

        if (codigoFilial != null) {
            sqlGenerica += " and ns.codfil = " + codigoFilial + "\n";
        }

        if (!StringUtils.isEmpty(descricaoPromocao)) {
            if (Utils.somenteNumeros(descricaoPromocao)) {
                sqlGenerica += " and nsi.coddig = '" + descricaoPromocao + "'\n";
            } else {
                sqlGenerica += " and tp.descricao like '" + descricaoPromocao + "%'\n";
            }
        }

        if (!datas.isEmpty()) {
            SimpleDateFormat dateFormatDate = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

            String date1 = datas.get(0);
            String date2 = datas.get(1);

            if (dateFormatDate.parse(date1).after(new Date())) {
                throw new Exception("Data inicial não pode ser superior a data atual : " + new SimpleDateFormat("dd/MM/yyyy").format(new Date()));
            }

            if (date1 != null && date2 != null) {

                if (dateFormatDate.parse(date2).after(new Date())) {
                    throw new Exception("Data final não pode ser superior a data atual : " + new SimpleDateFormat("dd/MM/yyyy").format(new Date()));
                }

                sqlGenerica += " and ns.dtemissao between '" + dateFormat.format(dateFormatDate.parse(date1)) + "' and '" + dateFormat.format(dateFormatDate.parse(date2)) + "'\n";

            } else {

                sqlGenerica += " and ns.dtemissao >='" + dateFormat.format(dateFormatDate.parse(date1)) + "'\n";

            }
        }

        Long count = ((Number) emFirebird.createNativeQuery(countSql + sqlGenerica).getSingleResult()).longValue();
        List<ConsultaVendas> result = new ArrayList<>();

        if (count > 0) {

            List<Object[]> resp = emFirebird.createNativeQuery(sql + sqlGenerica + " group by tf.nomfil,ns.dtemissao,tp.descricao,nsi.coddig,nsi.qtdvend,nsi.vlsubsidio").setFirstResult(inicial).setMaxResults(finalR).getResultList();

            result = new TransformNativeQuery(ConsultaVendas.class).execute(resp);

        }else{
             throw new Exception("Nenhum registro encontrado na base de dados.");
        }

        map = new HashMap();
        map.put("count", count);
        map.put("list", result);

        return map;
    }

}
