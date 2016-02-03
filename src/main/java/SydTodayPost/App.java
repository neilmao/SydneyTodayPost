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
    private static final String SETTINGS_FILE = "settings.properties";

    public static void main( String[] args ) throws IOException {

        LOG.info("Loading configurations from file \"" + SETTINGS_FILE + "\"...");

        Properties properties;
        try {
            properties = new App().loadSettings();
        } catch (IOException ex) {
            LOG.warn("File \"" + SETTINGS_FILE + "\" is missing, program exit.");
            createPropertyFile();
            return;
        }

        Spider spider = new Spider(properties);
        Thread thread = new Thread(spider);

        String command = "Command: \"start\", \"stop\", \"status\", \"quit\"";
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
            if ("stop".equals(buffer)) {
                spider.stop();
                return;
            }
            if ("quit".equals(buffer) || "q".equals(buffer)) {
                if (thread.isAlive()) {
                    spider.stop();
                    thread.interrupt();
                }
                return;
            }
            LOG.warn("Unknown command. " + command);
        }
    }

    private Properties loadSettings() throws IOException {
        InputStream inputStream = new FileInputStream(new File(SETTINGS_FILE));
        Properties properties = new Properties();
        properties.load(inputStream);
        return properties;
    }

    private static void createPropertyFile() throws IOException {
        FileOutputStream fos = new FileOutputStream(new File(SETTINGS_FILE));
        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));
        bw.write("username=\n");
        bw.write("password=\n");
        bw.write("# thread starts with http://\n");
        bw.write("thread=\n");
        bw.write("# delay every 15 mins to try if previous action received waiting\n");
        bw.write("delay=900000\n");
        bw.write("# post interval in milliseconds, defaults to 12 hours\n");
        bw.write("interval=43200000\n");
        bw.close();
    }
}
