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
        Long codigoFilial = (Long) map.get("codigoFilial");
        Integer numeroCaixa = (Integer) map.get("numeroCaixa");
        String numeroCupom = (String) map.get("numeroCupom");
        String situacao = (String) map.get("situacao");
        List<Date> datasEnvio = (List<Date>) map.get("datasEnvio");
        
        String sql = "SELECT o FROM SincronizacaoVendaLog o where o.id is not null";
        
        if (codigoFilial != null) {
            sql += " and o.codigoFilial=" + codigoFilial;
        }
        
        if (numeroCaixa != null) {
            sql += " and o.numeroCaixa=" + numeroCaixa;
        }
        
        if (!StringUtils.isEmpty(numeroCupom)) {
            sql += " and o.numeroCupom = '" + numeroCupom + "'";
        }
        
        if (!StringUtils.isEmpty(situacao)) {
            sql += " and o.situacao = '" + situacao + "'";
        }
        
        if (!datasEnvio.isEmpty()) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            if (datasEnvio.size() > 1) {
                sql += " and o.dataEnvio between '" + dateFormat.format(datasEnvio.get(0)) + "' and '" + dateFormat.format(datasEnvio.get(1)) + "'";
            } else {
                sql += " and o.dataEnvio='" + dateFormat.format(datasEnvio.get(0)) + "'";
            }
        }
        
        TypedQuery<SincronizacaoVendaLog> result = em.createQuery(sql, SincronizacaoVendaLog.class);
        
        for (SincronizacaoVendaLog o : result.getResultList()) {
            o.setFilial(o.getCodigoFilial() + " - " + filialScanntechService.loadNameFilialByCodigoFilial(o.getCodigoFilial().longValue()));
            o.setSituacaoDesc(o.validarSituacao());
        }
        
        return result.getResultList();
    }
}
