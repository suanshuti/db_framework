package holoenglish.com.db;

import android.database.sqlite.SQLiteDatabase;
import android.os.Environment;

public class BaseDaoFactory {
    /** 此工厂类的实例是单例*/
    private static BaseDaoFactory instance = new BaseDaoFactory();

    private String sqliteDatabasePath;
    private SQLiteDatabase sqLiteDatabase;

    public BaseDaoFactory(){
        this.sqliteDatabasePath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/user.db";
        openDatabase();
    }

    public synchronized <T extends BaseDao<M>,M> T getDataHelper(Class<T> clazz, Class<M>entityClazz) {
        BaseDao baseDao = null;
        try {
            baseDao = clazz.newInstance();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        }
        if (baseDao != null) {
            baseDao.init(entityClazz,sqLiteDatabase);
        }

        return (T) baseDao;
    }

    private void openDatabase() {
        this.sqLiteDatabase = SQLiteDatabase.openOrCreateDatabase(this.sqliteDatabasePath, null);
    }

    public static BaseDaoFactory getInstance (){
        return instance;
    }


}
