package com.xmexe;


import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.util.HashMap;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Created by kylin on 16/4/18.
 */
public class ZXLBuild {

    public String projectPath;
    public String configFile;
    public String target;
    public String version;

    private TargetConfig target_config;

    //private static Logger logger = Logger.getLogger(ZXLBuild.class);

    public ZXLBuild() {

    }


    private void checkIsNull(String value, String name) {
        if(value == null || value=="") {
            throw  new IllegalArgumentException(String.format("%s 未设置", name));
        }
    }

    public void execute() throws Throwable {
        checkIsNull(this.configFile, "配置文件");
        checkIsNull(this.target, "target");
        checkIsNull(this.projectPath, "项目路径");

        this.target_config = new TargetConfig();
        System.out.println(">读取配置文件");
        loadConfig();
        if(this.target_config.getNameValues().size() > 0) {
          for (String item_name: this.target_config.getNameValues().keySet()) {
              String sMsg = String.format("\t %s=>%s", item_name, this.target_config.get(item_name));
              System.out.println(sMsg);
          }
        }

//        System.out.println(">更改极光的配置文件");
//        task_update_jpushconfig();
        System.out.println(">更改 config.xml ");
        task_update_configxml();
//        System.out.println(">更改版本号");
//        task_updateVersion();
        System.out.println(">更改页面版本号");
        updateHtmlVersion();
        task_build();
        System.out.println(">Success");
    }


    protected void updateHtmlVersion(){
        File file_version = new File(this.projectPath + "/assets/www/js/versionConfig.txt");
        String content = target_config.get("html.version");
        try {
            setFileContent(file_version.getAbsolutePath(), content);
        } catch (IOException e) {

            e.printStackTrace();
        }
    }

    protected void loadConfig() throws Exception {
        File file = new File(this.configFile);
        if (!file.exists()) {
            throw new Exception("无法找到配置文件");
        }
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = null;
        builder = factory.newDocumentBuilder();
        Document document = builder.parse(file);
        NodeList nodeList = document.getElementsByTagName("target");
        Node node = null;
        if (nodeList != null && nodeList.getLength() > 0) {

            if (target == null || target == "") {
                node = nodeList.item(0);
            } else {
                for (int i = 0; i < nodeList.getLength(); i++) {
                    Element tmp = (Element) nodeList.item(i);
                    String sValue = tmp.getAttribute("name");
                    if (this.target.compareToIgnoreCase(sValue) == 0) {
                        node = tmp;
                        break;
                    }
                }
            }
        }
        if (node == null)
            throw new Exception(String.format("配置文件无法找到 target %s", target));
        else {
            NodeList childList = node.getChildNodes();
            for (int i = 0, icount = childList.getLength(); i < icount; i++) {
                Node item = childList.item(i);
                if ("property".equals(item.getNodeName())) {
                    Element element = (Element) item;
                    String sName = element.getAttribute("name");
                    String sValue = element.getAttribute("value");
                    this.target_config.put(sName, sValue);
                }
            }
        }
    }

    private void task_update_configxml() throws Exception {
        File file_config = new File(this.projectPath + "/res/xml/config.xml");
        if(!file_config.exists()) {
            throw new IOException(String.format("无法找到 %s", file_config.getAbsoluteFile()));
        }
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = null;
        builder = factory.newDocumentBuilder();
        Document document = builder.parse(file_config);
        Element root = document.getDocumentElement();
        if (this.version != null) {
            root.setAttribute("version", this.version);
        }
        if (this.target_config.has("appId")) {
            root.setAttribute("id", this.target_config.get("appId"));
        }
        NodeList nodeList = document.getElementsByTagName("preference");
        if (nodeList != null && nodeList.getLength() > 0) {
            for (int i = 0, icount = nodeList.getLength(); i < icount; i++) {
                Node item = nodeList.item(i);
                if (item.getNodeType() == Node.ELEMENT_NODE) {
                    Element item_element = (Element) item;
                    String item_element_name = item_element.getAttribute("name");
                    if ("WECHATAPPID".compareTo(item_element_name) == 0) {
                        item_element.setAttribute("value", this.target_config.get("wechat.appId"));
                        //break;
                    }
                }
            }
        }
        //保存
        TransformerFactory factory_save = TransformerFactory.newInstance();
        Transformer former = factory_save.newTransformer();
        former.transform(new DOMSource(document), new StreamResult(file_config));
    }


    private String getFileContent(String path) throws IOException {
        StringBuilder sb = new StringBuilder();
        FileReader reader = new FileReader(path);
        BufferedReader bufferReader = new BufferedReader(reader);
        try {
            String line = null;
            do {
                line = bufferReader.readLine();
                if (line != null) {
                    sb.append(line);
                }
            } while (line != null);
        } finally {
            bufferReader.close();
        }
        return sb.toString();
    }

    /*
     *  内容写入文档
     */
    private void setFileContent(String path, String content) throws IOException {
        File tmpFile = new File(path + "~");
        if ((tmpFile.exists())) {
            tmpFile.delete();
        }
        BufferedWriter writer = new BufferedWriter(new FileWriter((tmpFile)));
        try {
            writer.write(content);
        } finally {
            writer.close();
        }
        File file_path = new File(path);
        file_path.delete();
        tmpFile.renameTo(file_path);
    }

    private void task_update_jpushconfig() throws IOException {
        String str_jpush_appId = target_config.get("jpush.appId");
        if (str_jpush_appId == null || str_jpush_appId == "") return;
        String filePath_PushConfig = this.projectPath + "/exe/Resources/PushConfig.plist";
        File file = new File(filePath_PushConfig);
        if (file.exists()) {
            try {
                String file_content = getFileContent(filePath_PushConfig);
                Pattern pattern = Pattern.compile("<key>APP_KEY</key>.?<string>(\\w+)</string>", Pattern.MULTILINE | Pattern.DOTALL);
                //Pattern pattern = Pattern.compile("<string>.+</string>", Pattern.MULTILINE);
                Matcher matcher = pattern.matcher(file_content);
                if (matcher.find()) {
                    StringBuilder sb = new StringBuilder();
                    sb.append(file_content.substring(0, matcher.start(1)));
                    sb.append(str_jpush_appId);
                    sb.append(file_content.substring(matcher.end(1)));
                    setFileContent(filePath_PushConfig, sb.toString());

                }
            } catch (IOException exp) {
                System.out.print(exp.getMessage());
            }
        }
    }

    private void task_build() {

    }

    private void task_updateVersion() throws IOException {
            if (this.version == null && this.version == "") return;
            //<key>CFBundleShortVersionString</key>
            //<string>1.6.0415</string>
            //<key>CFBundleVersion</key>
            //<string>1.6.0415</string>
            String info_plist = target_config.get("info.plist");
            if (info_plist == null) return;
            String filePath_info_plist = this.projectPath + "/exe/" + info_plist;
            File file = new File(filePath_info_plist);
            if (!file.exists()) {
                throw  new IOException(String.format("找不到文件 info.plist 文件->%s", filePath_info_plist));
            }
            boolean bChange = false;
            String file_content = getFileContent(filePath_info_plist);
            Pattern pattern = Pattern.compile("<key>CFBundleShortVersionString</key>.+?<string>(.+?)</string>", Pattern.MULTILINE | Pattern.DOTALL);
            Matcher matcher = pattern.matcher(file_content);
            if (matcher.find()) {
                StringBuilder sb = new StringBuilder();
                sb.append(file_content.substring(0, matcher.start(1)));
                sb.append(this.version);
                sb.append(file_content.substring(matcher.end(1)));
                file_content = sb.toString();
                bChange = true;
            }
            //
            pattern = Pattern.compile("<key>CFBundleVersion</key>.+?<string>(.+?)</string>", Pattern.MULTILINE | Pattern.DOTALL);
            matcher = pattern.matcher(file_content);
            if (matcher.find()) {
                StringBuilder sb = new StringBuilder();
                sb.append(file_content.substring(0, matcher.start(1)));
                sb.append(this.version);
                sb.append(file_content.substring(matcher.end(1)));
                file_content = sb.toString();
                bChange = true;
            }
            if (bChange) setFileContent(filePath_info_plist, file_content);
    }

    class  TargetConfig {
        private  HashMap<String, String> _nameValues = new HashMap<String, String>();

        public  void put(String name, String value) {
            if(name != null && name != "") {
                _nameValues.put(name, value);
            }
        }

        public HashMap<String, String> getNameValues() {
            return  _nameValues;
        }

        public  String get(String name) {
            return  _nameValues.get(name);
        }

        public  boolean has(String name) {
            return  _nameValues.containsKey(name);
        }
    }
}
