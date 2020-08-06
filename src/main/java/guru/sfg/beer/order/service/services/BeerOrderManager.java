package guru.sfg.beer.order.service.services;

import guru.sfg.beer.order.service.domain.BeerOrder;
import guru.sfg.brewery.model.BeerOrderDto;
import guru.sfg.brewery.model.events.ValidateOrderResponse;

public interface BeerOrderManager {
    BeerOrder newBeerOrder(BeerOrder beerOrder);
    void handleBeerOrderValidationResponse(ValidateOrderResponse response);

    void beerOrderAllocationPassed(BeerOrderDto beerOrder);

    void beerOrderAllocationPendingInventory(BeerOrderDto beerOrder);

    void beerOrderAllocationFailed(BeerOrderDto beerOrder);
}
