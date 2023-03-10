package abc.project.projectcheckinapp.ui.Student;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import abc.project.projectcheckinapp.R;
import abc.project.projectcheckinapp.databinding.FragmentReviseStdDataBinding;
import abc.project.projectcheckinapp.databinding.FragmentStdRollCallBinding;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class StdRollCallFragment extends Fragment {

    FragmentStdRollCallBinding binding;
    NavController navController;
    String classname;
    SimpleDateFormat dateFormat ;
    Date date ;
    String currentDate ;
    int cid, sid;
    ExecutorService executor;
    SharedPreferences preferences;
    SharedPreferences.Editor editor;


    Handler StdRollCallHandler = new Handler(Looper.getMainLooper()){
        @Override
        public void handleMessage(@NonNull Message msg) {
            //AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            super.handleMessage(msg);
            Bundle bundle2 = msg.getData();

                //關閉按鈕
                binding.btnStu1Checkin.setEnabled(false);


        }
    };

    Handler EnterCheckRollCallHandler = new Handler(Looper.getMainLooper()){
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            Bundle bundle2 = msg.getData();
            Log.w("EnterCheckRollCallHandler","EnterCheckRollCallHandler status: "+bundle2.getInt("status"));
            if(bundle2.getInt("status")==12) {
                binding.btnStu1Checkin.setEnabled(true);
                binding.btnStu1Checkin.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        //取得簽到時間
                        dateFormat = new SimpleDateFormat("yyyyMMdd");
                        date = new Date(System.currentTimeMillis());
                        currentDate = dateFormat.format(date);
                        Log.e("date",currentDate);
                        binding.btnStu1Checkin.setText(currentDate+"已簽到");
                        //時間存至共用sharedPreferences
                        preferences.edit().putString("data",currentDate).apply();

                        //時間存至DB
                        JSONObject packet = new JSONObject();
                        JSONObject data = new JSONObject();
                        try {
                            packet.put("type",1);
                            packet.put("status",20);
                            data.put("date",currentDate);
                            data.put("cid",cid);
                            data.put("sid",sid);
                            packet.put("data",data);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                        MediaType mediaType = MediaType.parse("application/json");
                        RequestBody rb = RequestBody.create(packet.toString(),mediaType);
                        Log.e("packet", String.valueOf(packet));
                        Request request = new Request.Builder()
                                .url("http://20.2.232.79:8864/api/project/StdRollCall")
                                .post(rb)
                                .build();
                        simpleAPIworker api = new simpleAPIworker(request);
                        executor = Executors.newSingleThreadExecutor();
                        executor.execute(api);

                    }
                });
            }else if(bundle2.getInt("status")==13){
                binding.btnStu1Checkin.setEnabled(false);
                binding.btnStu1Checkin.setText("已點名");
            }
            else {
                binding.btnStu1Checkin.setEnabled(false);
                binding.btnStu1Checkin.setText("尚未開放點名");
            }
//設定簽到按鈕

        }
    };


    public StdRollCallFragment() {

    }

    public static StdRollCallFragment newInstance(String param1, String param2) {
        StdRollCallFragment fragment = new StdRollCallFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


    }
    //binding.btnStu1Checkin.setEnabled(true);
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentStdRollCallBinding.inflate(inflater, container, false);
        //把課程名稱+cid+sid從sharedPreferences中取出並顯示
        preferences = getActivity().getSharedPreferences("userInfo",Context.MODE_PRIVATE);
        cid = preferences.getInt("cid",0);
        sid = preferences.getInt("sid",0);
        classname = preferences.getString("classname","0");
        binding.txtStdClassname.setText(classname);

        //取得開啟時間
        dateFormat = new SimpleDateFormat("yyyyMMdd");
        date = new Date(System.currentTimeMillis());
        currentDate = dateFormat.format(date);
        executor = Executors.newSingleThreadExecutor();
        JSONObject packet1 = new JSONObject();
        JSONObject data1 = new JSONObject();
        try {
            packet1.put("type",0);
            packet1.put("status",10);
            data1.put("date",currentDate);
            data1.put("cid",cid);
            data1.put("sid",sid);
            packet1.put("data",data1);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        MediaType mtyp = MediaType.parse("application/json");
        RequestBody rb = RequestBody.create(packet1.toString(),mtyp);
        Request request = new Request.Builder()
                .url("http://20.2.232.79:8864/api/project/StdEnterRollCall")
                .post(rb)
                .build();
        EnterCheckAPI enterCheckAPIAPI = new EnterCheckAPI(request);
        executor.execute(enterCheckAPIAPI);




        binding.imageRcllRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                navController.navigate(R.id.action_nav_stdRollCall_to_nav_stdRecord);
            }
        });


        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        navController = Navigation.findNavController(view);
    }

    class simpleAPIworker implements Runnable{

        OkHttpClient client;
        Request request;

        public simpleAPIworker(Request request){
            client = new OkHttpClient();
            this.request = request ;
        }

        @Override
        public void run() {

            try {
                Response response = client.newCall(request).execute();
                String responseBody = response.body().string();
                Log.w("api回應", responseBody);
                Message m = StdRollCallHandler.obtainMessage();
                Bundle bundle = new Bundle();
                bundle.putString("123","開始你的動作");
                m.setData(bundle);
                StdRollCallHandler.sendMessage(m);

            } catch (Exception e) {
                throw new RuntimeException(e);
            }


        }
    }

    class EnterCheckAPI implements Runnable{

        OkHttpClient client;
        Request request;

        public EnterCheckAPI(Request request){
            client = new OkHttpClient();
            this.request = request ;
        }

        @Override
        public void run() {

            try {
                Response response = client.newCall(request).execute();
                String responseBody = response.body().string();
                Log.w("api回應", responseBody);
                JSONObject result = new JSONObject(responseBody);
                Message m = EnterCheckRollCallHandler.obtainMessage();
                Bundle bundle = new Bundle();
                if(result.getInt("status")==12){                         //日期成功寫入資料庫
                    bundle.putInt("status",result.getInt("status"));
                    bundle.putString("mesg","未點名:點名按鈕設定可點選");
                }
                else if(result.getInt("status")==13){
                    bundle.putInt("status",result.getInt("status"));
                    bundle.putString("mesg","已點名/重複點名(btn關掉)");
                }
                else{
                    bundle.putString("mesg","還沒開放點名(btn關掉)");
                }
                m.setData(bundle);
                EnterCheckRollCallHandler.sendMessage(m);

            } catch (Exception e) {
                throw new RuntimeException(e);
            }


        }
    }



}