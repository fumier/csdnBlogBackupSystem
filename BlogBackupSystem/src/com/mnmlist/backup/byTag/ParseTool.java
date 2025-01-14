package com.mnmlist.backup.byTag;

import java.util.List;

import javax.management.Attribute;
import javax.management.AttributeList;

import org.htmlparser.Node;
import org.htmlparser.NodeFilter;
import org.htmlparser.Parser;
import org.htmlparser.filters.AndFilter;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.ImageTag;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.tags.Span;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;

/**
 * @author mnmlist@163.com
 * @blog http://blog.csdn.net/mnmlist
 * @version v1.0
 */
public class ParseTool {
	/*
	 * @param url html页面url
	 * 
	 * @param tagNameFilter tagName of a filter
	 *
	 * @param attributeKey 标签名
	 *
	 * @param attributeValue 标签值
	 * 
	 * @return 返回某个结点的子节点,如果标签名为article_title，直接返回标签名为article_title对应的所有结点
	 */
	public static NodeList getNodeList(String url, String tagNameFilter,
			String attributeKey, String attributeValue) {
		Parser parser = ParserInstance.getParserInstance(url);
		NodeFilter andFilter = new AndFilter(new TagNameFilter(tagNameFilter),
				new HasAttributeFilter(attributeKey, attributeValue));
		NodeList list = null;
		try {
			list = parser.extractAllNodesThatMatch(andFilter);
		} catch (ParserException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		NodeList nlist = null;
		if (list.size() > 0)
			if(attributeValue.equals("article_title"))
			return list;
			nlist = list.elementAt(0).getChildren();
		return nlist;
	}
	/*
	 * @param urlString 某博客的url
	 * 
	 * @param tagString 博客文章的某个类别
	 * 
	 * @return 某类别对应的url
	 */
	public static String getUrlFromTag(String urlString,String tagString,BlogInfo blogInfo)
	{
		NodeList nList=ParseTool.getNodeList(urlString,"div","id","panel_Category");
    	NodeFilter andFilter = new AndFilter(new TagNameFilter("ul"),
				new HasAttributeFilter("class", "panel_body"));
    	nList=nList.extractAllNodesThatMatch(andFilter);
    	nList=nList.elementAt(0).getChildren();
		boolean flag=ParseTool.parseUrl(nList,tagString,blogInfo);
		if(flag)
			return blogInfo.getTagCorrespondedURL();
		return null;
	}
	/*
	 * @param nlist 博客目录的子标签链表
	 * 
	 * @param tag 博客文章的某个类别
	 */
	public static boolean parseUrl(NodeList nList,String tag,BlogInfo blogInfo)
    {
    	int size=nList.size();
    	boolean flag=false;
    	for(int j=0;j<size;j++)
		{
			Node atls=nList.elementAt(j);
			if (atls instanceof LinkTag) {
				LinkTag link = (LinkTag) atls;
				if(link.getLinkText().equals(tag))
				{
					blogInfo.setTagCorrespondedURL(link.extractLink());//获得tag对应的url
					flag=true;
					break;
				}
			}else {
				NodeList slist = atls.getChildren();
				if (slist != null && slist.size() > 0) {
					flag=parseUrl(slist,tag,blogInfo);
					if(flag)
						break;
				}
			}
		}
    	return flag;
    }
	/*
	 * @param nlist HTML正文的子标签链表
	 * 
	 * @param index 用于索引图片的个数以及当前的图片数
	 * 
	 * @return 当前的图片数
	 */
	public static int parseImg(NodeList nlist, int index,final BlogInfo blogInfo) {
		String path=blogInfo.getNewArticlePath();
		Node img = null;
		int count = nlist.size();
		for (int i = 0; i < count; i++) {
			img = nlist.elementAt(i);
			if (img instanceof ImageTag) {
				ImageTag imgtag = (ImageTag) img;
				if (!imgtag.isEndTag()) {
					/* 将图片的URL映射成本地路径 */
					blogInfo.getImageResourceList().add(new Attribute("" + index,
							new String(imgtag.extractImageLocn().getBytes())));
					imgtag.setImageURL(path + "_files/" + index + ".gif");
					/* 递增本地路径序列 */
					index++;
				}
			} else {
				NodeList slist = img.getChildren();
				if (slist != null && slist.size() > 0) {
					index = ParseTool.parseImg(slist, index,blogInfo);
				}
			}
		}
		return index;
	}
	/*
	 * @param nlist HTML每月份存档的子标签链表
	 * 
	 * @param index 无用
	 * 
	 * @return 无用
	 */
	public static void parsePerArticle(NodeList nlist,BlogInfo blogInfo) {
		Node atl = null;
		int count = nlist.size();
		for (int i = 1; i < count; i+=2) {
			atl = nlist.elementAt(i);
			if (atl instanceof Span) {
				Span span = (Span) atl;
				if (span.getAttribute("class") != null
						&& span.getAttribute("class").equalsIgnoreCase(
								"link_title")) {
					LinkTag link = (LinkTag) span.childAt(0);
					String urlDescripString=link.getLinkText().trim().replaceAll("[\\?/:*|<>\"]","_");
					blogInfo.getColumArticleList().add(new Attribute(urlDescripString, "http://blog.csdn.net"
							+ link.extractLink()));
				}
			} else {
				NodeList slist = atl.getChildren();
				if (slist != null && slist.size() > 0) {
					parsePerArticle(slist,blogInfo);
				}
			}
		}
	}

	/*
	 * @param nlist HTML分页显示标签的子标签链表
	 * 
	 * @param index 无用
	 * 
	 * @return 无用
	 */
	public static void parsePage(NodeList nlist,BlogInfo blogInfo) {// from parseMonth
		Node pg = null;
		int count = nlist.size();
		for (int i = 0; i < count; i++) {
			pg = nlist.elementAt(i);
			if (pg instanceof LinkTag) {
				LinkTag lt = (LinkTag) pg;
				if (lt.getLinkText().equalsIgnoreCase("下一页")) {
					String url = "http://blog.csdn.net" + lt.extractLink();
					NodeList titleList =getNodeList(url, "div", "id", "article_list");
					parsePerArticle(titleList,blogInfo);
					NodeList fenYeList = getNodeList(url, "div", "id", "papelist");
					if(fenYeList!=null)
						parsePage(fenYeList,blogInfo);
					else 
						break;
				}
			}
		}
	}

	/*
	 * @param filepath 本地存档的路径
	 * 
	 * @param url 保存本月存档的网页的URL
	 * 
	 * @param articles 保存本月存档的链表
	 * 
	 * @return 无
	 */
	public static void parseColums(String filepath, String url,
			AttributeList articles,BlogInfo blogInfo) {
		NodeList titleList = getNodeList(url, "div", "id", "article_list");//list view
		int size=titleList.size();
		for(int i=1;i<size;i+=2)
		{
			NodeList sList=titleList.elementAt(i).getChildren();
			sList=sList.elementAt(1).getChildren();//article title的子节点
			parsePerArticle(sList,blogInfo);
		}
		NodeList fenYeList = getNodeList(url, "div", "id", "papelist");
		if (fenYeList != null)
			parsePage(fenYeList,blogInfo);
		/* 慢一点，否则会被认为是恶意行为 */
		List<Attribute> li =blogInfo.getColumArticleList().asList();
		for (int i = 0; i < li.size(); i++) {
			String titleName=(String)li.get(i).getName();
			HandleTool.handleHtml(titleName, (String) li.get(i).getValue(),blogInfo);
			try {
				/* 慢一点，否则会被认为是恶意行为 */
				Thread.sleep(500);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

}
