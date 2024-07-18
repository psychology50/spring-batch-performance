package com.test.batch.domain;

import com.test.batch.common.model.DateAuditable;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Getter
@Table(name = "user")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@DynamicInsert
@SQLRestriction("deleted_at IS NULL")
@SQLDelete(sql = "UPDATE user SET deleted_at = NOW() WHERE id = ?")
public class User extends DateAuditable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;
    private String name;
    @Embedded
    private NotifySetting notifySetting;
    private LocalDateTime deletedAt;

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    private List<DeviceToken> deviceTokens = new ArrayList<>();

    @Builder
    private User(String username, String name, NotifySetting notifySetting, LocalDateTime deletedAt) {
        this.username = Objects.requireNonNull(username, "username은 null이 될 수 없습니다.");
        this.name = Objects.requireNonNull(name, "name은 null이 될 수 없습니다.");
        this.notifySetting = Objects.requireNonNull(notifySetting, "notifySetting은 null이 될 수 없습니다.");
        this.deletedAt = deletedAt;
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", name='" + name + '\'' +
                ", deletedAt=" + deletedAt +
                '}';
    }
}
