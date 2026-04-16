package com.ts.download.crypto;

import lombok.extern.slf4j.Slf4j;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class CryptoFilter implements Filter {

    private RsaUtil rsaUtil;

    public void setRsaUtil(RsaUtil rsaUtil) {
        this.rsaUtil = rsaUtil;
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        String encrypted = request.getHeader("X-Encrypted");
        if (!"true".equals(encrypted)) {
            chain.doFilter(request, response);
            return;
        }

        try {
            String encryptedKey = request.getHeader("X-Encrypted-Key");
            String ivBase64 = request.getHeader("X-Encrypted-IV");

            String aesKeyBase64 = new String(rsaUtil.decryptByPrivateKey(encryptedKey), StandardCharsets.UTF_8);
            byte[] aesKey = Base64.getDecoder().decode(aesKeyBase64);
            byte[] iv = Base64.getDecoder().decode(ivBase64);

            // 解密请求体
            String encryptedBody = readBody(request);
            String decryptedBody = AesUtil.decrypt(encryptedBody, aesKey, iv);

            // 包装解密后的请求
            HttpServletRequest wrappedRequest = new DecryptedRequestWrapper(request, decryptedBody);

            // 包装响应以捕获输出
            CryptoResponseWrapper wrappedResponse = new CryptoResponseWrapper(response);
            chain.doFilter(wrappedRequest, wrappedResponse);

            // 加密响应体
            String originalResponse = wrappedResponse.getCapturedBody();
            if (originalResponse != null && !originalResponse.isEmpty()) {
                String encryptedResponse = AesUtil.encrypt(originalResponse, aesKey, iv);
                response.setContentType("text/plain;charset=UTF-8");
                response.setContentLength(encryptedResponse.getBytes(StandardCharsets.UTF_8).length);
                response.getWriter().write(encryptedResponse);
            }

        } catch (Exception e) {
            log.error("加解密处理失败", e);
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":400,\"msg\":\"加解密失败\"}");
        }
    }

    private String readBody(HttpServletRequest request) throws IOException {
        try (BufferedReader reader = request.getReader()) {
            return reader.lines().collect(Collectors.joining());
        }
    }

    private static class DecryptedRequestWrapper extends HttpServletRequestWrapper {
        private static final String JSON_CONTENT_TYPE = "application/json";
        private final String body;

        public DecryptedRequestWrapper(HttpServletRequest request, String body) {
            super(request);
            this.body = body;
        }

        @Override
        public ServletInputStream getInputStream() {
            ByteArrayInputStream bis = new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8));
            return new ServletInputStream() {
                @Override public boolean isFinished() { return bis.available() == 0; }
                @Override public boolean isReady() { return true; }
                @Override public void setReadListener(ReadListener readListener) {}
                @Override public int read() { return bis.read(); }
            };
        }

        @Override
        public BufferedReader getReader() {
            return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
        }

        @Override
        public String getContentType() {
            return JSON_CONTENT_TYPE;
        }

        @Override
        public String getHeader(String name) {
            if ("Content-Type".equalsIgnoreCase(name)) {
                return JSON_CONTENT_TYPE;
            }
            return super.getHeader(name);
        }

        @Override
        public Enumeration<String> getHeaders(String name) {
            if ("Content-Type".equalsIgnoreCase(name)) {
                return Collections.enumeration(Collections.singletonList(JSON_CONTENT_TYPE));
            }
            return super.getHeaders(name);
        }

        @Override
        public int getContentLength() {
            return body.getBytes(StandardCharsets.UTF_8).length;
        }

        @Override
        public long getContentLengthLong() {
            return body.getBytes(StandardCharsets.UTF_8).length;
        }
    }

    private static class CryptoResponseWrapper extends javax.servlet.http.HttpServletResponseWrapper {
        private final StringWriter stringWriter = new StringWriter();
        private final PrintWriter writer = new PrintWriter(stringWriter);

        public CryptoResponseWrapper(HttpServletResponse response) {
            super(response);
        }

        @Override
        public PrintWriter getWriter() {
            return writer;
        }

        @Override
        public ServletOutputStream getOutputStream() {
            return new ServletOutputStream() {
                @Override public boolean isReady() { return true; }
                @Override public void setWriteListener(WriteListener writeListener) {}
                @Override public void write(int b) { stringWriter.write(b); }
                @Override public void write(byte[] b, int off, int len) {
                    stringWriter.write(new String(b, off, len, StandardCharsets.UTF_8));
                }
            };
        }

        public String getCapturedBody() {
            writer.flush();
            return stringWriter.toString();
        }
    }
}
