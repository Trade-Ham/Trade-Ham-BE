package com.trade_ham.domain.product.service;

import com.trade_ham.domain.auth.entity.UserEntity;
import com.trade_ham.domain.auth.repository.UserRepository;
import com.trade_ham.domain.locker.dto.NotificationBuyerDTO;
import com.trade_ham.domain.locker.dto.NotificationLockerDTO;
import com.trade_ham.domain.locker.entity.LockerEntity;
import com.trade_ham.domain.locker.entity.NotificationEntity;
import com.trade_ham.domain.locker.repository.LockerRepository;
import com.trade_ham.domain.locker.repository.NotificationRepository;
import com.trade_ham.domain.product.entity.ProductEntity;
import com.trade_ham.domain.product.entity.ProductStatus;
import com.trade_ham.domain.product.entity.TradeEntity;
import com.trade_ham.domain.product.repository.ProductRepository;
import com.trade_ham.domain.product.repository.TradeRepository;
import com.trade_ham.global.common.exception.AccessDeniedException;
import com.trade_ham.global.common.exception.ErrorCode;
import com.trade_ham.global.common.exception.InvalidProductStateException;
import com.trade_ham.global.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Random;

@Service
@RequiredArgsConstructor
public class PurchaseProductService {
    private final ProductRepository productRepository;
    private final LockerRepository lockerRepository;
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final TradeRepository tradeRepository;

    /*
    사용자가 구매 요청 버튼 클릭
    -> 해당 물품에 락 걸기
    -> 물품 상태 변경
     */
    @Transactional
    public ProductEntity purchaseProduct(Long productId, Long buyerId) {
        // 동시성을 고려해 비관적 락을 사용
        ProductEntity productEntity = productRepository.findByIdWithPessimisticLock(productId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.PRODUCT_NOT_FOUND));

        if (!productEntity.getStatus().equals(ProductStatus.SELL)) {
            throw new AccessDeniedException(ErrorCode.ACCESS_DENIED);
        }

        // ProductEntity 상태를 'CHECK'로 변경
        productEntity.setStatus(ProductStatus.CHECK);
        productRepository.save(productEntity);

        // 구매자에게 판매자 실명과 계좌번호 전송 (알림)
        UserEntity seller = productEntity.getSeller();
        UserEntity buyer = userRepository.findById(buyerId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND));

        NotificationBuyerDTO notificationBuyerDTO = new NotificationBuyerDTO("판매자의 실명 및 계좌번호", false, buyer);
        NotificationEntity notificationBuyerEntity = new NotificationEntity(notificationBuyerDTO);
        notificationRepository.save(notificationBuyerEntity);

        return productEntity;
    }

    /*
     구매 완료 버튼 클릭
     물품 상태 변경
     물품에 사물함을 할당하고 사물함 상태 변경
     판매자에게 알림을 보내주는 서비스 구현
     거래 내역 생성
     */
    @Transactional
    public TradeEntity completePurchase(Long productId, Long buyerId) {
        ProductEntity productEntity = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.PRODUCT_NOT_FOUND));

        if (!productEntity.getStatus().equals(ProductStatus.CHECK)) {
            throw new InvalidProductStateException(ErrorCode.INVALID_PRODUCT_STATE);
        }

        // 상태를 WAIT으로 변경
        productEntity.setStatus(ProductStatus.WAIT);

        // 사용 가능한 사물함 할당
        LockerEntity availableLockerEntity = lockerRepository.findFirstByLockerStatusTrue()
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.LOCKER_NOT_AVAILABLE));

        availableLockerEntity.setLockerStatus(false);
        lockerRepository.save(availableLockerEntity);

        productEntity.setLockerEntity(availableLockerEntity);
        productRepository.save(productEntity);

        //판매자에게 사물함 ID와 비밀번호 전달 (알림)
        UserEntity seller = productEntity.getSeller();

        NotificationLockerDTO notificationLockerDTO = new NotificationLockerDTO("사물함 번호 및 비밀번호", availableLockerEntity.getId(), generateLockerPassword(), true, seller);
        NotificationEntity notificationSellerEntity = new NotificationEntity(notificationLockerDTO);
        notificationRepository.save(notificationSellerEntity);


        // 거래 내역 생성
        UserEntity buyer = userRepository.findById(buyerId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND));

        TradeEntity tradeEntity = TradeEntity.builder()
                .product(productEntity)
                .buyer(buyer)
                .seller(productEntity.getSeller())
                .lockerEntity(availableLockerEntity)
                .build();

        buyer.addPurchasedProduct(productEntity);

        return tradeRepository.save(tradeEntity);
    }


    public ProductEntity findProductById(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.PRODUCT_NOT_FOUND));
    }

    // 비밀번호 생성
    public String generateLockerPassword() {
        Random random = new Random();
        int password = 1000 + random.nextInt(9000);
        return String.valueOf(password);
    }

    /*
     판매자는 물건을 넣고 확인 버튼 누름
     구매자에게 사물함에 물건이 보관되었다고 알림
     */
    @Transactional
    public void productInLocker(Long productId) {
        TradeEntity tradeEntity = tradeRepository.findByProduct_Id(productId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.PRODUCT_NOT_FOUND));

        LockerEntity lockerEntity = tradeEntity.getLockerEntity();
        UserEntity userEntity = tradeEntity.getBuyer();

        NotificationLockerDTO notificationLockerDTO = new NotificationLockerDTO("물건이 사물함에 보관되었습니다.", lockerEntity.getId(), lockerEntity.getLockerPassword(), true, userEntity);
        NotificationEntity notificationEntity = new NotificationEntity(notificationLockerDTO);
        notificationRepository.save(notificationEntity);
    }

    /*
    구매자 수령 완료
    물품 상태 변경
    사물함 상태 변경
    */
    @Transactional
    public void completePurchaseByBuyer(Long productId) {
        ProductEntity productEntity = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.PRODUCT_NOT_FOUND));

        productEntity.setStatus(ProductStatus.DONE);
        productRepository.save(productEntity);

        TradeEntity tradeEntity = tradeRepository.findByProduct_Id(productId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.PRODUCT_NOT_FOUND));
        LockerEntity lockerEntity = tradeEntity.getLockerEntity();
        lockerEntity.setLockerStatus(true);
        lockerRepository.save(lockerEntity);
    }
}