package com.boatofstudy.knowledge;

import android.content.Context;
import java.io.File;
import java.util.List;
import java.util.ArrayList;
import java.util.Stack;
import android.os.Environment;
import android.content.pm.PackageManager;
import android.Manifest;

public class BackendController {
    private Context context;
    private List<String> fileList = new ArrayList<>();
    private List<String> pathList = new ArrayList<>();
    private Stack<String> pathHistory = new Stack<>();
    private String currentPath;
    
    public BackendController(Context context) {
        this.context = context;
        this.currentPath = getRootDirectoryPath();
    }
    
    public String getRootDirectoryPath() {
        File externalDir = Environment.getExternalStorageDirectory();
        String rootPath = externalDir != null ? externalDir.getAbsolutePath() : "";
        if (rootPath.endsWith("/")) {
            rootPath = rootPath.substring(0, rootPath.length() - 1);
        }
        return rootPath;
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
        return fileList;
    }
    
    public List<String> getPathList() {
        return pathList;
    }

    public boolean deleteFile(String path) {
        File file = new File(path);
        return file.delete();
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

    public void requestStoragePermission(Activity activity) {
        ActivityCompat.requestPermissions(activity,
                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                PERMISSION_REQUEST_CODE);
    }
}
