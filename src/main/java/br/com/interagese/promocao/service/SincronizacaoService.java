package br.com.interagese.promocao.service;

import br.com.firebird.models.Tabpromocao;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Date;
import java.util.List;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class SincronizacaoService {

    private boolean executando = false;

    @Scheduled(initialDelay = 2000, fixedDelay = 2000)
    public void executarTransmissao() {
        if (!executando) {
            executando = true;

            executando = false;
        }
    }

    private void handlePromocoesBaixadas() throws Exception {

        RestTemplate restClient = new RestTemplate();
        ResponseEntity<JsonNode> response = restClient.exchange("http://br.homo.apipdv.scanntech.com", HttpMethod.GET, HttpEntity.EMPTY, JsonNode.class);

        int promocoesInseridas = 0;

        List<Tabpromocao> promocoes = null;
        for (int i = 0; i < promocoes.size(); i++) {
            Tabpromocao promocao = promocoes.get(i);
            Tabpromocao promocaoTemp = null;
            if (promocaoTemp != null) {
                promocaoTemp.setSituacao("");
               // update(promocaoTemp);
                promocoes.set(i, promocaoTemp);
            } else {

               
                promocao.setRgcodusu(1);
                promocao.setRgusuario("INTER");
                promocao.setRgdata(new Date());
                promocao.setRgevento(1);

                promocao.setSituacao("");
                //insert(promocao);
                promocoesInseridas++;
            }
        }

        //insertTabpromocaofilial(tabfil, promocoes);

//        msg = "Foram inseridas " + promocoesInseridas + " novas promoções " + showSituacaoDescription(situacao);
//        event = new SincronizadorEvent(200, msg, new Date(), 0);
//        notifyItemRecebido(event);

    }

}
