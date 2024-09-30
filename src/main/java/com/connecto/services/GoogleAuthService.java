package com.connecto.services;

import java.util.Map;

public interface GoogleAuthService {

    Map<String, Object> login(Map<String, Object> request) throws Exception;

    Map<String, Object> register(Map<String, Object> request) throws Exception;
}
