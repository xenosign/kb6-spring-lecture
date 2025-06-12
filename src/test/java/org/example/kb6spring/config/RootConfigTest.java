package org.example.kb6spring.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import javax.sql.DataSource;
import java.sql.Connection;


@Configuration
@SpringJUnitConfig
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = RootConfig.class)
@Slf4j
@PropertySource("classpath:application.properties")
class RootConfigTest {
    @Autowired
    private SqlSessionFactory sqlSessionFactory;

    @Autowired
    private DataSource dataSource;

    @Test
    @DisplayName("DataSource 연결이 된다.")
    public void dataSource() {
        try(Connection con = dataSource.getConnection()){
            log.info("DataSource 준비 완료");
            log.info("con = {}", con.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    void sqlSessionFactory() {
        try (SqlSession session = sqlSessionFactory.openSession();
             Connection con = session.getConnection()) {
            log.info("SqlSession: {}", session);
            log.info("Connection: {}", con);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}