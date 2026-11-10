package com.aristino.backend.dto.admin;

import java.math.BigDecimal;
import java.util.List;

/** Du lieu trang tong quan (dashboard) cua quan tri. */
public record StatsResponse(
        int days,
        Kpis kpis,
        List<DailyPoint> daily,
        List<StatusCount> ordersByStatus,
        List<TopProduct> topProducts
) {
    public record Kpis(
            BigDecimal revenueInPeriod,     // doanh thu cac don HOAN TAT trong khoang thoi gian
            long completedOrdersInPeriod,   // so don hoan tat trong khoang thoi gian
            BigDecimal revenueAllTime,      // doanh thu cac don hoan tat tu truoc toi nay
            long newOrdersToday,            // so don dat hom nay (moi trang thai)
            long pendingOrders,             // so don dang cho xac nhan
            long totalOrders,
            long totalCustomers,
            long totalProducts
    ) {
    }

    public record DailyPoint(String date, BigDecimal revenue, long orders) {
    }

    public record StatusCount(String status, long count) {
    }

    public record TopProduct(String name, long quantity, BigDecimal revenue) {
    }
}