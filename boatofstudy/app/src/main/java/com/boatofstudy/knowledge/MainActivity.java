package com.boatofstudy.knowledge;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ClipboardManager;
import android.content.ClipData;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;
import java.util.List;

/**
 * 主Activity，协调前端UI、后端控制器和模板解析器的工作
 */
public class MainActivity extends Activity implements TmltParser.ResultCallback {
    private final FrontendActivity frontend;
    private final BackendController backend;
    private final TmltParser tmltParser;
    private ArrayAdapter<String> fileAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        // 初始化三个核心组件
        
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == BackendController.DOCUMENT_TREE_REQUEST_CODE && resultCode == RESULT_OK) {
            Uri treeUri = data.getData();
            if (backend.handleDocumentTreeResult(treeUri)) {
                frontend.updateFileList(backend.getFileList());
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == BackendController.STORAGE_ACCESS_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                frontend.updateFileList(backend.getFileList());
            }
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

    private void showNewFolderDialog() {
        final android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("新建文件夹");

        final EditText input = new EditText(this);
        input.setHint("输入文件夹名称");
        builder.setView(input);

        builder.setPositiveButton("创建", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String folderName = input.getText().toString();
                if (!folderName.isEmpty() && backend.createNewDirectory(folderName)) {
                    frontend.updateFileList(backend.getFileList());
                } else {
                    frontend.showErrorDialog("错误", "创建文件夹失败");
                }
            }
        });
        builder.setNegativeButton("取消", null);
        builder.show();
    }

    private void showFileInfoDialog(String path) {
        String info = "路径: " + path + "\n"
                    + "大小: " + backend.getFileSize(path) + " bytes\n"
                    + "修改时间: " + backend.getFileLastModified(path) + "\n"
                    + "类型: " + (new File(path).isDirectory() ? "目录" : "文件");
        
        if (!new File(path).isDirectory()) {
            info += "\n扩展名: " + backend.getFileExtension(path);
        }

        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("文件信息")
               .setMessage(info)
               .setPositiveButton("确定", null)
               .show();
    }
}
