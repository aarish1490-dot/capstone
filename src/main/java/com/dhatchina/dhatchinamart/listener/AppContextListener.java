package com.dhatchina.dhatchinamart.listener;

import com.dhatchina.dhatchinamart.util.DbUtil;
import com.dhatchina.dhatchinamart.util.ServiceRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

@WebListener
public class AppContextListener implements ServletContextListener {

    private static final Logger log = LoggerFactory.getLogger(AppContextListener.class);

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        log.info("DhatchinaMart starting up");
        DbUtil.init();
        if (!DbUtil.isInitialized()) {
            log.info("Database not initialized - running schema and seed");
            DbUtil.runScript("db/schema.sql");
            DbUtil.runScript("db/seed.sql");
        }
        DbUtil.runScript("db/migrations/001_otp_mobile_number.sql");
        ServiceRegistry.init();
        log.info("DhatchinaMart ready");
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        log.info("DhatchinaMart shutting down");
        ServiceRegistry.reset();
        DbUtil.close();
    }
}
