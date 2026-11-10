package com.aristino.backend.dto.admin;

import com.aristino.backend.dto.order.OrderResponse;

import java.util.List;

/** Chi tiet don hang o trang quan tri: thong tin don + khach dat + cac thao tac duoc phep. */
public record AdminOrderDetail(
        OrderResponse order,
        String customerName,
        String customerEmail,
        String customerPhone,
        // Cac trang thai co the chuyen tiep (CONFIRMED / SHIPPING / COMPLETED), rong neu khong co
        List<String> nextStatuses,
        // Neu khong the xac nhan don thi ghi ma ly do (hien tai chi co AWAITING_PAYMENT), nguoc lai null
        String blockedReason,
        // Quan tri duoc huy khi don dang cho xac nhan hoac da xac nhan
        boolean canCancel
) {
}