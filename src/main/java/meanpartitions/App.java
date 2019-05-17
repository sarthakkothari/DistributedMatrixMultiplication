package meanpartitions;

import org.apache.hadoop.util.ToolRunner;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

/**
 * Hello world!
 *
 */
public class App 
{
    private static final Logger logger = LogManager.getLogger(App.class);

    public static void main( String[] args )
    {
        if (args.length != 3) {
            throw new Error("lol lol lol lol arguments wrong");
        }

        try {
            // To run Simple BB uncomment below
            ToolRunner.run(new BBAlgorithm(), args);

            // To run Cannon uncomment below
            // ToolRunner.run(new CannonAlgorithm(), args);
        } catch (final Exception e) {
            logger.error("", e);
        }
    }
}
