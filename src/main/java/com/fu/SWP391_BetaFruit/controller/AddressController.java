package com.fu.SWP391_BetaFruit.controller;

import com.fu.SWP391_BetaFruit.dto.request.AddressRequest;
import com.fu.SWP391_BetaFruit.dto.response.AddressResponse;
import com.fu.SWP391_BetaFruit.enums.AddressType;
import com.fu.SWP391_BetaFruit.service.AddressService;
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
@RequestMapping("/{role}/address")
public class AddressController {
    @Autowired
    private AddressService addressService;

    @GetMapping("/create")
    public String showCreateForm(@PathVariable(name = "role") String role, Model model) {
        if (!role.equals("customer") && !role.equals("shop")) {
            return "redirect:/403";
        }
        if (!model.containsAttribute("addressRequest")) {
            model.addAttribute("addressRequest", new AddressRequest());
        }
        model.addAttribute("role", role);
        return role + "/address-form";
    }

    @PostMapping("/create")
    public String createAddress(@PathVariable(name = "role") String role,
            @Valid @ModelAttribute("addressRequest") AddressRequest request, BindingResult bindingResult,
            HttpSession session, RedirectAttributes redirectAttributes, Model model) {
        if (!role.equals("customer") && !role.equals("shop")) {
            return "redirect:/403";
        }
        if (role.equals("shop") && (request.getAddressType() == null || request.getAddressType().trim().isEmpty())) {
            bindingResult.rejectValue("addressType", "error.addressType",
                    "Vui lòng chọn loại địa chỉ (Lấy hàng / Trả hàng)");
        }
        if (bindingResult.hasErrors()) {
            return role + "/address-form";
        }
        try {
            Integer userId = (Integer) session.getAttribute("LOGGED_IN_USER_ID");
            AddressType forceType = role.equals("customer") ? AddressType.SHIPPING : null;
            addressService.createAddress(userId, request, forceType);
            redirectAttributes.addFlashAttribute("successMessage", "Thêm địa chỉ thành công!");
            return "redirect:/" + role + "/address";
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Lỗi: " + e.getMessage());
            return role + "/address-form";
        }
    }

    @GetMapping
    public String showAddresses(@PathVariable(name = "role") String role, HttpSession session, Model model) {
        Integer userId = (Integer) session.getAttribute("LOGGED_IN_USER_ID");
        if (!role.equals("customer") && !role.equals("shop")) {
            return "redirect:/403";
        }
        model.addAttribute("role", role);
        try {
            List<AddressResponse> responseList = addressService.getAllAddressesByRole(userId, role);
            model.addAttribute("addresses", responseList);
            return role + "/address-list";
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Lỗi: " + e.getMessage());
            return role + "/address-list";
        }
    }

    @GetMapping("/{id}")
    public String getAddressById(@PathVariable(name = "id") Integer id, Model model,
            @PathVariable(name = "role") String role, HttpSession session) {
        if (!role.equals("customer") && !role.equals("shop")) {
            return "redirect:/403";
        }
        try {
            Integer userId = (Integer) session.getAttribute("LOGGED_IN_USER_ID");
            AddressResponse addressResponse = addressService.getAddressById(userId, id);
            model.addAttribute("addressResponse", addressResponse);
            return role + "-address-detail";
        } catch (Exception e) {
            return "redirect:/" + role + "/address";
        }
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable(name = "id") Integer id, Model model,
            @PathVariable(name = "role") String role, HttpSession session, RedirectAttributes redirectAttributes) {
        if (!role.equals("customer") && !role.equals("shop")) {
            return "redirect:/403";
        }
        try {
            Integer userId = (Integer) session.getAttribute("LOGGED_IN_USER_ID");
            AddressResponse existingAddress = addressService.getAddressById(userId, id);
            if (!model.containsAttribute("addressRequest")) {
                AddressRequest requestForm = new AddressRequest();
                requestForm.setReceiverName(existingAddress.getReceiverName());
                requestForm.setReceiverPhone(existingAddress.getReceiverPhone());
                requestForm.setAddressLine(existingAddress.getAddressLine());
                requestForm.setCity(existingAddress.getCity());
                requestForm.setDistrict(existingAddress.getDistrict());
                requestForm.setWard(existingAddress.getWard());
                requestForm.setIsDefault(existingAddress.getIsDefault());
                requestForm.setAddressType(existingAddress.getAddressType());
                model.addAttribute("addressRequest", requestForm);
            }
            model.addAttribute("isEditMode", true);
            model.addAttribute("editAddressId", id);
            model.addAttribute("role", role);
            return role + "/address-form";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/" + role + "/address";
        }
    }

    @PostMapping("/edit/{id}")
    public String editAddress(@PathVariable(name = "id") Integer id, Model model,
            @PathVariable(name = "role") String role,
            @Valid @ModelAttribute("addressRequest") AddressRequest request,
            BindingResult bindingResult,
            HttpSession session, RedirectAttributes redirectAttributes) {
        if (!role.equals("customer") && !role.equals("shop")) {
            return "redirect:/403";
        }
        if (role.equals("shop") && (request.getAddressType() == null || request.getAddressType().trim().isEmpty())) {
            bindingResult.rejectValue("addressType", "error.addressType", "Vui lòng chọn loại địa chỉ");
        }
        if (bindingResult.hasErrors()) {
            model.addAttribute("isEditMode", true);
            model.addAttribute("editAddressId", id);
            model.addAttribute("role", role);
            return role + "/address-form";
        }
        try {
            Integer userId = (Integer) session.getAttribute("LOGGED_IN_USER_ID");
            AddressType forceType = role.equals("customer") ? AddressType.SHIPPING : null;
            addressService.updateAddress(userId, id, request, forceType);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật địa chỉ thành công.");
            return "redirect:/" + role + "/address";
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Lỗi: " + e.getMessage());
            model.addAttribute("isEditMode", true);
            model.addAttribute("editAddressId", id);
            model.addAttribute("role", role);
            return role + "/address-form";
        }
    }

    @PostMapping("/delete/{id}")
    public String deleteAddress(@PathVariable(name = "id") Integer id, @PathVariable(name = "role") String role,
            HttpSession session, RedirectAttributes redirectAttributes) {
        if (!role.equals("customer") && !role.equals("shop")) {
            return "redirect:/403";
        }
        try {
            Integer userId = (Integer) session.getAttribute("LOGGED_IN_USER_ID");
            addressService.deleteAddress(userId, id);
            redirectAttributes.addFlashAttribute("successMessage", "Xóa địa chỉ thành công.");
            return "redirect:/" + role + "/address";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/" + role + "/address";
        }
    }
}
