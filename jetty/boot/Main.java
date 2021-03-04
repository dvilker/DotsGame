import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;

public class Main {
    public static void main(String[] args) throws Throwable {
        ClassLoader parentClassloader = Main.class.getClassLoader();
        ArrayList<URL> urls = new ArrayList<>(32);
        for (int i = 0; true; i++) {
            URL url = parentClassloader.getResource(Integer.toString(i));
            if (url == null) {
                break;
            }
            urls.add(new URL(url.toString() + "/"));
        }
        URLClassLoader appClassLoader = new URLClassLoader(
                urls.toArray(new URL[0]),
                parentClassloader.getParent()
        );
        try {
            Thread.currentThread().setContextClassLoader(appClassLoader);
            appClassLoader.loadClass("MainKt").getMethod("main", String[].class).invoke(null, (Object) args);
        } catch (InvocationTargetException e) {
            throw e.getTargetException();
        }
    }
}
