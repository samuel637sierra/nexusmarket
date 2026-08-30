package application.domain.models;


import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Getter
@Setter
@NoArgsConstructor

public class Warehouse {

  private String warehouseCode;
  private String name;
  private String address;
  private int capacity;
}
