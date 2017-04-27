import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.cloudformation.AmazonCloudFormation;
import com.amazonaws.services.cloudformation.AmazonCloudFormationClient;
import com.amazonaws.services.cloudformation.AmazonCloudFormationClientBuilder;
import com.amazonaws.services.cloudformation.model.DeleteStackRequest;
import com.amazonaws.services.cloudformation.model.DeleteStackResult;
import com.amazonaws.services.cloudformation.model.DescribeStacksRequest;
import com.amazonaws.waiters.WaiterHandler;
import com.amazonaws.waiters.WaiterParameters;
import com.google.common.base.Stopwatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.ws.Holder;

public class App {

    private static final Logger logger = LoggerFactory.getLogger(App.class);

    private final static String STACK_NAME = "AwsDeleteStackS1";
    private static AmazonCloudFormation acfClient;
    private static String awsAccessKey = "";
    private static String awsSecretKey = "";
    0
    private static Holder<Boolean> stackDelete = new Holder<>();
    private static Holder<Boolean> stackDeleteResult = new Holder<>();

    public static void main(String... args) throws InterruptedException {
        System.out.println("Begin Delete Stack " + STACK_NAME);
        Stopwatch timer = Stopwatch.createStarted();
        deleteStack();
        System.out.println("End Delete Stack " + STACK_NAME);
        logger.info("Delete Stack took: " + timer.stop());
        acfClient.shutdown();
    }


    private static void deleteStack() throws InterruptedException {
        acfClient = getClient();

        DeleteStackRequest delStackReq = new DeleteStackRequest().
            withStackName(STACK_NAME).withSdkRequestTimeout(300 * 1000);
        DeleteStackResult delStackRes = acfClient.deleteStack(delStackReq);
        logger.debug("delStackRes = {}", delStackRes);

        stackDelete.value = false;
        stackDeleteResult.value = false;

        // Should the waiter handler require DeleteStackRequest instead of DescribeStackRequest?
        acfClient.waiters().stackDeleteComplete().runAsync(new WaiterParameters()
                                                               .withRequest(new DescribeStacksRequest()
                                                                                .withSdkRequestTimeout(300 * 1000)
                                                               ), new WaiterHandler<DescribeStacksRequest>() {
                                                               @Override
                                                               public void onWaitSuccess(DescribeStacksRequest request) {
                                                                   // request is null pointer
                                                                   logger.info("Stack deletion success!!!!!");
                                                                   stackDelete.value = true;
                                                                   stackDeleteResult.value = true;
                                                               }

                                                               @Override
                                                               public void onWaitFailure(Exception e) {
                                                                   logger.error("AWS is telling us that the stack delete failed!!", e);
                                                                   stackDelete.value = true;
                                                                   stackDeleteResult.value = false;
                                                               }
                                                           }
        );

        logger.info("Waiting for stack deletion to complete...");
        try {
            while (!stackDelete.value) {
                logger.info("Check status of delete operation");
                Thread.sleep(1 * 1000);
            }
        } catch (InterruptedException | RuntimeException e) {
            logger.error("Stack deletion FAILED!!", e);
            throw e;
        }

        acfClient.shutdown();
    }

    public static AmazonCloudFormation getClient() {
        AmazonCloudFormationClientBuilder acb = AmazonCloudFormationClient.builder()
            .withRegion(Regions.DEFAULT_REGION);
        if (!(awsAccessKey.isEmpty() && awsSecretKey.isEmpty())) {
            setCredentials(acb, awsAccessKey, awsSecretKey);
        }
        return acb.build();
    }

    private static void setCredentials(AwsClientBuilder acb, final String awsAccessKey, final String awsSecretKey) {
        acb.setCredentials(new AWSCredentialsProvider() {
            @Override
            public AWSCredentials getCredentials() {
                return new AWSCredentials() {
                    @Override
                    public String getAWSAccessKeyId() {
                        return awsAccessKey;
                    }

                    @Override
                    public String getAWSSecretKey() {
                        return awsSecretKey;
                    }
                };
            }

            @Override
            public void refresh() {
            }
        });
    }
}
