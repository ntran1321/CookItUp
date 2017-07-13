package entities;

import java.util.List;

public class IngredientDTO {

	private String name;
	private String quantity;
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getQuantity() {
		return quantity;
	}
	public void setQuantity(String quantity) {
		this.quantity = quantity;
	}
	@Override
	public String toString() {
		return "IngredientDTO [name=" + name + ", quantity=" + quantity + "]";
	}
	

	
}
	
