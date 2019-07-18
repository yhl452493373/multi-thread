import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 异步计算
 *
 * @param <ReturnType> 此参数限制异步计算的任务结果类型.设置后,所有任务都要求返回此类型的数据.如果数据类型不同,可以设为Object，然后根据Object类型处理
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public class MultiThread<ReturnType> {
    //异步线程的结果列表
    private final Collection<ReturnType> results;
    //线程池
    private final ThreadPoolExecutor executor;

    /**
     * 构造函数
     *
     * @param threadCount 异步线程数
     */
    public MultiThread(int threadCount) {
        this.executor = new ThreadPoolExecutor(threadCount, threadCount, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>());
        this.results = new ArrayList<>();
    }

    /**
     * 构造函数
     *
     * @param threadCount 异步线程数
     * @param tasks       要执行的任务
     */
    public MultiThread(int threadCount, List<MultiTask<ReturnType>> tasks) {
        this(threadCount);
        tasks.forEach(this::run);
    }

    /**
     * 构造函数
     *
     * @param threadCount 异步线程数
     * @param tasks       要执行的任务
     * @param finished    任务执行后计算结果的回调
     */
    public MultiThread(int threadCount, List<MultiTask<ReturnType>> tasks, MultiFinished<ReturnType> finished) {
        this(threadCount, tasks);
        this.done(finished);
    }

    /**
     * 添加一个需要执行的任务
     *
     * @param task 需要执行的任务
     */
    public void run(MultiTask<ReturnType> task) {
        executor.execute(() -> {
            //使results集合线程安全，避免出现线程结果未放到集合就执行done里面的calculate.run方法的情况
            synchronized (results) {
                results.add(task.run());
            }
        });
    }

    /**
     * 执行任务并处理所有任务返回的结果集合
     *
     * @param finished 处理结果的回调
     * @param timeout  任务经过多久后超时
     * @param unit     任务超时的单位
     */
    public void done(MultiFinished<ReturnType> finished, long timeout, TimeUnit unit) {
        try {
            executor.shutdown();
            //true - 正常结束
            if (executor.awaitTermination(timeout, unit)) {
                finished.run(results);
            } else {
                //false - 任务结束时已超时
                throw new TimeoutException("任务超时");
            }
        } catch (InterruptedException | TimeoutException e) {
            e.printStackTrace();
        }
    }

    /**
     * 执行任务并处理任务返回的结果集合。此方法默认{@link Integer#MAX_VALUE}天后超时
     *
     * @param finished 处理结果的回调
     */
    public void done(MultiFinished<ReturnType> finished) {
        done(finished, Integer.MAX_VALUE, TimeUnit.DAYS);
    }

    //以下为测试方法
    private static void test() {
        MultiThread<String> multiThread = new MultiThread<>(2);
        for (int i = 0; i < 3; i++) {
            int finalI = i;
            multiThread.run(() -> {
                MultiThread<String> stringMultiThread = new MultiThread<>(10);
                for (int j = 0; j < 4; j++) {
                    int finalJ = j;
                    stringMultiThread.run(() -> "i:" + finalI + "-j:" + finalJ + "");
                }
                final String[] last = new String[1];
                stringMultiThread.done(results -> last[0] = String.join("|", results));
                return last[0];
            });
        }
        multiThread.done(results -> System.out.println("\n" + results + "\n"));
    }

    @SuppressWarnings("InfiniteRecursion")
    private static void repeatTest() throws InterruptedException {
        test();
        Thread.sleep(2000);
        repeatTest();
    }

    public static void main(String[] args) throws InterruptedException {
        repeatTest();
    }
}
