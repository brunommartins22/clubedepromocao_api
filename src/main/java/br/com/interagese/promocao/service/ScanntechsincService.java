package br.com.interagese.promocao.service;

import br.com.firebird.models.Sincronizacao;
import java.util.Date;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import org.springframework.stereotype.Service;

@Service
public class ScanntechsincService {

    @PersistenceContext(unitName = "default")
    private EntityManager em;

    public Date getDataDaUltimaSincronizacao() {

        String hql = "SELECT s.data FROM Schanntechsinc s WHERE s.codigo = MAX(s.codigo) ";

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
