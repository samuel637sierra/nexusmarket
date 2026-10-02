package application.domain.models;

import application.domain.valueObjects.SystemRole;
import application.domain.valueObjects.UserStatus;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Vendedor registrado en el marketplace.
 */
public class Seller extends User {

    private String sellerCode;
    private String businessName;
    private long taxIdentificationNumber;
    private final List<Product> products = new ArrayList<>();
    private double totalSales;

    public Seller() {
        setRole(SystemRole.SELLER);
        setStatus(UserStatus.ACTIVE);
    }

    public Seller(long userId, String username, String password, String sellerCode,
                  String businessName, long taxIdentificationNumber, String personId,
                  String fullName, String email, String phoneNumber, String address,
                  LocalDate birthDate) {
        super(userId, username, password, SystemRole.SELLER, UserStatus.ACTIVE,
                personId, fullName, email, phoneNumber, address, birthDate);
        setSellerCode(sellerCode);
        setBusinessName(businessName);
        setTaxIdentificationNumber(taxIdentificationNumber);
    }

    public String getSellerCode() {
        return sellerCode;
    }

    public void setSellerCode(String sellerCode) {
        this.sellerCode = requireText(sellerCode, "El código del vendedor es obligatorio");
    }

    public String getBusinessName() {
        return businessName;
    }

    public void setBusinessName(String businessName) {
        this.businessName = requireText(businessName, "El nombre comercial es obligatorio");
    }

    public long getTaxIdentificationNumber() {
        return taxIdentificationNumber;
    }

    public void setTaxIdentificationNumber(long taxIdentificationNumber) {
        if (taxIdentificationNumber <= 0) {
            throw new IllegalArgumentException("La identificación tributaria es obligatoria");
        }
        this.taxIdentificationNumber = taxIdentificationNumber;
    }

    public void publishProduct(Product product) {
        Objects.requireNonNull(product, "El producto es obligatorio");
        product.publish();
        if (!products.contains(product)) {
            products.add(product);
        }
    }

    public void suspendProduct(Product product) {
        requireOwnedProduct(product);
        product.suspend();
    }

    public List<Product> getProducts() {
        return List.copyOf(products);
    }

    public double calculateSales() {
        return totalSales;
    }

    public void recordSale(double amount) {
        if (!Double.isFinite(amount) || amount < 0) {
            throw new IllegalArgumentException("El monto de venta debe ser finito y no negativo");
        }
        double updatedSales = totalSales + amount;
        if (!Double.isFinite(updatedSales)) {
            throw new ArithmeticException("El total de ventas excede el límite permitido");
        }
        totalSales = updatedSales;
    }

    public void addProduct(Product product) {
        Objects.requireNonNull(product, "El producto es obligatorio");
        if (!products.contains(product)) {
            products.add(product);
        }
    }

    private void requireOwnedProduct(Product product) {
        Objects.requireNonNull(product, "El producto es obligatorio");
        if (!products.contains(product)) {
            throw new IllegalStateException("El producto no pertenece al vendedor");
        }
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }
}