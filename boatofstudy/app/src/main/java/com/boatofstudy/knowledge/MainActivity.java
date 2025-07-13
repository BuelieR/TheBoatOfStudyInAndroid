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
import android.os.Environment;
import android.widget.TabHost.*;
import android.support.v4.content.ContextCompat; // 使用兼容库
import android.support.v4.app.ActivityCompat;
import android.annotation.*;
import java.net.*;
import java.lang.Process;
import android.net.*;
import android.provider.*;
import android.service.autofill.*;

public class MainActivity extends Activity {

    private LinearLayout welcomeLayout;
    private LinearLayout fileBrowserLayout;
    private TextView currentPathView;
    private ListView fileListView;
    
    private List<String> fileList = new ArrayList<>();
    private List<String> pathList = new ArrayList<>();
    private String currentPath;
    private ArrayAdapter<String> fileAdapter;
    // 新增变量
    private static final int PERMISSION_REQUEST_CODE = 100;
    private int selectedPosition = -1; // 长按选中的位置

    private TabHost tabHost; 

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        // 初始化底部导航
        tabHost = (TabHost) findViewById(android.R.id.tabhost);
        tabHost.setup();
        
        // 主页标签
        TabSpec tab1 = tabHost.newTabSpec("tab1");
        tab1.setIndicator("主页");
        tab1.setContent(R.id.tab1);
        tabHost.addTab(tab1);
        
        // 个人标签
        TabSpec tab2 = tabHost.newTabSpec("tab2");
        tab2.setIndicator("个人");
        tab2.setContent(R.id.tab2);
        tabHost.addTab(tab2);
        
        currentPath = getRootDirectoryPath();
        
        // 获取布局中的UI组件
        welcomeLayout = findViewById(R.id.welcome_layout);
        fileBrowserLayout = findViewById(R.id.file_browser_layout);
        currentPathView = findViewById(R.id.current_path);
        fileListView = findViewById(R.id.file_list_view);
        
        // 设置文件列表适配器
        fileAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, fileList) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                // 视图复用优化内存 [6,8](@ref)
                View view = super.getView(position, convertView, parent);
                TextView textView = (TextView) view.findViewById(android.R.id.text1);
                textView.setSingleLine(true);
                return view;
            }
        };
        fileListView.setAdapter(fileAdapter);
        
        // 设置文件列表适配器
        fileAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, fileList);
        fileListView.setAdapter(fileAdapter);
        
        // 设置列表项点击监听器
        fileListView.setOnItemClickListener(new FileItemClickListener());
    }

    private Stack<String> pathHistory = new Stack<>();

    // 重构的根目录获取方法
    private String getRootDirectoryPath() {
        File externalDir = Environment.getExternalStorageDirectory();
        String rootPath = externalDir != null ? externalDir.getAbsolutePath() : "";
        
        // 特殊处理三星，华为等设备
        if (rootPath.isEmpty() || !new File(rootPath).exists()) {
            rootPath = System.getenv("EXTERNAL_STORAGE");
            if (rootPath == null || !new File(rootPath).exists()) {
                // 回退到系统根目录
                rootPath = "/";
            }
        }
        
        // 确保路径格式正确
        if (rootPath.endsWith("/")) {
            rootPath = rootPath.substring(0, rootPath.length() - 1);
        }
        
        return rootPath;
    }

    // 优化文件加载方法
    private void listFiles(String path) {
        fileList.clear();
        pathList.clear();
        
        // 添加返回上级选项
        if (!path.equals("/")) {
            fileList.add("..");
            pathList.add("..");
        }
        
        File currentDir = new File(path);
        File[] files = currentDir.listFiles();
        
        if (files != null) {
            // 按照文件类型分组显示（先目录后文件）
            List<File> directories = new ArrayList<>();
            List<File> fileItems = new ArrayList<>();
            
            // 递归列出所有目录
            for (File file : files) {
                if (file.isDirectory()) {
                    directories.add(file);
                } else {
                    fileItems.add(file);
                }
            }
            
            // 添加系统目录标记
            for (File dir : directories) {
                // 判断是否是系统目录
                boolean isSystemDir = isSystemDirectory(dir);
                String prefix = isSystemDir ? "⚙ " : "/";
                
                fileList.add(prefix + dir.getName());
                pathList.add(dir.getAbsolutePath());
            }
            
            // 添加文件项
            for (File file : fileItems) {
                fileList.add(file.getName());
                pathList.add(file.getAbsolutePath());
            }
        } else {
            // 特殊处理需要权限的系统目录
            if (isProtectedSystemPath(path)) {
                tryAccessProtectedDirectory(path);
            } else {
                Log.e("FileList", "无法访问目录: " + path);
                Toast.makeText(this, "无法访问目录，可能无权限", Toast.LENGTH_SHORT).show();
            }
        }
        
        currentPathView.setText("当前路径: " + path);
        fileAdapter.notifyDataSetChanged();
    }
    
    // [新] 判断是否是系统关键目录
    private boolean isSystemDirectory(File dir) {
        String[] systemDirs = {"/system", "/proc", "/dev", "/sys", "/acct", "/cache", "/config"};
        String path = dir.getAbsolutePath();
        for (String sysDir : systemDirs) {
            if (path.startsWith(sysDir)) {
                return true;
            }
        }
        return false;
    }
    
    // [新] 判断受保护的系统路径
    private boolean isProtectedSystemPath(String path) {
        String[] protectedPaths = {"/system", "/proc", "/sys", "/dev"};
        for (String protPath : protectedPaths) {
            if (path.startsWith(protPath)) {
                return true;
            }
        }
        return false;
    }
    
    // [新] 尝试访问受保护的目录
    private void tryAccessProtectedDirectory(String path) {
        // 使用低级API尝试访问
        try {
            Process process = Runtime.getRuntime().exec("ls " + path);
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()));
            
            String line;
            while ((line = reader.readLine()) != null) {
                // 9. 特殊处理系统目录项
                fileList.add("🔒 " + line);
                pathList.add(path + "/" + line);
            }
            
            process.waitFor();
            reader.close();
        } catch (Exception e) {
            Log.e("SystemDirAccess", "访问系统目录失败: " + e.getMessage());
            Toast.makeText(this, "需要Root权限访问系统目录", Toast.LENGTH_SHORT).show();
        }
    }
    
    // 处理特殊系统目录的点击
    private class FileItemClickListener implements AdapterView.OnItemClickListener {
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            if (position >= 0 && position < pathList.size()) {
                String selectedPath = pathList.get(position);
                
                if ("..".equals(selectedPath)) {
                    handleBackDirectory();
                } else {
                    // 检查是否受限系统目录项
                    if (selectedPath.startsWith("🔒 ")) {
                        String actualPath = selectedPath.substring(2);
                        openRestrictedSystemDirectory(actualPath);
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
    }
    
    // [新] 打开受限制的系统目录
    private void openRestrictedSystemDirectory(String path) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("系统目录访问");
        builder.setMessage("访问此系统目录需要ROOT权限。\n可能破坏系统稳定性！");
        
		final String fpath = path;
        // 设置PositiveButton的匿名内部类实现
        builder.setPositiveButton("继续", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                try {
                    // 获取 root 权限
                    Process process = Runtime.getRuntime().exec("su");
                    
                    // 获取命令输出流
                    OutputStream os = process.getOutputStream();
                    OutputStreamWriter osw = new OutputStreamWriter(os);
                    
                    osw.write("cd " + fpath + "\n");
                    osw.write("ls\n");
                    osw.flush();
                    osw.close();
                    
                    // 读取结果
                    BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream()));
                    
                    List<String> sysItems = new ArrayList<>();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sysItems.add(line);
                    }
                    reader.close();
                    
                    showSystemDirectoryContents(fpath, sysItems);
                    
                } catch (Exception e) {
                    // 显示Toast需要完整的调用
                    Toast.makeText(
                        MainActivity.this, 
                        "ROOT访问失败: " + e.getMessage(), 
                        Toast.LENGTH_SHORT
                    ).show();
                }
            }
        });
        
        builder.setNegativeButton("取消", null);
        builder.show();
    }
    
    // [新增] 显示系统目录内容
    private void showSystemDirectoryContents(String path, List<String> contents) {
        StringBuilder sb = new StringBuilder();
        sb.append("目录: ").append(path).append("\n\n");
        
        for (String item : contents) {
            sb.append("• ").append(item).append("\n");
        }
        
        new AlertDialog.Builder(this)
            .setTitle("系统目录内容")
            .setMessage(sb.toString())
            .setPositiveButton("确定", null)
            .show();
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


    private boolean checkStoragePermission() {
        // 检查读取和写入权限
        int readPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
        int writePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        
        if (readPermission != PackageManager.PERMISSION_GRANTED || 
            writePermission != PackageManager.PERMISSION_GRANTED) {
            
            // 动态请求权限
            ActivityCompat.requestPermissions(
                this,
                new String[]{
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                },
                PERMISSION_REQUEST_CODE
            );
            return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, 
            @NonNull String[] permissions, 
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            
            // 检查所有权限是否都被授予
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break; // 发现一个未授权即退出循环
                }
            }
            
            if (allGranted) {
                // 权限全部授予，初始化文件浏览器
                initFileBrowser();
            } else {
                // 权限被拒绝，提示用户
                Toast.makeText(this, "需要存储权限才能使用文件管理功能", Toast.LENGTH_LONG).show();
                
                // 可以根据需要添加再次请求的逻辑
                showPermissionDeniedDialog();
            }
        }
    }
    
    // 权限被拒绝时显示的对话框
    private void showPermissionDeniedDialog() {
        new AlertDialog.Builder(this)
            .setTitle("权限被拒绝")
            .setMessage("您拒绝了存储权限，这将导致文件管理功能无法使用。\n\n请进入应用设置手动开启权限。")
            .setPositiveButton("去设置", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // 打开应用设置页面
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    intent.setData(Uri.parse("package:" + getPackageName()));
                    startActivity(intent);
                }
            })
            .setNegativeButton("取消", null)
            .show();
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
		checkStoragePermission();
		
        // 显示文件浏览器界面，隐藏欢迎界面
        welcomeLayout.setVisibility(View.GONE);
        fileBrowserLayout.setVisibility(View.VISIBLE);
        
        // 列出初始目录文件
        listFiles(currentPath);
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
                
                // 提取类名
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
    }
    
    // 显示错误对话框
    private void showErrorDialog(String title, String message) {
        // 创建简误提示对话框
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
            if (!currentPath.equals("/")) {
                handleBackDirectory();
            } else {
                // 在根目录时返回欢迎界面
                welcomeLayout.setVisibility(View.VISIBLE);
                fileBrowserLayout.setVisibility(View.GONE);
            }
        } else {
            super.onBackPressed();
        }
    }
    
}