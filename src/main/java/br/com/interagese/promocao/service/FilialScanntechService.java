/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package br.com.interagese.promocao.service;

import br.com.interagese.padrao.rest.util.PadraoService;
import br.com.interagese.postgres.models.FilialScanntech;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.springframework.stereotype.Service;

/**
 *
 * @author Bruno Martins
 */
@Service
public class FilialScanntechService extends PadraoService<FilialScanntech> {

    @PersistenceContext(unitName = "integradoPU")
    private EntityManager emFirebird;

    public List<FilialScanntech> loadAllFilial() {

        List<FilialScanntech> result = new ArrayList<>();

        List<Object[]> resp = emFirebird.createNativeQuery("select codfil,nomfil,codscanntech from tabfil").getResultList();

        for (Object[] object : resp) {
            FilialScanntech filial = new FilialScanntech();
            filial.setCodigoFilial(((Number) object[0]).longValue());
            filial.setNomeFilial(((String) object[1]) != null ? ((String) object[1]) : " ");
            filial.setCodigoScanntech(((Number) object[2]) != null ? ((Number) object[2]).longValue() : 0);
            result.add(filial);
        }

        return result;

    }

    public String loadNameFilialByCodigoFilial(Long codigoFilial) {
        String name = "";

        name = em.createQuery("SELECT o.nomeFilial FROM FilialScanntech o where o.codigoFilial = " + codigoFilial, String.class).getSingleResult();

        return name;
    }

}
