package com.test.batch.common.item.options;

import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.StringPath;
import com.querydsl.jpa.impl.JPAQuery;
import com.test.batch.common.item.expression.Expression;
import jakarta.annotation.Nonnull;

public class QuerydslNoOffsetStringOptions<T> extends QuerydslNoOffsetOptions<T> {
    private final StringPath field;
    private String currentId;
    private String lastId;

    private JPAQuery<T> idSelectQuery;

    public QuerydslNoOffsetStringOptions(@Nonnull StringPath field,
                                         @Nonnull Expression expression) {
        super(field, expression);
        this.field = field;
    }

    public QuerydslNoOffsetStringOptions(@Nonnull StringPath field,
                                         @Nonnull Expression expression,
                                         String idName) {
        super(idName, expression);
        this.field = null;
    }

    public String getCurrentId() {
        return currentId;
    }

    public String getLastId() {
        return lastId;
    }

    @Override
    public void setIdSelectQuery(JPAQuery<T> idSelectQuery) {
        this.idSelectQuery = idSelectQuery;
    }

    @Override
    public void initKeys(JPAQuery<T> query, int page) {
        if (page == 0) {
            query = (idSelectQuery != null) ? idSelectQuery.clone() : query.clone();
            initFirstId(query);
            initLastId(query);

            if (logger.isDebugEnabled()) {
                logger.debug("First Key= " + currentId + ", Last Key= " + lastId);
            }
        }
    }

    @Override
    protected void initFirstId(JPAQuery<T> query) {
        JPAQuery<T> clone = query.clone();
        boolean isGroupByQuery = isGroupByQuery(clone);

        if (isGroupByQuery) {
            currentId = clone
                    .select(field)
                    .orderBy(expression.isAsc() ? field.asc() : field.desc())
                    .fetchFirst();
        } else {
            currentId = clone
                    .select(expression.isAsc() ? field.min() : field.max())
                    .fetchFirst();
        }

    }

    @Override
    protected void initLastId(JPAQuery<T> query) {
        JPAQuery<T> clone = query.clone();
        boolean isGroupByQuery = isGroupByQuery(clone);

        if (isGroupByQuery) {
            lastId = clone
                    .select(field)
                    .orderBy(expression.isAsc() ? field.desc() : field.asc())
                    .fetchFirst();
        } else {
            lastId = clone
                    .select(expression.isAsc() ? field.max() : field.min())
                    .fetchFirst();
        }
    }

    @Override
    public JPAQuery<T> createQuery(JPAQuery<T> query, int page) {
        if (currentId == null) {
            return query;
        }

        return query
                .where(whereExpression(page))
                .orderBy(orderExpression());
    }

    private BooleanExpression whereExpression(int page) {
        return expression.where(field, page, currentId)
                .and(expression.isAsc() ? field.loe(lastId) : field.goe(lastId));
    }

    private OrderSpecifier<String> orderExpression() {
        return expression.order(field);
    }

    @Override
    public void resetCurrentId(T item) {
        currentId = (String) getFiledValue(item);
        if (logger.isDebugEnabled()) {
            logger.debug("Current Select Key= " + currentId);
        }
    }
}
