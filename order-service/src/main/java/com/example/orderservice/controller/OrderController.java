package com.example.orderservice.controller;

import com.example.orderservice.client.InventoryClient;
import com.example.orderservice.dto.OrderDto;
import com.example.orderservice.model.Order;
import com.example.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JCircuitBreaker;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JCircuitBreakerFactory;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;

@RestController
@RequestMapping("/api/order")
@RequiredArgsConstructor
@Slf4j
public class OrderController {

    private final OrderRepository orderRepository;
    private final InventoryClient inventoryClient;
    private final Resilience4JCircuitBreakerFactory circuitBreakerFactory;
    private final StreamBridge streamBridge;
    private final ExecutorService traceableExecutorService;

    @PostMapping
    public String placeOrder(@RequestBody OrderDto orderDto) {
        circuitBreakerFactory.configureExecutorService(traceableExecutorService);
        Resilience4JCircuitBreaker circuitBreaker = circuitBreakerFactory.create("inventory");

        Supplier<Boolean> booleanSupplier = () -> orderDto.getOrderLineItems().stream()
                .allMatch(lineItem -> {
                    log.info("Making Call to Inventory Service for SkuCode {}", lineItem.getSkuCode());
                    return inventoryClient.isInStock(lineItem.getSkuCode());
                });
        boolean productsInStock = circuitBreaker.run(booleanSupplier, throwable -> handleErrorCase());

        if (productsInStock) {
            Order order = new Order();
            order.setOrderLineItems(orderDto.getOrderLineItems());
            order.setOrderNumber(UUID.randomUUID().toString());
            orderRepository.save(order);

            log.info("Sending order details to notification service");
            streamBridge.send("notificationEventSupplier-out-0",
                    MessageBuilder.withPayload(order.getId()).build());
            return "Order place successfully";
        }

        return "Order failed, One of the products in the order is not in stock";
    }

    private Boolean handleErrorCase() {
        return false;
    }
}
