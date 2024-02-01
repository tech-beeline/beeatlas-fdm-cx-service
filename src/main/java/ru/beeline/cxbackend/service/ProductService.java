package ru.beeline.cxbackend.service;

import org.springframework.stereotype.Service;
import ru.beeline.cxbackend.domain.Product;
import ru.beeline.cxbackend.domain.UserProfile;

import java.util.List;

@Service
public class ProductService {


    public Product findProductById(String productId) {
        return null;
    }

    public List<Product> findProductsByPermission(String bearerToken) {
        return null;
    }

    public List<Product> findProductsByUser(UserProfile user) {
        return null;
    }
}
