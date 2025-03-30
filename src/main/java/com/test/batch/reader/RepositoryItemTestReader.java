package com.test.batch.reader;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.item.adapter.AbstractMethodInvokingDelegator;
import org.springframework.batch.item.adapter.DynamicMethodInvocationException;
import org.springframework.batch.item.support.AbstractItemCountingItemStreamItemReader;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.MethodInvoker;
import org.springframework.util.StringUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
public class RepositoryItemTestReader<T> extends AbstractItemCountingItemStreamItemReader<T> implements InitializingBean {

    private final Lock lock = new ReentrantLock();
    protected Log logger = LogFactory.getLog(getClass());
    private PagingAndSortingRepository<?, ?> repository;
    private Sort sort;
    private volatile int page = 0;
    private int pageSize = 10;
    private volatile int current = 0;
    private List<?> arguments;
    private volatile List<T> results;
    private String methodName;

    // 성능 측정 파라미터
    private long methodInvokerCreationTime = 0;
    private long prepareTime = 0;
    private long invokeTime = 0;
    private int invocationCount = 0;

    public RepositoryItemTestReader() {
        setName(ClassUtils.getShortName(org.springframework.batch.item.data.RepositoryItemReader.class));
    }

    public void setArguments(List<?> arguments) {
        this.arguments = arguments;
    }

    public void setSort(Map<String, Sort.Direction> sorts) {
        this.sort = convertToSort(sorts);
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public void setRepository(PagingAndSortingRepository<?, ?> repository) {
        this.repository = repository;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Assert.state(repository != null, "A PagingAndSortingRepository is required");
        Assert.state(pageSize > 0, "Page size must be greater than 0");
        Assert.state(sort != null, "A sort is required");
        Assert.state(this.methodName != null && !this.methodName.isEmpty(), "methodName is required.");
        if (isSaveState()) {
            Assert.state(StringUtils.hasText(getName()), "A name is required when saveState is set to true.");
        }
    }

    @Nullable
    @Override
    protected T doRead() throws Exception {

        this.lock.lock();
        try {
            boolean nextPageNeeded = (results != null && current >= results.size());

            if (results == null || nextPageNeeded) {

                if (logger.isDebugEnabled()) {
                    logger.debug("Reading page " + page);
                }

                results = doPageRead();
                page++;

                if (results.isEmpty()) {
                    return null;
                }

                if (nextPageNeeded) {
                    current = 0;
                }
            }

            if (current < results.size()) {
                T curLine = results.get(current);
                current++;
                return curLine;
            } else {
                return null;
            }
        } finally {
            this.lock.unlock();
        }
    }

    @Override
    protected void jumpToItem(int itemLastIndex) throws Exception {
        this.lock.lock();
        try {
            page = itemLastIndex / pageSize;
            current = itemLastIndex % pageSize;
        } finally {
            this.lock.unlock();
        }
    }

    /**
     * Performs the actual reading of a page via the repository. Available for overriding
     * as needed.
     *
     * @return the list of items that make up the page
     * @throws Exception Based on what the underlying method throws or related to the
     *                   calling of the method
     */
    @SuppressWarnings("unchecked")
    protected List<T> doPageRead() throws Exception {
        Pageable pageRequest = PageRequest.of(page, pageSize, sort);

        // 리플렉션 성능 측정
        long start = System.nanoTime();
        MethodInvoker invoker = createMethodInvoker(repository, methodName);
        methodInvokerCreationTime += (System.nanoTime() - start);

        List<Object> parameters = new ArrayList<>();

        if (arguments != null && arguments.size() > 0) {
            parameters.addAll(arguments);
        }

        parameters.add(pageRequest);

        invoker.setArguments(parameters.toArray());

        // 쿼리 수행 시간 성능 측정
        start = System.nanoTime();
        Slice<T> curPage = (Slice<T>) doInvoke(invoker);
        invokeTime += (System.nanoTime() - start);

        invocationCount++;
        if (invocationCount % 10 == 0) { // 10번째마다 성능 측정
            logPerformanceMetrics();
        }

        return curPage.getContent();
    }

    @Override
    protected void doOpen() throws Exception {
    }

    @Override
    protected void doClose() throws Exception {
        this.lock.lock();
        try {
            current = 0;
            page = 0;
            results = null;
        } finally {
            this.lock.unlock();
        }
    }

    private Sort convertToSort(Map<String, Sort.Direction> sorts) {
        List<Sort.Order> sortValues = new ArrayList<>();

        for (Map.Entry<String, Sort.Direction> curSort : sorts.entrySet()) {
            sortValues.add(new Sort.Order(curSort.getValue(), curSort.getKey()));
        }

        return Sort.by(sortValues);
    }

    private Object doInvoke(MethodInvoker invoker) throws Exception {
        long start = System.nanoTime();
        try {
            invoker.prepare();
            prepareTime += (System.nanoTime() - start);
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            throw new DynamicMethodInvocationException(e);
        }

        try {
            return invoker.invoke();
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof Exception) {
                throw (Exception) e.getCause();
            } else {
                throw new AbstractMethodInvokingDelegator.InvocationTargetThrowableWrapper(e.getCause());
            }
        } catch (IllegalAccessException e) {
            throw new DynamicMethodInvocationException(e);
        }
    }

    private MethodInvoker createMethodInvoker(Object targetObject, String targetMethod) {
        MethodInvoker invoker = new MethodInvoker();
        invoker.setTargetObject(targetObject);
        invoker.setTargetMethod(targetMethod);
        return invoker;
    }

    private void logPerformanceMetrics() {
        log.info("--- RepositoryItemReader 성능 측정 (호출 횟수: {}) ---", invocationCount);
        log.info("메서드 Invoker 생성 평균 시간: {}ms", methodInvokerCreationTime / (invocationCount * 1_000_000.0));
        log.info("메서드 prepare() 평균 시간: {}ms", prepareTime / (invocationCount * 1_000_000.0));
        log.info("메서드 invoke() 평균 시간: {}ms", invokeTime / (invocationCount * 1_000_000.0));
        log.info("총 리플렉션 관련 평균 시간: {}ms",
                (methodInvokerCreationTime + prepareTime + invokeTime) / (invocationCount * 1_000_000.0));
    }
}