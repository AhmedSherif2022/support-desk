package com.supportdesk.common;

import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * The bare base URL has no page of its own; send visitors to the API docs.
 */
@Controller
@Hidden   // keep it out of the OpenAPI spec
public class RootController {

    @GetMapping("/")
    public String root() {
        return "redirect:/swagger-ui/index.html";
    }
}
