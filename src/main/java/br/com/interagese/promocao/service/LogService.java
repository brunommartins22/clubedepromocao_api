package br.com.interagese.promocao.service;

import br.com.interagese.postgres.models.SincronizacaoVendaLog;
import java.util.Date;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LogService {

    @PersistenceContext(name = "default")
    private EntityManager em;

    @Transactional
    public void logVenda(String nrcupom, String nrcontr, Integer numeroCaixa, Integer codigoFilial) {

        SincronizacaoVendaLog log = new SincronizacaoVendaLog();
        log.setNumeroCupom(nrcupom);
        log.setNrcontr(nrcontr);
        log.setSituacao("E");
        log.setNumeroCaixa(numeroCaixa);
        log.setCodigoFilial(codigoFilial);
        log.setDataEnvio(new Date());

        em.persist(log);

    }

    @Transactional
    public void logVendaComErro(String nrcupom, String nrcontr, String erro, Integer numeroCaixa, Integer codigoFilial) {

        SincronizacaoVendaLog log = new SincronizacaoVendaLog();
        log.setNumeroCupom(nrcupom);
        log.setNrcontr(nrcontr);
        log.setSituacao("R");
        log.setDataEnvio(new Date());
        log.setNumeroCaixa(numeroCaixa);
        log.setCodigoFilial(codigoFilial);
        log.setErro(erro.substring(0, Math.min(erro.length(), 255)));

        em.persist(log);

    }

}
