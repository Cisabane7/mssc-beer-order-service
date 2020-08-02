package guru.sfg.beer.order.service.web.mappers;

import guru.sfg.beer.order.service.domain.BeerOrderLine;
import guru.sfg.beer.order.service.services.beer.BeerService;
import guru.sfg.brewery.model.BeerDto;
import guru.sfg.brewery.model.BeerOrderLineDto;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Objects;

public abstract class BeerOrderLineMapperDecorator implements BeerOrderLineMapper {

    private BeerOrderLineMapper beerOrderLineMapper;
    private BeerService beerService;

    @Override
    public BeerOrderLineDto beerOrderLineToDto(BeerOrderLine line) {
        BeerOrderLineDto dto = beerOrderLineMapper.beerOrderLineToDto(line);
        BeerDto beerDto = Objects.requireNonNull(beerService.getBeerByUpc(dto.getUpc(), false));
        dto.setBeerName(beerDto.getBeerName());
        dto.setBeerStyle(beerDto.getBeerStyle());
        dto.setPrice(beerDto.getPrice());
        return dto;
    }

    @Override
    public BeerOrderLine dtoToBeerOrderLine(BeerOrderLineDto dto) {
        return beerOrderLineMapper.dtoToBeerOrderLine(dto);
    };

    @Autowired
    public void setBeerOrderLineMapper(BeerOrderLineMapper beerOrderLineMapper) {
        this.beerOrderLineMapper = beerOrderLineMapper;
    }
    @Autowired
    public void setBeerService(BeerService beerService) {
        this.beerService = beerService;
    }
}
