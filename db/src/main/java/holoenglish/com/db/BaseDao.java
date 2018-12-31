package holoenglish.com.db;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;
import holoenglish.com.db.annotation.DbFeild;
import holoenglish.com.db.annotation.DbTable;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public abstract class BaseDao<T> implements IBaseDao<T> {

    /**
     * 持有数据库操作类的引用
     */
    private SQLiteDatabase dataBase;
    /**
     * 保证实例化一次
     */
    private boolean isInit = false;
    /**
     * 要操作的表
     */
    private String tableName;
    /**
     * 持有要操作的表所对应的java类型实例
     */
    private Class<T> entityClazz;
    /**
     * 维护表名与成员变量名的映射关系
     * key -> 表名
     * value -> Field类型
     */
    private HashMap<String, Field> cacheMap;

    protected synchronized boolean init(Class<T> entityClazz, SQLiteDatabase sqLiteDatabase) {
        // 只进行一次初始化操作
        if (!this.isInit) {
            // 1. 得到dataBase。
            // 数据库应该在BaseDaoFactory.getDataHelper() 方法中已经打开，
            // 但为了安全起见，这里要做一下非空检查。
            if (!sqLiteDatabase.isOpen()) {
                // 如果数据库没有打开，则直接返回false，表示没有初始化成功。
                return false;
            }
            this.dataBase = sqLiteDatabase;
            this.entityClazz = entityClazz;

            // 2. 通过注解得到要操作的表名。
            DbTable dbTableAnnotation = entityClazz.getAnnotation(DbTable.class);
            if (dbTableAnnotation == null) {
                // 如果在entity 类上没有使用 @DbTable 注解，这里就会为null
                this.tableName = entityClazz.getClass().getSimpleName();
            } else {
                this.tableName = dbTableAnnotation.value();
            }

            // 3. 创建表
            String createTableSql = createTable();
            if (!TextUtils.isEmpty(createTableSql)) {
                dataBase.execSQL(createTableSql);
            }

            // 4. 建立表字段和成员变量之间的映射关系
            initCacheMap();
            this.isInit = true;
        }
        return this.isInit;
    }

    private void initCacheMap() {
        cacheMap = new HashMap<>();

        // 先查询一遍表（无论表中有无数据），以得到列名。
        String sql = "select * from " + this.tableName + " limit 1,0";
        // 表的列名数组
        String[] columnNames = null;
        Cursor cursor = null;
        try {
            cursor = dataBase.rawQuery(sql, null);
            columnNames = cursor.getColumnNames();
        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        if (columnNames == null) {
            return;
        }

        // 拿到Field数组
        Field[] fields = entityClazz.getFields();
        for (Field field : fields) {
            field.setAccessible(true);
        }
        // 开始找对应关系
        for (String columnName : columnNames) {
            Field columnField = null;

            // 通过fieldName 和columnName 是否相等，来查找相对应的field，
            for (Field field : fields) {
                // 先需要获得fieldName
                String fieldName = null;
                DbFeild dbFieldAnnotation = field.getAnnotation(DbFeild.class);
                if (dbFieldAnnotation != null) {
                    // 如果有注解，就用注解的值作为fieldName
                    fieldName = dbFieldAnnotation.value();
                } else {
                    // 如果没有使用注解，就直接使用fieldName
                    fieldName = field.getName();
                }

                // 如果表的列名 等于 成员变量的注解名字
                if (columnName.equals(fieldName)) {
                    columnField = field;
                    break;
                }
            }

            // 找到了对应关系
            if (columnField != null) {
                cacheMap.put(columnName, columnField);
            }
        }
    }

    @Override
    public Long insert(T entity) {
        Map<String, String> map = getValues(entity);
        ContentValues contentValues = getContentValues(map);
        long result = dataBase.insert(tableName, null, contentValues);
        return result;
    }

    private ContentValues getContentValues(Map<String, String> map) {
        ContentValues contentValues = new ContentValues();
        Iterator<String> iterator = map.keySet().iterator();
        while (iterator.hasNext()) {
            String key = iterator.next();
            String value = map.get(key);
            if (value != null) {
                contentValues.put(key,value);
            }
        }
        return contentValues;
    }

    private Map<String, String> getValues(T entity) {
        HashMap<String, String> result = new HashMap<>();

        // 遍历 映射map中存放的 field，取出
        Iterator<Field> fieldsIterator = cacheMap.values().iterator();
        while (fieldsIterator.hasNext()) {
            Field colmunFiled = fieldsIterator.next();
            String cacheKey;
            String cacheValue;

            // ？感觉这里的cacheKey 可以直接使用cacheMap 的key呀。
            // 为什么还要再从field 中取？
            DbFeild filedAnnotation = colmunFiled.getAnnotation(DbFeild.class);
            if (filedAnnotation != null) {
                cacheKey = filedAnnotation.value();
            }else {
                cacheKey = colmunFiled.getName();
            }

            Object o = null;
            try {
                o = colmunFiled.get(entity);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }

            if (o == null) {
                continue;
            }

            cacheValue = o.toString();

            result.put(cacheKey, cacheValue);
        }
        return result;
    }

    @Override
    public Long update(T entity, T where) {
        return null;
    }
    /**
     * 创建表，交由各子类具体实现
     */
    protected abstract String createTable();
}
