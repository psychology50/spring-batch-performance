package com.test.batch.repository;

import com.test.batch.domain.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long>, NotificationCustomRepository {
}
