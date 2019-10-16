/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package br.com.interagese.promocao.controllers;

import br.com.interagese.padrao.rest.util.IsServiceDefault;
import br.com.interagese.padrao.rest.util.PadraoController;
import br.com.interagese.promocao.service.ConfiguracaoService;
import br.com.interagese.postgres.models.Configuracao;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author Bruno Martins
 */
@RestController
@RequestMapping(path = "api/configuracoes")
public class ConfiguracaoController extends PadraoController<Configuracao> {

    @IsServiceDefault
    @Autowired
    public ConfiguracaoService service;

//    @PostMapping(path = "/processConfimationConfig")
//    public String processConfimationConfig(@RequestBody String json) {
//        try {
//            Configuracao result = (Configuracao) deserializar(json, Configuracao.class); 
//            
//            service.processConfimationConfig(result);
//            return serializar("ok");
//        } catch (Exception ex) {
//            return returnException(ex);
//        }
//    }

}
