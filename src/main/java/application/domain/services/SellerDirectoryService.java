package application.domain.services;

import application.domain.models.Seller;

import java.util.List;

public interface SellerDirectoryService {
    Seller findByCode(String sellerCode);
    List<Seller> findAll();
    List<Seller> search(String query);
}