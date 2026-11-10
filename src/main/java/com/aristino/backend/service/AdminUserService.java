package com.aristino.backend.service;

import com.aristino.backend.common.CurrentUserProvider;
import com.aristino.backend.dto.admin.AdminUserDetail;
import com.aristino.backend.dto.admin.AdminUserSummary;
import com.aristino.backend.entity.Order;
import com.aristino.backend.entity.Role;
import com.aristino.backend.entity.User;
import com.aristino.backend.repository.OrderRepository;
import com.aristino.backend.repository.RoleRepository;
import com.aristino.backend.repository.UserRepository;
import com.aristino.backend.spec.AdminUserSpecs;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Quan ly tai khoan cho cua hang: xem, khoa / mo khoa, cap / thu hoi quyen quan tri.
 *
 * Quy tac:
 * - Quan tri vien khong the khoa chinh minh hoac tu thu hoi quyen cua minh (de luon con it nhat 1 quan tri vien).
 * - Khong xoa tai khoan (da co don hang gan voi tai khoan) - chi khoa.
 * - Quyen duoc doc lai tu database moi request nen cap / thu hoi co hieu luc ngay tren server;
 *   rieng giao dien cua nguoi bi doi quyen chi doi sau khi ho dang nhap lai.
 */
@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final OrderRepository orderRepository;
    private final CurrentUserProvider currentUserProvider;

    @PersistenceContext
    private EntityManager em;

    @Transactional(readOnly = true)
    public Page<AdminUserSummary> list(String q, String role, String status, Pageable pageable) {
        Specification<User> spec = Specification
                .where(AdminUserSpecs.matches(q))
                .and(AdminUserSpecs.hasRole(parseRole(role)))
                .and(AdminUserSpecs.hasStatus(parseStatus(status)));

        Page<User> page = userRepository.findAll(spec, pageable);
        Map<Long, Long> counts = orderCounts(page.getContent().stream().map(User::getId).toList());
        return page.map(u -> AdminUserSummary.from(u, counts.getOrDefault(u.getId(), 0L)));
    }

    @Transactional(readOnly = true)
    public AdminUserDetail getDetail(Long id) {
        return toDetail(find(id));
    }

    @Transactional
    public AdminUserDetail updateStatus(Long id, String status) {
        User user = find(id);
        User.Status target = User.Status.valueOf(status);

        if (target == User.Status.LOCKED && isCurrentUser(user)) {
            throw new IllegalStateException("Ban khong the khoa chinh minh");
        }
        user.setStatus(target);
        userRepository.save(user);
        return toDetail(user);
    }

    @Transactional
    public AdminUserDetail setAdmin(Long id, boolean admin) {
        User user = find(id);

        if (admin) {
            Role adminRole = roleRepository.findByName("ROLE_ADMIN")
                    .orElseThrow(() -> new IllegalStateException("Chua seed du lieu ROLE_ADMIN trong bang roles"));
            user.getRoles().add(adminRole);
        } else {
            if (isCurrentUser(user)) {
                throw new IllegalStateException("Ban khong the tu thu hoi quyen quan tri cua minh");
            }
            user.getRoles().removeIf(r -> "ROLE_ADMIN".equals(r.getName()));
        }
        userRepository.save(user);
        return toDetail(user);
    }

    // ===== Ham phu =====

    private User find(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Khong tim thay nguoi dung"));
    }

    private boolean isCurrentUser(User user) {
        return user.getId().equals(currentUserProvider.getCurrentUser().getId());
    }

    private AdminUserDetail toDetail(User user) {
        Page<Order> recent = orderRepository.findByUserId(
                user.getId(), PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt")));

        Object spent = em.createQuery(
                        "select sum(o.totalAmount) from Order o where o.user.id = :id and o.orderStatus = :s")
                .setParameter("id", user.getId())
                .setParameter("s", Order.OrderStatus.COMPLETED)
                .getSingleResult();

        return new AdminUserDetail(
                AdminUserSummary.from(user, recent.getTotalElements()),
                spent == null ? BigDecimal.ZERO : new BigDecimal(spent.toString()),
                recent.getContent().stream()
                        .map(o -> new AdminUserDetail.RecentOrder(
                                o.getId(), o.getOrderCode(), o.getTotalAmount(), o.getOrderStatus().name(), o.getCreatedAt()))
                        .toList()
        );
    }

    // So don hang cua tung tai khoan trong trang hien tai (1 truy van chung, khong goi tung nguoi)
    private Map<Long, Long> orderCounts(List<Long> userIds) {
        Map<Long, Long> result = new HashMap<>();
        if (userIds.isEmpty()) {
            return result;
        }
        List<?> rows = em.createQuery(
                        "select o.user.id, count(o) from Order o where o.user.id in :ids group by o.user.id")
                .setParameter("ids", userIds)
                .getResultList();
        for (Object row : rows) {
            Object[] r = (Object[]) row;
            result.put(((Number) r[0]).longValue(), ((Number) r[1]).longValue());
        }
        return result;
    }

    private String parseRole(String role) {
        if (role == null || role.isBlank()) {
            return null;
        }
        return switch (role.trim().toUpperCase()) {
            case "ADMIN" -> "ROLE_ADMIN";
            case "CUSTOMER" -> "ROLE_CUSTOMER";
            default -> throw new IllegalArgumentException("Quyen khong hop le");
        };
    }

    private User.Status parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return User.Status.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Trang thai tai khoan khong hop le");
        }
    }
}