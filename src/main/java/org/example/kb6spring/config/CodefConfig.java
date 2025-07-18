package org.example.kb6spring.config;

import io.codef.api.EasyCodef;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CodefConfig {
    private static final String CODEF_CLIENT_ID = "";
    private static final String CODEF_CLIENT_SECRET = "";
    private static final String CODEF_PUBLIC_KEY = "";

    @Bean
    public EasyCodef easyCodef() {
        EasyCodef codef = new EasyCodef();
        codef.setClientInfoForDemo(CODEF_CLIENT_ID, CODEF_CLIENT_SECRET); // 개발환경
        // codef.setClientInfoForProd("YOUR_CLIENT_ID", "YOUR_CLIENT_SECRET"); // 운영환경 전환 시
        codef.setPublicKey(CODEF_PUBLIC_KEY);

        return codef;
    }
}
