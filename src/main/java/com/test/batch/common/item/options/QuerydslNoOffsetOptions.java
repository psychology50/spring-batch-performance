package com.test.batch.common.item.options;

import com.querydsl.core.types.Path;
import com.querydsl.jpa.impl.JPAQuery;
import com.test.batch.common.item.expression.Expression;
import jakarta.annotation.Nonnull;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.lang.reflect.Field;

public abstract class QuerydslNoOffsetOptions<T> {
    protected final String fieldName;
    protected final Expression expression;
    protected Log logger = LogFactory.getLog(getClass());

    protected QuerydslNoOffsetOptions(@Nonnull Path field, @Nonnull Expression expression) {
        String[] qField = field.toString().split("\\.");
        this.fieldName = qField[qField.length - 1];
        this.expression = expression;

        if (logger.isDebugEnabled()) {
            logger.debug("fieldName= " + fieldName);
        }
    }

    public QuerydslNoOffsetOptions(@Nonnull String dtoField, @Nonnull Expression expression) {
        this.fieldName = dtoField;
        this.expression = expression;

        if (logger.isDebugEnabled()) {
            logger.debug("fieldName= " + fieldName);
        }
    }

    public String getFieldName() {
        return fieldName;
    }

    /**
     * currentId와 lastId를 초기화하기 위한 JPAQuery를 설정한다.
     * <p>
     * idSelectQuery가 null이면 내부적으로 currentId와 lastId를 초기화한다.
     */
    public abstract void setIdSelectQuery(JPAQuery<T> idSelectQuery);

    public abstract void initKeys(JPAQuery<T> query, int page);

    protected abstract void initFirstId(JPAQuery<T> query);

    protected abstract void initLastId(JPAQuery<T> query);

    public abstract JPAQuery<T> createQuery(JPAQuery<T> query, int page);

    public abstract void resetCurrentId(T item);

    protected Object getFiledValue(T item) {
        try {
            Field field = item.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(item);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            logger.error("Not Found or Not Access Field= " + fieldName, e);
            throw new IllegalArgumentException("Not Found or Not Access Field");
        }
    }

    public boolean isGroupByQuery(JPAQuery<T> query) {
        return isGroupByQuery(query.toString());
    }

    public boolean isGroupByQuery(String sql) {
        return sql.contains("group by");

    }

}