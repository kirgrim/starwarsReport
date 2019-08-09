package com;

import com.db_session.HibernateUtil;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        //HibernateUtil.getSessionFactory().openSession();
        SpringApplication.run(Application.class, args);
    }
}
