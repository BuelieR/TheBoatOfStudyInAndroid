package com.boatofstudy.knowledge;

import android.*;
import android.app.*;
import android.os.*;
import android.widget.*;
import android.view.*;
import android.content.*;
import java.io.*;
import java.util.*;
import android.util.*;
import android.Manifest;
import android.content.pm.PackageManager;

public class MainActivity extends Activity {

    private LinearLayout welcomeLayout;
    private LinearLayout fileBrowserLayout;
    private TextView currentPathView;
    private ListView fileListView;
    
    private List<String> fileList = new ArrayList<>();
    private List<String> pathList = new ArrayList<>();
    private String currentPath;
    private ArrayAdapter<String> fileAdapter;
    
    private static final int PERMISSION_REQUEST_CODE = 100;
    private int selectedPosition = -1; // 长按选中的位置

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        // 获取设备存储根目录
        currentPath = Environment.getExternalStorageDirectory().getAbsolutePath();
        
        // 获取布局中的UI组件
        welcomeLayout = findViewById(R.id.welcome_layout);
        fileBrowserLayout = findViewById(R.id.file_browser_layout);
        currentPathView = findViewById(R.id.current_path);
        fileListView = findViewById(R.id.file_list_view); // 匹配截图中的ID
        
        // 设置文件列表适配器
        fileAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, fileList);
        fileListView.setAdapter(fileAdapter);
        
        // 设置列表项点击监听器
        fileListView.setOnItemClickListener(new FileItemClickListener());
        
    }

    private Stack<String> pathHistory = new Stack<>(); // 新增路径历史栈

    private class FileItemClickListener implements AdapterView.OnItemClickListener {
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            if (position >= 0 && position < pathList.size()) {
                String selectedPath = pathList.get(position);
                
                if ("..".equals(selectedPath)) {
                    handleBackDirectory();
                } else {
                    File file = new File(selectedPath);
                    if (file.isDirectory()) {
                        enterNewDirectory(selectedPath);
                    } else if (selectedPath.endsWith(".tmlt")) {
                        processTmltFile(selectedPath);
                    } else {
                        Toast.makeText(MainActivity.this, "请选择.tmlt文件", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }
    }
    
    // 处理进入新目录
    private void enterNewDirectory(String newPath) {
        // 保存当前路径到历史栈
        pathHistory.push(currentPath);
        
        // 限制历史栈深度（保留2层）
        if (pathHistory.size() > 2) {
            pathHistory.remove(0); // 移除最旧的历史记录
        }
        
        // 更新当前路径并加载文件
        currentPath = newPath;
        listFiles(currentPath);
        
        // 清理上上层数据
        if (pathHistory.size() >= 2) {
            String upperLevelPath = pathHistory.get(0);
            clearCachedFiles(upperLevelPath);
        }
    }
    
    // 处理返回上级目录
    private void handleBackDirectory() {
        if (!pathHistory.isEmpty()) {
            currentPath = pathHistory.pop();
            listFiles(currentPath);
        } else {
            File currentDir = new File(currentPath);
            String parentPath = currentDir.getParent();
            if (parentPath != null) {
                currentPath = parentPath;
                listFiles(currentPath);
            } else {
                Toast.makeText(this, "已到达最顶层目录", Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    // 清理指定路径的缓存数据
    private void clearCachedFiles(String path) {
        // 实际清理操作可根据需要扩展
        Log.d("CacheClean", "清理上上层目录缓存: " + path);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, 
            String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && 
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initFileBrowser();
            } else {
                Toast.makeText(this, "需要存储权限才能使用文件管理功能", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void initFileBrowser() {
        // 注册长按菜单
        registerForContextMenu(fileListView);
        
        // 设置长按监听器
        fileListView.setOnItemLongClickListener(new FileLongClickListener());
    }

    // 文件长按监听器类[8,9](@ref)
    private class FileLongClickListener implements AdapterView.OnItemLongClickListener {
        @Override
        public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
            selectedPosition = position;
            openContextMenu(fileListView);
            return true;
        }
    }

    // 创建上下文菜单
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        if (v == fileListView && selectedPosition >= 0) {
            String selectedPath = pathList.get(selectedPosition);
            
            // 排除".."目录的特殊处理
            if (!"..".equals(selectedPath)) {
                menu.setHeaderTitle("文件操作");
                menu.add(0, 1, 0, "删除");
                menu.add(0, 2, 1, "重命名");
                menu.add(0, 3, 2, "新建文件夹");
            }
        }
    }

    // 上下文菜单选择处理
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        if (selectedPosition < 0 || selectedPosition >= pathList.size()) {
            return false;
        }
        
        String selectedPath = pathList.get(selectedPosition);
        File selectedFile = new File(selectedPath);
        
        switch (item.getItemId()) {
            case 1: 
                deleteFileOrFolder(selectedFile);
                return true;
                
            case 2: 
                showRenameDialog(selectedFile);
                return true;
                
            case 3: 
                showCreateFolderDialog();
                return true;
                
            default:
                return super.onContextItemSelected(item);
        }
    }

    // 删除文件或文件夹
    private void deleteFileOrFolder(File file) {
        if (file.isDirectory()) {
            // 递归删除文件夹内容[4](@ref)
            File[] files = file.listFiles();
            if (files != null) {
                for (File child : files) {
                    deleteFileOrFolder(child);
                }
            }
        }
        
        if (file.delete()) {
            Toast.makeText(this, "删除成功", Toast.LENGTH_SHORT).show();
            listFiles(currentPath); // 刷新列表
        } else {
            Toast.makeText(this, "删除失败", Toast.LENGTH_SHORT).show();
        }
    }

    // 显示重命名对话框
    private void showRenameDialog(final File file) {
        final EditText input = new EditText(this);
        input.setText(file.getName());
        
        new AlertDialog.Builder(this)
            .setTitle("重命名")
            .setView(input)
            .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    String newName = input.getText().toString().trim();
                    if (!newName.isEmpty()) {
                        renameFile(file, newName);
                    }
                }
            })
            .setNegativeButton("取消", null)
            .show();
    }

    // 文件重命名
    private void renameFile(File file, String newName) {
        File newFile = new File(file.getParent(), newName);
        if (file.renameTo(newFile)) {
            Toast.makeText(this, "重命名成功", Toast.LENGTH_SHORT).show();
            listFiles(currentPath); // 刷新列表
        } else {
            Toast.makeText(this, "重命名失败", Toast.LENGTH_SHORT).show();
        }
    }

    // 显示创建文件夹对话框
    private void showCreateFolderDialog() {
        final EditText input = new EditText(this);
        
        new AlertDialog.Builder(this)
            .setTitle("新建文件夹")
            .setView(input)
            .setPositiveButton("创建", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    String folderName = input.getText().toString().trim();
                    if (!folderName.isEmpty()) {
                        createFolder(folderName);
                    }
                }
            })
            .setNegativeButton("取消", null)
            .show();
    }

    // 创建新文件夹
    private void createFolder(String folderName) {
        File newFolder = new File(currentPath, folderName);
        if (!newFolder.exists()) {
            if (newFolder.mkdir()) {
                Toast.makeText(this, "文件夹创建成功", Toast.LENGTH_SHORT).show();
                listFiles(currentPath); // 刷新列表
            } else {
                Toast.makeText(this, "文件夹创建失败", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "文件夹已存在", Toast.LENGTH_SHORT).show();
        }
    }
    
    // "打开文件"按钮点击事件
    public void ope(View view) {
        // 显示文件浏览器界面，隐藏欢迎界面
        welcomeLayout.setVisibility(View.GONE);
        fileBrowserLayout.setVisibility(View.VISIBLE);
        
        // 列出初始目录文件
        listFiles(currentPath);
    }
    
    // 列出目录中的文件
    private void listFiles(String path) {
        // 清除当前文件列表
        fileList.clear();
        pathList.clear();
        
        // 添加返回上级选项（..）
        if (!path.equals("/")) {
            fileList.add("..");
            pathList.add(".."); // 特殊标记，表示返回上级
        }
        
        File currentDir = new File(path);
        File[] files = currentDir.listFiles();
        
        if (files != null) {
            // 添加目录项（前面加/）
            for (File file : files) {
                if (file.isDirectory()) {
                    fileList.add("/" + file.getName());
                    pathList.add(file.getAbsolutePath());
                }
            }
            
            // 添加文件项
            for (File file : files) {
                if (!file.isDirectory()) {
                    fileList.add(file.getName());
                    pathList.add(file.getAbsolutePath());
                }
            }
        }
        
        // 更新当前路径显示（匹配截图中的文本格式）
        currentPathView.setText("当前路径: " + path);
        
        // 通知适配器数据已更新
        fileAdapter.notifyDataSetChanged();
    }
    
    // 处理tmlt文件
    private void processTmltFile(String filePath) {
        try {
            // 读取文件内容
            StringBuilder content = new StringBuilder();
            BufferedReader reader = new BufferedReader(new FileReader(filePath));
            String line;
            
            while ((line = reader.readLine()) != null) {
                String trimmedLine = line.trim();
                // 跳过注释行和空行
                if (!trimmedLine.startsWith("//") && !trimmedLine.isEmpty()) {
                    content.append(line).append("\n");
                }
            }
            reader.close();
            
            // 解析文件内容
            String fullContent = content.toString();
            String[] parts = fullContent.split("\n\n"); // 使用两个换行分隔类
            
            if (parts.length < 1) {
                // 无效格式处理
                showErrorDialog("错误", "无效的TMLT格式：缺少模板标识符");
                return;
            }
            
            // 提取模板标识符
            String templateIdentifier = parts[0].trim();
            StringBuilder result = new StringBuilder("模板标识符: " + templateIdentifier + "\n\n");
            
            // 解析每个类
            for (int i = 1; i < parts.length; i++) {
                String classPart = parts[i].trim();
                if (classPart.isEmpty()) continue;
                
                String[] lines = classPart.split("\n");
                if (lines.length < 1) continue;
                
                // 检查类名格式：[类名]
                String classNameLine = lines[0].trim();
                if (!classNameLine.startsWith("[") || !classNameLine.endsWith("]")) {
                    continue;
                }
                
                // 提取类名（去掉方括号）
                String className = classNameLine.substring(1, classNameLine.length() - 1);
                result.append("类名: ").append(className).append("\n");
                
                // 解析属性
                for (int j = 1; j < lines.length; j++) {
                    String attrLine = lines[j].trim();
                    // 跳过空行或注释行
                    if (attrLine.isEmpty() || attrLine.startsWith("//")) continue;
                    
                    // 按冒号分割属性名和属性值
                    int colonPos = attrLine.indexOf(':');
                    if (colonPos <= 0) continue;
                    
                    String attrName = attrLine.substring(0, colonPos).trim();
                    String attrValue = attrLine.substring(colonPos + 1).trim();
                    
                    // 替换分隔符&为逗号
                    attrValue = attrValue.replace("&", ", ");
                    
                    result.append("  ")
                          .append(attrName)
                          .append(": ")
                          .append(attrValue)
                          .append("\n");
                }
                
                result.append("\n");
            }
            
            // 显示解析结果
            showResultDialog(filePath, result.toString());
            
        } catch(Exception e) {
            // 错误处理
            showErrorDialog("解析错误", "文件解析失败: " + e.getMessage());
        }
    }
    
    
    // 修改对话框按钮监听器为命名类
    private class DialogButtonListener implements DialogInterface.OnClickListener {
        public void onClick(DialogInterface dialog, int which) {
            dialog.dismiss();
        }
    }
    
    // 在showResultDialog和showErrorDialog中使用：
    private void showResultDialog(String filePath, String content) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("解析结果: " + new File(filePath).getName());
        
        // 创建可滚动视图
        ScrollView scrollView = new ScrollView(this);
        TextView textView = new TextView(this);
        textView.setText(content);
        textView.setTextSize(16);
        textView.setPadding(40, 30, 40, 30);
        scrollView.addView(textView);
        
        builder.setView(scrollView);
        
        // 设置确定按钮
        builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        
        // 设置返回按钮
        builder.setNegativeButton("返回", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        
        // 显示对话框
        builder.create().show();
        builder.setPositiveButton("确定", new DialogButtonListener());
        builder.setNegativeButton("返回", new DialogButtonListener());
        // ...
    }
    
    // 显示错误对话框
    private void showErrorDialog(String title, String message) {
        // 创建简单错误提示对话框
        new AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("确定", null)
            .show();
    }
    
    // 处理返回按钮
    @Override
    public void onBackPressed() {
        if (fileBrowserLayout.getVisibility() == View.VISIBLE) {
            // 如果不在根目录，返回上级目录
            if (!currentPath.equals(Environment.getExternalStorageDirectory().getPath())) {
                File currentDir = new File(currentPath);
                currentPath = currentDir.getParent();
                listFiles(currentPath);
            } else {
                // 在根目录时返回欢迎界面
                welcomeLayout.setVisibility(View.VISIBLE);
                fileBrowserLayout.setVisibility(View.GONE);
            }
        } else {
            // 在欢迎界面时退出应用
            super.onBackPressed();
        }
    }
    
}
