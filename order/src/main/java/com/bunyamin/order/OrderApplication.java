package com.bunyamin.order;

import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.data.repository.CrudRepository;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;

@SpringBootApplication
@EnableFeignClients
public class OrderApplication {

	public static void main(String[] args) {
		SpringApplication.run(OrderApplication.class, args);
	}


}
@RestController
@RequiredArgsConstructor
class OrderController{

	private final OrderService orderService;

	@PostMapping("/orders")
	@ResponseStatus(HttpStatus.CREATED)
	public void placeOrder(@RequestBody PlaceOrderRequest request){
		this.orderService.placeOrder(request);
	}

}
@Data
@NoArgsConstructor
@AllArgsConstructor
class PlaceOrderRequest{
	private String product;
	private double price;
}

@Service
@RequiredArgsConstructor
class OrderService{

	private final OrderRepository orderRepository;
	private final KafkaTemplate kafkaTemplate;
	@Autowired
	private  InventoryClient inventoryClient;
	public void  placeOrder(PlaceOrderRequest request){
        InventoryStatus status =  inventoryClient.exists(request.getProduct());
		if (!status.isExists()){
			throw new RuntimeException("product does not exists");
		}
		Order order = new Order();
		order.setPrice(request.getPrice());
		order.setProduct(request.getProduct());
		order.setStatus("PLACED");
		Order o = this.orderRepository.save(order);
		//save into db
		this.kafkaTemplate.send("prod.orders.placed",o.getId().toString(),OrderPlacedEvent.builder()
				.product(request.getProduct())
						.orderId(o.getId().intValue())
				.price(request.getPrice()).build());
	}

	@KafkaListener(topics = "prod.orders.shipped",groupId = "order-group")
	public void hadleOrderShippedEvent(String orderId){
		this.orderRepository.findById(Long.valueOf(orderId)).ifPresent(order ->{
			order.setStatus("SHIPPED");
			this.orderRepository.save(order);
		});
	}
}

@Data
@Builder
class OrderPlacedEvent{
	private Integer orderId;
	private String product;
	private double price;
}

interface OrderRepository extends CrudRepository<Order, Long>{

}

@Entity(name = "orders")
@Data
class Order{
	@Id
	@GeneratedValue
	private Long Id;

	private String product;
	private double price;

	private String status;

}

@FeignClient(url = "http://localhost:8092", name = "inventories")
interface InventoryClient{
	@GetMapping("/inventories")
	InventoryStatus exists(@RequestParam("productId") String productId);
}

@Data
class InventoryStatus{
	private boolean exists;
}
