package com.ymhou.service;

import com.ymhou.controller.HomeController;
import org.apache.commons.lang.CharUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by ymhou on 2017/3/30.
 */
@Service
public class SensitiveService implements InitializingBean{
    private static final Logger logger = LoggerFactory.getLogger(SensitiveService.class);

    public static final String DEFAULT_REPLACEMENT = "***";

    @Override
    public void afterPropertiesSet() throws Exception {
        try{
            InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("SensitiveWords.txt");
            InputStreamReader read = new InputStreamReader(inputStream);
            BufferedReader bufferedReader = new BufferedReader(read);
            String lineTxt;
            while((lineTxt = bufferedReader.readLine()) != null){
                lineTxt = lineTxt.trim();
                addWord(lineTxt);
            }
            read.close();
        }
        catch (Exception e){
            logger.error("读取敏感词文件失败"+e.getMessage());
        }
    }

    /**
     * 定义前缀树
     */
    private class TrieNode {
        //关键词终结
        private boolean end = false;

        //key下一个字符，value对应的节点
        private Map<Character, TrieNode> subNodes = new HashMap<>();

        //向指定位置添加节点树
        void addSubNode(Character key, TrieNode node) {
            subNodes.put(key, node);
        }

        //获取下个节点
        TrieNode getSubNode(Character key) {
            return subNodes.get(key);
        }

        boolean isKeyWordEnd() {
            return end;
        }

        void setKeyWordEnd(boolean end) {
            this.end = end;
        }
    }

    //根节点
    private TrieNode rootNode = new TrieNode();

    //增加敏感词
    private void addWord(String lineTxt) {
        TrieNode tempNode = rootNode;

        for (int i = 0; i < lineTxt.length(); i++) {
            Character c = lineTxt.charAt(i);

            TrieNode node = tempNode.getSubNode(c);
            if (node == null) {
                node = new TrieNode();
                tempNode.addSubNode(c, node);
            }

            tempNode = node;

            if (i == lineTxt.length() - 1) {
                //关键词结束，设置结束标识
                tempNode.setKeyWordEnd(true);
            }
        }
    }

    /**
     * 过滤敏感词
     *
     * @param text
     * @return
     */
    public String filter(String text) {
        if (StringUtils.isBlank(text)) {
            return text;
        }
        String replacement = DEFAULT_REPLACEMENT;
        StringBuilder result = new StringBuilder();

        TrieNode tempNode = rootNode;
        int begin = 0;
        int position = 0;//当前比较的位置

        while (position < text.length()) {
            char c = text.charAt(position);
            //跳过东亚文字
            if(isSymbol(c)){
                if(tempNode == rootNode){
                    result.append(c);
                    begin++;
                }
                position++;
                continue;
            }

            tempNode = tempNode.getSubNode(c);

            //当前位置的匹配结束
            if (tempNode == null) {
                result.append(text.charAt(begin));
                position = begin + 1;
                begin = position;
                tempNode = rootNode;
            } else if (tempNode.isKeyWordEnd()) {
                result.append(replacement);
                position = position + 1;
                begin = position;
                tempNode = rootNode;
            } else {
                position++;
            }
        }

        result.append(text.substring(begin));

        return result.toString();
    }

    /**
     * 判断是否是一个符号
     */
    private boolean isSymbol(char c){
        int ic = (int)c;
        // 0x2E80-0x9FFF 东亚文字范围
        return !CharUtils.isAsciiAlphanumeric(c) && (ic < 0x2E80 || ic > 0x9FFF);
    }

//    public static void main(String[] argv) {
//        SensitiveService s = new SensitiveService();
//        s.addWord("色情");
//        System.out.println(s.filter("你好色@情"));
//    }
}
