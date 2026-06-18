package com.antojito.maps_backend.service;

import com.antojito.maps_backend.dto.DashboardResponse;
import com.antojito.maps_backend.dto.DashboardTopCouponResponse;
import com.antojito.maps_backend.dto.OwnerAuthorizationRequest;
import com.antojito.maps_backend.exception.ResourceNotFoundException;
import com.antojito.maps_backend.repository.RestauranteRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final RestauranteRepository restauranteRepository;
    private final JdbcTemplate jdbcTemplate;

    @Transactional(readOnly = true)
    public DashboardResponse getRestaurantDashboard(UUID restaurantId, OwnerAuthorizationRequest request) {
        requireRestaurant(restaurantId);
        UUID ownerUuid = resolveOwnerUuid(request.getOwnerUuid(), request.getOwnerMail());
        validateOwnerRestaurantRelationship(ownerUuid, restaurantId);

        return DashboardResponse.builder()
                .restaurantId(restaurantId)
                .activeCoupons(countActiveCoupons(restaurantId))
                .totalClaimedCoupons(countClaimedCoupons(restaurantId))
                .totalUsedCoupons(countUsedCoupons(restaurantId))
                .recurringClients(countRecurringClients(restaurantId))
                .expiredPromotions(countExpiredPromotions(restaurantId))
                .topUsedCoupons(findTopUsedCoupons(restaurantId))
                .build();
    }

    private Long countActiveCoupons(UUID restaurantId) {
        return queryCount("""
                select count(*)
                from coupons
                where restaurant_uuid = ?
                  and status = 'ACTIVE'
                  and start_date <= current_date
                  and expiration_date >= current_date
                """, restaurantId);
    }

    private Long countClaimedCoupons(UUID restaurantId) {
        return queryCount("""
                select count(*)
                from claimed_coupons cc
                join coupons c on c.uuid = cc.coupon_uuid
                where c.restaurant_uuid = ?
                """, restaurantId);
    }

    private Long countUsedCoupons(UUID restaurantId) {
        return queryCount("""
                select count(*)
                from claimed_coupons cc
                join coupons c on c.uuid = cc.coupon_uuid
                where c.restaurant_uuid = ?
                  and cc.status = 'USED'
                """, restaurantId);
    }

    private Long countRecurringClients(UUID restaurantId) {
        return queryCount("""
                select count(*)
                from (
                    select cc.client_uuid
                    from claimed_coupons cc
                    join coupons c on c.uuid = cc.coupon_uuid
                    where c.restaurant_uuid = ?
                      and cc.status = 'USED'
                    group by cc.client_uuid
                    having count(*) > 1
                ) recurrent_clients
                """, restaurantId);
    }

    private Long countExpiredPromotions(UUID restaurantId) {
        return queryCount("""
                select count(*)
                from promotions
                where id_restaurant = ?
                  and date_end_promotion < current_date
                """, restaurantId);
    }

    private List<DashboardTopCouponResponse> findTopUsedCoupons(UUID restaurantId) {
        return jdbcTemplate.query(
                """
                select c.uuid, c.name, count(cc.uuid) as total_used
                from coupons c
                join claimed_coupons cc on cc.coupon_uuid = c.uuid
                where c.restaurant_uuid = ?
                  and cc.status = 'USED'
                group by c.uuid, c.name
                order by total_used desc, c.name asc
                limit 5
                """,
                (rs, rowNum) -> DashboardTopCouponResponse.builder()
                        .couponId(rs.getObject("uuid", UUID.class))
                        .couponName(rs.getString("name"))
                        .totalUsed(rs.getLong("total_used"))
                        .build(),
                restaurantId);
    }

    private Long queryCount(String sql, UUID restaurantId) {
        Number count = jdbcTemplate.queryForObject(sql, Number.class, restaurantId);
        return count == null ? 0L : count.longValue();
    }

    private void requireRestaurant(UUID restaurantId) {
        if (!restauranteRepository.existsById(restaurantId)) {
            throw new ResourceNotFoundException("No existe restaurante con uuid " + restaurantId);
        }
    }

    private UUID resolveOwnerUuid(UUID ownerUuid, String ownerMailValue) {
        String ownerMail = normalizeMail(ownerMailValue);

        if (ownerUuid == null && ownerMail == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debes enviar ownerUuid u ownerMail");
        }

        if (ownerUuid != null) {
            requireOwner(ownerUuid);
            if (ownerMail != null) {
                validateOwnerIdentity(ownerUuid, ownerMail);
            }
            return ownerUuid;
        }

        return findOwnerUuidByMail(ownerMail);
    }

    private void requireOwner(UUID ownerUuid) {
        Integer matches = jdbcTemplate.queryForObject(
                "select count(*) from owner_account where uuid = ?",
                Integer.class,
                ownerUuid);

        if (matches == null || matches == 0) {
            throw new ResourceNotFoundException("No existe owner con uuid " + ownerUuid);
        }
    }

    private void validateOwnerIdentity(UUID ownerUuid, String ownerMail) {
        Integer matches = jdbcTemplate.queryForObject(
                "select count(*) from owner_account where uuid = ? and mail = ?",
                Integer.class,
                ownerUuid,
                ownerMail);

        if (matches == null || matches == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ownerUuid y ownerMail no corresponden al mismo owner");
        }
    }

    private UUID findOwnerUuidByMail(String ownerMail) {
        try {
            UUID ownerUuid = jdbcTemplate.queryForObject(
                    "select uuid from owner_account where mail = ?",
                    UUID.class,
                    ownerMail);
            if (ownerUuid == null) {
                throw new ResourceNotFoundException("No existe owner con mail " + ownerMail);
            }
            return ownerUuid;
        } catch (EmptyResultDataAccessException exception) {
            throw new ResourceNotFoundException("No existe owner con mail " + ownerMail);
        }
    }

    private void validateOwnerRestaurantRelationship(UUID ownerUuid, UUID restaurantId) {
        Integer matches = jdbcTemplate.queryForObject(
                "select count(*) from owner_restaurant where id_owner = ? and id_restaurant = ?",
                Integer.class,
                ownerUuid,
                restaurantId);

        if (matches == null || matches == 0) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "El owner no tiene permisos para ver el dashboard de este restaurante");
        }
    }

    private String normalizeMail(String mail) {
        if (mail == null) {
            return null;
        }

        String normalized = mail.trim().toLowerCase();
        return normalized.isBlank() ? null : normalized;
    }
}
