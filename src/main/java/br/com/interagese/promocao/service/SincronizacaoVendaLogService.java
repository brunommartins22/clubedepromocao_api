/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package br.com.interagese.promocao.service;

import br.com.interagese.padrao.rest.util.PadraoService;
import br.com.interagese.postgres.models.SincronizacaoVendaLog;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import javax.persistence.TypedQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 *
 * @author Alan
 */
@Service
public class SincronizacaoVendaLogService extends PadraoService<SincronizacaoVendaLog> {

    @Autowired
    private FilialScanntechService filialScanntechService;

    public List<SincronizacaoVendaLog> loadSearchFilters(Map map) throws Exception {
        Integer codigoFilial = (Integer) map.get("codigoFilial");
        String numeroCaixa = (String) map.get("numeroCaixa");
        String numeroCupom = (String) map.get("numeroCupom");
        String situacao = (String) map.get("situacao");
        List<String> datasEnvio = (List<String>) map.get("datasEnvio");

        String sql = "SELECT o FROM SincronizacaoVendaLog o where o.id is not null";
        String sqlCount = "SELECT count(o) FROM SincronizacaoVendaLog o where o.id is not null";
        String sqlGenerica = "";
        if (codigoFilial != null) {
            sqlGenerica += " and o.codigoFilial =" + codigoFilial;
        }

        if (numeroCaixa != null) {
            sqlGenerica += " and o.numeroCaixa =" + numeroCaixa;
        }

        if (!StringUtils.isEmpty(numeroCupom)) {
            sqlGenerica += " and o.numeroCupom = '" + numeroCupom + "'";
        }

        if (!StringUtils.isEmpty(situacao)) {
            sqlGenerica += " and o.situacao = '" + situacao + "'";
        }

        if (!datasEnvio.isEmpty()) {
//            2019-10-14T03:00:00.000Z
            SimpleDateFormat dateFormatDate = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

            String date1 = datasEnvio.get(0);
            String date2 = datasEnvio.get(1);

            if (dateFormatDate.parse(date1).after(new Date())) {
                addErro("Data inicial não pode ser superior a data atual : " + new SimpleDateFormat("dd/MM/yyyy").format(new Date()));
            }

            if (date1 != null && date2 != null) {

                if (dateFormatDate.parse(date2).after(new Date())) {
                    addErro("Data final não pode ser superior a data atual : " + new SimpleDateFormat("dd/MM/yyyy").format(new Date()));
                }

                sqlGenerica += " and o.dataEnvio between '" + dateFormat.format(dateFormatDate.parse(date1)) + "' and '" + dateFormat.format(dateFormatDate.parse(date2)) + "'";

            } else {

                sqlGenerica += " and o.dataEnvio >='" + dateFormat.format(dateFormatDate.parse(date1)) + "'";

            }
        }

        TypedQuery<SincronizacaoVendaLog> result = em.createQuery(sql + " order by o.situacao asc", SincronizacaoVendaLog.class);

        if (result.getResultList() != null && !result.getResultList().isEmpty()) {
            for (SincronizacaoVendaLog o : result.getResultList()) {
                o.setFilial(o.getCodigoFilial() + " - " + filialScanntechService.loadNameFilialByCodigoFilial(o.getCodigoFilial().longValue()));
                o.setSituacaoDesc(o.getValidarSitucao());
            }
        } else {
            addErro("Nenhum registro encontrado na base de dados.");
        }

        return result.getResultList();
    }
}
