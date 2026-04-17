package com.ts.download.service.impl;

import com.ts.download.dao.ValidUserDao;
import com.ts.download.service.ValidUserService;
import com.ts.download.util.LocalFileUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.atomic.AtomicLong;

@Service
@Slf4j
public class ValidUserServiceImpl implements ValidUserService {

    @Autowired
    private ValidUserDao validUserDao;

    @Autowired
    private LocalFileUtil localFileUtil;

    @Override
    public String downloadValidUsers(String type, String countryCode) throws Exception {
        long startTime = System.currentTimeMillis();
        log.info("开始导出有效用户, type={}, countryCode={}", type, countryCode);

        String dateStr = new SimpleDateFormat("yyyyMMdd").format(new Date());
        String folder = "valid_users/" + dateStr;
        String fileName = type + "_valid_users_" + countryCode + "_" + System.currentTimeMillis() + ".txt";

        File tempFile = File.createTempFile("valid_users_", ".txt");
        AtomicLong lineCount = new AtomicLong(0);

        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(tempFile), StandardCharsets.UTF_8))) {

            validUserDao.streamPhones(type, countryCode, (phones) -> {
                try {
                    for (String phone : phones) {
                        writer.write(phone);
                        writer.newLine();
                        lineCount.incrementAndGet();
                    }
                    writer.flush();
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }, 10000);

            writer.flush();
        }

        if (lineCount.get() == 0) {
            tempFile.delete();
            throw new RuntimeException("该国家没有有效用户数据");
        }

        String downloadUrl = localFileUtil.saveToLocal(tempFile, folder);
        tempFile.delete();

        long elapsed = System.currentTimeMillis() - startTime;
        log.info("有效用户导出完成, type={}, countryCode={}, 行数={}, 耗时={}ms",
                type, countryCode, lineCount.get(), elapsed);

        return downloadUrl;
    }

    @Override
    public Long countValidUsers(String type, String countryCode) {
        return validUserDao.countByCountry(type, countryCode);
    }
}
