package br.com.interagese.promocao.service;

import br.com.interagese.postgres.models.SincronizacaoFechamento;
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

    public Date getDataDaUltimaSincronizacaoDeVenda(Integer codfil) throws NoResultException{

        String hql = "SELECT s.data FROM SincronizacaoVenda s WHERE s.id = "
                + "(SELECT MAX(s.id) FROM SincronizacaoVenda s WHERE s.codigoFilial = :codfil) ";

            return em.createQuery(hql, Date.class)
                    .setParameter("codfil", codfil)
                    .getSingleResult();

    }
    
    public Date getDataDaUltimaSincronizacaoDeFechamento(Integer codfil) throws NoResultException{

        String hql = "SELECT s.data FROM SincronizacaoFechamento s WHERE s.id = "
                + "(SELECT MAX(s.id) FROM SincronizacaoFechamento s WHERE s.codigoFilial = :codfil) ";

            return em.createQuery(hql, Date.class)
                    .setParameter("codfil", codfil)
                    .getSingleResult();

    }

    @Transactional
    public void insertSincronizacaoVenda(Integer codigoFilial, Date data) {
        SincronizacaoVenda scanntechsinc = new SincronizacaoVenda();
        scanntechsinc.setCodigoFilial(codigoFilial);
        scanntechsinc.setData(data);
        em.persist(scanntechsinc);
    }
    
    @Transactional
    public void insertSincronizacaoFechamento(Integer codigoFilial, Date data) {
        SincronizacaoFechamento sincronizacaoFechamento = new SincronizacaoFechamento();
        sincronizacaoFechamento.setCodigoFilial(codigoFilial);
        sincronizacaoFechamento.setData(data);
        em.persist(sincronizacaoFechamento);
    }

}
