package holoenglish.com.db;

public interface IBaseDao<T> {

    /**
     * 插入数据
     * @param entity
     * @return
     */
    Long insert(T entity);

    Long update(T entity, T where);
}
