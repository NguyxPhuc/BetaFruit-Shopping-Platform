package com.fu.SWP391_BetaFruit.service.impl;

import com.fu.SWP391_BetaFruit.dto.request.ImportNoteDetailRequest;
import com.fu.SWP391_BetaFruit.dto.request.ImportNoteRequest;
import com.fu.SWP391_BetaFruit.dto.response.ImportNoteDetailResponse;
import com.fu.SWP391_BetaFruit.dto.response.ImportNoteItemResponse;
import com.fu.SWP391_BetaFruit.dto.response.ImportNoteResponse;
import com.fu.SWP391_BetaFruit.entity.*;
import com.fu.SWP391_BetaFruit.enums.InventoryTransactionType;
import com.fu.SWP391_BetaFruit.enums.ProductApprovalStatus;
import com.fu.SWP391_BetaFruit.repository.*;
import com.fu.SWP391_BetaFruit.service.ImportNoteService;
import com.fu.SWP391_BetaFruit.utils.ImportNoteUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class ImportNoteServiceImpl implements ImportNoteService {
    @Autowired
    private ImportNoteRepository importNoteRepository;

    @Autowired
    private ImportNoteDetailRepository importNoteDetailRepository;

    @Autowired
    private ShopRepository shopRepository;

    @Autowired
    private ProductVariantRepository productVariantRepository;

    @Autowired
    private InventoryTransactionRepository inventoryTransactionRepository;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createImportNote(Integer ownerId, ImportNoteRequest importNoteRequest) throws Exception {
        Shop shop = shopRepository.findByOwner_UserId(ownerId)
                .orElseThrow(() -> new Exception("Lỗi hệ thống: không tìm thấy thông tin shop."));

        if (importNoteRequest.getDetails() == null || importNoteRequest.getDetails().isEmpty()) {
            throw new Exception("Phiếu nhập phải có ít nhất 1 mặt hàng.");
        }

        // Kiểm tra trùng lặp phân loại trong cùng một phiếu nhập
        Set<Integer> checkedVariantIds = new HashSet<>();
        for (ImportNoteDetailRequest item : importNoteRequest.getDetails()) {
            if (item.getVariantId() == null) {
                throw new Exception("Vui lòng chọn phân loại sản phẩm cho tất cả các dòng.");
            }
            if (!checkedVariantIds.add(item.getVariantId())) {
                throw new Exception("Phân loại sản phẩm không được trùng lặp trong cùng một phiếu nhập.");
            }
        }

        ImportNote importNote = new ImportNote();
        importNote.setShop(shop);
        importNote.setNote(importNoteRequest.getNote());
        importNote.setTotalCost(BigDecimal.ZERO);
        importNote = importNoteRepository.save(importNote);
        BigDecimal grandTotal = BigDecimal.ZERO; // biến lưu tổng số tiền của phiếu nhập kho lần này
        List<ImportNoteDetail> detailsToSave = new ArrayList<>();
        List<InventoryTransaction> transactionsToSave = new ArrayList<>();

        for (ImportNoteDetailRequest item : importNoteRequest.getDetails()) {
            // Sử dụng findForStockUpdate với PESSIMISTIC_WRITE để chống race-condition
            ProductVariant productVariant = productVariantRepository.findForStockUpdate(item.getVariantId())
                    .orElseThrow(() -> new Exception("Không tìm thấy phân loại sản phẩm."));

            if (!productVariant.getProduct().getShop().getShopId().equals(shop.getShopId())) {
                throw new Exception("Sản phẩm không thuộc quyền quản lý của cửa hàng bạn.");
            }
            if (productVariant.getProduct().getApprovalStatus() != ProductApprovalStatus.APPROVED) {
                throw new Exception("Sản phẩm '" + productVariant.getProduct().getProductName() + "' chưa được phê duyệt hoặc đã bị từ chối.");
            }

            int oldStock = productVariant.getStockQuantity() != null ? productVariant.getStockQuantity() : 0;
            int importQty = item.getQuantity();
            int newStock = oldStock + importQty;
            BigDecimal lineTotal = item.getImportPrice().multiply(BigDecimal.valueOf(importQty));
            grandTotal = grandTotal.add(lineTotal);
            // Tính giá vốn bình quân di động (MAC):
            // Nếu tồn kho cũ <= 0 hoặc giá vốn cũ null thì giá vốn mới chính bằng giá nhập mới.
            if (newStock > 0) {
                BigDecimal oldCost = productVariant.getCostPrice() != null ? productVariant.getCostPrice() : BigDecimal.ZERO;
                BigDecimal newMacCost;
                if (oldStock <= 0) {
                    newMacCost = item.getImportPrice();
                } else {
                    BigDecimal oldTotalValue = oldCost.multiply(BigDecimal.valueOf(oldStock));
                    newMacCost = (oldTotalValue.add(lineTotal)).divide(BigDecimal.valueOf(newStock), 2, RoundingMode.HALF_UP);
                }
                productVariant.setCostPrice(newMacCost);
            }
            productVariant.setStockQuantity(newStock);
            productVariantRepository.save(productVariant);

            ImportNoteDetail detail = new ImportNoteDetail();
            detail.setImportNote(importNote);
            detail.setVariant(productVariant);
            detail.setQuantity(importQty);
            detail.setImportPrice(item.getImportPrice());
            detailsToSave.add(detail);

            InventoryTransaction trans = new InventoryTransaction();
            trans.setVariant(productVariant);
            trans.setUser(shop.getOwner());
            trans.setTransactionType(InventoryTransactionType.RESTOCK);
            trans.setQuantityChanged(importQty);
            trans.setPreviousStock(oldStock);
            trans.setNewStock(newStock);
            transactionsToSave.add(trans);
        }
        importNote.setTotalCost(grandTotal);
        importNoteRepository.save(importNote);
        importNoteDetailRepository.saveAll(detailsToSave);
        inventoryTransactionRepository.saveAll(transactionsToSave);
    }

    private ImportNoteResponse toImportNoteResponse(ImportNote importNote) {
        ImportNoteResponse importNoteResponse = new ImportNoteResponse();
        importNoteResponse.setImportId(importNote.getImportId());
        importNoteResponse.setNote(importNote.getNote());
        importNoteResponse.setTotalCost(importNote.getTotalCost());
        importNoteResponse.setImportDate(importNote.getCreatedAt());
        String importCode = ImportNoteUtils.generateImportCode(importNote.getImportId());
        importNoteResponse.setImportCode(importCode);
        return importNoteResponse;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ImportNoteResponse> getAllImportNote(Integer ownerId) throws Exception {
        Shop shop = shopRepository.findByOwner_UserId(ownerId)
                .orElseThrow(() -> new Exception("Lỗi hệ thống: không tìm thấy thông tin shop."));
        List<ImportNote> importNoteList = importNoteRepository.findAllByShopOrderByCreatedAtDesc(shop);
        List<ImportNoteResponse> importNoteResponseList = new ArrayList<>();
        for (ImportNote importNote : importNoteList) {
            importNoteResponseList.add(toImportNoteResponse(importNote));
        }
        return importNoteResponseList;
    }

    private ImportNoteItemResponse toImportNoteItemResponse(ImportNoteDetail importNoteDetail) {
        ImportNoteItemResponse importNoteItemResponse = new ImportNoteItemResponse();
        importNoteItemResponse.setDetailId(importNoteDetail.getDetailId());
        importNoteItemResponse.setProductName(importNoteDetail.getVariant().getProduct().getProductName());
        importNoteItemResponse.setVariantName(importNoteDetail.getVariant().getVariantName());
        importNoteItemResponse.setImportPrice(importNoteDetail.getImportPrice());
        importNoteItemResponse.setQuantity(importNoteDetail.getQuantity());
        importNoteItemResponse.setVariantId(importNoteDetail.getVariant().getVariantId());
        importNoteItemResponse.setLineTotal(importNoteDetail.getImportPrice().multiply(BigDecimal.valueOf(importNoteDetail.getQuantity())));

        return importNoteItemResponse;
    }

    @Override
    @Transactional(readOnly = true)
    public ImportNoteDetailResponse getImportNoteDetail(Integer ownerId, Integer importId) throws Exception {
        Shop shop = shopRepository.findByOwner_UserId(ownerId)
                .orElseThrow(() -> new Exception("Lỗi hệ thống: không tìm thấy thông tin shop."));
        ImportNote importNote = importNoteRepository.findById(importId)
                .orElseThrow(() -> new Exception("Không tìm thấy phiếu nhập kho này."));
        if (!importNote.getShop().getShopId().equals(shop.getShopId())) {
            throw new Exception("Lỗi bảo mật: Bạn không có quyền xem phiếu nhập kho của cửa hàng khác.");
        }
        ImportNoteDetailResponse response = new ImportNoteDetailResponse();
        response.setImportId(importNote.getImportId());
        response.setNote(importNote.getNote());
        response.setTotalCost(importNote.getTotalCost());
        response.setImportDate(importNote.getCreatedAt());

        // lấy danh sách tất cả các variant đã nhập trong phiếu nhập kho
        List<ImportNoteDetail> details = importNoteDetailRepository.findByImportNote_ImportId(importId);
        List<ImportNoteItemResponse> itemResponseList = new ArrayList<>();
        for (ImportNoteDetail importNoteDetail : details) {
            itemResponseList.add(toImportNoteItemResponse(importNoteDetail));
        }
        response.setItems(itemResponseList);
        return response;
    }
}
