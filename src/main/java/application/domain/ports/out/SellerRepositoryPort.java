package application.domain.ports.out;

import application.domain.models.Seller;
import java.util.List;
import java.util.Optional;

/** Puerto de salida para persistir vendedores. */
public interface SellerRepositoryPort {
    Seller save(Seller seller);
    Optional<Seller> findBySellerCode(String sellerCode);
    List<Seller> findAll();
    void delete(String sellerCode);
}