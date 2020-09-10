package guru.sfg.beer.order.service.services.beer;

import guru.sfg.brewery.model.BeerDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Objects;
import java.util.UUID;

@Component
@Slf4j
public class BeerServiceRestTemplateImpl implements BeerService {

    private RestTemplate restTemplate;
    private String beerServiceHost;
    public static final String BEER_PATH = "/api/v1";

    public BeerServiceRestTemplateImpl(@Value("${sfg.brewery.beerServiceHost}") String beerServiceHost, RestTemplateBuilder restTemplate) {
        this.restTemplate = restTemplate.build();
        this.beerServiceHost = beerServiceHost;
        log.debug("Beer Service Host = {}", this.beerServiceHost);
    }

    @Override
    public BeerDto getBeerByUpc(String upc, Boolean showInventoryOnHand) {
       ResponseEntity<BeerDto> response = restTemplate.exchange(beerServiceHost + BEER_PATH + "/beerUpc/{upc}?showInventoryOnHand={showInventoryOnHand}", HttpMethod.GET, null
                , new ParameterizedTypeReference<BeerDto>() {
                }, (Object) upc, (Object) showInventoryOnHand);
       return Objects.requireNonNull(response.getBody());
    }

    @Override
    public BeerDto getBeerByUUID(UUID id, Boolean showInventoryOnHand) {
        ResponseEntity<BeerDto> response = restTemplate.exchange(beerServiceHost + BEER_PATH + "/beer/{id}?showInventoryOnHand={showInventoryOnHand}", HttpMethod.GET, null
                , new ParameterizedTypeReference<BeerDto>() {
                }, (Object) id, (Object) showInventoryOnHand);
        return Objects.requireNonNull(response.getBody());
    }
}








