package guru.sfg.beer.order.service.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.jenspiegsa.wiremockextension.WireMockExtension;
import com.github.tomakehurst.wiremock.WireMockServer;
import guru.sfg.beer.order.service.config.JmsConfig;
import guru.sfg.beer.order.service.domain.BeerOrder;
import guru.sfg.beer.order.service.domain.BeerOrderLine;
import guru.sfg.beer.order.service.domain.BeerOrderStatusEnum;
import guru.sfg.beer.order.service.domain.Customer;
import guru.sfg.beer.order.service.repositories.BeerOrderRepository;
import guru.sfg.beer.order.service.repositories.CustomerRepository;
import guru.sfg.beer.order.service.services.beer.BeerServiceRestTemplateImpl;
import guru.sfg.brewery.model.BeerDto;
import guru.sfg.brewery.model.events.AllocationFailureEvent;
import guru.sfg.brewery.model.events.DeallocateBeerOrderRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jms.core.JmsTemplate;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static com.github.jenspiegsa.wiremockextension.ManagedWireMockServer.with;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(WireMockExtension.class)
@SpringBootTest
public class BeerOrderManagerImplIT {

    @Autowired
    private BeerOrderManagerImpl beerOrderManager;

    @Autowired
    private BeerOrderRepository beerOrderRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private WireMockServer wireMockServer;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JmsTemplate jmsTemplate;

    private Customer testCustomer;

    UUID beerId = UUID.randomUUID();

    @TestConfiguration
    static class RestTemplateBuilderProvider {

        @Bean(destroyMethod = "stop")
        public WireMockServer wireMockServer() {
            WireMockServer server = with(wireMockConfig().port(8083));
            server.start();
            return server;
        }
    }

    @BeforeEach
    void setUp() {
        testCustomer = customerRepository.save(Customer.builder()
                .customerName("Test Customer")
                .build());
    }

    @Test
    public void testNewToPickup() throws JsonProcessingException {
        BeerDto beerDto = BeerDto.builder()
                .id(beerId)
                .upc("12345")
                .build();
        wireMockServer.stubFor(get(BeerServiceRestTemplateImpl.BEER_PATH + "/beerUpc/12345?showInventoryOnHand=false")
                .willReturn(okJson(objectMapper.writeValueAsString(beerDto))));

        BeerOrder beerOrder = createBeerOrder();

        BeerOrder savedBeerOrder = beerOrderManager.newBeerOrder(beerOrder);

        await().untilAsserted(() -> {
            BeerOrder found = beerOrderRepository.findById(beerOrder.getId()).get();
            assertEquals(BeerOrderStatusEnum.ALLOCATED, found.getOrderStatus());
        });

        beerOrderManager.beerOrderPickedUp(beerOrder.getId());

        await().untilAsserted(() -> {
            BeerOrder found = beerOrderRepository.findById(beerOrder.getId()).get();
            assertEquals(BeerOrderStatusEnum.PICKED_UP, found.getOrderStatus());
        });

        BeerOrder pickedUpBeerOrder = beerOrderRepository.findById(beerOrder.getId()).get();
        assertEquals(BeerOrderStatusEnum.PICKED_UP,pickedUpBeerOrder.getOrderStatus());
    }
    @Test
    public void testNewToAllocated() throws JsonProcessingException, InterruptedException {
        BeerDto beerDto = BeerDto.builder()
                .id(beerId)
                .upc("12345")
                .build();
        wireMockServer.stubFor(get(BeerServiceRestTemplateImpl.BEER_PATH + "/beerUpc/12345?showInventoryOnHand=false")
                .willReturn(okJson(objectMapper.writeValueAsString(beerDto))));

        BeerOrder beerOrder = createBeerOrder();

        BeerOrder savedBeerOrder = beerOrderManager.newBeerOrder(beerOrder);

        await().untilAsserted(() -> {
            BeerOrder found = beerOrderRepository.findById(beerOrder.getId()).get();
            assertEquals(BeerOrderStatusEnum.ALLOCATED, found.getOrderStatus());
        });

        await().untilAsserted(() -> {
            BeerOrder found = beerOrderRepository.findById(beerOrder.getId()).get();
            BeerOrderLine line = found.getBeerOrderLines().iterator().next();
            assertNotNull(line);
            assertEquals(line.getQuantityAllocated(), line.getOrderQuantity());
        });
        BeerOrder savedBeerOrder2 = beerOrderRepository.findById(beerOrder.getId()).get();

        assertNotNull(savedBeerOrder2);
        assertEquals(BeerOrderStatusEnum.ALLOCATED, savedBeerOrder2.getOrderStatus());
    }

    @Test
    void testFailedValidation() throws JsonProcessingException {
        BeerDto beerDto = BeerDto.builder()
                .id(beerId)
                .upc("12345")
                .build();
        wireMockServer.stubFor(get(BeerServiceRestTemplateImpl.BEER_PATH + "/beerUpc/12345?showInventoryOnHand=false")
                .willReturn(okJson(objectMapper.writeValueAsString(beerDto))));

        BeerOrder beerOrder = createBeerOrder();
        beerOrder.setCustomerRef("fail-validation");

        BeerOrder savedBeerOrder = beerOrderManager.newBeerOrder(beerOrder);

        await().untilAsserted(() -> {
            BeerOrder found = beerOrderRepository.findById(beerOrder.getId()).get();
            assertEquals(BeerOrderStatusEnum.VALIDATION_EXCEPTION, found.getOrderStatus());
        });
    }
    @Test
    void testPendingValidationToCancel() throws JsonProcessingException {
        BeerDto beerDto = BeerDto.builder()
                .id(beerId)
                .upc("12345")
                .build();
        wireMockServer.stubFor(get(BeerServiceRestTemplateImpl.BEER_PATH + "/beerUpc/12345?showInventoryOnHand=false")
                .willReturn(okJson(objectMapper.writeValueAsString(beerDto))));

        BeerOrder beerOrder = createBeerOrder();
        beerOrder.setCustomerRef("dont-validate");

        BeerOrder savedBeerOrder = beerOrderManager.newBeerOrder(beerOrder);

        await().untilAsserted(() -> {
            BeerOrder found = beerOrderRepository.findById(beerOrder.getId()).get();
            assertEquals(BeerOrderStatusEnum.PENDING_VALIDATION, found.getOrderStatus());
        });
        beerOrderManager.cancelOrder(savedBeerOrder.getId());

        await().untilAsserted(() -> {
            BeerOrder found = beerOrderRepository.findById(beerOrder.getId()).get();
            assertEquals(BeerOrderStatusEnum.CANCELLED, found.getOrderStatus());
        });
    }

    @Test
    void testPendingAllocationToCancel() throws JsonProcessingException {
        BeerDto beerDto = BeerDto.builder()
                .id(beerId)
                .upc("12345")
                .build();
        wireMockServer.stubFor(get(BeerServiceRestTemplateImpl.BEER_PATH + "/beerUpc/12345?showInventoryOnHand=false")
                .willReturn(okJson(objectMapper.writeValueAsString(beerDto))));

        BeerOrder beerOrder = createBeerOrder();
        beerOrder.setCustomerRef("dont-allocate");

        BeerOrder savedBeerOrder = beerOrderManager.newBeerOrder(beerOrder);

        await().untilAsserted(() -> {
            BeerOrder found = beerOrderRepository.findById(beerOrder.getId()).get();
            assertEquals(BeerOrderStatusEnum.PENDING_ALLOCATION, found.getOrderStatus());
        });
        beerOrderManager.cancelOrder(savedBeerOrder.getId());

        await().untilAsserted(() -> {
            BeerOrder found = beerOrderRepository.findById(beerOrder.getId()).get();
            assertEquals(BeerOrderStatusEnum.CANCELLED, found.getOrderStatus());
        });
    }

    @Test
    void testAllocatedToCancel() throws JsonProcessingException {
        BeerDto beerDto = BeerDto.builder()
                .id(beerId)
                .upc("12345")
                .build();
        wireMockServer.stubFor(get(BeerServiceRestTemplateImpl.BEER_PATH + "/beerUpc/12345?showInventoryOnHand=false")
                .willReturn(okJson(objectMapper.writeValueAsString(beerDto))));

        BeerOrder beerOrder = createBeerOrder();

        BeerOrder savedBeerOrder = beerOrderManager.newBeerOrder(beerOrder);

        await().untilAsserted(() -> {
            BeerOrder found = beerOrderRepository.findById(beerOrder.getId()).get();
            assertEquals(BeerOrderStatusEnum.ALLOCATED, found.getOrderStatus());
        });
        beerOrderManager.cancelOrder(savedBeerOrder.getId());

        await().untilAsserted(() -> {
            BeerOrder found = beerOrderRepository.findById(beerOrder.getId()).get();
            assertEquals(BeerOrderStatusEnum.CANCELLED, found.getOrderStatus());
        });

        DeallocateBeerOrderRequest allocationFailureEvent = (DeallocateBeerOrderRequest) jmsTemplate.receiveAndConvert(JmsConfig.DEALLOCATE_ORDER_QUEUE);
        assertNotNull(allocationFailureEvent);
        assertEquals(allocationFailureEvent.getBeerOrderDto().getId(), savedBeerOrder.getId());
    }

    @Test
    void testFailedAllocation() throws JsonProcessingException {
        BeerDto beerDto = BeerDto.builder()
                .id(beerId)
                .upc("12345")
                .build();
        wireMockServer.stubFor(get(BeerServiceRestTemplateImpl.BEER_PATH + "/beerUpc/12345?showInventoryOnHand=false")
                .willReturn(okJson(objectMapper.writeValueAsString(beerDto))));

        BeerOrder beerOrder = createBeerOrder();
        beerOrder.setCustomerRef("fail-allocation");

        BeerOrder savedBeerOrder = beerOrderManager.newBeerOrder(beerOrder);

        await().untilAsserted(() -> {
            BeerOrder found = beerOrderRepository.findById(beerOrder.getId()).get();
            assertEquals(BeerOrderStatusEnum.ALLOCATION_EXCEPTION, found.getOrderStatus());
        });

        AllocationFailureEvent allocationFailureEvent = (AllocationFailureEvent) jmsTemplate.receiveAndConvert(JmsConfig.ALLOCATION_FAILURE_QUEUE);
        assertNotNull(allocationFailureEvent);
        assertEquals(allocationFailureEvent.getOrderId(), savedBeerOrder.getId());
    }

    @Test
    void testPartialAllocation() throws JsonProcessingException {
        BeerDto beerDto = BeerDto.builder()
                .id(beerId)
                .upc("12345")
                .build();
        wireMockServer.stubFor(get(BeerServiceRestTemplateImpl.BEER_PATH + "/beerUpc/12345?showInventoryOnHand=false")
                .willReturn(okJson(objectMapper.writeValueAsString(beerDto))));

        BeerOrder beerOrder = createBeerOrder();
        beerOrder.setCustomerRef("partial-allocation");

        BeerOrder savedBeerOrder = beerOrderManager.newBeerOrder(beerOrder);

        await().untilAsserted(() -> {
            BeerOrder found = beerOrderRepository.findById(beerOrder.getId()).get();
            assertEquals(BeerOrderStatusEnum.PENDING_INVENTORY, found.getOrderStatus());
        });
    }

    public BeerOrder createBeerOrder() {
        BeerOrder beerOrder = BeerOrder.builder()
                .customer(testCustomer)
                .build();

        Set<BeerOrderLine> lines = new HashSet<>();
        lines.add(BeerOrderLine.builder()
                .beerId(beerId)
                .upc("12345")
                .beerOrder(beerOrder)
                .orderQuantity(1)
                .build());
        beerOrder.setBeerOrderLines(lines);

        return beerOrder;
    }
}
