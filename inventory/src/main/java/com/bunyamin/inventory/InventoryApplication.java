package com.bunyamin.inventory;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@SpringBootApplication
public class InventoryApplication {

	public static void main(String[] args) {
		SpringApplication.run(InventoryApplication.class, args);
	}

}
@RestController
class InventoryController{

	private final Map<String,InventoryStatus> status = Map.of("1",new InventoryStatus(false),"2",new InventoryStatus(true));
	@GetMapping("/inventories")
	public InventoryStatus getInventory(@RequestParam("productId") String productId){
		return this.status.getOrDefault(productId,new InventoryStatus(false));
	}
}

@Data
@AllArgsConstructor
class InventoryStatus{
	private boolean exists;
}
