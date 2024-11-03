package com.trade_ham.domain.product.service;

import com.trade_ham.domain.auth.entity.UserEntity;
import com.trade_ham.domain.auth.repository.UserRepository;
import com.trade_ham.domain.product.domain.Product;
import com.trade_ham.domain.product.domain.ProductStatus;
import com.trade_ham.domain.product.dto.ProductDTO;
import com.trade_ham.domain.product.dto.ProductResponseDTO;
import com.trade_ham.domain.product.repository.ProductRepository;
import com.trade_ham.global.common.exception.ErrorCode;
import com.trade_ham.global.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SellProductService {
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    // 물품 올리기
    public ProductResponseDTO createProduct(ProductDTO productDTO, Long sellerId) {
        UserEntity seller = userRepository.findById(sellerId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND));


        Product product = Product.builder()
                .seller(seller)
                .name(productDTO.getName())
                .description(productDTO.getDescription())
                .status(ProductStatus.SELL)
                .price(productDTO.getPrice())
                .build();

        seller.addSellingProduct(product);

        Product savedProduct = productRepository.save(product);

        return new ProductResponseDTO(savedProduct);
    }

    // 물품 수정
    public ProductResponseDTO updateProduct(Long productId, ProductDTO productDTO) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.PRODUCT_NOT_FOUND));

        product.updateProduct(productDTO.getName(), productDTO.getDescription(), productDTO.getPrice());

        Product updatedProduct = productRepository.save(product);

        return new ProductResponseDTO(updatedProduct);
    }

    // 물품 삭제
    public void deleteProduct(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.PRODUCT_NOT_FOUND));

        // 판매자 판매 내역에서 해당 상품 제거
        UserEntity seller = product.getSeller();
        if (seller != null) {
            seller.deleteSellingProduct(product);
        }

        productRepository.delete(product);
    }

    // 물품 검색 (이름을 기반으로 검색)
    public List<ProductResponseDTO> searchProducts(String keyword) {
        List<Product> products = productRepository.findByNameContainingIgnoreCase(keyword);

        return products.stream()
                .map(ProductResponseDTO::new)
                .collect(Collectors.toList());
    }

    // 판매자 판매 내역 관리
    public List<ProductResponseDTO> findProductsBySeller(Long sellerId) {
        UserEntity seller = userRepository.findById(sellerId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND));
        List<Product> products = productRepository.findBySeller(seller);

        return products.stream()
                .map(ProductResponseDTO::new)
                .collect(Collectors.toList());
    }

    // 상태가 SELL인 전체 판매 물품 최신순 조회
    public List<ProductResponseDTO> findAllSellProducts() {
        List<Product> products = productRepository.findByStatusOrderByCreatedAtDesc(ProductStatus.SELL);

        return products.stream()
                .map(ProductResponseDTO::new)
                .collect(Collectors.toList());
    }
}