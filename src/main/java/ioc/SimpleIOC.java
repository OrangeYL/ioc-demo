package ioc;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.FileInputStream;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * IOC实现类，实现下面四个步骤
 * 1.加载 xml 配置文件，遍历其中的标签
 * 2.获取标签中的 id 和 class 属性，加载 class 属性对应的类，并创建 bean
 * 3.遍历标签中的标签，获取属性值，并将属性值填充到 bean 中
 * 4.将 bean 注册到 bean 容器中
 */
public class SimpleIOC {
    //使用Map当做bean容器
    private Map<String,Object> beanMap=new HashMap<String,Object>();

    public SimpleIOC(String location) throws Exception{
        loadBeans(location);
    }
    public Object getBean(String name){
        Object bean = beanMap.get(name);
        if(bean==null){
            throw new IllegalArgumentException("there is no bean with name "+name);
        }
        return bean;
    }

    private void loadBeans(String location) throws Exception {
        //一、加载配置文件

        //1.将xml文件转化为输入流，以便 DOM 解析器解析它
        FileInputStream fileInputStream = new FileInputStream(location);

        //2.调用 DocumentBuilderFactory.newInstance() 方法得到创建 DOM 解析器的工厂对象。
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

        //3.调用工厂对象的 newDocumentBuilder方法得到 DOM 解析器对象
        DocumentBuilder newDocumentBuilder = factory.newDocumentBuilder();

        //4. 调用 DOM 解析器对象的 parse() 方法解析 XML 文档，得到代表整个文档的 Document 对象
        // 然后可以利用DOM特性对整个XML文档进行操作了。
        Document document = newDocumentBuilder.parse(fileInputStream);

        //5.得到 XML 文档的根节点
        Element root = document.getDocumentElement();

        //6.得到节点的子节点
        NodeList nodes = root.getChildNodes();

        //二、遍历<bean>标签
        for(int i=0;i<nodes.getLength();i++){
            Node item = nodes.item(i);
            if(item instanceof Element){
                Element element = (Element) item;
                String id = element.getAttribute("id");
                String className = element.getAttribute("class");
                //加载beanClass
                Class beanClass=null;
                try {
                    beanClass=Class.forName(className);
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                    return;
                }
                //创建bean
                Object bean = beanClass.newInstance();

                //遍历<property>标签
                NodeList propertyNodes = element.getElementsByTagName("property");
                for(int j=0;j<propertyNodes.getLength();j++){
                    Node propertyNode = propertyNodes.item(j);
                    if(propertyNode instanceof Element){
                        Element propertyElement=(Element) propertyNode;
                        String name = propertyElement.getAttribute("name");
                        String value = propertyElement.getAttribute("value");
                        // 利用反射将 bean 相关字段访问权限设为可访问
                        Field declaredField = bean.getClass().getDeclaredField(name);
                        declaredField.setAccessible(true);

                        if(value!=null&&value.length()>0){
                            //把属性值填充到相关字段中
                            declaredField.set(bean,value);
                        }else {
                            String ref = propertyElement.getAttribute("ref");
                            if(ref==null||ref.length()==0){
                                throw new IllegalArgumentException("ref config error");
                            }
                            //将引用填充到相关字段中
                            declaredField.set(bean,getBean(ref));
                        }
                        //将bean注册到bean容器中
                        registerBean(id,bean);
                    }
                }
            }
        }

    }
    private void registerBean(String id,Object bean){
        beanMap.put(id,bean);
    }
}
