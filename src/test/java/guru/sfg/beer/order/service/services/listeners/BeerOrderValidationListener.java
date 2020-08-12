package guru.sfg.beer.order.service.services.listeners;

import guru.sfg.beer.order.service.config.JmsConfig;
import guru.sfg.brewery.model.events.ValidateBeerOrderRequest;
import guru.sfg.brewery.model.events.ValidateOrderResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;


@Component
@Slf4j
@RequiredArgsConstructor
public class BeerOrderValidationListener {
    private final JmsTemplate jmsTemplate;

    @JmsListener(destination = JmsConfig.VALIDATE_ORDER_QUEUE)
    public void listen(Message message) {
        boolean isValid = true;
        boolean sendMessage = true;
        ValidateBeerOrderRequest request = (ValidateBeerOrderRequest) message.getPayload();
        if("fail-validation".equals(request.getBeerOrderDto().getCustomerRef())) {
            isValid = false;
        } else if ("dont-validate".equals(request.getBeerOrderDto().getCustomerRef())) {
            sendMessage = false;
        }
        if (sendMessage) {
            jmsTemplate.convertAndSend(JmsConfig.VALIDATE_ORDER_RESPONSE_QUEUE, ValidateOrderResponse.builder()
                    .isValid(isValid)
                    .orderId(request.getBeerOrderDto().getId())
                    .build());
        }
    }
}
