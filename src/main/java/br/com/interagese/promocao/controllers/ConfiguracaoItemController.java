/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package br.com.interagese.promocao.controllers;

import br.com.interagese.padrao.rest.util.IsServiceDefault;
import br.com.interagese.padrao.rest.util.PadraoController;
import br.com.interagese.postgres.models.ConfiguracaoItem;
import br.com.interagese.promocao.service.ConfiguracaoItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author Bruno Martins
 */
@RestController
@RequestMapping("api/configuracaoitem")
public class ConfiguracaoItemController extends PadraoController<ConfiguracaoItem>{
    
    
    @IsServiceDefault
    @Autowired
    private ConfiguracaoItemService service;


   
    
}
