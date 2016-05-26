package com.zhaobo.expressquerey;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    private EditText editText;
    private Button button;
    private TextView textView,textView2;

    private static final int START = 100;
    private static final int END = 101;
    private static final int SUCCESS = 102;
    private static final int FAIL = 103;

    private static final String URL = "http://api.kuaidi100.com/api";
    private static final String APPKEY = "29833628d495d7a5";

    private static final int CONNECT_TIMEOUT = 8*1000;
    private static final int READ_TIMEOUT = 8*1000;

    private static String number;//全局变量“快递单号”

    //快递返回的信息集
    private static String time;//每条跟踪信息的时间
    private static String context;//每条跟综信息的描述

    private static StringBuilder contentBuilder;

    private static int state;/*快递单当前的状态 ：　
									0：在途，即货物处于运输过程中；
									1：揽件，货物已由快递公司揽收并且产生了第一条跟踪信息；
									2：疑难，货物寄送过程出了问题；
									3：签收，收件人已签收；
									4：退签，即货物由于用户拒签、超区等原因退回，而且发件人已经签收；
									5：派件，即快递正在进行同城派件；
									6：退回，货物正处于退回发件人的途中；*/
    //定义state的各种状态
    private static final int ZERO = 0;
    private static final int ONE = 1;
    private static final int TWO = 2;
    private static final int THREE = 3;
    private static final int FOUR = 4;
    private static final int FIVE = 5;
    private static final int SIX = 6;


    private static final String TAG = "MainActivity";



    Handler handler = new Handler(){
        public void handleMessage(Message msg){
            switch (msg.what){
                case START:
                    Toast.makeText(MainActivity.this,"正在查询中,请稍后……",Toast.LENGTH_SHORT).show();
                    break;
                case END:
                    String mTime = contentBuilder.toString();
                    textView.setText(mTime);
                    break;
                case SUCCESS:
                    Toast.makeText(MainActivity.this,"查询成功^_^",Toast.LENGTH_SHORT).show();
                    break;
                case FAIL:
                    Toast.makeText(MainActivity.this,"未知错误，查询失败。",Toast.LENGTH_SHORT).show();
                    break;

                default:
                    break;
            }
        }
    };

    public void onCreate(Bundle saveInstanceState){
        super.onCreate(saveInstanceState);
        setContentView(R.layout.activity_main);
        initView();
    }

    //初始化各个控件
    public void initView(){
        contentBuilder = new StringBuilder();
        editText = (EditText)findViewById(R.id.editText);
        button = (Button)findViewById(R.id.button);
        textView = (TextView)findViewById(R.id.textView);
        button.setOnClickListener(this);
    }

    //设置Button的点击事件，在监听器中设置workerThread，并跳转至handleMessage更新UI
    public void onClick(View v){
        number = editText.getText().toString();
        switch (v.getId()){
            case R.id.button:
                if (!TextUtils.isEmpty(number)) {
                    new Thread(){
                        public void run(){
                            handler.obtainMessage(START).sendToTarget();
                            parseJsonData();
							handler.obtainMessage(END).sendToTarget();
                        }
                    }.start();
                } else {
                    Toast.makeText(this,"快递单号不能为空！",Toast.LENGTH_SHORT).show();
                }
                break;
            default:
                break;

        }
    }


    //通过JSONObject解析获取到得json数据
    public void parseJsonData(){
        String jsonData = getJsonData();
        JSONObject jsonObject = null;
        try {
            jsonObject = new JSONObject(jsonData);
            String message = jsonObject.getString("message");
            if ("ok".equals(message)) {
                handler.obtainMessage(SUCCESS).sendToTarget();
                String array = jsonObject.getString("data");

                JSONArray jsonArray = new JSONArray(array);
                for (int i = 0;i <= jsonArray.length();i++ ) {
                    JSONObject ob = jsonArray.getJSONObject(i);
                    time = ob.getString("time");
                    context = ob.getString("context");
                    contentBuilder.append("【" + time + "】" + context + "\n" + "------------------------------------------------------------" +"\n");
                    Log.i(TAG, "time" + time);
                    Log.i(TAG, "context" + context);
                }
            } else {
                handler.obtainMessage(FAIL).sendToTarget();
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    //通过HttpURLConnectin获取网络上的json原始数据（无BUG）
    public String getJsonData(){
        String response = "";
        HttpURLConnection conn = null;
        try {
            String httpUrl = getURL(editText.getText().toString());
            URL url = new URL(httpUrl);
            conn = (HttpURLConnection)url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(CONNECT_TIMEOUT);
            conn.setReadTimeout(READ_TIMEOUT);
            conn.connect();
            InputStream is = conn.getInputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            String line;
            while ((line = br.readLine()) != null){
                response += line;
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            conn.disconnect();
        }

        return response;
    }

    //获取完整的URL请求地址(天天快递，返回多行json数据，按时间由新到旧排列)（无BUG）
    public String getURL(String expressNumber){
        String url = URL + "?id=" + APPKEY + "&com=" + "tiantian" + "&nu=" + expressNumber + "&show=" + 0 + "&muti=" + 1 + "&order=" + "desc";
        return url;
    }
}
