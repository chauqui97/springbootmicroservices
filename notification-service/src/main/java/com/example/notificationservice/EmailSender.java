package com.example.notificationservice;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EmailSender {
    public void sendMail(String orderNumber) {
        System.err.println("Order placed successfully");
        log.info("Order placed successfully - Order number is {}", orderNumber);
        log.info("Email sent");
    }
}
