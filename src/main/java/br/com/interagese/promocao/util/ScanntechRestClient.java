package br.com.interagese.promocao.util;

import br.com.firebird.models.Notasai;
import br.com.interagese.postgres.models.Configuracao;
import br.com.interagese.postgres.models.Url;
import br.com.interagese.promocao.enuns.EstadoPromocao;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.SocketTimeoutException;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

public class ScanntechRestClient {

    public ResponseEntity<JsonNode> buscarPromocoes(Configuracao configuracao, EstadoPromocao estado, Integer idLocal) {

        for (int i = 0; i < configuracao.getListaUrl().size(); i++) {

            Url url = configuracao.getListaUrl().get(i);

            try {
                String urlBase = url.getValor();
                String idEmpresa = configuracao.getCodigoEmpresa();
                String usuario = configuracao.getUsuario();
                String senha = configuracao.getSenha();

                String endPoint = urlBase + "/pmkt-rest-api/minoristas/" + idEmpresa + "/locales/" + idLocal + "/promocionesConLimitePorTicket?estado=" + estado.getValorScanntech();

                RestTemplate restTemplate = new RestTemplate();
                MultiValueMap<String, String> headers = createHeaders(usuario, senha);

                ResponseEntity<JsonNode> response = restTemplate.exchange(endPoint, HttpMethod.GET, new HttpEntity<>(headers), JsonNode.class);

                return response;

            } catch (RestClientException e) {
                if (!(e.getCause() instanceof SocketTimeoutException)) {
                    throw e;
                }

                if (i >= configuracao.getListaUrl().size()) {
                    throw e;
                }

            }

        }

        return null;

    }

    public ResponseEntity<String> enviarVendas(Configuracao configuracao, Notasai venda, Integer idLocal, Integer nrcaixa) {

        for (int i = 0; i < configuracao.getListaUrl().size(); i++) {

            Url url = configuracao.getListaUrl().get(i);

            try {
                String urlBase = url.getValor();
                String idEmpresa = configuracao.getCodigoEmpresa();
                String usuario = configuracao.getUsuario();
                String senha = configuracao.getSenha();

                String endPoint = urlBase + "/api-minoristas/api/v2/minoristas/" + idEmpresa + "/locales/" + idLocal + "/cajas/"+ nrcaixa +"/movimientos";

                RestTemplate restTemplate = new RestTemplate();
                MultiValueMap<String, String> headers = createHeaders(usuario, senha);
                
                ObjectMapper mapper = new ObjectMapper();
                try {
                    String str = mapper.writeValueAsString(venda);
                    System.out.println("Txt " + str);
                } catch (JsonProcessingException ex) {
                    Logger.getLogger(ScanntechRestClient.class.getName()).log(Level.SEVERE, null, ex);
                }
                
                ResponseEntity<String> response = restTemplate.exchange(endPoint, HttpMethod.POST, new HttpEntity<>(venda, headers), String.class);

                return response;

            } catch (RestClientException e) {
                if(e instanceof HttpClientErrorException){
                    System.out.println("Causa: " + ((HttpClientErrorException) e).getResponseBodyAsString());
                }
                if (!(e.getCause() instanceof SocketTimeoutException)) {
                    throw e;
                }

                if (i >= configuracao.getListaUrl().size()) {
                    throw e;
                }

            }

        }

        return null;

    }

    private MultiValueMap<String, String> createHeaders(String usuario, String senha) {
        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        String authorization = usuario + ":" + senha;
        authorization = Base64.getEncoder().encodeToString(authorization.getBytes());
        headers.add("Authorization", "Basic " + authorization);
        headers.add("Content-Type", "application/json");
        return headers;
    }
    
    public static void main(String[] args) {
        System.out.println(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ").format(new Date()));
    }

}
