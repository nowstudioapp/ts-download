package com.ts.download.util;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Locale;

/**
 * Mock HttpServletResponse - 用于将Excel输出到文件
 * 
 * @author TS Team
 */
public class MockHttpServletResponse implements HttpServletResponse {

    private OutputStream outputStream;
    private String characterEncoding = "UTF-8";
    private String contentType;

    public void setOutputStream(OutputStream outputStream) {
        this.outputStream = outputStream;
    }

    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        return new ServletOutputStream() {
            @Override
            public void write(int b) throws IOException {
                outputStream.write(b);
            }

            @Override
            public void write(byte[] b) throws IOException {
                outputStream.write(b);
            }

            @Override
            public void write(byte[] b, int off, int len) throws IOException {
                outputStream.write(b, off, len);
            }

            @Override
            public void flush() throws IOException {
                outputStream.flush();
            }

            @Override
            public void close() throws IOException {
                outputStream.close();
            }

            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setWriteListener(javax.servlet.WriteListener writeListener) {
            }
        };
    }

    @Override
    public void setCharacterEncoding(String charset) {
        this.characterEncoding = charset;
    }

    @Override
    public String getCharacterEncoding() {
        return characterEncoding;
    }

    @Override
    public void setContentType(String type) {
        this.contentType = type;
    }

    @Override
    public String getContentType() {
        return contentType;
    }

    // 以下方法为空实现
    @Override
    public void addCookie(Cookie cookie) {}

    @Override
    public boolean containsHeader(String name) {
        return false;
    }

    @Override
    public String encodeURL(String url) {
        return url;
    }

    @Override
    public String encodeRedirectURL(String url) {
        return url;
    }

    @Override
    public String encodeUrl(String url) {
        return url;
    }

    @Override
    public String encodeRedirectUrl(String url) {
        return url;
    }

    @Override
    public void sendError(int sc, String msg) throws IOException {}

    @Override
    public void sendError(int sc) throws IOException {}

    @Override
    public void sendRedirect(String location) throws IOException {}

    @Override
    public void setDateHeader(String name, long date) {}

    @Override
    public void addDateHeader(String name, long date) {}

    @Override
    public void setHeader(String name, String value) {}

    @Override
    public void addHeader(String name, String value) {}

    @Override
    public void setIntHeader(String name, int value) {}

    @Override
    public void addIntHeader(String name, int value) {}

    @Override
    public void setStatus(int sc) {}

    @Override
    public void setStatus(int sc, String sm) {}

    @Override
    public int getStatus() {
        return 200;
    }

    @Override
    public String getHeader(String name) {
        return null;
    }

    @Override
    public Collection<String> getHeaders(String name) {
        return null;
    }

    @Override
    public Collection<String> getHeaderNames() {
        return null;
    }

    @Override
    public PrintWriter getWriter() throws IOException {
        return null;
    }

    @Override
    public void setContentLength(int len) {}

    @Override
    public void setContentLengthLong(long len) {}

    @Override
    public void setBufferSize(int size) {}

    @Override
    public int getBufferSize() {
        return 0;
    }

    @Override
    public void flushBuffer() throws IOException {}

    @Override
    public void resetBuffer() {}

    @Override
    public boolean isCommitted() {
        return false;
    }

    @Override
    public void reset() {}

    @Override
    public void setLocale(Locale loc) {}

    @Override
    public Locale getLocale() {
        return null;
    }
}
