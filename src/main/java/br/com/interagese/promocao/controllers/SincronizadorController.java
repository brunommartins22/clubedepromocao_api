package br.com.interagese.promocao.controllers;

import br.com.interagese.erplibrary.Utils;
import br.com.interagese.padrao.rest.util.ExceptionMessage;
import br.com.interagese.postgres.dtos.StatusSincronizadorDto;
import br.com.interagese.promocao.service.SincronizadorService;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
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

    private final SimpleDateFormat jsonDatFormat;

    public SincronizadorController() {
        this.jsonDatFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    }

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
            Date dataInicio = null, dataFim = null;
            Integer nrcaixa = null;

            List<String> datas = (List<String>) map.get("datasReenvio");
            try {
                dataInicio = jsonDatFormat.parse(datas.get(0));
                if (datas.get(1) != null) {
                    dataFim = jsonDatFormat.parse(datas.get(1));
                }

                if (map.containsKey("numCaixa") && map.get("numCaixa") != null && !map.get("numCaixa").toString().isEmpty()) {
                    nrcaixa = Integer.parseInt(map.get("numCaixa").toString());
                }
            } catch (ParseException e) {

            }

            service.reenviarFechamento(dataInicio, dataFim, nrcaixa);
        }).start();
        return Utils.serializar("OK");
    }

    @PostMapping(path = "/vendas/desmarcar")
    public String desmarcarVendas(@RequestBody Map map) {
        try {

            Date dataInicio = null, dataFim = null;

            List<String> datas = (List<String>) map.get("datasReenvio");

            dataInicio = jsonDatFormat.parse(datas.get(0));
            if (datas.get(1) != null) {
                dataFim = jsonDatFormat.parse(datas.get(1));
            }

            service.desmarcarVendas(dataInicio, dataFim);
            return Utils.serializar("OK");
        } catch (Exception e) {
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
