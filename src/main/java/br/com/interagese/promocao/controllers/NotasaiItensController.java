/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package br.com.interagese.promocao.controllers;

import br.com.interagese.erplibrary.Utils;
import br.com.interagese.padrao.rest.util.ExceptionMessage;
import br.com.interagese.padrao.rest.util.IsServiceDefault;
import br.com.interagese.promocao.service.NotasaiItensService;
import java.util.Map;
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
@RequestMapping(path = "/api/notasaiitens")
public class NotasaiItensController {
    
    @IsServiceDefault
    @Autowired
    private NotasaiItensService service;
    
    
    @PostMapping("findItensByFilters")
    public String findItensByFilters(@RequestBody Map map){
        try{
            return Utils.serializar(service.findItensByFilters(map));
        }catch(Exception ex){
            return returnException(ex);
        }
    }
    
    
    
     public String returnException(Exception ex) {
        ExceptionMessage exceptionMessage = new ExceptionMessage();
        exceptionMessage.setErrorCode(1);
        exceptionMessage.setMessage(ex.getMessage());

        return Utils.serializar(exceptionMessage);
    }
    
    
    
    
}
