
package br.com.interagese.promocao.controllers;

import br.com.interagese.erplibrary.Utils;
import br.com.interagese.postgres.dtos.StatusSincronizadorDto;
import br.com.interagese.promocao.service.SincronizadorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/sincronizador")
public class SincronizadorController {
    
    @Autowired
    private SincronizadorService service;
    
    @GetMapping(path = "/status")
    public StatusSincronizadorDto getStatus(){
        return service.getStatus();
    }
    
    @PostMapping(path = "/venda/start")
    public String startVenda(){
        
        new Thread(service::sincronizarVendas).start();
        return Utils.serializar("OK");
    }
    
    @PostMapping(path = "/promocao/start")
    public String startPromocao(){
        new Thread(service::sincronizarPromocao).start();
        return Utils.serializar("OK");
    }
    
    @PostMapping(path = "/stop")
    public void stop(){
        service.finalizarSincronizacao();
    }
    
    @PostMapping(path = "/start")
    public void start(){
        service.iniciarSincronizacao();
    }
    
}
