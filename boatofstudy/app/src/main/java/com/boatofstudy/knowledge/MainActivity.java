package com.boatofstudy.knowledge;

import android.app.Activity;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import java.util.List;

public class MainActivity extends Activity implements TmltParser.ResultCallback {
    private FrontendActivity frontend;
    private BackendController backend;
    private TmltParser tmltParser;
    private ArrayAdapter<String> fileAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        // 初始化三个组件
        frontend = new FrontendActivity(this);
        backend = new BackendController(this);
        tmltParser = new TmltParser(this, this);
        
        // 设置前端UI
        frontend.onCreate(savedInstanceState);
        
        // 初始化文件适配器
        fileAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, backend.getFileList());
        frontend.setFileAdapter(fileAdapter);
        
        // 初始化文件浏览器
        backend.listFiles(backend.getRootDirectoryPath());
        frontend.updateFileList(backend.getFileList());
    }
    
    @Override
    public void onResult(String filePath, String content) {
        frontend.showResultDialog(filePath, content);
    }
    
    @Override
    public void onError(String title, String message) {
        frontend.showErrorDialog(title, message);
    }
    
    @Override
    public void onBackPressed() {
        if (backend.handleBackDirectory()) {
            frontend.updateFileList(backend.getFileList());
        } else {
            super.onBackPressed();
        }
    }
    
    public void processTmltFile(String filePath) {
        tmltParser.processTmltFile(filePath);
    }

    private void setupFileLongClick() {
        fileListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                String path = backend.getPathList().get(position);
                showFileOperationDialog(path);
                return true;
            }
        });
    }

    private void showFileOperationDialog(final String path) {
        final CharSequence[] items = {"删除", "重命名"};
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("文件操作")
               .setItems(items, new DialogInterface.OnClickListener() {
                   public void onClick(DialogInterface dialog, int which) {
                       if (which == 0) {
                           deleteFile(path);
                       } else {
                           showRenameDialog(path);
                       }
                   }
               });
        builder.show();
    }

    private void deleteFile(String path) {
        if (backend.deleteFile(path)) {
            frontend.updateFileList(backend.getFileList());
        } else {
            frontend.showErrorDialog("错误", "删除文件失败");
        }
    }

    private void showRenameDialog(final String oldPath) {
        final android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("重命名文件");

        final EditText input = new EditText(this);
        input.setText(new File(oldPath).getName());
        builder.setView(input);

        builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String newName = input.getText().toString();
                if (backend.renameFile(oldPath, newName)) {
                    frontend.updateFileList(backend.getFileList());
                } else {
                    frontend.showErrorDialog("错误", "重命名文件失败");
                }
            }
        });
        builder.setNegativeButton("取消", null);
        builder.show();
    }
}
