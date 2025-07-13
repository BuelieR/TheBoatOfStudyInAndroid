package com.boatofstudy.knowledge;

import android.content.Context;
import android.widget.TabHost;
import android.widget.ListView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.view.View;
import android.os.Bundle;
import android.widget.TabHost.TabSpec;

public class FrontendActivity {
    private Context context;
    private TabHost tabHost;
    private ListView fileListView;
    private LinearLayout welcomeLayout;
    private LinearLayout fileBrowserLayout;
    private TextView currentPathView;
    private ArrayAdapter<String> fileAdapter;
    
    public FrontendActivity(Context context) {
        this.context = context;
    }
    
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
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(context);
        builder.setTitle("文件内容: " + filePath)
               .setMessage(content)
               .setPositiveButton("确定", null)
               .show();
    }
    
    public void showErrorDialog(String title, String message) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(context);
        builder.setTitle(title)
               .setMessage(message)
               .setPositiveButton("确定", null)
               .show();
    }
}
