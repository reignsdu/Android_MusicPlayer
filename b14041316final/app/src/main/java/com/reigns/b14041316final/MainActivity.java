package com.reigns.b14041316final;
/*
*create by reigns
*2016.12.22
 */
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.SimpleAdapter;
import android.R.layout;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.SeekBar.OnSeekBarChangeListener;


public class MainActivity extends Activity {
    private TextView nameTextView;
    private SeekBar seekBar;
    private ListView listView;
    private List<Map<String, String>> data;
    private int current;
    private MediaPlayer player;
    private Handler handler = new Handler();
    private Button ppButton;
    private boolean isPause;
    private boolean isStartTrackingTouch;
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.listview_music);
        nameTextView = (TextView) findViewById(R.id.name);
        seekBar = (SeekBar) findViewById(R.id.seekBar);
        listView = (ListView) findViewById(R.id.list);
        ppButton = (Button) findViewById(R.id.pp);
        //创建一个音乐播放器
        player = new MediaPlayer();
        //显示音乐播放列表
        generateListView();
        //进度条监听器
        seekBar.setOnSeekBarChangeListener(new MySeekBarListener());
        //播放器监听器
        player.setOnCompletionListener(new MyPlayerListener());
        //意图过滤器
        IntentFilter filter = new IntentFilter();
        //播出电话暂停音乐播放
        filter.addAction("android.intent.action.NEW_OUTGOING_CALL");
        registerReceiver(new PhoneListener(), filter);
        //创建一个电话服务
        TelephonyManager manager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        //监听电话状态，接电话时停止播放
        manager.listen(new MyPhoneStateListener(), PhoneStateListener.LISTEN_CALL_STATE);
    }
    /*
     * 监听电话状态
     */
    private final class MyPhoneStateListener extends PhoneStateListener {
        public void onCallStateChanged(int state, String incomingNumber) {
            pause();
        }
    }
    /*
     * 播放器监听器
     */
    private final class MyPlayerListener implements OnCompletionListener {
        //歌曲播放完后自动播放下一首歌曲
        public void onCompletion(MediaPlayer mp) {
            next();
        }
    }
    /*
     * 下一首按钮
     */
    public void next(View view) {
        next();
    }
    /*
     * 前一首按钮
     */
    public void previous(View view) {
        previous();
    }
    /*
     * 播放前一首歌
     */
    private void previous() {
        current = current - 1 < 0 ? data.size() - 1 : current - 1;
        play();
    }
    /*
     * 播放下一首歌
     */
    private void next() {
        current = (current + 1) % data.size();
        play();
    }
    /*
     * 进度条监听器
     */
    private final class MySeekBarListener implements OnSeekBarChangeListener {
        //移动触发
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        }
        //起始触发
        public void onStartTrackingTouch(SeekBar seekBar) {
            isStartTrackingTouch = true;
        }
        //结束触发
        public void onStopTrackingTouch(SeekBar seekBar) {
            player.seekTo(seekBar.getProgress());
            isStartTrackingTouch = false;
        }
    }
    /*
     * 显示音乐播放列表
     */
    private void generateListView() {
        List<File> list = new ArrayList<File>();
        //获取sdcard中的所有歌曲
        findAll(Environment.getExternalStorageDirectory(), list);
        //播放列表进行排序，字符顺序
        Collections.sort(list);
        data = new ArrayList<Map<String, String>>();
        for (File file : list) {
            Map<String, String> map = new HashMap<String, String>();
            map.put("name", file.getName());
            map.put("path", file.getAbsolutePath());
            data.add(map);
        }
        SimpleAdapter adapter = new SimpleAdapter(this, data, R.layout.item, new String[] { "name" }, new int[] { R.id.mName });
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new MyItemListener());
    }
    private final class MyItemListener implements OnItemClickListener {
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            current = position;
            play();
        }
    }
    private void play() {
        try {
            //重播
            player.reset();
            //获取歌曲路径
            player.setDataSource(data.get(current).get("path"));
            //缓冲
            player.prepare();
            //开始播放
            player.start();
            //显示歌名
            nameTextView.setText(data.get(current).get("name"));
            //设置进度条长度
            seekBar.setMax(player.getDuration());
            //播放按钮样式
            ppButton.setText("||");
            //发送一个Runnable, handler收到之后就会执行run()方法
            handler.post(new Runnable() {
                public void run() {
                    // 更新进度条状态
                    if (!isStartTrackingTouch)
                        seekBar.setProgress(player.getCurrentPosition());
                    // 1秒之后再次发送
                    handler.postDelayed(this, 1000);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    /**
     * 查找文件路径中所有mp3文件
     * @param file 要找的目录
     * @param list 用来装文件的List
     */
    private void findAll(File file, List<File> list) {
        File[] subFiles = file.listFiles();
        if (subFiles != null)
            for (File subFile : subFiles) {
                if (subFile.isFile() && subFile.getName().endsWith(".mp3"))
                    list.add(subFile);
                else if (subFile.isDirectory())//如果是目录
                    findAll(subFile, list); //递归
            }
    }
    /*
     * 暂停/播放按钮
     */
    public void pp(View view) {
        //默认从第一首歌曲开始播放
        if (!player.isPlaying() && !isPause) {
            play();
            return;
        }
        Button button = (Button) view;
        //暂停/播放按钮
        if ("||".equals(button.getText())) {
            pause();
            button.setText("▶");
        } else {
            resume();
            button.setText("||");
        }
    }
    /*
     * 开始操作
     */
    private void resume() {
        if (isPause) {
            player.start();
            isPause = false;
        }
    }
    /*
     * 暂停操作
     */
    private void pause() {
        if (player != null && player.isPlaying()) {
            player.pause();
            isPause = true;
        }
    }
    /*
     * 收到广播时暂停
     */
    private final class PhoneListener extends BroadcastReceiver {
        public void onReceive(Context context, Intent intent) {
            pause();
        }
    }
    /*
     * 恢复播放
     * @see android.app.Activity#onResume()
     */
    protected void onResume() {
        super.onResume();
        resume();
    }
}