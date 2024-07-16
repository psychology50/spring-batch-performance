package com.test.batch.domain;

import com.test.batch.common.converter.AnnouncementConverter;
import com.test.batch.common.converter.NoticeTypeConverter;
import com.test.batch.common.model.DateAuditable;
import com.test.batch.common.type.Announcement;
import com.test.batch.common.type.NoticeType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Getter
@Table(name = "notification")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification extends DateAuditable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime readAt;
    @Convert(converter = NoticeTypeConverter.class)
    private NoticeType type;
    @Convert(converter = AnnouncementConverter.class)
    private Announcement announcement; // 공지 종류

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver")
    private User receiver;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender")
    private User sender;

    private Notification(LocalDateTime readAt, NoticeType type, Announcement announcement, User receiver, User sender) {
        this.readAt = readAt;
        this.type = Objects.requireNonNull(type);
        this.announcement = Objects.requireNonNull(announcement);
        this.receiver = receiver;
        this.sender = sender;
    }

    @Override
    public String toString() {
        return "Notification{" +
                "id=" + id +
                ", readAt=" + readAt +
                ", type=" + type +
                ", announcement=" + announcement +
                '}';
    }

    public static class Builder {
        private LocalDateTime readAt;
        private NoticeType type;
        private Announcement announcement;

        private User receiver = null;
        private User sender = null;

        public Builder(NoticeType type, Announcement announcement) {
            this.type = type;
            this.announcement = announcement;
        }

        public Builder readAt(LocalDateTime readAt) {
            this.readAt = readAt;
            return this;
        }

        public Builder receiver(User receiver) {
            this.receiver = receiver;
            return this;
        }

        public Builder sender(User sender) {
            this.sender = sender;
            return this;
        }

        public Notification build() {
            return new Notification(readAt, type, announcement, receiver, sender);
        }
    }
}
