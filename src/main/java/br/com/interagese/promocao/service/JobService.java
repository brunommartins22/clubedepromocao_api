
package br.com.interagese.promocao.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class JobService {
    
    private boolean executando = false;
    
    @Scheduled(initialDelay = 2000)
    public void executarTransmissao(){
        if(!executando){
            executando = true;
            
            
            
            executando = false;
        }
    }
    
}
