package com.connecto.controller;

import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
@Controller
public class IndexController    {

    @RequestMapping(value = "/{path:[^\\.]*}")  // Matches all routes that don't contain a period (.)
    public String forward() {
        return "forward:/index.html";  // Forward to React's index.html
    }
}