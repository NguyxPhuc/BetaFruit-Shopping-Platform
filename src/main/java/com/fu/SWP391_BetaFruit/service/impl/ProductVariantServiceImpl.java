package com.fu.SWP391_BetaFruit.service.impl;

import com.fu.SWP391_BetaFruit.dto.response.VariantResponse;
import com.fu.SWP391_BetaFruit.entity.ProductVariant;
import com.fu.SWP391_BetaFruit.entity.Shop;
import com.fu.SWP391_BetaFruit.repository.ProductVariantRepository;
import com.fu.SWP391_BetaFruit.repository.ShopRepository;
import com.fu.SWP391_BetaFruit.service.ProductVariantService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class ProductVariantServiceImpl implements ProductVariantService {
    @Autowired
    private ProductVariantRepository productVariantRepository;

    @Autowired
    private ShopRepository shopRepository;

    private VariantResponse toVariantResponse(ProductVariant productVariant) {
        VariantResponse variantResponse = new VariantResponse();
        variantResponse.setVariantId(productVariant.getVariantId());
        variantResponse.setVariantName(productVariant.getVariantName());
        variantResponse.setProductName(productVariant.getProduct().getProductName());
        return variantResponse;
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<VariantResponse> getAllVariantsByShopId(Integer userId) throws Exception {
        Shop shop = shopRepository.findByOwner_UserId(userId)
                .orElseThrow(() -> new Exception("Không tìm thấy shop."));
        List<ProductVariant> variantList = productVariantRepository.findAllActiveVariantsByShopOwner(userId);
        List<VariantResponse> variantResponseList = new ArrayList<>();
        for (ProductVariant productVariant : variantList) {
            variantResponseList.add(toVariantResponse(productVariant));
        }
        return variantResponseList;
    }


}
