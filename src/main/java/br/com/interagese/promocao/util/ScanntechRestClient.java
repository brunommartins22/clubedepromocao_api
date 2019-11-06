package br.com.interagese.promocao.util;

import br.com.firebird.models.Notasai;
import br.com.interagese.postgres.models.ConfiguracaoItem;
import br.com.interagese.postgres.models.FechamentoPromocao;
import br.com.interagese.postgres.models.Url;
import br.com.interagese.promocao.enuns.EstadoPromocao;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

public class ScanntechRestClient extends DefaultResponseErrorHandler {

    private final RestTemplate restTemplate;
    private final ObjectMapper mapper;

    public ScanntechRestClient() {
        restTemplate = new RestTemplate();
        restTemplate.setErrorHandler(this);
        restTemplate.getMessageConverters().add(0, new StringHttpMessageConverter(StandardCharsets.UTF_8));
        mapper = new ObjectMapper();
    }

    public ResponseEntity<JsonNode> buscarPromocoes(ConfiguracaoItem configuracao, EstadoPromocao estado, Integer idLocal) {

        for (int i = 0; i < configuracao.getListaUrl().size(); i++) {

            Url url = configuracao.getListaUrl().get(i);

            try {
                String urlBase = url.getValor();
                String idEmpresa = configuracao.getCodigoEmpresa();
                String usuario = configuracao.getUsuario();
                String senha = configuracao.getSenha();

                String endPoint = urlBase + "/pmkt-rest-api/minoristas/" + idEmpresa + "/locales/" + idLocal + "/promocionesConLimitePorTicket?estado=" + estado.getValorScanntech();

                MultiValueMap<String, String> headers = createHeaders(usuario, senha);

                ResponseEntity<JsonNode> response = restTemplate.exchange(endPoint, HttpMethod.GET, new HttpEntity<>(headers), JsonNode.class);

                return response;

            } catch (RestClientException e) {

                if (i >= configuracao.getListaUrl().size()) {
                    throw e;
                }

                if (!(e.getCause() instanceof SocketTimeoutException)) {
                    throw e;
                }

            }

        }

        return null;

    }

    public ResponseEntity<String> enviarVenda(ConfiguracaoItem configuracao, Notasai venda, Integer idLocal, Integer nrcaixa) throws HttpClientErrorException {

        for (int i = 0; i < configuracao.getListaUrl().size(); i++) {

            int tentativas = 3;

            Url url = configuracao.getListaUrl().get(i);

            for (int j = 0; j < tentativas; j++) {
                try {
                    String urlBase = url.getValor();
                    String idEmpresa = configuracao.getCodigoEmpresa();
                    String usuario = configuracao.getUsuario();
                    String senha = configuracao.getSenha();

                    String endPoint = urlBase + "/api-minoristas/api/v2/minoristas/" + idEmpresa + "/locales/" + idLocal + "/cajas/" + nrcaixa + "/movimientos";

                    MultiValueMap<String, String> headers = createHeaders(usuario, senha);

                    String json = "";
                    try {
                        json = mapper.writeValueAsString(venda);
                        System.out.println("Json: " + json);
                    } catch (JsonProcessingException ex) {
                        Logger.getLogger(ScanntechRestClient.class.getName()).log(Level.SEVERE, null, ex);
                    }

                  ResponseEntity<String> response = restTemplate.exchange(endPoint, HttpMethod.POST, new HttpEntity<>(json, headers), String.class);

                    System.out.println("Status Code: " + response.getStatusCode());
                    System.out.println("Body: " + response.getBody());

                    return response;

                } catch (RestClientException e) {
                    if ((e.getCause() instanceof SocketException)) {
                        continue;
                    }

                    if (!(e.getCause() instanceof SocketTimeoutException)) {
                        throw e;
                    }

                    if (i >= configuracao.getListaUrl().size()) {
                        throw e;
                    }

                }
            }

        }

        return null;

    }

    public ResponseEntity<String> enviarFechamento(ConfiguracaoItem configuracao, FechamentoPromocao fechamento, Integer idLocal, Integer nrcaixa) throws HttpClientErrorException {

        for (int i = 0; i < configuracao.getListaUrl().size(); i++) {

            Url url = configuracao.getListaUrl().get(i);

            try {
                String urlBase = url.getValor();
                String idEmpresa = configuracao.getCodigoEmpresa();
                String usuario = configuracao.getUsuario();
                String senha = configuracao.getSenha();

                String endPoint = urlBase + "/api-minoristas/api/v2/minoristas/" + idEmpresa + "/locales/" + idLocal + "/cajas/" + nrcaixa + "/cierresDiarios";

                MultiValueMap<String, String> headers = createHeaders(usuario, senha);

                ResponseEntity<String> response = restTemplate.exchange(endPoint, HttpMethod.POST, new HttpEntity<>(fechamento, headers), String.class);

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
        headers.add("Content-Type", "application/json");
        return headers;
    }

    @Override
    public boolean hasError(ClientHttpResponse response) throws IOException {
        return !(response.getRawStatusCode() >= 200 && response.getRawStatusCode() <= 599);
    }

}
