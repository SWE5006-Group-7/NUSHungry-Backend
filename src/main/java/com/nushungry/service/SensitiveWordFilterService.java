package com.nushungry.service;

import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 敏感词过滤服务
 * 使用DFA算法实现敏感词检测
 */
@Service
public class SensitiveWordFilterService {

    // 敏感词词库(使用Map构建DFA算法的树结构)
    private Map<String, Object> sensitiveWordMap = new HashMap<>();

    // 敏感词列表(可以从数据库或配置文件加载)
    private static final Set<String> SENSITIVE_WORDS = new HashSet<>(Arrays.asList(
            // 政治相关
            "敏感词1", "敏感词2",
            // 色情相关
            "色情", "黄色",
            // 暴力相关
            "暴力", "血腥",
            // 赌博相关
            "赌博", "博彩",
            // 诈骗相关
            "诈骗", "传销",
            // 侮辱相关
            "傻逼", "草泥马", "fuck", "shit", "damn",
            // 其他
            "垃圾", "骗子", "黑心", "宰客"
    ));

    /**
     * 初始化敏感词库
     */
    public SensitiveWordFilterService() {
        initSensitiveWordMap();
    }

    /**
     * 初始化敏感词库Map
     */
    private void initSensitiveWordMap() {
        for (String word : SENSITIVE_WORDS) {
            addSensitiveWord(word);
        }
    }

    /**
     * 添加敏感词到词库
     */
    private void addSensitiveWord(String word) {
        if (word == null || word.trim().isEmpty()) {
            return;
        }

        Map<String, Object> currentMap = sensitiveWordMap;
        for (int i = 0; i < word.length(); i++) {
            String key = String.valueOf(word.charAt(i));

            // 如果当前字符不存在,则添加
            if (!currentMap.containsKey(key)) {
                Map<String, Object> newMap = new HashMap<>();
                currentMap.put(key, newMap);
                currentMap = newMap;
            } else {
                currentMap = (Map<String, Object>) currentMap.get(key);
            }

            // 最后一个字符,标记为结束
            if (i == word.length() - 1) {
                currentMap.put("isEnd", true);
            }
        }
    }

    /**
     * 检查文本是否包含敏感词
     * @param text 待检测文本
     * @return true-包含敏感词, false-不包含
     */
    public boolean containsSensitiveWord(String text) {
        if (text == null || text.trim().isEmpty()) {
            return false;
        }

        for (int i = 0; i < text.length(); i++) {
            int matchLength = checkSensitiveWord(text, i);
            if (matchLength > 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * 获取文本中的所有敏感词
     * @param text 待检测文本
     * @return 敏感词集合
     */
    public Set<String> getSensitiveWords(String text) {
        Set<String> sensitiveWords = new HashSet<>();
        if (text == null || text.trim().isEmpty()) {
            return sensitiveWords;
        }

        for (int i = 0; i < text.length(); i++) {
            int matchLength = checkSensitiveWord(text, i);
            if (matchLength > 0) {
                sensitiveWords.add(text.substring(i, i + matchLength));
                i += matchLength - 1; // 跳过已匹配的部分
            }
        }
        return sensitiveWords;
    }

    /**
     * 替换文本中的敏感词
     * @param text 待处理文本
     * @param replaceChar 替换字符(默认*)
     * @return 替换后的文本
     */
    public String replaceSensitiveWord(String text, char replaceChar) {
        if (text == null || text.trim().isEmpty()) {
            return text;
        }

        StringBuilder result = new StringBuilder(text);
        for (int i = 0; i < text.length(); i++) {
            int matchLength = checkSensitiveWord(text, i);
            if (matchLength > 0) {
                for (int j = 0; j < matchLength; j++) {
                    result.setCharAt(i + j, replaceChar);
                }
                i += matchLength - 1;
            }
        }
        return result.toString();
    }

    /**
     * 替换文本中的敏感词(使用默认替换字符*)
     */
    public String replaceSensitiveWord(String text) {
        return replaceSensitiveWord(text, '*');
    }

    /**
     * 检查从指定位置开始是否存在敏感词
     * @param text 文本
     * @param beginIndex 开始位置
     * @return 敏感词长度(0表示不存在)
     */
    private int checkSensitiveWord(String text, int beginIndex) {
        int matchLength = 0;
        Map<String, Object> currentMap = sensitiveWordMap;

        for (int i = beginIndex; i < text.length(); i++) {
            String key = String.valueOf(text.charAt(i));

            currentMap = (Map<String, Object>) currentMap.get(key);
            if (currentMap == null) {
                break;
            }

            matchLength++;

            // 找到敏感词结尾
            if (currentMap.containsKey("isEnd") && (Boolean) currentMap.get("isEnd")) {
                return matchLength;
            }
        }

        return 0;
    }

    /**
     * 批量添加敏感词
     */
    public void addSensitiveWords(Set<String> words) {
        if (words == null || words.isEmpty()) {
            return;
        }
        for (String word : words) {
            addSensitiveWord(word);
        }
    }

    /**
     * 获取当前敏感词库大小
     */
    public int getSensitiveWordCount() {
        return SENSITIVE_WORDS.size();
    }
}
