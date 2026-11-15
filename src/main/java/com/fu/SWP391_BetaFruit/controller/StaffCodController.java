package com.fu.SWP391_BetaFruit.controller;

import com.fu.SWP391_BetaFruit.entity.CODCollection;
import com.fu.SWP391_BetaFruit.entity.CODDetail;
import com.fu.SWP391_BetaFruit.entity.DeliveryAssignment;
import com.fu.SWP391_BetaFruit.entity.Shipper;
import com.fu.SWP391_BetaFruit.service.CODCollectionService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/staff/cod-collections")
public class StaffCodController {

    private final CODCollectionService codCollectionService;

    @GetMapping
    public String listCollections(
            @RequestParam(required = false) Integer shipperId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpSession session,
            Model model
    ) {
        Integer staffId = (Integer) session.getAttribute("LOGGED_IN_USER_ID");
        if (staffId == null) {
            return "redirect:/auth/login";
        }

        Page<CODCollection> collections = codCollectionService.getCollectionsPage(shipperId, PageRequest.of(page, size));
        List<Shipper> shippersWithDebt = codCollectionService.getShippersWithDebt();
        List<Shipper> allShippers = codCollectionService.getAllShippers();

        BigDecimal totalOutstandingDebt = shippersWithDebt.stream()
                .map(Shipper::getCodDebt)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        model.addAttribute("collections", collections);
        model.addAttribute("shippersWithDebt", shippersWithDebt);
        model.addAttribute("allShippers", allShippers);
        model.addAttribute("selectedShipperId", shipperId);
        model.addAttribute("totalOutstandingDebt", totalOutstandingDebt);
        model.addAttribute("activeMenu", "cod-collections");

        return "operations-staff/cod/list";
    }

    @GetMapping("/create")
    public String createForm(
            @RequestParam(required = false) Integer shipperId,
            HttpSession session,
            Model model
    ) {
        Integer staffId = (Integer) session.getAttribute("LOGGED_IN_USER_ID");
        if (staffId == null) {
            return "redirect:/auth/login";
        }

        List<Shipper> allShippers = codCollectionService.getAllShippers();
        model.addAttribute("allShippers", allShippers);
        model.addAttribute("selectedShipperId", shipperId);
        model.addAttribute("activeMenu", "cod-collections");

        if (shipperId != null) {
            List<DeliveryAssignment> pendingAssignments = codCollectionService.getUnremittedAssignmentsByShipper(shipperId);
            BigDecimal currentDebt = codCollectionService.getShipperDebt(shipperId);

            model.addAttribute("pendingAssignments", pendingAssignments);
            model.addAttribute("currentDebt", currentDebt);
        }

        return "operations-staff/cod/create";
    }

    @PostMapping("/create")
    public String processRemittance(
            @RequestParam Integer shipperId,
            @RequestParam List<Integer> orderIds,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        Integer staffId = (Integer) session.getAttribute("LOGGED_IN_USER_ID");
        if (staffId == null) {
            return "redirect:/auth/login";
        }

        try {
            CODCollection collection = codCollectionService.remitCodCollection(staffId, shipperId, orderIds);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Đã lập phiếu thu COD #" + collection.getCollectionId() + " thành công! Công nợ đã được cấn trừ.");
            return "redirect:/staff/cod-collections/" + collection.getCollectionId();
        } catch (Exception e) {
            log.error("Lỗi khi lập phiếu thu COD", e);
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/staff/cod-collections/create?shipperId=" + shipperId;
        }
    }

    @GetMapping("/{id}")
    public String viewDetail(
            @PathVariable("id") Integer collectionId,
            HttpSession session,
            Model model
    ) {
        Integer staffId = (Integer) session.getAttribute("LOGGED_IN_USER_ID");
        if (staffId == null) {
            return "redirect:/auth/login";
        }

        CODCollection collection = codCollectionService.getCollectionById(collectionId);
        List<CODDetail> details = codCollectionService.getCollectionDetails(collectionId);

        model.addAttribute("collection", collection);
        model.addAttribute("details", details);
        model.addAttribute("activeMenu", "cod-collections");

        return "operations-staff/cod/detail";
    }
}
