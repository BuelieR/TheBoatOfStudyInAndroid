package com.boatofstudy.knowledge;

import android.content.Context;
import android.net.Uri;
import android.provider.DocumentsContract;
import android.os.Build;
import android.os.Environment;
import android.content.pm.PackageManager;
import android.Manifest;
import androidx.core.content.ContextCompat;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.Stack;
import java.util.Date;

/**
 * 文件系统操作控制器，处理文件浏览、增删改查等操作
 */
public class BackendController {
    private final Context context;
    private final List<String> fileList = new ArrayList<>();
    private final List<String> pathList = new ArrayList<>();
    private final Stack<String> pathHistory = new Stack<>();
    private String currentPath;
    
    public BackendController(Context context) {
        this.context = context;
        this.currentPath = getRootDirectoryPath();
    }

    // 获取设备上可用的根存储路径
    
    public List<String> getAvailableRootPaths() {
        List<String> rootPaths = new ArrayList<>();
        
        // 添加外部存储目录
        
        // 添加标准存储目录
        File externalDir = Environment.getExternalStorageDirectory();
        if (externalDir != null) {
            rootPaths.add(externalDir.getAbsolutePath());
        }
        
        // 添加下载目录
        
        // 添加其他可能的外部存储目录
        File[] externalDirs = ContextCompat.getExternalFilesDirs(context, null);
        for (File dir : externalDirs) {
            if (dir != null) {
                String path = dir.getAbsolutePath();
                // 提取存储根目录 (移除Android/data/...部分)
                int androidPos = path.indexOf("/Android/data/");
                if (androidPos > 0) {
                    path = path.substring(0, androidPos);
                }
                if (!rootPaths.contains(path)) {
                    rootPaths.add(path);
                }
            }
        }
        
        // 添加下载目录
        File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        if (downloadsDir != null && !rootPaths.contains(downloadsDir.getAbsolutePath())) {
            rootPaths.add(downloadsDir.getAbsolutePath());
        }
        
        return rootPaths;
    }

    public String getRootDirectoryPath() {
        List<String> availablePaths = getAvailableRootPaths();
        return availablePaths.isEmpty() ? "/" : availablePaths.get(0);
    }
    
    public void listFiles(String path) {
        fileList.clear();
        pathList.clear();
        
        if (!path.equals("/")) {
            fileList.add("../");
            pathList.add(path.substring(0, path.lastIndexOf('/')));
        }
        
        File currentDir = new File(path);
        File[] files = currentDir.listFiles();
        
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    fileList.add(file.getName() + "/");
                } else {
                    fileList.add(file.getName());
                }
                pathList.add(file.getAbsolutePath());
            }
        }
    }
    
    public void enterNewDirectory(String newPath) {
        pathHistory.push(currentPath);
        currentPath = newPath;
    }
    
    public boolean handleBackDirectory() {
        if (!pathHistory.isEmpty()) {
            currentPath = pathHistory.pop();
            return true;
        }
        return false;
    }
    
    public List<String> getFileList() {
        List<String> normalizedList = new ArrayList<>();
        for (String fileName : fileList) {
            // 规范化文件名显示
            if (fileName.endsWith("/")) {
                normalizedList.add(fileName.substring(0, fileName.length() - 1));
            } else {
                normalizedList.add(fileName);
            }
        }
        return normalizedList;
    }
    
    public List<String> getPathList() {
        List<String> normalizedPaths = new ArrayList<>();
        for (String path : pathList) {
            // 统一路径格式
            String normalizedPath = path.replace('\\', '/');
            if (!normalizedPath.endsWith("/") && new File(normalizedPath).isDirectory()) {
                normalizedPath += "/";
            }
            normalizedPaths.add(normalizedPath);
        }
        return normalizedPaths;
    }

    public boolean deleteFile(String path) {
        try {
            File file = new File(path);
            if (isAndroid10OrAbove()) {
                // Android 10+需要特殊处理
                Uri uri = getUriFromPath(path);
                if (uri != null) {
                    return DocumentsContract.deleteDocument(context.getContentResolver(), uri);
                }
            }
            return file.delete();
        } catch (Exception e) {
            return false;
        }
    }

    private Uri getUriFromPath(String path) {
        // 将路径转换为Uri (Android 10+兼容)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            String relativePath = path.substring(path.indexOf("/Android/data/"));
            return DocumentsContract.buildDocumentUri(
                "com.android.externalstorage.documents",
                "primary:" + relativePath
            );
        }
        return Uri.fromFile(new File(path));
    }

    public boolean renameFile(String oldPath, String newName) {
        File oldFile = new File(oldPath);
        File newFile = new File(oldFile.getParent(), newName);
        return oldFile.renameTo(newFile);
    }

    public boolean checkStoragePermission() {
        return ContextCompat.checkSelfPermission(context, 
                Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    private static final int STORAGE_ACCESS_REQUEST_CODE = 101;
    private static final int DOCUMENT_TREE_REQUEST_CODE = 102;

    public String getCurrentPath() {
        return currentPath;
    }

    public boolean isAndroid10OrAbove() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q;
    }

    public void requestStorageAccess(Activity activity) {
        if (isAndroid10OrAbove()) {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            activity.startActivityForResult(intent, DOCUMENT_TREE_REQUEST_CODE);
        } else {
            ActivityCompat.requestPermissions(activity,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    STORAGE_ACCESS_REQUEST_CODE);
        }
    }

    public boolean handleDocumentTreeResult(Uri treeUri) {
        if (treeUri != null) {
            context.getContentResolver().takePersistableUriPermission(
                    treeUri, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            currentPath = getPathFromTreeUri(treeUri);
            return true;
        }
        return false;
    }

    private String getPathFromTreeUri(Uri treeUri) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return DocumentsContract.getTreeDocumentId(treeUri);
        } else {
            return treeUri.getPath();
        }
    }

    public boolean createNewDirectory(String dirName) {
        File newDir = new File(currentPath, dirName);
        return newDir.mkdir();
    }

    public boolean copyFile(String sourcePath, String destPath) {
        try {
            File source = new File(sourcePath);
            File dest = new File(destPath);
            FileInputStream in = new FileInputStream(source);
            FileOutputStream out = new FileOutputStream(dest);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = in.read(buffer)) > 0) {
                out.write(buffer, 0, length);
            }
            in.close();
            out.close();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public String getFileExtension(String path) {
        int dotIndex = path.lastIndexOf('.');
        return dotIndex > 0 ? path.substring(dotIndex + 1) : "";
    }

    public long getFileSize(String path) {
        File file = new File(path);
        return file.length();
    }

    public String getFileLastModified(String path) {
        File file = new File(path);
        return new Date(file.lastModified()).toString();
    }
}
