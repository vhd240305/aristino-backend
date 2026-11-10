package com.aristino.backend.service;

import com.aristino.backend.dto.admin.StatsResponse;
import com.aristino.backend.entity.Order;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Thong ke doanh thu cho trang tong quan.
 * Quy uoc: DOANH THU = tong tien cua cac don o trang thai COMPLETED (da giao thanh cong).
 * Ngay cua tung don tinh theo ngay DAT hang (created_at), vi bang orders chua co cot ngay hoan tat.
 */
@Service
@Transactional(readOnly = true)
public class StatsService {

    @PersistenceContext
    private EntityManager em;

    public StatsResponse getStats(int days) {
        int range = Math.min(Math.max(days, 1), 365);
        LocalDate today = LocalDate.now();
        LocalDateTime from = today.minusDays(range - 1L).atStartOfDay();
        LocalDateTime startOfToday = today.atStartOfDay();

        StatsResponse.Kpis kpis = new StatsResponse.Kpis(
                money(em.createQuery(
                        "select sum(o.totalAmount) from Order o where o.orderStatus = :s and o.createdAt >= :from")
                        .setParameter("s", Order.OrderStatus.COMPLETED).setParameter("from", from)),
                count(em.createQuery(
                        "select count(o) from Order o where o.orderStatus = :s and o.createdAt >= :from")
                        .setParameter("s", Order.OrderStatus.COMPLETED).setParameter("from", from)),
                money(em.createQuery("select sum(o.totalAmount) from Order o where o.orderStatus = :s")
                        .setParameter("s", Order.OrderStatus.COMPLETED)),
                count(em.createQuery("select count(o) from Order o where o.createdAt >= :t")
                        .setParameter("t", startOfToday)),
                count(em.createQuery("select count(o) from Order o where o.orderStatus = :s")
                        .setParameter("s", Order.OrderStatus.PENDING)),
                count(em.createQuery("select count(o) from Order o")),
                count(em.createQuery(
                        "select count(distinct u) from User u join u.roles r where r.name = 'ROLE_CUSTOMER'")),
                count(em.createQuery("select count(p) from Product p"))
        );

        return new StatsResponse(range, kpis, dailySeries(from, today), statusCounts(), topProducts(from));
    }

    // Doanh thu theo ngay; ngay khong co don duoc dien 0 de bieu do lien tuc
    private List<StatsResponse.DailyPoint> dailySeries(LocalDateTime from, LocalDate today) {
        Query q = em.createNativeQuery(
                "select date(o.created_at) as d, sum(o.total_amount) as revenue, count(*) as cnt "
                        + "from orders o where o.order_status = 'COMPLETED' and o.created_at >= :from "
                        + "group by date(o.created_at)");
        q.setParameter("from", from);

        Map<LocalDate, StatsResponse.DailyPoint> byDay = new HashMap<>();
        for (Object row : q.getResultList()) {
            Object[] r = (Object[]) row;
            LocalDate day = LocalDate.parse(r[0].toString());
            byDay.put(day, new StatsResponse.DailyPoint(
                    day.toString(), new BigDecimal(r[1].toString()), ((Number) r[2]).longValue()));
        }

        List<StatsResponse.DailyPoint> result = new ArrayList<>();
        for (LocalDate d = from.toLocalDate(); !d.isAfter(today); d = d.plusDays(1)) {
            result.add(byDay.getOrDefault(d, new StatsResponse.DailyPoint(d.toString(), BigDecimal.ZERO, 0L)));
        }
        return result;
    }

    private List<StatsResponse.StatusCount> statusCounts() {
        List<StatsResponse.StatusCount> result = new ArrayList<>();
        for (Object row : em.createQuery(
                "select o.orderStatus, count(o) from Order o group by o.orderStatus").getResultList()) {
            Object[] r = (Object[]) row;
            result.add(new StatsResponse.StatusCount(((Order.OrderStatus) r[0]).name(), ((Number) r[1]).longValue()));
        }
        return result;
    }

    // Top 5 san pham ban chay (theo so luong) trong cac don hoan tat
    private List<StatsResponse.TopProduct> topProducts(LocalDateTime from) {
        List<StatsResponse.TopProduct> result = new ArrayList<>();
        Query q = em.createQuery(
                "select oi.productName, sum(oi.quantity), sum(oi.price * oi.quantity) "
                        + "from OrderItem oi join oi.order o "
                        + "where o.orderStatus = :s and o.createdAt >= :from "
                        + "group by oi.productName order by sum(oi.quantity) desc");
        q.setParameter("s", Order.OrderStatus.COMPLETED);
        q.setParameter("from", from);
        q.setMaxResults(5);
        for (Object row : q.getResultList()) {
            Object[] r = (Object[]) row;
            result.add(new StatsResponse.TopProduct(
                    (String) r[0], ((Number) r[1]).longValue(), new BigDecimal(r[2].toString())));
        }
        return result;
    }

    // sum() tra ve null khi khong co dong nao -> doi thanh 0
    private BigDecimal money(Query q) {
        Object value = q.getSingleResult();
        return value == null ? BigDecimal.ZERO : new BigDecimal(value.toString());
    }

    private long count(Query q) {
        return ((Number) q.getSingleResult()).longValue();
    }
}