package org.example.kb6spring.controller.codef;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.kb6spring.service.codef.CodefService;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    private CodefService codefService;

    @GetMapping("/info")
    @ResponseBody
    public String getAccountInfo(
            @RequestParam String connectedId,
            @RequestParam String organization,
            @RequestParam String accountNumber
    ) {
        return codefService.getAccountInfo(connectedId, organization, accountNumber);
    }

    @PostMapping("/create-connected-id")
    @ResponseBody
    public String createConnectedId(@RequestBody HashMap<String, Object> requestBody) {
        return codefService.createConnectedId(requestBody);
    }
}