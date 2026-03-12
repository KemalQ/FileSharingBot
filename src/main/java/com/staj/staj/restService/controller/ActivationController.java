package com.staj.staj.restService.controller;

import com.staj.staj.Node.service.impl.UserActivationServiceImpl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/user")
@RestController
public class ActivationController {//обработка запросов по ссылке из письма
    private final UserActivationServiceImpl userActivationService;

    public ActivationController(UserActivationServiceImpl userActivationService) {
        this.userActivationService = userActivationService;
    }

    @RequestMapping(method = RequestMethod.GET, value = "/activation")
    public ResponseEntity<?> activation(@RequestParam("id") String id){
        var res = userActivationService.activation(id);
        if (res){
            return ResponseEntity.ok().body("Registration succesfully completed!");
        }
        return ResponseEntity.internalServerError().build();
    }
}
