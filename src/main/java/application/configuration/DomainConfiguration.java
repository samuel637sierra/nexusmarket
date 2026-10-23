package application.configuration;

import application.adapters.memory.InMemoryInfrastructureAdapters;
import application.adapters.persistence.ObjectStore;
import application.adapters.persistence.PersistentRepositoryAdapters;
import application.domain.models.InternalAdministrator;
import application.domain.models.User;
import application.domain.ports.out.CustomerRepositoryPort;
import application.domain.ports.out.InventoryRepositoryPort;
import application.domain.ports.out.InvoiceGeneratorPort;
import application.domain.ports.out.InvoiceRepositoryPort;
import application.domain.ports.out.NotificationPort;
import application.domain.ports.out.OrderRepositoryPort;
import application.domain.ports.out.PasswordServicePort;
import application.domain.ports.out.PaymentGatewayPort;
import application.domain.ports.out.ProductRepositoryPort;
import application.domain.ports.out.ReportGeneratorPort;
import application.domain.ports.out.SellerRepositoryPort;
import application.domain.ports.out.ShipmentRepositoryPort;
import application.domain.ports.out.ShipmentTrackingPort;
import application.domain.ports.out.UserRepositoryPort;
import application.domain.ports.out.WarehouseRepositoryPort;
import org.springframework.boot.ApplicationRunner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.time.LocalDate;
import java.util.UUID;

@Configuration(proxyBeanMethods = false)
public class DomainConfiguration {
    @Bean
    ApplicationRunner initialAdministrator(UserRepositoryPort users, PasswordServicePort passwords,
                                           @Value("${nexusmarket.bootstrap.admin.username:}") String username,
                                           @Value("${nexusmarket.bootstrap.admin.password:}") String password) {
        if (username.isBlank() && password.isBlank()) {
            return arguments -> { };
        }
        if (username.isBlank() || password.length() < 12) {
            throw new IllegalStateException("Configure ambos valores de bootstrap y una contraseña de al menos 12 caracteres");
        }
        return arguments -> {
            var existing = users.findByUsername(username.trim());
            if (existing.isPresent()) {
                if (existing.get().getRole() != application.domain.valueObjects.SystemRole.ADMINISTRATOR) {
                    throw new IllegalStateException("El usuario de bootstrap ya existe con otro rol");
                }
                return;
            }
            long userId = UUID.randomUUID().getMostSignificantBits() & Long.MAX_VALUE;
            if (userId == 0) {
                userId = 1;
            }
            User administrator = new InternalAdministrator(userId, username.trim(),
                    passwords.hash(password), "ADM-BOOTSTRAP", LocalDate.now(),
                    "PER-" + UUID.randomUUID(), "Administrador NexusMarket",
                    username.trim() + "@localhost", "0000000000", "NexusMarket", LocalDate.now());
            users.save(administrator);
        };
    }

    @Bean
    CustomerRepositoryPort customerRepository(ObjectStore store) {
        return new PersistentRepositoryAdapters.CustomerRepositoryAdapter(store);
    }

    @Bean
    UserRepositoryPort userRepository(ObjectStore store) {
        return new PersistentRepositoryAdapters.UserRepositoryAdapter(store);
    }

    @Bean
    SellerRepositoryPort sellerRepository(ObjectStore store) {
        return new PersistentRepositoryAdapters.SellerRepositoryAdapter(store);
    }

    @Bean
    ProductRepositoryPort productRepository(ObjectStore store) {
        return new PersistentRepositoryAdapters.ProductRepositoryAdapter(store);
    }

    @Bean
    OrderRepositoryPort orderRepository(ObjectStore store) {
        return new PersistentRepositoryAdapters.OrderRepositoryAdapter(store);
    }

    @Bean
    WarehouseRepositoryPort warehouseRepository(ObjectStore store) {
        return new PersistentRepositoryAdapters.WarehouseRepositoryAdapter(store);
    }

    @Bean
    InventoryRepositoryPort inventoryRepository(ObjectStore store) {
        return new PersistentRepositoryAdapters.InventoryRepositoryAdapter(store);
    }

    @Bean
    InvoiceRepositoryPort invoiceRepository(ObjectStore store) {
        return new PersistentRepositoryAdapters.InvoiceRepositoryAdapter(store);
    }

    @Bean
    ShipmentRepositoryPort shipmentRepository(ObjectStore store) {
        return new PersistentRepositoryAdapters.ShipmentRepositoryAdapter(store);
    }

    @Bean
    PasswordServicePort passwordService() {
        return new InMemoryInfrastructureAdapters.PasswordServiceAdapter();
    }

    @Bean
    @Profile({"dev", "test"})
    NotificationPort notificationPort() {
        return new InMemoryInfrastructureAdapters.NotificationAdapter();
    }

    @Bean
    @Profile({"dev", "test"})
    PaymentGatewayPort sandboxPaymentGateway() {
        return new InMemoryInfrastructureAdapters.SandboxPaymentGatewayAdapter();
    }

    @Bean
    @Profile({"dev", "test"})
    ShipmentTrackingPort shipmentTrackingPort() {
        return new InMemoryInfrastructureAdapters.ShipmentTrackingAdapter();
    }

    @Bean
    @Profile({"dev", "test"})
    InvoiceGeneratorPort invoiceGeneratorPort() {
        return new InMemoryInfrastructureAdapters.InvoiceGeneratorAdapter();
    }

    @Bean
    @Profile({"dev", "test"})
    ReportGeneratorPort reportGeneratorPort(OrderRepositoryPort orders,
                                            InventoryRepositoryPort inventories,
                                            ShipmentRepositoryPort shipments) {
        return new InMemoryInfrastructureAdapters.ReportGeneratorAdapter(orders, inventories, shipments);
    }

}