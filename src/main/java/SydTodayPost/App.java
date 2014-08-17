package SydTodayPost;

import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

import java.io.*;
import java.util.Properties;

/**
 * SydneyToday Auto Post Program
 *
 */
public class App 
{
    private static Log LOG = LogFactory.getLog(App.class);
    private static final String SETTINGS_FILE = "/settings.properties";

    public static void main( String[] args ) throws IOException {

        LOG.info("Loading configurations from file \"" + SETTINGS_FILE + "\"...");
        Properties properties = new App().loadSettings();

        if (properties == null) {
            LOG.warn("File \"" + SETTINGS_FILE + "\" is missing, program exit.");
        } else {
            Spider spider = new Spider(properties);
            Thread thread = new Thread(spider);

            String command = "Command: \"start\", \"stop\", \"status\", \"time\", \"interrupt\"";
            LOG.info(command);

            // listen on keyboard
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            String buffer;
            while ((buffer = reader.readLine()) != null) {
                if ("start".equals(buffer)) {
                    if (!thread.isAlive())
                        thread.start();
                    else
                        LOG.warn("Already started.");
                    continue;
                }
                if ("status".equals(buffer)) {
                    spider.reportStatus();
                    continue;
                }
                if ("time".equals(buffer)) {
                    continue;
                }
                if ("stop".equals(buffer)) {
                    spider.stop();
                    continue;
                }
                if ("interrupt".equals(buffer)) {
                    thread.interrupt();
                    return;
                }
                LOG.warn("Unknown command. " + command);
            }
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
