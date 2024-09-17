package com.connecto.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
@Controller
public class IndexController    {

    @GetMapping(value = { "/auth/**", "/app/**", "/{path:[^\\.]*}" }) // Matches all routes that don't contain a period (.)
    public String forward() {
        return "forward:/index.html";  // Forward to React's index.html
    }
}