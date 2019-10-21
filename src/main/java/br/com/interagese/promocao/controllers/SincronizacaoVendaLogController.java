/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package br.com.interagese.promocao.controllers;

import br.com.interagese.padrao.rest.util.IsServiceDefault;
import br.com.interagese.padrao.rest.util.PadraoController;
import br.com.interagese.postgres.models.SincronizacaoVendaLog;
import br.com.interagese.promocao.service.SincronizacaoVendaLogService;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author Alan
 */
@RestController
@RequestMapping("api/sincronizacoes")
public class SincronizacaoVendaLogController extends PadraoController<SincronizacaoVendaLog> {
    @Autowired
    @IsServiceDefault
    private SincronizacaoVendaLogService service;
    
    @PostMapping(path = "loadSearchFilters")
    public String loadSearchFilters(@RequestBody Map map){
        try{
            return serializar(service.loadSearchFilters(map));
        }catch(Exception ex){
            return returnException(ex);
        }
    }
}
