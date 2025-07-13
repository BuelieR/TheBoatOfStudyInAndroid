package com.boatofstudy.knowledge;

import android.content.Context;
import android.content.ClipboardManager;
import android.content.ClipData;
import android.content.DialogInterface;
import android.widget.TabHost;
import android.widget.ListView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.view.View;
import android.os.Bundle;
import android.widget.TabHost.TabSpec;
import android.widget.ScrollView;
import android.widget.Toast;

/**
 * 前端UI控制器，处理所有用户界面交互
 */
public class FrontendActivity {
    private final Context context;
    private TabHost tabHost;
    private ListView fileListView;
    private final LinearLayout welcomeLayout;
    private final LinearLayout fileBrowserLayout;
    private final TextView currentPathView;
    private ArrayAdapter<String> fileAdapter;
    
    public FrontendActivity(Context context) {
        this.context = context;
    }

    // 初始化UI组件
    
    public void onCreate(Bundle savedInstanceState) {
        // 初始化UI组件
        tabHost = (TabHost) ((Activity)context).findViewById(android.R.id.tabhost);
        tabHost.setup();
        
        TabSpec tab1 = tabHost.newTabSpec("tab1");
        tab1.setIndicator("主页");
        tab1.setContent(R.id.tab1);
        tabHost.addTab(tab1);
        
        TabSpec tab2 = tabHost.newTabSpec("tab2");
        tab2.setIndicator("个人");
        tab2.setContent(R.id.tab2);
        tabHost.addTab(tab2);
        
        welcomeLayout = (LinearLayout) ((Activity)context).findViewById(R.id.welcome_layout);
        fileBrowserLayout = (LinearLayout) ((Activity)context).findViewById(R.id.file_browser_layout);
        currentPathView = (TextView) ((Activity)context).findViewById(R.id.current_path);
        fileListView = (ListView) ((Activity)context).findViewById(R.id.file_list_view);
    }
    
    public void setFileAdapter(ArrayAdapter<String> adapter) {
        this.fileAdapter = adapter;
        if (fileListView != null) {
            fileListView.setAdapter(fileAdapter);
        }
    }
    
    public void updateFileList(List<String> files) {
        if (fileAdapter != null) {
            fileAdapter.clear();
            fileAdapter.addAll(files);
            fileAdapter.notifyDataSetChanged();
        }
    }
    
    public void showResultDialog(String filePath, String content) {
        // 创建更详细的对话框
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(context);
        builder.setTitle("解析结果: " + filePath);
        
        // 使用ScrollView容纳长内容
        ScrollView scrollView = new ScrollView(context);
        TextView textView = new TextView(context);
        textView.setText(content);
        textView.setPadding(16, 16, 16, 16);
        scrollView.addView(textView);
        
        builder.setView(scrollView)
               .setPositiveButton("确定", null)
               .setNeutralButton("复制", new DialogInterface.OnClickListener() {
                   @Override
                   public void onClick(DialogInterface dialog, int which) {
                       ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                       ClipData clip = ClipData.newPlainText("解析结果", content);
                       clipboard.setPrimaryClip(clip);
                       Toast.makeText(context, "已复制到剪贴板", Toast.LENGTH_SHORT).show();
                   }
               })
               .show();
    }
    
    public void showErrorDialog(String title, String message) {
        // 区分错误和提示样式
        int icon = title.equals("解析完成") ? android.R.drawable.ic_dialog_info : android.R.drawable.ic_dialog_alert;
        
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(context);
        builder.setTitle(title)
               .setMessage(message)
               .setIcon(icon)
               .setPositiveButton("确定", null)
               .show();
    }
}
