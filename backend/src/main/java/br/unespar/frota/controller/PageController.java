package br.unespar.frota.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    @GetMapping("/admin")
    public String adminPage() {
        return "forward:/admin.html";
    }

    @GetMapping("/admin/callback")
    public String adminCallback() {
        return "forward:/admin/callback.html";
    }
}
