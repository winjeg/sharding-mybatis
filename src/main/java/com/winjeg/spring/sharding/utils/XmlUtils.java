package com.winjeg.spring.sharding.utils;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * 主要是读取XML，并修改XML的内容，生成新的xml，供mybatis读取消费
 *
 * @author winjeg
 */
@Slf4j
public class XmlUtils {
    private static final SAXBuilder XML_BUILDER = new SAXBuilder();


    public static Pair<String, Resource> changeMapperNameSpace(String dsName, Resource resource) {
        InputStream inputStream = null;
        try {
            inputStream = resource.getInputStream();
        } catch (IOException e) {
            log.warn("changeMapperNameSpace - exception occurred", e);
        }
        if (inputStream == null) {
            return null;
        }
        Document doc = null;
        try {
            doc = XML_BUILDER.build(inputStream);
        } catch (JDOMException | IOException e) {
            log.warn("changeMapperNameSpace - error in mapper");
            return null;
        }
        val element = doc.getRootElement();
        String originalName = element.getAttribute("namespace").getValue();
        if (originalName == null || originalName.length() == 0) {
            return null;
        }
        String name = NameUtils.buildClassName(dsName, originalName);
        element.setAttribute("namespace", name);
        doc.detachRootElement();
        doc.setRootElement(element);
        Format format = Format.getPrettyFormat();
        format.setEncoding("UTF-8");
        format.setTextMode(Format.TextMode.PRESERVE);
        XMLOutputter output = new XMLOutputter(format);
        String xml = output.outputString(doc);
        val res = new InputStreamResource(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)), name);
        return Pair.of(name, res);
    }

    /**
     * 读取一个mapper的namespace
     *
     * @param resource 资源
     * @return namespace
     */
    public static String extractNamespace(Resource resource) {
        InputStream inputStream = null;
        try {
            inputStream = resource.getInputStream();
        } catch (IOException e) {
            log.warn("changeMapperNameSpace - exception occurred", e);
        }
        if (inputStream == null) {
            return null;
        }
        Document doc = null;
        try {
            doc = XML_BUILDER.build(inputStream);
        } catch (JDOMException | IOException e) {
            log.warn("changeMapperNameSpace - error in mapper");
            return null;
        }
        val element = doc.getRootElement();
        return element.getAttribute("namespace").getValue();
    }
}
