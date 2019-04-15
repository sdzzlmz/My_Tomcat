package ex05.pyrmont.startup;

import ex05.pyrmont.core.SimpleContext;
import ex05.pyrmont.core.SimpleContextConfig;
import ex05.pyrmont.core.SimpleContextLifecycleListener;
import ex05.pyrmont.core.SimpleContextMapper;
import ex05.pyrmont.core.SimpleLoader;
import ex05.pyrmont.core.SimpleWrapper;
import ex05.pyrmont.loader.WebappClassLoader;
import ex05.pyrmont.loader.WebappLoader;

import org.apache.catalina.Connector;
import org.apache.catalina.Context;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.Loader;
import org.apache.catalina.Mapper;
import org.apache.catalina.Wrapper;
import org.apache.catalina.connector.http.HttpConnector;
import org.apache.catalina.core.StandardContext;
import org.apache.naming.resources.ProxyDirContext;

public final class Bootstrap {
  public static void main(String[] args) {
    
	System.setProperty("catalina.base", System.getProperty("user.dir"));
    
    Wrapper wrapper1 = new SimpleWrapper();
    wrapper1.setName("Primitive");
    wrapper1.setServletClass("PrimitiveServlet");
    
    Wrapper wrapper2 = new SimpleWrapper();
    wrapper2.setName("Modern");
    wrapper2.setServletClass("ModernServlet");

    Context context = new StandardContext();
    context.setPath("/myApp");
    context.setDocBase("myApp");
    context.addChild(wrapper1);
    context.addChild(wrapper2);

//    Mapper mapper = new SimpleContextMapper();
//    mapper.setProtocol("http");
//    context.addMapper(mapper);
    
    LifecycleListener listener = new SimpleContextConfig();
    ((Lifecycle) context).addLifecycleListener(listener);
    
    Loader loader = new WebappLoader();
    context.setLoader(loader);
    
    // context.addServletMapping(pattern, name);
    context.addServletMapping("/Primitive", "Primitive");
    context.addServletMapping("/Modern", "Modern");
    
    Connector connector = new HttpConnector();
    connector.setContainer(context);
    
    try {
      connector.initialize();
      ((Lifecycle) connector).start();
      ((Lifecycle) context).start();

      // make the application wait until we press a key.
      WebappClassLoader classLoader = (WebappClassLoader)loader.getClassLoader();
      System.out.println("Resources' docBase: " + ((ProxyDirContext)classLoader.getResources()).getDocBase());
      String[] repositories = classLoader.findRepositories();
      for (int i=0; i<repositories.length; i++) 
        System.out.println("  repository: " + repositories[i]);
      System.in.read();
      ((Lifecycle) context).stop();}
    catch (Exception e) {
      e.printStackTrace();
    }  
  }
}