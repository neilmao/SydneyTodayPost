package SydTodayPost;

import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Hello world!
 *
 */
public class App 
{
    private static Log log = LogFactory.getLog(App.class);
    private static final String SETTINGS_FILE = "/settings.properties";

    public static void main( String[] args ) throws IOException {

        log.info( "Loading configurations from file \"" + SETTINGS_FILE + "\"..." );
        Properties properties = new App().loadSettings();

        if (properties == null) {
            log.warn("File \"" + SETTINGS_FILE + "\" is missing, program exit.");
        } else {
            Runnable spider = new Spider(properties);
            Thread thread = new Thread(spider);
            thread.start();
        }
    }

    private Properties loadSettings() throws IOException {
        InputStream inputStream = getClass().getResourceAsStream(SETTINGS_FILE);
        if (inputStream != null) {
            Properties properties = new Properties();
            properties.load(inputStream);
            return properties;
        }
        return null;
    }
}
