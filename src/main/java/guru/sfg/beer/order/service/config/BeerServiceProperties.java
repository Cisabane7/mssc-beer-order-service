package guru.sfg.beer.order.service.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Data
@Component
public class BeerServiceProperties {
    @Value("${sfg.brewery.beerServiceHost}")
    private String beerServiceHost;
}
