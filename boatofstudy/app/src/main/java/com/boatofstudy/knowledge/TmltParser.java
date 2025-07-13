package com.boatofstudy.knowledge;

import android.content.Context;
import java.io.File;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * TMLT模板文件解析器，处理.tmlt格式文件的解析
 */
public class TmltParser {
    /**
     * 解析结果回调接口
     */
    public interface ResultCallback {
        void onResult(String filePath, String content);
        void onError(String title, String message);
    }

    private final Context context;
    private final ResultCallback callback;
    
    public TmltParser(Context context, ResultCallback callback) {
        this.context = context;
        this.callback = callback;
    }

    // 处理TMLT文件解析
    
    public void processTmltFile(String filePath) {
        try {
            File file = new File(filePath);
            if (file.exists()) {
                // 读取文件全部内容
                BufferedReader reader = new BufferedReader(new FileReader(file));
                StringBuilder fullContent = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    fullContent.append(line).append("\n");
                }
                reader.close();
                
                String fullText = fullContent.toString();
                String[] lines = fullText.split("\n");
                
                // 过滤注释行和空行
                List<String> filteredLines = new ArrayList<>();
                for (String l : lines) {
                    if (!l.trim().isEmpty() && !l.trim().startsWith("//")) {
                        filteredLines.add(l);
                    }
                }
                
                if (filteredLines.isEmpty()) {
                    if (callback != null) {
                        callback.onError("文件内容错误", "文件不包含有效内容");
                    }
                    return;
                }
                
                // 获取模板标识符 (第一行)
                String templateIdentifier = filteredLines.get(0).trim();
                
                // 获取剩余内容
                String remainingContent = fullText.substring(templateIdentifier.length()).trim();
                
                // 按双换行分割类
                String[] classParts = remainingContent.split("\n\n");
                Map<String, Map<String, String[]>> templateDict = new HashMap<>();
                
                for (String classPart : classParts) {
                    if (classPart.trim().isEmpty()) continue;
                    
                    String[] classLines = classPart.split("\n");
                    if (classLines.length == 0) continue;
                    
                    // 获取类名 ([[ClassName]]格式)
                    String className = classLines[0].trim();
                    if (className.startsWith("[[") && className.endsWith("]]")) {
                        className = className.substring(2, className.length() - 2).trim();
                    } else {
                        continue; // 跳过无效类
                    }
                    
                    // 处理类属性
                    Map<String, String[]> classDict = new HashMap<>();
                    for (int i = 1; i < classLines.length; i++) {
                        String attrLine = classLines[i].trim();
                        if (attrLine.isEmpty() || attrLine.startsWith("//")) continue;
                        
                        // 去除行尾注释
                        String cleanLine = attrLine.split("//")[0].trim();
                        String[] attrParts = cleanLine.split(":");
                        if (attrParts.length == 2) {
                            String attrName = attrParts[0].trim();
                            String[] attrValues = attrParts[1].split("&");
                            for (int j = 0; j < attrValues.length; j++) {
                                attrValues[j] = attrValues[j].trim();
                            }
                            classDict.put(attrName, attrValues);
                        }
                    }
                    
                    if (!classDict.isEmpty()) {
                        templateDict.put(className, classDict);
                    }
                }
                
                // 构建结果对象
                Map<String, Object> result = new HashMap<>();
                result.put("identifier", templateIdentifier);
                result.put("template", templateDict);
                
                // 回调结果
                if (callback != null) {
                    callback.onResult(filePath, result.toString());
                    
                    // 同时显示用户提示
                    String message = "成功解析模板: " + templateIdentifier + "\n" +
                                    "包含类数量: " + templateDict.size();
                    callback.onError("解析完成", message);
                }
            }
        } catch (IOException e) {
            if (callback != null) {
                callback.onError("文件读取错误", e.getMessage());
            }
        }
    }
