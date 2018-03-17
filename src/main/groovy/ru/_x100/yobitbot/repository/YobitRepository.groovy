package ru._x100.yobitbot.repository

import groovy.util.logging.Slf4j
import org.hibernate.Query
import org.hibernate.Session
import org.hibernate.SessionFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
@Transactional
@Slf4j
class YobitRepository {

    @Autowired
    SessionFactory sessionFactory

    Long getNonce() {
        Query query = session.createSQLQuery("select sq_nonce.nextval from dual")

        long nonce = query.uniqueResult() as long
        log.debug("nonce: ${nonce}")

        nonce
    }

    private Session getSession() {
        sessionFactory.currentSession
    }
}
