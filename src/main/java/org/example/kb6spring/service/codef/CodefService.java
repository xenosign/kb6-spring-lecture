package org.example.kb6spring.service.codef;


import io.codef.api.EasyCodef;
import io.codef.api.EasyCodefServiceType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class CodefService {
    @Autowired
    private EasyCodef easyCodef;

    public String getAccountInfo(String connectedId, String organization, String accountNumber) {
        HashMap<String, Object> params = new HashMap<>();
        params.put("connectedId", connectedId);
        params.put("organization", organization);
        params.put("accountNumber", accountNumber);

        try {
            return easyCodef.requestProduct(
                    "/v1/account/account-info",
                    EasyCodefServiceType.DEMO,  // 혹은 EasyCodefServiceType.API(운영)
                    params
            );
        } catch (Exception e) {
            throw new RuntimeException("CODEF API 호출 실패: " + e.getMessage(), e);
        }
    }

    public String createConnectedId(HashMap<String, Object> requestBody) {
        try {
            return easyCodef.createAccount(EasyCodefServiceType.DEMO, requestBody);
        } catch (Exception e) {
            throw new RuntimeException("CODEF API connectedId 생성 실패: " + e.getMessage(), e);
        }
    }
}