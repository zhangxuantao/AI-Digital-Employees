package com.ai.cs.domain.customer;

import com.ai.cs.infrastructure.persistence.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
@Entity
@Table(name = "customer_profile")
public class CustomerProfile extends BaseEntity {

    @Column(name = "nickname")
    private String nickname;

    @Column(name = "platform")
    private String platform;

    @Column(name = "openid")
    private String openid;

    @Column(name = "avatar")
    private String avatar;

    @Column(name = "phone")
    private String phone;

    @Column(name = "email")
    private String email;

    @Column(name = "gender")
    private String gender;

    @Column(name = "city")
    private String city;

    @Column(name = "tags", columnDefinition = "JSON")
    private String tags;

    @Column(name = "extra_fields", columnDefinition = "JSON")
    private String extraFields;
}
