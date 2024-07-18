package com.test.batch.repository;

import com.test.batch.common.type.Announcement;

import java.util.List;

public interface NotificationCustomRepository {
    void saveDailySpendingAnnounceInBulk(List<Long> userIds, Announcement announcement);
}
