package abc.project.projectcheckinapp.rawData;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import android.widget.CursorAdapter;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class UniversityArray {
    InputStream inputStream;
    SQLiteDatabase db;

    public UniversityArray() {
    }

    public UniversityArray(InputStream inputStream, SQLiteDatabase db) {
        this.inputStream = inputStream;
        this.db = db;
    }

    public ArrayList<String> FirstTimeGetSpinner(InputStream inputStream, SQLiteDatabase db)  {
        BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder sb =new StringBuilder();
        String line;
        String drop_sql = "drop table if exists University;";
        String create_sql = "create table University (_id integer primary key autoincrement,univ_name text);";
        db.execSQL(drop_sql);
        db.execSQL(create_sql);
        ArrayList< String > universityArray =new ArrayList<>();
        try {
            while ((line = br.readLine()) != null)
                sb.append(line);
            br.close();
            JSONArray jsonArray = new JSONArray(sb.toString());
            for (int i=0; i<jsonArray.length();i++){
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                universityArray.add(jsonObject.getString("學校名稱"));
                db.execSQL("insert into University(univ_name) values (?)",
                        new String[]{ jsonObject.getString("學校名稱") });
            }
            db.close();
        } catch (Exception e){
            Log.w("json", "讀取異常");
        }
        return universityArray;
    }

    public Cursor GetSpinnerFromDB(SQLiteDatabase db){
        String sql = "select _id , univ_name from University;";
        Cursor cursor = db.rawQuery(sql,null);
        //String queryCols =cursor.getString(0);
        return cursor;
    }
    public ArrayList< String > GetSpinnerArrayFromDB(SQLiteDatabase db){
        String sql = "select _id , univ_name from University;";
        Cursor cursor = db.rawQuery(sql,null);
        ArrayList< String > universityArray =new ArrayList<>();
        cursor.moveToFirst();
        String info;
        do{
            info = cursor.getString(1);
            universityArray.add(info);
        }while (cursor.moveToNext());
        //String queryCols =cursor.getString(0);
        return universityArray;
    }
}
