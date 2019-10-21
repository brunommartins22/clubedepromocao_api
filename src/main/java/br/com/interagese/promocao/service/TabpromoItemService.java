/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package br.com.interagese.promocao.service;

import br.com.firebird.models.Tabpromoitem;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import org.springframework.stereotype.Service;

/**
 *
 * @author Bruno Martins
 */
@Service
public class TabpromoItemService {

    @PersistenceContext(unitName = "integradoPU")
    private EntityManager emFirebird;

    public List<Tabpromoitem> findTabpromoitemByCodigopromocao(Integer codigoPromocao) {

        TypedQuery<Tabpromoitem> result = emFirebird.createQuery("SELECT o FROM Tabpromoitem o WHERE o.tabpromocao.codpromocao = " + codigoPromocao, Tabpromoitem.class);

        return result.getResultList();
    }

}
