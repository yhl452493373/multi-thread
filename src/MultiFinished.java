import java.util.Collection;

/**
 * 用于在所有任务执行完后计算结果的接口
 */
public interface MultiFinished<ReturnType> {
    void run(Collection<ReturnType> results);
}
