package com.test.batch.common.provider;

import com.querydsl.jpa.impl.JPAQuery;
import jakarta.persistence.Query;
import org.springframework.batch.item.database.orm.AbstractJpaQueryProvider;
import org.springframework.util.Assert;

public class JpaQueryDslProvider<E> extends AbstractJpaQueryProvider {
    private final JPAQuery<E> query;

    public JpaQueryDslProvider(JPAQuery<E> query) {
        this.query = query;
    }

    @Override
    public Query createQuery() {
        return query.createQuery();
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Assert.state(query != null, "Querydsl query must be set");
    }
}
