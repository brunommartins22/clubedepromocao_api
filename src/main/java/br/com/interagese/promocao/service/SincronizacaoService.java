package br.com.interagese.promocao.service;

import br.com.interagese.postgres.models.SincronizacaoVenda;
import java.util.Date;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class SincronizacaoService {

    @PersistenceContext(unitName = "default")
    private EntityManager em;

    public Date getDataDaUltimaSincronizacaoDeVenda() {

        String hql = "SELECT s.data FROM SincronizacaoVenda s WHERE s.codigo = (SELECT MAX(s.codigo) FROM SincronizacaoVenda s) ";

        try {

            return em.createQuery(hql, Date.class).getSingleResult();

        } catch (NoResultException e) {
            return new Date();
        }

    }
    
    public Date getDataDaUltimaSincronizacaoDeFechamento() {

        String hql = "SELECT s.data FROM SincronizacaoFechamento s WHERE s.codigo = (SELECT MAX(s.codigo) FROM SincronizacaoFechamento s) ";

        try {

            return em.createQuery(hql, Date.class).getSingleResult();

        } catch (NoResultException e) {
            return new Date();
        }

    }

    public void insertSincronizacaoVenda(Date data) {
        SincronizacaoVenda scanntechsinc = new SincronizacaoVenda();
        scanntechsinc.setData(data);
        em.persist(scanntechsinc);
    }

}
