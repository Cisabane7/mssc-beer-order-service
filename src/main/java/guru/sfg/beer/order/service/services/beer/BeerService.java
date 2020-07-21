package guru.sfg.beer.order.service.services.beer;

import guru.sfg.beer.order.service.services.beer.model.BeerDto;

import java.util.UUID;

public interface BeerService {
    BeerDto getBeerByUpc(String upc, Boolean showInventoryOnHand);
    BeerDto getBeerByUUID(UUID id, Boolean showInventoryOnHand);
}
