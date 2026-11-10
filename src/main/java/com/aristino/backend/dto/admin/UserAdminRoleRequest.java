package com.aristino.backend.dto.admin;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserAdminRoleRequest {

    // true = cap quyen quan tri, false = thu hoi quyen quan tri
    @NotNull(message = "Thieu thong tin quyen quan tri")
    private Boolean admin;
}