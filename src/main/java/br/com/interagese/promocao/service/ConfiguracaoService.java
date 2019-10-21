/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package br.com.interagese.promocao.service;

import br.com.interagese.padrao.rest.util.PadraoService;
import br.com.interagese.postgres.models.Configuracao;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 *
 * @author Bruno Martins
 */
@Service
public class ConfiguracaoService extends PadraoService<Configuracao> {

    @Autowired
    private FilialScanntechService service;

    public Integer getIntervalo() {

        String hql = "SELECT o.intervaloSincronizacao FROM Configuracao o WHERE o.id = 1 ";

        try {
            return em.createQuery(hql, Integer.class).getSingleResult();
        } catch (NoResultException e) {
            return null;
        }

    }
     
}
