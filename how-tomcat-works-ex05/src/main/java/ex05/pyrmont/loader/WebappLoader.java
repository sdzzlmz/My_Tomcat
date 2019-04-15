package ex05.pyrmont.loader;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilePermission;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLStreamHandlerFactory;
import java.util.jar.JarFile;

import javax.naming.Binding;
import javax.naming.NameClassPair;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.servlet.ServletContext;

import org.apache.catalina.Container;
import org.apache.catalina.Context;
import org.apache.catalina.DefaultContext;
import org.apache.catalina.Globals;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.Loader;
import org.apache.catalina.loader.Constants;
import org.apache.catalina.loader.Extension;
import org.apache.catalina.loader.WebappClassLoader;
import org.apache.catalina.util.LifecycleSupport;
import org.apache.catalina.util.StringManager;
import org.apache.naming.resources.DirContextURLStreamHandler;
import org.apache.naming.resources.DirContextURLStreamHandlerFactory;
import org.apache.naming.resources.Resource;
import org.apache.tomcat.util.digester.ObjectParamRule;

public class WebappLoader implements Lifecycle, Loader, PropertyChangeListener, Runnable {
/********************************************instance variables*****/
    /**
     * The number of seconds between checks for modified classes, if
     * automatic reloading is enabled.
     */
    private int checkInterval = 15;
    /**
     * The class loader being managed by this Loader component.
     */
    private WebappClassLoader classLoader = null;


    /**
     * The Container with which this Loader has been associated.
     */
    private Container container = null;


    /**
     * The debugging detail level for this component.
     */
    private int debug = 0;


    /**
     * The DefaultContext with which this Loader is associated.
     */
    protected DefaultContext defaultContext = null;


    /**
     * The "follow standard delegation model" flag that will be used to
     * configure our ClassLoader.
     */
    private boolean delegate = false;


    /**
     * The descriptive information about this Loader implementation.
     */
    private static final String info =
        "org.apache.catalina.loader.WebappLoader/1.0";


    /**
     * The lifecycle event support for this component.
     */
    protected LifecycleSupport lifecycle = new LifecycleSupport(this);


    /**
     * The Java class name of the ClassLoader implementation to be used.
     * This class should extend WebappClassLoader, otherwise, a different
     * loader implementation must be used.
     */
    private String loaderClass =
        "org.apache.catalina.loader.WebappClassLoader";


    /**
     * The parent class loader of the class loader we will create.
     */
    private ClassLoader parentClassLoader = null;


    /**
     * The reloadable flag for this Loader.
     */
    private boolean reloadable = false;


    /**
     * The set of repositories associated with this class loader.
     */
    private String repositories[] = new String[0];


    /**
     * The string manager for this package.
     */
    protected static final StringManager sm =
        StringManager.getManager(Constants.Package);


    /**
     * Has this component been started?
     */
    private boolean started = false;


    /**
     * The property change support for this component.
     */
    protected PropertyChangeSupport support = new PropertyChangeSupport(this);


    /**
     * The background thread.
     */
    private Thread thread = null;


    /**
     * The background thread completion semaphore.
     */
    private boolean threadDone = false;


    /**
     * Name to register for the background thread.
     */
    private String threadName = "WebappLoader";

/************************************************private methods**********/
    private WebappClassLoader createClassLoader() throws Exception {
    	Class clazz = Class.forName(loaderClass);
    	WebappClassLoader classloader = null;
    	if (parentClassLoader == null) {
    		classloader = (WebappClassLoader) clazz.newInstance();
    	}
    	else {
    		Class[] argsTypes = { ClassLoader.class };
    		Object[] args = { parentClassLoader };
    		Constructor constr = clazz.getConstructor();
    		classLoader = (WebappClassLoader) constr.newInstance(args); 
    	}
    	return classLoader;
    }
/************************Runnable methods****************************/
	public void run() {
		// TODO Auto-generated method stub
		if (debug >= 1) {
			System.out.println("BACKGROUND THREAD Starting!");
		}
		while (!threadDone) {
			threadSleep();
			if(!started) {
				break;
			}
			try {
				if (!classLoader.modified()) {
					continue;
				}
			} catch (Exception e) {
				// TODO: handle exception
				e.printStackTrace();
			}
			notifyContext();
			break;
		}
		if(debug >= 1) {
			System.out.println("BACKGROUND THREAD stopping");
		}

	}
	 /**
     * Start the background thread that will periodically check for
     * session timeouts.
     *
     * @exception IllegalStateException if we should not be starting
     *  a background thread now
     */
    private void threadStart() {

        // Has the background thread already been started?
        if (thread != null)
            return;

        // Validate our current state
        if (!reloadable)
            throw new IllegalStateException
                (sm.getString("webappLoader.notReloadable"));
        if (!(container instanceof Context))
            throw new IllegalStateException
                (sm.getString("webappLoader.notContext"));

        // Start the background thread
        if (debug >= 1)
            System.out.println(" Starting background thread");
        threadDone = false;
        threadName = "WebappLoader[" + container.getName() + "]";
        thread = new Thread(this, threadName);
        thread.setDaemon(true);
        thread.start();
    }
	private void threadSleep() {
		try {
			Thread.sleep(checkInterval * 1000L); 
		} 
		catch (InterruptedException e) {
			// TODO: handle exception
			e.printStackTrace();
		}
	}
	private void notifyContext() {
		WebappContextNotifier notifier = new WebappContextNotifier();
		(new Thread(notifier)).start();
	}
	public ClassLoader getClassLoader() {
		// TODO Auto-generated method stub
		
		return (ClassLoader)classLoader;
	}

	public Container getContainer() {
		// TODO Auto-generated method stub
		return container;
	}

	public void setContainer(Container container) {
		// TODO Auto-generated method stub
        // Deregister from the old Container (if any)
        if ((this.container != null) && (this.container instanceof Context))
            ((Context) this.container).removePropertyChangeListener(this);

        // Process this property change
        Container oldContainer = this.container;
        this.container = container;
        support.firePropertyChange("container", oldContainer, this.container);

        // Register with the new Container (if any)
        if ((this.container != null) && (this.container instanceof Context)) {
            setReloadable( ((Context) this.container).getReloadable() );
            ((Context) this.container).addPropertyChangeListener(this);
        }

	}

	public DefaultContext getDefaultContext() {
		// TODO Auto-generated method stub
		return null;
	}

	public void setDefaultContext(DefaultContext defaultContext) {
		// TODO Auto-generated method stub

	}

	public boolean getDelegate() {
		// TODO Auto-generated method stub
		return false;
	}

	public void setDelegate(boolean delegate) {
		// TODO Auto-generated method stub

	}

	public String getInfo() {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean getReloadable() {
		// TODO Auto-generated method stub
		return false;
	}

	public void setReloadable(boolean reloadable) {
		// TODO Auto-generated method stub

	}

	public void addPropertyChangeListener(PropertyChangeListener listener) {
		// TODO Auto-generated method stub

	}

	public void addRepository(String repository) {
		// TODO Auto-generated method stub

	}

	public String[] findRepositories() {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean modified() {
		// TODO Auto-generated method stub
		return false;
	}

	public void removePropertyChangeListener(PropertyChangeListener listener) {
		// TODO Auto-generated method stub

	}
/***********************Lifecycle methods************/
	public void addLifecycleListener(LifecycleListener listener) {
		// TODO Auto-generated method stub

	}

	public LifecycleListener[] findLifecycleListeners() {
		// TODO Auto-generated method stub
		return null;
	}

	public void removeLifecycleListener(LifecycleListener listener) {
		// TODO Auto-generated method stub

	}

	public void start() throws LifecycleException {
		// TODO Auto-generated method stub
        // Validate and update our current component state
        if (started)
            throw new LifecycleException
                (sm.getString("webappLoader.alreadyStarted"));
        if (debug >= 1)
            System.out.println(sm.getString("webappLoader.starting"));
        lifecycle.fireLifecycleEvent(START_EVENT, null);
        started = true;

        if (container.getResources() == null)
            return;

        // Register a stream handler factory for the JNDI protocol
        URLStreamHandlerFactory streamHandlerFactory =
            new DirContextURLStreamHandlerFactory();
        try {
            URL.setURLStreamHandlerFactory(streamHandlerFactory);
        } catch (Throwable t) {
            // Ignore the error here.
        }

        // Construct a class loader based on our current repositories list
        try {

            classLoader = createClassLoader();
            classLoader.setResources(container.getResources());
            classLoader.setDebug(this.debug);
            classLoader.setDelegate(this.delegate);

            for (int i = 0; i < repositories.length; i++) {
                classLoader.addRepository(repositories[i]);
            }

            // Configure our repositories
            setRepositories();
            setClassPath();
            setPermissions();

            if (classLoader instanceof Lifecycle)
                ((Lifecycle) classLoader).start();

            // Binding the Webapp class loader to the directory context
            DirContextURLStreamHandler.bind
                ((ClassLoader) classLoader, this.container.getResources());

        } catch (Throwable t) {
            throw new LifecycleException("start: ", t);
        }

        // Validate that all required packages are actually available
        validatePackages();

        // Start our background thread if we are reloadable
        if (reloadable) {
            System.out.println(sm.getString("webappLoader.reloading"));
            try {
                threadStart();
            } catch (IllegalStateException e) {
                throw new LifecycleException(e);
            }
        }


	}
	   /**
     * Validate that the required optional packages for this application
     * are actually present.
     *
     * @exception LifecycleException if a required package is not available
     */
    private void validatePackages() throws LifecycleException {

        ClassLoader classLoader = getClassLoader();
        if (classLoader instanceof WebappClassLoader) {

            Extension available[] =
                ((WebappClassLoader) classLoader).findAvailable();
            Extension required[] =
                ((WebappClassLoader) classLoader).findRequired();
            if (debug >= 1)
                System.out.println("Optional Packages:  available=" +
                    available.length + ", required=" +
                    required.length);

            for (int i = 0; i < required.length; i++) {
                if (debug >= 1)
                    System.out.println("Checking for required package " + required[i]);
                boolean found = false;
                for (int j = 0; j < available.length; j++) {
                    if (available[j].isCompatibleWith(required[i])) {
                        found = true;
                        break;
                    }
                }
                if (!found)
                    throw new LifecycleException
                        ("Missing optional package " + required[i]);
            }

        }

    }

	private boolean copyDir(DirContext srcDir, File destDir) {

        try {

            NamingEnumeration enum1 = srcDir.list("");
            while (enum1.hasMoreElements()) {
                NameClassPair ncPair =
                    (NameClassPair) enum1.nextElement();
                String name = ncPair.getName();
                Object object = srcDir.lookup(name);
                File currentFile = new File(destDir, name);
                if (object instanceof Resource) {
                    InputStream is = ((Resource) object).streamContent();
                    OutputStream os = new FileOutputStream(currentFile);
                    if (!copy(is, os))
                        return false;
                } else if (object instanceof InputStream) {
                    OutputStream os = new FileOutputStream(currentFile);
                    if (!copy((InputStream) object, os))
                        return false;
                } else if (object instanceof DirContext) {
                    currentFile.mkdir();
                    copyDir((DirContext) object, currentFile);
                }
            }

        } catch (NamingException e) {
            return false;
        } catch (IOException e) {
            return false;
        }

        return true;

    }

	/**
     * Copy a file to the specified temp directory. This is required only
     * because Jasper depends on it.
     */
    private boolean copy(InputStream is, OutputStream os) {

        try {
            byte[] buf = new byte[4096];
            while (true) {
                int len = is.read(buf);
                if (len < 0)
                    break;
                os.write(buf, 0, len);
            }
            is.close();
            os.close();
        } catch (IOException e) {
            return false;
        }

        return true;

    }


	public void stop() throws LifecycleException {
		// TODO Auto-generated method stub

	}
	   /**
     * Configure associated class loader permissions.
     */
    private void setPermissions() {

        if (System.getSecurityManager() == null)
            return;
        if (!(container instanceof Context))
            return;

        // Tell the class loader the root of the context
        ServletContext servletContext =
            ((Context) container).getServletContext();

        // Assigning permissions for the work directory
        File workDir =
            (File) servletContext.getAttribute(Globals.WORK_DIR_ATTR);
        if (workDir != null) {
            try {
                String workDirPath = workDir.getCanonicalPath();
                classLoader.addPermission
                    (new FilePermission(workDirPath, "read,write"));
                classLoader.addPermission
                    (new FilePermission(workDirPath + File.separator + "-",
                                        "read,write,delete"));
            } catch (IOException e) {
                // Ignore
            }
        }

        try {

            URL rootURL = servletContext.getResource("/");
            classLoader.addPermission(rootURL);

            String contextRoot = servletContext.getRealPath("/");
            if (contextRoot != null) {
                try {
                    contextRoot =
                        (new File(contextRoot)).getCanonicalPath()
                        + File.separator;
                    classLoader.addPermission(contextRoot);
                } catch (IOException e) {
                    // Ignore
                }
            }

            URL classesURL =
                servletContext.getResource("/WEB-INF/classes/");
            if (classesURL != null)
                classLoader.addPermission(classesURL);

            URL libURL = servletContext.getResource("/WEB-INF/lib/");
            if (libURL != null) {
                classLoader.addPermission(libURL);
            }

            if (contextRoot != null) {

                if (libURL != null) {
                    File rootDir = new File(contextRoot);
                    File libDir = new File(rootDir, "WEB-INF/lib/");
                    String path = null;
                    try {
                        path = libDir.getCanonicalPath() + File.separator;
                    } catch (IOException e) {
                    }
                    if (path != null)
                        classLoader.addPermission(path);
                }

            } else {

                if (workDir != null) {
                    if (libURL != null) {
                        File libDir = new File(workDir, "WEB-INF/lib/");
                        String path = null;
                        try {
                            path = libDir.getCanonicalPath() + File.separator;
                        } catch (IOException e) {
                        }
                        classLoader.addPermission(path);
                    }
                    if (classesURL != null) {
                        File classesDir =
                            new File(workDir, "WEB-INF/classes/");
                        String path = null;
                        try {
                            path = classesDir.getCanonicalPath()
                                + File.separator;
                        } catch (IOException e) {
                        }
                        classLoader.addPermission(path);
                    }
                }

            }

        } catch (MalformedURLException e) {
        }

    }


    /**
     * Configure the repositories for our class loader, based on the
     * associated Context.
     */
    private void setRepositories() {

        if (!(container instanceof Context))
            return;
        ServletContext servletContext =
            ((Context) container).getServletContext();
        if (servletContext == null)
            return;

        // Loading the work directory
        File workDir =
            (File) servletContext.getAttribute(Globals.WORK_DIR_ATTR);
        if (workDir == null)
            return;
        System.out.println(sm.getString("webappLoader.deploy", workDir.getAbsolutePath()));

        DirContext resources = container.getResources();

        // Setting up the class repository (/WEB-INF/classes), if it exists

        String classesPath = "/WEB-INF/classes";
        DirContext classes = null;

        try {
            Object object = resources.lookup(classesPath);
            if (object instanceof DirContext) {
                classes = (DirContext) object;
            }
        } catch(NamingException e) {
            // Silent catch: it's valid that no /WEB-INF/classes collection
            // exists
        }

        if (classes != null) {

            File classRepository = null;

            String absoluteClassesPath =
                servletContext.getRealPath(classesPath);

            if (absoluteClassesPath != null) {

                classRepository = new File(absoluteClassesPath);

            } else {

                classRepository = new File(workDir, classesPath);
                classRepository.mkdirs();
                copyDir(classes, classRepository);

            }

            System.out.println(sm.getString("webappLoader.classDeploy", classesPath,
                             classRepository.getAbsolutePath()));


            // Adding the repository to the class loader
            classLoader.addRepository(classesPath + "/", classRepository);

        }

        // Setting up the JAR repository (/WEB-INF/lib), if it exists

        String libPath = "/WEB-INF/lib";

        classLoader.setJarPath(libPath);

        DirContext libDir = null;
        // Looking up directory /WEB-INF/lib in the context
        try {
            Object object = resources.lookup(libPath);
            if (object instanceof DirContext)
                libDir = (DirContext) object;
        } catch(NamingException e) {
            // Silent catch: it's valid that no /WEB-INF/lib collection
            // exists
        }

        if (libDir != null) {

            boolean copyJars = false;
            String absoluteLibPath = servletContext.getRealPath(libPath);

            File destDir = null;

            if (absoluteLibPath != null) {
                destDir = new File(absoluteLibPath);
            } else {
                copyJars = true;
                destDir = new File(workDir, libPath);
                destDir.mkdirs();
            }

            // Looking up directory /WEB-INF/lib in the context
            try {
                NamingEnumeration enum1 = resources.listBindings(libPath);
                while (enum1.hasMoreElements()) {

                    Binding binding = (Binding) enum1.nextElement();
                    String filename = libPath + "/" + binding.getName();
                    if (!filename.endsWith(".jar"))
                        continue;

                    // Copy JAR in the work directory, always (the JAR file
                    // would get locked otherwise, which would make it
                    // impossible to update it or remove it at runtime)
                    File destFile = new File(destDir, binding.getName());

                    System.out.println(sm.getString("webappLoader.jarDeploy", filename,
                                     destFile.getAbsolutePath()));

                    Resource jarResource = (Resource) binding.getObject();
                    if (copyJars) {
                        if (!copy(jarResource.streamContent(),
                                  new FileOutputStream(destFile)))
                            continue;
                    }

                    JarFile jarFile = new JarFile(destFile);
                    classLoader.addJar(filename, jarFile, destFile);

                }
            } catch (NamingException e) {
                // Silent catch: it's valid that no /WEB-INF/lib directory
                // exists
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

    }


    /**
     * Set the appropriate context attribute for our class path.  This
     * is required only because Jasper depends on it.
     */
    private void setClassPath() {

        // Validate our current state information
        if (!(container instanceof Context))
            return;
        ServletContext servletContext =
            ((Context) container).getServletContext();
        if (servletContext == null)
            return;

        StringBuffer classpath = new StringBuffer();

        // Assemble the class path information from our class loader chain
        ClassLoader loader = getClassLoader();
        int layers = 0;
        int n = 0;
        while ((layers < 3) && (loader != null)) {
            if (!(loader instanceof URLClassLoader))
                break;
            URL repositories[] =
                ((URLClassLoader) loader).getURLs();
            for (int i = 0; i < repositories.length; i++) {
                String repository = repositories[i].toString();
                if (repository.startsWith("file://"))
                    repository = repository.substring(7);
                else if (repository.startsWith("file:"))
                    repository = repository.substring(5);
                else if (repository.startsWith("jndi:"))
                    repository =
                        servletContext.getRealPath(repository.substring(5));
                else
                    continue;
                if (repository == null)
                    continue;
                if (n > 0)
                    classpath.append(File.pathSeparator);
                classpath.append(repository);
                n++;
            }
            loader = loader.getParent();
            layers++;
        }

        // Store the assembled class path as a servlet context attribute
        servletContext.setAttribute(Globals.CLASS_PATH_ATTR,
                                    classpath.toString());

    }


	/*******************************PropertyChangeListener methods******************/
	public void propertyChange(PropertyChangeEvent evt) {
		
	}
	// -------------------------------------- WebappContextNotifier Inner Class


    /**
     * Private thread class to notify our associated Context that we have
     * recognized the need for a reload.
     */
    protected class WebappContextNotifier implements Runnable {
        /**
         * Perform the requested notification.
         */
        public void run() {

            ((Context) container).reload();

        }
    }

}
