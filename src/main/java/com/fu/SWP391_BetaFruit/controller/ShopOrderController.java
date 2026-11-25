package com.fu.SWP391_BetaFruit.controller;

import com.fu.SWP391_BetaFruit.enums.OrderStatus;
import com.fu.SWP391_BetaFruit.service.ShopOrderService;
import jakarta.servlet.http.HttpSession;
import com.fu.SWP391_BetaFruit.exception.GlobalExceptionHandler.InvalidPagination;
import org.springframework.util.MultiValueMap;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.data.domain.Page;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.transaction.CannotCreateTransactionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller
@RequestMapping({"/shop/orders", "/shop/orders.html"})
public class ShopOrderController {
    private static final Logger log = LoggerFactory.getLogger(ShopOrderController.class);

    private final ShopOrderService shopOrderService;

    public ShopOrderController(ShopOrderService shopOrderService) {
        this.shopOrderService = shopOrderService;
    }

    /**
     * Tác dụng: Nhận yêu cầu danh sách cùng từ khóa, trạng thái, cách sắp xếp và phân trang; gọi
     * getOrdersForShopOwner rồi đưa dữ liệu vào template shop/orders.
     * Màn hình / luồng: Màn shop/orders và shop/order-detail; gọi nghiệp vụ qua ShopOrderService.
     */
    @GetMapping
    public String orderList(
            Model model,
            HttpSession session,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection,
            @RequestParam MultiValueMap<String, String> paginationParams) {
        Integer shopOwnerId = currentUserId(session);
        if (shopOwnerId == null) return "redirect:/auth/login";
        int page = paginationNumber(paginationParams, "page", 0, 0, Integer.MAX_VALUE);
        int size = paginationNumber(paginationParams, "size", 10, 1, 100);
        model.addAttribute("pageSize", size);
        try {
            model.addAttribute("orders", shopOrderService.getOrdersForShopOwner(shopOwnerId, keyword, status, sortBy, sortDirection, page, size));
        } catch (ResponseStatusException exception) {
            log.warn("Order data validation failed for ownerId={}", shopOwnerId, exception);
            model.addAttribute("orders", Page.empty());
            model.addAttribute("orderLoadError", "Dữ liệu đơn hàng không hợp lệ: " + exception.getReason());
        } catch (DataAccessResourceFailureException | CannotCreateTransactionException exception) {
            log.error("Cannot access order database for ownerId={}", shopOwnerId, exception);
            model.addAttribute("orders", Page.empty());
            model.addAttribute("orderLoadError", "Không thể kết nối cơ sở dữ liệu để tải đơn hàng. Kiểm tra lại SQL Server rồi tải lại trang.");
        } catch (DataAccessException exception) {
            log.error("Order query failed for ownerId={}", shopOwnerId, exception);
            model.addAttribute("orders", Page.empty());
            model.addAttribute("orderLoadError", "Không thể truy vấn dữ liệu đơn hàng. Xem log ứng dụng để biết nguyên nhân.");
        } catch (Exception exception) {
            log.error("Order loading failed for ownerId={}", shopOwnerId, exception);
            model.addAttribute("orders", Page.empty());
            model.addAttribute("orderLoadError", "Có lỗi khi xử lý đơn hàng. Xem log ứng dụng để biết nguyên nhân.");
        }
        model.addAttribute("keyword", keyword);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("sortDirection", sortDirection);
        addOwnerName(session, model);
        return "shop/orders";
    }

    /**
     * Tác dụng: Nhận mã đơn, gọi getOrderDetailForShopOwner cho chủ shop đăng nhập rồi hiển thị template
     * shop/order-detail.
     * Màn hình / luồng: Màn shop/orders và shop/order-detail; gọi nghiệp vụ qua ShopOrderService.
     */
    @GetMapping("/{orderId}")
    public String orderDetail(@PathVariable Integer orderId,
                              HttpSession session,
                              Model model) {
        Integer ownerId = currentUserId(session);
        if (ownerId == null) return "redirect:/auth/login";
        model.addAttribute("detail", shopOrderService.getOrderDetailForShopOwner(orderId, ownerId));
        addOwnerName(session, model);
        return "shop/order-detail";
    }
    /**
     * Tác dụng: Nhận nút Xác nhận, gọi service.confirmOrder và chuyển lại danh sách hoặc chi tiết; giữ bộ
     * lọc/phân trang và hiển thị lỗi nghiệp vụ nếu có.
     * Màn hình / luồng: Màn shop/orders và shop/order-detail; gọi nghiệp vụ qua ShopOrderService.
     */
    @PostMapping("/{orderId}/confirm")
    public String confirmOrder(@PathVariable Integer orderId,
                               HttpSession session,
                               @RequestParam(defaultValue = "false") boolean returnToDetail,
                               @RequestParam MultiValueMap<String, String> paginationParams,
                               RedirectAttributes redirectAttributes) {
        Integer ownerId = currentUserId(session);
        if (ownerId == null) return "redirect:/auth/login";
        String target = returnToDetail ? "redirect:/shop/orders/" + orderId
                : paginationRedirect("/shop/orders", paginationParams, 0, "keyword", "status", "sortBy", "sortDirection");
        try {
            shopOrderService.confirmOrder(orderId, ownerId);
        } catch (ResponseStatusException exception) {
            if (exception.getStatusCode().value() != HttpStatus.CONFLICT.value()) throw exception;
            redirectAttributes.addFlashAttribute("orderActionError", exception.getReason());
        }
        return target;
    }

    /**
     * Tác dụng: Nhận nút Chuẩn bị hàng, gọi service.startPreparingOrder rồi chuyển về danh sách hoặc chi
     * tiết theo returnToDetail.
     * Màn hình / luồng: Màn shop/orders và shop/order-detail; gọi nghiệp vụ qua ShopOrderService.
     */
    @PostMapping("/{orderId}/preparing")
    public String startPreparingOrder(@PathVariable Integer orderId,
                                      HttpSession session,
                               @RequestParam(defaultValue = "false") boolean returnToDetail,
                               @RequestParam MultiValueMap<String, String> paginationParams) {
        Integer ownerId = currentUserId(session);
        if (ownerId == null) return "redirect:/auth/login";
        String target = returnToDetail ? "redirect:/shop/orders/" + orderId
                : paginationRedirect("/shop/orders", paginationParams, 0, "keyword", "status", "sortBy", "sortDirection");
        shopOrderService.startPreparingOrder(orderId, ownerId);
        return target;
    }

    /**
     * Tác dụng: Nhận nút Sẵn sàng giao, gọi service.markOrderReady rồi tải lại màn đơn hàng với các tham số
     * danh sách được giữ.
     * Màn hình / luồng: Màn shop/orders và shop/order-detail; gọi nghiệp vụ qua ShopOrderService.
     */
    @PostMapping("/{orderId}/ready")
    public String markOrderReady(@PathVariable Integer orderId,
                                 HttpSession session,
                               @RequestParam(defaultValue = "false") boolean returnToDetail,
                               @RequestParam MultiValueMap<String, String> paginationParams) {
        Integer ownerId = currentUserId(session);
        if (ownerId == null) return "redirect:/auth/login";
        String target = returnToDetail ? "redirect:/shop/orders/" + orderId
                : paginationRedirect("/shop/orders", paginationParams, 0, "keyword", "status", "sortBy", "sortDirection");
        shopOrderService.markOrderReady(orderId, ownerId);
        return target;
    }

    /**
     * Tác dụng: Nhận nút Hủy đơn, gọi service.cancelOrder rồi chuyển lại danh sách hoặc chi tiết theo
     * returnToDetail.
     * Màn hình / luồng: Màn shop/orders và shop/order-detail; gọi nghiệp vụ qua ShopOrderService.
     */
    @PostMapping("/{orderId}/cancel")
    public String cancelOrder(@PathVariable Integer orderId,
                              HttpSession session,
                               @RequestParam(defaultValue = "false") boolean returnToDetail,
                               @RequestParam MultiValueMap<String, String> paginationParams) {
        Integer ownerId = currentUserId(session);
        if (ownerId == null) return "redirect:/auth/login";
        String target = returnToDetail ? "redirect:/shop/orders/" + orderId
                : paginationRedirect("/shop/orders", paginationParams, 0, "keyword", "status", "sortBy", "sortDirection");
        shopOrderService.cancelOrder(orderId, ownerId);
        return target;
    }

    /**
     * Tác dụng: Đọc ID người đăng nhập từ session; các handler dùng để lấy đơn/thao tác đúng chủ shop hoặc
     * chuyển sang đăng nhập.
     * Màn hình / luồng: Màn shop/orders và shop/order-detail; gọi nghiệp vụ qua ShopOrderService.
     */
    private Integer currentUserId(HttpSession session) {
        return session != null && session.getAttribute("LOGGED_IN_USER_ID") instanceof Integer id ? id : null;
    }

    /**
     * Tác dụng: Đưa tên chủ shop từ session vào model để hiển thị phần thông tin người dùng của trang đơn hàng.
     * Màn hình / luồng: Màn shop/orders và shop/order-detail; gọi nghiệp vụ qua ShopOrderService.
     */
    private void addOwnerName(HttpSession session, Model model) {
        String name = (String) session.getAttribute("LOGGED_IN_FULLNAME");
        model.addAttribute("shopOwnerName", name != null ? name : "Chủ cửa hàng");
    }

    /**
     * Tác dụng: Đọc page/size từ request, yêu cầu mỗi tham số xuất hiện một lần và là số nguyên trong giới
     * hạn; thiếu thì dùng mặc định, sai thì tạo InvalidPagination.
     * Màn hình / luồng: Hàm nội bộ phục vụ danh sách đơn shop và chuyển về danh sách sau xác nhận/chuẩn
     * bị/sẵn sàng/hủy.
     */
    private static int paginationNumber(MultiValueMap<String, String> params, String key, int fallback, int min, int max) {
        var values = params.get(key);
        if (values == null) return fallback;
        if (values.size() != 1) throw new InvalidPagination("Tham số " + key + " chỉ được xuất hiện một lần.");
        String raw = values.getFirst();
        if (raw == null || !raw.matches("[0-9]+")) throw invalid(key, min, max);
        try {
            int value = Integer.parseInt(raw);
            if (value < min || value > max) throw invalid(key, min, max);
            return value;
        } catch (NumberFormatException ex) {
            throw invalid(key, min, max);
        }
    }

    /**
     * Tác dụng: Tạo lỗi InvalidPagination kèm khoảng giá trị hợp lệ; paginationNumber gọi khi số trang/kích
     * thước trang sai.
     * Màn hình / luồng: Hàm nội bộ phục vụ danh sách đơn shop và chuyển về danh sách sau xác nhận/chuẩn
     * bị/sẵn sàng/hủy.
     */
    private static InvalidPagination invalid(String key, int min, int max) {
        return new InvalidPagination("Tham số " + key + " phải là số nguyên từ " + min + " đến " + max + ". Hãy sửa giá trị đã nhập.");
    }

    /**
     * Tác dụng: Tạo URL redirect giữ page, size và các bộ lọc được truyền vào sau thao tác; dùng
     * paginationNumber để kiểm tra tham số trước khi dựng URL.
     * Màn hình / luồng: Hàm nội bộ phục vụ danh sách đơn shop và chuyển về danh sách sau xác nhận/chuẩn
     * bị/sẵn sàng/hủy.
     */
    private String paginationRedirect(String path, MultiValueMap<String, String> params, int firstPage, String... filters) {
        int page = paginationNumber(params, "page", firstPage, firstPage, Integer.MAX_VALUE);
        int size = paginationNumber(params, "size", 10, 1, 100);
        var uri = org.springframework.web.util.UriComponentsBuilder.fromPath(path)
                .queryParam("page", page).queryParam("size", size);
        for (String filter : filters) {
            String value = params.getFirst(filter);
            if (value != null && !value.isEmpty()) uri.queryParam(filter, value);
        }
        return "redirect:" + uri.build().encode().toUriString();
    }
}
