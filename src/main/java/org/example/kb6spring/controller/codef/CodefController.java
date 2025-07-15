package org.example.kb6spring.controller.codef;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.MediaType;

import javax.crypto.Cipher;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;

@Controller
@RequestMapping("/codef")
public class CodefController {

    private static final ObjectMapper mapper = new ObjectMapper();

    private static final String OAUTH_DOMAIN = "https://oauth.codef.io";
    private static final String GET_TOKEN_PATH = "/oauth/token";
    private static final String API_DOMAIN = "https://api.codef.io";
    private static final String CREATE_ACCOUNT_PATH = "/v1/account/create";

    private static final String CLIENT_ID = "";
    private static final String CLIENT_SECRET = "";
    private static final String PUBLIC_KEY = ""; // RSA 공개키

    /**
     * 토큰 발급 및 ConnectedId 발급을 한 번에 수행
     */
    @PostMapping(value = "/connected-id", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public HashMap<String, Object> issueConnectedId(HttpServletResponse response) {
        try {
            // 1. Access Token 발급
            HashMap<String, Object> tokenResponse = publishToken(CLIENT_ID, CLIENT_SECRET);
            if (tokenResponse == null || tokenResponse.get("access_token") == null) {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                HashMap<String, Object> errorMap = new HashMap<>();
                errorMap.put("error", "토큰 발급 실패");
                return errorMap;
            }

            String accessToken = (String) tokenResponse.get("access_token");
            String bearerToken = "Bearer " + accessToken;

            // 2. ConnectedId 요청 데이터 구성
            String urlPath = API_DOMAIN + CREATE_ACCOUNT_PATH;

            HashMap<String, Object> bodyMap = new HashMap<>();
            List<HashMap<String, Object>> accountList = new ArrayList<>();

            // 우리은행(공인인증서 로그인)
            HashMap<String, Object> account1 = new HashMap<>();
            account1.put("countryCode", "KR");
            account1.put("businessType", "BK");
            account1.put("clientType", "P");
            account1.put("organization", "0020"); // 우리은행 코드
            account1.put("loginType", "0");       // 공인인증서 로그인

            String certPassword = "";
            account1.put("password", encryptRSA(certPassword, PUBLIC_KEY));
            account1.put("keyFile", "BASE64 인코딩된 인증서 key 파일");
            account1.put("derFile", "BASE64 인코딩된 인증서 der 파일");

            // 3. ConnectedId 발급 API 호출
            String result = apiRequest(urlPath, bearerToken, bodyMap);

            // 4. 응답 반환
            return mapper.readValue(result, new TypeReference<HashMap<String, Object>>() {});
        } catch (Exception e) {
            System.err.println("ConnectedId 발급 중 오류: " + e.getMessage());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            HashMap<String, Object> errorMap = new HashMap<>();
            errorMap.put("error", "ConnectedId 발급 실패");
            return errorMap;
        }
    }

    /**
     * Access Token 발급 메소드
     */
    protected static HashMap<String, Object> publishToken(String clientId, String clientSecret) {
        BufferedReader br = null;
        try {
            URL url = new URL(OAUTH_DOMAIN + GET_TOKEN_PATH);
            String params = "grant_type=client_credentials&scope=read";

            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("POST");
            con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

            String auth = clientId + ":" + clientSecret;
            String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
            con.setRequestProperty("Authorization", "Basic " + encodedAuth);
            con.setDoInput(true);
            con.setDoOutput(true);

            try (OutputStream os = con.getOutputStream()) {
                os.write(params.getBytes(StandardCharsets.UTF_8));
                os.flush();
            }

            int responseCode = con.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                br = new BufferedReader(new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8));
            } else {
                System.err.println("CODEF 토큰 발급 실패 - HTTP 응답 코드: " + responseCode);
                return null;
            }

            StringBuilder responseStr = new StringBuilder();
            String inputLine;
            while ((inputLine = br.readLine()) != null) {
                responseStr.append(inputLine);
            }

            String decodedResponse = URLDecoder.decode(responseStr.toString(), "UTF-8");
            return mapper.readValue(decodedResponse, new TypeReference<HashMap<String, Object>>() {});
        } catch (Exception e) {
            System.err.println("토큰 발급 중 예외 발생: " + e.getMessage());
            return null;
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    /**
     * RSA 암호화 메소드
     */
    public static String encryptRSA(String plainText, String base64PublicKey)
            throws NoSuchAlgorithmException, InvalidKeySpecException, NoSuchPaddingException,
            InvalidKeyException, IllegalBlockSizeException, BadPaddingException {

        byte[] bytePublicKey = Base64.getDecoder().decode(base64PublicKey);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PublicKey publicKey = keyFactory.generatePublic(new X509EncodedKeySpec(bytePublicKey));

        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        byte[] bytePlain = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(bytePlain);
    }

    /**
     * API 호출 메소드
     */
    public static String apiRequest(String apiUrl, String bearerToken, Map<String, Object> body) throws IOException {
        HttpURLConnection con = (HttpURLConnection) new URL(apiUrl).openConnection();
        con.setRequestMethod("POST");
        con.setRequestProperty("Authorization", bearerToken);
        con.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        con.setDoOutput(true);

        try (OutputStream os = con.getOutputStream()) {
            mapper.writeValue(os, body);
            os.flush();
        }

        int responseCode = con.getResponseCode();
        BufferedReader br = new BufferedReader(new InputStreamReader(
                (responseCode == 200) ? con.getInputStream() : con.getErrorStream(),
                StandardCharsets.UTF_8
        ));

        StringBuilder responseStr = new StringBuilder();
        String inputLine;
        while ((inputLine = br.readLine()) != null) {
            responseStr.append(inputLine);
        }
        br.close();

        // URL 디코딩 추가
        String decodedResponse = URLDecoder.decode(responseStr.toString(), "UTF-8");
        return decodedResponse;
    }
}
