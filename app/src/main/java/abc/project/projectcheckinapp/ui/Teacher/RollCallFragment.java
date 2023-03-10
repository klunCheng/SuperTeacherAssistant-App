package abc.project.projectcheckinapp.ui.Teacher;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import abc.project.projectcheckinapp.databinding.FragmentRollCallBinding;
import abc.project.projectcheckinapp.rawData.AdapterNoRcStu;
import abc.project.projectcheckinapp.rawData.ClickListener;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link RollCallFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class RollCallFragment extends Fragment {
    static int flag = 0; // 點名開關
    FragmentRollCallBinding binding;
    NavController navController;
    SharedPreferences preferences;
    SharedPreferences.Editor contextEditor;
    ExecutorService executor;
    SQLiteDatabase db;
    RecyclerView recyclerView;
    ClickListener clickListener;
    AdapterNoRcStu adapter;
    AlertDialog.Builder builder;
    AlertDialog dialog;
    public RollCallFragment() {
        // Required empty public constructor
    }

    Handler OpenResultHandler = new Handler(Looper.getMainLooper()){
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            Bundle bundle = msg.getData();
            if (bundle.getInt("status")==12){
                binding.btnTec2Start.setText("結束點名");
                flag = 1;
            }
        }
    };
    Handler CloseResultHandler = new Handler(Looper.getMainLooper()){
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            Bundle bundle = msg.getData();
            binding.btnTec2Start.setText("已點名");
            binding.btnTec2Start.setEnabled(false);
            flag = 0;

            JSONArray stuNoRCInfos;
            preferences = getActivity().getSharedPreferences("userInfo",MODE_PRIVATE);
            int cid = preferences.getInt("cid",0);
            db = getActivity().openOrCreateDatabase("allList",MODE_PRIVATE,null); // 將學生資訊存進裝置的DB
            // 每個跟RecyclerView有關的資料都存進裝置名為 allList 的DB中，以table名稱來區別每個list
            db.execSQL("drop table if exists no_rc_stu_"+cid+";");
            db.execSQL("create table no_rc_stu_"+cid+"(sid integer,name text, depart text, stuId text);");
            try {
                stuNoRCInfos =new JSONArray( bundle.getString("list") );
                for (int i = 0; i<stuNoRCInfos.length();i++){
                    JSONObject stuInfo = stuNoRCInfos.getJSONObject(i);
                    db.execSQL("insert into no_rc_stu_"+cid+" values (?,?,?,?);",
                            new Object[] { stuInfo.getInt("sid"),
                                    stuInfo.getString("學生姓名"),
                                    stuInfo.getString("科系"),
                                    stuInfo.getString("學號")}); }
                db = getActivity().openOrCreateDatabase("allList",MODE_PRIVATE,null);
                adapter = new AdapterNoRcStu(db,clickListener,cid);
                recyclerView.setAdapter(adapter);
                recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        }
    };
    public static RollCallFragment newInstance(String param1, String param2) {
        RollCallFragment fragment = new RollCallFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding = FragmentRollCallBinding.inflate(inflater, container, false);
        preferences = getActivity().getSharedPreferences("userInfo", Context.MODE_PRIVATE);
        executor = Executors.newSingleThreadExecutor();
        int cid = preferences.getInt("cid",0);
        if (cid == 0){
            Toast.makeText(getActivity(), "請重新進入教室", Toast.LENGTH_SHORT).show();
        }
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd");
        String date = formatter.format(new Date());
        MediaType mediaType = MediaType.parse("application/json");
        JSONObject packet = new JSONObject();
        JSONObject data = new JSONObject();
        try {
            packet.put("type", 1);
            packet.put("status", 11);
            data.put("cid", cid);
            data.put("date", date);
            packet.put("data", data);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        RequestBody body = RequestBody.create(packet.toString(), mediaType);
        binding.btnTec2Start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (flag == 0) {
                    Request request = new Request.Builder()
                            .url("http://20.2.232.79:8864/api/rollcall/teacher/open")
                            .post(body)
                            .build();
                    SimpleAPIWorker apiCaller = new SimpleAPIWorker(request);
                    executor.execute(apiCaller);
                    if (flag == 0){
                        builder = new AlertDialog.Builder(getActivity());
                        builder.setMessage("今日已點名，是否刪除紀錄後重新點名?");
                        builder.setPositiveButton("是", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Request request = new Request.Builder()
                                        .url("http://20.2.232.79:8864/api/rollcall/teacher/open/again")
                                        .post(body)
                                        .build();
                                SimpleAPIWorker apiCaller = new SimpleAPIWorker(request);
                                executor.execute(apiCaller);
                            }
                        });
                        builder.setNegativeButton("否", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {  flag = 0;  }
                        });
                        dialog = builder.create();
                        dialog.show();
                    }
                } else {
                    Log.e("app","app 送出資訊:" +body);
                    Request request = new Request.Builder()
                            .url("http://20.2.232.79:8864/api/rollcall/teacher/close")
                            .post(body)
                            .build();
                    SimpleAPIWorker2 apiCaller2 = new SimpleAPIWorker2(request);
                    executor.execute(apiCaller2);
                    recyclerView = binding.RecyclerStuAbsence;

                }
            }
        });
        clickListener = new ClickListener() {
            @Override
            public void onClickForAllStuList(int position, int sid, String stuname, String studepart, String stuid) {  }
            @Override
            public void onClickForClassroom(int position, int cid,String classname) {  }
            @Override
            public void onClickForNoRcStuList(int position, int sid) {
                JSONObject data1 = new JSONObject();
                try {
                    data1.put("cid", cid);
                    data1.put("sid",sid);
                    data1.put("date", date);
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
                Log.w("api","onClickForNoRcStuList  送出資訊:"+data1);
                Request request = new Request.Builder()
                        .url("http://20.2.232.79:8864/api/rollcall/manual/call")
                        .post(RequestBody.create(data1.toString(), mediaType))
                        .build();
                SimpleAPIWorker3 apiCaller = new SimpleAPIWorker3(request);
                executor.execute(apiCaller);
            }
        };
        return binding.getRoot();
    }

    class SimpleAPIWorker implements Runnable {
        OkHttpClient client;
        Request request;
        public SimpleAPIWorker(Request request) {
            this.request = request;
            client = new OkHttpClient();
        }
        @Override
        public void run() {
            try {
                Response response = client.newCall(request).execute();
                JSONObject result = new JSONObject(response.body().string());
                Message m = OpenResultHandler.obtainMessage();
                Bundle bundle = new Bundle();
                bundle.putInt("status", result.getInt("status"));
                m.setData(bundle);
                OpenResultHandler.sendMessage(m);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
    class SimpleAPIWorker2 implements Runnable {
        OkHttpClient client;
        Request request;
        public SimpleAPIWorker2(Request request) {
            this.request = request;
            client = new OkHttpClient();
        }
        @Override
        public void run() {
            try {
                Response response = client.newCall(request).execute();
                String apidata = response.body().string();
                Log.w("api","simple2 api回應:"+apidata);
                JSONObject result = new JSONObject(apidata);
                Message m = CloseResultHandler.obtainMessage();
                Bundle bundle = new Bundle();
                if ( result.getInt("type")==2){
                    String stuNoRCInfo = result.getJSONArray("list").toString();
                    bundle.putString("list",stuNoRCInfo);
                    m.setData(bundle);
                    CloseResultHandler.sendMessage(m);
                } else {
                    Log.e("error","api回應有問題");
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
    class SimpleAPIWorker3 implements Runnable {
        OkHttpClient client;
        Request request;
        public SimpleAPIWorker3(Request request) {
            this.request = request;
            client = new OkHttpClient();
        }
        @Override
        public void run() {
            try {
                client.newCall(request).execute();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        navController = Navigation.findNavController(view);
    }
}