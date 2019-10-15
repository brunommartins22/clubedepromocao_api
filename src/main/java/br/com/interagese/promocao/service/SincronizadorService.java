package br.com.interagese.promocao.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class SincronizadorService {

    private boolean executando = false;

    @Autowired
    private TabpromocaoService tabpromocaoService;
    
    @Autowired
    private NotasaiService notasaiService;

    public SincronizadorService() {

    }

    @Scheduled(initialDelay = 2000, fixedDelay = 999999999)
    public void executarTransmissao() {
        if (!executando) {
            executando = true;
            try {
               // notasaiService.enviarVendas();
                // tabpromocaoService.baixarPromocoes();
                //System.out.println("Promoção baixadas");
            } catch (Exception ex) {
                ex.printStackTrace();
            } finally {
                executando = false;
            }
        }
    }

}
