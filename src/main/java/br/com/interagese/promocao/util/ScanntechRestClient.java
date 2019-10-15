package br.com.interagese.promocao.util;

import br.com.firebird.models.Notasai;
import br.com.interagese.postgres.models.Configuracao;
import br.com.interagese.postgres.models.Url;
import br.com.interagese.promocao.EstadoPromocao;
import com.fasterxml.jackson.databind.JsonNode;
import java.net.SocketTimeoutException;
import java.util.Base64;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
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

    public ResponseEntity<String> enviarVendas(Configuracao configuracao, Notasai venda, Integer idLocal, String nrcaixa) {

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

                ResponseEntity<String> response = restTemplate.exchange(endPoint, HttpMethod.POST, new HttpEntity<>(venda, headers), String.class);

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

    private MultiValueMap<String, String> createHeaders(String usuario, String senha) {
        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        String authorization = usuario + ":" + senha;
        authorization = Base64.getEncoder().encodeToString(authorization.getBytes());
        headers.add("Authorization", "Basic " + authorization);
        return headers;
    }

}
