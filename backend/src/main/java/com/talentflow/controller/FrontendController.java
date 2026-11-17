package com.talentflow.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class FrontendController {

    @GetMapping({"/", "/register", "/profile", "/hr"})
    public String serveFrontend() {
        return "forward:/index.html";
    }
}
