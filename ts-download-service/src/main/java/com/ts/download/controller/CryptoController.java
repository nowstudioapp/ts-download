package com.ts.download.controller;

import com.ts.download.crypto.RsaUtil;
import com.ts.download.domain.vo.R;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Api(tags = "加密")
@RestController
@RequestMapping("/api/crypto")
public class CryptoController {

    @Autowired
    private RsaUtil rsaUtil;

    @GetMapping("/publicKey")
    @ApiOperation("获取RSA公钥")
    public R<String> getPublicKey() {
        return R.ok(rsaUtil.getPublicKeyBase64(), "获取成功");
    }
}
