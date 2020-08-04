package guru.sfg.beer.order.service.services;

import guru.sfg.beer.order.service.domain.BeerOrder;
import guru.sfg.brewery.model.events.ValidateOrderResponse;

public interface BeerOrderManager {
    BeerOrder newBeerOrder(BeerOrder beerOrder);
    void handleBeerOrderValidationResponse(ValidateOrderResponse response);
}
