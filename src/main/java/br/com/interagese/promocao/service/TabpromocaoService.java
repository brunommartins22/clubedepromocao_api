package br.com.interagese.promocao.service;

import br.com.interagese.promocao.Tabpromocao;
import br.com.firebird.models.Tabpromoitem;
import br.com.interagese.padrao.rest.util.PadraoService;
import br.com.interagese.promocao.EstadoPromocao;
import com.fasterxml.jackson.databind.JsonNode;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Map;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Service
public class TabpromocaoService extends PadraoService<Tabpromocao> {

    @PersistenceContext(unitName = "integradoPU")
    private EntityManager emFirebird;

    private final SimpleDateFormat dateFormat;

    public TabpromocaoService() {
        this.dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
    }

    public Map<String, ?> load(Object id, String idName, List<String> fields) {

        try {
            StringBuilder hql = new StringBuilder();

            // String columns = String.join(",", fields);
            StringBuilder columns = new StringBuilder();
            String delimiter = "";
            for (String f : fields) {
                String value[] = f.split(",");
                for (int i = 0; i < value.length; i++) {
                    columns.append(delimiter).append(" ");
                    columns.append(value[i]).append(" as ").append(value[i]);
                    delimiter = ",";
                }
            }

            hql.append(" select new Map(").append(columns).append(") ");
            hql.append(" from Tabpromoitem ");
            hql.append(" where ").append(idName).append(" = :id ");
            Map<String, ?> result = (Map<String, ?>) emFirebird.createQuery(hql.toString()).setParameter("id", id)
                    .getSingleResult();

            return result;
        } catch (NoResultException ex) {
            return null;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public Tabpromocao create(Tabpromocao obj) throws Exception {

        int codMax = getCodMax();

        obj.setCodpromocao(codMax);

        int i = 1;
        for (Tabpromoitem tabpromoitem : obj.getItemList()) {
            tabpromoitem.getTabpromoitemPK().setCodpromocao(codMax);
            tabpromoitem.getTabpromoitemPK().setCoditem(i);
            i++;
        }

        for (Tabpromoitem tabpromoitem : obj.getBeneficioList()) {
            tabpromoitem.getTabpromoitemPK().setCodpromocao(codMax);
            tabpromoitem.getTabpromoitemPK().setCoditem(i);
            i++;
        }

        emFirebird.persist(obj);
        for (Tabpromoitem tabpromoitem : obj.getItemList()) {
            emFirebird.persist(tabpromoitem);
        }
        for (Tabpromoitem tabpromoitem : obj.getBeneficioList()) {
            emFirebird.persist(tabpromoitem);
        }

        return obj;
    }

    private int getCodMax() {

        String hql = "SELECT MAX(t.codpromocao) "
                + "FROM Tabpromocao t ";

        TypedQuery<Number> query = emFirebird.createQuery(hql, Number.class);
        try {
            Number max = query.getSingleResult();
            return max == null ? 1 : max.intValue() + 1;
        } catch (NoResultException e) {
            return 1;
        }

    }

    private void baixarPromocoes(EstadoPromocao estado) throws Exception {
        String urlBase = "http://br.homo.apipdv.scanntech.com";
        String idEmpresa = "31672";
        String idLocal = "1";
        String usuario = "integrador_test@interagese.com.br";
        String senha = "integrador";

        String endpoint = urlBase + "/pmkt-rest-api/minoristas/" + idEmpresa + "/locales/" + idLocal + "/promocionesConLimitePorTicket?estado=" + estado.getValorScanntech();

        RestTemplate restClient = new RestTemplate();
        MultiValueMap<String, String> headers = createHeaders(usuario, senha);
        ResponseEntity<JsonNode> response = restClient.exchange(endpoint, HttpMethod.GET, new HttpEntity<>(headers), JsonNode.class);

        if (response.getStatusCodeValue() == 200) {
            int promocoesInseridas = 0;
            

            List<Tabpromocao> promocoes = convertJsonNodeToTabpromocaoList(response.getBody());
            for (int i = 0; i < promocoes.size(); i++) {
                Tabpromocao promocao = promocoes.get(i);
                Tabpromocao promocaoTemp = loadBycodscanntech(promocao.getCodscanntech());
                if (promocaoTemp != null) {
                    promocaoTemp.setSituacao(estado.getValorInterage());
                    update(promocaoTemp);
                    promocoes.set(i, promocaoTemp);
                } else {

                    promocao.setRgcodusu(1);
                    promocao.setRgusuario("INTER");
                    promocao.setRgdata(new Date());
                    promocao.setRgevento(1);

                    promocao.setSituacao(estado.getValorInterage());
                    create(promocao);
                    promocoesInseridas++;
                }
            }

           
        }
    }

    @Transactional("integradoTransaction")
    public void baixarPromocoes() throws Exception {

        baixarPromocoes(EstadoPromocao.ACEITA);
        baixarPromocoes(EstadoPromocao.PENDENTE);
        baixarPromocoes(EstadoPromocao.REJEITADA);
    }

    private List<Tabpromocao> convertJsonNodeToTabpromocaoList(JsonNode json) throws Exception {

        JsonNode jsonArray = json.get("results");
        List<Tabpromocao> promocoes = new ArrayList();
        for (JsonNode jsonNode : jsonArray) {
            Tabpromocao tabpromocao = new Tabpromocao();
            tabpromocao.setCodscanntech(jsonNode.get("id").asText());
            tabpromocao.setTitulo(jsonNode.get("titulo").asText());
            tabpromocao.setDescricao(jsonNode.get("descripcion").asText());
            tabpromocao.setTipo(convertPromocaoConstantToPromocaoCode(jsonNode.get("tipo").asText()));
            tabpromocao.setAutor(jsonNode.get("autor").get("descripcion").asText());
            tabpromocao.setDatainicio(dateFormat.parse(jsonNode.get("vigenciaDesde").asText()));
            tabpromocao.setDatafim(dateFormat.parse(jsonNode.get("vigenciaHasta").asText()));
            if (jsonNode.has("limitePromocionesPorTicket")) {
                tabpromocao.setLimiteporcupom(jsonNode.get("limitePromocionesPorTicket").asInt());
            }

            JsonNode detalheNode = jsonNode.get("detalles");
            if (detalheNode.get("precio") != null) {
                tabpromocao.setPreco(detalheNode.get("precio").asDouble());
            }
            if (detalheNode.get("descuento") != null) {
                tabpromocao.setDesconto(detalheNode.get("descuento").asDouble());
            }
            if (detalheNode.get("paga") != null) {
                tabpromocao.setPaga(detalheNode.get("paga").asInt());
            }

            List<Tabpromoitem> promoitens = new ArrayList();
            JsonNode condicaoNode = detalheNode.get("condiciones");
            JsonNode itens = condicaoNode.get("items");
            for (JsonNode item : itens) {
                for (JsonNode articulo : item.get("articulos")) {
                    Tabpromoitem tabpromoitem = new Tabpromoitem();
                    tabpromoitem.setCodbarun(articulo.get("codigoBarras").asText());
                    Map<String, String> resultMap = (Map<String, String>) load(tabpromoitem.getCodbarun(), "codbarun", Arrays.asList("codpro", "descpro"));
                    if (resultMap != null) {
                        tabpromoitem.setCodpro(resultMap.get("codpro"));
                        tabpromoitem.setDescpro(resultMap.get("descpro"));
                    } else {
                        String descpro = articulo.get("nombre").asText();
                        if (descpro.length() > 60) {
                            tabpromoitem.setDescpro(descpro.substring(0, 60));
                        } else {
                            tabpromoitem.setDescpro(descpro);
                        }
                    }
                    tabpromoitem.setQuantidade(item.get("cantidad").asDouble());
                    tabpromoitem.setTipo("I");
                    promoitens.add(tabpromoitem);

                }
            }
            tabpromocao.setItemList(promoitens);

            List<Tabpromoitem> promobeneficios = new ArrayList();
            JsonNode beneficios = detalheNode.get("beneficios");
            if (!beneficios.isNull()) {
                for (JsonNode beneficioItem : beneficios.get("items")) {
                    for (JsonNode articulo : beneficioItem.get("articulos")) {
                        Tabpromoitem tabpromoitem = new Tabpromoitem();
                        tabpromoitem.setCodbarun(articulo.get("codigoBarras").asText());
                        Map<String, String> resultMap = (Map<String, String>) load(tabpromoitem.getCodbarun(), "codbarun", Arrays.asList("codpro", "descpro"));
                        if (resultMap != null) {
                            tabpromoitem.setCodpro(resultMap.get("codpro"));
                            tabpromoitem.setDescpro(resultMap.get("descpro"));
                        } else {
                            String descpro = articulo.get("nombre").asText();
                            if (descpro.length() > 60) {
                                tabpromoitem.setDescpro(descpro.substring(0, 60));
                            } else {
                                tabpromoitem.setDescpro(descpro);
                            }
                        }
                        tabpromoitem.setQuantidade(beneficioItem.get("cantidad").asDouble());
                        tabpromoitem.setTipo("B");
                        promobeneficios.add(tabpromoitem);
                    }

                }

            }

            tabpromocao.setBeneficioList(promobeneficios);

            promocoes.add(tabpromocao);
        }

        return promocoes;

    }
    
    public Tabpromocao loadBycodscanntech(String codscann) {
        String hql = "SELECT t FROM Tabpromocao t WHERE t.codscanntech = :codscann ";
        TypedQuery<Tabpromocao> query = emFirebird.createQuery(hql, Tabpromocao.class);
        query.setParameter("codscann", codscann);
        try {
            return query.getSingleResult();
        } catch (NoResultException | NonUniqueResultException e) {
            return null;
        }
    }

    private Integer convertPromocaoConstantToPromocaoCode(String constant) {
        switch (constant) {
            case "ADICIONAL_REGALO":
                return 1;
            case "LLEVA_PAGA":
                return 2;
            case "DESCUENTO_VARIABLE":
                return 3;
            case "ADICIONAL_DESCUENTO":
                return 4;
            case "PRECIO_FIJO":
                return 5;
            case "DESCUENTO_FIJO":
                return 6;
            default:
                return null;
        }
    }

    public MultiValueMap<String, String> createHeaders(String usuario, String senha) {
        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        String authorization = usuario + ":" + senha;
        authorization = Base64.getEncoder().encodeToString(authorization.getBytes());
        headers.add("Authorization", "Basic " + authorization);
        return headers;
    }

    @Override
    public Tabpromocao update(Tabpromocao obj) throws Exception {
        return emFirebird.merge(obj);
    }

}
