package application.api;

import application.domain.cmd.AddStockCommand;
import application.domain.cmd.AuthenticateUserCommand;
import application.domain.cmd.CreateProductCommand;
import application.domain.cmd.CreateShipmentCommand;
import application.domain.cmd.DeliverShipmentCommand;
import application.domain.cmd.DispatchShipmentCommand;
import application.domain.cmd.GenerateInvoiceCommand;
import application.domain.cmd.ProcessPaymentCommand;
import application.domain.cmd.RegisterCustomerCommand;
import application.domain.cmd.RegisterSellerCommand;
import application.domain.cmd.SuspendProductCommand;
import application.domain.cmd.UpdateProductCommand;
import application.domain.models.Product;
import application.domain.models.Shipment;
import application.domain.models.Warehouse;
import application.domain.ports.out.InvoiceRepositoryPort;
import application.domain.ports.out.ShipmentRepositoryPort;
import application.domain.ports.out.WarehouseRepositoryPort;
import application.domain.services.CartCheckoutService;
import application.domain.services.CustomerCartService;
import application.domain.services.CustomerOrderSummaryService;
import application.domain.services.InventoryService;
import application.domain.services.InvoiceService;
import application.domain.services.MarketplaceReportService;
import application.domain.services.OrderHistoryService;
import application.domain.services.OrderService;
import application.domain.services.ProductAvailabilityService;
import application.domain.services.ProductCatalogAnalyticsService;
import application.domain.services.ProductDiscoveryService;
import application.domain.services.ProductService;
import application.domain.services.SellerSalesAnalyticsService;
import application.domain.services.ShipmentAnalyticsService;
import application.domain.services.ShipmentService;
import application.domain.services.UserManagementService;
import application.domain.services.WarehouseCapacityService;
import application.domain.valueObjects.ProductType;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import static application.api.ApiViews.*;

@RestController
@RequestMapping("/api")
public class MarketplaceController {
    private final UserManagementService users;
    private final ProductService products;
    private final ProductDiscoveryService productDiscovery;
    private final ProductAvailabilityService availability;
    private final ProductCatalogAnalyticsService productAnalytics;
    private final CustomerCartService carts;
    private final CartCheckoutService checkout;
    private final OrderService orders;
    private final OrderHistoryService orderHistory;
    private final CustomerOrderSummaryService customerSummary;
    private final InventoryService inventory;
    private final ShipmentService shipments;
    private final ShipmentRepositoryPort shipmentRepository;
    private final ShipmentAnalyticsService shipmentAnalytics;
    private final WarehouseRepositoryPort warehouses;
    private final WarehouseCapacityService warehouseCapacity;
    private final InvoiceService invoices;
    private final InvoiceRepositoryPort invoiceRepository;
    private final SellerSalesAnalyticsService sellerAnalytics;
    private final MarketplaceReportService reports;
    private final MarketplaceAccessPolicy access;

    public MarketplaceController(UserManagementService users, ProductService products,
                                 ProductDiscoveryService productDiscovery,
                                 ProductAvailabilityService availability,
                                 ProductCatalogAnalyticsService productAnalytics,
                                 CustomerCartService carts, CartCheckoutService checkout,
                                 OrderService orders, OrderHistoryService orderHistory,
                                 CustomerOrderSummaryService customerSummary,
                                 InventoryService inventory, ShipmentService shipments,
                                 ShipmentRepositoryPort shipmentRepository,
                                 ShipmentAnalyticsService shipmentAnalytics,
                                 WarehouseRepositoryPort warehouses,
                                 WarehouseCapacityService warehouseCapacity,
                                 InvoiceService invoices, InvoiceRepositoryPort invoiceRepository,
                                 SellerSalesAnalyticsService sellerAnalytics,
                                 MarketplaceReportService reports, MarketplaceAccessPolicy access) {
        this.users = users;
        this.products = products;
        this.productDiscovery = productDiscovery;
        this.availability = availability;
        this.productAnalytics = productAnalytics;
        this.carts = carts;
        this.checkout = checkout;
        this.orders = orders;
        this.orderHistory = orderHistory;
        this.customerSummary = customerSummary;
        this.inventory = inventory;
        this.shipments = shipments;
        this.shipmentRepository = shipmentRepository;
        this.shipmentAnalytics = shipmentAnalytics;
        this.warehouses = warehouses;
        this.warehouseCapacity = warehouseCapacity;
        this.invoices = invoices;
        this.invoiceRepository = invoiceRepository;
        this.sellerAnalytics = sellerAnalytics;
        this.reports = reports;
        this.access = access;
    }

    @PostMapping("/auth/register/customer")
    public UserView registerCustomer(@RequestBody RegisterCustomerCommand command) {
        return UserView.from(users.registerCustomer(command));
    }

    @PostMapping("/auth/register/seller")
    public UserView registerSeller(@RequestBody RegisterSellerCommand command) {
        return UserView.from(users.registerSeller(command));
    }

    @PostMapping("/auth/login")
    public ResponseEntity<UserView> login(@RequestBody AuthenticateUserCommand command) {
        return users.authenticateUser(command)
                .map(user -> ResponseEntity.ok(UserView.from(user)))
                .orElseGet(() -> ResponseEntity.status(401).build());
    }

    @GetMapping("/products")
    public List<ProductView> listProducts(@RequestParam(required = false) String search,
                                          @RequestParam(required = false) ProductType type) {
        List<Product> results = search == null || search.isBlank()
                ? productDiscovery.findPublished()
                : productDiscovery.searchPublished(search);
        if (type != null) {
            results = results.stream().filter(product -> product.getType() == type).toList();
        }
        return results.stream().map(ProductView::from).toList();
    }

    @GetMapping("/products/{productCode}")
    public ProductView getProduct(@PathVariable String productCode) {
        Product product = products.findProduct(productCode);
        if (!product.isPublished()) {
            throw new IllegalArgumentException("Producto no encontrado: " + productCode);
        }
        return ProductView.from(product);
    }

    @PostMapping("/products")
    public ProductView createProduct(@RequestBody CreateProductCommand command, Authentication authentication) {
        access.requireSellerAccess(command.sellerCode(), authentication);
        return ProductView.from(products.createProduct(command));
    }

    @PutMapping("/products/{productCode}")
    public ProductView updateProduct(@PathVariable String productCode,
                                     @RequestBody UpdateProductRequest request,
                                     Authentication authentication) {
        access.requireProductSellerAccess(productCode, authentication);
        return ProductView.from(products.updateProduct(new UpdateProductCommand(
                productCode, request.name(), request.description(), request.price())));
    }

    @PostMapping("/products/{productCode}/publish")
    public ProductView publishProduct(@PathVariable String productCode, Authentication authentication) {
        access.requireProductSellerAccess(productCode, authentication);
        return ProductView.from(products.publishProduct(productCode));
    }

    @PostMapping("/products/{productCode}/suspend")
    public Map<String, String> suspendProduct(@PathVariable String productCode, Authentication authentication) {
        access.requireProductSellerAccess(productCode, authentication);
        products.suspendProduct(new SuspendProductCommand(productCode));
        return Map.of("status", "SUSPENDED");
    }

    @GetMapping("/products/{productCode}/availability")
    public Map<String, Object> getAvailability(@PathVariable String productCode) {
        if (!products.findProduct(productCode).isPublished()) {
            throw new IllegalArgumentException("Producto no encontrado: " + productCode);
        }
        return Map.of("productCode", productCode,
                "total", availability.getTotalAvailable(productCode));
    }

    @GetMapping("/products/analytics")
    public Map<String, Object> getProductAnalytics() {
        return Map.of("countByType", productAnalytics.countPublishedProductsByType(),
                "averagePriceByType", productAnalytics.averagePublishedPriceByType());
    }

    @GetMapping("/customers/{customerId}/cart")
    public CartView getCart(@PathVariable String customerId, Authentication authentication) {
        access.requireCustomerAccess(customerId, authentication);
        return CartView.from(carts.findCustomer(customerId));
    }

    @PostMapping("/customers/{customerId}/cart/items")
    public CartView addToCart(@PathVariable String customerId, @RequestBody CartItemRequest request,
                              Authentication authentication) {
        access.requireCustomerAccess(customerId, authentication);
        carts.addProduct(customerId, request.productCode(), request.quantity());
        return CartView.from(carts.findCustomer(customerId));
    }

    @DeleteMapping("/customers/{customerId}/cart/items/{productCode}")
    public CartView removeFromCart(@PathVariable String customerId, @PathVariable String productCode,
                                   Authentication authentication) {
        access.requireCustomerAccess(customerId, authentication);
        carts.removeProduct(customerId, productCode);
        return CartView.from(carts.findCustomer(customerId));
    }

    @DeleteMapping("/customers/{customerId}/cart")
    public Map<String, String> clearCart(@PathVariable String customerId, Authentication authentication) {
        access.requireCustomerAccess(customerId, authentication);
        carts.clearCart(customerId);
        return Map.of("status", "cleared");
    }

    @PostMapping("/customers/{customerId}/checkout")
    public OrderView checkout(@PathVariable String customerId, Authentication authentication) {
        access.requireCustomerAccess(customerId, authentication);
        return OrderView.from(checkout.checkout(customerId));
    }

    @GetMapping("/customers/{customerId}/orders")
    public List<OrderView> orderHistory(@PathVariable String customerId, Authentication authentication) {
        access.requireCustomerAccess(customerId, authentication);
        return orderHistory.findByCustomer(customerId).stream().map(OrderView::from).toList();
    }

    @GetMapping("/customers/{customerId}/summary")
    public CustomerOrderSummaryService.Summary customerSummary(@PathVariable String customerId,
                                                               Authentication authentication) {
        access.requireCustomerAccess(customerId, authentication);
        return customerSummary.summarize(customerId);
    }

    @GetMapping("/orders/{orderCode}")
    public OrderView getOrder(@PathVariable String orderCode, Authentication authentication) {
        access.requireOrderAccess(orderCode, authentication);
        return OrderView.from(orders.getOrder(orderCode));
    }

    @PostMapping("/orders/{orderCode}/payment")
    public Map<String, Object> payOrder(@PathVariable String orderCode, Authentication authentication) {
        access.requireOrderAccess(orderCode, authentication);
        boolean paid = orders.processPayment(new ProcessPaymentCommand(orderCode));
        return Map.of("orderCode", orderCode, "paid", paid,
                "status", orders.getOrder(orderCode).getStatus().name());
    }

    @PostMapping("/warehouses")
    public WarehouseView createWarehouse(@RequestBody CreateWarehouseRequest request) {
        Warehouse warehouse = new Warehouse(request.warehouseCode(), request.name(),
                request.address(), request.capacity());
        return WarehouseView.from(warehouses.save(warehouse));
    }

    @PostMapping("/inventory/stock")
    public Map<String, Object> addStock(@RequestBody AddStockCommand command, Authentication authentication) {
        access.requireWarehouseAccess(command.warehouseCode(), authentication);
        inventory.addStock(command);
        return Map.of("productCode", command.productCode(), "warehouseCode", command.warehouseCode(),
                "available", inventory.getAvailableStock(command.productCode(), command.warehouseCode()));
    }

    @PostMapping("/shipments")
    public ShipmentView createShipment(@RequestBody CreateShipmentCommand command, Authentication authentication) {
        access.requireShipmentCreationAccess(command.orderCode(), command.warehouseCode(), authentication);
        return ShipmentView.from(shipments.createShipment(command));
    }

    @PostMapping("/shipments/{shipmentCode}/dispatch")
    public ShipmentView dispatchShipment(@PathVariable String shipmentCode, Authentication authentication) {
        access.requireShipmentAccess(shipmentCode, authentication);
        shipments.dispatchShipment(new DispatchShipmentCommand(shipmentCode));
        return getShipment(shipmentCode, authentication);
    }

    @PostMapping("/shipments/{shipmentCode}/deliver")
    public ShipmentView deliverShipment(@PathVariable String shipmentCode, Authentication authentication) {
        access.requireShipmentAccess(shipmentCode, authentication);
        shipments.deliverShipment(new DeliverShipmentCommand(shipmentCode));
        return getShipment(shipmentCode, authentication);
    }

    @GetMapping("/shipments/{shipmentCode}")
    public ShipmentView getShipment(@PathVariable String shipmentCode, Authentication authentication) {
        access.requireShipmentAccess(shipmentCode, authentication);
        Shipment shipment = shipmentRepository.findByCode(shipmentCode)
                .orElseThrow(() -> new IllegalArgumentException("Envío no encontrado: " + shipmentCode));
        return ShipmentView.from(shipment);
    }

    @GetMapping("/shipments/analytics")
    public ShipmentAnalyticsService.Metrics shipmentSummary() {
        return shipmentAnalytics.summarize();
    }

    @GetMapping("/warehouses/{warehouseCode}/capacity")
    public Map<String, Integer> warehouseCapacity(@PathVariable String warehouseCode) {
        return Map.of("occupancy", warehouseCapacity.getOccupancy(warehouseCode),
                "remaining", warehouseCapacity.getRemainingCapacity(warehouseCode));
    }

    @PostMapping("/invoices")
    public InvoiceView generateInvoice(@RequestBody GenerateInvoiceCommand command, Authentication authentication) {
        access.requireOrderAccess(command.orderCode(), authentication);
        return InvoiceView.from(invoices.generateInvoice(command));
    }

    @GetMapping("/invoices/{invoiceNumber}")
    public InvoiceView getInvoice(@PathVariable String invoiceNumber, Authentication authentication) {
        access.requireInvoiceAccess(invoiceNumber, authentication);
        return InvoiceView.from(invoiceRepository.findByNumber(invoiceNumber)
                .orElseThrow(() -> new IllegalArgumentException("Factura no encontrada: " + invoiceNumber)));
    }

    @GetMapping("/invoices/{invoiceNumber}/export")
    public ResponseEntity<byte[]> exportInvoice(@PathVariable String invoiceNumber,
                                                Authentication authentication) {
        access.requireInvoiceAccess(invoiceNumber, authentication);
        String dataUri = invoices.exportInvoice(invoiceNumber);
        byte[] pdf = Base64.getDecoder().decode(dataUri.substring(dataUri.indexOf(',') + 1));
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + invoiceNumber + ".pdf\"")
                .body(pdf);
    }

    @GetMapping("/invoices/{invoiceNumber}/taxes")
    public Map<String, Double> calculateTaxes(@PathVariable String invoiceNumber,
                                              Authentication authentication) {
        access.requireInvoiceAccess(invoiceNumber, authentication);
        return Map.of("tax", invoices.calculateTaxes(invoiceNumber));
    }

    @GetMapping("/sellers/{sellerCode}/analytics")
    public Map<String, Object> sellerAnalytics(@PathVariable String sellerCode,
                                               Authentication authentication) {
        access.requireSellerAccess(sellerCode, authentication);
        return Map.of("grossSales", sellerAnalytics.calculateGrossSales(sellerCode),
                "salesByProduct", sellerAnalytics.calculateSalesByProduct(sellerCode));
    }

    @GetMapping("/reports/{type}")
    public ResponseEntity<byte[]> downloadReport(@PathVariable String type) {
        byte[] content = switch (type.toLowerCase(java.util.Locale.ROOT)) {
            case "sales" -> reports.generateSalesReport();
            case "inventory" -> reports.generateInventoryReport();
            case "shipments" -> reports.generateShipmentReport();
            default -> throw new IllegalArgumentException("Tipo de reporte no soportado: " + type);
        };
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"nexus-" + type + ".csv\"")
                .body(content);
    }

    public record CartItemRequest(String productCode, int quantity) {
    }

    public record UpdateProductRequest(String name, String description, double price) {
    }

    public record CreateWarehouseRequest(String warehouseCode, String name, String address, int capacity) {
    }
}