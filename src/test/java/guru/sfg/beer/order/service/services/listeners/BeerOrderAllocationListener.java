package guru.sfg.beer.order.service.services.listeners;

import guru.sfg.beer.order.service.config.JmsConfig;
import guru.sfg.brewery.model.events.AllocateBeerOrderRequest;
import guru.sfg.brewery.model.events.AllocateOrderResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;


@Component
@Slf4j
@RequiredArgsConstructor
public class BeerOrderAllocationListener {
    private final JmsTemplate jmsTemplate;

    @JmsListener(destination = JmsConfig.ALLOCATE_ORDER_QUEUE)
    public void listen(Message message) {
        AllocateBeerOrderRequest request = (AllocateBeerOrderRequest) message.getPayload();

        boolean allocationError = false;
        boolean pendingInventory = false;
        boolean sendMessage = true;
        if ("fail-allocation".equals(request.getBeerOrderDto().getCustomerRef())) {
            allocationError = true;
        } else if ("partial-allocation".equals(request.getBeerOrderDto().getCustomerRef())) {
            pendingInventory = true;
        } else if ("dont-allocate".equals(request.getBeerOrderDto().getCustomerRef())) {
            sendMessage = false;
        }

        boolean finalPendingInventory = pendingInventory;

        request.getBeerOrderDto().getBeerOrderLines().forEach(beerOrderLineDto -> {
            if (finalPendingInventory) {
                beerOrderLineDto.setQuantityAllocated(beerOrderLineDto.getOrderQuantity() - 1);
            } else {
                beerOrderLineDto.setQuantityAllocated(beerOrderLineDto.getOrderQuantity());
            }
        });
        if (sendMessage) {
            jmsTemplate.convertAndSend(JmsConfig.ALLOCATE_ORDER_RESPONSE_QUEUE, AllocateOrderResult.builder()
                    .beerOrderDto(request.getBeerOrderDto())
                    .allocationError(allocationError)
                    .pendingInventory(pendingInventory)
                    .build());
        }
    }
}
