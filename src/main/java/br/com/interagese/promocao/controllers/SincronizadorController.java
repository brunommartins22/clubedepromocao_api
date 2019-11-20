package br.com.interagese.promocao.controllers;

import br.com.interagese.erplibrary.Utils;
import br.com.interagese.padrao.rest.util.ExceptionMessage;
import br.com.interagese.postgres.dtos.StatusSincronizadorDto;
import br.com.interagese.promocao.service.SincronizadorService;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/sincronizador")
public class SincronizadorController {

    @Autowired
    private SincronizadorService service;

    @GetMapping(path = "/status")
    public StatusSincronizadorDto getStatus() {
        return service.getStatus();
    }

    @PostMapping(path = "/venda/start")
    public String startVenda() {

        new Thread(service::sincronizarVendas).start();
        return Utils.serializar("OK");
    }

    @PostMapping(path = "/promocao/start")
    public String startPromocao() {
        new Thread(service::sincronizarPromocao).start();
        return Utils.serializar("OK");
    }

    @PostMapping(path = "/fechamento/reenvio")
    public String reenviarFechamento(@RequestBody Map map) {
        new Thread(() -> {
            service.reenviarFechamento(map);
        }).start();
        return Utils.serializar("OK");
    }
    
    @PostMapping(path = "/vendas/desmarcar")
    public String desmarcarVendas(@RequestBody Map map) {
        try{
            service.desmarcarVendas(map);
             return Utils.serializar("OK");
        }catch(Exception e){
            return returnException(e);
        }
       
    }

    @PostMapping(path = "/stop")
    public void stop() {
        service.finalizarSincronizacao();
    }

    @PostMapping(path = "/start")
    public void start() {
        service.iniciarSincronizacao();
    }
    
    public String returnException(Exception ex) {
        ExceptionMessage exceptionMessage = new ExceptionMessage();
        exceptionMessage.setErrorCode(1);
        exceptionMessage.setMessage(ex.getMessage());

        return Utils.serializar(exceptionMessage);
    }

}
