
package application.domain.models;


import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Getter
@Setter


public class Shipment {

private String shipmentCode;
private Order order;
private Warehouse originWarehouse;
private String destinationAddress;
private LocalDate shippingDate;
private LocalDate deliveryDate;
private String status;

}