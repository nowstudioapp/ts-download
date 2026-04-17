package com.ts.download.service;

public interface ValidUserService {

    String downloadValidUsers(String type, String countryCode) throws Exception;

    Long countValidUsers(String type, String countryCode);
}
