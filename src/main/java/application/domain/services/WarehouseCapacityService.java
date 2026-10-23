package application.domain.services;

public interface WarehouseCapacityService {
    int getOccupancy(String warehouseCode);
    int getRemainingCapacity(String warehouseCode);
}