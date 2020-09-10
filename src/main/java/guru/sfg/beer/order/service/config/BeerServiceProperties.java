package guru.sfg.beer.order.service.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "sfg.brewery", ignoreUnknownFields = false)
public class BeerServiceProperties {
    private String beerServiceHost;
}
