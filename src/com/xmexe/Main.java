package com.xmexe;

import com.sun.deploy.xml.XMLParser;
//import com.sun.tools.internal.ws.wsdl.document.jaxws.Exception;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;

public class Main {

    public static void main(String[] args) {
	// write your code here
        ZXLBuild build = new ZXLBuild();
        File pwd = new   File(".");
        String cur_path = pwd.getAbsolutePath();
        build.projectPath = cur_path;
        parse_args(args, build);
        try {
            build.execute();
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            System.out.print("Error:" + throwable.getMessage());
            System.exit(-1);
        }

    }

    private static void parse_args(String[] args, ZXLBuild build) {
        int i = 0;
        while (i < args.length) {
            if("-f" .equals(args[i])) {
                i ++;
                if(i < args.length) {
                    build.configFile = args[i];
                }
            }
            else if("-v".equals(args[i])) {
                i ++;
                if(i < args.length) {
                    build.version = args[i];
                }
            }
            else if("-t".equals(args[i])) {
                i ++;
                if(i < args.length) {
                    build.target = args[i];
                }
            }
            else if("-p".equals(args[i])) {
                i ++;
                if(i < args.length) {
                    build.projectPath = args[i];
                }
            }
            i ++;
        }
    }

}
