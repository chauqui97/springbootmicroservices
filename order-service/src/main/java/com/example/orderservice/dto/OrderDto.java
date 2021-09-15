package com.example.orderservice.dto;

import com.example.orderservice.model.OrderLineItem;
import lombok.Data;

import java.util.List;

@Data
public class OrderDto {
    private List<OrderLineItem> orderLineItems;
}
