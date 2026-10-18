package com.fu.SWP391_BetaFruit.controller;

import com.fu.SWP391_BetaFruit.dto.request.ImportNoteDetailRequest;
import com.fu.SWP391_BetaFruit.dto.request.ImportNoteRequest;
import com.fu.SWP391_BetaFruit.dto.response.ImportNoteDetailResponse;
import com.fu.SWP391_BetaFruit.dto.response.ImportNoteResponse;
import com.fu.SWP391_BetaFruit.dto.response.VariantResponse;
import com.fu.SWP391_BetaFruit.service.ImportNoteService;
import com.fu.SWP391_BetaFruit.service.ProductVariantService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/shop/inventory/import-note")
public class ImportNoteController {
    @Autowired
    private ImportNoteService importNoteService;

    @Autowired
    private ProductVariantService variantService;

    @GetMapping
    public String showListImportNote(Model model, HttpSession session) {
        Integer id = (Integer) session.getAttribute("LOGGED_IN_USER_ID");
        try{
            List<ImportNoteResponse> responseList = importNoteService.getAllImportNote(id);
            model.addAttribute("responseList", responseList);
            return "shop/inventory/import-note-list";
        }catch (Exception e){
            model.addAttribute("error", e.getMessage());
            return "shop/inventory/import-note-list";
        }
    }

    @GetMapping("/{id}")
    public String showImportNoteDetail(@PathVariable("id") Integer importId, Model model, HttpSession session, RedirectAttributes redirectAttributes) {
        Integer ownerId = (Integer) session.getAttribute("LOGGED_IN_USER_ID");
        try {
            ImportNoteDetailResponse detailResponse = importNoteService.getImportNoteDetail(ownerId, importId);
            model.addAttribute("importDetail", detailResponse);
            return "shop/inventory/import-note-detail";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/shop/inventory/import-note";
        }
    }

    @GetMapping("/new")
    public String showCreateImportNote(Model model, HttpSession session, RedirectAttributes redirectAttributes) {
        Integer id = (Integer) session.getAttribute("LOGGED_IN_USER_ID");
        try{
            if (!model.containsAttribute("importNoteRequest")) {
                ImportNoteRequest request = new ImportNoteRequest();
                request.getDetails().add(new ImportNoteDetailRequest());
                model.addAttribute("importNoteRequest", request);
            }
            List<VariantResponse> variantList = variantService.getAllVariantsByShopId(id);
            model.addAttribute("variantList", variantList);
            return "shop/inventory/import-note-form";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/shop/inventory/import-note";
        }
    }

    private void reloadVariantListForForm(Model model, Integer ownerId) {
        try {
            List<VariantResponse> variantList = variantService.getAllVariantsByShopId(ownerId);
            model.addAttribute("variantList", variantList);
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Lỗi tải danh sách sản phẩm: " + e.getMessage());
        }
    }

    @PostMapping("/new")
    public String createImportNote(Model model, HttpSession session, RedirectAttributes redirectAttributes, @Valid @ModelAttribute("importNoteRequest") ImportNoteRequest request, BindingResult bindingResult) {
        Integer ownerId = (Integer) session.getAttribute("LOGGED_IN_USER_ID");
        if(bindingResult.hasErrors()){
            reloadVariantListForForm(model, ownerId);
            return "shop/inventory/import-note-form";
        }
        try {
            importNoteService.createImportNote(ownerId, request);
            redirectAttributes.addFlashAttribute("successMessage", "Tạo phiếu nhập kho thành công! Hàng đã được cộng vào kho.");
            return "redirect:/shop/inventory/import-note";
        } catch (Exception e) {
            model.addAttribute("errorMessage", e.getMessage());
            reloadVariantListForForm(model, ownerId);
            return "shop/inventory/import-note-form";
        }
    }
}
