package com.ANT.MiddleWare.PartyPlayerActivity;


import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;


import com.ANT.MiddleWare.Celluar.GroupCell.GroupCell;
import com.ANT.MiddleWare.PartyPlayerActivity.bean.DashApplication;
import com.ANT.MiddleWare.PartyPlayerActivity.bean.Message;
import com.ANT.MiddleWare.PartyPlayerActivity.util.Method;
import com.android.volley.Response;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.ANT.MiddleWare.PartyPlayerActivity.ViewVideoActivity.getMsg;
import static com.ANT.MiddleWare.PartyPlayerActivity.ViewVideoActivity.sendMsg;
import static com.ANT.MiddleWare.PartyPlayerActivity.ViewVideoActivity.userName;


/**
 * A simple {@link Fragment} subclass.
 */
public class ChatFragment extends Fragment {
    private static final String TAG = ChatFragment.class.getSimpleName();
    private View view;
    private ListView listView;
    private EditText editText;
    private ImageButton chat_btn;
    private List<Msg> msgList=new CopyOnWriteArrayList<>();
    private MsgAdapter msgAdapter;
    private Bitmap bitmap;
    private  ViewVideoActivity viewActivity;
    private ExecutorService threadPool = Executors.newFixedThreadPool(1);


    public ChatFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        view = inflater.inflate(R.layout.fragment_chat, container, false);
        listView=(ListView)view.findViewById(R.id.list_main);
        editText=(EditText)view.findViewById(R.id.edit_main);
        chat_btn=(ImageButton)view.findViewById(R.id.button_main);
        viewActivity =(ViewVideoActivity) getActivity();
        onTalkState();
        return view;
    }

    public void onTalkState(){
        chat_btn.setBackgroundResource(R.drawable.send);
        editText.setHint("  聊天列表");
        MsgInit();
        chat_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String content = editText.getText().toString();
                if (!"".equals(content)) {
                    Msg msg = new Msg(content, Msg.TYP_SEND, userName, System.currentTimeMillis());
                    msgList.add(msg);
                    msgAdapter.notifyDataSetChanged();
                    listView.setSelection(msgList.size());
                    editText.setText("");
                    Message message= new Message();
                    message.setMessage(content);
                    message.setName(userName);
                    sendMsg(message);
                    //开启发送线程
                }
            }
        });

        msgAdapter = new MsgAdapter(getActivity(), R.layout.chat_item_message, msgList);
        listView.setAdapter(msgAdapter);
        startReceiveThread();
    }
    public void startReceiveThread(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                while(true) {
                            Message message = getMsg();
                            if(message!=null) {
                                Log.d("ChatFragment", "receive message:" + String.valueOf(message.getMessage()));
                                String msgR = message.getMessage();
                                if (msgR.equals(ViewVideoActivity.SYSTEM_MESSAGE_SHARE_LOCAL)) {
                                    android.os.Message msg = new android.os.Message();
                                    msg.what = 1;
                                    viewActivity.getmHandler().sendMessage(msg);
                                    threadPool.execute(this);
                                    return;
                                } else if (msgR.startsWith(ViewVideoActivity.SYSTEM_MESSAGE_SHARE_NETWORK)) {
                                    android.os.Message msg = new android.os.Message();
                                    msg.what = 2;
                                    String[] infos = msgR.split("~");
                                    msg.obj = infos[1];
                                    GroupCell.groupSession = infos[2];
                                    viewActivity.getmHandler().sendMessage(msg);
                                    threadPool.execute(this);
                                    return;
                                } else if (msgR.startsWith(ViewVideoActivity.SYSTEM_MESSAGE)) {
                                    android.os.Message msg = new android.os.Message();
                                    msg.what = 4;
                                    msg.obj = msgR.split("~")[1];
                                    viewActivity.getmHandler().sendMessage(msg);
                                    threadPool.execute(this);
                                    return;
                                }
                                Log.d(TAG, "receive and refresh list");
                                Msg receivedmsg = new Msg(msgR, Msg.TYP_RECIEVED, message.getName(), System.currentTimeMillis());
//                            Msg receivedmsg = new Msg("hello", Msg.TYP_RECIEVED, "bbb", System.currentTimeMillis());
                                msgList.add(receivedmsg);
                                Log.d(TAG,receivedmsg.getContent());
                                android.os.Message passmsg=new android.os.Message();
                                passmsg.what=1;
                                handler.sendMessage(passmsg);
                            }

            }
        }
        }).start();}

    Handler handler = new Handler() {
    public void handleMessage(android.os.Message msg){
          super.handleMessage(msg);
            if(msg.what==1){
                msgAdapter.notifyDataSetChanged();
                listView.setSelection(msgList.size());
                Log.d(TAG,String.valueOf(msgList.size())+":"
                +msgList.get(msgList.size()-1).getContent());
            }
        }
    };

    public void MsgInit(){
        msgList.clear();
    }
}
