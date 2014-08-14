package SydTodayPost;

import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

import java.io.FileInputStream;
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
    private static final String SETTINGS_FILE = "settings.properties";

    public static void main( String[] args ) throws IOException {

        log.info( "Loading configurations from file " + SETTINGS_FILE + "..." );
        Properties properties = new App().loadSettings();

        if (properties == null) {
            log.warn("File " + SETTINGS_FILE + " is missing, program exit.");
        } else {
            Spider spider = new Spider(properties);
            spider.execute();
        }
    }

    private Properties loadSettings() throws IOException {
        InputStream inputStream = new FileInputStream(SETTINGS_FILE);
        Properties properties = new Properties();
        properties.load(inputStream);
        return properties;
    }

}
