package com.boatofstudy.knowledge;

import android.content.Context;
import java.io.File;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TmltParser {
    public interface ResultCallback {
        void onResult(String filePath, String content);
        void onError(String title, String message);
    }

    private Context context;
    private ResultCallback callback;
    
    public TmltParser(Context context, ResultCallback callback) {
        this.context = context;
        this.callback = callback;
    }
    
    public void processTmltFile(String filePath) {
        try {
            File file = new File(filePath);
            if (file.exists()) {
                List<String> contentLines = new ArrayList<>();
                BufferedReader reader = new BufferedReader(new FileReader(file));
                String line;
                while ((line = reader.readLine()) != null) {
                    contentLines.add(parseTmltLine(line));
                }
                reader.close();
                
                String result = processContent(contentLines);
                if (callback != null) {
                    callback.onResult(filePath, result);
                }
            }
        } catch (IOException e) {
            if (callback != null) {
                callback.onError("文件读取错误", e.getMessage());
            }
        }
    }
    
    private String parseTmltLine(String line) {
        return line.trim();
    }
    
    private String processContent(List<String> contentLines) {
        StringBuilder result = new StringBuilder();
        for (String line : contentLines) {
            result.append(line).append("\n");
        }
        return result.toString();
    }
}
