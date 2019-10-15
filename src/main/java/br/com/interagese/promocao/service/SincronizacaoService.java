package br.com.interagese.promocao.service;

import br.com.interagese.postgres.models.Sincronizacao;
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

    public Date getDataDaUltimaSincronizacao() {

        String hql = "SELECT s.data FROM Sincronizacao s WHERE s.codigo = (SELECT MAX(s.codigo) FROM Sincronizacao s) ";

        try {

            return em.createQuery(hql, Date.class).getSingleResult();

        } catch (NoResultException e) {
            return new Date();
        }

    }

    public void insertSincronizacao(Date data) {
        Sincronizacao scanntechsinc = new Sincronizacao();
        scanntechsinc.setData(data);
        em.persist(scanntechsinc);
    }

}
